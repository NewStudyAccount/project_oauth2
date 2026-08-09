package com.example.authserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

/**
 * 自定义 OAuth2AuthorizationConsentService，使用 JDBC 存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {

    private final JdbcTemplate jdbcTemplate;
    private final RegisteredClientRepository registeredClientRepository;

    @Override
    public void save(OAuth2AuthorizationConsent authorizationConsent) {
        RegisteredClient registeredClient = registeredClientRepository.findById(
                authorizationConsent.getRegisteredClientId());
        if (registeredClient == null) {
            throw new IllegalArgumentException("Registered client not found: " + authorizationConsent.getRegisteredClientId());
        }

        String authorities = StringUtils.collectionToDelimitedString(
                authorizationConsent.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList(), ",");

        int count = jdbcTemplate.update(
                "UPDATE oauth2_authorization_consent SET authorities = ? " +
                        "WHERE registered_client_id = ? AND principal_name = ?",
                authorities,
                authorizationConsent.getRegisteredClientId(),
                authorizationConsent.getPrincipalName()
        );

        if (count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO oauth2_authorization_consent (registered_client_id, principal_name, authorities) " +
                            "VALUES (?, ?, ?)",
                    authorizationConsent.getRegisteredClientId(),
                    authorizationConsent.getPrincipalName(),
                    authorities
            );
        }
    }

    @Override
    public void remove(OAuth2AuthorizationConsent authorizationConsent) {
        jdbcTemplate.update(
                "DELETE FROM oauth2_authorization_consent " +
                        "WHERE registered_client_id = ? AND principal_name = ?",
                authorizationConsent.getRegisteredClientId(),
                authorizationConsent.getPrincipalName()
        );
    }

    @Override
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        try {
            return jdbcTemplate.query(
                    "SELECT * FROM oauth2_authorization_consent " +
                            "WHERE registered_client_id = ? AND principal_name = ?",
                    (ps) -> {
                        ps.setString(1, registeredClientId);
                        ps.setString(2, principalName);
                    },
                    (rs) -> rs.next() ? mapRow(rs) : null
            );
        } catch (Exception e) {
            log.error("Error loading authorization consent", e);
            return null;
        }
    }

    private OAuth2AuthorizationConsent mapRow(ResultSet rs) throws Exception {
        String registeredClientId = rs.getString("registered_client_id");
        String principalName = rs.getString("principal_name");
        String authoritiesStr = rs.getString("authorities");

        Set<GrantedAuthority> authorities = new HashSet<>();
        if (StringUtils.hasText(authoritiesStr)) {
            for (String authority : authoritiesStr.split(",")) {
                authorities.add(new SimpleGrantedAuthority(authority));
            }
        }

        return OAuth2AuthorizationConsent.withId(registeredClientId, principalName)
                .authorities(auth -> auth.addAll(authorities))
                .build();
    }
}
