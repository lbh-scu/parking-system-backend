package com.smartparking.service;

import com.smartparking.entity.Fee;
import com.smartparking.entity.Vehicle;
import com.smartparking.repository.FeeRepository;
import com.smartparking.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeeService {

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    // 每小时费率
    private static final BigDecimal HOURLY_RATE = new BigDecimal("5.00");

    /**
     * 计算停车费用
     */
    public Fee calculateFee(String plateNumber) {
        Vehicle vehicle = vehicleRepository.findByPlateNumberAndStatus(plateNumber, "PARKING")
                .orElseThrow(() -> new RuntimeException("未找到该车辆的入场记录"));

        LocalDateTime entryTime = vehicle.getEntryTime();
        LocalDateTime exitTime = LocalDateTime.now();

        // 计算停车时长（小时）
        Duration duration = Duration.between(entryTime, exitTime);
        long minutes = duration.toMinutes();
        double hours = minutes / 60.0;

        // 计算费用（最少按1小时计费）
        BigDecimal totalAmount = HOURLY_RATE.multiply(BigDecimal.valueOf(Math.max(1, Math.ceil(hours))));

        Fee fee = new Fee();
        fee.setPlateNumber(plateNumber);
        fee.setEntryTime(entryTime);
        fee.setExitTime(exitTime);
        fee.setParkingHours(hours);
        fee.setHourlyRate(HOURLY_RATE);
        fee.setTotalAmount(totalAmount);
        fee.setStatus("PENDING");

        return feeRepository.save(fee);
    }

    /**
     * 支付费用
     */
    public Fee payFee(Long feeId) {
        Fee fee = feeRepository.findById(feeId)
                .orElseThrow(() -> new RuntimeException("费用记录不存在"));

        fee.setStatus("PAID");
        fee.setPaymentTime(LocalDateTime.now());

        return feeRepository.save(fee);
    }

    /**
     * 获取待支付费用
     */
    public List<Fee> getPendingFees() {
        return feeRepository.findByStatus("PENDING");
    }

    /**
     * 获取收费统计
     */
    public BigDecimal getTotalRevenue() {
        List<Fee> paidFees = feeRepository.findByStatus("PAID");
        return paidFees.stream()
                .map(Fee::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
