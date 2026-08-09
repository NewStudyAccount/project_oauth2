package com.example.authserver.controller;

import com.example.authserver.entity.SysUser;
import com.example.authserver.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserInfoController {

    private final CustomUserDetailsService userDetailsService;

    @GetMapping("/userinfo")
    public Map<String, Object> userinfo(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        String username = jwt.getClaimAsString("username");

        if (username == null) {
            username = jwt.getSubject();
        }

        SysUser user = userDetailsService.getUserByUsername(username);

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
