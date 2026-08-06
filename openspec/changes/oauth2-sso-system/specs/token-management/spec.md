## ADDED Requirements

### Requirement: JWT Token 签发

认证中心 SHALL 使用 RSA 签名算法签发 JWT Token。

#### Scenario: 签发 access_token
- **WHEN** 客户端成功换取 Token
- **THEN** 系统签发 JWT 格式的 access_token，包含 iss、sub、aud、exp、iat、scope、jti claims

#### Scenario: 签发 id_token
- **WHEN** 客户端请求 openid scope
- **THEN** 系统签发 JWT 格式的 id_token，包含 iss、sub、aud、exp、iat、name、email 等 claims

#### Scenario: RSA 密钥对生成
- **WHEN** 认证中心首次启动
- **THEN** 系统自动生成 RSA 密钥对，私钥用于签名，公钥通过 JWKS 端点发布

### Requirement: Refresh Token 轮转

认证中心 SHALL 支持 Refresh Token 轮转 (Rotation)。

#### Scenario: 使用 refresh_token 刷新
- **WHEN** 客户端使用有效的 refresh_token 请求新 Token
- **THEN** 系统返回新的 access_token 和新的 refresh_token，旧 refresh_token 失效

#### Scenario: 重放旧 refresh_token
- **WHEN** 客户端使用已使用过的 refresh_token 请求新 Token
- **THEN** 系统拒绝请求，撤销该 refresh_token 家族的所有 Token (检测到潜在泄露)

#### Scenario: refresh_token 过期
- **WHEN** 客户端使用过期的 refresh_token 请求新 Token
- **THEN** 系统返回错误，提示客户端重新认证

### Requirement: Token 撤销

认证中心 SHALL 支持 Token 撤销。

#### Scenario: 撤销 access_token
- **WHEN** 客户端调用撤销端点，传入 access_token
- **THEN** 系统将该 Token 的 jti 添加到撤销列表

#### Scenario: 撤销 refresh_token
- **WHEN** 客户端调用撤销端点，传入 refresh_token
- **THEN** 系统将该 Token 及其关联的所有 Token 标记为已撤销

#### Scenario: 验证已撤销的 Token
- **WHEN** 资源服务器验证已撤销的 access_token
- **THEN** 系统返回 Token 无效

### Requirement: Token 过期时间配置

认证中心 SHALL 支持配置 Token 过期时间。

#### Scenario: access_token 过期时间
- **WHEN** 签发 access_token
- **THEN** Token 的 exp claim 设置为签发时间 + 配置的过期时间 (默认 30 分钟)

#### Scenario: refresh_token 过期时间
- **WHEN** 签发 refresh_token
- **THEN** Token 的 exp claim 设置为签发时间 + 配置的过期时间 (默认 7 天)

### Requirement: Token 存储

认证中心 SHALL 使用数据库存储 Token 和授权信息。

#### Scenario: 存储授权信息
- **WHEN** 用户授权客户端
- **THEN** 系统将授权信息存储到 oauth2_authorization 表

#### Scenario: 存储授权同意
- **WHEN** 用户首次授权客户端
- **THEN** 系统将授权同意记录存储到 oauth2_authorization_consent 表
