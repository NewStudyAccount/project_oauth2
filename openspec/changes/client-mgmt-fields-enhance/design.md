## Context

认证中心客户端管理基于 Spring Authorization Server 的 `JdbcRegisteredClientRepository`，客户端配置存储在 `oauth2_registered_client` 表中。核心配置通过 `client_settings` 和 `token_settings` 两个 JSON 字段管理。

当前后端 `ClientDTO` 暴露了大部分字段，但遗漏了 4 个存在于 `token_settings` 中的配置项。前端表单 `ClientFormView.vue` 使用 Element Plus 组件，列表页 `ClientListView.vue` 使用 `el-table`。

## Goals / Non-Goals

**Goals:**
- 补齐 4 个缺失字段的完整管理能力（后端 DTO + 转换 + 前端表单）
- 为所有表单字段添加说明文字，降低 OAuth2 术语理解门槛
- 增强列表页显示列，方便管理员快速了解客户端配置

**Non-Goals:**
- 不修改数据库 schema（字段已存在于 JSON 中）
- 不修改 API 路由或接口路径
- 不做字段分组/折叠（表单虽长但结构清晰）
- 不做国际化（当前项目仅中文）

## Decisions

### Decision 1: 表单说明采用方式1（表单下方小字提示）

**选择**: 在每个 `el-form-item` 下方添加 `<div class="form-tip">` 小字说明

**替代方案**: 方式2 el-tooltip 悬浮提示

**理由**: OAuth2 术语对非专业管理员不直观，始终可见的说明比悬浮提示更友好，减少操作步骤。提示文字使用灰色小字，不干扰表单视觉。

### Decision 2: 新增字段默认值与现有数据兼容

| 字段 | 默认值 | 理由 |
|------|--------|------|
| `clientSecretExpiresAt` | null（永不过期） | 当前所有客户端均无过期时间 |
| `reuseRefreshTokens` | true | 与 data.sql 中现有数据一致 |
| `idTokenSignatureAlgorithm` | RS256 | 与 data.sql 中现有数据一致 |
| `accessTokenFormat` | self-contained | 与 data.sql 中现有数据一致 |

### Decision 3: idTokenSignatureAlgorithm 可选值范围

提供常用签名算法：RS256、RS384、RS512、ES256、ES384、ES512、HS256、HS384、HS512。前端使用 `el-select` 下拉选择。

### Decision 4: accessTokenFormat 可选值范围

提供两种格式：`self-contained`（JWT）和 `reference`（Opaque）。前端使用 `el-select` 下拉选择，显示中文标签。

### Decision 5: 列表页增加列但不增加操作列宽度

新增 Scopes、Token 有效期、PKCE、授权确认 4 列。Scopes 使用 el-tag 展示，Token 有效期显示秒数（可读格式如 "30min"），PKCE 和授权确认使用 el-icon 图标。

## Risks / Trade-offs

- **[表单过长]** → 字段多但分组清晰（基础信息/认证授权/重定向作用域/客户端设置/令牌设置），可接受
- **[reuseRefreshTokens 关闭后旧 refresh_token 失效]** → 这是 OAuth2 标准行为，在说明文字中提示
- **[accessTokenFormat 改为 reference 需要资源服务器支持 introspection]** → 在说明文字中提示此要求

## Open Questions

无