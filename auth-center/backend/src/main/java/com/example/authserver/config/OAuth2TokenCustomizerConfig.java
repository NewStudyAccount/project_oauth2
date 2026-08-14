package com.example.authserver.config;

import com.example.authserver.entity.SysUser;
import com.example.authserver.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class OAuth2TokenCustomizerConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue()) ||
                    "access_token".equals(context.getTokenType().getValue())) {

                Authentication principal = context.getPrincipal();
                if (principal != null && principal.getPrincipal() instanceof UserDetails userDetails) {
                    String username = userDetails.getUsername();
                    SysUser sysUser = userDetailsService.getUserByUsername(username);

                    context.getClaims().claims(claims -> {
                        claims.put("username", username);
                        if (sysUser != null) {
                            claims.put("nickname", sysUser.getNickname());
                            claims.put("email", sysUser.getEmail());
                            claims.put("phone", sysUser.getPhone());
                        }
                        // 添加 JTI 用于 Token 黑名单
                        claims.put("jti", UUID.randomUUID().toString());
                    });
                }
            }

            // 设置 ID Token 的 subject
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())
                    && context.getPrincipal() != null) {
                context.getClaims().claim(IdTokenClaimNames.SUB, context.getPrincipal().getName());
            }
        };
    }
}
