package com.example.appspringboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 配置为 OAuth2 Client，指定使用 springboot-app 作为 client registration
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                )

                // 授权配置
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/error", "/public/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
