package com.example.authserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseAuthorizationService implements OAuth2AuthorizationService {

    private final JdbcTemplate jdbcTemplate;
    private final RegisteredClientRepository registeredClientRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void save(OAuth2Authorization authorization) {
        OAuth2Authorization existing = findById(authorization.getId());
        if (existing != null) {
            updateAuthorization(authorization);
        } else {
            insertAuthorization(authorization);
        }
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        jdbcTemplate.update(
                "DELETE FROM oauth2_authorization WHERE id = ?",
                authorization.getId()
        );
    }

    @Override
    public OAuth2Authorization findById(String id) {
        List<OAuth2Authorization> results = jdbcTemplate.query(
                "SELECT * FROM oauth2_authorization WHERE id = ?",
                this::mapRow,
                id
        );
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        if (token == null || tokenType == null) {
            return null;
        }

        String column = getTokenColumn(tokenType);
        if (column == null) {
            return null;
        }

        List<OAuth2Authorization> results = jdbcTemplate.query(
                "SELECT * FROM oauth2_authorization WHERE " + column + " = ?",
                this::mapRow,
                token
        );
        return results.isEmpty() ? null : results.get(0);
    }

    private String getTokenColumn(OAuth2TokenType tokenType) {
        String value = tokenType.getValue();
        return switch (value) {
            case "code" -> "authorization_code_value";
            case "access_token" -> "access_token_value";
            case "id_token" -> "oidc_id_token_value";
            case "refresh_token" -> "refresh_token_value";
            case "state" -> "state";
            default -> null;
        };
    }

    private OAuth2Authorization mapRow(ResultSet rs, int rowNum) throws SQLException {
        String id = rs.getString("id");
        String registeredClientId = rs.getString("registered_client_id");
        String principalName = rs.getString("principal_name");
        String grantTypeName = rs.getString("authorization_grant_type");
        String authorizedScopesStr = rs.getString("authorized_scopes");
        String attributesStr = rs.getString("attributes");

        RegisteredClient registeredClient = registeredClientRepository.findById(registeredClientId);
        if (registeredClient == null) {
            return null;
        }

        Set<String> authorizedScopes = parseScopes(authorizedScopesStr);
        AuthorizationGrantType grantType = new AuthorizationGrantType(grantTypeName);

        Map<String, Object> attributes = deserializeMap(attributesStr);

        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(id)
                .principalName(principalName)
                .authorizationGrantType(grantType)
                .authorizedScopes(authorizedScopes)
                .attributes(attrs -> attrs.putAll(attributes));

        // Pre-read all token values and metadata from ResultSet
        String authCodeValue = rs.getString("authorization_code_value");
        String authCodeMetadata = rs.getString("authorization_code_metadata");
        String accessTokenValue = rs.getString("access_token_value");
        String accessTokenMetadata = rs.getString("access_token_metadata");
        String accessTokenScopes = rs.getString("access_token_scopes");
        String idTokenValue = rs.getString("oidc_id_token_value");
        String idTokenMetadata = rs.getString("oidc_id_token_metadata");
        String idTokenClaims = rs.getString("oidc_id_token_claims");
        String refreshTokenValue = rs.getString("refresh_token_value");
        String refreshTokenMetadata = rs.getString("refresh_token_metadata");

        // Authorization Code
        if (authCodeValue != null) {
            OAuth2AuthorizationCode authCode = new OAuth2AuthorizationCode(authCodeValue,
                    toInstant(rs.getTimestamp("authorization_code_issued_at")),
                    toInstant(rs.getTimestamp("authorization_code_expires_at")));
            builder.token(authCode, meta -> deserializeTokenMetadata(meta, authCodeMetadata));
        }

        // Access Token
        if (accessTokenValue != null) {
            Set<String> scopes = parseScopes(accessTokenScopes);
            OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, accessTokenValue,
                    toInstant(rs.getTimestamp("access_token_issued_at")),
                    toInstant(rs.getTimestamp("access_token_expires_at")),
                    scopes);
            builder.token(accessToken, meta -> deserializeTokenMetadata(meta, accessTokenMetadata));
        }

        // OIDC ID Token
        if (idTokenValue != null) {
            Map<String, Object> claims = deserializeMap(idTokenClaims);
            OidcIdToken idToken = new OidcIdToken(idTokenValue,
                    toInstant(rs.getTimestamp("oidc_id_token_issued_at")),
                    toInstant(rs.getTimestamp("oidc_id_token_expires_at")),
                    claims);
            builder.token(idToken, meta -> deserializeTokenMetadata(meta, idTokenMetadata));
        }

        // Refresh Token
        if (refreshTokenValue != null) {
            OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(refreshTokenValue,
                    toInstant(rs.getTimestamp("refresh_token_issued_at")),
                    toInstant(rs.getTimestamp("refresh_token_expires_at")));
            builder.token(refreshToken, meta -> deserializeTokenMetadata(meta, refreshTokenMetadata));
        }

        return builder.build();
    }

    private void insertAuthorization(OAuth2Authorization auth) {
        jdbcTemplate.update(
                "INSERT INTO oauth2_authorization (" +
                        "id, registered_client_id, principal_name, authorization_grant_type, " +
                        "authorized_scopes, attributes, state, " +
                        "authorization_code_value, authorization_code_issued_at, authorization_code_expires_at, authorization_code_metadata, " +
                        "access_token_value, access_token_issued_at, access_token_expires_at, access_token_metadata, access_token_type, access_token_scopes, " +
                        "oidc_id_token_value, oidc_id_token_issued_at, oidc_id_token_expires_at, oidc_id_token_metadata, oidc_id_token_claims, " +
                        "refresh_token_value, refresh_token_issued_at, refresh_token_expires_at, refresh_token_metadata" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                auth.getId(),
                auth.getRegisteredClientId(),
                auth.getPrincipalName(),
                auth.getAuthorizationGrantType().getValue(),
                serializeScopes(auth.getAuthorizedScopes()),
                serializeMap(auth.getAttributes()),
                auth.<String>getAttribute("state"),
                getTokenValue(auth, OAuth2AuthorizationCode.class),
                toTimestamp(getTokenIssuedAt(auth, OAuth2AuthorizationCode.class)),
                toTimestamp(getTokenExpiresAt(auth, OAuth2AuthorizationCode.class)),
                serializeTokenMetadata(auth, OAuth2AuthorizationCode.class),
                getTokenValue(auth, OAuth2AccessToken.class),
                toTimestamp(getTokenIssuedAt(auth, OAuth2AccessToken.class)),
                toTimestamp(getTokenExpiresAt(auth, OAuth2AccessToken.class)),
                serializeTokenMetadata(auth, OAuth2AccessToken.class),
                auth.getToken(OAuth2AccessToken.class) != null ? "Bearer" : null,
                auth.getToken(OAuth2AccessToken.class) != null ? serializeScopes(auth.getToken(OAuth2AccessToken.class).getToken().getScopes()) : null,
                getTokenValue(auth, OidcIdToken.class),
                toTimestamp(getTokenIssuedAt(auth, OidcIdToken.class)),
                toTimestamp(getTokenExpiresAt(auth, OidcIdToken.class)),
                serializeTokenMetadata(auth, OidcIdToken.class),
                auth.getToken(OidcIdToken.class) != null ? serializeMap(auth.getToken(OidcIdToken.class).getToken().getClaims()) : null,
                getTokenValue(auth, OAuth2RefreshToken.class),
                toTimestamp(getTokenIssuedAt(auth, OAuth2RefreshToken.class)),
                toTimestamp(getTokenExpiresAt(auth, OAuth2RefreshToken.class)),
                serializeTokenMetadata(auth, OAuth2RefreshToken.class)
        );
    }

    private void updateAuthorization(OAuth2Authorization auth) {
        jdbcTemplate.update(
                "UPDATE oauth2_authorization SET " +
                        "registered_client_id = ?, principal_name = ?, authorization_grant_type = ?, " +
                        "authorized_scopes = ?, attributes = ?, state = ?, " +
                        "authorization_code_value = ?, authorization_code_issued_at = ?, authorization_code_expires_at = ?, authorization_code_metadata = ?, " +
                        "access_token_value = ?, access_token_issued_at = ?, access_token_expires_at = ?, access_token_metadata = ?, access_token_type = ?, access_token_scopes = ?, " +
                        "oidc_id_token_value = ?, oidc_id_token_issued_at = ?, oidc_id_token_expires_at = ?, oidc_id_token_metadata = ?, oidc_id_token_claims = ?, " +
                        "refresh_token_value = ?, refresh_token_issued_at = ?, refresh_token_expires_at = ?, refresh_token_metadata = ? " +
                        "WHERE id = ?",
                auth.getRegisteredClientId(),
                auth.getPrincipalName(),
                auth.getAuthorizationGrantType().getValue(),
                serializeScopes(auth.getAuthorizedScopes()),
                serializeMap(auth.getAttributes()),
                auth.<String>getAttribute("state"),
                getTokenValue(auth, OAuth2AuthorizationCode.class),
                toTimestamp(getTokenIssuedAt(auth, OAuth2AuthorizationCode.class)),
                toTimestamp(getTokenExpiresAt(auth, OAuth2AuthorizationCode.class)),
                serializeTokenMetadata(auth, OAuth2AuthorizationCode.class),
                getTokenValue(auth, OAuth2AccessToken.class),
                toTimestamp(getTokenIssuedAt(auth, OAuth2AccessToken.class)),
                toTimestamp(getTokenExpiresAt(auth, OAuth2AccessToken.class)),
                serializeTokenMetadata(auth, OAuth2AccessToken.class),
                auth.getToken(OAuth2AccessToken.class) != null ? "Bearer" : null,
                auth.getToken(OAuth2AccessToken.class) != null ? serializeScopes(auth.getToken(OAuth2AccessToken.class).getToken().getScopes()) : null,
                getTokenValue(auth, OidcIdToken.class),
                toTimestamp(getTokenIssuedAt(auth, OidcIdToken.class)),
                toTimestamp(getTokenExpiresAt(auth, OidcIdToken.class)),
                serializeTokenMetadata(auth, OidcIdToken.class),
                auth.getToken(OidcIdToken.class) != null ? serializeMap(auth.getToken(OidcIdToken.class).getToken().getClaims()) : null,
                getTokenValue(auth, OAuth2RefreshToken.class),
                toTimestamp(getTokenIssuedAt(auth, OAuth2RefreshToken.class)),
                toTimestamp(getTokenExpiresAt(auth, OAuth2RefreshToken.class)),
                serializeTokenMetadata(auth, OAuth2RefreshToken.class),
                auth.getId()
        );
    }

    // --- Token helper methods ---

    private String resolveTokenType(Class<?> tokenClass) {
        if (OAuth2AuthorizationCode.class.isAssignableFrom(tokenClass)) return "code";
        if (OAuth2AccessToken.class.isAssignableFrom(tokenClass)) return "access_token";
        if (OidcIdToken.class.isAssignableFrom(tokenClass)) return "id_token";
        if (OAuth2RefreshToken.class.isAssignableFrom(tokenClass)) return "refresh_token";
        throw new IllegalArgumentException("Unsupported token class: " + tokenClass.getName());
    }

    private String getTokenValue(OAuth2Authorization auth, Class<?> tokenClass) {
        OAuth2Authorization.Token<?> token = auth.getToken(resolveTokenType(tokenClass));
        if (token == null) return null;
        return token.getToken().getTokenValue();
    }

    private Instant getTokenIssuedAt(OAuth2Authorization auth, Class<?> tokenClass) {
        OAuth2Authorization.Token<?> token = auth.getToken(resolveTokenType(tokenClass));
        if (token == null) return null;
        return token.getToken().getIssuedAt();
    }

    private Instant getTokenExpiresAt(OAuth2Authorization auth, Class<?> tokenClass) {
        OAuth2Authorization.Token<?> token = auth.getToken(resolveTokenType(tokenClass));
        if (token == null) return null;
        return token.getToken().getExpiresAt();
    }

    // --- Serialization helpers ---

    private String serializeMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to serialize map", e);
            return null;
        }
    }

    private Map<String, Object> deserializeMap(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize map", e);
            return Collections.emptyMap();
        }
    }

    private String serializeScopes(Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) return null;
        return String.join(",", scopes);
    }

    private Set<String> parseScopes(String scopesStr) {
        if (scopesStr == null || scopesStr.isEmpty()) return Collections.emptySet();
        return Set.of(scopesStr.split(","));
    }

    private String serializeTokenMetadata(OAuth2Authorization auth, Class<?> tokenClass) {
        OAuth2Authorization.Token<?> token = auth.getToken(resolveTokenType(tokenClass));
        if (token == null) return null;
        return serializeMap(new HashMap<>(token.getMetadata()));
    }

    private void deserializeTokenMetadata(Map<String, Object> metadata, String json) {
        if (json == null || json.isEmpty()) return;
        try {
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            metadata.putAll(parsed);
        } catch (Exception e) {
            log.error("Failed to deserialize token metadata", e);
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }
}
