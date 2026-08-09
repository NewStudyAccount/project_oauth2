-- 初始数据

USE oauth2_center;

-- 默认管理员 (密码: Admin@123, BCrypt加密)
INSERT INTO sys_user (username, password, nickname, email, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin@example.com', 1)
ON DUPLICATE KEY UPDATE username=username;

-- 内部应用: Vue SPA (公开客户端, PKCE)
INSERT INTO oauth2_client (client_id, client_secret, client_name, client_type, scopes, grant_types, redirect_uris, require_consent, access_token_ttl, refresh_token_ttl)
VALUES ('vue-app', NULL, 'Vue前端应用', 'PUBLIC', 'openid,profile,email', 'authorization_code,refresh_token', 'http://localhost:5173/callback', 0, 1800, 604800)
ON DUPLICATE KEY UPDATE client_id=client_id;

-- 内部应用: Spring Boot (机密客户端)
INSERT INTO oauth2_client (client_id, client_secret, client_name, client_type, scopes, grant_types, redirect_uris, require_consent, access_token_ttl, refresh_token_ttl)
VALUES ('springboot-app', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'SpringBoot应用', 'CONFIDENTIAL', 'openid,profile,email', 'authorization_code,refresh_token', 'http://localhost:8082/login/oauth2/code/auth-server', 0, 1800, 604800)
ON DUPLICATE KEY UPDATE client_id=client_id;

-- 第三方应用示例 (需要用户确认授权)
INSERT INTO oauth2_client (client_id, client_secret, client_name, client_type, scopes, grant_types, redirect_uris, require_consent, access_token_ttl, refresh_token_ttl)
VALUES ('third-party-app', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '第三方示例应用', 'THIRD_PARTY', 'openid,profile', 'authorization_code', 'http://localhost:8080/callback', 1, 1800, 604800)
ON DUPLICATE KEY UPDATE client_id=client_id;

-- 默认管理员可以访问所有内部应用
INSERT INTO user_client_access (user_id, client_id, allowed)
SELECT u.id, c.client_id, 1
FROM sys_user u, oauth2_client c
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE allowed=1;
