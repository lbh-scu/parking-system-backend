package com.smartparking.service;

import com.smartparking.entity.ParkingSpot;
import com.smartparking.entity.SystemConfig;
import com.smartparking.repository.ParkingSpotRepository;
import com.smartparking.repository.SystemConfigRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemConfigService {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    /**
     * 初始化默认配置（如果数据库中没有则创建）
     * 注意：车位初始化由 ParkingSpotInitializer 负责，本模块不再重复创建
     */
    @PostConstruct
    public void initDefaultConfig() {
        createIfNotExist("hourly_rate", "5.00", "每小时停车费率（元）");
        createIfNotExist("free_minutes", "30", "免费停车时长（分钟）");
        createIfNotExist("daily_max", "50.00", "每日封顶费用（元）");
        createIfNotExist("total_spots_a", "80", "A区总车位数");
        createIfNotExist("total_spots_b", "70", "B区总车位数");
        createIfNotExist("total_spots_c", "50", "C区总车位数");
    }

    private void createIfNotExist(String key, String value, String description) {
        if (systemConfigRepository.findByConfigKey(key).isEmpty()) {
            SystemConfig config = new SystemConfig(key, value, description);
            systemConfigRepository.save(config);
        }
    }

    /**
     * 获取所有系统配置
     */
    public List<Map<String, Object>> getAllConfigs() {
        List<SystemConfig> configs = systemConfigRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (SystemConfig config : configs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", config.getId());
            item.put("configKey", config.getConfigKey());
            item.put("configValue", config.getConfigValue());
            item.put("description", config.getDescription());
            item.put("updatedAt", config.getUpdatedAt());
            result.add(item);
        }
        return result;
    }

    /**
     * 更新配置
     * 如果是车位数量配置修改，同步更新实际车位表
     * @param key 配置键
     * @param value 配置值
     */
    @Transactional
    public SystemConfig updateConfig(String key, String value) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new RuntimeException("配置项不存在: " + key));
        config.setConfigValue(value);
        SystemConfig saved = systemConfigRepository.save(config);

        // 如果是车位数量配置修改，同步更新实际车位表
        if (key.startsWith("total_spots_")) {
            String areaKey = key.substring("total_spots_".length()).toUpperCase();
            // 适配其他成员的车位区域命名："A" -> "A区", "B" -> "B区", "C" -> "C区"
            String areaName = areaKey + "区";
            int targetCount = Integer.parseInt(value);
            syncParkingSpots(areaName, targetCount);
        }

        return saved;
    }

    /**
     * 同步车位：根据配置的目标数量，增删 parking_spot 表中的实际车位记录
     * 此方法不修改成员B的任何代码，直接操作数据库
     */
    private void syncParkingSpots(String area, int targetCount) {
        List<ParkingSpot> existingSpots = parkingSpotRepository.findByArea(area);
        int currentCount = existingSpots.size();

        if (targetCount > currentCount) {
            // 需要新增车位
            int toAdd = targetCount - currentCount;
            int maxNum = existingSpots.stream()
                    .mapToInt(s -> {
                        try {
                            return Integer.parseInt(s.getSpotNumber().substring(1));
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .max()
                    .orElse(0);

            for (int i = 1; i <= toAdd; i++) {
                int spotNum = maxNum + i;
                ParkingSpot spot = new ParkingSpot();
                spot.setSpotNumber(String.format("%s%03d", area.charAt(0), spotNum));
                spot.setArea(area);
                spot.setStatus("FREE");
                parkingSpotRepository.save(spot);
            }
        } else if (targetCount < currentCount) {
            // 需要删除空闲的车位（不能删已占用的）
            int toRemove = currentCount - targetCount;
            List<ParkingSpot> freeSpots = existingSpots.stream()
                    .filter(s -> "FREE".equals(s.getStatus()))
                    .sorted((a, b) -> b.getSpotNumber().compareTo(a.getSpotNumber()))
                    .collect(Collectors.toList());

            int removed = 0;
            for (ParkingSpot spot : freeSpots) {
                if (removed >= toRemove) break;
                parkingSpotRepository.delete(spot);
                removed++;
            }
        }
    }

    /**
     * 获取单个配置值
     */
    public String getConfigValue(String key) {
        return systemConfigRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(null);
    }

    /**
     * 获取配置值并转为 double
     */
    public double getConfigDouble(String key, double defaultValue) {
        String val = getConfigValue(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取配置值并转为 int
     */
    public int getConfigInt(String key, int defaultValue) {
        String val = getConfigValue(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}