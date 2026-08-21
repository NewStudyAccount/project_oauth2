package com.example.authserver.controller;

import com.example.authserver.dto.UserDTO;
import com.example.authserver.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * OIDC UserInfo 端点 —— 返回当前认证用户的详细信息。
 *
 * <p>客户端通过 Access Token 调用此端点获取用户资料（GET /userinfo）。
 * <p>请求头需携带：{@code Authorization: Bearer <access_token>}
 * <p>用户信息通过 {@link CustomUserDetailsService} 从 user-service 获取。
 */
@RestController
@RequiredArgsConstructor
public class UserInfoController {

    private final CustomUserDetailsService userDetailsService;

    /**
     * 返回用户信息 —— 从 JWT 中提取用户名，再通过 user-service 获取完整资料。
     */
    @GetMapping("/userinfo")
    public Map<String, Object> userinfo(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        // 优先从自定义 claim 获取 username，降级使用 sub（subject）
        String username = jwt.getClaimAsString("username");

        if (username == null) {
            username = jwt.getSubject();
        }

        UserDTO user = userDetailsService.getUserByUsername(username);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("sub", jwt.getSubject());
        userInfo.put("username", username);

        if (user != null) {
            userInfo.put("nickname", user.getNickname());
            userInfo.put("email", user.getEmail());
            userInfo.put("phone", user.getPhone());
        }

        return userInfo;
    }
}
