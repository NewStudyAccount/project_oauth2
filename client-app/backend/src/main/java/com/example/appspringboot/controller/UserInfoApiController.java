package com.example.appspringboot.controller;

import com.example.appspringboot.client.UserServiceClient;
import com.example.appspringboot.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户信息 API 端点。
 * <p>从 JWT 提取 username，调用 user-service 获取完整用户资料。
 */
@RestController
@RequestMapping("/api/userinfo")
@RequiredArgsConstructor
public class UserInfoApiController {

    private final UserServiceClient userServiceClient;

    @GetMapping
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> result = new HashMap<>();

        String username = jwt.getClaimAsString("username");
        if (username == null) {
            username = jwt.getSubject();
        }

        result.put("sub", jwt.getSubject());
        result.put("username", username);
        result.put("authType", "bearer");

        // 调用 user-service 获取完整用户信息
        try {
            UserDTO user = userServiceClient.getUserByUsername(username);
            if (user != null) {
                result.put("nickname", user.getNickname());
                result.put("email", user.getEmail());
                result.put("phone", user.getPhone());
            }
        } catch (Exception e) {
            // user-service 不可用时，降级使用 JWT claims
            result.put("email", jwt.getClaimAsString("email"));
            result.put("nickname", jwt.getClaimAsString("nickname"));
            result.put("fallback", true);
        }

        return result;
    }

    @GetMapping("/claims")
    public Map<String, Object> getAllClaims(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> result = new HashMap<>();
        result.put("claims", jwt.getClaims());
        result.put("issuer", jwt.getIssuer());
        result.put("audience", jwt.getAudience());
        result.put("expiresAt", jwt.getExpiresAt());
        result.put("authType", "bearer");
        return result;
    }
}
