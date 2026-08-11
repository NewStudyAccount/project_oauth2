-- 更新 springboot-app 的 client_secret 为 BCrypt 格式
-- BCrypt 加密的 "Admin@123"

UPDATE oauth2_client
SET client_secret = '{bcrypt}$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi'
WHERE client_id = 'springboot-app';

-- 验证更新
SELECT client_id, client_secret FROM oauth2_client WHERE client_id = 'springboot-app';
