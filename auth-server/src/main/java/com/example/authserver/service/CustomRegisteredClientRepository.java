package com.example.authserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.authserver.entity.OAuth2Client;
import com.example.authserver.repository.OAuth2ClientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomRegisteredClientRepository implements RegisteredClientRepository {

    private final OAuth2ClientMapper oauth2ClientMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void save(RegisteredClient registeredClient) {
        // 由 ClientService 处理
    }

    @Override
    public RegisteredClient findById(String id) {
        OAuth2Client client = oauth2ClientMapper.selectOne(
                new LambdaQueryWrapper<OAuth2Client>()
                        .eq(OAuth2Client::getClientId, id)
                        .eq(OAuth2Client::getStatus, 1)
        );
        return client != null ? toRegisteredClient(client) : null;
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        OAuth2Client client = oauth2ClientMapper.selectOne(
                new LambdaQueryWrapper<OAuth2Client>()
                        .eq(OAuth2Client::getClientId, clientId)
                        .eq(OAuth2Client::getStatus, 1)
        );
        return client != null ? toRegisteredClient(client) : null;
    }

    private RegisteredClient toRegisteredClient(OAuth2Client client) {
        Set<String> scopeSet = new HashSet<>(Arrays.asList(client.getScopes().split(",")));

        RegisteredClient.Builder builder = RegisteredClient.withId(client.getClientId())
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .scopes(scopes -> scopes.addAll(scopeSet));

        // 认证方式
        if ("PUBLIC".equals(client.getClientType())) {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        } else {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
            if (client.getClientSecret() != null) {
                builder.clientSecret(client.getClientSecret());
            }
        }

        // 授权类型
        for (String grantType : client.getGrantTypes().split(",")) {
            switch (grantType.trim()) {
                case "authorization_code":
                    builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                    break;
                case "refresh_token":
                    builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                    break;
                case "client_credentials":
                    builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
                    break;
            }
        }

        // 重定向URI
        for (String uri : client.getRedirectUris().split(",")) {
            builder.redirectUri(uri.trim());
        }

        // Token 设置
        builder.tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofSeconds(client.getAccessTokenTtl()))
                .refreshTokenTimeToLive(Duration.ofSeconds(client.getRefreshTokenTtl()))
                .build());

        // 客户端设置
        builder.clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(client.getRequireConsent() == 1)
                .build());

        return builder.build();
    }
}
