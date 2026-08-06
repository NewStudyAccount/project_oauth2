## ADDED Requirements

### Requirement: OAuth2 PKCE 授权码流程

Vue 前端 SHALL 实现完整的 OAuth2 PKCE 授权码流程。

#### Scenario: 发起登录
- **WHEN** 用户点击"登录"按钮
- **THEN** 前端生成 code_verifier 和 code_challenge，存储 verifier 到 sessionStorage，跳转到 auth-server 授权端点

#### Scenario: 处理授权回调
- **WHEN** auth-server 重定向回 Vue 前端回调地址，携带 code 和 state
- **THEN** 前端验证 state 参数，取出 sessionStorage 中的 code_verifier，向 Token 端点发起请求换取 Token

#### Scenario: PKCE 验证失败
- **WHEN** code_verifier 与 code_challenge 不匹配
- **THEN** Token 端点返回错误，前端显示登录失败提示

### Requirement: Token 存储与管理

Vue 前端 SHALL 安全存储和管理 Token。

#### Scenario: 存储 Token
- **WHEN** 成功获取 Token
- **THEN** 前端将 access_token、id_token、refresh_token 存储到 sessionStorage

#### Scenario: Token 过期
- **WHEN** access_token 过期，API 返回 401
- **THEN** 前端使用 refresh_token 自动刷新 access_token

#### Scenario: Refresh Token 也过期
- **WHEN** refresh_token 也过期或无效
- **THEN** 前端清除所有 Token，跳转到登录页

### Requirement: 用户信息展示

Vue 前端 SHALL 解析 id_token 并展示用户信息。

#### Scenario: 展示用户信息
- **WHEN** 用户登录成功
- **THEN** 前端解析 id_token (JWT) 中的 claims，展示用户名、邮箱等信息

#### Scenario: 未登录状态
- **WHEN** 用户未登录访问首页
- **THEN** 前端显示"未登录"状态和登录按钮

### Requirement: 登出处理

Vue 前端 SHALL 支持用户登出。

#### Scenario: 用户主动登出
- **WHEN** 用户点击"登出"按钮
- **THEN** 前端清除本地 Token，调用 auth-server 撤销 refresh_token，跳转到首页

#### Scenario: 被动登出 (Token 失效)
- **WHEN** 所有 Token 都失效且无法刷新
- **THEN** 前端自动跳转到登录页

### Requirement: 路由守卫

Vue 前端 SHALL 实现路由守卫保护需要认证的页面。

#### Scenario: 访问受保护路由
- **WHEN** 未登录用户访问需要认证的路由 (如 /profile)
- **THEN** 路由守卫拦截，跳转到登录流程

#### Scenario: 已登录用户访问受保护路由
- **WHEN** 已登录用户访问需要认证的路由
- **THEN** 路由守卫放行，正常展示页面

### Requirement: API 请求拦截器

Vue 前端 SHALL 使用 Axios 拦截器自动携带 Token。

#### Scenario: 自动附加 Token
- **WHEN** 前端发起 API 请求
- **THEN** Axios 请求拦截器自动在 Authorization 头中附加 Bearer access_token

#### Scenario: 自动刷新 Token
- **WHEN** API 返回 401
- **THEN** Axios 响应拦截器自动使用 refresh_token 刷新，失败后跳转登录
