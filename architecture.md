# project_oauth2 架构设计文档

## 一、项目总览

本项目是一个基于 OAuth2 / OIDC 的统一认证授权体系，由 **认证中心**、**多个外部系统** 和 **资源服务** 组成，通过 Spring Authorization Server 实现集中认证，各外部系统以不同方式接入。

---

## 二、模块清单

| 模块 | 目录 | 端口 | 域名 | 技术栈 | OAuth2 角色 |
|------|------|------|------|--------|-------------|
| **auth-server** | auth-center/backend/ | 9000 | auth.local | Spring Boot + Spring Authorization Server + MyBatis-Plus + Thymeleaf | Authorization Server |
| **admin-vue3** | auth-center/frontend/ | 5174 | auth.local | Vue3 + Vite | 管理前端 (Session 认证) |
| **app-springboot** | client-app/backend/ | 8082 | client.a.local | Spring Boot + OAuth2 Client + Resource Server | Client + Resource Server |
| **app-vue3-springboot** | client-app/frontend/ | 5173 | client.a.local | Vue3 + Vite | 前端 (代理到后端) |
| **app-vue** | standalone-app/frontend/ | 5173 | client.b.local | Vue2 + Pinia + Vite | Public Client (PKCE) |
| **gateway** | platform/gateway/ | 8080 | gateway.local | Spring Cloud Gateway (WebFlux) + OAuth2 Client + Redis Session | Client + Token Relay |
| **resource-api** | platform/resource-api/ | 8083 | - | Spring Boot + OAuth2 Resource Server | Resource Server |

---

