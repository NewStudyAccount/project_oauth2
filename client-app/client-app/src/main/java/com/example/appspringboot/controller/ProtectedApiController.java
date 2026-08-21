package com.example.appspringboot.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 受保护 API 端点示例
 * 需要有效的 JWT Token 才能访问
 */
@RestController
@RequestMapping("/api/protected")
public class ProtectedApiController {

    @GetMapping
    public Map<String, Object> protectedEndpoint(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "This is a protected API endpoint");
        result.put("timestamp", System.currentTimeMillis());
        result.put("status", "success");
        result.put("user", jwt.getSubject());
        return result;
    }

    @GetMapping("/data")
    public Map<String, Object> getProtectedData(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> result = new HashMap<>();
        result.put("data", "This is protected data");
        result.put("requestedBy", jwt.getSubject());
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
