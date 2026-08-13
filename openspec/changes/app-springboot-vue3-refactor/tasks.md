## 1. 后端改造 - 移除模板渲染

- [x] 1.1 从 `app-springboot/pom.xml` 移除 `spring-boot-starter-thymeleaf` 和 `thymeleaf-extras-springsecurity6` 依赖
- [x] 1.2 删除 `src/main/resources/templates/index.html` 和 `profile.html`
- [x] 1.3 删除 `HomeController.java` 中的页面渲染方法（`home()` 和 `profile()`），保留或改写为 API 端点返回用户信息

## 2. 后端改造 - API 增强

- [x] 2.1 新增登出 API 端点 `POST /api/logout`，使 session 失效并返回 JSON 成功响应
- [x] 2.2 确保 `/api/userinfo` 端点返回完整的当前用户信息（sub、username、email、nickname）
- [x] 2.3 修改根路径 `/` 的处理：未认证时返回 401 JSON 响应或重定向到前端

## 3. 后端改造 - CORS 与 CSRF

- [x] 3.1 新建 `CorsConfig.java`，配置 CORS 允许 `http://localhost:5173`（开发环境），允许携带 cookie
- [x] 3.2 修改 `SecurityConfig.java`，配置 `CookieCsrfTokenRepository` 设置 `HttpOnly=false`，使前端可读取 XSRF-TOKEN cookie
- [x] 3.3 配置 CSRF cookie 的 SameSite 策略为 Lax

## 4. 后端改造 - 配置更新

- [x] 4.1 更新 `application.yml`，移除 Thymeleaf 相关配置
- [x] 4.2 验证现有 `/api/public/**`、`/api/protected/**`、`/api/userinfo/**` 端点正常工作

## 5. 前端项目初始化

- [x] 5.1 创建 `app-vue3-springboot/` 目录，使用 Vite 初始化 Vue3 项目（`npm create vue@latest`）
- [x] 5.2 配置 `package.json` 依赖：vue-router、axios
- [x] 5.3 配置 `vite.config.ts` 代理：`/api`、`/oauth2`、`/login`、`/logout` 代理到 `http://localhost:8082`
- [x] 5.4 创建项目基础结构：`src/views/`、`src/router/`、`src/api/`、`src/utils/`

## 6. 前端实现 - 路由与布局

- [x] 6.1 配置 Vue Router：`/`（首页）、`/profile`（用户资料页）
- [x] 6.2 创建 `App.vue` 布局组件，包含导航栏（登录/登出状态切换）
- [x] 6.3 创建 `axios` 实例配置，设置 `withCredentials: true`，自动携带 XSRF-TOKEN

## 7. 前端实现 - 页面

- [x] 7.1 实现首页 `HomeView.vue`：调用 `/api/userinfo` 获取用户信息，已登录显示用户信息，未登录显示登录按钮
- [x] 7.2 实现用户资料页 `ProfileView.vue`：调用 `/api/userinfo/claims` 获取所有 claims，以 key-value 列表展示
- [x] 7.3 实现登录功能：点击登录按钮重定向到 `/oauth2/authorization/springboot-app`
- [x] 7.4 实现登出功能：调用 `POST /api/logout`，清除本地状态，重定向到首页

## 8. 前端实现 - 错误处理

- [x] 8.1 实现 401 响应拦截器：自动引导用户登录
- [x] 8.2 实现 CSRF token 处理：Axios 请求拦截器从 cookie 读取 XSRF-TOKEN

## 9. 联调与验证

- [x] 9.1 启动 auth-server（9000）和 app-springboot（8082），验证后端 API 正常
- [x] 9.2 启动 Vue3 前端（5173），验证 Vite 代理正常工作
- [x] 9.3 验证完整登录流程：前端登录 → OAuth2 回调 → 显示用户信息
- [x] 9.4 验证登出流程：前端登出 → session 失效 → 返回未登录状态
- [x] 9.5 验证受保护 API 调用：已认证可访问，未认证返回 401
