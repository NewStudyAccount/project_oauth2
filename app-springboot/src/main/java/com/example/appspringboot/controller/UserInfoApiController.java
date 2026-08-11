package com.example.appspringboot.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户信息 API 端点示例
 * 从 JWT Token 中提取用户信息
 */
@RestController
@RequestMapping("/api/userinfo")
public class UserInfoApiController {

    @GetMapping
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> result = new HashMap<>();
        result.put("sub", jwt.getSubject());
        result.put("username", jwt.getClaimAsString("preferred_username"));
        result.put("email", jwt.getClaimAsString("email"));
        result.put("nickname", jwt.getClaimAsString("nickname"));
        result.put("tokenType", jwt.getTokenValue().substring(0, 20) + "...");
        return result;
    }

    @GetMapping("/claims")
    public Map<String, Object> getAllClaims(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> result = new HashMap<>();
        result.put("claims", jwt.getClaims());
        result.put("issuer", jwt.getIssuer());
        result.put("audience", jwt.getAudience());
        result.put("expiresAt", jwt.getExpiresAt());
        return result;
    }
}
