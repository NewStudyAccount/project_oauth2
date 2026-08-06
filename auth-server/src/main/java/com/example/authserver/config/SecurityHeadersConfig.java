package com.example.authserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

@Configuration
public class SecurityHeadersConfig {

    /**
     * 安全响应头配置 (集成到 SecurityFilterChain)
     * 注意: 实际配置在 SecurityConfig 中，这里作为说明文档
     *
     * 生产环境建议添加的响应头:
     * - X-Content-Type-Options: nosniff
     * - X-Frame-Options: DENY
     * - X-XSS-Protection: 1; mode=block
     * - Strict-Transport-Security (HSTS) - 仅 HTTPS
     * - Content-Security-Policy (CSP)
     * - Referrer-Policy: strict-origin-when-cross-origin
     */
}
