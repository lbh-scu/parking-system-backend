package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.dto.ResidentDTO;
import com.smartparking.entity.Resident;
import com.smartparking.repository.ResidentRepository;
import com.smartparking.service.ExcelCommonService;
import com.smartparking.service.ResidentService;
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
    private ResidentService residentService;

    @Autowired
    private ResidentRepository residentRepository;

    @Autowired
    private ExcelCommonService  excelCommonService;

    /**
     * 获取所有住户
     */
    @GetMapping
    public ApiResponse<List<Resident>> getResidents() {
        List<Resident> residents = residentRepository.findAll();
        return ApiResponse.success(residents);
    }

    @PostMapping("/resident/add")
    public ApiResponse<Void> addResident(@RequestBody ResidentDTO dto) {
        residentService.addResident(dto);
        return ApiResponse.success();
    }

    /**
     * 导出住户数据为 Excel（含所有住户，不分页）
     */
    @GetMapping("/export")
    public void exportExcel(HttpServletResponse response) throws IOException {
        List<Resident> residents = residentRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("住户信息");

        // 表头
        String[] headers = {"ID", "用户名", "车牌号"};
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据填充
        for (int i = 0; i < residents.size(); i++) {
            Row row = sheet.createRow(i + 1);
            var r = residents.get(i);

            row.createCell(0).setCellValue(r.getId() != null ? r.getId().toString() : "");
            row.createCell(1).setCellValue(r.getUserName() != null ? r.getUserName() : "");
            row.createCell(2).setCellValue(r.getPlateNumber() != null ? r.getPlateNumber() : "");
        }

        // 设置列宽为 10
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 10 * 256);
        }

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("用户信息.xlsx", StandardCharsets.UTF_8)
                        .replaceAll("\\+", "%20"));

        workbook.write(response.getOutputStream());
        response.getOutputStream().flush();
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