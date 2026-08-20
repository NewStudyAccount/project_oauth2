# 管理后台 API 改造为 JWT 认证

## Why

当前 auth-center 的管理后台 API（`/api/admin/**`）使用 Session Cookie 认证（表单登录），存在以下问题：

- **CSRF 风险**：`/api/**` 豁免了 CSRF 保护，但实际依赖 Session Cookie，恶意网站可伪造请求
- **架构不统一**：client-app、resource-api 等其他服务已使用 JWT 认证，唯独管理后台 API 仍用 Session
- **登录不走统一认证**：admin 前端直接访问 `/login` 表单登录，未走 OAuth2 统一认证流程
- **前后端耦合**：Session 认证要求前端和后端同域或通过代理，限制了部署灵活性

需要将 `/api/**` 改造为 JWT Resource Server 认证，admin 前端作为 OAuth2 Client 通过统一认证中心获取 Token。

## What Changes

### auth-center 后端

- 在 `AuthorizationServerConfig` 中注册 `admin-frontend` OAuth2 客户端（授权码模式，PKCE 支持）
- 新增 `/api/**` 的 Resource Server 配置，校验 JWT Bearer Token
- 移除 `/api/**` 的 CSRF 豁免（JWT 无状态，无需 CSRF）
- 移除 `/api/**` 的 Session 认证依赖
- 更新 `SecurityConfig` 中 `/api/**` 的认证方式为 JWT

### admin 前端（auth-server-frontend）

- 新增 OAuth2 PKCE 登录流程（参考 standalone-app 的实现）
- 新增 `stores/auth.js` Pinia store 管理 Token 生命周期
- 新增 `utils/pkce.js` PKCE 工具
- 新增 `utils/auth.js` 授权 URL 构建、Token 交换
- 新增 `/callback` 页面处理 OAuth2 回调
- 修改 `api/index.js`：请求头携带 `Authorization: Bearer <token>` 替代 `withCredentials`
- 修改 `router/index.js`：检查 token 存在性替代检查 session
- 修改 `AppLayout.vue`：登出改为清除 token + 跳转认证中心登出
- 更新 `vite.config.ts`：移除 `/login`、`/logout`、`/register` 等代理（不再需要）

## Capabilities

### New Capabilities

- `admin-frontend-oauth2`: 管理后台前端 OAuth2 PKCE 登录流程

### Modified Capabilities

- `admin-api-security`: 管理后台 API 从 Session 认证改为 JWT Resource Server 认证

## Impact

- **后端 API 兼容性**: `/api/**` 端点本身不变，但认证方式从 Session 改为 Bearer Token，调用方式需同步修改
- **前端**: admin 前端需新增 OAuth2 登录流程、Token 管理、PKCE 支持
- **数据库**: 无需变更
- **依赖**: auth-center 无需新增依赖（已有 oauth2-resource-server）；admin 前端新增 pinia 依赖
- **配置变更**: 新增 `admin-frontend` OAuth2 客户端注册
- **安全性提升**: 消除 CSRF 风险，统一认证方式
