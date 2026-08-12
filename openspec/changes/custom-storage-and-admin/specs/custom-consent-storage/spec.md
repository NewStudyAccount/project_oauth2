## ADDED Requirements

### Requirement: 自定义授权确认存储
系统 SHALL 实现 `OAuth2AuthorizationConsentService` 接口，从 `oauth2_authorization_consent` 表读写授权确认记录，替代框架的 `JdbcOAuth2AuthorizationConsentService`。

#### Scenario: 保存授权确认记录
- **WHEN** 框架调用 `save(consent)` 且该记录不存在
- **THEN** 系统将授权确认记录插入 `oauth2_authorization_consent` 表

#### Scenario: 更新已有授权确认记录
- **WHEN** 框架调用 `save(consent)` 且该记录已存在（相同 registeredClientId + principalName）
- **THEN** 系统更新 `oauth2_authorization_consent` 表中对应的记录

#### Scenario: 查询授权确认记录
- **WHEN** 框架调用 `findById("client-id", "username")`
- **THEN** 系统从 `oauth2_authorization_consent` 表查询并返回对应的 `OAuth2AuthorizationConsent` 对象，未找到返回 `null`

#### Scenario: 删除授权确认记录
- **WHEN** 框架调用 `remove(consent)`
- **THEN** 系统从 `oauth2_authorization_consent` 表删除对应的记录
