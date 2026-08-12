## ADDED Requirements

### Requirement: 从数据库加载客户端配置
系统 SHALL 实现 `RegisteredClientRepository` 接口，从 `oauth2_client` 表加载客户端配置，替代 `application.yml` 静态配置。

#### Scenario: 通过 client_id 查询客户端
- **WHEN** 框架调用 `findByClientId("vue-app")`
- **THEN** 系统从 `oauth2_client` 表查询 `client_id = 'vue-app'` 的记录，转换为 `RegisteredClient` 对象返回

#### Scenario: 公开客户端映射
- **WHEN** `oauth2_client` 记录的 `client_type = 'PUBLIC'`
- **THEN** 转换后的 `RegisteredClient` 的 `clientAuthenticationMethod` 为 `NONE`

#### Scenario: 机密客户端映射
- **WHEN** `oauth2_client` 记录的 `client_type = 'CONFIDENTIAL'`
- **THEN** 转换后的 `RegisteredClient` 的 `clientAuthenticationMethod` 为 `CLIENT_SECRET_BASIC`

#### Scenario: scopes 逗号分隔转换
- **WHEN** `oauth2_client` 记录的 `scopes = 'openid,profile,email'`
- **THEN** 转换后的 `RegisteredClient` 的 scopes 为 `{"openid", "profile", "email"}`

#### Scenario: grant_types 逗号分隔转换
- **WHEN** `oauth2_client` 记录的 `grant_types = 'authorization_code,refresh_token'`
- **THEN** 转换后的 `RegisteredClient` 的 grantTypes 包含 `AUTHORIZATION_CODE` 和 `REFRESH_TOKEN`

#### Scenario: redirect_uris 逗号分隔转换
- **WHEN** `oauth2_client` 记录的 `redirect_uris = 'http://a.com/callback,http://b.com/callback'`
- **THEN** 转换后的 `RegisteredClient` 的 redirectUris 为 `{"http://a.com/callback", "http://b.com/callback"}`

#### Scenario: Token 有效期映射
- **WHEN** `oauth2_client` 记录的 `access_token_ttl = 1800`, `refresh_token_ttl = 604800`
- **THEN** 转换后的 `RegisteredClient` 的 TokenSettings 中 accessTokenTimeToLive 为 30 分钟，refreshTokenTimeToLive 为 7 天

#### Scenario: 客户端不存在
- **WHEN** 框架调用 `findByClientId("nonexistent")`
- **THEN** 系统返回 `null`

### Requirement: 移除 yml 静态客户端配置
系统 SHALL 不再使用 `application.yml` 中的 `spring.security.oauth2.authorizationserver.client` 配置。

#### Scenario: 配置已移除
- **WHEN** 服务启动
- **THEN** 系统不从 yml 加载任何客户端配置，所有客户端从数据库加载
