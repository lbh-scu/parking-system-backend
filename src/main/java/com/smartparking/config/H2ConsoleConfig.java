package com.smartparking.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class H2ConsoleConfig {

    @Bean
    public ServletRegistrationBean<?> h2ConsoleServlet() {
        try {
            Class<?> servletClass = Class.forName("org.h2.server.web.JakartaWebServlet");
            ServletRegistrationBean<?> bean = new ServletRegistrationBean<>(
                    (jakarta.servlet.http.HttpServlet) servletClass.getDeclaredConstructor().newInstance()
            );
            bean.addUrlMappings("/h2-console/*");
            bean.setLoadOnStartup(1);
            return bean;
        } catch (Exception e) {
            throw new RuntimeException("H2 Console servlet registration failed", e);
        }
    }
}
