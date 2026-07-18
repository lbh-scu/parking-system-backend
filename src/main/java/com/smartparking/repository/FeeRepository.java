package com.smartparking.repository;

import com.smartparking.entity.Fee;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeeRepository extends BaseRepository<Fee, Long> {

    // 根据状态查找费用记录
    List<Fee> findByStatus(String status);

    // 根据车牌号查找
    List<Fee> findByPlateNumberOrderByCreatedAtDesc(String plateNumber);

    // 根据车牌号和状态查找（含状态匹配）
    List<Fee> findByPlateNumberAndStatus(String plateNumber, String status);

    // 查找某时间范围内的已支付费用
    @Query("SELECT f FROM Fee f WHERE f.status = 'PAID' AND f.paymentTime BETWEEN :start AND :end ORDER BY f.paymentTime DESC")
    List<Fee> findPaidRecordsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 统计某时间范围内的已支付总金额
    @Query("SELECT COALESCE(SUM(f.totalAmount), 0) FROM Fee f WHERE f.status = 'PAID' AND f.paymentTime BETWEEN :start AND :end")
    java.math.BigDecimal sumPaidBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
