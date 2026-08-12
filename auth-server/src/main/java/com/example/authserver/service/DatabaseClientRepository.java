package com.example.authserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.authserver.entity.OAuth2Client;
import com.example.authserver.repository.OAuth2ClientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseClientRepository implements RegisteredClientRepository {

    private final OAuth2ClientMapper clientMapper;

    @Override
    public void save(RegisteredClient registeredClient) {
        // 由管理后台 ClientService 处理，此处不直接实现框架级 save
        log.info("save() called for client: {} — delegated to ClientService", registeredClient.getClientId());
    }

    @Override
    public RegisteredClient findById(String id) {
        OAuth2Client entity = clientMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        return toRegisteredClient(entity);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        OAuth2Client entity = clientMapper.selectOne(
                new LambdaQueryWrapper<OAuth2Client>()
                        .eq(OAuth2Client::getClientId, clientId)
                        .eq(OAuth2Client::getStatus, 1)
        );
        if (entity == null) {
            return null;
        }
        return toRegisteredClient(entity);
    }

    private RegisteredClient toRegisteredClient(OAuth2Client entity) {
        RegisteredClient.Builder builder = RegisteredClient.withId(entity.getId().toString())
                .clientId(entity.getClientId())
                .clientName(entity.getClientName() != null ? entity.getClientName() : entity.getClientId());

        // client_secret（公开客户端可能为 null）
        if (entity.getClientSecret() != null && !entity.getClientSecret().isEmpty()) {
            builder.clientSecret(entity.getClientSecret());
        }

        // client_type → ClientAuthenticationMethod
        ClientAuthenticationMethod authMethod = toAuthMethod(entity.getClientType());
        builder.clientAuthenticationMethod(authMethod);

        // grant_types → AuthorizationGrantType
        if (entity.getGrantTypes() != null && !entity.getGrantTypes().isEmpty()) {
            for (String gt : entity.getGrantTypes().split(",")) {
                builder.authorizationGrantType(toGrantType(gt.trim()));
            }
        }

        // redirect_uris
        if (entity.getRedirectUris() != null && !entity.getRedirectUris().isEmpty()) {
            for (String uri : entity.getRedirectUris().split(",")) {
                builder.redirectUri(uri.trim());
            }
        }

        // scopes
        if (entity.getScopes() != null && !entity.getScopes().isEmpty()) {
            for (String scope : entity.getScopes().split(",")) {
                builder.scope(scope.trim());
            }
        }

        // TokenSettings
        TokenSettings.Builder tokenSettingsBuilder = TokenSettings.builder();
        if (entity.getAccessTokenTtl() != null && entity.getAccessTokenTtl() > 0) {
            tokenSettingsBuilder.accessTokenTimeToLive(Duration.ofSeconds(entity.getAccessTokenTtl()));
        }
        if (entity.getRefreshTokenTtl() != null && entity.getRefreshTokenTtl() > 0) {
            tokenSettingsBuilder.refreshTokenTimeToLive(Duration.ofSeconds(entity.getRefreshTokenTtl()));
        }
        builder.tokenSettings(tokenSettingsBuilder.build());

        // ClientSettings
        ClientSettings.Builder clientSettingsBuilder = ClientSettings.builder();
        if (entity.getRequireConsent() != null) {
            clientSettingsBuilder.requireAuthorizationConsent(entity.getRequireConsent() == 1);
        }
        // 公开客户端启用 PKCE
        if ("PUBLIC".equalsIgnoreCase(entity.getClientType())) {
            clientSettingsBuilder.requireProofKey(true);
        }
        builder.clientSettings(clientSettingsBuilder.build());

        return builder.build();
    }

    private ClientAuthenticationMethod toAuthMethod(String clientType) {
        if (clientType == null) {
            return ClientAuthenticationMethod.NONE;
        }
        return switch (clientType.toUpperCase()) {
            case "CONFIDENTIAL" -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
            case "PUBLIC" -> ClientAuthenticationMethod.NONE;
            default -> ClientAuthenticationMethod.NONE;
        };
    }

    private AuthorizationGrantType toGrantType(String grantType) {
        return switch (grantType.toLowerCase().trim()) {
            case "authorization_code" -> AuthorizationGrantType.AUTHORIZATION_CODE;
            case "refresh_token" -> AuthorizationGrantType.REFRESH_TOKEN;
            case "client_credentials" -> AuthorizationGrantType.CLIENT_CREDENTIALS;
            case "password" -> AuthorizationGrantType.PASSWORD;
            case "urn:ietf:params:oauth:grant-type:device_code" -> AuthorizationGrantType.DEVICE_CODE;
            default -> new AuthorizationGrantType(grantType.trim());
        };
    }
}
