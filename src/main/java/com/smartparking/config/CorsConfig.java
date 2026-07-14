package com.smartparking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 允许所有域名访问（生产环境应该指定具体域名）
        config.addAllowedOriginPattern("*");

        // 允许的HTTP方法
        config.addAllowedMethod("*");

        // 允许的头部
        config.addAllowedHeader("*");

        // 允许凭证（如果需要）
        config.setAllowCredentials(true);

        // 预检请求的缓存时间（秒）
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
