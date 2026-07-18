package com.smartparking.repository;

import com.smartparking.entity.SystemConfig;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigRepository extends BaseRepository<SystemConfig, Long> {

    Optional<SystemConfig> findByConfigKey(String configKey);
}