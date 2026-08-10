package com.example.authserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.authserver.config.JacksonConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 自定义 OAuth2AuthorizationService，使用 JDBC 存储
 * 使用 Spring Authorization Server 内置的表结构
 */
@Slf4j
@Service
public class CustomOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final JdbcTemplate jdbcTemplate;
    private final RegisteredClientRepository registeredClientRepository;
    private final ObjectMapper objectMapper;

    public CustomOAuth2AuthorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository,
            @Qualifier(JacksonConfig.OAUTH2_OBJECT_MAPPER) ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.registeredClientRepository = registeredClientRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        String state = authorization.getAttribute("state");
        String authorizationCodeValue = getTokenValue(authorization, "authorization_code");
        String accessTokenValue = getTokenValue(authorization, "access_token");
        String refreshTokenValue = getTokenValue(authorization, "refresh_token");
        String oidcIdTokenValue = getTokenValue(authorization, "id_token");

        log.debug("Saving authorization: id={}, clientId={}, principalName={}, grantType={}, hasAuthCode={}, hasAccessToken={}, hasRefreshToken={}",
                authorization.getId(),
                authorization.getRegisteredClientId(),
                authorization.getPrincipalName(),
                authorization.getAuthorizationGrantType().getValue(),
                authorizationCodeValue != null,
                accessTokenValue != null,
                refreshTokenValue != null);

        String attributes = writeMap(authorization.getAttributes());
        String authorizationCodeMetadata = writeMap(getTokenMetadata(authorization, "authorization_code"));
        String accessTokenMetadata = writeMap(getTokenMetadata(authorization, "access_token"));
        String refreshTokenMetadata = writeMap(getTokenMetadata(authorization, "refresh_token"));
        String oidcIdTokenMetadata = writeMap(getTokenMetadata(authorization, "id_token"));

        int count = jdbcTemplate.update(
                "UPDATE oauth2_authorization SET " +
                        "authorization_code_value = ?, authorization_code_metadata = ?, " +
                        "access_token_value = ?, access_token_metadata = ?, " +
                        "refresh_token_value = ?, refresh_token_metadata = ?, " +
                        "oidc_id_token_value = ?, oidc_id_token_metadata = ?, " +
                        "attributes = ?, state = ? " +
                        "WHERE id = ?",
                authorizationCodeValue, authorizationCodeMetadata,
                accessTokenValue, accessTokenMetadata,
                refreshTokenValue, refreshTokenMetadata,
                oidcIdTokenValue, oidcIdTokenMetadata,
                attributes, state,
                authorization.getId()
        );

        if (count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO oauth2_authorization (" +
                            "id, registered_client_id, principal_name, authorization_grant_type, " +
                            "authorized_scopes, attributes, state, " +
                            "authorization_code_value, authorization_code_metadata, " +
                            "access_token_value, access_token_metadata, " +
                            "refresh_token_value, refresh_token_metadata, " +
                            "oidc_id_token_value, oidc_id_token_metadata" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    authorization.getId(),
                    authorization.getRegisteredClientId(),
                    authorization.getPrincipalName(),
                    authorization.getAuthorizationGrantType().getValue(),
                    StringUtils.collectionToDelimitedString(authorization.getAuthorizedScopes(), ","),
                    attributes, state,
                    authorizationCodeValue, authorizationCodeMetadata,
                    accessTokenValue, accessTokenMetadata,
                    refreshTokenValue, refreshTokenMetadata,
                    oidcIdTokenValue, oidcIdTokenMetadata
            );
        }
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE id = ?", authorization.getId());
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return findBy("id", id);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        log.debug("findByToken called: tokenType={}, tokenValue={}", tokenType, token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null");
        String column = resolveColumn(tokenType);
        return findBy(column, token);
    }

    private OAuth2Authorization findBy(String column, String value) {
        try {
            log.debug("Looking up authorization by column={}, value={}", column, value);
            OAuth2Authorization result = jdbcTemplate.query(
                    "SELECT * FROM oauth2_authorization WHERE " + column + " = ?",
                    (ps) -> ps.setString(1, value),
                    (rs) -> {
                        try {
                            return rs.next() ? mapRow(rs) : null;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
            );

            // 如果按指定列没找到，且列不是 authorization_code_value，则尝试按 authorization_code_value 查找
            // 框架在某些情况下可能传递 null 的 tokenType，导致使用 state 列查找
            if (result == null && !"authorization_code_value".equals(column)) {
                log.debug("Not found by column={}, trying authorization_code_value", column);
                result = jdbcTemplate.query(
                        "SELECT * FROM oauth2_authorization WHERE authorization_code_value = ?",
                        (ps) -> ps.setString(1, value),
                        (rs) -> {
                            try {
                                return rs.next() ? mapRow(rs) : null;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
            }

            log.debug("Authorization lookup result: {}", result != null ? "found (id=" + result.getId() + ")" : "not found");
            return result;
        } catch (Exception e) {
            log.error("Error loading authorization by {}: {}", column, value, e);
        }
        return null;
    }

    private OAuth2Authorization mapRow(ResultSet rs) throws Exception {
        String id = rs.getString("id");
        String registeredClientId = rs.getString("registered_client_id");
        String principalName = rs.getString("principal_name");
        String authorizationGrantType = rs.getString("authorization_grant_type");
        String authorizedScopes = rs.getString("authorized_scopes");
        String attributes = rs.getString("attributes");
        String state = rs.getString("state");

        String authorizationCodeValue = rs.getString("authorization_code_value");
        String authorizationCodeMetadata = rs.getString("authorization_code_metadata");
        String accessTokenValue = rs.getString("access_token_value");
        String accessTokenMetadata = rs.getString("access_token_metadata");
        String refreshTokenValue = rs.getString("refresh_token_value");
        String refreshTokenMetadata = rs.getString("refresh_token_metadata");
        String oidcIdTokenValue = rs.getString("oidc_id_token_value");
        String oidcIdTokenMetadata = rs.getString("oidc_id_token_metadata");

        RegisteredClient registeredClient = registeredClientRepository.findById(registeredClientId);
        if (registeredClient == null) {
            return null;
        }

        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(id)
                .principalName(principalName)
                .authorizationGrantType(new AuthorizationGrantType(authorizationGrantType))
                .authorizedScopes(StringUtils.commaDelimitedListToSet(authorizedScopes))
                .attributes(attrs -> attrs.putAll(readMap(attributes)));

        if (state != null) {
            builder.attribute("state", state);
        }

        if (authorizationCodeValue != null) {
            Map<String, Object> authorizationCodeMetadataMap = readMap(authorizationCodeMetadata);
            Instant[] authorizationCodeTimes = extractTokenTimes(authorizationCodeMetadataMap, 300);
            builder.token(new OAuth2AuthorizationCode(authorizationCodeValue, authorizationCodeTimes[0], authorizationCodeTimes[1]),
                    meta -> {
                        if (authorizationCodeMetadataMap != null) meta.putAll(authorizationCodeMetadataMap);
                    });
        }

        if (accessTokenValue != null) {
            Map<String, Object> accessTokenMetadataMap = readMap(accessTokenMetadata);
            Instant[] accessTokenTimes = extractTokenTimes(accessTokenMetadataMap, 1800);
            builder.token(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, accessTokenValue, accessTokenTimes[0], accessTokenTimes[1]),
                    meta -> {
                        if (accessTokenMetadataMap != null) meta.putAll(accessTokenMetadataMap);
                    });
        }

        if (refreshTokenValue != null) {
            Map<String, Object> refreshTokenMetadataMap = readMap(refreshTokenMetadata);
            Instant[] refreshTokenTimes = extractTokenTimes(refreshTokenMetadataMap, 604800);
            builder.token(new OAuth2RefreshToken(refreshTokenValue, refreshTokenTimes[0], refreshTokenTimes[1]),
                    meta -> {
                        if (refreshTokenMetadataMap != null) meta.putAll(refreshTokenMetadataMap);
                    });
        }

        if (oidcIdTokenValue != null) {
            Map<String, Object> oidcIdTokenMetadataMap = readMap(oidcIdTokenMetadata);
            Instant[] oidcIdTokenTimes = extractTokenTimes(oidcIdTokenMetadataMap, 1800);
            builder.token(new org.springframework.security.oauth2.core.oidc.OidcIdToken(oidcIdTokenValue, oidcIdTokenTimes[0], oidcIdTokenTimes[1], Map.of()),
                    meta -> {
                        if (oidcIdTokenMetadataMap != null) meta.putAll(oidcIdTokenMetadataMap);
                    });
        }

        return builder.build();
    }

    private String resolveColumn(OAuth2TokenType tokenType) {
        if (tokenType == null) {
            log.debug("tokenType is null, returning state column");
            return "state";
        }
        String tokenValue = tokenType.getValue();
        log.debug("Resolving column for tokenType: class={}, value={}", tokenType.getClass().getName(), tokenValue);
        String column = switch (tokenValue) {
            case "code", "authorization_code" -> "authorization_code_value";
            case "access_token" -> "access_token_value";
            case "refresh_token" -> "refresh_token_value";
            case "id_token" -> "oidc_id_token_value";
            default -> {
                log.warn("Unknown token type value: '{}', falling back to state column", tokenValue);
                yield "state";
            }
        };
        log.debug("Resolved column: {} for tokenValue: {}", column, tokenValue);
        return column;
    }

    private String getTokenValue(OAuth2Authorization authorization, String tokenType) {
        OAuth2Authorization.Token<?> token = getToken(authorization, tokenType);
        return Optional.ofNullable(token)
                .map(t -> t.getToken().getTokenValue())
                .orElse(null);
    }

    private Map<String, Object> getTokenMetadata(OAuth2Authorization authorization, String tokenType) {
        OAuth2Authorization.Token<?> token = getToken(authorization, tokenType);
        return Optional.ofNullable(token)
                .map(OAuth2Authorization.Token::getMetadata)
                .orElse(null);
    }

    /**
     * 根据 token 类型获取 token。
     * Spring Authorization Server 内部使用类名作为 key，需要通过 class 方式获取。
     */
    private OAuth2Authorization.Token<?> getToken(OAuth2Authorization authorization, String tokenType) {
        return switch (tokenType) {
            case "authorization_code" -> authorization.getToken(OAuth2AuthorizationCode.class);
            case "access_token" -> authorization.getToken(OAuth2AccessToken.class);
            case "refresh_token" -> authorization.getToken(OAuth2RefreshToken.class);
            case "id_token" -> authorization.getToken(org.springframework.security.oauth2.core.oidc.OidcIdToken.class);
            default -> authorization.getToken(tokenType);
        };
    }

    private String writeMap(Map<String, ?> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Map<String, Object> readMap(String data) {
        try {
            if (data == null) return Map.of();
            return objectMapper.readValue(data, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 从 token metadata 中提取 issuedAt 和 expiresAt 时间
     * Spring Authorization Server 的 metadata 使用这些 key:
     * - "metadata.token.issued-at" -> Instant
     * - "metadata.token.expires-at" -> Instant
     */
    private Instant[] extractTokenTimes(Map<String, Object> metadataMap, long defaultExpiresInSeconds) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = Instant.now().plusSeconds(defaultExpiresInSeconds);

        if (metadataMap != null && !metadataMap.isEmpty()) {
            try {
                issuedAt = parseInstant(metadataMap.get("metadata.token.issued-at"), issuedAt);
                expiresAt = parseInstant(metadataMap.get("metadata.token.expires-at"), expiresAt);
            } catch (Exception e) {
                log.warn("Failed to extract token times from metadata, using defaults", e);
            }
        }

        return new Instant[]{issuedAt, expiresAt};
    }

    /**
     * 将对象解析为 Instant，支持 Instant / String / Number 类型
     */
    private Instant parseInstant(Object obj, Instant defaultVal) {
        if (obj == null) {
            return defaultVal;
        }
        if (obj instanceof Instant) {
            return (Instant) obj;
        }
        if (obj instanceof String) {
            return Instant.parse((String) obj);
        }
        if (obj instanceof Number) {
            return Instant.ofEpochSecond(((Number) obj).longValue());
        }
        // Jackson 可能序列化为数组 [epochSecond, nanoAdjustment]
        if (obj instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) obj;
            if (list.size() == 2 && list.get(0) instanceof Number && list.get(1) instanceof Number) {
                return Instant.ofEpochSecond(((Number) list.get(0)).longValue(), ((Number) list.get(1)).longValue());
            }
        }
        return defaultVal;
    }
}
