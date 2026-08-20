# client-app 改造为公开客户端

## Why

当前 `client-app` 是一个前后端分离项目（Vue 前端 + Spring Boot 后端），但后端同时承担了 **OAuth2 Client**（登录换 Token）和 **Resource Server**（校验 Token）两个角色，存在以下问题：

- **client_secret 暴露风险**：secret 配在后端 application.yml，代码泄露则 secret 泄露
- **Session 状态**：后端需要维护 Session，不是真正的无状态架构
- **CSRF 依赖**：前端需要读 Cookie 中的 XSRF-TOKEN 放到请求头，增加了前端复杂度
- **前后端耦合**：前端登录依赖后端代理，后端既是 Client 又是 Resource Server
- **跨域复杂**：Cookie 跨域需要额外配置，限制了前后端独立部署
- **自测困难**：Token 存在后端 Session，前端无法直接获取，调试不便

需要将 client-app 改造为**公开客户端**模式：前端直接与认证中心交互获取 Token，后端降级为纯 Resource Server。

## What Changes

### 认证中心侧

- 修改数据库中 `springboot-app` 客户端配置：`client_authentication_methods` 改为 `none`，清空 `client_secret`，`require_proof_key` 改为 `true`，`redirect_uri` 改为前端回调地址 `http://client.a.local:5175/callback`

### client-app 后端

- 从 `pom.xml` 移除 `spring-boot-starter-oauth2-client` 依赖
- 简化 `SecurityConfig.java`：移除 `oauth2Login()`、CSRF、logout 配置，仅保留 `oauth2ResourceServer`
- 简化 `application.yml`：移除 `spring.security.oauth2.client.*` 配置
- 改造 `UserInfoApiController`：从 JWT 提取 username，调用 user-service 获取完整用户信息
- 改造 `AuthApiController`：移除后端登出逻辑（前端自行处理）

### client-app 前端

- 新增 `utils/pkce.js`：PKCE 工具
- 新增 `utils/auth.js`：授权 URL 构建、Token 交换、刷新、登出
- 新增 `stores/auth.js`：Pinia store 管理 Token 生命周期
- 新增 `views/CallbackView.vue`：OAuth2 回调页面
- 修改 `api/index.js`：请求头携带 `Authorization: Bearer <token>` 替代 Cookie，移除 CSRF 拦截器
- 修改 `router/index.js`：添加 `/callback` 路由，认证检查改为 Token 存在性
- 修改 `App.vue`：登录改为 `authStore.login()`，登出改为清除 Token + 跳转认证中心
- 修改 `main.js`：引入 Pinia
- 安装 `pinia` 和 `js-sha256` 依赖
- 更新 `vite.config.js`：移除 `/oauth2`、`/login`、`/logout` 代理（仅保留 `/api`）

## Capabilities

### New Capabilities

- `client-app-oauth2-pkce`: client-app 前端 OAuth2 PKCE 登录流程

### Modified Capabilities

- `client-app-security`: client-app 从机密客户端（Session + Cookie）改为公开客户端（PKCE + Bearer Token）

## Impact

- **后端 API 兼容性**: `/api/**` 端点本身不变，但认证方式从 Session 改为 Bearer Token
- **前端**: 需要新增 OAuth2 PKCE 登录流程、Token 管理
- **数据库**: 修改 `oauth2_registered_client` 表中 `springboot-app` 的配置
- **依赖**: 后端移除 oauth2-client；前端新增 pinia、js-sha256
- **安全性提升**: 消除 client_secret 暴露风险、消除 CSRF 风险、无状态架构
