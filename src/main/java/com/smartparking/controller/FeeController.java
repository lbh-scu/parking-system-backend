package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.entity.Fee;
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
     * 收费统计
     */
    @GetMapping("/statistics")
    public ApiResponse<BigDecimal> getStatistics() {
        BigDecimal totalRevenue = feeService.getTotalRevenue();
        return ApiResponse.success(totalRevenue);
    }

    /**
     * 导出费用记录为 Excel（全部记录，不分页）
     */
    @GetMapping("/export")
    public void exportExcel(HttpServletResponse response) throws IOException {
        List<Fee> fees = feeService.getPendingFees();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("费用记录");

        // 表头
        String[] headers = {"ID", "车牌号", "入场时间", "出场时间", "停车时长(小时)", "费率(元/小时)", "总费用(元)", "状态", "支付时间", "创建时间"};
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据
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

        // 自适应列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 写入响应
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("费用记录.xlsx", StandardCharsets.UTF_8));
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}