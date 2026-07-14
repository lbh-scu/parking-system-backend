package com.smartparking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "fee")
public class Fee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_number", nullable = false, length = 20)
    private String plateNumber;

    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(name = "parking_hours")
    private Double parkingHours;

    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate = new BigDecimal("5.00");

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    @Column(name = "created_at")
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