## 三、整体架构图

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                            project_oauth2 整体架构                                    │
│                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────────┐  │
│  │                         认证中心 (Authorization Center)                       │  │
│  │                                                                                │  │
│  │  ┌─────────────────────┐      ┌────────────────────────────────────────────┐  │  │
│  │  │   admin-vue3        │      │   auth-server  (port: 9000)                │  │  │
│  │  │   (port: 5174)      │─────▶│   auth.local                               │  │  │
│  │  │   auth.local        │      │                                            │  │  │
│  │  │                     │      │   Spring Authorization Server               │  │  │
│  │  │  · Dashboard 仪表盘 │      │                                            │  │  │
│  │  │  · Client 客户端管理│      │   内置页面: /login /register /consent       │  │  │
│  │  │  · User 用户管理   │      │   管理API: /api/admin/** (ADMIN角色)        │  │  │
│  │  │  · Access 权限管理 │      │   OAuth2: /authorize /token /jwks /revoke  │  │  │
│  │  │  · AuditLog 审计日志│      │   OIDC: /userinfo                         │  │  │
│  │  └─────────────────────┘      │                                            │  │  │
│  │  认证方式: Session+Cookie     │   存储: MySQL + Redis                      │  │  │
│  │                               └────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────────┐  │
│  │  外部系统A: app-vue3-springboot + app-springboot (前后端分离, 服务端OAuth2)    │  │
│  │                                                                                │  │
│  │  ┌────────────────────────┐      ┌────────────────────────────────────────┐  │  │
│  │  │  app-vue3-springboot   │      │  app-springboot  (port: 8082)          │  │  │
│  │  │  (port: 5173)          │─────▶│  client.a.local                       │  │  │
│  │  │  client.a.local        │      │                                        │  │  │
│  │  │                        │      │  OAuth2 Client (授权码模式)             │  │  │
│  │  │  · HomeView 首页       │      │    client-id: springboot-app           │  │  │
│  │  │  · ProfileView 个人信息│      │  OAuth2 Resource Server (JWT验证)     │  │  │
│  │  └────────────────────────┘      │                                        │  │  │
│  │  代理 → client.a.local:8082      │  API: /api/public /api/protected ...   │  │  │
│  │  认证方式: 代理到后端Session      └────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────────┐  │
│  │  外部系统B: app-vue (纯前端, PKCE授权码模式)                                    │  │
│  │                                                                                │  │
│  │  ┌────────────────────────────────────────────────────────────────────────┐  │  │
│  │  │  app-vue  (port: 5173)                                                │  │  │
│  │  │  client.b.local                                                       │  │  │
│  │  │                                                                        │  │  │
│  │  │  OAuth2 Public Client (PKCE + 授权码模式)                              │  │  │
│  │  │    client-id: vue-app                                                  │  │  │
│  │  │    自行管理 Token (localStorage)                                       │  │  │
│  │  │                                                                        │  │  │
│  │  │  页面: Home / Callback / Profile / NoPermission / ApiDemo              │  │  │
│  │  └────────────────────────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────────┐  │
│  │  外部系统C: gateway (网关层, Token中继)                                         │  │
│  │                                                                                │  │
│  │  ┌────────────────────────────────────────────────────────────────────────┐  │  │
│  │  │  gateway  (port: 8080)                                                │  │  │
│  │  │                                                                        │  │  │
│  │  │  Spring Cloud Gateway (WebFlux) + OAuth2 Client                       │  │  │
│  │  │    client-id: gateway-app                                              │  │  │
│  │  │                                                                        │  │  │
│  │  │  路由: /auth/**→auth-server  /api/**→resource-api  (Nacos lb://)      │  │  │
│  │  │  TokenRelayFilter: Session → Bearer Token → 下游                      │  │  │
│  │  │  Session: Redis (分布式, 30min)                                        │  │  │
│  │  └────────────────────────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────────┐  │
│  │  资源服务: resource-api (JWT验证)                                               │  │
│  │                                                                                │  │
│  │  ┌────────────────────────────────────────────────────────────────────────┐  │  │
│  │  │  resource-api  (port: 8083)                                           │  │  │
│  │  │                                                                        │  │  │
│  │  │  OAuth2 Resource Server (JWT, issuer: auth.local:9000)                │  │  │
│  │  │  注册到 Nacos                                                          │  │  │
│  │  │  API: /api/profile  /api/resources                                    │  │  │
│  │  └────────────────────────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────────┐  │
│  │  基础设施                                                                        │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────────────────────────────────┐  │  │
│  │  │ MySQL :3306│  │ Redis :6379│  │ Nacos (192.168.99.100:8848)            │  │  │
│  │  │oauth2_center│  │   db:12    │  │ gateway, resource-api 注册            │  │  │
│  │  └────────────┘  └────────────┘  └────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 四、交互流程图

```
                          ┌─────────────┐
                          │  浏览器/用户  │
                          └──────┬──────┘
                                 │
            ┌────────────────────┼────────────────────┐
            │                    │                    │
            ▼                    ▼                    ▼
   ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
   │  admin-vue3     │ │  app-vue3-      │ │  app-vue        │
   │  :5174          │ │  springboot     │ │  :5173          │
   │  auth.local     │ │  :5173          │ │  client.b.local │
   └────────┬────────┘ │  client.a.local │ └────────┬────────┘
            │          └────────┬────────┘           │
            │                   │                    │
            ▼                   ▼                    ▼
   ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
   │  auth-server    │ │  app-springboot │ │  auth-server    │
   │  :9000          │ │  :8082          │ │  :9000          │
   └────────┬────────┘ └────────┬────────┘ └────────┬────────┘
            │                   │                     │
            │                   │                     ▼
            │                   │           ┌─────────────────┐
            │                   │           │  resource-api   │
            │                   │           │  :8083          │
            │                   │           └─────────────────┘
            ▼                   ▼
   ┌─────────────────────────────────────────────────────┐
   │              auth-server :9000                       │
   │  · /oauth2/authorize    授权端点                     │
   │  · /oauth2/token        令牌端点                     │
   │  · /oauth2/jwks         JWK Set端点                  │
   │  · /userinfo            用户信息端点                  │
   │  · /oauth2/revoke       撤销端点                     │
   └─────────────────────────────────────────────────────┘
```

---

## 五、四条独立的认证交互链路

### 链路1: admin-vue3 → auth-server（Session 认证，管理后台）

```
  admin-vue3 ──代理──▶ auth-server
    · /api/admin/**   客户端/用户/权限/审计管理 (需ADMIN角色)
    · /login, /logout  登录登出
    · 认证方式: Session + Cookie (withCredentials)
```

- admin-vue3 作为认证中心的管理前端，通过 Vite 代理将 API 请求转发到 auth-server
- 使用 Session + Cookie 方式认证，axios 开启 `withCredentials: true`
- 401 响应时自动跳转到 auth-server 的登录页

### 链路2: app-vue3-springboot + app-springboot → auth-server（服务端 OAuth2 Client）

```
  app-vue3-springboot ──代理──▶ app-springboot ──OAuth2──▶ auth-server

  登录流程:
  1. 前端访问受保护页 → 302 → auth-server/oauth2/authorize
  2. 用户登录授权 → 302 → app-springboot/login/oauth2/code/springboot-app
  3. app-springboot 用授权码换 Token (服务端完成, Session存储)
  4. 前端通过 Session Cookie 访问 app-springboot 的 API

  · client-id: springboot-app
  · 认证方式: 授权码模式 + Session (后端持有Token)
```

- 前端 app-vue3-springboot 通过 Vite 代理将 `/api`、`/oauth2`、`/login`、`/logout` 转发到 app-springboot
- 后端 app-springboot 同时充当 OAuth2 Client 和 Resource Server
- 401 响应时前端跳转到 `/oauth2/authorization/springboot-app` 触发 OAuth2 登录
- CSRF 保护: 使用 CookieCsrfTokenRepository，前端从 Cookie 读取 XSRF-TOKEN 添加到请求头

### 链路3: app-vue → auth-server + resource-api（纯前端，PKCE 授权码模式）

```
  app-vue ──OAuth2/PKCE──▶ auth-server
  app-vue ──Bearer Token──▶ resource-api

  登录流程:
  1. 前端生成 PKCE (code_verifier + code_challenge)
  2. 跳转 → auth-server/oauth2/authorize?code_challenge=S256
  3. 用户登录授权 → 302 → app-vue/callback?code=xxx
  4. 前端用 code + code_verifier → auth-server/oauth2/token
  5. 前端用 access_token → auth-server/userinfo
  6. 前端用 access_token → resource-api/api/profile (Bearer Token)

  · client-id: vue-app (Public Client)
  · 认证方式: PKCE + 授权码模式 (前端自行管理Token)
```

- app-vue 是完全独立于 app-springboot 的纯前端系统
- 使用 PKCE (Proof Key for Code Exchange) 增强安全性，无需 client_secret
- Token 存储在 localStorage，由 Pinia store 统一管理
- 直接携带 Bearer Token 调用 resource-api 的受保护接口

### 链路4: gateway → auth-server + resource-api（网关 Token 中继模式）

```
  浏览器 ──▶ gateway ──OAuth2登录──▶ auth-server

  登录流程:
  1. 浏览器访问 gateway → 302 → auth-server/oauth2/authorize
  2. 用户登录授权 → gateway 获取 Token, 存入 Redis Session
  3. 后续请求: gateway TokenRelayFilter 从 Session 取 Token
     → 添加 Authorization: Bearer xxx → 转发给下游服务

  路由:
  · /auth/**  → lb://auth-server   (Nacos)
  · /api/**   → lb://resource-api  (Nacos)

  · client-id: gateway-app
  · 认证方式: 授权码模式 + Redis Session + Token中继
```

- Gateway 基于 Spring Cloud Gateway (WebFlux 响应式)
- TokenRelayGlobalFilter: 从 OAuth2AuthorizedClientService 获取 Access Token，添加到下游请求头
- 使用 Redis 分布式 Session，支持多实例部署
- 通过 Nacos 服务发现实现负载均衡路由

---

## 六、认证中心内部架构 (auth-server)

### 6.1 核心配置

- **Authorization Server**: 基于 Spring Authorization Server，issuer 为 `http://auth.local:9000`
- **客户端存储**: JdbcRegisteredClientRepository，客户端配置存储在 `oauth2_registered_client` 表
- **授权存储**: JdbcOAuth2AuthorizationService
- **同意存储**: JdbcOAuth2AuthorizationConsentService
- **JWK**: RSA 2048 位密钥对，启动时生成
- **OIDC**: 启用 OpenID Connect 1.0 支持

### 6.2 安全过滤链

| 顺序 | 过滤链 | 职责 |
|------|--------|------|
| 1 | authServerFilterChain | OAuth2 Authorization Server 端点 (authorize, token, jwks 等) |
| 2 | defaultFilterChain | 默认安全链：页面登录 + API 认证 |

### 6.3 内置页面 (Thymeleaf)

| 页面 | 路径 | 说明 |
|------|------|------|
| 登录页 | /login | 表单登录 |
| 注册页 | /register | 用户注册 (支持邮箱验证码) |
| 授权确认页 | /consent | 用户授权确认 (OAuth2 consent) |
| 错误页 | /error | 通用错误页面 |
| 无权限页 | /error/no_permission | 权限不足提示 |
| 授权拒绝页 | /error/consent_denied | 用户拒绝授权提示 |

### 6.4 管理 API (AdminController)

所有管理接口需要 `ADMIN` 角色，路径前缀 `/api/admin`：

| 接口 | 方法 | 说明 |
|------|------|------|
| /clients | GET | 列出所有 OAuth2 客户端 |
| /clients/{id} | GET | 获取客户端详情 |
| /clients | POST | 创建客户端 |
| /clients/{id} | PUT | 更新客户端 |
| /clients/{id} | DELETE | 删除客户端 |
| /clients/{id}/status | PUT | 启用/禁用客户端 |
| /users | GET | 列出所有用户 |
| /users/{id}/status | PUT | 启用/禁用用户 |
| /access | GET | 查询用户权限 |
| /access | PUT | 设置用户权限 |
| /audit-logs | GET | 查询审计日志 |
| /stats | GET | 统计概览 |

---

## 七、资源服务架构 (resource-api)

### 7.1 安全配置

- 纯 Resource Server 角色，仅验证 JWT Bearer Token
- issuer-uri: `http://auth.local:9000`
- 所有请求均需认证

### 7.2 API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/profile | GET | 获取用户资料 (从 JWT 解析 sub, username, nickname, email, phone) |
| /api/resources | GET | 获取受保护资源列表 |

### 7.3 服务注册

- 注册到 Nacos (`192.168.99.100:8848`)，供 Gateway 路由发现

---

## 八、基础设施

### 8.1 MySQL

- 地址: `192.168.99.100:3306`
- 数据库: `oauth2_center`
- 主要表:
  - `oauth2_registered_client` — OAuth2 客户端配置
  - `oauth2_authorization` — 授权信息
  - `oauth2_authorization_consent` — 授权同意记录
  - `sys_user` — 系统用户
  - `user_client_access` — 用户-客户端访问控制
  - `sys_audit_log` — 审计日志

### 8.2 Redis

- 地址: `192.168.99.100:6379`，database: 12
- 用途:
  - auth-server: 缓存
  - gateway: 分布式 Session 存储

### 8.3 Nacos

- 地址: `192.168.99.100:8848`
- 注册服务:
  - gateway
  - resource-api
  - auth-server (已注释，未启用)

---

## 九、域名与代理关系

| 前端 | 域名 | 代理目标 |
|------|------|----------|
| admin-vue3 | auth.local:5174 | → auth.local:9000 (auth-server) |
| app-vue3-springboot | client.a.local:5173 | → client.a.local:8082 (app-springboot) |
| app-vue | client.b.local:5173 | → auth.local:9000 (仅 /userinfo) |

> 所有系统通过自定义本地域名 (auth.local, client.a.local, client.b.local) 实现同域 SSO 和跨域 CORS 控制。

---

## 十、三种客户端接入模式对比

| 特性 | 服务端 OAuth2 Client | 纯前端 PKCE Client | 网关 Token 中继 |
|------|---------------------|-------------------|----------------|
| 代表系统 | app-springboot | app-vue | gateway |
| client 类型 | Confidential | Public | Confidential |
| Token 持有者 | 后端 (Session) | 前端 (localStorage) | 网关 (Redis Session) |
| 安全性 | 高 (client_secret 不暴露) | 中 (PKCE 补偿) | 高 (Token 不暴露给浏览器) |
| 需要后端 | 是 | 否 | 是 (网关本身) |
| 前端复杂度 | 低 (代理到后端) | 高 (自行管理Token) | 低 (只与网关交互) |
| 适用场景 | 前后端分离项目 | 纯前端/移动端 | 微服务网关统一入口 |