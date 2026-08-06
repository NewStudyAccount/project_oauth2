-- =============================================
-- OAuth2 SSO 学习项目 - 数据库初始化脚本
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS oauth2_sso DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE oauth2_sso;

-- =============================================
-- 1. 用户系统表
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(200) NOT NULL COMMENT '密码 (BCrypt)',
    email       VARCHAR(100) COMMENT '邮箱',
    enabled     BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称 (ROLE_USER, ROLE_ADMIN)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- =============================================
-- 2. Spring Authorization Server 必需表
-- 参考: https://docs.spring.io/spring-authorization-server/reference/getting-started.html
-- =============================================

-- 客户端注册表
CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id                            VARCHAR(100) PRIMARY KEY,
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
    UNIQUE KEY uk_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2 客户端注册表';

-- 授权记录表
CREATE TABLE IF NOT EXISTS oauth2_authorization (
    id                            VARCHAR(100) PRIMARY KEY,
    registered_client_id          VARCHAR(100) NOT NULL,
    principal_name                VARCHAR(200) NOT NULL,
    authorization_grant_type      VARCHAR(100) NOT NULL,
    authorized_scopes             VARCHAR(1000) DEFAULT NULL,
    attributes                    TEXT DEFAULT NULL,
    state                         VARCHAR(500) DEFAULT NULL,
    authorization_code_value      TEXT DEFAULT NULL,
    authorization_code_issued_at  TIMESTAMP DEFAULT NULL,
    authorization_code_expires_at TIMESTAMP DEFAULT NULL,
    authorization_code_metadata   TEXT DEFAULT NULL,
    access_token_value            TEXT DEFAULT NULL,
    access_token_issued_at        TIMESTAMP DEFAULT NULL,
    access_token_expires_at       TIMESTAMP DEFAULT NULL,
    access_token_metadata         TEXT DEFAULT NULL,
    access_token_type             VARCHAR(100) DEFAULT NULL,
    access_token_scopes           VARCHAR(1000) DEFAULT NULL,
    oidc_id_token_value           TEXT DEFAULT NULL,
    oidc_id_token_issued_at       TIMESTAMP DEFAULT NULL,
    oidc_id_token_expires_at      TIMESTAMP DEFAULT NULL,
    oidc_id_token_metadata        TEXT DEFAULT NULL,
    oidc_id_token_claims          TEXT DEFAULT NULL,
    refresh_token_value           TEXT DEFAULT NULL,
    refresh_token_issued_at       TIMESTAMP DEFAULT NULL,
    refresh_token_expires_at      TIMESTAMP DEFAULT NULL,
    refresh_token_metadata        TEXT DEFAULT NULL,
    user_code_value               TEXT DEFAULT NULL,
    user_code_issued_at           TIMESTAMP DEFAULT NULL,
    user_code_expires_at          TIMESTAMP DEFAULT NULL,
    user_code_metadata            TEXT DEFAULT NULL,
    device_code_value             TEXT DEFAULT NULL,
    device_code_issued_at         TIMESTAMP DEFAULT NULL,
    device_code_expires_at        TIMESTAMP DEFAULT NULL,
    device_code_metadata          TEXT DEFAULT NULL,
    FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2 授权记录表';

-- 授权同意表
CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name       VARCHAR(200) NOT NULL,
    authorities          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name),
    FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2 授权同意表';
