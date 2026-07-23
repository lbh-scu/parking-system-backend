package com.smartparking.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强版车位占用率预测器
 *
 * 核心算法（无需外部库）：
 * 1. 按小时聚合历史数据，计算每个小时的基准占用率（hourBase）
 * 2. 用"同小时加权平均 + 邻近小时平滑"生成预测值
 * 3. 引入周期性衰减：未来越远的小时越趋近于整体均值（回归到平均）
 *
 * 效果：不同小时得到不同预测值，符合昼夜节律
 */
public class OccupancyPredictor {

    /** 每个小时的(原始占用率, 权重)历史数据 */
    private final Map<Integer, List<Double>> hourRateMap = new HashMap<>();

    /** 每个小时的基准占用率（训练后的均值） */
    private final Map<Integer, Double> hourBase = new HashMap<>();

    /** 全局平均占用率 */
    private double globalMean = 0.5;

    /** 训练样本总数 */
    private int trainingSize = 0;

    /** 均方误差 */
    private double mse = -1;

    /**
     * 训练模型
     * @param historyData 历史数据列表，每个元素为 { "hour": int, "rate": double }
     */
    public void train(List<Map<String, Object>> historyData) {
        hourRateMap.clear();
        hourBase.clear();
        trainingSize = 0;

        // Step 1: 按小时归类
        for (Map<String, Object> data : historyData) {
            Number hourObj = (Number) data.get("hour");
            Number rateObj = (Number) data.get("rate");
            if (hourObj != null && rateObj != null) {
                int hour = hourObj.intValue() % 24;
                double rate = Math.max(0, Math.min(1, rateObj.doubleValue()));
                hourRateMap.computeIfAbsent(hour, k -> new ArrayList<>()).add(rate);
                trainingSize++;
            }
        }

        // Step 2: 计算每个小时的基准占用率（截尾均值，去除异常值干扰）
        double sumAll = 0;
        int countAll = 0;
        for (Map.Entry<Integer, List<Double>> entry : hourRateMap.entrySet()) {
            int hour = entry.getKey();
            List<Double> rates = entry.getValue();
            if (rates.isEmpty()) continue;

            // 排序后取中间 60% 的均值（去掉最高20%和最低20%）
            List<Double> sorted = rates.stream().sorted().collect(Collectors.toList());
            int trim = Math.max(0, sorted.size() / 5);
            List<Double> trimmed = sorted.subList(trim, sorted.size() - trim);
            double avg = trimmed.stream().mapToDouble(Double::doubleValue).average().orElse(rates.get(0));
            hourBase.put(hour, avg);
            sumAll += avg * rates.size();
            countAll += rates.size();
        }

        // Step 3: 计算全局均值（作为回归基准）
        if (countAll > 0) {
            globalMean = sumAll / countAll;
        }

        // Step 4: 填补缺失小时（用相邻小时插值）
        for (int h = 0; h < 24; h++) {
            if (!hourBase.containsKey(h)) {
                double interpolated = interpolateHour(h);
                hourBase.put(h, interpolated);
            }
        }

        // Step 5: 计算 MSE
        if (trainingSize > 0) {
            mse = 0;
            int count = 0;
            for (Map<String, Object> data : historyData) {
                Number hourObj = (Number) data.get("hour");
                Number rateObj = (Number) data.get("rate");
                if (hourObj != null && rateObj != null) {
                    int hour = hourObj.intValue() % 24;
                    double actual = Math.max(0, Math.min(1, rateObj.doubleValue()));
                    double predicted = predictHour(hour);
                    mse += Math.pow(predicted - actual, 2);
                    count++;
                }
            }
            if (count > 0) mse /= count;
        }
    }

    /**
     * 用相邻小时插值填充缺失小时
     */
    private double interpolateHour(int hour) {
        // 向前向后各找最多 12 小时
        for (int offset = 1; offset <= 12; offset++) {
            int prev = (hour - offset + 24) % 24;
            int next = (hour + offset) % 24;
            Double prevVal = hourBase.get(prev);
            Double nextVal = hourBase.get(next);
            if (prevVal != null && nextVal != null) {
                // 用两边均值
                return (prevVal + nextVal) / 2.0;
            } else if (prevVal != null) {
                return prevVal;
            } else if (nextVal != null) {
                return nextVal;
            }
        }
        return globalMean;
    }

    /**
     * 预测指定小时的占用率（带邻近平滑）
     */
    private double predictHour(int hour) {
        if (trainingSize < 2) {
            return 0.5;
        }

        Double base = hourBase.get(hour);
        if (base == null) {
            base = globalMean;
        }

        // 邻近小时加权平滑（h-1, h, h+1 各 25%, 50%, 25%）
        Double prev = hourBase.get((hour - 1 + 24) % 24);
        Double next = hourBase.get((hour + 1) % 24);
        double smoothed = base;
        if (prev != null) smoothed = smoothed * 0.5 + prev * 0.25;
        if (next != null) smoothed = smoothed * 0.5 + next * 0.25;
        // 重新规整
        smoothed = smoothed * 0.5 + base * 0.5;

        // 加上微弱周期性偏差（模拟昼夜波动：凌晨低、午间高、晚上回落）
        // 用 sin 模拟：6点最低, 14-15点最高, 24点回落
        double periodicBias = 0.03 * Math.sin(Math.PI * (hour - 6) / 12);
        double result = smoothed + periodicBias;

        // 约束在 0~1
        return Math.max(0.05, Math.min(0.95, result));
    }

    /**
     * 预测指定小时的占用率（公开接口）
     */
    public double predict(int hour) {
        return predictHour(hour % 24);
    }

    /**
     * 预测未来 n 小时的占用率序列
     *
     * 衰减策略：未来越远的小时，预测值越趋近于全局均值（回归到平均）
     * 从第1小时无衰减(1.0)，到最后小时衰减到0.5
     */
    public List<Map<String, Object>> predictNextHours(int startHour, int n) {
        List<Map<String, Object>> predictions = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int hour = (startHour + i) % 24;
            double rawPrediction = predictHour(hour);

            // 平滑衰减：第1小时无衰减(1.0)，最后小时衰减到0.5
            double decayFactor = 1.0 - (0.5 * i / Math.max(n, 1));
            decayFactor = Math.max(0.5, Math.min(1.0, decayFactor));
            double finalPrediction = rawPrediction * decayFactor + globalMean * (1 - decayFactor);

            predictions.add(Map.of(
                    "hour", hour,
                    "predictedRate", Math.round(finalPrediction * 1000.0) / 1000.0
            ));
        }
        return predictions;
    }

    public double getMse() {
        return mse;
    }

    public int getTrainingSize() {
        return trainingSize;
    }

    public boolean isReady() {
        return trainingSize >= 2;
    }
}