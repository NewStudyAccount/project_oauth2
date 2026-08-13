-- 迁移脚本: 将 oauth2_client 数据迁移到 oauth2_registered_client
-- 用于从自定义 DatabaseClientRepository 切换到内置 JdbcRegisteredClientRepository

USE oauth2_center;

-- 迁移数据

INSERT INTO oauth2_registered_client (
    id,
    client_id,
    client_id_issued_at,
    client_secret,
    client_secret_expires_at,
    client_name,
    client_authentication_methods,
    authorization_grant_types,
    redirect_uris,
    scopes,
    client_settings,
    token_settings
)
SELECT
    -- 使用 client_id 作为 id (JdbcRegisteredClientRepository 使用字符串 ID)
    client_id AS id,
    client_id,
    created_at AS client_id_issued_at,
    client_secret,
    NULL AS client_secret_expires_at,
    COALESCE(client_name, client_id) AS client_name,
    -- client_type 转换为 ClientAuthenticationMethod
    CASE
        WHEN client_type = 'CONFIDENTIAL' THEN 'client_secret_basic'
        ELSE 'none'
        END AS client_authentication_methods,
    -- grant_types 转换格式
    REPLACE(grant_types, ',', ',') AS authorization_grant_types,
    redirect_uris,
    scopes,
    -- client_settings JSON
    CONCAT(
            '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":',
            CASE WHEN client_type = 'PUBLIC' THEN 'true' ELSE 'false' END,
            ',"settings.client.require-authorization-consent":',
            CASE WHEN require_consent = 1 THEN 'true' ELSE 'false' END,
            '}'
    ) AS client_settings,
    -- token_settings JSON
    CONCAT(
            '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true',
            ',"settings.token.access-token-time-to-live":["java.time.Duration",', access_token_ttl, '.000000000]',
            ',"settings.token.refresh-token-time-to-live":["java.time.Duration",', refresh_token_ttl, '.000000000]',
            ',"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}'
    ) AS token_settings
FROM oauth2_client
WHERE status = 1
    ON DUPLICATE KEY UPDATE
                         client_secret = VALUES(client_secret),
                         client_name = VALUES(client_name),
                         client_authentication_methods = VALUES(client_authentication_methods),
                         authorization_grant_types = VALUES(authorization_grant_types),
                         redirect_uris = VALUES(redirect_uris),
                         scopes = VALUES(scopes),
                         client_settings = VALUES(client_settings),
                         token_settings = VALUES(token_settings);