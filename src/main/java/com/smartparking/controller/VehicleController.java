package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.entity.Vehicle;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.service.VehicleService;
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
@RequestMapping("/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    /**
     * 车辆入场
     */
    @PostMapping("/entry")
    public ApiResponse<Vehicle> vehicleEntry(
            @RequestParam String plateNumber,
            @RequestParam String spotNumber) {
        try {
            Vehicle vehicle = vehicleService.vehicleEntry(plateNumber, spotNumber);
            return ApiResponse.success("车辆入场成功", vehicle);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 车辆出场
     */
    @PostMapping("/exit")
    public ApiResponse<Vehicle> vehicleExit(@RequestParam String plateNumber) {
        try {
            Vehicle vehicle = vehicleService.vehicleExit(plateNumber);
            return ApiResponse.success("车辆出场成功", vehicle);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取在场车辆列表
     */
    @GetMapping("/parking")
    public ApiResponse<List<Vehicle>> getParkingVehicles() {
        List<Vehicle> vehicles = vehicleService.getParkingVehicles();
        return ApiResponse.success(vehicles);
    }

    /**
     * 获取历史记录
     */
    @GetMapping("/history")
    public ApiResponse<List<Vehicle>> getVehicleHistory(
            @RequestParam(required = false) String plateNumber) {
        List<Vehicle> history = vehicleService.getVehicleHistory(plateNumber);
        return ApiResponse.success(history);
    }

    /**
     * 导出车辆记录为 Excel（全部记录，不分页）
     */
    @GetMapping("/export")
    public void exportExcel(HttpServletResponse response) throws IOException {
        List<Vehicle> vehicles = vehicleRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("车辆记录");

        // 表头
        String[] headers = {"ID", "车牌号", "车位号", "入场时间", "出场时间", "是否为居民", "状态", "创建时间", "更新时间"};
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 数据填充
        for (int i = 0; i < vehicles.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Vehicle v = vehicles.get(i);

            row.createCell(0).setCellValue(v.getId() != null ? v.getId().toString() : "");
            row.createCell(1).setCellValue(v.getPlateNumber() != null ? v.getPlateNumber() : "");
            row.createCell(2).setCellValue(v.getSpotNumber() != null ? v.getSpotNumber() : "");
            row.createCell(3).setCellValue(v.getEntryTime() != null ? v.getEntryTime().toString() : "");
            row.createCell(4).setCellValue(v.getExitTime() != null ? v.getExitTime().toString() : "");
            row.createCell(5).setCellValue(v.getIsResident() != null ? v.getIsResident().toString() : "");      // 是否为居民
            row.createCell(6).setCellValue(v.getStatus() != null ? v.getStatus() : "");
            row.createCell(7).setCellValue(v.getCreatedAt() != null ? v.getCreatedAt().toString() : "");
            row.createCell(8).setCellValue(v.getUpdatedAt() != null ? v.getUpdatedAt().toString() : "");
        }

        // 设置列宽为 10
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 10 * 256);
        }

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("车辆记录.xlsx", StandardCharsets.UTF_8)
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