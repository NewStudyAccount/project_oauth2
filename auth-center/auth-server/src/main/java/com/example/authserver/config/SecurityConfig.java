package com.example.authserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.util.Map;

/**
 * 安全配置。
 *
 * <p>包含两条过滤链：
 * <ul>
 *   <li>{@code @Order(2)} 默认链 —— 处理页面请求（/login, /consent 等），表单登录 + Session</li>
 *   <li>{@code @Order(3)} API 链 —— 处理 /api/** 请求，JWT Resource Server 认证</li>
 * </ul>
 * <p>OAuth2 协议端点由 {@link AuthorizationServerConfig#authServerFilterChain}（@Order(1)）处理。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * 默认安全过滤链（优先级 2）—— 处理页面端点（/login, /register, /consent 等）。
     * <p>使用表单登录 + Session 认证。
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")  // 排除 /api/** 由下面的链处理
                .authorizeHttpRequests(auth -> auth
                        // 公开路径：登录、注册、发送验证码、错误页、静态资源
                        .requestMatchers("/login", "/register", "/send-code", "/error", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        // 其他页面请求需要认证
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new LoginRedirectEntryPoint())
                )
                // 表单登录配置
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .failureUrl("/login?error")
                )
                // 登出配置
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                // CSRF：页面端点保留 CSRF，/oauth2/** 豁免（OAuth2 端点自带防护），/logout 豁免（登出无需防 CSRF）
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/oauth2/**", "/logout")
                );

        return http.build();
    }

    /**
     * API 安全过滤链（优先级 3）—— 处理 /api/** 请求。
     * <p>使用 JWT Resource Server 认证，无状态，无需 CSRF。
     */
    @Bean
    @Order(3)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                )
                // JWT 无状态认证，无需 CSRF
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * 页面请求的认证入口点 —— 未认证时重定向到登录页。
     */
    static class LoginRedirectEntryPoint implements AuthenticationEntryPoint {
        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                             AuthenticationException authException) throws IOException {
            response.sendRedirect(request.getContextPath() + "/login");
        }
    }
}
