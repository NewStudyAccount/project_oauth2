-- 添加 oauth2_registered_client 表
-- Spring Authorization Server 的 JdbcRegisteredClientRepository 需要这个表

CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id                            VARCHAR(100) NOT NULL,
    client_id                     VARCHAR(100) NOT NULL,
    client_id_issued_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret                 VARCHAR(200) DEFAULT NULL,
    client_secret_expires_at      TIMESTAMP DEFAULT NULL,
    client_name                   VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types     VARCHAR(1000) NOT NULL,
    redirect_uris                 VARCHAR(1000) DEFAULT NULL,
    post_logout_redirect_uris     VARCHAR(1000) DEFAULT NULL,
    scopes                        VARCHAR(1000) NOT NULL,
    client_settings               VARCHAR(2000) NOT NULL,
    token_settings                VARCHAR(2000) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入客户端数据
-- Vue SPA (公开客户端, PKCE)
INSERT INTO oauth2_registered_client (id, client_id, client_secret, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, scopes, client_settings, token_settings)
VALUES (
    'vue-app-001',
    'vue-app',
    NULL,
    'Vue前端应用',
    'none',
    'authorization_code,refresh_token',
    'http://client.a.local:5173/callback',
    'openid,profile,email',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",1800.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.core.OAuth2AccessTokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}'
)
ON DUPLICATE KEY UPDATE id=id;

-- Spring Boot 应用 (机密客户端)
INSERT INTO oauth2_registered_client (id, client_id, client_secret, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, scopes, client_settings, token_settings)
VALUES (
    'springboot-app-001',
    'springboot-app',
    '{noop}Admin@123',
    'SpringBoot应用',
    'client_secret_basic',
    'authorization_code,refresh_token',
    'http://client.a.local:8082/login/oauth2/code/springboot-app',
    'openid,profile,email',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",1800.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.core.OAuth2AccessTokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}'
)
ON DUPLICATE KEY UPDATE id=id;

-- Gateway 网关客户端
INSERT INTO oauth2_registered_client (id, client_id, client_secret, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, scopes, client_settings, token_settings)
VALUES (
    'gateway-app-001',
    'gateway-app',
    '{noop}gateway-secret',
    'Gateway网关',
    'client_secret_basic',
    'authorization_code,refresh_token',
    'http://gateway.local:8080/login/oauth2/code/gateway-client',
    'openid,profile,email',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",1800.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.core.OAuth2AccessTokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}'
)
ON DUPLICATE KEY UPDATE id=id;

-- 验证
SELECT 'oauth2_registered_client 表已创建' AS status;
SELECT client_id, client_name, client_secret FROM oauth2_registered_client;
