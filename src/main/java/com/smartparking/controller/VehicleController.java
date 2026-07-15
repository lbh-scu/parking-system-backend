package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.entity.Vehicle;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.service.ExcelCommonService;
import com.smartparking.service.VehicleService;
import com.smartparking.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private ExcelCommonService excelCommonService;

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
     * 下载车辆Excel导入模板
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws Exception {
        String templatePath = "excel-template/vehicle_template.xlsx";
        byte[] bytes = ExcelUtil.getTemplateFile(templatePath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=车辆信息导入模板.xlsx")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(bytes);
    }

    /**
     * 批量导入车辆Excel
     */
    @PostMapping("/import")
    public ApiResponse<String> importExcel(@RequestParam MultipartFile file) {
        try {
            int count = excelCommonService.importData(file, Vehicle.class, vehicleRepository);
            return ApiResponse.success("车辆导入成功，共" + count + "条数据", null);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 导出全部车辆数据Excel（浏览器下载）
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() throws Exception {
        byte[] bytes = excelCommonService.exportAllData(vehicleRepository, Vehicle.class);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=车辆信息数据导出.xlsx")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(bytes);
    }
}
