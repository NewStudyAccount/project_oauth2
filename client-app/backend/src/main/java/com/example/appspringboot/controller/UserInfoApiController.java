package com.example.appspringboot.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户信息 API 端点示例
 * 支持两种认证方式:
 * - Bearer Token (JWT) — @AuthenticationPrincipal Jwt
 * - Session (OAuth2 Login) — OAuth2AuthenticationToken
 */
@RestController
@RequestMapping("/api/userinfo")
public class UserInfoApiController {

    @GetMapping
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal Object principal) {
        Map<String, Object> result = new HashMap<>();

        if (principal instanceof Jwt jwt) {
            // Bearer Token 方式
            result.put("sub", jwt.getSubject());
            result.put("username", jwt.getClaimAsString("preferred_username"));
            result.put("email", jwt.getClaimAsString("email"));
            result.put("nickname", jwt.getClaimAsString("nickname"));
            result.put("authType", "bearer");
        } else if (principal instanceof OidcUser oidcUser) {
            // Session (OAuth2 Login) 方式
            result.put("sub", oidcUser.getSubject());
            result.put("username", oidcUser.getPreferredUsername());
            result.put("email", oidcUser.getEmail());
            result.put("nickname", oidcUser.getNickName());
            result.put("authType", "session");
        } else {
            result.put("error", "unauthenticated");
        }

        return result;
    }

    @GetMapping("/claims")
    public Map<String, Object> getAllClaims(@AuthenticationPrincipal Object principal) {
        Map<String, Object> result = new HashMap<>();

        if (principal instanceof Jwt jwt) {
            result.put("claims", jwt.getClaims());
            result.put("issuer", jwt.getIssuer());
            result.put("audience", jwt.getAudience());
            result.put("expiresAt", jwt.getExpiresAt());
            result.put("authType", "bearer");
        } else if (principal instanceof OidcUser oidcUser) {
            result.put("claims", oidcUser.getClaims());
            result.put("issuer", oidcUser.getIssuer());
            result.put("audience", oidcUser.getAudience());
            result.put("authType", "session");
        } else {
            result.put("error", "unauthenticated");
        }

        return result;
    }
}