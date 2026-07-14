package com.smartparking.repository;

import com.smartparking.entity.Fee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {

    // 根据状态查找费用记录
    List<Fee> findByStatus(String status);

    // 根据车牌号查找
    List<Fee> findByPlateNumberOrderByCreatedAtDesc(String plateNumber);

    // 根据车牌号和状态查找
    List<Fee> findByPlateNumberAndStatus(String plateNumber, String status);
}
