package com.example.authserver.config;

import com.example.authserver.dto.UserDTO;
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

/**
 * OAuth2 JWT 令牌自定义配置。
 *
 * <p>在签发 ID Token 和 Access Token 时，向 JWT 载荷中添加自定义声明（claims）：
 * <ul>
 *   <li>username / nickname / email / phone —— 用户详细信息</li>
 *   <li>jti —— 令牌唯一标识，用于支持 Token 黑名单机制</li>
 *   <li>sub（ID Token）—— 设置 subject 为用户名</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class OAuth2TokenCustomizerConfig {

    private final CustomUserDetailsService userDetailsService;

    /**
     * JWT 令牌自定义器 —— 在令牌签发前修改 JWT 声明。
     * <p>仅对 ID Token 和 Access Token 生效，Refresh Token 不会包含这些自定义声明。
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            // 仅对 ID Token 和 Access Token 添加自定义声明
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue()) ||
                    "access_token".equals(context.getTokenType().getValue())) {

                Authentication principal = context.getPrincipal();
                if (principal != null && principal.getPrincipal() instanceof UserDetails userDetails) {
                    String username = userDetails.getUsername();
                    // 从数据库查询用户详细信息
                    UserDTO sysUser = userDetailsService.getUserByUsername(username);

                    context.getClaims().claims(claims -> {
                        claims.put("username", username);
                        if (sysUser != null) {
                            claims.put("nickname", sysUser.getNickname());
                            claims.put("email", sysUser.getEmail());
                            claims.put("phone", sysUser.getPhone());
                        }
                        // jti（JWT ID）用于 Token 黑名单：撤销 Token 时通过 jti 加入黑名单
                        claims.put("jti", UUID.randomUUID().toString());
                    });
                }
            }

            // ID Token 必须包含 sub（subject）声明，值为用户名
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())
                    && context.getPrincipal() != null) {
                context.getClaims().claim(IdTokenClaimNames.SUB, context.getPrincipal().getName());
            }
        };
    }
}
