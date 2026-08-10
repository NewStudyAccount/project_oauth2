package com.example.authserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class CustomOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final JdbcTemplate jdbcTemplate;
    private final RegisteredClientRepository registeredClientRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void save(OAuth2Authorization authorization) {
        String state = authorization.getAttribute("state");
        String authorizationCodeValue = getTokenValue(authorization, "authorization_code");
        String accessTokenValue = getTokenValue(authorization, "access_token");
        String refreshTokenValue = getTokenValue(authorization, "refresh_token");
        String oidcIdTokenValue = getTokenValue(authorization, "id_token");

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
        String column = resolveColumn(tokenType);
        return findBy(column, token);
    }

    private OAuth2Authorization findBy(String column, String value) {
        try {
            return jdbcTemplate.query(
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
            builder.token(new OAuth2AuthorizationCode(authorizationCodeValue, Instant.now(), Instant.now().plusSeconds(300)),
                    meta -> {
                        if (authorizationCodeMetadata != null) meta.putAll(readMap(authorizationCodeMetadata));
                    });
        }

        if (accessTokenValue != null) {
            builder.token(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, accessTokenValue, Instant.now(), Instant.now().plusSeconds(1800)),
                    meta -> {
                        if (accessTokenMetadata != null) meta.putAll(readMap(accessTokenMetadata));
                    });
        }

        if (refreshTokenValue != null) {
            builder.token(new OAuth2RefreshToken(refreshTokenValue, Instant.now(), Instant.now().plusSeconds(604800)),
                    meta -> {
                        if (refreshTokenMetadata != null) meta.putAll(readMap(refreshTokenMetadata));
                    });
        }

        if (oidcIdTokenValue != null) {
            builder.token(new org.springframework.security.oauth2.core.oidc.OidcIdToken(oidcIdTokenValue, Instant.now(), Instant.now().plusSeconds(1800), Map.of()),
                    meta -> {
                        if (oidcIdTokenMetadata != null) meta.putAll(readMap(oidcIdTokenMetadata));
                    });
        }

        return builder.build();
    }

    private String resolveColumn(OAuth2TokenType tokenType) {
        if (tokenType == null) {
            return "state";
        }
        return switch (tokenType.getValue()) {
            case "authorization_code" -> "authorization_code_value";
            case "access_token" -> "access_token_value";
            case "refresh_token" -> "refresh_token_value";
            case "id_token" -> "oidc_id_token_value";
            default -> "state";
        };
    }

    private String getTokenValue(OAuth2Authorization authorization, String tokenType) {
        return Optional.ofNullable(authorization.getToken(tokenType))
                .map(token -> token.getToken().getTokenValue())
                .orElse(null);
    }

    private Map<String, Object> getTokenMetadata(OAuth2Authorization authorization, String tokenType) {
        return Optional.ofNullable(authorization.getToken(tokenType))
                .map(OAuth2Authorization.Token::getMetadata)
                .orElse(null);
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
}
