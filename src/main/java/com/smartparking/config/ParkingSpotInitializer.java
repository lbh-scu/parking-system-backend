package com.smartparking.config;

import com.smartparking.entity.ParkingSpot;
import com.smartparking.repository.ParkingSpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ParkingSpotInitializer implements CommandLineRunner {

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Override
    public void run(String... args) {
        if (parkingSpotRepository.count() > 0) {
            return;
        }

        // A区 B1: A001-A050, B2: A051-A100
        for (int i = 1; i <= 50; i++) {
            createSpot(String.format("A%03d", i), "A区", 1);
        }
        for (int i = 51; i <= 100; i++) {
            createSpot(String.format("A%03d", i), "A区", 2);
        }
        // B区 B1: B001-B035, B2: B036-B070
        for (int i = 1; i <= 35; i++) {
            createSpot(String.format("B%03d", i), "B区", 1);
        }
        for (int i = 36; i <= 70; i++) {
            createSpot(String.format("B%03d", i), "B区", 2);
        }
        // C区 B1: C001-C035, B2: C036-C070
        for (int i = 1; i <= 35; i++) {
            createSpot(String.format("C%03d", i), "C区", 1);
        }
        for (int i = 36; i <= 70; i++) {
            createSpot(String.format("C%03d", i), "C区", 2);
        }

        System.out.println("===== 初始化车位数据完成: " + parkingSpotRepository.count() + " 个车位 =====");
    }

    private void createSpot(String spotNumber, String area, int floor) {
        ParkingSpot spot = new ParkingSpot();
        spot.setSpotNumber(spotNumber);
        spot.setArea(area);
        spot.setFloor(floor);
        spot.setStatus("FREE");
        parkingSpotRepository.save(spot);
    }
}
