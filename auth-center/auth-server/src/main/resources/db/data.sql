-- 初始数据

USE oauth2_center;

-- 内部应用: Vue SPA (公开客户端, PKCE)
INSERT INTO oauth2_registered_client (id, client_id, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, scopes, client_settings, token_settings)
VALUES (
    'vue-app-001',
    'vue-app',
    'Vue前端应用',
    'none',
    'authorization_code,refresh_token',
    'http://client.a.local:5173/callback',
    'openid,profile,email,offline_access',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false,"settings.client.enabled":true}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",1800.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}'
)
ON DUPLICATE KEY UPDATE client_name=client_name;

-- 内部应用: Spring Boot (公开客户端, PKCE)
INSERT INTO oauth2_registered_client (id, client_id, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, scopes, client_settings, token_settings)
VALUES (
    'springboot-app-001',
    'springboot-app',
    'SpringBoot应用',
    'none',
    'authorization_code,refresh_token',
    'http://client.a.local:5173/callback',
    'openid,profile,email,offline_access',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false,"settings.client.enabled":true}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",1800.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}'
)
ON DUPLICATE KEY UPDATE client_name=client_name;

-- 第三方应用示例 (需要用户确认授权)
INSERT INTO oauth2_registered_client (id, client_id, client_secret, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, scopes, client_settings, token_settings)
VALUES (
    'third-party-001',
    'third-party-app',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    '第三方示例应用',
    'client_secret_basic',
    'authorization_code',
    'http://localhost:8080/callback',
    'openid,profile',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":true,"settings.client.enabled":true}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",1800.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}'
)
ON DUPLICATE KEY UPDATE client_name=client_name;

-- 管理后台前端 (公开客户端, PKCE)
INSERT INTO oauth2_registered_client (id, client_id, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, scopes, client_settings, token_settings)
VALUES (
    'admin-frontend-001',
    'admin-frontend',
    '管理后台前端',
    'none',
    'authorization_code,refresh_token',
    'http://auth.local:5174/callback',
    'openid,profile,email,offline_access',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false,"settings.client.enabled":true}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",1800.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}'
)
ON DUPLICATE KEY UPDATE client_name=client_name;

-- 默认管理员可以访问所有内部应用
INSERT INTO user_client_access (user_id, client_id, allowed)
SELECT u.id, c.client_id, 1
FROM sys_user u, oauth2_registered_client c
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE allowed=1;
