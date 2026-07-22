package com.smartparking.service;

import com.smartparking.entity.SystemLog;
import com.smartparking.repository.SystemLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统操作日志服务
 * 提供日志记录和查询功能
 */
@Service
public class SystemLogService {

    @Autowired
    private SystemLogRepository systemLogRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 记录操作日志
     */
    public void log(String actionType, String message, String plateNumber, String operator, String result) {
        SystemLog log = new SystemLog(actionType, message, plateNumber, operator, result);
        systemLogRepository.save(log);
    }

    /**
     * 快速记录成功操作
     */
    public void logSuccess(String actionType, String message) {
        log(actionType, message, null, "系统", "SUCCESS");
    }

    /**
     * 快速记录失败操作
     */
    public void logFailure(String actionType, String message) {
        log(actionType, message, null, "系统", "FAILURE");
    }

    /**
     * 获取最近N条日志，按时间倒序
     */
    public List<SystemLog> getRecentLogs(int lines) {
        List<SystemLog> allLogs = systemLogRepository.findTop100ByOrderByCreatedAtDesc();
        return allLogs.stream()
                .limit(Math.min(lines, allLogs.size()))
                .collect(Collectors.toList());
    }

    /**
     * 获取最近N条日志，格式化为字符串列表
     * 格式：2026-07-22 14:30:00 [ENTRY] SUCCESS: 车辆京A12345入场 - 车位B1-001
     */
    public List<String> getRecentLogsFormatted(int lines) {
        return getRecentLogs(lines).stream()
                .map(this::formatLog)
                .collect(Collectors.toList());
    }

    /**
     * 将日志实体格式化为可读字符串
     */
    private String formatLog(SystemLog log) {
        String time = log.getCreatedAt() != null
                ? log.getCreatedAt().format(FORMATTER)
                : "未知时间";
        String actionType = log.getActionType() != null ? log.getActionType() : "SYSTEM";
        String result = log.getResult() != null ? log.getResult() : "UNKNOWN";
        String operator = log.getOperator() != null ? log.getOperator() : "系统";
        String message = log.getMessage() != null ? log.getMessage() : "";

        return String.format("%s [%s] %s - %s (%s)", time, actionType, result, message, operator);
    }
}