## ADDED Requirements

### Requirement: OAuth2 授权端点

认证中心 SHALL 提供标准 OAuth2 授权端点 `GET /oauth2/authorize`，支持授权码流程。

#### Scenario: 未登录用户访问授权端点
- **WHEN** 用户浏览器跳转到 `/oauth2/authorize` 且未携带有效会话 Cookie
- **THEN** 系统返回登录页面，用户输入账号密码后生成授权码并重定向回客户端

#### Scenario: 已登录用户访问授权端点
- **WHEN** 用户浏览器跳转到 `/oauth2/authorize` 且携带有效会话 Cookie
- **THEN** 系统直接生成授权码并重定向回客户端，无需再次登录

#### Scenario: 无效的 redirect_uri
- **WHEN** 客户端请求中携带的 redirect_uri 未在注册信息中配置
- **THEN** 系统返回错误，拒绝授权

### Requirement: OAuth2 Token 端点

认证中心 SHALL 提供标准 Token 端点 `POST /oauth2/token`，支持授权码换 Token 和刷新 Token。

#### Scenario: 授权码换 Token (PKCE)
- **WHEN** 客户端使用授权码 + code_verifier 请求 Token
- **THEN** 系统验证 SHA256(code_verifier) == code_challenge，返回 access_token、id_token、refresh_token

#### Scenario: 授权码换 Token (Client Secret)
- **WHEN** 机密客户端使用授权码 + client_secret 请求 Token
- **THEN** 系统验证 client_secret，返回 access_token、id_token、refresh_token

#### Scenario: 授权码已使用
- **WHEN** 客户端使用已使用过的授权码请求 Token
- **THEN** 系统拒绝请求，撤销该授权码关联的所有 Token

### Requirement: OAuth2 撤销端点

认证中心 SHALL 提供 Token 撤销端点 `POST /oauth2/revoke`。

#### Scenario: 撤销 access_token
- **WHEN** 客户端请求撤销有效的 access_token
- **THEN** 系统将该 Token 标记为已撤销，后续验证返回无效

#### Scenario: 撤销 refresh_token
- **WHEN** 客户端请求撤销有效的 refresh_token
- **THEN** 系统将该 Token 及其关联的 access_token 标记为已撤销

### Requirement: OIDC Discovery 端点

认证中心 SHALL 提供 OpenID Connect Discovery 端点 `GET /.well-known/openid-configuration`。

#### Scenario: 获取 OIDC 配置
- **WHEN** 客户端请求 `/.well-known/openid-configuration`
- **THEN** 系统返回 JSON，包含 issuer、authorization_endpoint、token_endpoint、jwks_uri、userinfo_endpoint 等

### Requirement: JWKS 端点

认证中心 SHALL 提供 JWKS 端点 `GET /oauth2/jwks`，发布公钥用于 JWT 验证。

#### Scenario: 获取公钥集
- **WHEN** 客户端请求 `/oauth2/jwks`
- **THEN** 系统返回 JSON Web Key Set，包含用于签名 Token 的 RSA 公钥

### Requirement: 用户信息端点

认证中心 SHALL 提供 OIDC UserInfo 端点 `GET /userinfo`。

#### Scenario: 使用有效 Token 获取用户信息
- **WHEN** 客户端携带有效 access_token 请求 `/userinfo`
- **THEN** 系统返回用户的 sub、name、email 等 claims

#### Scenario: 使用无效 Token 请求
- **WHEN** 客户端携带无效或过期的 access_token 请求 `/userinfo`
- **THEN** 系统返回 401 Unauthorized

### Requirement: 自定义登录页面

认证中心 SHALL 提供自定义的登录页面，替代 Spring Security 默认登录页。

#### Scenario: 访问登录页
- **WHEN** 用户访问需要认证的页面
- **THEN** 系统展示自定义登录页面，包含用户名、密码输入框和登录按钮

#### Scenario: 登录成功
- **WHEN** 用户输入正确的用户名和密码
- **THEN** 系统创建会话，设置 Cookie，重定向到原始请求页面

#### Scenario: 登录失败
- **WHEN** 用户输入错误的用户名或密码
- **THEN** 系统返回登录页面，显示错误提示

### Requirement: 用户认证 (MyBatis-Plus)

认证中心 SHALL 使用 MyBatis-Plus 从 MySQL 数据库加载用户信息进行认证。

#### Scenario: 加载用户信息
- **WHEN** 用户提交登录表单
- **THEN** 系统通过 MyBatis-Plus 查询 sys_user 表，验证密码 (BCrypt)

#### Scenario: 用户被禁用
- **WHEN** 被禁用的用户 (enabled=false) 尝试登录
- **THEN** 系统拒绝登录，返回用户被禁用的错误提示

### Requirement: 客户端注册管理

认证中心 SHALL 支持通过数据库管理 OAuth2 客户端注册信息。

#### Scenario: 查询已注册客户端
- **WHEN** 客户端发起授权请求
- **THEN** 系统从 oauth2_registered_client 表查询客户端配置 (client_id、redirect_uris、scopes、grant_types)

#### Scenario: 客户端不存在
- **WHEN** 请求中携带未注册的 client_id
- **THEN** 系统返回错误，提示客户端未注册

### Requirement: CORS 配置

认证中心 SHALL 配置精确的 CORS 策略，允许跨域客户端访问 Token 端点。

#### Scenario: 允许已注册域的跨域请求
- **WHEN** 已注册的客户端域 (app-a.local:5173) 发起跨域请求到 Token 端点
- **THEN** 系统返回正确的 CORS 头 (Access-Control-Allow-Origin)

#### Scenario: 拒绝未注册域的跨域请求
- **WHEN** 未注册的域发起跨域请求
- **THEN** 系统不返回 CORS 头，浏览器阻止请求
