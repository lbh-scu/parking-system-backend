package com.smartparking.service;

import com.smartparking.entity.*;
import com.smartparking.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 全量数据备份服务
 * 将系统中所有数据表导出为单个 Excel 文件（每个数据表一个 Sheet）
 */
@Service
public class BackupService {

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private ResidentRepository residentRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private SystemLogRepository systemLogRepository;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成全量备份 Excel
     * Sheet1: 收费记录
     * Sheet2: 住户信息
     * Sheet3: 车辆记录
     * Sheet4: 车位数据
     * Sheet5: 系统配置
     * Sheet6: 操作日志
     */
    public byte[] exportFullBackup() {
        try (Workbook workbook = new XSSFWorkbook()) {
            // ---------- 通用样式 ----------
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // ==================== Sheet1: 收费记录 ====================
            createFeeSheet(workbook, headerStyle, dataStyle);

            // ==================== Sheet2: 住户信息 ====================
            createResidentSheet(workbook, headerStyle, dataStyle);

            // ==================== Sheet3: 车辆记录 ====================
            createVehicleSheet(workbook, headerStyle, dataStyle);

            // ==================== Sheet4: 车位数据 ====================
            createParkingSpotSheet(workbook, headerStyle, dataStyle);

            // ==================== Sheet5: 系统配置 ====================
            createSystemConfigSheet(workbook, headerStyle, dataStyle);

            // ==================== Sheet6: 操作日志 ====================
            createSystemLogSheet(workbook, headerStyle, dataStyle);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("全量备份失败: " + e.getMessage());
        }
    }

    // ==================== Sheet 创建方法 ====================

