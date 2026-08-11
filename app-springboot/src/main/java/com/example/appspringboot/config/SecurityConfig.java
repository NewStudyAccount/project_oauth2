package com.example.appspringboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（Resource Server 通常不需要）
                .csrf(csrf -> csrf.disable())

                // 配置为 Resource Server
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {})
                )

                // 授权配置
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error", "/public/**").permitAll()
                        .anyRequest().authenticated()
                )

                // 无状态 Session（Resource Server 不需要 Session）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 禁用 OAuth2 Login（由 Gateway 处理）
                // .oauth2Login(...) - 移除

                // 禁用 logout（Resource Server 无状态）
                .logout(logout -> logout.disable());

        return http.build();
    }
}
