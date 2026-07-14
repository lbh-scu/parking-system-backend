package com.smartparking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_number", nullable = false, length = 20)
    private String plateNumber;

    @Column(name = "spot_id")
    private Long spotId;

    @Column(name = "spot_number", length = 10)
    private String spotNumber;

    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(name = "is_resident")
    private Boolean isResident = false;

    @Column(name = "status", length = 20)
    private String status = "PARKING";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
