package com.smartparking.repository;

import com.smartparking.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // 根据车牌号查找车辆
    Optional<Vehicle> findByPlateNumber(String plateNumber);

    // 查找在场车辆
    List<Vehicle> findByStatus(String status);

    // 查找指定状态的车辆
    List<Vehicle> findByStatusOrderByEntryTimeDesc(String status);

    // 根据车牌号和状态查找
    Optional<Vehicle> findByPlateNumberAndStatus(String plateNumber, String status);

    // 根据是否住户查找
    List<Vehicle> findByIsResident(Boolean isResident);

    // 统计在场车辆数
    long countByStatus(String status);
}
