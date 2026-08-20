package com.example.authserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 跨域（CORS）配置。
 *
 * <p>允许前端客户端（运行在不同域名/端口）访问授权服务器的 OAuth2 和 API 端点。
 * <p>仅对 /oauth2/**、/userinfo、/api/** 路径生效，其他路径不添加 CORS 头。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许的前端来源地址（两个客户端应用 + 授权服务器自身前端）
        configuration.setAllowedOrigins(List.of(
                "http://client.a.local:5173",   // 客户端 A（Vite 开发服务器）
                "http://client.b.local:5173",   // 客户端 B
                "http://auth.local:5174"        // 授权服务器管理前端
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);  // 允许携带 Cookie（Session 认证需要）
        configuration.setMaxAge(3600L);           // preflight 缓存 1 小时，减少 OPTIONS 请求

        // 按路径注册 CORS 配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/oauth2/**", configuration);  // OAuth2 协议端点
        source.registerCorsConfiguration("/userinfo", configuration);   // OIDC 用户信息端点
        source.registerCorsConfiguration("/api/**", configuration);     // 管理 API 端点
        return source;
    }
}