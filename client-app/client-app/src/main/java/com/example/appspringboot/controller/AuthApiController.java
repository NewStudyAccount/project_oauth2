package com.example.appspringboot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证状态 API。
 * <p>前端通过此端点检查当前 Token 是否有效。
 */
@RestController
@RequestMapping("/api")
public class AuthApiController {

    /**
     * 检查认证状态（前端用此端点判断 Token 是否有效）
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> result = new HashMap<>();
        if (jwt != null) {
            result.put("authenticated", true);
            result.put("name", jwt.getClaimAsString("username"));
        } else {
            result.put("authenticated", false);
        }
        return ResponseEntity.ok(result);
    }
}
