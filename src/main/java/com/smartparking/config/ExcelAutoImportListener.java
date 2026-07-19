package com.smartparking.config;

import com.smartparking.entity.Fee;
import com.smartparking.entity.ParkingSpot;
import com.smartparking.entity.Resident;
import com.smartparking.entity.Vehicle;
import com.smartparking.repository.BaseRepository;
import com.smartparking.repository.FeeRepository;
import com.smartparking.repository.ParkingSpotRepository;
import com.smartparking.repository.ResidentRepository;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.util.ExcelUtil;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ExcelAutoImportListener {
    @Resource
    private EntityManager entityManager;
    @Resource
    private ResidentRepository residentRepository;
    @Resource
    private VehicleRepository vehicleRepository;
    @Resource
    private FeeRepository feeRepository;
    @Resource
    private ParkingSpotRepository parkingSpotRepository;
    @Resource
    private InitDataProperties initDataProperties;

    public static final class TableImportMeta<T, ID> {
        String templatePath;
        Class<T> entityCls;
        BaseRepository<T, ID> repository;

        public TableImportMeta(String templatePath, Class<T> entityCls, BaseRepository<T, ID> repository) {
            this.templatePath = templatePath;
            this.entityCls = entityCls;
            this.repository = repository;
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void autoImportExcelOnStart() {
        boolean forceRefresh = initDataProperties.isForceRefresh();
        List<TableImportMeta<?, ?>> importList = List.of(
                new TableImportMeta<>("excel-template/用户信息.xlsx", Resident.class, residentRepository),
                new TableImportMeta<>("excel-template/车辆记录.xlsx", Vehicle.class, vehicleRepository),
                new TableImportMeta<>("excel-template/费用记录.xlsx", Fee.class, feeRepository)
        );

        for (TableImportMeta<?, ?> meta : importList) {
            String filePath = meta.templatePath;
            BaseRepository<?, ?> repo = meta.repository;
            long tableCount = repo.count();

            ClassPathResource resource = new ClassPathResource(filePath);
            if (!resource.exists()) {
                System.out.printf("[DEBUG 自动导入] 文件【%s】不存在，跳过该表导入%n", filePath);
                continue;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                if (forceRefresh) {
                    System.out.printf("[强制重置] 清空表%s，重置自增主键，开始导入模板%n", meta.entityCls.getSimpleName());

                    repo.deleteAll();

                    String tableName;
                    Table tableAnno = meta.entityCls.getAnnotation(Table.class);
                    if(tableAnno != null && !tableAnno.name().isBlank()){
                        tableName = tableAnno.name();
                    }else{
                        tableName = meta.entityCls.getSimpleName();
                    }
                    // H2重置自增SQL，直接拼接表名（表名不能用?占位符）
                    String resetSql = "ALTER TABLE " + tableName + " ALTER COLUMN id RESTART WITH 1";
                    // 执行DDL
                    entityManager.createNativeQuery(resetSql).executeUpdate();
                    // 刷新持久化上下文
                    entityManager.flush();

                    doImport(meta, inputStream);
                    continue;
                }

                if (tableCount == 0) {
                    System.out.printf("[初始化导入] 表%s无数据，执行Excel导入%n", meta.entityCls.getSimpleName());
                    doImport(meta, inputStream);
                } else {
                    System.out.printf("[初始化导入] 表%s已有%d条数据，跳过导入%n", meta.entityCls.getSimpleName(), tableCount);
                }
            } catch (IOException e) {
                System.err.printf("[DEBUG 自动导入] 文件【%s】读取IO异常：%s%n", filePath, e.getMessage());
            }
        }

        // ===== 关键补充：同步车位状态 =====
        // 导入车辆记录后，检查所有 status=PARKING 的车辆，同步更新 parking_spot 表
        syncParkingSpotsFromVehicles();

        System.out.println("===== 项目启动自动Excel导入执行完成 =====");
    }

    /**
     * 同步车位状态：将所有在场车辆（status=PARKING）对应的车位标记为 OCCUPIED
     */
    @Transactional
    public void syncParkingSpotsFromVehicles() {
        List<Vehicle> parkingVehicles = vehicleRepository.findByStatus("PARKING");
        if (parkingVehicles.isEmpty()) {
            System.out.println("[车位同步] 无在场车辆，跳过");
            return;
        }

        int updated = 0;
        for (Vehicle v : parkingVehicles) {
            String spotNumber = v.getSpotNumber();
            if (spotNumber == null || spotNumber.isBlank()) {
                continue;
            }
            String plateNumber = v.getPlateNumber();

            java.util.Optional<ParkingSpot> opt = parkingSpotRepository.findBySpotNumber(spotNumber);
            if (opt.isPresent()) {
                ParkingSpot spot = opt.get();
                spot.setStatus("OCCUPIED");
                spot.setCurrentPlate(plateNumber);
                parkingSpotRepository.save(spot);
                updated++;
                System.out.printf("[车位同步] 车位 %s → OCCUPIED（%s）%n", spotNumber, plateNumber);
            } else {
                System.out.printf("[车位同步] 警告：车辆 %s 关联的车位 %s 不存在%n", plateNumber, spotNumber);
            }
        }
        System.out.printf("[车位同步] 完成：共同步 %d 个车位%n", updated);
    }

    @Transactional
    public void doImport(TableImportMeta<?, ?> meta, InputStream inputStream) {
        try {
            List<Object> entityList = ExcelUtil.<Object>importExcelByStream(inputStream, (Class<Object>) meta.entityCls);

            Set<Long> idSet = new HashSet<>();
            Field idField = meta.entityCls.getDeclaredField("id");
            idField.setAccessible(true);
            boolean hasDuplicateId = false;
            Long duplicateId = null;
            for (Object row : entityList) {
                Long id = (Long) idField.get(row);
                // ID为空时跳过重复校验
                if (id != null) {
                    if (!idSet.add(id)) {
                        hasDuplicateId = true;
                        duplicateId = id;
                        break;
                    }
                }
            }

            if (hasDuplicateId) {
                System.err.printf("[导入失败] %s Excel内部存在重复主键ID=%d，跳过当前表%n", meta.templatePath, duplicateId);
                return;
            }

            BaseRepository<Object, Long> repoCast = (BaseRepository<Object, Long>) meta.repository;
            int importCount = repoCast.saveAll(entityList).size();
            System.out.printf("[导入成功] %s 导入%d条数据%n", meta.templatePath, importCount);
        } catch (Exception e) {
            System.err.printf("[导入失败] %s 数据异常，跳过当前表：%s%n", meta.templatePath, e.getMessage());
        }
    }
}