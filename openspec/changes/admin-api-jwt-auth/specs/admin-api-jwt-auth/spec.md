## Purpose

管理后台 API 改为 JWT Resource Server 认证，admin 前端通过 OAuth2 PKCE 流程从统一认证中心获取 Token。

## ADDED Requirements

### Requirement: admin-frontend OAuth2 客户端注册

auth-center SHALL 在数据库中注册 `admin-frontend` OAuth2 客户端，支持授权码模式和 PKCE。

#### Scenario: 客户端配置
- **WHEN** 查询 `oauth2_registered_client` 表中 client_id 为 `admin-frontend` 的记录
- **THEN** 包含以下配置：client_authentication_methods=none, authorization_grant_types=authorization_code, redirect_uris=http://auth.local:5174/callback, scopes=openid profile email, require_proof_key=true

### Requirement: admin 前端 PKCE 登录

admin 前端 SHALL 实现 OAuth2 授权码模式 + PKCE 登录流程。

#### Scenario: 未登录访问管理页面
- **WHEN** 用户未登录访问 admin 前端的管理页面
- **THEN** 跳转到认证中心的 `/oauth2/authorize` 端点，携带 code_challenge 和 state 参数

#### Scenario: 认证中心登录成功
- **WHEN** 用户在认证中心完成登录
- **THEN** 重定向到 admin 前端的 `/callback` 页面，携带 authorization_code 和 state

#### Scenario: Token 交换
- **WHEN** admin 前端收到回调中的 authorization_code
- **THEN** 使用 code + code_verifier 调用认证中心的 `/oauth2/token` 端点换取 access_token

#### Scenario: state 校验
- **WHEN** 回调中的 state 与本地存储的 state 不匹配
- **THEN** 拒绝 Token 交换，报错

### Requirement: /api/** JWT Resource Server 认证

auth-center 的 `/api/**` 端点 SHALL 使用 JWT Bearer Token 认证。

#### Scenario: 携带有效 Token 访问
- **WHEN** 请求 `/api/admin/**` 且 Authorization 头包含有效的 Bearer Token
- **THEN** 正常处理请求，返回 200

#### Scenario: 未携带 Token 访问
- **WHEN** 请求 `/api/admin/**` 且未携带 Authorization 头
- **THEN** 返回 401 Unauthorized

#### Scenario: Token 过期
- **WHEN** 请求 `/api/admin/**` 且 Token 已过期
- **THEN** 返回 401 Unauthorized

### Requirement: admin 前端 Token 管理

admin 前端 SHALL 在 localStorage 中管理 Token 生命周期。

#### Scenario: Token 存储
- **WHEN** 成功获取 access_token
- **THEN** 存储到 localStorage

#### Scenario: API 请求携带 Token
- **WHEN** 发起 `/api/admin/**` 请求
- **THEN** 请求头自动添加 `Authorization: Bearer <token>`

#### Scenario: 401 响应处理
- **WHEN** API 返回 401
- **THEN** 清除本地 Token，跳转到登录流程

### Requirement: 登出流程

admin 前端 SHALL 实现完整的登出流程。

#### Scenario: 用户点击登出
- **WHEN** 用户点击登出按钮
- **THEN** 清除 localStorage 中的 Token，跳转到认证中心的 `/logout` 端点

#### Scenario: 认证中心登出
- **WHEN** 认证中心处理完登出请求
- **THEN** 销毁 Session，重定向到登录页面

## MODIFIED Requirements

### Requirement: /api/** CSRF 保护

auth-center 的 `/api/**` 端点 SHALL 不再豁免 CSRF 保护（JWT 认证天然免疫 CSRF）。

#### Scenario: CSRF 豁免移除
- **WHEN** 检查 SecurityConfig 的 CSRF 配置
- **THEN** `/api/**` 不在 `ignoringRequestMatchers` 列表中
