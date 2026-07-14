package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import com.smartparking.entity.Fee;
import com.smartparking.service.FeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
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
}
