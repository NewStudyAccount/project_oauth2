## Purpose

Vue3 前端 SPA 应用，替代 app-springboot 的 Thymeleaf 模板，通过调用后端 BFF API 展示用户信息和受保护资源，提供登录/登出交互。

## ADDED Requirements

### Requirement: 首页展示
前端 SHALL 提供首页，展示当前登录用户的基本信息（nickname、username、email）。未登录时 SHALL 显示登录入口。

#### Scenario: 已登录用户访问首页
- **WHEN** 用户已通过 OAuth2 登录，访问首页
- **THEN** 页面显示用户的 nickname、username、email 信息，以及指向用户资料页的导航链接

#### Scenario: 未登录用户访问首页
- **WHEN** 用户未登录，访问首页
- **THEN** 页面显示登录按钮，点击后跳转到后端的 OAuth2 登录发起端点

### Requirement: 用户资料页
前端 SHALL 提供用户资料页，展示当前用户的所有 OIDC claims 信息。

#### Scenario: 已登录用户查看资料
- **WHEN** 用户已登录，访问用户资料页
- **THEN** 页面以 key-value 列表形式展示所有 OIDC claims

#### Scenario: 未登录用户访问资料页
- **WHEN** 用户未登录，访问用户资料页
- **THEN** 系统 SHALL 将用户重定向到登录流程

### Requirement: 登录流程
前端 SHALL 通过重定向到后端 OAuth2 登录发起端点完成登录，登录成功后 SHALL 将用户重定向回前端页面。

#### Scenario: 发起登录
- **WHEN** 用户点击登录按钮
- **THEN** 浏览器 SHALL 重定向到后端的 `/oauth2/authorization/springboot-app` 端点

#### Scenario: 登录成功回调
- **WHEN** OAuth2 登录流程完成，后端设置 session cookie
- **THEN** 前端 SHALL 重定向到首页，并能通过 session cookie 访问受保护的 API

### Requirement: 登出流程
前端 SHALL 提供登出功能，清除后端 session 并可选择清除 SSO 会话。

#### Scenario: 用户登出
- **WHEN** 用户点击登出按钮
- **THEN** 前端 SHALL 调用后端登出 API，清除本地状态，并重定向到首页（未登录状态）

### Requirement: 受保护 API 调用
前端 SHALL 能够调用后端的受保护 API 端点，通过 session cookie 进行身份认证。

#### Scenario: 调用已认证 API
- **WHEN** 已登录用户触发受保护 API 调用
- **THEN** 前端 SHALL 携带 session cookie 发送请求，收到 JSON 响应

#### Scenario: API 返回 401
- **WHEN** 未登录用户尝试调用受保护 API
- **THEN** 前端 SHALL 处理 401 响应，引导用户登录

### Requirement: CSRF 处理
前端 SHALL 正确处理 Spring Security 的 CSRF 保护机制。

#### Scenario: 发送需要 CSRF 的请求
- **WHEN** 前端发送 POST/PUT/DELETE 请求到后端
- **THEN** 前端 SHALL 从 cookie 中读取 XSRF-TOKEN 并在请求头中携带 X-XSRF-TOKEN

### Requirement: 前端独立部署
前端 SHALL 作为独立的 Vue3 + Vite 项目运行，通过 Vite 代理解决开发环境跨域问题。

#### Scenario: 开发环境启动
- **WHEN** 开发者运行 `npm run dev`
- **THEN** 前端 SHALL 在独立端口启动，并通过 Vite proxy 将 `/api`、`/oauth2`、`/logout` 请求代理到后端 8082 端口
