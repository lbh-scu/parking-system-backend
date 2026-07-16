package com.smartparking.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "vehicle")
public class Vehicle {
    // 车辆记录主键
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ExcelProperty("记录ID")
    private Long id;

    // 车牌号码，车辆唯一标识，不可为空
    @Column(name = "plate_number", nullable = false, length = 20)
    @ExcelProperty("车牌号")
    private String plateNumber;

    // 车位主键ID，关联车位表
    @Column(name = "spot_id")
    private Long spotId;

    // 车位编号，展示给用户看的车位号
    @Column(name = "spot_number", length = 10)
    @ExcelProperty("车位号")
    private String spotNumber;

    // 车辆入场时间,关联fee表
    @Column(name = "entry_time")
    @ExcelProperty("入场时间")
    private LocalDateTime entryTime;

    // 车辆离场时间,车辆还在场内时为null,关联fee表
    @Column(name = "exit_time")
    @ExcelProperty("离场时间")
    private LocalDateTime exitTime;

    // 是否为小区常住居民车辆
    @Column(name = "is_resident")
    @ExcelProperty("是否住户")
    private Boolean isResident = false;

    // 车辆状态:"PARKING"：在场停车,"LEAVE"：已离场
    @Column(name = "status", length = 20)
    @ExcelProperty("状态")
    private String status = "PARKING";

    // 车辆进场记录创建时间
    @Column(name = "created_at")
    @ExcelProperty("创建时间")
    private LocalDateTime createdAt;

    // 记录更新时间
    @Column(name = "updated_at")
    @ExcelProperty("更新时间")
    private LocalDateTime updatedAt;

    public Vehicle() {}

    // ===== getters =====

    public Long getId() { return id; }
    public String getPlateNumber() { return plateNumber; }
    public Long getSpotId() { return spotId; }
    public String getSpotNumber() { return spotNumber; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public LocalDateTime getExitTime() { return exitTime; }
    public Boolean getIsResident() { return isResident; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ===== setters =====

    public void setId(Long id) { this.id = id; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public void setSpotId(Long spotId) { this.spotId = spotId; }
    public void setSpotNumber(String spotNumber) { this.spotNumber = spotNumber; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }
    public void setIsResident(Boolean isResident) { this.isResident = isResident; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (entryTime == null) {
            entryTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(id, vehicle.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Vehicle{id=" + id + ", plateNumber='" + plateNumber + "', status='" + status + "'}";
    }
}
