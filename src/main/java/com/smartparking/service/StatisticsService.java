package com.smartparking.service;

import com.smartparking.entity.Fee;
import com.smartparking.entity.ParkingSpot;
import com.smartparking.entity.Vehicle;
import com.smartparking.repository.FeeRepository;
import com.smartparking.repository.ParkingSpotRepository;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.util.OccupancyPredictor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * M5 数据统计与AI模块 核心服务
 *
 * 算法说明：
 * - 高峰时段: 按入场/出场时间的小时聚合24小时分布
 * - 占用率趋势: 每天在场车辆数 / 总车位数
 * - AI预测: 基于真实历史占用率的同小时加权平均 + 邻域平滑 + 周期性衰减
 * - 收费趋势: 按日/周/月聚合已支付费用
 * - 报表导出: .xlsx Excel格式
 */
@Service
public class StatisticsService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    // ==================== M5.1 高峰时段与使用率统计 ====================

    /**
     * 获取24小时入场/出场频次（高峰时段统计）
     */
    public List<Map<String, Object>> getPeakHours() {
        List<Vehicle> allVehicles = vehicleRepository.findAll();

        int[] entryCounts = new int[24];
        int[] exitCounts = new int[24];

        for (Vehicle v : allVehicles) {
            if (v.getEntryTime() != null) {
                entryCounts[v.getEntryTime().getHour()]++;
            }
            if (v.getExitTime() != null) {
                exitCounts[v.getExitTime().getHour()]++;
            }
        }

        List<Map<String, Object>> result = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hour", i);
            item.put("entryCount", entryCounts[i]);
            item.put("exitCount", exitCounts[i]);
            result.add(item);
        }
        return result;
    }

    /**
     * 按天统计车位使用率变化趋势
     *
     * 在场条件：entryTime <= 当天23:59:59 AND (exitTime == null OR exitTime >= 当天00:00:00)
     */
    public List<Map<String, Object>> getTrend(int days) {
        long totalSpots = parkingSpotRepository.count();
        if (totalSpots == 0) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>(days);
        LocalDate today = LocalDate.now();
        List<Vehicle> allVehicles = vehicleRepository.findAll();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            long activeCount = allVehicles.stream()
                    .filter(v -> v.getEntryTime() != null
                            && !v.getEntryTime().isAfter(dayEnd)
                            && (v.getExitTime() == null || !v.getExitTime().isBefore(dayStart)))
                    .count();

            double rate = Math.min(1.0, (double) activeCount / totalSpots);
            rate = BigDecimal.valueOf(rate).setScale(3, RoundingMode.HALF_UP).doubleValue();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date.toString());
            item.put("rate", rate);
            item.put("activeCount", activeCount);
            result.add(item);
        }
        return result;
    }

    // ==================== M5.2 AI占用率预测（核心优化） ====================

    /**
     * 获取AI预测的未来占用率
     */
    public Map<String, Object> getAiPrediction(int predictHours) {
        List<Map<String, Object>> historyData = buildHourlyOccupancyHistory();

        OccupancyPredictor predictor = new OccupancyPredictor();
        predictor.train(historyData);

        int currentHour = LocalDateTime.now().getHour();
        List<Map<String, Object>> predictions = predictor.predictNextHours(currentHour, predictHours);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("predictions", predictions);
        result.put("mse", predictor.getMse());
        result.put("trainingSize", predictor.getTrainingSize());
        result.put("modelReady", predictor.isReady());
        result.put("historyData", historyData);

        return result;
    }

    /**
     * 构建24小时的真实历史占用率数据（优化版）
     *
     * ██ 原算法问题 ██
     * 原方法使用一个固定的 baseOccupancy（当前在场车辆/总车位数）作为24小时的公共基数，
     * 加上微不足道的小时级入场/出场活跃度波动。由于入场/出场频次是跨所有天聚合的，
     * 24个hour的频次相近，导致24小时的占用率几乎完全相同。
     * ➜ AI模型训练数据平坦 ➜ 预测结果一直不变
     *
     * ██ 优化算法 ██
     * 真正的"该小时占用率" = 在该小时时刻「在场的车辆数」/「总车位数」
     * 在场判断：entryTime <= 该小时 END  AND (exitTime == null OR exitTime >= 该小时 START)
     *
     * 为获得有区分度的24小时分布，使用「历史平均」策略：
     * 以过去N天的数据为样本，对每个小时做横截面统计，然后取均值。
     */
    private List<Map<String, Object>> buildHourlyOccupancyHistory() {
        long totalSpots = parkingSpotRepository.count();
        if (totalSpots == 0) return List.of();

        List<Vehicle> allVehicles = vehicleRepository.findAll();
        if (allVehicles.isEmpty()) return List.of();

        // 确定数据的时间范围（过去30天）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past30Days = now.minusDays(30);

        // 筛选时间范围内的车辆记录
        List<Vehicle> recentVehicles = allVehicles.stream()
                .filter(v -> v.getEntryTime() != null
                        && !v.getEntryTime().isBefore(past30Days))
                .collect(Collectors.toList());

        if (recentVehicles.isEmpty()) {
            recentVehicles = allVehicles.stream()
                    .filter(v -> v.getEntryTime() != null)
                    .collect(Collectors.toList());
        }
        if (recentVehicles.isEmpty()) return List.of();

        // 对每一天的每个小时做一次横截面统计
        // 找出所有有数据的日期
        LocalDate startDate = recentVehicles.stream()
                .map(v -> v.getEntryTime().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        LocalDate endDate = LocalDate.now();
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (totalDays > 30) {
            startDate = endDate.minusDays(30);
            totalDays = 31;
        }
        if (totalDays <= 0) totalDays = 1;

        // hourCount[h] = 有多少天的h小时时刻有在场数据
        int[] hourCount = new int[24];
        double[] hourOccupancySum = new double[24];

        for (int d = 0; d < totalDays; d++) {
            LocalDate date = startDate.plusDays(d);

            for (int h = 0; h < 24; h++) {
                // 该小时的时间窗口
                LocalDateTime hourStart = date.atTime(h, 0, 0);
                LocalDateTime hourEnd = date.atTime(h, 59, 59);

                // 统计在该小时「在位」的车辆数
                long parkedAtHour = recentVehicles.stream()
                        .filter(v -> v.getEntryTime() != null
                                && !v.getEntryTime().isAfter(hourEnd)
                                && (v.getExitTime() == null || !v.getExitTime().isBefore(hourStart)))
                        .count();

                if (parkedAtHour > 0) {
                    double occupancy = Math.min(1.0, (double) parkedAtHour / totalSpots);
                    hourOccupancySum[h] += occupancy;
                    hourCount[h]++;
                }
            }
        }

        // 计算每小时的平均占用率
        List<Map<String, Object>> history = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            double rate;
            if (hourCount[h] > 0) {
                rate = hourOccupancySum[h] / hourCount[h];
            } else {
                // 没有数据的用相邻小时插值
                rate = interpolateMissingHour(h, hourOccupancySum, hourCount);
            }
            rate = Math.max(0.01, Math.min(0.99, rate));
            rate = BigDecimal.valueOf(rate).setScale(3, RoundingMode.HALF_UP).doubleValue();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hour", h);
            item.put("rate", rate);
            item.put("activeCount", (int) Math.round(rate * totalSpots));
            history.add(item);
        }
        return history;
    }

    /**
     * 对缺失历史数据的小时进行插值（相邻小时均值）
     */
    private double interpolateMissingHour(int hour, double[] hourOccupancySum, int[] hourCount) {
        double sum = 0;
        int count = 0;
        for (int offset = 1; offset <= 12; offset++) {
            int prev = (hour - offset + 24) % 24;
            int next = (hour + offset) % 24;
            if (hourCount[prev] > 0) {
                sum += hourOccupancySum[prev] / hourCount[prev];
                count++;
            }
            if (hourCount[next] > 0) {
                sum += hourOccupancySum[next] / hourCount[next];
                count++;
            }
            if (count >= 2) break;
        }
        if (count > 0) {
            return sum / count;
        }
        return 0.5;
    }

    // ==================== M5.3 收费趋势与报表导出 ====================

    /**
     * 获取收费趋势统计
     */
    public List<Map<String, Object>> getRevenueTrend(String period) {
        List<Fee> paidFees = feeRepository.findByStatus("PAID");
        if (paidFees.isEmpty()) {
            return List.of();
        }

        Map<String, List<Fee>> grouped;
        switch (period) {
            case "week":
                grouped = paidFees.stream()
                        .collect(Collectors.groupingBy(f -> {
                            LocalDate d = getFeeDate(f);
                            return d.with(java.time.DayOfWeek.MONDAY).toString();
                        }));
                break;
            case "month":
                grouped = paidFees.stream()
                        .collect(Collectors.groupingBy(f -> {
                            LocalDate d = getFeeDate(f);
                            return d.withDayOfMonth(1).toString();
                        }));
                break;
            default: // day
                grouped = paidFees.stream()
                        .collect(Collectors.groupingBy(f -> getFeeDate(f).toString()));
                break;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        List<String> sortedKeys = new ArrayList<>(grouped.keySet());
        Collections.sort(sortedKeys);

        for (String key : sortedKeys) {
            List<Fee> fees = grouped.get(key);
            BigDecimal total = fees.stream()
                    .map(f -> f.getTotalAmount() != null ? f.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long count = fees.size();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("period", key);
            item.put("totalAmount", total.setScale(2, RoundingMode.HALF_UP));
            item.put("count", count);
            result.add(item);
        }
        return result;
    }

    private LocalDate getFeeDate(Fee f) {
        return f.getPaymentTime() != null
                ? f.getPaymentTime().toLocalDate()
                : f.getCreatedAt().toLocalDate();
    }

    /**
     * 导出统计报表（Excel .xlsx格式）
     */
    public byte[] exportReport() {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Sheet1: 收费明细
            Sheet feeSheet = workbook.createSheet("收费明细");
            String[] feeHeaders = {"序号", "车牌号", "入场时间", "离场时间", "停车时长(小时)", "小时费率(元)", "费用金额(元)", "状态", "支付时间", "创建时间"};
            Row feeHeaderRow = feeSheet.createRow(0);
            for (int i = 0; i < feeHeaders.length; i++) {
                Cell cell = feeHeaderRow.createCell(i);
                cell.setCellValue(feeHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            List<Fee> allFees = feeRepository.findAll();
            int rowIdx = 1;
            for (Fee fee : allFees) {
                Row row = feeSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(nullToEmpty(fee.getPlateNumber()));
                row.createCell(2).setCellValue(fee.getEntryTime() != null ? fee.getEntryTime().toString() : "");
                row.createCell(3).setCellValue(fee.getExitTime() != null ? fee.getExitTime().toString() : "");
                row.createCell(4).setCellValue(fee.getParkingHours() != null ? fee.getParkingHours() : 0);
                row.createCell(5).setCellValue(fee.getHourlyRate() != null ? fee.getHourlyRate().doubleValue() : 0);
                row.createCell(6).setCellValue(fee.getTotalAmount() != null ? fee.getTotalAmount().doubleValue() : 0);
                row.createCell(7).setCellValue("PAID".equals(fee.getStatus()) ? "已支付" : "待支付");
                row.createCell(8).setCellValue(fee.getPaymentTime() != null ? fee.getPaymentTime().toString() : "");
                row.createCell(9).setCellValue(fee.getCreatedAt() != null ? fee.getCreatedAt().toString() : "");

                for (int j = 0; j < feeHeaders.length; j++) {
                    row.getCell(j).setCellStyle(dataStyle);
                }
            }
            for (int i = 0; i < feeHeaders.length; i++) {
                feeSheet.autoSizeColumn(i);
            }

            // Sheet2: 每日汇总
            Sheet dailySheet = workbook.createSheet("每日汇总");
            String[] dailyHeaders = {"日期", "收费笔数", "收费总额(元)"};
            Row dailyHeaderRow = dailySheet.createRow(0);
            for (int i = 0; i < dailyHeaders.length; i++) {
                Cell cell = dailyHeaderRow.createCell(i);
                cell.setCellValue(dailyHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            List<Map<String, Object>> dailyTrend = getRevenueTrend("day");
            int dailyRowIdx = 1;
            BigDecimal grandTotal = BigDecimal.ZERO;
            long grandCount = 0;
            for (Map<String, Object> item : dailyTrend) {
                Row row = dailySheet.createRow(dailyRowIdx++);
                row.createCell(0).setCellValue((String) item.get("period"));
                long count = ((Long) item.get("count")).intValue();
                row.createCell(1).setCellValue(count);
                double amount = ((BigDecimal) item.get("totalAmount")).doubleValue();
                row.createCell(2).setCellValue(amount);
                grandCount += count;
                grandTotal = grandTotal.add((BigDecimal) item.get("totalAmount"));

                for (int j = 0; j < dailyHeaders.length; j++) {
                    row.getCell(j).setCellStyle(dataStyle);
                }
            }

            Row totalRow = dailySheet.createRow(dailyRowIdx);
            CellStyle totalStyle = workbook.createCellStyle();
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            totalStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            for (int j = 0; j < dailyHeaders.length; j++) {
                totalRow.createCell(j).setCellStyle(totalStyle);
            }
            totalRow.getCell(0).setCellValue("合计");
            totalRow.getCell(1).setCellValue(grandCount);
            totalRow.getCell(2).setCellValue(grandTotal.setScale(2, RoundingMode.HALF_UP).doubleValue());
            for (int i = 0; i < dailyHeaders.length; i++) {
                dailySheet.autoSizeColumn(i);
            }

            // Sheet3: 营业概况
            Sheet summarySheet = workbook.createSheet("营业概况");
            Row s1 = summarySheet.createRow(0);
            s1.createCell(0).setCellValue("统计项目");
            s1.getCell(0).setCellStyle(headerStyle);
            s1.createCell(1).setCellValue("数值");
            s1.getCell(1).setCellStyle(headerStyle);

            Map<String, Object> summary = getTodaySummary();
            String[][] summaryData = {
                    {"今日营收", "¥" + summary.get("todayIncome")},
                    {"今日笔数", String.valueOf(summary.get("todayCount"))},
                    {"本周营收", "¥" + summary.get("weekIncome")},
                    {"本周笔数", String.valueOf(summary.get("weekCount"))},
                    {"本月营收", "¥" + summary.get("monthIncome")},
                    {"本月笔数", String.valueOf(summary.get("monthCount"))},
                    {"累计营收", "¥" + summary.get("totalIncome")},
            };
            for (int i = 0; i < summaryData.length; i++) {
                Row row = summarySheet.createRow(i + 1);
                row.createCell(0).setCellValue(summaryData[i][0]);
                row.createCell(1).setCellValue(summaryData[i][1]);
                for (int j = 0; j < 2; j++) {
                    row.getCell(j).setCellStyle(dataStyle);
                }
            }
            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出Excel报表失败: " + e.getMessage());
        }
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    // ==================== 今日/本周/本月营收概览 ====================

    public Map<String, Object> getTodaySummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);

        List<Fee> todayFees = feeRepository.findAll().stream()
                .filter(f -> "PAID".equals(f.getStatus())
                        && f.getPaymentTime() != null
                        && !f.getPaymentTime().isBefore(dayStart)
                        && !f.getPaymentTime().isAfter(dayEnd))
                .collect(Collectors.toList());
        BigDecimal todayIncome = sumAmounts(todayFees);

        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        List<Fee> weekFees = feeRepository.findAll().stream()
                .filter(f -> "PAID".equals(f.getStatus())
                        && f.getPaymentTime() != null
                        && !f.getPaymentTime().toLocalDate().isBefore(weekStart))
                .collect(Collectors.toList());
        BigDecimal weekIncome = sumAmounts(weekFees);

        LocalDate monthStart = today.withDayOfMonth(1);
        List<Fee> monthFees = feeRepository.findAll().stream()
                .filter(f -> "PAID".equals(f.getStatus())
                        && f.getPaymentTime() != null
                        && !f.getPaymentTime().toLocalDate().isBefore(monthStart))
                .collect(Collectors.toList());
        BigDecimal monthIncome = sumAmounts(monthFees);

        List<Fee> allPaid = feeRepository.findByStatus("PAID");
        BigDecimal totalIncome = sumAmounts(allPaid);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayIncome", todayIncome.setScale(2, RoundingMode.HALF_UP));
        result.put("todayCount", todayFees.size());
        result.put("weekIncome", weekIncome.setScale(2, RoundingMode.HALF_UP));
        result.put("weekCount", weekFees.size());
        result.put("monthIncome", monthIncome.setScale(2, RoundingMode.HALF_UP));
        result.put("monthCount", monthFees.size());
        result.put("totalIncome", totalIncome.setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    private BigDecimal sumAmounts(List<Fee> fees) {
        return fees.stream()
                .map(f -> f.getTotalAmount() != null ? f.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}