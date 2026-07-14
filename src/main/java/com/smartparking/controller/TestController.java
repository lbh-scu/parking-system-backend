package com.smartparking.controller;

import com.smartparking.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/hello")
    public ApiResponse<String> hello() {
        return ApiResponse.success("✅ Spring Boot is running! Java version: " +
                System.getProperty("java.version"));
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("{\"status\":\"UP\",\"service\":\"smart-parking\"}");
    }
}