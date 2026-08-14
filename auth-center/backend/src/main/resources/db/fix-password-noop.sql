-- 更新 springboot-app 的 client_secret 为明文格式
-- 与 application.yml 中的 {noop}Admin@123 一致

UPDATE oauth2_client
SET client_secret = '{noop}Admin@123'
WHERE client_id = 'springboot-app';

-- 验证更新
SELECT client_id, client_secret FROM oauth2_client WHERE client_id = 'springboot-app';
