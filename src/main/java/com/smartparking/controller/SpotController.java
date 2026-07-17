package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.entity.ParkingSpot;
import com.smartparking.service.ParkingSpotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/spots")
public class SpotController {

    @Autowired
    private ParkingSpotService parkingSpotService;

    /**
     * 获取所有车位列表
     */
    @GetMapping
    public ApiResponse<List<ParkingSpot>> getAllSpots() {
        List<ParkingSpot> spots = parkingSpotService.findAll();
        return ApiResponse.success(spots);
    }

    /**
     * 获取空闲车位列表
     */
    @GetMapping("/free")
    public ApiResponse<List<ParkingSpot>> getFreeSpots() {
        return ApiResponse.success(parkingSpotService.findFreeSpots());
    }

    /**
     * 分配车位
     */
    @PostMapping("/assign")
    public ApiResponse<ParkingSpot> assignSpot(@RequestParam String plateNumber) {
        try {
            ParkingSpot spot = parkingSpotService.assignSpot(plateNumber);
            return ApiResponse.success("车位分配成功", spot);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 释放车位
     */
    @PostMapping("/release")
    public ApiResponse<ParkingSpot> releaseSpot(@RequestParam String spotNumber) {
        try {
            ParkingSpot spot = parkingSpotService.releaseSpot(spotNumber);
            return ApiResponse.success("车位释放成功", spot);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取热力图数据（按区域+楼层统计占用率）
     */
    @GetMapping("/heatmap")
    public ApiResponse<List<Map<String, Object>>> getHeatmap() {
        return ApiResponse.success(parkingSpotService.getHeatmapData());
    }

    /**
     * 获取区域占用率对比
     */
    @GetMapping("/area-compare")
    public ApiResponse<List<Map<String, Object>>> getAreaCompare() {
        return ApiResponse.success(parkingSpotService.getAreaCompareData());
    }

    /**
     * 获取总体占用率
     */
    @GetMapping("/occupancy-rate")
    public ApiResponse<Map<String, Object>> getOccupancyRate() {
        return ApiResponse.success(parkingSpotService.getOccupancyRate());
    }
}
