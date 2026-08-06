package com.example.authserver.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserInfoController {

    /**
     * OIDC UserInfo 端点
     * 参考: https://openid.net/specs/openid-connect-core-1_0.html#UserInfo
     */
    @GetMapping("/userinfo")
    public Map<String, Object> userinfo(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("sub", jwt.getSubject());
        userInfo.put("name", jwt.getClaimAsString("sub")); // 使用 username 作为 name
        userInfo.put("email", jwt.getClaimAsString("email"));
        userInfo.put("iss", jwt.getIssuer().toString());
        userInfo.put("aud", jwt.getAudience());
        return userInfo;
    }
}
