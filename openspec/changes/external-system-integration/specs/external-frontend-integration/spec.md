## Purpose

定义外部前端系统直接接入 SSO 平台的能力，展示如何使用 OAuth2 PKCE 流程获取 Token，作为独立的前端示例。

## ADDED Requirements

### Requirement: OAuth2 PKCE 流程
外部前端系统 SHALL 使用 OAuth2 Authorization Code Flow with PKCE 获取 Token，直接与 auth-server 交互。

#### Scenario: 启动授权流程
- **WHEN** 用户点击登录按钮
- **THEN** 系统 SHALL 生成 PKCE 参数（code_verifier 和 code_challenge）
- **THEN** 系统 SHALL 重定向到 auth-server 的授权端点，携带 PKCE 参数

#### Scenario: 处理授权回调
- **WHEN** auth-server 重定向回前端，携带授权码
- **THEN** 系统 SHALL 验证 state 参数
- **THEN** 系统 SHALL 使用授权码和 code_verifier 向 auth-server 请求 Token

### Requirement: Token 管理
外部前端系统 SHALL 独立管理 Token，不依赖 Gateway。

#### Scenario: 存储 Token
- **WHEN** 成功获取 Token
- **THEN** 系统 SHALL 将 access_token 和 refresh_token 存储在本地（localStorage 或内存）

#### Scenario: 刷新 Token
- **WHEN** access_token 过期
- **THEN** 系统 SHALL 使用 refresh_token 获取新的 access_token

#### Scenario: 清除 Token
- **WHEN** 用户登出
- **THEN** 系统 SHALL 清除所有存储的 Token 和用户信息

### Requirement: 用户信息获取
外部前端系统 SHALL 能够获取用户信息。

#### Scenario: 获取用户信息
- **WHEN** 用户登录成功
- **THEN** 系统 SHALL 调用 auth-server 的 userinfo 端点获取用户信息
- **THEN** 系统 SHALL 在界面显示用户信息

#### Scenario: 显示用户信息
- **WHEN** 用户访问个人中心页面
- **THEN** 系统 SHALL 显示从 userinfo 端点获取的用户信息
