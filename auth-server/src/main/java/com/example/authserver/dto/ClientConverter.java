package com.example.authserver.dto;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * RegisteredClient ↔ ClientDTO 转换工具
 */
public final class ClientConverter {

    private ClientConverter() {}

    /**
     * RegisteredClient → ClientDTO（展开 JSON 字段为扁平字段）
     */
    public static ClientDTO toDTO(RegisteredClient client) {
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setClientId(client.getClientId());
        dto.setClientSecret(null); // 不暴露已加密的 secret
        dto.setClientIdIssuedAt(client.getClientIdIssuedAt());
        dto.setClientName(client.getClientName());

        // 认证方式
        dto.setClientAuthenticationMethods(
                client.getClientAuthenticationMethods().stream()
                        .map(ClientAuthenticationMethod::getValue)
                        .toList()
        );

        // 授权类型
        dto.setAuthorizationGrantTypes(
                client.getAuthorizationGrantTypes().stream()
                        .map(AuthorizationGrantType::getValue)
                        .toList()
        );

        // 重定向 URI
        dto.setRedirectUris(new ArrayList<>(client.getRedirectUris()));

        // Scopes
        dto.setScopes(new ArrayList<>(client.getScopes()));

        // ClientSettings
        ClientSettings clientSettings = client.getClientSettings();
        dto.setRequireProofKey(clientSettings.isRequireProofKey());
        dto.setRequireAuthorizationConsent(clientSettings.isRequireAuthorizationConsent());
        dto.setEnabled(clientSettings.getSetting("settings.client.enabled") != null
                ? (Boolean) clientSettings.getSetting("settings.client.enabled")
                : true);

        // TokenSettings
        TokenSettings tokenSettings = client.getTokenSettings();
        dto.setAccessTokenTtl(tokenSettings.getAccessTokenTimeToLive() != null
                ? tokenSettings.getAccessTokenTimeToLive().getSeconds() : 1800);
        dto.setRefreshTokenTtl(tokenSettings.getRefreshTokenTimeToLive() != null
                ? tokenSettings.getRefreshTokenTimeToLive().getSeconds() : 604800);
        dto.setAuthorizationCodeTtl(tokenSettings.getAuthorizationCodeTimeToLive() != null
                ? tokenSettings.getAuthorizationCodeTimeToLive().getSeconds() : 300);

        return dto;
    }

