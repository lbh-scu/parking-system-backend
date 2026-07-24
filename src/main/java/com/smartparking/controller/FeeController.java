package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.entity.Fee;
import com.smartparking.repository.FeeRepository;
import com.smartparking.service.FeeService;
import com.smartparking.util.DateTimeUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ResponseEntity;

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
    public ResponseEntity<ApiResponse<Fee>> calculateFee(@RequestParam String plateNumber) {
        try {
            Fee fee = feeService.calculateFee(plateNumber);
            return ResponseEntity.ok(ApiResponse.success("费用计算成功", fee));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 支付费用
     */
    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<Fee>> payFee(@RequestParam Long feeId) {
        try {
            Fee fee = feeService.payFee(feeId);
            return ResponseEntity.ok(ApiResponse.success("支付成功", fee));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
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
        // 导出今日已结算记录
        List<Fee> fees = feeService.getTodayPaidRecords();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("今日收费记录");

        // 表头
        String[] headers = {"ID", "车牌号", "入场时间", "出场时间", "停车时长(小时)",
                "小时单价", "总费用", "状态", "支付时间", "创建时间"};
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 创建日期样式yyyy-MM-dd HH:mm:ss
        CellStyle standardDateTimeStyle = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        standardDateTimeStyle.setDataFormat(dataFormat.getFormat(DateTimeUtil.DATETIME_PATTERN));

        // 数据填充
        for (int i = 0; i < fees.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Fee f = fees.get(i);

            row.createCell(0).setCellValue(f.getId() != null ? f.getId().toString() : "");
            row.createCell(1).setCellValue(f.getPlateNumber() != null ? f.getPlateNumber() : "");

            Cell entryTimeCell = row.createCell(2);
            if (f.getEntryTime() != null) {
                entryTimeCell.setCellValue(f.getEntryTime()); // 直接写入LocalDateTime，POI自动转Excel日期序列号
                entryTimeCell.setCellStyle(standardDateTimeStyle); // 绑定自定义格式，和手动设置完全一致
            }

            Cell exitTimeCell = row.createCell(3);
            if (f.getExitTime() != null) {
                exitTimeCell.setCellValue(f.getExitTime());
                exitTimeCell.setCellStyle(standardDateTimeStyle);
            }

            row.createCell(4).setCellValue(f.getParkingHours() != null ? f.getParkingHours().toString() : "");
            row.createCell(5).setCellValue(f.getHourlyRate() != null ? f.getHourlyRate().toString() : "");
            row.createCell(6).setCellValue(f.getTotalAmount() != null ? f.getTotalAmount().toString() : "");
            row.createCell(7).setCellValue(f.getStatus() != null ? f.getStatus() : "");

            Cell payTimeCell = row.createCell(8);
            if (f.getPaymentTime() != null) {
                payTimeCell.setCellValue(f.getPaymentTime());
                payTimeCell.setCellStyle(standardDateTimeStyle);
            }

            Cell createTimeCell = row.createCell(9);
            if (f.getCreatedAt() != null) {
                createTimeCell.setCellValue(f.getCreatedAt());
                createTimeCell.setCellStyle(standardDateTimeStyle);
            }
        }

        // 设置列宽为 20
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 20 * 256);
        }

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("今日收费记录.xlsx", StandardCharsets.UTF_8)
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