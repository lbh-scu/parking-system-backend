package com.smartparking.util;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于简单线性回归（Apache Commons Math3）的车位占用率预测器
 * 特征 = hourOfDay（小时），目标值 = 占用率（0~1）
 */
public class OccupancyPredictor {

    private final SimpleRegression regression = new SimpleRegression();

    /** 训练数据条目数 */
    private int trainingSize = 0;

    /** 均方误差（MSE） */
    private double mse = -1;

    /**
     * 训练模型
     * @param historyData 历史数据列表，每个元素为 { "hour": int, "rate": double }
     */
    public void train(List<Map<String, Object>> historyData) {
        regression.clear();
        trainingSize = 0;

        for (Map<String, Object> data : historyData) {
            Number hourObj = (Number) data.get("hour");
            Number rateObj = (Number) data.get("rate");
            if (hourObj != null && rateObj != null) {
                double hour = hourObj.doubleValue();
                double rate = rateObj.doubleValue();
                // 约束：占用率必须在 0~1 之间
                rate = Math.max(0, Math.min(1, rate));
                regression.addData(hour, rate);
                trainingSize++;
            }
        }

        // 计算 MSE
        if (trainingSize > 0) {
            mse = 0;
            for (Map<String, Object> data : historyData) {
                Number hourObj = (Number) data.get("hour");
                Number rateObj = (Number) data.get("rate");
                if (hourObj != null && rateObj != null) {
                    double predicted = regression.predict(hourObj.doubleValue());
                    double actual = Math.max(0, Math.min(1, rateObj.doubleValue()));
                    mse += Math.pow(predicted - actual, 2);
                }
            }
            mse /= trainingSize;
        }
    }

    /**
     * 预测指定小时的占用率
     * @param hour 小时（0~23）
     * @return 预测占用率（0~1之间）
     */
    public double predict(int hour) {
        if (trainingSize < 2) {
            // 数据不足时返回默认值
            return 0.5;
        }
        double result = regression.predict(hour);
        // 约束在 0~1 之间
        return Math.max(0, Math.min(1, result));
    }

    /**
     * 预测未来 n 小时的占用率序列
     * @param startHour 起始小时
     * @param n 预测小时数
     * @return 预测结果列表，每个元素为 { "hour": int, "predictedRate": double }
     */
    public List<Map<String, Object>> predictNextHours(int startHour, int n) {
        List<Map<String, Object>> predictions = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int hour = (startHour + i) % 24;
            double predictedRate = predict(hour);
            predictions.add(Map.of(
                    "hour", hour,
                    "predictedRate", Math.round(predictedRate * 1000.0) / 1000.0
            ));
        }
        return predictions;
    }

    /**
     * 获取模型精度评分（MSE）
     * @return 均方误差，-1表示尚未训练
     */
    public double getMse() {
        return mse;
    }

    /**
     * 获取训练样本数
     */
    public int getTrainingSize() {
        return trainingSize;
    }

    /**
     * 判断模型是否可用
     */
    public boolean isReady() {
        return trainingSize >= 2;
    }
}