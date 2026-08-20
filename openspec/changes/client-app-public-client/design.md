# client-app 公开客户端改造设计

## Context

client-app 是一个前后端分离项目：
- **前端**：Vue 3 + Vite，运行在 `client.a.local:5175`
- **后端**：Spring Boot，运行在 `client.a.local:8082`
- **认证中心**：auth-center，运行在 `auth.local:9000`

当前后端同时承担 OAuth2 Client 和 Resource Server 两个角色：
- `oauth2-client`：用 `client_secret` 与 auth-center 交互获取 Token，存入 Session
- `oauth2-resource-server`：校验 JWT Bearer Token

前端通过 Cookie（JSESSIONID）+ CSRF Token 与后端通信。

## Goals / Non-Goals

**Goals:**
1. client-app 改为公开客户端，前端直接与认证中心交互获取 Token
2. 后端降级为纯 Resource Server，只校验 JWT
3. 前端使用 PKCE 流程获取 Token，存入 localStorage
4. 前端通过 `Authorization: Bearer <token>` 调用后端 API
5. 后端从 JWT 提取 username，调用 user-service 获取完整用户信息

**Non-Goals:**
- 不改变 auth-center 的 OAuth2 协议端点
- 不改变 user-service 的 API
- 不改变 client-app 的业务逻辑

## Decisions

### Decision 1: 客户端类型

**选择**: 将现有 `springboot-app` 从机密客户端改为公开客户端

**替代方案**:
- A: 新建一个公开客户端 `client-app-frontend`，保留原有机密客户端
- B: 保留机密客户端，前端通过后端代理登录

**理由**: client-app 的后端不再需要作为 OAuth2 Client 登录，直接修改现有客户端更简洁。如果未来后端需要调用其他服务，可以通过 JWT 直接调用（aud 匹配即可）。

### Decision 2: Token 存储

**选择**: localStorage 存储 access_token 和 refresh_token

**替代方案**:
- A: SessionStorage（标签页关闭后丢失）
- B: 内存（刷新页面后丢失）

**理由**: 前端需要跨标签页共享登录状态。

### Decision 3: 后端获取用户信息方式

**选择**: 后端从 JWT 提取 username，调用 user-service 获取完整用户信息

**替代方案**:
- A: 后端仅使用 JWT 中的信息（nickname/email/phone 已在 JWT 中）
- B: 后端维护自己的用户缓存

**理由**: JWT 中已包含基本用户信息（username/nickname/email/phone），但调用 user-service 可以获取最新数据。JWT 中的信息是签发时的快照，user-service 的数据是实时的。

### Decision 4: 登出流程

**选择**: 前端清除 localStorage 中的 Token，跳转认证中心 `/logout`

**理由**: 与 admin-frontend 保持一致，确保认证中心的服务端 Session 也被销毁。

## Risks / Trade-offs

- **[风险] Token 泄露**: localStorage 存储 Token 可被 XSS 攻击窃取 → 前端需做好 XSS 防护
- **[权衡] 后端无法主动踢人**: JWT 无状态，后端无法主动使 Token 失效 → 可通过 Token 短有效期 + refresh_token 机制缓解
- **[权衡] 前端复杂度增加**: 需要实现 PKCE + Token 管理 → 参考 standalone-app 和 admin-frontend 已有成熟实现

## Open Questions

无
