-- 数据迁移脚本：从 oauth2_center.sys_user 迁移到 user_center.sys_user
-- 执行前请确保两个数据库都已创建

-- 1. 在 user_center 库创建 sys_user 表（如果尚未创建）
USE user_center;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    status INT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 从 oauth2_center 复制数据到 user_center
INSERT INTO user_center.sys_user (id, username, password, nickname, email, phone, status, created_at, updated_at)
SELECT id, username, password, nickname, email, phone, status, created_at, updated_at
FROM oauth2_center.sys_user;

-- 3. 验证迁移结果
SELECT COUNT(*) AS user_count FROM user_center.sys_user;
SELECT id, username, nickname, email, status FROM user_center.sys_user;
