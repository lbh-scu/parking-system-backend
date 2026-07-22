package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.service.BackupService;
import com.smartparking.service.SystemConfigService;
import com.smartparking.service.SystemLogService;
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
    private BackupService backupService;

    @Autowired
    private SystemLogService systemLogService;

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
    public ApiResponse<List<String>> getLogs(@RequestParam(defaultValue = "100") int lines) {
        List<String> logs = systemLogService.getRecentLogsFormatted(lines);
        return ApiResponse.success(logs);
    }

    /** 一键全量备份 — 导出所有数据表为Excel文件
     *  包含：收费记录、住户信息、车辆记录、车位数据、系统配置、操作日志 */
    @GetMapping("/backup")
    public ResponseEntity<byte[]> backup() {
        byte[] data = backupService.exportFullBackup();
        String fn = "全量数据备份_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        h.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"");
        return ResponseEntity.ok().headers(h).body(data);
    }
}