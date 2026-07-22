package com.smartparking.repository;

import com.smartparking.entity.Resident;
import org.springframework.stereotype.Repository;

@Repository
public interface ResidentRepository extends BaseRepository<Resident, Long> {
    // 判断车牌号是否存在
    boolean existsByPlateNumber(String plateNumber);

    // 根据车牌号查找住户（可能多个住户共用同一车牌，返回List）
    java.util.List<Resident> findByPlateNumber(String plateNumber);
}
