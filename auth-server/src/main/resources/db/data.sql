-- =============================================
-- OAuth2 SSO 学习项目 - 测试数据
-- =============================================

USE oauth2_sso;

-- =============================================
-- 1. 用户数据 (密码均为 123456，BCrypt 加密)
-- =============================================

-- 插入角色
INSERT INTO sys_role (name) VALUES
    ('ROLE_USER'),
    ('ROLE_ADMIN');

-- 插入用户
-- BCrypt hash for "123456": $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
INSERT INTO sys_user (username, password, email, enabled) VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@example.com', TRUE),
    ('user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'user@example.com', TRUE);

-- 用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES
    (1, 1), -- admin -> ROLE_USER
    (1, 2), -- admin -> ROLE_ADMIN
    (2, 1); -- user -> ROLE_USER

-- =============================================
-- 2. OAuth2 客户端注册
-- =============================================

-- Vue SPA 客户端 (PKCE, 公开客户端, 无 secret)
INSERT INTO oauth2_registered_client (
    id, client_id, client_secret, client_name,
    client_authentication_methods, authorization_grant_types,
    redirect_uris, scopes, client_settings, token_settings
) VALUES (
    'vue-app-001',
    'vue-app',
    NULL,  -- 公开客户端，无 secret
    'Vue SPA 客户端',
    'none',  -- PKCE, 不需要客户端认证
    'authorization_code,refresh_token',
    'http://app-a.local:5173/callback',
    'openid,profile,email',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",1800.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}'
);

-- Spring Boot 客户端B (机密客户端, client_secret 方式)
-- client-b-secret BCrypt hash
INSERT INTO oauth2_registered_client (
    id, client_id, client_secret, client_name,
    client_authentication_methods, authorization_grant_types,
    redirect_uris, scopes, client_settings, token_settings
) VALUES (
    'client-b-001',
    'client-b',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    'Spring Boot 客户端B',
    'client_secret_basic',
    'authorization_code,refresh_token',
    'http://app-b.local:8082/login/oauth2/code/client-b',
    'openid,profile,email',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",1800.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}'
);
