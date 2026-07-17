package com.smartparking.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 小区常住业主实体
 */
@Entity
@Table(name = "resident")
public class Resident {
    // 业主记录主键
    @Id
    @ExcelIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 业主姓名，不可为空
    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    // 业主绑定车牌，关联Vehicle、Fee表
    @Column(name = "plate_number", nullable = false, length = 20)
    private String plateNumber;



    public Resident() {}

    // ===== getters =====
    public Long getId() { return id; }
    public String getUserName() { return userName; }
    public String getPlateNumber() { return plateNumber; }

    // ===== setters =====
    public void setId(Long id) { this.id = id; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resident resident = (Resident) o;
        return Objects.equals(id, resident.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Resident{id=" + id + ", userName='" + userName + "', plateNumber='" + plateNumber + "'}";
    }
}
