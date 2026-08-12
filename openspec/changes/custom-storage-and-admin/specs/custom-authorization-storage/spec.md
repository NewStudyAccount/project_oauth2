## ADDED Requirements

### Requirement: 自定义授权记录存储
系统 SHALL 实现 `OAuth2AuthorizationService` 接口，从 `oauth2_authorization` 表读写授权记录，替代框架的 `JdbcOAuth2AuthorizationService`。

#### Scenario: 保存新的授权记录
- **WHEN** 框架调用 `save(authorization)` 且该授权记录不存在
- **THEN** 系统将授权记录插入 `oauth2_authorization` 表

#### Scenario: 更新已有授权记录
- **WHEN** 框架调用 `save(authorization)` 且该授权记录已存在（相同 id）
- **THEN** 系统更新 `oauth2_authorization` 表中对应的记录

#### Scenario: 通过 id 查询授权记录
- **WHEN** 框架调用 `findById("authorization-id")`
- **THEN** 系统从 `oauth2_authorization` 表查询并返回对应的 `OAuth2Authorization` 对象，未找到返回 `null`

#### Scenario: 通过 token 查询授权记录
- **WHEN** 框架调用 `findByToken("token-value", OAuth2TokenType.ACCESS_TOKEN)`
- **THEN** 系统从 `oauth2_authorization` 表查询 `access_token_value` 匹配的记录并返回

#### Scenario: 删除授权记录
- **WHEN** 框架调用 `remove(authorization)`
- **THEN** 系统从 `oauth2_authorization` 表删除对应的记录

### Requirement: 正确序列化/反序列化授权数据
系统 SHALL 正确处理 `oauth2_authorization` 表中序列化字段的读写。

#### Scenario: attributes 字段序列化
- **WHEN** 保存包含 OAuth2AuthorizationAttributes 的授权记录
- **THEN** `attributes` 字段正确序列化为 TEXT 存储

#### Scenario: metadata 字段序列化
- **WHEN** 保存包含 token metadata 的授权记录
- **THEN** 各 `*_metadata` 字段正确序列化为 TEXT 存储
