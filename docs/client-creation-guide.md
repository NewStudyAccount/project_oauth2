# OAuth2 客户端创建指南

## 概述

本文档说明如何通过管理页面新增 OAuth2 客户端，以及不同场景下的客户端配置方式。

## 前置条件

1. **启动基础设施**
   ```bash
   docker compose -f infra/docker-compose.yml up -d
   ```

2. **启动 auth-server**
   ```bash
   cd auth-center/backend && mvn spring-boot:run
   ```

3. **启动管理前端**
   ```bash
   cd auth-center/auth-server-frontend && npm install && npm run dev
   ```

4. **配置 hosts 文件**
   ```
   127.0.0.1  auth.local
   ```

## 操作步骤

### 1. 登录管理后台

- 访问 http://auth.local:5174
- 使用管理员账号登录：
  - 用户名：`admin`
  - 密码：`Admin@123`

### 2. 进入客户端管理

- 左侧菜单点击"客户端管理"
- 点击右上角"新增客户端"按钮
- 路由跳转到 `/clients/add`

### 3. 填写客户端信息

根据业务需求填写表单字段，详见下方字段说明。

## 表单字段说明

| 字段 | 必填 | 说明 |
|------|------|------|
| **Client ID** | 是 | 客户端唯一标识符，用于 OAuth2 授权请求中的 `client_id` 参数。创建后不可修改。 |
| **Client Secret** | 否 | 客户端密钥，用于机密客户端的身份验证。BCrypt 加密存储，编辑时留空表示不修改。公开客户端（如纯前端 SPA）无需密钥。 |
| **密钥过期时间** | 否 | 客户端密钥的过期时间，过期后需重新生成密钥。留空表示永不过期。 |
| **客户端名称** | 否 | 人类可读名称，显示在授权确认页面和审计日志中。 |
| **客户端认证方式** | 是 | 客户端向认证服务器证明身份的方式（见下方说明）。 |
| **授权类型** | 是 | 客户端可使用的 OAuth2 授权流程（见下方说明）。 |
| **重定向 URI** | 否 | 授权完成后认证服务器将用户重定向回客户端的 URI。必须精确匹配（含协议、域名、端口、路径）。 |
| **Scopes** | 否 | 客户端可请求的权限范围，默认 `openid,profile,email`。 |
| **需要 PKCE** | 否 | 是否强制使用 PKCE 增强安全，适用于公开客户端。 |
| **需要授权确认** | 否 | 用户首次使用时是否弹出授权确认页面。 |
| **启用** | 否 | 客户端启用/禁用状态，禁用后所有 OAuth2 请求将被拒绝。 |
| **Access Token 有效期** | 否 | 访问令牌有效时长（秒），默认 1800（30 分钟）。 |
| **Refresh Token 有效期** | 否 | 刷新令牌有效时长（秒），默认 604800（7 天）。 |
| **Auth Code 有效期** | 否 | 授权码有效时长（秒），默认 300（5 分钟）。 |
| **复用刷新令牌** | 否 | 刷新时是否返回相同 refresh_token。开启更简单，关闭（Rotation）更安全。 |
| **ID Token 签名算法** | 否 | OIDC ID Token 签名算法，默认 RS256。 |
| **访问令牌格式** | 否 | JWT（self-contained）或 Opaque（reference），默认 JWT。 |

## 认证方式说明

| 方式 | 说明 | 适用场景 |
|------|------|----------|
| `client_secret_basic` | 通过 HTTP Basic Auth 传递凭证 | 最常用，适合服务端应用 |
| `client_secret_post` | 通过 POST 请求体传递凭证 | 部分客户端不支持 Basic Auth 时使用 |
| `none` | 公开客户端，无需认证 | 纯前端 SPA / PKCE 场景 |

## 授权类型说明

| 类型 | 说明 | 适用场景 |
|------|------|----------|
| `authorization_code` | 授权码模式，用户登录授权后获取授权码再换取令牌 | 最安全，适合有用户交互的场景 |
| `refresh_token` | 刷新令牌，access_token 过期后免登录获取新令牌 | 需要长期保持登录状态 |
| `client_credentials` | 客户端凭证模式，无用户参与 | 服务间调用，后台任务 |

## 三种典型客户端配置

### 1. 机密客户端（服务端应用）

适用于有后端服务的应用，如 `app-springboot`。

```
Client ID:           springboot-app
Client Secret:       Admin@123
客户端名称:           Spring Boot 应用
认证方式:             client_secret_basic
授权类型:             authorization_code, refresh_token
重定向 URI:          http://client.a.local:8082/login/oauth2/code/springboot-app
Scopes:             openid, profile, email
需要 PKCE:           否
需要授权确认:         否
启用:                是
Access Token 有效期:  1800
Refresh Token 有效期: 604800
```

