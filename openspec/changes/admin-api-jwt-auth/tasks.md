# 管理后台 API JWT 认证改造 — 任务清单

## 1. auth-center 后端改造

- [x] 1.1 在数据库中注册 `admin-frontend` OAuth2 客户端（授权码模式，PKCE 必选，redirect_uri: `http://auth.local:5174/callback`）
- [x] 1.2 在 `SecurityConfig` 中新增 `/api/**` 的 Resource Server FilterChain（`@Order(3)`），配置 JWT 认证
- [x] 1.3 从 defaultFilterChain 中移除 `/api/**` 的 CSRF 豁免
- [x] 1.4 在 `CorsConfig` 中添加 `http://auth.local:5174` 到允许的来源列表（已包含）

## 2. admin 前端改造 — OAuth2 PKCE 登录

- [x] 2.1 创建 `src/utils/pkce.js` PKCE 工具（复用 standalone-app 的实现）
- [x] 2.2 创建 `src/utils/auth.js` 授权 URL 构建、Token 交换、刷新、登出工具函数
- [x] 2.3 创建 `src/stores/auth.js` Pinia store 管理 Token 生命周期（login, handleCallback, refresh, logout）
- [x] 2.4 创建 `src/views/CallbackView.vue` OAuth2 回调页面（处理 code 换 token）
- [x] 2.5 修改 `src/router/index.js`：添加 `/callback` 路由，认证检查改为检查 token 存在性
- [x] 2.6 修改 `src/api/index.js`：请求头携带 `Authorization: Bearer <token>`，移除 `withCredentials`，401 时跳转登录
- [x] 2.7 修改 `src/components/AppLayout.vue`：登出改为清除 token + 跳转认证中心登出
- [x] 2.8 修改 `src/main.js`：引入 Pinia
- [x] 2.9 更新 `vite.config.ts`：移除 `/login`、`/logout`、`/register`、`/send-code`、`/consent` 代理（仅保留 `/api`）

## 3. 验证

- [ ] 3.1 访问 admin 前端，验证未登录时跳转到认证中心登录页
- [ ] 3.2 在认证中心登录后，验证回调到 admin 前端并获取 token
- [ ] 3.3 验证管理 API 调用携带 Bearer Token 正常工作
- [ ] 3.4 验证登出流程：清除 token → 跳转认证中心登出 → 回到登录页
- [ ] 3.5 验证 CSRF 保护生效：不带 token 的 `/api/**` 请求返回 401
