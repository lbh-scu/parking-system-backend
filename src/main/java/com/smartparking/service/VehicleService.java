package com.smartparking.service;

import com.smartparking.entity.Fee;
import com.smartparking.entity.ParkingSpot;
import com.smartparking.entity.Vehicle;
import com.smartparking.repository.FeeRepository;
import com.smartparking.repository.ParkingSpotRepository;
import com.smartparking.repository.ResidentRepository;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.util.DateTimeUtil;
import com.smartparking.util.LicensePlateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private ResidentRepository residentRepository;

    @Autowired
    private FeeService feeService;

    /**
     * 车辆入场
     */
    @Transactional
    public Vehicle vehicleEntry(String plateNumber, String spotNumber) {
        // 检查是否已有未出场的同一辆车
        List<Vehicle> existing = vehicleRepository.findByPlateNumberAndStatusOrderByEntryTimeDesc(plateNumber, "PARKING");
        if (!existing.isEmpty()) {
            throw new RuntimeException("该车辆已在停车场内");
        }

        // 车位号为空时自动分配
        ParkingSpot spot;
        String finalSpot = spotNumber;
        if (spotNumber == null || spotNumber.isBlank()) {
            List<ParkingSpot> freeSpots = parkingSpotRepository.findByStatus("FREE");
            if (freeSpots.isEmpty()) {
                throw new RuntimeException("暂无空闲车位");
            }
            spot = freeSpots.get(0);
            finalSpot = spot.getSpotNumber();
        } else {
            spot = parkingSpotRepository.findBySpotNumber(spotNumber)
                    .orElseThrow(() -> new RuntimeException("车位不存在: " + spotNumber));
            if (!"FREE".equals(spot.getStatus())) {
                throw new RuntimeException("车位 " + finalSpot + " 已被占用");
            }
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber(plateNumber);
        vehicle.setSpotNumber(finalSpot);
        vehicle.setSpotId(spot.getId());
        vehicle.setStatus("PARKING");
        vehicle.setEntryTime(DateTimeUtil.now());

        // 判断车牌是否在住户表，是则标记为小区住户
        boolean isResidentCar = residentRepository.existsByPlateNumber(plateNumber);
        vehicle.setIsResident(isResidentCar);

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
        List<Vehicle> parkingRecords = vehicleRepository.findByPlateNumberAndStatusOrderByEntryTimeDesc(plateNumber, "PARKING");
        if (parkingRecords.isEmpty()) {
            throw new RuntimeException("未找到该车辆的入场记录");
        }
        Vehicle vehicle = parkingRecords.get(0);

        LocalDateTime exitTime = DateTimeUtil.now();
        vehicle.setExitTime(exitTime);
        vehicle.setStatus("EXITED");
        vehicleRepository.save(vehicle);

        // 住户车辆出场：直接释放车位，不创建费用记录
        if (Boolean.TRUE.equals(vehicle.getIsResident())) {
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

        // 非住户车辆：自动创建费用记录（PENDING状态，放入待结算）
        Duration duration = Duration.between(vehicle.getEntryTime(), exitTime);
        long minutes = duration.toMinutes();
        double hours = minutes / 60.0;
        BigDecimal totalAmount = feeService.calculateAmount(minutes);
        BigDecimal hourlyRate = feeService.getHourlyRate();

        Fee fee = new Fee();
        fee.setPlateNumber(plateNumber);
        fee.setEntryTime(vehicle.getEntryTime());
        fee.setExitTime(exitTime);
        fee.setParkingHours(hours);
        fee.setHourlyRate(hourlyRate);
        fee.setTotalAmount(totalAmount);
        fee.setStatus("PENDING");
        feeRepository.save(fee);

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
