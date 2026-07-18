package com.smartparking.controller;

import com.smartparking.entity.Fee;
import com.smartparking.entity.ParkingSpot;
import com.smartparking.entity.Vehicle;
import com.smartparking.repository.FeeRepository;
import com.smartparking.repository.ParkingSpotRepository;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/test-data")
public class TestDataController {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    private final Random random = new Random(42);

    /**
     * 生成过去7天的完整测试数据（含车辆出入、费用结算）
     * 用于验证M5统计页面的趋势图、高峰时段、AI预测等功能
     */
    @PostMapping("/generate")
    public ApiResponse<String> generateTestData() {
        // 清空旧数据（只清车辆和费用，保留车位）
        vehicleRepository.deleteAll();
        feeRepository.deleteAll();

        // 获取车位列表
        List<ParkingSpot> spots = parkingSpotRepository.findAll();
        int spotCount = spots.size();
        if (spotCount == 0) {
            return ApiResponse.error(400, "请先初始化车位数据");
        }

        // 车牌前缀
        String[] prefixes = {"京A", "京B", "沪C", "粤D", "苏E", "浙F", "闽G", "川H", "鄂I", "湘J",
                             "豫K", "皖L", "鲁M", "冀N", "吉O", "津P", "赣Q", "陕R", "晋S", "辽T"};

        // 模拟8:00~20:00各时段的入场概率（真实高峰分布）
        double[] hourWeights = {
            0.02, 0.01, 0.0, 0.0, 0.0, 0.0, 0.0, 0.05,  // 0~7时
            0.15, 0.12, 0.08, 0.06, 0.07, 0.08, 0.10, 0.12,  // 8~15时
            0.10, 0.04, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0   // 16~23时
        };

        int totalVehicles = 0;
        int totalFees = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        // 生成过去7天（含今天）的数据
        LocalDateTime now = LocalDateTime.now();

        for (int dayOffset = 6; dayOffset >= 0; dayOffset--) {
            LocalDateTime dayStart = now.minusDays(dayOffset).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime dayEnd = dayStart.plusDays(1);

            // 每天随机生成 20~40 辆车
            int vehiclesToday = 20 + random.nextInt(21);
            int spotIndex = 0;

            for (int v = 0; v < vehiclesToday && spotIndex < spotCount; v++) {
                // 根据权重随机选择入场小时
                int entryHour = weightedRandomHour(hourWeights);
                // 入场分钟随机
                int entryMinute = random.nextInt(60);

                LocalDateTime entryTime = dayStart.withHour(entryHour).withMinute(entryMinute);

                // 车牌号
                String prefix = prefixes[random.nextInt(prefixes.length)];
                String plateNum = String.format("%05d", random.nextInt(100000));
                String plateNumber = prefix + plateNum;

                // 随机停车 1~6 小时后出场
                double stayHours = 1 + random.nextDouble() * 5;
                LocalDateTime exitTime = entryTime.plusMinutes((long)(stayHours * 60));

                // 限定出场时间不能超过当天
                if (exitTime.isAfter(dayEnd)) {
                    exitTime = dayEnd.minusMinutes(1);
                }

                // 获取车位
                ParkingSpot spot = spots.get(spotIndex % spotCount);
                spotIndex++;

                // 创建车辆记录
                Vehicle vehicle = new Vehicle();
                vehicle.setPlateNumber(plateNumber);
                vehicle.setSpotNumber(spot.getSpotNumber());
                vehicle.setSpotId(spot.getId());
                vehicle.setEntryTime(entryTime);
                vehicle.setExitTime(exitTime);
                vehicle.setIsResident(false);
                vehicle.setStatus("EXITED");
                vehicle.setCreatedAt(entryTime);
                vehicle.setUpdatedAt(exitTime);
                vehicleRepository.save(vehicle);
                totalVehicles++;

                // 计算费用（基于实际停车时长）
                double parkingMinutes = java.time.Duration.between(entryTime, exitTime).toMinutes();
                double parkingHours = parkingMinutes / 60.0;

                // 前30分钟免费
                double chargeableMinutes = Math.max(0, parkingMinutes - 30);
                double chargeableHours = chargeableMinutes / 60.0;
                // 5元/小时，每日封顶50元
                BigDecimal amount = new BigDecimal("5.00")
                        .multiply(BigDecimal.valueOf(Math.ceil(chargeableHours)))
                        .min(new BigDecimal("50.00"));

                // 支付时间在出场后1~10分钟内
                LocalDateTime paymentTime = exitTime.plusMinutes(1 + random.nextInt(10));

                Fee fee = new Fee();
                fee.setPlateNumber(plateNumber);
                fee.setEntryTime(entryTime);
                fee.setExitTime(exitTime);
                fee.setParkingHours(parkingHours);
                fee.setHourlyRate(new BigDecimal("5.00"));
                fee.setTotalAmount(amount);
                fee.setStatus("PAID");
                fee.setPaymentTime(paymentTime);
                fee.setCreatedAt(entryTime);
                feeRepository.save(fee);
                totalFees++;
                totalRevenue = totalRevenue.add(amount);
            }
        }

        // 创建几条当前仍在场的车辆（用于展示占用率）
        for (int i = 0; i < 5; i++) {
            String prefix = prefixes[random.nextInt(prefixes.length)];
            String plateNum = String.format("%05d", random.nextInt(100000));
            String plateNumber = prefix + plateNum;

            ParkingSpot spot = spots.get(random.nextInt(spotCount));

            Vehicle vehicle = new Vehicle();
            vehicle.setPlateNumber(plateNumber + "Z"); // 加后缀避免重复
            vehicle.setSpotNumber(spot.getSpotNumber());
            vehicle.setSpotId(spot.getId());
            vehicle.setEntryTime(now.minusHours(1 + random.nextInt(4)));
            vehicle.setExitTime(null);
            vehicle.setIsResident(false);
            vehicle.setStatus("PARKING");
            vehicle.setCreatedAt(now.minusHours(1 + random.nextInt(4)));
            vehicleRepository.save(vehicle);
            totalVehicles++;

            // 更新车位状态
            spot.setStatus("OCCUPIED");
            spot.setCurrentPlate(plateNumber + "Z");
            parkingSpotRepository.save(spot);
        }

        String result = String.format(
            "✅ 测试数据生成完成！\n   📅 覆盖过去7天\n   🚗 共 %d 条车辆记录（含5辆在场）\n   💰 共 %d 笔已结算费用，总收入 ¥%s",
            totalVehicles, totalFees, totalRevenue.setScale(2, BigDecimal.ROUND_HALF_UP).toString()
        );

        return ApiResponse.success(result);
    }

    /**
     * 根据权重数组随机选择小时（0~23）
     */
    private int weightedRandomHour(double[] weights) {
        double total = 0;
        for (double w : weights) total += w;
        double r = random.nextDouble() * total;
        double cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (r <= cumulative) return i;
        }
        return 8;
    }
}