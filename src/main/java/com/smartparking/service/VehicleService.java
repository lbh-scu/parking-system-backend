package com.smartparking.service;

import com.smartparking.entity.ParkingSpot;
import com.smartparking.entity.Vehicle;
import com.smartparking.repository.ParkingSpotRepository;
import com.smartparking.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    /**
     * 车辆入场
     */
    @Transactional
    public Vehicle vehicleEntry(String plateNumber, String spotNumber) {
        // 检查是否已有未出场的同一辆车
        Optional<Vehicle> existing = vehicleRepository.findByPlateNumberAndStatus(plateNumber, "PARKING");
        if (existing.isPresent()) {
            throw new RuntimeException("该车辆已在停车场内");

        }

        ParkingSpot spot;
        String assignedSpot;
        if (spotNumber == null || spotNumber.isBlank()) {
            List<ParkingSpot> freeSpots = parkingSpotRepository.findByStatus("FREE");
            if (freeSpots.isEmpty()) {
                throw new RuntimeException("暂无空闲车位");
            }
            spot = freeSpots.get(0);
            assignedSpot = spot.getSpotNumber();
        } else {
            String input = spotNumber;
            spot = parkingSpotRepository.findBySpotNumber(spotNumber)
                    .orElseThrow(() -> new RuntimeException("车位不存在: " + input));
            if (!"FREE".equals(spot.getStatus())) {
                throw new RuntimeException("车位 " + spotNumber + " 已被占用");
            }
            assignedSpot = spotNumber;
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber(plateNumber);
        vehicle.setSpotNumber(assignedSpot);
        vehicle.setSpotId(spot.getId());
        vehicle.setStatus("PARKING");
        vehicle.setEntryTime(LocalDateTime.now());
        vehicle.setIsResident(false);
        vehicleRepository.save(vehicle);

        // 更新车位状态
        spot.setStatus("OCCUPIED");
        spot.setCurrentPlate(plateNumber);
        parkingSpotRepository.save(spot);

        return vehicle;
    }

    /**
     * 车辆出场
     */
    @Transactional
    public Vehicle vehicleExit(String plateNumber) {
        Vehicle vehicle = vehicleRepository.findByPlateNumberAndStatus(plateNumber, "PARKING")
                .orElseThrow(() -> new RuntimeException("未找到该车辆的入场记录"));

        vehicle.setExitTime(LocalDateTime.now());
        vehicle.setStatus("EXITED");
        vehicleRepository.save(vehicle);

        // 释放车位
        if (vehicle.getSpotNumber() != null) {
            parkingSpotRepository.findBySpotNumber(vehicle.getSpotNumber())
                    .ifPresent(spot -> {
                        spot.setStatus("FREE");
                        spot.setCurrentPlate(null);
                        parkingSpotRepository.save(spot);
                    });
        }

        return vehicle;
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
