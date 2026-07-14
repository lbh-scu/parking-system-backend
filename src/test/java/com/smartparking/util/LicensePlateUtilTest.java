package com.smartparking.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 车牌号校验工具类单元测试
 */
class LicensePlateUtilTest {

    @Test
    void testValidFuelPlate() {
        // 普通燃油车：1省份简称 + 1字母 + 5位字母数字
        assertTrue(LicensePlateUtil.isValid("京A12345"));
        assertTrue(LicensePlateUtil.isValid("沪B88888"));
        assertTrue(LicensePlateUtil.isValid("粤C66666"));
        assertTrue(LicensePlateUtil.isValid("苏D1234E"));
        assertTrue(LicensePlateUtil.isValid("川A99999"));
    }

    @Test
    void testValidNewEnergyPlate() {
        // 新能源：6位纯数字
        assertTrue(LicensePlateUtil.isValid("京AD12345"));
        // 新能源：D/F结尾
        assertTrue(LicensePlateUtil.isValid("粤BD12345D"));
        assertTrue(LicensePlateUtil.isValid("沪AF12345F"));
    }

    @Test
    void testInvalidPlate() {
        // 空值
        assertFalse(LicensePlateUtil.isValid(null));
        assertFalse(LicensePlateUtil.isValid(""));
        assertFalse(LicensePlateUtil.isValid("   "));

        // 格式错误
        assertFalse(LicensePlateUtil.isValid("京A1234"));        // 太短
        assertFalse(LicensePlateUtil.isValid("京A1234567"));     // 太长
        assertFalse(LicensePlateUtil.isValid("AB12345"));        // 无省份
        assertFalse(LicensePlateUtil.isValid("京012345"));       // 省份后不能是数字
        assertFalse(LicensePlateUtil.isValid("京A1234O"));       // 字母O
        assertFalse(LicensePlateUtil.isValid("京A1234I"));       // 字母I
        assertFalse(LicensePlateUtil.isValid("test123"));        // 非法省份
    }

    @Test
    void testValidateMessage() {
        assertNull(LicensePlateUtil.validate("京A12345"), "合法车牌应返回null");
        assertNotNull(LicensePlateUtil.validate(null), "空值应返回错误信息");
        assertNotNull(LicensePlateUtil.validate(""), "空串应返回错误信息");
        assertTrue(LicensePlateUtil.validate("AB12345").contains("不合法"));
    }

    @Test
    void testFormat() {
        assertEquals("京A12345", LicensePlateUtil.format(" 京a12345 "));
        assertEquals("粤BD12345D", LicensePlateUtil.format("粤bd12345d"));
        assertNull(LicensePlateUtil.format(null));
    }
}
