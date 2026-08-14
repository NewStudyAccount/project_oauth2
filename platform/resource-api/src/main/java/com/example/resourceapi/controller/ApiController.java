package com.example.resourceapi.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ApiController {

    @GetMapping("/api/profile")
    public Map<String, Object> profile(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("sub", jwt.getSubject());
        profile.put("username", jwt.getClaimAsString("username"));
        profile.put("nickname", jwt.getClaimAsString("nickname"));
        profile.put("email", jwt.getClaimAsString("email"));
        profile.put("phone", jwt.getClaimAsString("phone"));
        return profile;
    }

    @GetMapping("/api/resources")
    public Map<String, Object> resources(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> result = new HashMap<>();
        result.put("user", jwt.getClaimAsString("username"));
        result.put("resources", List.of(
                Map.of("id", 1, "name", "资源A", "type", "文档"),
                Map.of("id", 2, "name", "资源B", "type", "图片"),
                Map.of("id", 3, "name", "资源C", "type", "视频")
        ));
        return result;
    }
}
