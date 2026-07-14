package com.smartparking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartParkingApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartParkingApplication.class, args);
        System.out.println("🚀 智能停车管理系统后端启动成功！");
        System.out.println("📡 API地址: http://localhost:8080/api");
    }
}
