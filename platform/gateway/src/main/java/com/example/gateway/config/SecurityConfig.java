package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;

@Configuration
@EnableWebFluxSecurity  // 注意：这是响应式版本
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            // CSRF 配置（Gateway 通常禁用 CSRF）
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            // 授权配置
            .authorizeExchange(exchanges -> exchanges
                // 公开路径
                .pathMatchers(
                    "/",
                    "/login",
                    "/oauth2/**",
                    "/auth/**",
                    "/error",
                    "/actuator/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico"
                ).permitAll()
                // 其他路径需要认证
                .anyExchange().authenticated()
            )

            // OAuth2 登录配置
            .oauth2Login(oauth2 -> oauth2
                .authenticationSuccessHandler(
                    new RedirectServerAuthenticationSuccessHandler("/app"))
                .authenticationFailureHandler(
                    new RedirectServerAuthenticationFailureHandler("/error?login_failed"))
            )

            // OAuth2 客户端配置（用于 Token 中继）
            .oauth2Client(oauth2 -> {})

            // 头部配置（禁用 frame options，允许 iframe 嵌入）
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())
            );

        return http.build();
    }
}
