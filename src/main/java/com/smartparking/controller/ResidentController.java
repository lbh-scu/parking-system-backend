package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.entity.Vehicle;
import com.smartparking.repository.VehicleRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/residents")
public class ResidentController {

    @Autowired
    private VehicleRepository vehicleRepository;

    /**
     * 获取所有住户（isResident = true）
     */
    @GetMapping
    public ApiResponse<List<Vehicle>> getResidents() {
        List<Vehicle> residents = vehicleRepository.findByIsResident(true);
        return ApiResponse.success(residents);
    }

    /**
     * 导出住户数据为 Excel（含所有住户，不分页）
     */
    @GetMapping("/export")
    public void exportExcel(HttpServletResponse response) throws IOException {
        List<Vehicle> residents = vehicleRepository.findByIsResident(true);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("住户信息");

        // 表头
        String[] headers = {"住户ID", "车牌号", "入场时间", "状态", "创建时间"};
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
        for (int i = 0; i < residents.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Vehicle v = residents.get(i);
            row.createCell(0).setCellValue(v.getId() != null ? v.getId().toString() : "");
            row.createCell(1).setCellValue(v.getPlateNumber() != null ? v.getPlateNumber() : "");
            row.createCell(2).setCellValue(v.getEntryTime() != null ? v.getEntryTime().toString() : "");
            row.createCell(3).setCellValue(v.getStatus() != null ? v.getStatus() : "");
            row.createCell(4).setCellValue(v.getCreatedAt() != null ? v.getCreatedAt().toString() : "");
        }

        // 自适应列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 写入响应
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("住户信息.xlsx", StandardCharsets.UTF_8));
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}