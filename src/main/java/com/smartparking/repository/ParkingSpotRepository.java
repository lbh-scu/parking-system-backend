package com.smartparking.repository;

import com.smartparking.entity.ParkingSpot;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSpotRepository extends BaseRepository<ParkingSpot, Long> {

    Optional<ParkingSpot> findBySpotNumber(String spotNumber);

    List<ParkingSpot> findByStatus(String status);

    List<ParkingSpot> findByArea(String area);

    List<ParkingSpot> findByAreaAndFloor(String area, Integer floor);

    long countByStatus(String status);

    long countByAreaAndStatus(String area, String status);
}
