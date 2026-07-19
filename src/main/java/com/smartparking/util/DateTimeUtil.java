package com.smartparking.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 时间工具类，统一格式化规则
 */
public class DateTimeUtil {

    // 固定标准日期时间格式
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    // 全局复用格式化器，仅用于 LocalDateTime 与字符串互转
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    // 获取当前时间，自动截断至秒仅生成时间对象(不涉及字符串转换)
    public static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    // LocalDateTime 对象 转为 标准格式字符串 yyyy-MM-dd HH:mm:ss
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return DATETIME_FORMATTER.format(dateTime);
    }

    //标准格式字符串 yyyy-MM-dd HH:mm:ss 解析为 LocalDateTime 对象 ,使用 DATETIME_FORMATTER 完成解析，适配Excel导入
    public static LocalDateTime parse(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr.trim(), DATETIME_FORMATTER);
    }
}