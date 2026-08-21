package com.example.appspringboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置 —— 纯 Resource Server（校验 JWT Bearer Token）。
 *
 * <p>前端通过 OAuth2 PKCE 流程从认证中心获取 Token，
 * 后端仅负责校验 Token，不参与登录流程。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/api/public/**", "/api/status", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                )
                .csrf(csrf -> csrf.disable());  // JWT 无状态认证，无需 CSRF

        return http.build();
    }
}
