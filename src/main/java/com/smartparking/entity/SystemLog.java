package com.smartparking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统操作日志实体
 * 记录车辆入场/出场、费用结算、配置变更等关键操作
 */
@Entity
@Table(name = "system_log")
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作类型：ENTRY/EXIT/FEE_PAY/CONFIG_UPDATE/BACKUP/SYSTEM */
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    /** 操作描述 */
    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    /** 关联车牌号（可选） */
    @Column(name = "plate_number", length = 20)
    private String plateNumber;

    /** 操作人（当前系统暂未实现用户认证，固定为"系统"） */
    @Column(name = "operator", length = 50)
    private String operator;

    /** 操作结果：SUCCESS/FAILURE */
    @Column(name = "result", length = 20)
    private String result;

    /** 操作时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SystemLog() {}

    public SystemLog(String actionType, String message, String plateNumber, String operator, String result) {
        this.actionType = actionType;
        this.message = message;
        this.plateNumber = plateNumber;
        this.operator = operator;
        this.result = result;
        this.createdAt = LocalDateTime.now();
    }

    // ===== getters =====
    public Long getId() { return id; }
    public String getActionType() { return actionType; }
    public String getMessage() { return message; }
    public String getPlateNumber() { return plateNumber; }
    public String getOperator() { return operator; }
    public String getResult() { return result; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ===== setters =====
    public void setId(Long id) { this.id = id; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public void setMessage(String message) { this.message = message; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public void setOperator(String operator) { this.operator = operator; }
    public void setResult(String result) { this.result = result; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}