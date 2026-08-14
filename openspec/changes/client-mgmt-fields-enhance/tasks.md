## 1. 后端 ClientDTO 新增字段

- [x] 1.1 在 `ClientDTO.java` 中添加 `clientSecretExpiresAt`（Instant）字段
- [x] 1.2 在 `ClientDTO.java` 中添加 `reuseRefreshTokens`（boolean，默认 true）字段
- [x] 1.3 在 `ClientDTO.java` 中添加 `idTokenSignatureAlgorithm`（String，默认 "RS256"）字段
- [x] 1.4 在 `ClientDTO.java` 中添加 `accessTokenFormat`（String，默认 "self-contained"）字段

## 2. 后端 ClientConverter 补充转换逻辑

- [x] 2.1 `toDTO` 方法：从 `RegisteredClient.getClientSecretExpiresAt()` 提取 `clientSecretExpiresAt`
- [x] 2.2 `toDTO` 方法：从 `TokenSettings.isReuseRefreshTokens()` 提取 `reuseRefreshTokens`
- [x] 2.3 `toDTO` 方法：从 `TokenSettings.getIdTokenSignatureAlgorithm()` 提取 `idTokenSignatureAlgorithm`
- [x] 2.4 `toDTO` 方法：从 `TokenSettings.getAccessTokenFormat()` 提取 `accessTokenFormat`
- [x] 2.5 `toEntity` 方法：将 `reuseRefreshTokens` 写入 `TokenSettings.builder().reuseRefreshTokens()`
- [x] 2.6 `toEntity` 方法：将 `idTokenSignatureAlgorithm` 写入 `TokenSettings.builder().idTokenSignatureAlgorithm()`
- [x] 2.7 `toEntity` 方法：将 `accessTokenFormat` 写入 `TokenSettings.builder().accessTokenFormat()`
- [x] 2.8 `toEntity` 方法：将 `clientSecretExpiresAt` 写入 `RegisteredClient.builder().clientSecretExpiresAt()`
- [x] 2.9 `toEntityForUpdate` 方法：合并新字段到 merged DTO，确保更新时保留或覆盖

## 3. 前端表单新增字段和说明文字

- [x] 3.1 在 `ClientFormView.vue` 中添加"密钥过期时间"表单项（el-date-picker），含说明文字
- [x] 3.2 在 `ClientFormView.vue` 中添加"复用刷新令牌"表单项（el-switch），含说明文字
- [x] 3.3 在 `ClientFormView.vue` 中添加"ID Token 签名算法"表单项（el-select），含说明文字
- [x] 3.4 在 `ClientFormView.vue` 中添加"访问令牌格式"表单项（el-select），含说明文字
- [x] 3.5 为所有已有表单字段添加说明文字（Client ID、Client Secret、客户端名称、认证方式、授权类型、重定向 URI、Scopes、Access Token 有效期、Refresh Token 有效期、Auth Code 有效期、需要 PKCE、需要授权确认、启用）
- [x] 3.6 添加 `.form-tip` CSS 样式（灰色小字，font-size: 12px, color: #909399, margin-top: 4px）
- [x] 3.7 更新 form ref 初始值，包含新字段默认值

## 4. 前端列表增加显示列

- [x] 4.1 在 `ClientListView.vue` 的 el-table 中添加 Scopes 列（el-tag 展示）
- [x] 4.2 添加 Access Token 有效期列（可读格式：秒数转为 "Xmin" / "Xh" / "Xd"）
- [x] 4.3 添加 PKCE 列（el-tag 显示 是/否）
- [x] 4.4 添加授权确认列（el-tag 显示 是/否）

## 5. 验证

- [ ] 5.1 启动 auth-server，通过 API 验证新增字段在 GET /api/admin/clients 响应中返回
- [ ] 5.2 通过前端创建新客户端，验证新字段可保存和回显
- [ ] 5.3 编辑已有客户端，验证新字段正确显示当前值且可修改