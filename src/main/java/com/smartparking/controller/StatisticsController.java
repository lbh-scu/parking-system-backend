package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 今日/本周/本月营收概览
     */
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getSummary() {
        return ApiResponse.success(statisticsService.getTodaySummary());
    }

    /**
     * 24小时高峰时段统计（入场/出场频次）
     */
    @GetMapping("/peak-hours")
    public ApiResponse<List<Map<String, Object>>> getPeakHours() {
        return ApiResponse.success(statisticsService.getPeakHours());
    }

    /**
     * 车位占用率趋势
     * @param days 最近天数，默认7天
     */
    @GetMapping("/trend")
    public ApiResponse<List<Map<String, Object>>> getTrend(
            @RequestParam(value = "days", defaultValue = "7") int days) {
        return ApiResponse.success(statisticsService.getTrend(days));
    }

    /**
     * AI占用率预测
     * @param hours 预测未来多少小时，默认12小时
     */
    @GetMapping("/ai-prediction")
    public ApiResponse<Map<String, Object>> getAiPrediction(
            @RequestParam(value = "hours", defaultValue = "12") int hours) {
        return ApiResponse.success(statisticsService.getAiPrediction(hours));
    }

    /**
     * 收费趋势
     * @param period 统计周期：day / week / month
     */
    @GetMapping("/revenue-trend")
    public ApiResponse<List<Map<String, Object>>> getRevenueTrend(
            @RequestParam(value = "period", defaultValue = "day") String period) {
        return ApiResponse.success(statisticsService.getRevenueTrend(period));
    }

    /**
     * 导出Excel统计报表
     * 包含三个Sheet：收费明细、每日汇总、营业概况
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport() {
        byte[] data = statisticsService.exportReport();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export_report.xlsx\"");
        return ResponseEntity.ok().headers(headers).body(data);
    }
}