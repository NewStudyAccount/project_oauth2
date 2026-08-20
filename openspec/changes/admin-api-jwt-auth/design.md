# 管理后台 API JWT 认证设计

## Context

auth-center 的管理后台 API（`/api/admin/**`）当前使用 Spring Security 的表单登录 + Session 认证：

1. Vue 管理前端（auth.local:5174）通过 Vite 代理将 `/api` 请求转发到 auth-center（auth.local:9000）
2. 未认证时重定向到 `/login` 页面
3. 登录后通过 JSESSIONID Cookie 维持会话
4. `/api/**` 豁免了 CSRF 保护

auth-center 同时是 OAuth2 授权服务器，已具备签发 JWT 的能力。其他服务（client-app、resource-api）已使用 JWT 认证。

## Goals / Non-Goals

**Goals:**
1. `/api/admin/**` 改为 JWT Resource Server 认证（Bearer Token）
2. admin 前端通过 OAuth2 授权码流程 + PKCE 从认证中心获取 JWT
3. 移除 `/api/**` 的 CSRF 豁免和 Session 依赖
4. 登出通过清除本地 Token + 跳转认证中心登出端点实现

**Non-Goals:**
- 不改变 OAuth2 协议端点（`/oauth2/**`）的认证方式
- 不改变页面端点（`/login`、`/register`、`/consent`）的认证方式
- 不改变其他服务（client-app、resource-api）的认证方式

## Decisions

### Decision 1: admin 前端认证方式

**选择**: OAuth2 授权码模式 + PKCE（公开客户端，无 client_secret）

**替代方案**:
- A: OAuth2 授权码模式 + client_secret（机密客户端）
- B: Resource Owner Password Grant（密码模式，已废弃）
- C: 保持表单登录 + Session

**理由**: PKCE 是公开客户端（浏览器 SPA）的安全最佳实践，无需在前端暴露 client_secret。参考 standalone-app 已有的 PKCE 实现。

### Decision 2: admin-frontend 客户端注册

**选择**: 在 auth-center 数据库中注册 `admin-frontend` 客户端，授权码模式，PKCE 必选

**注册信息**:
- client_id: `admin-frontend`
- client_authentication_method: `none`（公开客户端）
- authorization_grant_types: `authorization_code`
- redirect_uris: `http://auth.local:5174/callback`
- scopes: `openid,profile,email`
- require_proof_key: `true`（强制 PKCE）
- require_authorization_consent: `false`（管理后台不需要每次授权确认）

### Decision 3: Token 存储方式

**选择**: localStorage 存储 access_token 和 refresh_token

**替代方案**:
- A: SessionStorage（标签页关闭后丢失）
- B: Cookie（与 Session 认证类似，增加 XSS 风险）
- C: 内存（刷新页面后丢失）

**理由**: 管理后台需要跨标签页共享登录状态，localStorage 最合适。配合 Token 过期自动刷新机制。

### Decision 4: /api/** 认证配置方式

**选择**: 为 `/api/**` 创建独立的 SecurityFilterChain（`@Order(3)`），配置为 Resource Server

**替代方案**:
- A: 在现有 defaultFilterChain 中添加 oauth2ResourceServer 配置
- B: 使用 Gateway 层面的 JWT 校验

**理由**: 独立的 FilterChain 职责更清晰，避免与表单登录配置混在一起。现有 defaultFilterChain 继续处理页面端点（`/login`、`/consent` 等）。

### Decision 5: 登出流程

**选择**: 前端清除 localStorage 中的 Token，然后跳转到认证中心的登出端点

**流程**:
1. 前端清除 localStorage 中的 access_token / refresh_token / user_info
2. 前端跳转到 `http://auth.local:9000/logout`（认证中心的登出端点）
3. 认证中心销毁 Session，重定向到 `/login?logout`

**替代方案**:
- A: 前端调用后端 `/api/logout` 接口
- B: 仅清除前端 Token，不通知认证中心

**理由**: 通知认证中心可以同时销毁服务端 Session，确保完全登出。

## Risks / Trade-offs

- **[风险] Token 泄露**: localStorage 存储 Token 可被 XSS 攻击窃取 → 前端需做好 XSS 防护（CSP、输入过滤），Token 有效期不宜过长
- **[权衡] 前端复杂度增加**: 需要实现 PKCE、Token 管理、自动刷新 → 参考 standalone-app 已有成熟实现，可复用代码
- **[权衡] 页面端点仍用 Session**: `/login`、`/consent` 等页面端点保持 Session 认证 → 这些是 Thymeleaf 渲染的页面，Session 认证更合适
- **[权衡] 开发环境跨域**: admin 前端（auth.local:5174）和 auth-center（auth.local:9000）不同域 → 需要配置 CORS 允许 auth.local:5174 访问 `/oauth2/**` 和 `/api/**`

## Open Questions

- 是否需要支持 refresh_token 自动续期？（建议支持，避免用户频繁重新登录）

## Migration Plan

1. 在 auth-center 数据库中注册 `admin-frontend` 客户端
2. 改造 auth-center SecurityConfig，为 `/api/**` 添加 Resource Server FilterChain
3. 改造 admin 前端，实现 OAuth2 PKCE 登录流程
4. 验证完整流程：登录 → API 调用 → 登出

**回滚策略**: 保留现有 Session 认证代码（注释状态），如需回滚只需恢复 CSRF 豁免和 Session 认证配置。