**特点：**
- Client Secret 存储在服务端，不暴露给浏览器
- 安全性高，适合企业内部应用
- Token 存储在服务端 Session

### 2. 公开客户端 PKCE（纯前端 SPA）

适用于纯前端应用，无后端服务，如 `app-vue`。

```
Client ID:           vue-app
Client Secret:       (留空)
客户端名称:           Vue SPA 应用
认证方式:             none
授权类型:             authorization_code, refresh_token
重定向 URI:          http://client.b.local:5173/callback
Scopes:             openid, profile, email
需要 PKCE:           是
需要授权确认:         否
启用:                是
Access Token 有效期:  1800
Refresh Token 有效期: 604800
```

**特点：**
- 无需 Client Secret，通过 PKCE 保证安全性
- 前端自行管理 Token（localStorage）
- 需要前端实现 PKCE 逻辑（code_verifier + code_challenge）

### 3. 网关客户端（Token 中继模式）

适用于微服务网关，如 `gateway`。

```
Client ID:           gateway-app
Client Secret:       gateway-secret
客户端名称:           Gateway 网关
认证方式:             client_secret_basic
授权类型:             authorization_code, refresh_token
重定向 URI:          http://gateway.local:8082/login/oauth2/code/auth-server
Scopes:             openid, profile, email
需要 PKCE:           否
需要授权确认:         否
启用:                是
Access Token 有效期:  1800
Refresh Token 有效期: 604800
```

**特点：**
- 网关持有 Token，对下游服务透明
- 使用 Redis 分布式 Session
- Token 不暴露给浏览器

## 数据流转

```
前端表单
  ↓ POST /api/admin/clients (ClientDTO JSON)
AdminController.createClient()
  ↓
ClientConverter.toEntity()
  ↓ 密码 BCrypt 加密
JdbcRegisteredClientRepository.save()
  ↓
oauth2_registered_client 表
```

## 数据库结构

客户端信息存储在 `oauth2_registered_client` 表：

```sql
CREATE TABLE oauth2_registered_client (
    id                            VARCHAR(100)  NOT NULL,  -- UUID，系统生成
    client_id                     VARCHAR(100)  NOT NULL,  -- 业务标识
    client_id_issued_at           TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    client_secret                 VARCHAR(200)  DEFAULT NULL,  -- BCrypt 加密
    client_secret_expires_at      TIMESTAMP     DEFAULT NULL,
    client_name                   VARCHAR(200)  NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types     VARCHAR(1000) NOT NULL,
    redirect_uris                 VARCHAR(1000) DEFAULT NULL,
    scopes                        VARCHAR(1000) NOT NULL,
    client_settings               VARCHAR(2000) NOT NULL,  -- JSON，含 enabled、requireProofKey 等
    token_settings                VARCHAR(2000) NOT NULL,  -- JSON，含 TTL、算法等
    PRIMARY KEY (id)
);
```

## 注意事项

1. **Client ID 不可修改**
   - 创建后 Client ID 字段变为只读
   - 如需修改，只能删除重建

2. **Client Secret 加密存储**
   - 存储时通过 BCrypt 加密
   - 读取时不返回（前端看到的是 null）
   - 编辑时留空表示不修改原密码

3. **id 与 client_id 的区别**
   - `id`：UUID，系统自动生成，用于数据库主键和 API 路径
   - `client_id`：业务标识，用于 OAuth2 流程中的 client_id 参数

4. **启用/禁用机制**
   - 通过 `EnabledCheckingRegisteredClientRepository` 实现
   - 检查 `client_settings.settings.client.enabled` 字段
   - 禁用的客户端在 OAuth2 流程中返回 null（视为不存在）

5. **重定向 URI 必须精确匹配**
   - 包含协议（http/https）、域名、端口、路径
   - 防止开放重定向攻击

## API 接口

管理 API 路径前缀：`/api/admin`（需要 ADMIN 角色）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/clients` | GET | 列出所有客户端 |
| `/clients/{id}` | GET | 获取客户端详情 |
| `/clients` | POST | 创建客户端 |
| `/clients/{id}` | PUT | 更新客户端 |
| `/clients/{id}` | DELETE | 删除客户端 |
| `/clients/{id}/status` | PUT | 启用/禁用客户端 |

## 相关文件

- 前端表单：`auth-center/auth-server-frontend/src/views/ClientFormView.vue`
- 前端 API：`auth-center/auth-server-frontend/src/api/client.js`
- 后端控制器：`auth-center/backend/src/main/java/com/example/authserver/controller/AdminController.java`
- DTO 转换：`auth-center/backend/src/main/java/com/example/authserver/dto/ClientConverter.java`
- 客户端仓库：`auth-center/backend/src/main/java/com/example/authserver/config/EnabledCheckingRegisteredClientRepository.java`
- 数据库 Schema：`auth-center/backend/src/main/resources/db/schema.sql`
