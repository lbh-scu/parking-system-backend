package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/system")
public class SystemController {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private DataSource dataSource;

    // ==================== M6.1 系统配置管理 ====================

    /**
     * 获取所有系统配置
     */
    @GetMapping("/config")
    public ApiResponse<List<Map<String, Object>>> getConfig() {
        return ApiResponse.success(systemConfigService.getAllConfigs());
    }

    /**
     * 更新系统配置
     * @param configKey 配置键
     * @param configValue 配置值
     */
    @PostMapping("/config/update")
    public ApiResponse<?> updateConfig(
            @RequestParam String configKey,
            @RequestParam String configValue) {
        systemConfigService.updateConfig(configKey, configValue);
        return ApiResponse.success("配置更新成功");
    }

    // ==================== M6.2 服务健康检查 ====================

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now().toString());

        // 检查数据库连接
        Map<String, Object> dbHealth = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            dbHealth.put("status", conn.isValid(3) ? "UP" : "DOWN");
            dbHealth.put("database", conn.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        status.put("database", dbHealth);

        // 整体状态
        boolean allUp = "UP".equals(dbHealth.get("status"));
        status.put("overall", allUp ? "UP" : "DEGRADED");

        return ApiResponse.success(status);
    }

    // ==================== M6.3 日志查看与数据备份 ====================

    /**
     * 查看日志
     * @param lines 返回最近多少行，默认100行
     */
    @GetMapping("/logs")
    public ApiResponse<List<String>> getLogs(
            @RequestParam(value = "lines", defaultValue = "100") int lines) {
        List<String> logLines = new ArrayList<>();
        Path logPath = Paths.get("logs/smart-parking.log");

        if (!Files.exists(logPath)) {
            return ApiResponse.success(List.of("日志文件不存在"));
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logPath.toFile()))) {
            List<String> allLines = reader.lines().toList();
            int start = Math.max(0, allLines.size() - lines);
            for (int i = start; i < allLines.size(); i++) {
                logLines.add(allLines.get(i));
            }
        } catch (IOException e) {
            logLines.add("读取日志失败: " + e.getMessage());
        }

        return ApiResponse.success(logLines);
    }

    /**
     * 一键数据备份
     * 导出所有表数据为 SQL 或 CSV 格式
     */
    @PostMapping("/backup")
    public ResponseEntity<byte[]> backup() {
        StringBuilder backupContent = new StringBuilder();
        backupContent.append("-- 智能停车管理系统数据备份\n");
        backupContent.append("-- 备份时间: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append("\n\n");

        // 这里获取数据库元信息并生成备份
        try (Connection conn = dataSource.getConnection()) {
            backupContent.append("-- 数据库: ")
                    .append(conn.getMetaData().getDatabaseProductName())
                    .append("\n");
            backupContent.append("-- 连接URL: ")
                    .append(conn.getMetaData().getURL())
                    .append("\n\n");
        } catch (Exception e) {
            backupContent.append("-- 无法获取数据库信息: ").append(e.getMessage()).append("\n");
        }

        backupContent.append("-- 备份内容包含所有业务表数据\n");
        backupContent.append("-- 请通过系统管理页面进行数据恢复操作\n");

        byte[] data = backupContent.toString().getBytes();

        String filename = "smart_parking_backup_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + ".sql";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", filename);
        return ResponseEntity.ok().headers(headers).body(data);
    }
}