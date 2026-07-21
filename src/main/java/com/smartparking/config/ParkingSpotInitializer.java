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

        // A区: A001-A100
        for (int i = 1; i <= 100; i++) {
            createSpot(String.format("A%03d", i), "A区");
        }
        // B区: B001-B070
        for (int i = 1; i <= 70; i++) {
            createSpot(String.format("B%03d", i), "B区");
        }
        // C区: C001-C070
        for (int i = 1; i <= 70; i++) {
            createSpot(String.format("C%03d", i), "C区");
        }

        System.out.println("===== 初始化车位数据完成: " + parkingSpotRepository.count() + " 个车位 =====");
    }

    private void createSpot(String spotNumber, String area) {
        ParkingSpot spot = new ParkingSpot();
        spot.setSpotNumber(spotNumber);
        spot.setArea(area);
        spot.setStatus("FREE");
        parkingSpotRepository.save(spot);
    }
}