    /**
     * ClientDTO → RegisteredClient（使用 builder，框架保证 JSON 正确）
     */
    public static RegisteredClient toEntity(ClientDTO dto, PasswordEncoderWrapper passwordEncoderWrapper) {
        String id = dto.getId() != null ? dto.getId() : java.util.UUID.randomUUID().toString();
        RegisteredClient.Builder builder = RegisteredClient.withId(id)
                .clientId(dto.getClientId())
                .clientName(dto.getClientName() != null ? dto.getClientName() : dto.getClientId());

        // clientIdIssuedAt
        if (dto.getClientIdIssuedAt() != null) {
            builder.clientIdIssuedAt(dto.getClientIdIssuedAt());
        }

        // clientSecret（加密）
        if (dto.getClientSecret() != null && !dto.getClientSecret().isEmpty()) {
            builder.clientSecret(passwordEncoderWrapper.encode(dto.getClientSecret()));
        }

        // 认证方式
        if (dto.getClientAuthenticationMethods() != null) {
            for (String method : dto.getClientAuthenticationMethods()) {
                builder.clientAuthenticationMethod(new ClientAuthenticationMethod(method));
            }
        }

        // 授权类型
        if (dto.getAuthorizationGrantTypes() != null) {
            for (String grantType : dto.getAuthorizationGrantTypes()) {
                builder.authorizationGrantType(new AuthorizationGrantType(grantType));
            }
        }

        // 重定向 URI
        if (dto.getRedirectUris() != null) {
            for (String uri : dto.getRedirectUris()) {
                builder.redirectUri(uri);
            }
        }

        // Scopes
        if (dto.getScopes() != null) {
            for (String scope : dto.getScopes()) {
                builder.scope(scope);
            }
        }

        // TokenSettings
        builder.tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofSeconds(dto.getAccessTokenTtl() > 0 ? dto.getAccessTokenTtl() : 1800))
                .refreshTokenTimeToLive(Duration.ofSeconds(dto.getRefreshTokenTtl() > 0 ? dto.getRefreshTokenTtl() : 604800))
                .authorizationCodeTimeToLive(Duration.ofSeconds(dto.getAuthorizationCodeTtl() > 0 ? dto.getAuthorizationCodeTtl() : 300))
                .build());

        // ClientSettings
        builder.clientSettings(ClientSettings.builder()
                .requireProofKey(dto.isRequireProofKey())
                .requireAuthorizationConsent(dto.isRequireAuthorizationConsent())
                .setting("settings.client.enabled", dto.isEnabled())
                .build());

        return builder.build();
    }

    /**
     * 更新场景：基于现有 RegisteredClient 重建，保留未修改的 secret
     */
    public static RegisteredClient toEntityForUpdate(ClientDTO dto, RegisteredClient existing,
                                                      PasswordEncoderWrapper passwordEncoderWrapper) {
        // 如果 DTO 没有提供新 secret，保留原有的
        String secretToUse = (dto.getClientSecret() != null && !dto.getClientSecret().isEmpty())
                ? passwordEncoderWrapper.encode(dto.getClientSecret())
                : existing.getClientSecret();

        // 使用 DTO 字段重建，但保留 id 和 issuedAt
        ClientDTO merged = new ClientDTO();
        merged.setId(existing.getId());
        merged.setClientId(existing.getClientId());
        merged.setClientIdIssuedAt(existing.getClientIdIssuedAt());
        merged.setClientSecret(null); // 不传明文，下面单独设置
        merged.setClientName(dto.getClientName() != null ? dto.getClientName() : existing.getClientName());
        merged.setClientAuthenticationMethods(
                dto.getClientAuthenticationMethods() != null ? dto.getClientAuthenticationMethods()
                        : existing.getClientAuthenticationMethods().stream().map(ClientAuthenticationMethod::getValue).toList());
        merged.setAuthorizationGrantTypes(
                dto.getAuthorizationGrantTypes() != null ? dto.getAuthorizationGrantTypes()
                        : existing.getAuthorizationGrantTypes().stream().map(AuthorizationGrantType::getValue).toList());
        merged.setRedirectUris(
                dto.getRedirectUris() != null ? dto.getRedirectUris()
                        : new ArrayList<>(existing.getRedirectUris()));
        merged.setScopes(
                dto.getScopes() != null ? dto.getScopes()
                        : new ArrayList<>(existing.getScopes()));
        merged.setRequireProofKey(dto.isRequireProofKey());
        merged.setRequireAuthorizationConsent(dto.isRequireAuthorizationConsent());
        merged.setAccessTokenTtl(dto.getAccessTokenTtl());
        merged.setRefreshTokenTtl(dto.getRefreshTokenTtl());
        merged.setAuthorizationCodeTtl(dto.getAuthorizationCodeTtl());
        merged.setEnabled(dto.isEnabled());

        RegisteredClient rebuilt = toEntity(merged, passwordEncoderWrapper);

        // 如果没有新 secret，用回原有的
        if (dto.getClientSecret() == null || dto.getClientSecret().isEmpty()) {
            return RegisteredClient.from(rebuilt)
                    .clientSecret(secretToUse)
                    .build();
        }
        return rebuilt;
    }

    /**
     * 密码编码器包装，避免在 Converter 中直接依赖 Spring Bean
     */
    @FunctionalInterface
    public interface PasswordEncoderWrapper {
        String encode(String rawPassword);
    }
}