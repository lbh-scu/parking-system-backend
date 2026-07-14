package com.smartparking.util;

import java.util.regex.Pattern;

/**
 * 车牌号格式校验工具类
 * 支持大陆标准车牌格式校验
 */
public class LicensePlateUtil {

    private LicensePlateUtil() {}

    /** 省份简称 */
    private static final String PROVINCES = "京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤川青藏琼宁";

    /** 普通燃油车牌：1省份简称 + 1字母 + 5位字母数字 */
    private static final Pattern PATTERN_FUEL = Pattern.compile(
            "^[" + PROVINCES + "][A-HJ-NP-Z][A-HJ-NP-Z0-9]{5}$");

    /** 新能源车牌：1省份简称 + 1字母 + 6位字母数字（含D/F区分） */
    private static final Pattern PATTERN_NEW_ENERGY = Pattern.compile(
            "^[" + PROVINCES + "][A-HJ-NP-Z][A-HJ-NP-Z0-9]{4}[DDF]$|^[" + PROVINCES + "][A-HJ-NP-Z][DDF][A-HJ-NP-Z0-9]{5}$");

    /** 新能源6位（纯数字车牌） */
    private static final Pattern PATTERN_NEW_ENERGY_6 = Pattern.compile(
            "^[" + PROVINCES + "][A-HJ-NP-Z][0-9]{6}$");

    /**
     * 校验车牌号是否合法
     *
     * @param plateNumber 车牌号
     * @return true=合法, false=非法
     */
    public static boolean isValid(String plateNumber) {
        if (plateNumber == null || plateNumber.isBlank()) {
            return false;
        }
        String plate = plateNumber.trim().toUpperCase();
        return PATTERN_FUEL.matcher(plate).matches()
                || PATTERN_NEW_ENERGY.matcher(plate).matches()
                || PATTERN_NEW_ENERGY_6.matcher(plate).matches();
    }

    /**
     * 校验并返回错误信息
     *
     * @param plateNumber 车牌号
     * @return null=合法, 非null=错误描述
     */
    public static String validate(String plateNumber) {
        if (plateNumber == null || plateNumber.isBlank()) {
            return "车牌号不能为空";
        }
        String plate = plateNumber.trim().toUpperCase();
        if (plate.length() < 7 || plate.length() > 8) {
            return "车牌号长度不合法（应为7-8位）";
        }
        if (!isValid(plate)) {
            return "车牌号格式不合法（示例：京A12345、京AD12345）";
        }
        return null;
    }

    /**
     * 格式化车牌号：去除空格、转大写
     *
     * @param plateNumber 原始输入
     * @return 格式化后的车牌号
     */
    public static String format(String plateNumber) {
        if (plateNumber == null) {
            return null;
        }
        return plateNumber.trim().toUpperCase();
    }
}
