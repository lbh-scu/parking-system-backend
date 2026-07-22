package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.service.SystemConfigService;
import com.smartparking.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system")
public class SystemController {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private DataSource dataSource;

    @GetMapping("/config")
    public ApiResponse<?> getConfig() {
        return ApiResponse.success(systemConfigService.getAllConfigs());
    }

    @PostMapping("/config/update")
    public ApiResponse<?> updateConfig(@RequestParam String configKey, @RequestParam String configValue) {
        systemConfigService.updateConfig(configKey, configValue);
        return ApiResponse.success("配置更新成功");
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now().toString());
        Map<String, Object> db = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            db.put("status", conn.isValid(3) ? "UP" : "DOWN");
            db.put("database", conn.getMetaData().getDatabaseProductName());
        } catch (Exception e) { db.put("status", "DOWN"); db.put("error", e.getMessage()); }
        status.put("database", db);
        status.put("overall", "UP".equals(db.get("status")) ? "UP" : "DEGRADED");
        return ApiResponse.success(status);
    }

    @GetMapping("/logs")
    public ApiResponse<List<String>> getLogs() {
        return ApiResponse.success(List.of("日志功能请在服务端查看"));
    }

    /** 一键备份 — 复用统计报表导出，生成Excel文件 */
    @GetMapping("/backup")
    public ResponseEntity<byte[]> backup() {
        byte[] data = statisticsService.exportReport();
        String fn = "数据备份_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        h.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"");
        return ResponseEntity.ok().headers(h).body(data);
    }
}