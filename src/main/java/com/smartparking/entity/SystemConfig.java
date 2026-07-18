package com.smartparking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 系统配置实体
 * 存储费率、车位参数等运行时配置，修改后即时生效
 */
@Entity
@Table(name = "system_config")
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 配置键 */
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    /** 配置值 */
    @Column(name = "config_value", nullable = false, length = 500)
    private String configValue;

    /** 配置描述 */
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SystemConfig() {}

    public SystemConfig(String configKey, String configValue, String description) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
    }

    // ===== getters =====
    public Long getId() { return id; }
    public String getConfigKey() { return configKey; }
    public String getConfigValue() { return configValue; }
    public String getDescription() { return description; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ===== setters =====
    public void setId(Long id) { this.id = id; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public void setDescription(String description) { this.description = description; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemConfig that = (SystemConfig) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "SystemConfig{id=" + id + ", key='" + configKey + "'}";
    }
}