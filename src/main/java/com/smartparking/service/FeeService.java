package com.smartparking.service;

import com.smartparking.entity.Fee;
import com.smartparking.entity.Vehicle;
import com.smartparking.repository.FeeRepository;
import com.smartparking.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

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
        // 优先查找已存在的 PENDING 费用记录（车辆出场时已自动创建）
        List<Fee> pendingFees = feeRepository.findByPlateNumberAndStatus(plateNumber, "PENDING");
        if (!pendingFees.isEmpty()) {
            return pendingFees.get(0);
        }

        // 如果没有 PENDING 记录，尝试从在场车辆计算（直接入场后点击计算）
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

    /**
     * 获取今日已结算记录
     */
    public List<Fee> getTodayPaidRecords() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        return feeRepository.findPaidRecordsBetween(todayStart, todayEnd);
    }

    /**
     * 获取今日统计：总订单数、总收入、待结算数
     */
    public Map<String, Object> getTodayStatistics() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        List<Fee> todayPaid = feeRepository.findPaidRecordsBetween(todayStart, todayEnd);
        BigDecimal totalRevenue = todayPaid.stream()
                .map(Fee::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Fee> todayAll = feeRepository.findAll().stream()
                .filter(f -> f.getCreatedAt() != null && !f.getCreatedAt().toLocalDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());
        long pendingCount = todayAll.stream().filter(f -> "PENDING".equals(f.getStatus())).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todayOrderCount", todayPaid.size());
        stats.put("todayRevenue", totalRevenue.doubleValue());
        stats.put("pendingCount", pendingCount);
        return stats;
    }

    /**
     * 获取全部（不限今日）的待结算列表
     */
    public List<Fee> getPendingRecords() {
        return feeRepository.findByStatus("PENDING");
    }

    /**
     * 批量查询结算统计
     */
    public Map<String, Object> getOverallStatistics() {
        List<Fee> all = feeRepository.findAll();
        List<Fee> paid = all.stream().filter(f -> "PAID".equals(f.getStatus())).collect(Collectors.toList());
        long pendingCount = all.stream().filter(f -> "PENDING".equals(f.getStatus())).count();
        BigDecimal totalRevenue = paid.stream()
                .map(Fee::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCount", all.size());
        stats.put("paidCount", paid.size());
        stats.put("pendingCount", pendingCount);
        stats.put("totalRevenue", totalRevenue.doubleValue());
        return stats;
    }
}