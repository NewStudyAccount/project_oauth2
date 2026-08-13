# Proposal: OAuth2 SSO 统一认证平台

## Summary

构建基于 OAuth2/OIDC 的统一认证平台，实现企业内部多系统（OA、CRM 等）单点登录，同时支持第三方应用通过 OAuth2 授权接入。用户身份统一管理，业务权限各系统自治。

## Problem Statement

当前各系统（OA、CRM）各自维护独立的用户体系，存在以下问题：

1. **用户体验差**：每个系统单独登录，密码各管各的
2. **管理成本高**：新增用户要在每个系统都创建一遍
3. **安全风险**：密码分散存储，无法统一管控（禁用、强制定期修改）
4. **无法接入第三方**：没有标准化的授权协议，第三方应用无法安全接入

## Goals

### 核心目标
- [ ] 统一用户身份：一套账号密码，所有系统通行
- [ ] 单点登录（SSO）：一次登录，处处访问
- [ ] 权限隔离：用户能访问哪些系统，由管理员控制
- [ ] 第三方接入：支持外部应用通过 OAuth2 标准流程接入

### 学习目标
- [ ] 深入理解 OAuth2 四种授权模式
- [ ] 理解 OIDC（OpenID Connect）身份层
- [ ] 掌握 PKCE 机制（公开客户端安全方案）
- [ ] 理解 JWT 结构与验签机制

## Non-goals

