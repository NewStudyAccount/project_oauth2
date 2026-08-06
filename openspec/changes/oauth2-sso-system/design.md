## Context

本项目是一个 OAuth2/SSO 学习项目，目标是从零搭建生产级的跨域单点登录系统。当前项目为空白状态，无已有代码。需要搭建完整的认证中心、多个客户端应用，以及配套的数据库和前端。

技术栈已确定：Spring Authorization Server 1.2+ (OAuth2.1/OIDC)、Spring Boot 3.2+、MyBatis-Plus、MySQL 8、Vue3 + Vite + Pinia + Axios。通过 hosts 文件映射实现本地跨域 (auth.local / app-a.local / app-b.local)。

## Goals / Non-Goals

**Goals:**
- 实现完整的 OAuth2.1 授权码流程 + PKCE
- 实现跨域 SSO：一次登录，多客户端自动登录
- 实现 JWT Token 签发、刷新、撤销的完整生命周期
- 实现自定义登录页面和数据库用户认证
- Vue SPA 前端封装可复用的 OAuth2 PKCE 流程
- 预留 Resource Server 和 RBAC 扩展点

**Non-Goals:**
- 不实现社交登录 (GitHub/微信) — 预留扩展点但不实现
- 不实现多实例部署 (Redis Session) — 仅单实例
- 不实现 HTTPS — 本地开发使用 HTTP
- 不实现用户注册功能 — 仅使用预置用户
- 不实现管理后台 — 客户端通过数据库预置

## Decisions

### Decision 1: 使用 Spring Authorization Server 而非 Keycloak

**选择**: Spring Authorization Server

**理由**:
- 学习目的：直接使用框架能深入理解 OAuth2 协议细节
- 轻量级：不需要额外部署 Keycloak 服务
- 官方支持：Spring 生态的官方 OAuth2 服务器
- 灵活性：可以完全自定义登录页面、用户认证逻辑

**替代方案**:
- Keycloak: 功能强大但黑盒，不利于学习协议细节
- Auth0: SaaS 服务，不适合本地学习

### Decision 2: 使用 JWT 而非 Opaque Token

**选择**: JWT (无状态 Token)

**理由**:
- Resource Server 可以独立验证 Token，无需回调认证中心
- Token 中携带用户信息 (claims)，减少网络请求
- 学习 JWT 结构 (header.payload.signature) 本身就有价值

**替代方案**:
- Opaque Token: 需要每次请求都回调认证中心验证，增加延迟

**权衡**:
- JWT 无法即时撤销 — 通过 Token 过期时间 + 撤销列表缓解
- JWT 体积较大 — 学习场景可接受

### Decision 3: 使用 MyBatis-Plus 而非 JPA

**选择**: MyBatis-Plus

**理由**:
- 用户偏好：用户明确选择 MyBatis-Plus
- 灵活性：SQL 可控，适合复杂查询
- 国内生态：MyBatis-Plus 在国内 Java 生态中广泛使用

**替代方案**:
- Spring Data JPA: 与 Spring 生态更统一，但 SQL 不够透明

### Decision 4: 跨域 SSO 采用标准授权码流程 + 认证中心会话

**选择**: 基于 auth-server 统一会话的跨域 SSO

**架构**:
- auth-server 维护统一的用户会话 (JSESSIONID Cookie，domain=auth.local)
- 各客户端通过标准 OAuth2 授权码流程接入
- 用户在 auth-server 登录后，Cookie 记住会话
- 其他客户端跳转到 auth-server 时，检测到已有会话，直接发授权码

**替代方案**:
- 共享 Session (Redis): 需要所有应用连接同一个 Redis，侵入性强
- SAML: 企业级协议，过于复杂，不适合学习场景

### Decision 5: Vue SPA 使用 PKCE 而非 Client Secret

**选择**: PKCE (Proof Key for Code Exchange)

**理由**:
- SPA 是公开客户端，代码暴露在浏览器中，不能存储 secret
- PKCE 是 OAuth2.1 对公开客户端的强制要求
- Spring Authorization Server 原生支持 PKCE

**流程**:
1. 前端生成 code_verifier (随机字符串)
2. 计算 code_challenge = SHA256(code_verifier)
3. 授权请求携带 code_challenge
4. 换 Token 时携带原始 code_verifier
5. 服务端验证 SHA256(verifier) == challenge

### Decision 6: 项目结构采用 Maven 多模块

**结构**:
```
project-oauth2 (父 POM)
├── auth-server          (认证中心)
├── client-b             (Spring Boot 客户端)
├── resource-server      (预留骨架)
└── app-vue/             (独立 Vue 项目，非 Maven 模块)
```

**理由**:
- 模块间依赖清晰
- 可以统一管理依赖版本
- 各模块独立启动，符合微服务学习场景

### Decision 7: 端口和域名规划

| 模块 | 端口 | 域名 | 说明 |
|------|------|------|------|
| auth-server | :9000 | auth.local | 认证中心 |
| app-vue | :5173 | app-a.local | Vue 前端 (Vite dev server) |
| client-b | :8082 | app-b.local | Spring Boot 客户端 |
| resource-server | :8083 | api.local | 资源服务器 (预留) |

## Risks / Trade-offs

- **[JWT 无法即时撤销]** → 使用短过期时间 (access_token 30min) + 撤销端点 + 客户端 Token 刷新策略
- **[跨域 Cookie 限制]** → auth.local 的 Cookie 仅在 auth.local 域下有效，这是正常的；SSO 依赖浏览器跳转而非 Cookie 共享
- **[本地 hosts 配置]** → 开发者需要手动配置 hosts 文件，增加了环境搭建步骤；在 README 中提供详细说明
- **[HTTP 明文传输]** → 本地开发使用 HTTP，Token 在网络上明文传输；仅用于学习，生产环境必须 HTTPS
- **[单点故障]** → auth-server 是单点，宕机则所有客户端无法登录；学习场景可接受
