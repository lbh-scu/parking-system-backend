package com.smartparking.service;

import com.smartparking.entity.ParkingSpot;
import com.smartparking.repository.ParkingSpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ParkingSpotService {

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    /**
     * 获取所有车位
     */
    public List<ParkingSpot> findAll() {
        return parkingSpotRepository.findAll();
    }

    /**
     * 获取空闲车位列表
     */
    public List<ParkingSpot> findFreeSpots() {
        return parkingSpotRepository.findByStatus("FREE");
    }

    /**
     * 自动分配一个空闲车位
     */
    @Transactional
    public ParkingSpot assignSpot(String plateNumber) {
        List<ParkingSpot> freeSpots = parkingSpotRepository.findByStatus("FREE");
        if (freeSpots.isEmpty()) {
            throw new RuntimeException("暂无空闲车位");
        }
        ParkingSpot spot = freeSpots.get(0);
        spot.setStatus("OCCUPIED");
        spot.setCurrentPlate(plateNumber);
        return parkingSpotRepository.save(spot);
    }

    /**
     * 释放车位
     */
    @Transactional
    public ParkingSpot releaseSpot(String spotNumber) {
        ParkingSpot spot = parkingSpotRepository.findBySpotNumber(spotNumber)
                .orElseThrow(() -> new RuntimeException("车位不存在: " + spotNumber));
        spot.setStatus("FREE");
        spot.setCurrentPlate(null);
        return parkingSpotRepository.save(spot);
    }

    /**
     * 根据车位号释放车位
     */
    @Transactional
    public ParkingSpot releaseBySpotNumber(String spotNumber) {
        return releaseSpot(spotNumber);
    }

    /**
     * 获取热力图数据：按区域+楼层统计占用率
     */
    public List<Map<String, Object>> getHeatmapData() {
        List<ParkingSpot> allSpots = parkingSpotRepository.findAll();

        Map<String, List<ParkingSpot>> grouped = allSpots.stream()
                .collect(Collectors.groupingBy(ParkingSpot::getArea));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<ParkingSpot>> entry : grouped.entrySet()) {
            List<ParkingSpot> spots = entry.getValue();
            long occupied = spots.stream().filter(s -> "OCCUPIED".equals(s.getStatus())).count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("area", entry.getKey());
            item.put("total", spots.size());
            item.put("occupied", (int) occupied);
            item.put("rate", spots.isEmpty() ? 0 : (double) occupied / spots.size());
            result.add(item);
        }
        return result;
    }

    /**
     * 获取区域占用率对比数据
     */
    public List<Map<String, Object>> getAreaCompareData() {
        List<ParkingSpot> allSpots = parkingSpotRepository.findAll();

        Map<String, List<ParkingSpot>> grouped = allSpots.stream()
                .collect(Collectors.groupingBy(ParkingSpot::getArea));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<ParkingSpot>> entry : grouped.entrySet()) {
            List<ParkingSpot> spots = entry.getValue();
            long occupied = spots.stream().filter(s -> "OCCUPIED".equals(s.getStatus())).count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("area", entry.getKey());
            item.put("total", spots.size());
            item.put("occupied", (int) occupied);
            item.put("free", spots.size() - (int) occupied);
            result.add(item);
        }
        return result;
    }

    /**
     * 获取总体占用率
     */
    public Map<String, Object> getOccupancyRate() {
        long total = parkingSpotRepository.count();
        long occupied = parkingSpotRepository.countByStatus("OCCUPIED");
        long free = parkingSpotRepository.countByStatus("FREE");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", (int) total);
        result.put("occupied", (int) occupied);
        result.put("free", (int) free);
        result.put("rate", total == 0 ? 0 : (double) occupied / total);
        return result;
    }
}
