package com.example.authserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseConsentService implements OAuth2AuthorizationConsentService {

    private final JdbcTemplate jdbcTemplate;
    private final RegisteredClientRepository registeredClientRepository;

    @Override
    public void save(OAuth2AuthorizationConsent consent) {
        OAuth2AuthorizationConsent existing = findById(
                consent.getRegisteredClientId(), consent.getPrincipalName());

        if (existing != null) {
            jdbcTemplate.update(
                    "UPDATE oauth2_authorization_consent SET authorities = ? " +
                            "WHERE registered_client_id = ? AND principal_name = ?",
                    serializeAuthorities(consent),
                    consent.getRegisteredClientId(),
                    consent.getPrincipalName()
            );
        } else {
            jdbcTemplate.update(
                    "INSERT INTO oauth2_authorization_consent (registered_client_id, principal_name, authorities) " +
                            "VALUES (?, ?, ?)",
                    consent.getRegisteredClientId(),
                    consent.getPrincipalName(),
                    serializeAuthorities(consent)
            );
        }
    }

    @Override
    public void remove(OAuth2AuthorizationConsent consent) {
        jdbcTemplate.update(
                "DELETE FROM oauth2_authorization_consent " +
                        "WHERE registered_client_id = ? AND principal_name = ?",
                consent.getRegisteredClientId(),
                consent.getPrincipalName()
        );
    }

    @Override
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        List<OAuth2AuthorizationConsent> results = jdbcTemplate.query(
                "SELECT * FROM oauth2_authorization_consent " +
                        "WHERE registered_client_id = ? AND principal_name = ?",
                this::mapRow,
                registeredClientId,
                principalName
        );
        return results.isEmpty() ? null : results.get(0);
    }

    private OAuth2AuthorizationConsent mapRow(ResultSet rs, int rowNum) {
        try {
            String registeredClientId = rs.getString("registered_client_id");
            String principalName = rs.getString("principal_name");
            String authoritiesStr = rs.getString("authorities");

            RegisteredClient registeredClient = registeredClientRepository.findById(registeredClientId);
            if (registeredClient == null) {
                return null;
            }

            OAuth2AuthorizationConsent.Builder builder = OAuth2AuthorizationConsent
                    .withId(registeredClientId, principalName);

            if (authoritiesStr != null && !authoritiesStr.isEmpty()) {
                for (String authority : authoritiesStr.split(",")) {
                    String trimmed = authority.trim();
                    if (!trimmed.isEmpty()) {
                        builder.authority(new SimpleGrantedAuthority(trimmed));
                    }
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Failed to map consent row", e);
            return null;
        }
    }

    private String serializeAuthorities(OAuth2AuthorizationConsent consent) {
        if (consent.getAuthorities() == null || consent.getAuthorities().isEmpty()) {
            return "";
        }
        List<String> authorityStrings = new ArrayList<>();
        consent.getAuthorities().forEach(a -> authorityStrings.add(a.getAuthority()));
        return String.join(",", authorityStrings);
    }
}
