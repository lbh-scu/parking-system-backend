package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.entity.Fee;
import com.smartparking.repository.FeeRepository;
import com.smartparking.service.FeeService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/fees")
public class FeeController {

    @Autowired
    private FeeService feeService;

    @Autowired
    private FeeRepository feeRepository;

    /**
     * 计算停车费用
     */
    @PostMapping("/calculate")
    public ApiResponse<Fee> calculateFee(@RequestParam String plateNumber) {
        try {
            Fee fee = feeService.calculateFee(plateNumber);
            return ApiResponse.success("费用计算成功", fee);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 支付费用
     */
    @PostMapping("/pay")
    public ApiResponse<Fee> payFee(@RequestParam Long feeId) {
        try {
            Fee fee = feeService.payFee(feeId);
            return ApiResponse.success("支付成功", fee);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取待结算车辆
     */
    @GetMapping("/pending")
    public ApiResponse<List<Fee>> getPendingFees() {
        List<Fee> pendingFees = feeService.getPendingFees();
        return ApiResponse.success(pendingFees);
    }

    /**
     * 今日已结算记录
     */
    @GetMapping("/today-records")
    public ApiResponse<List<Fee>> getTodayRecords() {
        List<Fee> records = feeService.getTodayPaidRecords();
        return ApiResponse.success(records);
    }

    /**
     * 收费统计（含今日统计和总体统计）
     */
    @GetMapping("/statistics")
    public ApiResponse<java.util.Map<String, Object>> getStatistics() {
        java.util.Map<String, Object> todayStats = feeService.getTodayStatistics();
        java.util.Map<String, Object> overallStats = feeService.getOverallStatistics();
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("todayRevenue", todayStats.get("todayRevenue"));
        result.put("todayOrderCount", todayStats.get("todayOrderCount"));
        result.put("todayPendingCount", todayStats.get("pendingCount"));
        result.put("totalRevenue", overallStats.get("totalRevenue"));
        result.put("totalCount", overallStats.get("totalCount"));
        result.put("paidCount", overallStats.get("paidCount"));
        result.put("totalPendingCount", overallStats.get("pendingCount"));
        return ApiResponse.success(result);
    }

    /**
     * 导出费用记录为 Excel（全部费用记录，不分页）
     */
    @GetMapping("/export")
    public void exportExcel(HttpServletResponse response) throws IOException {
        List<Fee> fees = feeRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("费用记录");

        // 表头
        String[] headers = {"ID", "车牌号", "入场时间", "出场时间", "停车时长(小时)",
                "费率(元/小时)", "总费用(元)", "状态", "支付时间", "创建时间"};
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据填充
        for (int i = 0; i < fees.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Fee f = fees.get(i);

            row.createCell(0).setCellValue(f.getId() != null ? f.getId().toString() : "");
            row.createCell(1).setCellValue(f.getPlateNumber() != null ? f.getPlateNumber() : "");
            row.createCell(2).setCellValue(f.getEntryTime() != null ? f.getEntryTime().toString() : "");
            row.createCell(3).setCellValue(f.getExitTime() != null ? f.getExitTime().toString() : "");
            row.createCell(4).setCellValue(f.getParkingHours() != null ? f.getParkingHours().toString() : "");
            row.createCell(5).setCellValue(f.getHourlyRate() != null ? f.getHourlyRate().toString() : "");
            row.createCell(6).setCellValue(f.getTotalAmount() != null ? f.getTotalAmount().toString() : "");
            row.createCell(7).setCellValue(f.getStatus() != null ? f.getStatus() : "");
            row.createCell(8).setCellValue(f.getPaymentTime() != null ? f.getPaymentTime().toString() : "");
            row.createCell(9).setCellValue(f.getCreatedAt() != null ? f.getCreatedAt().toString() : "");
        }

        // 设置列宽为 10
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 10 * 256);
        }

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("费用记录.xlsx", StandardCharsets.UTF_8)
                        .replaceAll("\\+", "%20"));

        workbook.write(response.getOutputStream());
        response.getOutputStream().flush();
        workbook.close();
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        return style;
    }
}