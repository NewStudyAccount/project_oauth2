## Purpose

client-app 从机密客户端改造为公开客户端，前端通过 PKCE 流程获取 Token，后端降级为纯 Resource Server。

## MODIFIED Requirements

### Requirement: springboot-app 客户端配置

`springboot-app` 客户端 SHALL 配置为公开客户端，支持 PKCE。

#### Scenario: 客户端配置
- **WHEN** 查询 `oauth2_registered_client` 表中 client_id 为 `springboot-app` 的记录
- **THEN** client_authentication_methods 为 `none`，client_secret 为空，require_proof_key 为 `true`，redirect_uri 为 `http://client.a.local:5175/callback`

### Requirement: client-app 后端纯 Resource Server

client-app 后端 SHALL 仅作为 Resource Server，不参与 OAuth2 登录流程。

#### Scenario: 后端校验 Token
- **WHEN** 请求 `/api/**` 且 Authorization 头包含有效的 Bearer Token（aud 包含 `springboot-app`）
- **THEN** 正常处理请求

#### Scenario: 后端不持有 client_secret
- **WHEN** 检查 client-app 后端配置
- **THEN** 不包含 `spring.security.oauth2.client.*` 配置

### Requirement: client-app 前端 PKCE 登录

client-app 前端 SHALL 实现 OAuth2 授权码模式 + PKCE 登录流程。

#### Scenario: 未登录访问
- **WHEN** 用户未登录访问 client-app 前端
- **THEN** 跳转到认证中心的 `/oauth2/authorize` 端点，携带 code_challenge

#### Scenario: Token 交换
- **WHEN** 前端收到回调中的 authorization_code
- **THEN** 使用 code + code_verifier 调用认证中心换取 access_token，存入 localStorage

### Requirement: client-app 前端 API 调用

client-app 前端 SHALL 通过 Bearer Token 调用后端 API。

#### Scenario: 携带 Token 调用
- **WHEN** 前端发起 `/api/**` 请求
- **THEN** 请求头自动添加 `Authorization: Bearer <token>`

#### Scenario: 401 响应处理
- **WHEN** API 返回 401
- **THEN** 清除本地 Token，跳转到登录流程

### Requirement: 后端获取用户信息

client-app 后端 SHALL 从 JWT 提取 username，调用 user-service 获取完整用户信息。

#### Scenario: 获取用户信息
- **WHEN** 请求 `/api/userinfo` 且持有有效 JWT
- **THEN** 从 JWT 提取 username，调用 `UserServiceClient.getUserByUsername()` 返回完整用户资料
