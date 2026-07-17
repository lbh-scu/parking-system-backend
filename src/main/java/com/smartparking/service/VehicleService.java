package com.smartparking.service;

import com.smartparking.entity.Vehicle;
import com.smartparking.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    /**
     * 车辆入场
     */
    public Vehicle vehicleEntry(String plateNumber, String spotNumber) {
        // 检查是否已有未出场的同一辆车
        Optional<Vehicle> existing = vehicleRepository.findByPlateNumberAndStatus(plateNumber, "PARKING");
        if (existing.isPresent()) {
            throw new RuntimeException("该车辆已在停车场内");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber(plateNumber);
        vehicle.setSpotNumber(spotNumber);
        vehicle.setStatus("PARKING");
        vehicle.setEntryTime(LocalDateTime.now());
        // isResident 后续由 1.2 任务接入 ResidentService 动态判断
        vehicle.setIsResident(false);

        return vehicleRepository.save(vehicle);
    }

    /**
     * 车辆出场
     */
    public Vehicle vehicleExit(String plateNumber) {
        Vehicle vehicle = vehicleRepository.findByPlateNumberAndStatus(plateNumber, "PARKING")
                .orElseThrow(() -> new RuntimeException("未找到该车辆的入场记录"));

        vehicle.setExitTime(LocalDateTime.now());
        vehicle.setStatus("EXITED");

        return vehicleRepository.save(vehicle);
    }

    /**
     * 获取在场车辆列表
     */
    public List<Vehicle> getParkingVehicles() {
        return vehicleRepository.findByStatus("PARKING");
    }

    /**
     * 获取历史记录
     */
    public List<Vehicle> getVehicleHistory(String plateNumber) {
        if (plateNumber != null && !plateNumber.isEmpty()) {
            return vehicleRepository.findByPlateNumberContaining(plateNumber);
        }
        return vehicleRepository.findAll();
    }
}
