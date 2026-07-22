package com.smartparking.service;

import com.smartparking.entity.Fee;
import com.smartparking.entity.Vehicle;
import com.smartparking.repository.FeeRepository;
import com.smartparking.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 从 SystemConfig 读取动态费率配置
     */
    public BigDecimal getHourlyRate() {
        return BigDecimal.valueOf(systemConfigService.getConfigDouble("hourly_rate", 5.00));
    }

    private int getFreeMinutes() {
        return systemConfigService.getConfigInt("free_minutes", 30);
    }

    private BigDecimal getDailyMax() {
        return BigDecimal.valueOf(systemConfigService.getConfigDouble("daily_max", 50.00));
    }

    /**
     * 核心计费逻辑：计算停车费用
     * @param minutes  停车总分钟数
     * @return 计算后的费用金额
     */
    public BigDecimal calculateAmount(long minutes) {
        BigDecimal hourlyRate = getHourlyRate();
        int freeMinutes = getFreeMinutes();
        BigDecimal dailyMax = getDailyMax();

        // 减去免费时长
        long billableMinutes = Math.max(0, minutes - freeMinutes);
        if (billableMinutes == 0) {
            return BigDecimal.ZERO;
        }

        // 按小时计费，不足1小时按 ceil 取整
        double billableHours = Math.ceil(billableMinutes / 60.0);
        BigDecimal amount = hourlyRate.multiply(BigDecimal.valueOf(billableHours));

        // 计算停车的天数（向上取整），每日封顶 = dailyMax × 天数
        double days = Math.ceil(billableHours / 24.0);
        BigDecimal totalCap = dailyMax.multiply(BigDecimal.valueOf(days));

        // 应用每日封顶（多天则累加封顶）
        if (amount.compareTo(totalCap) > 0) {
            amount = totalCap;
        }

        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算停车费用
     * 每次调用都会重新计算（使用最新的 SystemConfig 费率配置）
     */
    public Fee calculateFee(String plateNumber) {
        // 优先更新已存在的 PENDING 费用记录（使用最新配置重新计算）
        List<Fee> pendingFees = feeRepository.findByPlateNumberAndStatus(plateNumber, "PENDING");
        if (!pendingFees.isEmpty()) {
            Fee existing = pendingFees.get(0);
            if (existing.getExitTime() == null) {
                existing.setExitTime(LocalDateTime.now());
            }
            long minutes = Duration.between(existing.getEntryTime(), existing.getExitTime()).toMinutes();
            existing.setParkingHours(minutes / 60.0);
            BigDecimal newAmount = calculateAmount(minutes);
            existing.setTotalAmount(newAmount);
            existing.setHourlyRate(getHourlyRate());
            return feeRepository.save(existing);
        }

        // 如果没有 PENDING 记录，尝试从在场车辆计算（按入场时间降序取最新记录）
        List<Vehicle> parkingRecords = vehicleRepository.findByPlateNumberAndStatusOrderByEntryTimeDesc(plateNumber, "PARKING");
        if (parkingRecords.isEmpty()) {
            throw new RuntimeException("未找到该车辆的入场记录");
        }
        Vehicle vehicle = parkingRecords.get(0);

        LocalDateTime entryTime = vehicle.getEntryTime();
        LocalDateTime exitTime = LocalDateTime.now();

        Duration duration = Duration.between(entryTime, exitTime);
        long minutes = duration.toMinutes();
        double hours = minutes / 60.0;
        BigDecimal totalAmount = calculateAmount(minutes);
        BigDecimal hourlyRate = getHourlyRate();

        Fee fee = new Fee();
        fee.setPlateNumber(plateNumber);
        fee.setEntryTime(entryTime);
        fee.setExitTime(exitTime);
        fee.setParkingHours(hours);
        fee.setHourlyRate(hourlyRate);
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