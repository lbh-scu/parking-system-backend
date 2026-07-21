package com.smartparking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "parking_spot")
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spot_number", nullable = false, length = 10, unique = true)
    private String spotNumber;

    @Column(name = "area", nullable = false, length = 10)
    private String area;

    @Column(name = "status", length = 20)
    private String status = "FREE";

    @Column(name = "current_plate", length = 20)
    private String currentPlate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ParkingSpot() {}

    // ===== getters =====
    public Long getId() { return id; }
    public String getSpotNumber() { return spotNumber; }
    public String getArea() { return area; }
    public String getStatus() { return status; }
    public String getCurrentPlate() { return currentPlate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ===== setters =====
    public void setId(Long id) { this.id = id; }
    public void setSpotNumber(String spotNumber) { this.spotNumber = spotNumber; }
    public void setArea(String area) { this.area = area; }
    public void setStatus(String status) { this.status = status; }
    public void setCurrentPlate(String currentPlate) { this.currentPlate = currentPlate; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParkingSpot that = (ParkingSpot) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ParkingSpot{id=" + id + ", spotNumber='" + spotNumber + "', status='" + status + "'}";
    }
}
