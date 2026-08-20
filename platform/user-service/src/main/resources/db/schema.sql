-- 用户中心数据库初始化脚本
-- 数据库: user_center

CREATE DATABASE IF NOT EXISTS user_center DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE user_center;

-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（登录标识，全局唯一）',
    password VARCHAR(200) NOT NULL COMMENT '密码（bcrypt 加密）',
    nickname VARCHAR(50) COMMENT '显示昵称',
    email VARCHAR(100) UNIQUE COMMENT '邮箱（全局唯一）',
    phone VARCHAR(20) COMMENT '手机号',
    status INT DEFAULT 1 COMMENT '账号状态：1=正常，0=禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';