- 不做微服务网关集成（后续独立项目）
- 不做多租户支持
- 不做 LDAP/AD 对接
- 不做社交登录（微信、GitHub 等）

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          系统架构                                       │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    Auth Server (:9000)                           │   │
│  │                                                                 │   │
│  │   Spring Authorization Server + Spring Security                 │   │
│  │   ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐      │   │
│  │   │ 用户管理  │  │ 客户端管理│  │ 会话管理  │  │ Webhook  │      │   │
│  │   └──────────┘  └──────────┘  └──────────┘  └──────────┘      │   │
│  │                                                                 │   │
│  │   端点:                                                        │   │
│  │   /oauth2/authorize  /oauth2/token  /oauth2/jwks               │   │
│  │   /userinfo  /.well-known/openid-configuration                 │   │
│  │   /login  /logout  /webhook/*                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│            │                    │                    │                  │
│            │                    │                    │                  │
│  ┌─────────▼──────┐  ┌─────────▼──────┐  ┌─────────▼──────┐          │
│  │    app-vue      │  │  app-springboot │  │  resource-api  │          │
│  │   (Vue SPA)     │  │  (后端应用)     │  │  (资源服务器)  │          │
│  │                 │  │                 │  │                │          │
│  │  公开客户端     │  │  机密客户端      │  │  验证JWT       │          │
│  │  PKCE 模式      │  │  授权码模式      │  │  返回业务数据  │          │
│  │  :5173          │  │  :8082          │  │  :8083         │          │
│  └─────────────────┘  └─────────────────┘  └────────────────┘          │
│                                                                         │
│  ┌─────────────────┐  ┌─────────────────┐                              │
│  │     MySQL        │  │     Redis        │                              │
│  │  用户/客户端/    │  │  Token缓存       │                              │
│  │  授权码/权限     │  │  Session存储     │                              │
│  └─────────────────┘  └─────────────────┘                              │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## Technical Design

### Tech Stack

| 组件 | 技术选型 | 版本 |
|------|----------|------|
| Auth Server | Spring Boot + Spring Authorization Server | 3.x |
| 安全框架 | Spring Security | 6.x |
| 数据库 | MySQL 8.0 | 8.0+ |
| 缓存 | Redis | 7.x |
| ORM | MyBatis-Plus | 3.5.x |
| 前端 | Vue 3 + Vite + Pinia | 3.4+ |
| JWT | Nimbus JOSE JWT | 9.x |

### 模块划分

```
project_oauth2/
├── auth-server/                 # 认证中心（前后端不分离，Thymeleaf 模板）
│   ├── src/main/java/
│   │   ├── config/
│   │   │   ├── AuthorizationServerConfig.java   # OAuth2/OIDC 配置
│   │   │   └── SecurityConfig.java              # 安全过滤链
│   │   ├── entity/
│   │   │   ├── SysUser.java                     # 用户实体
│   │   │   ├── OAuth2Client.java               # 客户端实体
│   │   │   ├── UserClientAccess.java           # 用户-系统访问权限
│   │   │   └── WebhookSubscriber.java          # Webhook订阅
│   │   ├── repository/                          # MyBatis Mapper
│   │   ├── service/
│   │   │   ├── CustomUserDetailsService.java   # 用户认证
│   │   │   ├── ClientService.java              # 客户端管理
│   │   │   ├── TokenBlacklistService.java      # Token撤销/黑名单
│   │   │   ├── AuditLogService.java            # 审计日志
│   │   │   ├── RegisterService.java            # 用户注册
│   │   │   └── WebhookService.java             # Webhook发送
│   │   ├── controller/
│   │   │   ├── LoginController.java            # 登录/注册页面
│   │   │   ├── UserInfoController.java         # /userinfo 端点
│   │   │   └── WebhookController.java          # Webhook管理API
│   │   └── GlobalExceptionHandler.java
│   └── src/main/resources/
│       ├── application.yml
│       ├── templates/                           # Thymeleaf 页面
│       │   ├── login.html                      # 登录页
│       │   ├── register.html                   # 注册页
│       │   ├── consent.html                    # 授权确认页（第三方）
│       │   └── error.html                      # 错误页
│       └── db/
│           ├── schema.sql                      # 建表语句
│           └── data.sql                        # 初始数据
│
├── app-vue/                     # Vue SPA 前端（公开客户端）
│   ├── src/
│   │   ├── utils/
│   │   │   ├── pkce.js                         # PKCE 工具
│   │   │   └── auth.js                         # 认证逻辑
│   │   ├── stores/auth.js                      # Pinia 状态管理
│   │   ├── views/
│   │   │   ├── Home.vue
│   │   │   ├── Callback.vue                    # OAuth2 回调
│   │   │   └── Profile.vue
│   │   └── router/index.js
│   └── vite.config.ts
│
├── app-springboot/              # Spring Boot 后端应用（机密客户端）
│   └── src/main/java/
│       ├── config/
│       │   └── SecurityConfig.java             # OAuth2 Client 配置
│       └── controller/
│           └── HomeController.java
│
└── resource-api/                # 资源服务器
    └── src/main/java/
        ├── config/
        │   └── ResourceServerConfig.java       # JWT 资源服务器配置
        └── controller/
            └── ApiController.java              # 业务API

升级路径（后续）:
  Phase N: auth-server 前后端分离
    ├── auth-frontend/  (Vue SPA, 登录/注册页面)
    ├── auth-server/    (纯 API)
    └── nginx.conf      (反向代理统一域名)
```

### 数据库设计

#### Auth Server 数据库 (oauth2_center)

```sql
-- 用户表
CREATE TABLE sys_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(200) NOT NULL,
    nickname    VARCHAR(50),
    email       VARCHAR(100),
    phone       VARCHAR(20),
    status      TINYINT DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- OAuth2 客户端注册表
CREATE TABLE oauth2_client (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_id       VARCHAR(100) NOT NULL UNIQUE,
    client_secret   VARCHAR(200),
    client_name     VARCHAR(100),
    client_type     VARCHAR(20) DEFAULT 'INTERNAL',
    scopes          VARCHAR(500),
    grant_types     VARCHAR(200),
    redirect_uris   VARCHAR(1000),
    require_consent TINYINT DEFAULT 0,
    access_token_ttl INT DEFAULT 3600,
    refresh_token_ttl INT DEFAULT 604800,
    status          TINYINT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 用户-系统访问权限表
CREATE TABLE user_client_access (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    client_id   VARCHAR(100) NOT NULL,
    allowed     TINYINT DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_client (user_id, client_id)
);

-- 授权码表
CREATE TABLE oauth2_authorization_code (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    code            VARCHAR(200) NOT NULL UNIQUE,
    client_id       VARCHAR(100) NOT NULL,
    user_id         BIGINT NOT NULL,
    scopes          VARCHAR(500),
    redirect_uri    VARCHAR(500),
    expires_at      DATETIME NOT NULL,
    used            TINYINT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Token 表
CREATE TABLE oauth2_token (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    client_id       VARCHAR(100) NOT NULL,
    access_token    VARCHAR(500) NOT NULL UNIQUE,
    refresh_token   VARCHAR(500),
    scopes          VARCHAR(500),
    access_expires  DATETIME NOT NULL,
    refresh_expires DATETIME,
    revoked         TINYINT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_client (user_id, client_id),
    INDEX idx_access_token (access_token),
    INDEX idx_refresh_token (refresh_token)
);

-- 第三方授权记录
CREATE TABLE user_client_consent (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    client_id   VARCHAR(100) NOT NULL,
    scopes      VARCHAR(500),
    consented_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_client (user_id, client_id)
);

-- Webhook 订阅表
CREATE TABLE webhook_subscriber (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_id   VARCHAR(100) NOT NULL,
    event_type  VARCHAR(50) NOT NULL,
    callback_url VARCHAR(500) NOT NULL,
    secret      VARCHAR(200),
    status      TINYINT DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_client_event (client_id, event_type)
);

-- Token 黑名单（主动撤销）
CREATE TABLE oauth2_token_blacklist (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    jti             VARCHAR(200) NOT NULL UNIQUE,   -- JWT ID
    token_value     VARCHAR(500),
    user_id         BIGINT,
    client_id       VARCHAR(100),
    reason          VARCHAR(100),                   -- 撤销原因: admin_revoke, user_logout, password_changed
    expires_at      DATETIME NOT NULL,              -- 与原 token 同过期，过期后可清理
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_jti (jti),
    INDEX idx_expires (expires_at)
);

-- 审计日志
CREATE TABLE sys_audit_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT,
    username        VARCHAR(50),
    client_id       VARCHAR(100),
    action          VARCHAR(50) NOT NULL,           -- LOGIN, LOGOUT, AUTHORIZE, TOKEN_ISSUED, TOKEN_REVOKED, REGISTER, PASSWORD_CHANGED
    detail          VARCHAR(500),
    ip              VARCHAR(50),
    user_agent      VARCHAR(500),
    status          VARCHAR(20),                    -- SUCCESS, FAILED
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_action (action),
    INDEX idx_created (created_at)
);

-- Webhook 发送日志
CREATE TABLE webhook_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscriber_id   BIGINT NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    payload         TEXT,
    status          VARCHAR(20) DEFAULT 'PENDING',
    retry_count     INT DEFAULT 0,
    next_retry_at   DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status_retry (status, next_retry_at)
);
```

#### 业务系统数据库 (oa_db / crm_db)

```sql
-- 用户快照表（从 JWT/Webhook 同步）
CREATE TABLE oa_user_snapshot (
    auth_user_id    BIGINT PRIMARY KEY,
    username        VARCHAR(50) NOT NULL,
    nickname        VARCHAR(50),
    email           VARCHAR(100),
    phone           VARCHAR(20),
    synced_at       DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 角色表（各系统自定义）
CREATE TABLE oa_role (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    name    VARCHAR(50) NOT NULL,
    code    VARCHAR(50) NOT NULL UNIQUE
);

-- 用户角色映射
CREATE TABLE oa_user_role (
    auth_user_id    BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    PRIMARY KEY (auth_user_id, role_id)
);
```

### 认证流程设计

#### 1. 内部应用 SSO 流程（自动授权）

```
Browser → App → Auth Server → 登录 → 检查 user_client_access
→ allowed=true, require_consent=false → 自动返回 code → 换 token → 完成
```

#### 2. 第三方应用授权流程（用户确认）

```
Browser → Third-party App → Auth Server → 登录
→ 检查 user_client_access → allowed=true, require_consent=true
→ 显示授权确认页 → 用户同意 → 返回 code → 换 token → 完成
```

#### 3. 无权限拦截

```
Browser → App → Auth Server → 登录
→ 检查 user_client_access → allowed=false
→ 返回 "您无权访问该系统" 错误页
```

### JWT Payload 设计

```json
{
  "sub": "1",
  "username": "zhangsan",
  "nickname": "张三",
  "email": "zhang@company.com",
  "phone": "13800138000",
  "iss": "http://auth.local:9000",
  "aud": "oa-app",
  "iat": 1691234567,
  "exp": 1691238167,
  "scope": "openid profile email"
}
```

### Webhook 设计

#### 支持的事件类型

| 事件 | 触发时机 | 数据 |
|------|----------|------|
| `user.created` | 新用户注册 | user_id, username, nickname, email |
| `user.updated` | 用户信息修改 | user_id, changes{field: {old, new}} |
| `user.disabled` | 用户禁用 | user_id |
| `user.enabled` | 用户启用 | user_id |
| `user.password_changed` | 密码修改 | user_id |

#### 请求格式

```
POST {callback_url}
Content-Type: application/json
X-Webhook-Signature: sha256={hmac_signature}
X-Webhook-Event: user.updated
X-Webhook-Timestamp: 1691234567

{
  "event": "user.updated",
  "timestamp": "2026-08-09T22:00:00Z",
  "data": {
    "user_id": 1,
    "username": "zhangsan",
    "changes": {
      "nickname": { "old": "张三", "new": "张三丰" }
    }
  }
}
```

#### 重试策略

```
失败后指数退避重试:
  第1次: 1分钟后
  第2次: 5分钟后
  第3次: 30分钟后
  第4次: 2小时后
  第5次: 失败，标记为 FAILED，人工介入

补偿机制: 每天凌晨定时全量同步
```

## Key Decisions

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 客户端注册 | 数据库存储 | 支持动态增删改查，生产可用 |
| Token 存储 | Redis + DB | Redis 快速查询，DB 持久化 |
| 用户存储 | MySQL | 已有基础设施 |
| 前端框架 | Vue 3 | 团队熟悉 |
| JWT 用户信息 | 频繁读取的字段放 JWT | 减少网络调用 |
| 权限检查时机 | /authorize 端点 | 统一拦截，各系统无需重复实现 |
| 用户同步 | JWT + 快照表 + Webhook | 无跨库查询，性能最优 |
| 内部/第三方区分 | require_consent 字段 | 内部应用无感，第三方需确认 |
| Token 有效期 | access 30min, refresh 7d | 安全与体验平衡 |
| Token 撤销 | JWT 黑名单（Redis + DB） | 支持管理员踢人、密码修改失效 |
| 用户注册 | 开放自助注册 | 需注册页 + 邮箱验证 |
| 审计日志 | 记录关键操作 | 安全合规、问题追溯 |
| 前后端架构 | 先不分离（Thymeleaf），跑通后再升级分离 | 聚焦 OAuth2 原理，避免分散精力 |

## Implementation Phases

### Phase 1: Auth Server 基础（核心）
- 数据库建表（全部表结构）
- Spring Authorization Server 配置
- 用户登录页面
- OAuth2/OIDC 端点（/authorize, /token, /jwks, /.well-known）
- JWT 签发与验签
- 审计日志记录（登录、授权、Token签发）

### Phase 2: 客户端接入
- 内部客户端注册（app-vue, app-springboot）
- PKCE 支持
- 授权码流程
- /userinfo 端点
- 审计日志（Token签发记录）

### Phase 3: 权限控制
- user_client_access 表实现
- /authorize 端点权限拦截
- 无权限错误页
- 管理员分配用户系统权限 API

### Phase 4: Token 管理与撤销
- Token 黑名单机制（Redis + DB 双写）
- 管理员强制踢人下线 API
- 用户登出时 Token 加入黑名单
- 密码修改时所有 Token 失效
- JWT 验证时检查黑名单

### Phase 5: 用户注册
- 注册页面（用户名、密码、邮箱）
- 邮箱验证码
- 注册成功后自动授权当前客户端
- 防刷机制（IP限流、验证码）

### Phase 6: 资源服务器
- resource-api 模块
- JWT 验证配置
- Token 黑名单检查
- 业务 API 示例

### Phase 7: Webhook 机制
- Webhook 订阅管理
- 事件发送与签名
- 失败重试（指数退避）
- 接收端示例
- 用户信息变更自动触发

### Phase 8: 前端完善
- app-vue 完整流程
- 登录/注册/回调/登出
- Token 自动刷新
- 无权限提示页
- 审计日志查询页（管理员）

## Risks & Mitigations

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| JWT 密钥泄露 | 所有 token 失效 | 密钥存储在环境变量，定期轮换 |
| Webhook 丢失 | 用户数据不一致 | 定时全量同步兜底 |
| Redis 宕机 | Token 无法查询 | DB 持久化，降级为查 DB |
| 跨域问题 | 前端无法调用 | CORS 配置 + 反向代理 |
| Session 共享 | 多实例 SSO 失效 | Redis 集中存储 Session |

## Design Decisions (已确认)

| 问题 | 决策 | 说明 |
|------|------|------|
| Token 有效期 | access_token 30min，refresh_token 7d | 已确认 |
| Token 主动撤销 | 需要支持（JWT 黑名单机制） | Redis 存储已撤销的 token JTI，验证时检查 |
| 用户注册 | 开放自助注册 | 需要注册页面 + 邮箱/手机验证 |
| 审计日志 | 需要记录 | 记录登录、授权、Token签发/撤销等操作 |

## Open Questions

无

## References

- [RFC 6749 - OAuth 2.0](https://datatracker.ietf.org/doc/html/rfc6749)
- [RFC 7636 - PKCE](https://datatracker.ietf.org/doc/html/rfc7636)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- [Spring Authorization Server Reference](https://docs.spring.io/spring-authorization-server/reference/)
