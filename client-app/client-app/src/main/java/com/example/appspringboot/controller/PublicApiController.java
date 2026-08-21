package com.example.appspringboot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 公开 API 端点示例
 * 不需要认证即可访问
 */
@RestController
@RequestMapping("/api/public")
public class PublicApiController {

    @GetMapping
    public Map<String, Object> publicEndpoint() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "This is a public API endpoint");
        result.put("timestamp", System.currentTimeMillis());
        result.put("status", "success");
        return result;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "app-springboot");
        return result;
    }
}
