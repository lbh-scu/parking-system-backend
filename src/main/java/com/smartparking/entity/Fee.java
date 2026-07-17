package com.smartparking.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;


/**
 * 停车费用账单表实体
 * 每一条记录对应一辆车辆一次完整停车产生的费用单据
 */
@Entity
@Table(name = "fee")
public class Fee {

    // 账单主键
    @Id
    @ExcelIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ExcelProperty("账单ID")
    private Long id;

    // 车牌号码
    @Column(name = "plate_number", nullable = false, length = 20)
    @ExcelProperty("车牌号")
    private String plateNumber;

    // 车辆入场时间
    @Column(name = "entry_time")
    @ExcelProperty("入场时间")
    private LocalDateTime entryTime;

    // 车辆离场时间,未离场时该字段为null
    @Column(name = "exit_time")
    @ExcelProperty("离场时间")
    private LocalDateTime exitTime;

    // 实际停车时长(小时)离场后根据入场,离场时间自动计算
    @Column(name = "parking_hours")
    @ExcelProperty("停车时长(小时)")
    private Double parkingHours;

    // 每小时停车单价，默认5元/小时
    @Column(name = "hourly_rate")
    @ExcelProperty("小时单价")
    private BigDecimal hourlyRate = new BigDecimal("5.00");

    // 停车总费用,计算公式：停车时长 × 小时单价
    @Column(name = "total_amount")
    @ExcelProperty("总费用")
    private BigDecimal totalAmount;

    // 账单状态:"PENDING"：待缴费,"PAID"：已缴费
    @Column(name = "status", length = 20)
    @ExcelProperty("状态")
    private String status = "PENDING";

    // 用户完成缴费的时间,未缴费时为null
    @Column(name = "payment_time")
    @ExcelProperty("缴费时间")
    private LocalDateTime paymentTime;

    // 本条账单记录创建时间,车辆入场时自动生成
    @Column(name = "created_at")
    @ExcelProperty("创建时间")
    private LocalDateTime createdAt;

    public Fee() {}

    // ===== getters =====

    public Long getId() { return id; }
    public String getPlateNumber() { return plateNumber; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public LocalDateTime getExitTime() { return exitTime; }
    public Double getParkingHours() { return parkingHours; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public LocalDateTime getPaymentTime() { return paymentTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ===== setters =====

    public void setId(Long id) { this.id = id; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }
    public void setParkingHours(Double parkingHours) { this.parkingHours = parkingHours; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setStatus(String status) { this.status = status; }
    public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fee fee = (Fee) o;
        return Objects.equals(id, fee.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Fee{id=" + id + ", plateNumber='" + plateNumber + "', status='" + status + "'}";
    }
}