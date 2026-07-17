package com.smartparking.repository;

import com.smartparking.entity.Resident;
import org.springframework.stereotype.Repository;

@Repository
public interface ResidentRepository extends BaseRepository<Resident, Long> {
    // 判断车牌号是否存在
    boolean existsByPlateNumber(String plateNumber);
}
