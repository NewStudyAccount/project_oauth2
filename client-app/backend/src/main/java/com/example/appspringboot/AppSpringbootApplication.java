package com.example.appspringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients  // 启用 Feign 客户端（调用 user-service）
public class AppSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppSpringbootApplication.class, args);
    }
}
