## Why

认证中心客户端管理界面缺少 4 个重要字段的配置能力：`clientSecretExpiresAt`（密钥过期时间）、`reuseRefreshTokens`（复用刷新令牌）、`idTokenSignatureAlgorithm`（ID Token 签名算法）、`accessTokenFormat`（访问令牌格式）。这些字段在数据库 `oauth2_registered_client` 表的 `token_settings` JSON 中有值，但后端 DTO 和前端表单均未暴露，管理员无法通过界面修改，只能手动改数据库。此外，前端表单各字段缺少说明文字，OAuth2 术语对非专业管理员不直观；列表页显示列过少，无法快速了解客户端关键配置。

## What Changes

- 后端 `ClientDTO` 新增 4 个字段：`clientSecretExpiresAt`、`reuseRefreshTokens`、`idTokenSignatureAlgorithm`、`accessTokenFormat`
- 后端 `ClientConverter` 的 `toDTO`/`toEntity`/`toEntityForUpdate` 方法补充新字段的转换逻辑
- 前端 `ClientFormView.vue` 新增 4 个表单项，并为所有字段添加说明文字（方式1：表单下方小字提示）
- 前端 `ClientListView.vue` 增加列表显示列（Scopes、Access Token 有效期、PKCE、授权确认）

## Capabilities

### Modified Capabilities
- `client-management`: 客户端管理 CRUD 补充缺失字段，增强表单说明和列表显示

## Impact

- **后端 API 兼容性**: 新增字段为可选，已有客户端数据不受影响，API 向后兼容
- **前端**: 表单变长，需滚动查看；列表变宽，可能需要横向滚动
- **数据库**: 无 schema 变更，新字段值已存在于 `token_settings` JSON 中