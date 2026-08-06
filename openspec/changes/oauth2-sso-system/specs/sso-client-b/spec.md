## ADDED Requirements

### Requirement: OAuth2 客户端认证

client-b SHALL 作为 OAuth2 机密客户端 (Confidential Client) 接入认证中心。

#### Scenario: 访问受保护页面
- **WHEN** 用户访问 client-b 的受保护页面且未登录
- **THEN** 系统重定向到 auth-server 的授权端点，携带 client_id、redirect_uri、scope、state

#### Scenario: 处理授权回调
- **WHEN** auth-server 重定向回 client-b 的回调地址，携带授权码
- **THEN** 系统使用授权码 + client_secret 向 Token 端点换取 Token，创建本地会话

#### Scenario: 登录成功
- **WHEN** 成功获取 Token
- **THEN** 系统将用户信息存储到会话中，重定向到原始请求页面

### Requirement: SSO 自动登录

client-b SHALL 验证跨域 SSO 自动登录能力。

#### Scenario: 用户已在 auth-server 登录
- **WHEN** 用户在 auth-server 已有有效会话，访问 client-b 的受保护页面
- **THEN** 系统重定向到 auth-server，auth-server 检测到已有会话，直接返回授权码，用户无需再次输入密码

#### Scenario: 用户未在 auth-server 登录
- **WHEN** 用户在 auth-server 没有有效会话，访问 client-b 的受保护页面
- **THEN** 系统重定向到 auth-server，auth-server 显示登录页，用户登录后返回授权码

### Requirement: 用户信息展示

client-b SHALL 展示当前登录用户的信息。

#### Scenario: 展示用户信息
- **WHEN** 用户已登录
- **THEN** 页面展示用户名、邮箱、角色等信息

#### Scenario: 展示 Token 信息
- **WHEN** 用户已登录
- **THEN** 页面展示 access_token 的过期时间、scope 等信息（调试用）

### Requirement: 登出处理

client-b SHALL 支持用户登出。

#### Scenario: 用户登出
- **WHEN** 用户点击"登出"按钮
- **THEN** 系统清除本地会话，重定向到 auth-server 的登出端点，auth-server 清除会话后重定向回 client-b

### Requirement: 客户端配置

client-b SHALL 通过 application.yml 配置 OAuth2 客户端参数。

#### Scenario: 配置客户端参数
- **WHEN** 应用启动
- **THEN** 系统从 application.yml 读取 client-id、client-secret、authorization-uri、redirect-uri 等配置
