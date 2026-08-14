## MODIFIED Requirements

### Requirement: ClientDTO includes all oauth2_registered_client fields
ClientDTO SHALL include `clientSecretExpiresAt`、`reuseRefreshTokens`、`idTokenSignatureAlgorithm`、`accessTokenFormat` fields.

#### Scenario: clientSecretExpiresAt field in DTO
- **WHEN** 查询客户端详情
- **THEN** 响应包含 `clientSecretExpiresAt` 字段，值为 Instant 或 null

#### Scenario: reuseRefreshTokens field in DTO
- **WHEN** 查询客户端详情
- **THEN** 响应包含 `reuseRefreshTokens` 字段，值为 boolean

#### Scenario: idTokenSignatureAlgorithm field in DTO
- **WHEN** 查询客户端详情
- **THEN** 响应包含 `idTokenSignatureAlgorithm` 字段，值为字符串（如 "RS256"）

#### Scenario: accessTokenFormat field in DTO
- **WHEN** 查询客户端详情
- **THEN** 响应包含 `accessTokenFormat` 字段，值为 "self-contained" 或 "reference"

### Requirement: ClientConverter handles new fields
ClientConverter 的 toDTO SHALL 从 TokenSettings/RegisteredClient 中提取新字段值，toEntity 和 toEntityForUpdate SHALL 将新字段值写入 TokenSettings。

#### Scenario: toDTO extracts reuseRefreshTokens
- **WHEN** 调用 `ClientConverter.toDTO(client)`
- **THEN** dto.reuseRefreshTokens 等于 client.getTokenSettings().isReuseRefreshTokens()

#### Scenario: toEntity writes reuseRefreshTokens
- **WHEN** 调用 `ClientConverter.toEntity(dto, encoder)` 且 dto.reuseRefreshTokens = false
- **THEN** 生成的 RegisteredClient 的 tokenSettings 中 reuseRefreshTokens 为 false

#### Scenario: toDTO extracts idTokenSignatureAlgorithm
- **WHEN** 调用 `ClientConverter.toDTO(client)`
- **THEN** dto.idTokenSignatureAlgorithm 等于 client.getTokenSettings().getIdTokenSignatureAlgorithm().getValue()

#### Scenario: toEntity writes idTokenSignatureAlgorithm
- **WHEN** 调用 `ClientConverter.toEntity(dto, encoder)` 且 dto.idTokenSignatureAlgorithm = "ES256"
- **THEN** 生成的 RegisteredClient 的 tokenSettings 中 idTokenSignatureAlgorithm 为 ES256

#### Scenario: toDTO extracts accessTokenFormat
- **WHEN** 调用 `ClientConverter.toDTO(client)`
- **THEN** dto.accessTokenFormat 等于 client.getTokenSettings().getAccessTokenFormat().getValue()

#### Scenario: toDTO extracts clientSecretExpiresAt
- **WHEN** 调用 `ClientConverter.toDTO(client)`
- **THEN** dto.clientSecretExpiresAt 等于 client.getClientSecretExpiresAt()

### Requirement: Client form includes all fields with descriptions
ClientFormView.vue SHALL 为每个表单字段添加说明文字，采用表单下方小字提示方式。

#### Scenario: All form items have description text
- **WHEN** 渲染客户端编辑表单
- **THEN** 每个 el-form-item 下方存在描述文字，解释该字段的含义和作用

#### Scenario: New field clientSecretExpiresAt in form
- **WHEN** 渲染客户端编辑表单
- **THEN** 存在"密钥过期时间"表单项，类型为 el-date-picker，说明文字解释其用途

#### Scenario: New field reuseRefreshTokens in form
- **WHEN** 渲染客户端编辑表单
- **THEN** 存在"复用刷新令牌"表单项，类型为 el-switch，说明文字解释 Rotation 安全机制

#### Scenario: New field idTokenSignatureAlgorithm in form
- **WHEN** 渲染客户端编辑表单
- **THEN** 存在"ID Token 签名算法"表单项，类型为 el-select，选项包含 RS256/RS384/RS512/ES256/ES384/ES512/HS256/HS384/HS512

#### Scenario: New field accessTokenFormat in form
- **WHEN** 渲染客户端编辑表单
- **THEN** 存在"访问令牌格式"表单项，类型为 el-select，选项为 "JWT (self-contained)" 和 "Opaque (reference)"

### Requirement: Client list shows key configuration columns
ClientListView.vue SHALL 增加显示列：Scopes、Access Token 有效期、PKCE、授权确认。

#### Scenario: Scopes column in list
- **WHEN** 渲染客户端列表
- **THEN** 存在 Scopes 列，使用 el-tag 展示各 scope

#### Scenario: Access Token TTL column in list
- **WHEN** 渲染客户端列表
- **THEN** 存在 Token 有效期列，显示可读格式（如 "30min"、"7d"）

#### Scenario: PKCE column in list
- **WHEN** 渲染客户端列表
- **THEN** 存在 PKCE 列，使用图标或标签显示是否启用

#### Scenario: Authorization consent column in list
- **WHEN** 渲染客户端列表
- **THEN** 存在授权确认列，使用图标或标签显示是否需要