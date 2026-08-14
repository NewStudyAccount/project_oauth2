-- OAuth2 SSO 统一认证中心数据库

CREATE DATABASE IF NOT EXISTS oauth2_center DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE oauth2_center;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(200) NOT NULL,
    nickname    VARCHAR(50),
    email       VARCHAR(100),
    phone       VARCHAR(20),
    status      TINYINT DEFAULT 1 COMMENT '1:正常 0:禁用',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 用户-系统访问权限表
CREATE TABLE IF NOT EXISTS user_client_access (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    client_id   VARCHAR(100) NOT NULL,
    allowed     TINYINT DEFAULT 1 COMMENT '1:允许 0:拒绝',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_client (user_id, client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-系统访问权限表';

-- Token 黑名单（主动撤销）
CREATE TABLE IF NOT EXISTS oauth2_token_blacklist (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    jti             VARCHAR(200) NOT NULL UNIQUE COMMENT 'JWT ID',
    token_value     VARCHAR(500),
    user_id         BIGINT,
    client_id       VARCHAR(100),
    reason          VARCHAR(100) COMMENT '撤销原因: admin_revoke, user_logout, password_changed',
    expires_at      DATETIME NOT NULL COMMENT '与原token同过期，过期后可清理',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_jti (jti),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Token黑名单';

-- 审计日志
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT,
    username        VARCHAR(50),
    client_id       VARCHAR(100),
    action          VARCHAR(50) NOT NULL COMMENT 'LOGIN, LOGOUT, AUTHORIZE, TOKEN_ISSUED, TOKEN_REVOKED, REGISTER, PASSWORD_CHANGED',
    detail          VARCHAR(500),
    ip              VARCHAR(50),
    user_agent      VARCHAR(500),
    status          VARCHAR(20) COMMENT 'SUCCESS, FAILED',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_action (action),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- 授权确认记录（第三方应用）
CREATE TABLE IF NOT EXISTS user_client_consent (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    client_id   VARCHAR(100) NOT NULL,
    scopes      VARCHAR(500),
    consented_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_client (user_id, client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户授权确认记录';

-- Webhook 订阅表
CREATE TABLE IF NOT EXISTS webhook_subscriber (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_id   VARCHAR(100) NOT NULL,
    event_type  VARCHAR(50) NOT NULL,
    callback_url VARCHAR(500) NOT NULL,
    secret      VARCHAR(200),
    status      TINYINT DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_client_event (client_id, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook订阅表';

-- Spring Authorization Server 内置 RegisteredClientRepository 所需表
CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id                            VARCHAR(100)  NOT NULL,
    client_id                     VARCHAR(100)  NOT NULL,
    client_id_issued_at           TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret                 VARCHAR(200)  DEFAULT NULL,
    client_secret_expires_at      TIMESTAMP     DEFAULT NULL,
    client_name                   VARCHAR(200)  NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types     VARCHAR(1000) NOT NULL,
    redirect_uris                 VARCHAR(1000) DEFAULT NULL,
    scopes                        VARCHAR(1000) NOT NULL,
    client_settings               VARCHAR(2000) NOT NULL,
    token_settings                VARCHAR(2000) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Spring Authorization Server 存储表
CREATE TABLE IF NOT EXISTS oauth2_authorization (
    id                            VARCHAR(100) NOT NULL,
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
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
    registered_client_id VARCHAR(100)  NOT NULL,
    principal_name       VARCHAR(200)  NOT NULL,
    authorities          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Webhook 发送日志
CREATE TABLE IF NOT EXISTS webhook_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscriber_id   BIGINT NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    payload         TEXT,
    status          VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, SUCCESS, FAILED, RETRYING',
    retry_count     INT DEFAULT 0,
    next_retry_at   DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status_retry (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook发送日志';
