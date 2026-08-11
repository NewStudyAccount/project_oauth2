-- 更新所有客户端的 client_secret 为正确的格式
-- 使用 {noop} 前缀表示明文密码

UPDATE oauth2_client
SET client_secret = '{noop}Admin@123'
WHERE client_id IN ('springboot-app', 'gateway-app', 'third-party-app');

-- 验证更新
SELECT client_id, client_secret FROM oauth2_client;