    private void createFeeSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle) {
        String[] headers = {"ID", "车牌号", "入场时间", "出场时间", "停车时长(小时)",
                "小时费率(元)", "总费用(元)", "状态", "支付时间", "创建时间"};
        Sheet sheet = workbook.createSheet("收费记录");
        createHeaderRow(sheet, headers, headerStyle);

        List<Fee> fees = feeRepository.findAll();
        int rowIdx = 1;
        for (Fee f : fees) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(f.getId() != null ? f.getId().doubleValue() : 0);
            row.createCell(1).setCellValue(nullToEmpty(f.getPlateNumber()));
            row.createCell(2).setCellValue(f.getEntryTime() != null ? f.getEntryTime().format(DT_FMT) : "");
            row.createCell(3).setCellValue(f.getExitTime() != null ? f.getExitTime().format(DT_FMT) : "");
            row.createCell(4).setCellValue(f.getParkingHours() != null ? f.getParkingHours().doubleValue() : 0);
            row.createCell(5).setCellValue(f.getHourlyRate() != null ? f.getHourlyRate().doubleValue() : 0);
            row.createCell(6).setCellValue(f.getTotalAmount() != null ? f.getTotalAmount().doubleValue() : 0);
            row.createCell(7).setCellValue("PAID".equals(f.getStatus()) ? "已支付" : "待支付");
            row.createCell(8).setCellValue(f.getPaymentTime() != null ? f.getPaymentTime().format(DT_FMT) : "");
            row.createCell(9).setCellValue(f.getCreatedAt() != null ? f.getCreatedAt().format(DT_FMT) : "");
            applyDataStyle(row, headers.length, dataStyle);
        }
        autoSizeColumns(sheet, headers.length);
    }

    private void createResidentSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle) {
        String[] headers = {"ID", "用户名", "车牌号"};
        Sheet sheet = workbook.createSheet("住户信息");
        createHeaderRow(sheet, headers, headerStyle);

        List<Resident> residents = residentRepository.findAll();
        int rowIdx = 1;
        for (Resident r : residents) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(r.getId() != null ? r.getId().doubleValue() : 0);
            row.createCell(1).setCellValue(nullToEmpty(r.getUserName()));
            row.createCell(2).setCellValue(nullToEmpty(r.getPlateNumber()));
            applyDataStyle(row, headers.length, dataStyle);
        }
        autoSizeColumns(sheet, headers.length);
    }

    private void createVehicleSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle) {
        String[] headers = {"ID", "车牌号", "车位ID", "车位号", "入场时间", "出场时间",
                "是否为居民", "状态", "创建时间", "更新时间"};
        Sheet sheet = workbook.createSheet("车辆记录");
        createHeaderRow(sheet, headers, headerStyle);

        List<Vehicle> vehicles = vehicleRepository.findAll();
        int rowIdx = 1;
        for (Vehicle v : vehicles) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(v.getId() != null ? v.getId().doubleValue() : 0);
            row.createCell(1).setCellValue(nullToEmpty(v.getPlateNumber()));
            row.createCell(2).setCellValue(v.getSpotId() != null ? v.getSpotId().doubleValue() : 0);
            row.createCell(3).setCellValue(nullToEmpty(v.getSpotNumber()));
            row.createCell(4).setCellValue(v.getEntryTime() != null ? v.getEntryTime().format(DT_FMT) : "");
            row.createCell(5).setCellValue(v.getExitTime() != null ? v.getExitTime().format(DT_FMT) : "");
            row.createCell(6).setCellValue(Boolean.TRUE.equals(v.getIsResident()) ? "是" : "否");
            row.createCell(7).setCellValue("PARKING".equals(v.getStatus()) ? "在场" : "已离场");
            row.createCell(8).setCellValue(v.getCreatedAt() != null ? v.getCreatedAt().format(DT_FMT) : "");
            row.createCell(9).setCellValue(v.getUpdatedAt() != null ? v.getUpdatedAt().format(DT_FMT) : "");
            applyDataStyle(row, headers.length, dataStyle);
        }
        autoSizeColumns(sheet, headers.length);
    }

    private void createParkingSpotSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle) {
        String[] headers = {"ID", "车位编号", "区域", "状态", "当前车牌", "创建时间", "更新时间"};
        Sheet sheet = workbook.createSheet("车位数据");
        createHeaderRow(sheet, headers, headerStyle);

        List<ParkingSpot> spots = parkingSpotRepository.findAll();
        int rowIdx = 1;
        for (ParkingSpot s : spots) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(s.getId() != null ? s.getId().doubleValue() : 0);
            row.createCell(1).setCellValue(nullToEmpty(s.getSpotNumber()));
            row.createCell(2).setCellValue(nullToEmpty(s.getArea()));
            row.createCell(3).setCellValue("FREE".equals(s.getStatus()) ? "空闲" :
                    "OCCUPIED".equals(s.getStatus()) ? "已占用" : s.getStatus());
            row.createCell(4).setCellValue(nullToEmpty(s.getCurrentPlate()));
            row.createCell(5).setCellValue(s.getCreatedAt() != null ? s.getCreatedAt().format(DT_FMT) : "");
            row.createCell(6).setCellValue(s.getUpdatedAt() != null ? s.getUpdatedAt().format(DT_FMT) : "");
            applyDataStyle(row, headers.length, dataStyle);
        }
        autoSizeColumns(sheet, headers.length);
    }

    private void createSystemConfigSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle) {
        String[] headers = {"ID", "配置键", "配置值", "描述", "更新时间"};
        Sheet sheet = workbook.createSheet("系统配置");
        createHeaderRow(sheet, headers, headerStyle);

        List<SystemConfig> configs = systemConfigRepository.findAll();
        int rowIdx = 1;
        for (SystemConfig c : configs) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(c.getId() != null ? c.getId().doubleValue() : 0);
            row.createCell(1).setCellValue(nullToEmpty(c.getConfigKey()));
            row.createCell(2).setCellValue(nullToEmpty(c.getConfigValue()));
            row.createCell(3).setCellValue(nullToEmpty(c.getDescription()));
            row.createCell(4).setCellValue(c.getUpdatedAt() != null ? c.getUpdatedAt().format(DT_FMT) : "");
            applyDataStyle(row, headers.length, dataStyle);
        }
        autoSizeColumns(sheet, headers.length);
    }

    private void createSystemLogSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle) {
        String[] headers = {"ID", "操作类型", "操作描述", "车牌号", "操作人", "结果", "操作时间"};
        Sheet sheet = workbook.createSheet("操作日志");
        createHeaderRow(sheet, headers, headerStyle);

        List<SystemLog> logs = systemLogRepository.findTop100ByOrderByCreatedAtDesc();
        int rowIdx = 1;
        for (SystemLog log : logs) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(log.getId() != null ? log.getId().doubleValue() : 0);
            row.createCell(1).setCellValue(nullToEmpty(log.getActionType()));
            row.createCell(2).setCellValue(nullToEmpty(log.getMessage()));
            row.createCell(3).setCellValue(nullToEmpty(log.getPlateNumber()));
            row.createCell(4).setCellValue(nullToEmpty(log.getOperator()));
            row.createCell(5).setCellValue("SUCCESS".equals(log.getResult()) ? "成功" :
                    "FAILURE".equals(log.getResult()) ? "失败" : log.getResult());
            row.createCell(6).setCellValue(log.getCreatedAt() != null ? log.getCreatedAt().format(DT_FMT) : "");
            applyDataStyle(row, headers.length, dataStyle);
        }
        autoSizeColumns(sheet, headers.length);
    }

    // ==================== 辅助方法 ====================

    private void createHeaderRow(Sheet sheet, String[] headers, CellStyle style) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void applyDataStyle(Row row, int colCount, CellStyle style) {
        for (int i = 0; i < colCount; i++) {
            Cell cell = row.getCell(i);
            if (cell != null) {
                cell.setCellStyle(style);
            }
        }
    }

    private void autoSizeColumns(Sheet sheet, int colCount) {
        for (int i = 0; i < colCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}