package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.entity.Vehicle;
import com.smartparking.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

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
}
