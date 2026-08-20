# client-app 公开客户端改造 — 任务清单

## 1. 认证中心侧改造

- [x] 1.1 修改数据库中 `springboot-app` 客户端配置：`client_authentication_methods` 改为 `none`，清空 `client_secret`，`require_proof_key` 改为 `true`，`redirect_uri` 改为 `http://client.a.local:5175/callback`
- [x] 1.2 更新 `auth-center/backend/src/main/resources/db/data.sql` 中 `springboot-app` 的 INSERT 语句

## 2. client-app 后端改造

- [x] 2.1 从 `client-app/backend/pom.xml` 移除 `spring-boot-starter-oauth2-client` 依赖
- [x] 2.2 简化 `SecurityConfig.java`：移除 `oauth2Login()`、CSRF、logout 配置，仅保留 `oauth2ResourceServer` + `authorizeHttpRequests`
- [x] 2.3 简化 `application.yml`：移除 `spring.security.oauth2.client.*` 配置，保留 `spring.security.oauth2.resourceserver.jwt.*`
- [x] 2.4 改造 `UserInfoApiController`：从 JWT 提取 username，调用 `UserServiceClient` 获取完整用户信息
- [x] 2.5 改造 `AuthApiController`：移除后端登出逻辑，保留 `/api/status` 端点

## 3. client-app 前端改造

- [x] 3.1 安装 `pinia` 和 `js-sha256` 依赖
- [x] 3.2 创建 `src/utils/pkce.js` PKCE 工具
- [x] 3.3 创建 `src/utils/auth.js` 授权 URL 构建、Token 交换、刷新、登出
- [x] 3.4 创建 `src/stores/auth.js` Pinia store 管理 Token 生命周期
- [x] 3.5 创建 `src/views/CallbackView.vue` OAuth2 回调页面
- [x] 3.6 修改 `src/api/index.js`：请求头携带 Bearer Token，移除 CSRF 拦截器和 `withCredentials`
- [x] 3.7 修改 `src/router/index.js`：添加 `/callback` 路由，认证检查改为 Token 存在性
- [x] 3.8 修改 `src/App.vue`：登录改为 `authStore.login()`，登出改为清除 Token + 跳转认证中心
- [x] 3.9 修改 `src/main.js`：引入 Pinia
- [x] 3.10 更新 `vite.config.js`：移除 `/oauth2`、`/login`、`/logout` 代理（仅保留 `/api`）

## 4. 验证

- [ ] 4.1 访问 client-app 前端，验证未登录时跳转到认证中心登录页
- [ ] 4.2 在认证中心登录后，验证回调到 client-app 前端并获取 Token
- [ ] 4.3 验证前端 localStorage 中存储了 access_token
- [ ] 4.4 验证 `/api/userinfo` 携带 Bearer Token 返回完整用户信息（含 user-service 数据）
- [ ] 4.5 验证登出流程：清除 Token → 跳转认证中心登出 → 回到首页
- [ ] 4.6 验证无 Token 的 `/api/**` 请求返回 401
