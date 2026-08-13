## ADDED Requirements

### Requirement: 客户端列表查询
系统 SHALL 提供 `GET /api/admin/clients` 接口，返回所有已注册客户端的列表。每个客户端 SHALL 包含从 `RegisteredClient` 展开的扁平化字段：id、clientId、clientName、clientAuthenticationMethods、authorizationGrantTypes、redirectUris、scopes、requireProofKey、requireAuthorizationConsent、accessTokenTtl、refreshTokenTtl、authorizationCodeTtl、enabled。

#### Scenario: 查询客户端列表
- **WHEN** 管理员发送 `GET /api/admin/clients`
- **THEN** 系统返回 200 和客户端列表 JSON 数组，每个元素包含所有扁平化字段

### Requirement: 客户端详情查询
系统 SHALL 提供 `GET /api/admin/clients/{id}` 接口，返回指定客户端的详情。

#### Scenario: 查询存在的客户端
- **WHEN** 管理员发送 `GET /api/admin/clients/{id}` 且客户端存在
- **THEN** 系统返回 200 和客户端详情 JSON

#### Scenario: 查询不存在的客户端
- **WHEN** 管理员发送 `GET /api/admin/clients/{id}` 且客户端不存在
- **THEN** 系统返回 404

### Requirement: 客户端创建
系统 SHALL 提供 `POST /api/admin/clients` 接口，接收客户端 DTO，通过 `RegisteredClient.builder()` 构建 `RegisteredClient` 对象后调用 `registeredClientRepository.save()` 保存。系统 SHALL 自动生成 id（UUID）和 clientIdIssuedAt。clientSecret SHALL 通过 PasswordEncoder 加密后存储。

#### Scenario: 创建机密客户端
- **WHEN** 管理员发送 `POST /api/admin/clients`，body 包含 clientId、clientSecret、clientName、clientAuthenticationMethods=["client_secret_basic"]、authorizationGrantTypes=["authorization_code","refresh_token"]、redirectUris、scopes、tokenTTL 配置
- **THEN** 系统创建 `RegisteredClient`，加密 clientSecret，保存到 `oauth2_registered_client` 表，返回 201 和创建的客户端详情

#### Scenario: 创建公开客户端
- **WHEN** 管理员发送 `POST /api/admin/clients`，body 中 clientAuthenticationMethods=["none"]，无 clientSecret
- **THEN** 系统创建 `RegisteredClient`，设置 requireProofKey=true，不存储 clientSecret，返回 201

#### Scenario: 创建客户端缺少必填字段
- **WHEN** 管理员发送 `POST /api/admin/clients`，body 缺少 clientId 或 authorizationGrantTypes
- **THEN** 系统返回 400 和错误信息

### Requirement: 客户端更新
系统 SHALL 提供 `PUT /api/admin/clients/{id}` 接口，接收客户端 DTO，基于现有 `RegisteredClient` 重新构建并保存。如果 DTO 中包含新的 clientSecret，SHALL 重新加密存储；如果不包含，SHALL 保留原有 secret。

#### Scenario: 更新客户端信息
- **WHEN** 管理员发送 `PUT /api/admin/clients/{id}`，body 包含更新后的字段
- **THEN** 系统基于原 RegisteredClient 构建新对象并保存，返回 200 和更新后的客户端详情

#### Scenario: 更新客户端密码
- **WHEN** 管理员发送 `PUT /api/admin/clients/{id}`，body 包含新的 clientSecret
- **THEN** 系统加密新 secret 后替换旧值，返回 200

### Requirement: 客户端删除
系统 SHALL 提供 `DELETE /api/admin/clients/{id}` 接口，从 `oauth2_registered_client` 表删除指定客户端。

#### Scenario: 删除存在的客户端
- **WHEN** 管理员发送 `DELETE /api/admin/clients/{id}` 且客户端存在
- **THEN** 系统删除该客户端记录，返回 200

#### Scenario: 删除不存在的客户端
- **WHEN** 管理员发送 `DELETE /api/admin/clients/{id}` 且客户端不存在
- **THEN** 系统返回 404

### Requirement: 客户端启用/禁用
系统 SHALL 支持通过 `client_settings` 中的自定义 key `settings.client.enabled` 控制客户端启用/禁用状态。系统 SHALL 提供 `PUT /api/admin/clients/{id}/status` 接口切换状态。禁用的客户端在 OAuth2 授权流程中 SHALL 被视为不存在。

#### Scenario: 禁用客户端
- **WHEN** 管理员发送 `PUT /api/admin/clients/{id}/status`，body 为 `{"enabled": false}`
- **THEN** 系统将 `settings.client.enabled` 设为 false，返回 200

#### Scenario: 禁用客户端后授权请求
- **WHEN** 用户尝试使用已禁用的 clientId 发起 OAuth2 授权请求
- **THEN** 系统返回错误，提示客户端不存在或已禁用

### Requirement: 用户列表查询
系统 SHALL 提供 `GET /api/admin/users` 接口，返回所有系统用户列表。

#### Scenario: 查询用户列表
- **WHEN** 管理员发送 `GET /api/admin/users`
- **THEN** 系统返回 200 和用户列表 JSON 数组

### Requirement: 用户启用/禁用
系统 SHALL 提供 `PUT /api/admin/users/{id}/status` 接口，切换用户启用/禁用状态。

#### Scenario: 禁用用户
- **WHEN** 管理员发送 `PUT /api/admin/users/{id}/status`，body 为 `{"enabled": false}`
- **THEN** 系统将用户状态设为禁用，返回 200

### Requirement: 权限管理查询
系统 SHALL 提供 `GET /api/admin/access` 接口，查询指定用户对各客户端的访问权限。

#### Scenario: 查询用户权限
- **WHEN** 管理员发送 `GET /api/admin/access?userId=1`
- **THEN** 系统返回 200 和该用户的权限列表

### Requirement: 权限设置
系统 SHALL 提供 `PUT /api/admin/access` 接口，设置用户对客户端的访问权限。

#### Scenario: 设置用户权限
- **WHEN** 管理员发送 `PUT /api/admin/access`，body 为 `{"userId": 1, "clientId": "my-app", "allowed": true}`
- **THEN** 系统创建或更新权限记录，返回 200

### Requirement: 审计日志查询
系统 SHALL 提供 `GET /api/admin/audit-logs` 接口，支持按 action 和 username 筛选审计日志。

#### Scenario: 查询审计日志
- **WHEN** 管理员发送 `GET /api/admin/audit-logs?action=LOGIN&username=admin`
- **THEN** 系统返回 200 和匹配的审计日志列表

### Requirement: 统计概览
系统 SHALL 提供 `GET /api/admin/stats` 接口，返回用户数、客户端数、审计日志数等统计信息。

#### Scenario: 查询统计概览
- **WHEN** 管理员发送 `GET /api/admin/stats`
- **THEN** 系统返回 200 和 `{"userCount": N, "clientCount": N, "auditLogCount": N}`

### Requirement: 管理员权限控制
所有 `/api/admin/**` 接口 SHALL 要求认证用户具有 ADMIN 角色。未认证请求 SHALL 返回 401，非管理员请求 SHALL 返回 403。

#### Scenario: 未认证访问
- **WHEN** 未认证用户发送 `GET /api/admin/clients`
- **THEN** 系统返回 401

#### Scenario: 非管理员访问
- **WHEN** 非 ADMIN 角色用户发送 `GET /api/admin/clients`
- **THEN** 系统返回 403