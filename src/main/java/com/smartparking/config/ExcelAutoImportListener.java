package com.smartparking.config;

import com.smartparking.entity.Fee;
import com.smartparking.entity.Resident;
import com.smartparking.entity.Vehicle;
import com.smartparking.repository.BaseRepository;
import com.smartparking.repository.FeeRepository;
import com.smartparking.repository.ResidentRepository;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.service.ExcelCommonService;
import com.smartparking.util.ExcelUtil;
import jakarta.annotation.Resource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class ExcelAutoImportListener {

    @Resource
    private ExcelCommonService excelCommonService;
    @Resource
    private ResidentRepository residentRepository;
    @Resource
    private VehicleRepository vehicleRepository;
    @Resource
    private FeeRepository feeRepository;

    private static final class TableImportMeta<T, ID> {
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
    public void autoImportExcelOnStart() {
        List<TableImportMeta<?, ?>> importList = List.of(
                new TableImportMeta<>("excel-template/resident_template.xlsx", Resident.class, residentRepository),
                new TableImportMeta<>("excel-template/vehicle_template.xlsx", Vehicle.class, vehicleRepository),
                new TableImportMeta<>("excel-template/fee_template.xlsx", Fee.class, feeRepository)
        );

        for (TableImportMeta<?, ?> meta : importList) {
            try (InputStream inputStream = ExcelUtil.class.getClassLoader().getResourceAsStream(meta.templatePath)) {
                if (inputStream == null) {
                    System.out.printf("[自动导入] 模板文件 %s 不存在，跳过导入%n", meta.templatePath);
                    continue;
                }
                // 强制指定泛型<Object,Long>，类型强转匹配参数
                int importCount = excelCommonService.<Object, Long>importDataByStream(
                        inputStream,
                        (Class<Object>) meta.entityCls,
                        (BaseRepository<Object, Long>) meta.repository
                );
                System.out.printf("[自动导入] %s 成功导入 %d 条初始数据%n", meta.templatePath, importCount);
            } catch (Exception e) {
                System.err.printf("[自动导入] %s 导入失败：%s%n", meta.templatePath, e.getMessage());
            }
        }
        System.out.println("===== 项目启动自动Excel导入执行完成 =====");
    }
}