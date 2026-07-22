package com.smartparking.repository;

import com.smartparking.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    /** 按操作时间倒序获取最近N条日志 */
    List<SystemLog> findTop100ByOrderByCreatedAtDesc();

    /** 按类型查询日志 */
    List<SystemLog> findByActionTypeOrderByCreatedAtDesc(String actionType);
}