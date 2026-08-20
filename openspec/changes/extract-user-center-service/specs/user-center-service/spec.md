## Purpose

用户中心服务提供统一的用户信息查询、用户注册和用户管理接口，供 auth-center 和 client-app 等内部服务调用。

## ADDED Requirements

### Requirement: 用户信息查询

user-service SHALL 提供根据用户名查询用户信息的 REST API，返回用户基本信息（不含密码哈希）。

#### Scenario: 根据存在的用户名查询
- **WHEN** 请求 `GET /api/users/{username}` 且 username 存在
- **THEN** 返回 200 和包含 id, username, nickname, email, phone, status 的 JSON

#### Scenario: 根据不存在的用户名查询
- **WHEN** 请求 `GET /api/users/{username}` 且 username 不存在
- **THEN** 返回 404

#### Scenario: 根据 ID 查询用户
- **WHEN** 请求 `GET /api/users/id/{id}` 且 ID 存在
- **THEN** 返回 200 和用户信息 JSON

#### Scenario: 根据 ID 查询不存在的用户
- **WHEN** 请求 `GET /api/users/id/{id}` 且 ID 不存在
- **THEN** 返回 404

### Requirement: 完整用户信息查询（含密码哈希）

user-service SHALL 提供返回完整用户信息（含密码哈希）的接口，仅供 auth-center 认证使用。

#### Scenario: 查询存在的用户的完整信息
- **WHEN** 请求 `GET /api/users/{username}/full` 且 username 存在
- **THEN** 返回 200 和包含 id, username, password, nickname, email, phone, status, createdAt, updatedAt 的完整 JSON

#### Scenario: 查询不存在的用户的完整信息
- **WHEN** 请求 `GET /api/users/{username}/full` 且 username 不存在
- **THEN** 返回 404

### Requirement: 用户注册

user-service SHALL 提供用户注册接口，创建新用户并返回创建结果。

#### Scenario: 正常注册
- **WHEN** 请求 `POST /api/users` 且 body 包含合法的 username, password, email, nickname
- **THEN** 返回 200 和创建的用户信息（不含密码），密码使用 bcrypt 加密存储

#### Scenario: 用户名已存在
- **WHEN** 请求 `POST /api/users` 且 username 已存在
- **THEN** 返回 409 Conflict 和错误信息

#### Scenario: 邮箱已存在
- **WHEN** 请求 `POST /api/users` 且 email 已存在
- **THEN** 返回 409 Conflict 和错误信息

#### Scenario: 参数不合法
- **WHEN** 请求 `POST /api/users` 且缺少必填字段或格式不合法
- **THEN** 返回 400 Bad Request 和错误信息

### Requirement: 用户列表查询

user-service SHALL 提供用户列表查询接口，供管理员使用。

#### Scenario: 查询所有用户
- **WHEN** 请求 `GET /api/users`
- **THEN** 返回 200 和用户列表 JSON（不含密码字段）

### Requirement: 用户状态管理

user-service SHALL 提供启用/禁用用户的接口。

#### Scenario: 启用用户
- **WHEN** 请求 `PUT /api/users/{id}/status` 且 body 为 `{"enabled": true}`
- **THEN** 返回 200，用户 status 更新为 1

#### Scenario: 禁用用户
- **WHEN** 请求 `PUT /api/users/{id}/status` 且 body 为 `{"enabled": false}`
- **THEN** 返回 200，用户 status 更新为 0

#### Scenario: 用户不存在
- **WHEN** 请求 `PUT /api/users/{id}/status` 且 ID 不存在
- **THEN** 返回 404

### Requirement: 独立数据库

user-service SHALL 使用独立的 `user_center` 数据库，与 auth-center 的 `oauth2_center` 数据库分离。

#### Scenario: 数据库隔离
- **WHEN** user-service 启动
- **THEN** 连接 `user_center` 数据库，不依赖 `oauth2_center` 数据库

### Requirement: Nacos 服务注册

user-service SHALL 注册到 Nacos 服务发现中心。

#### Scenario: 服务注册
- **WHEN** user-service 启动
- **THEN** 以服务名 `user-service` 注册到 Nacos

## MODIFIED Requirements

### Requirement: auth-center 用户认证

auth-center 的 `CustomUserDetailsService` SHALL 通过 Feign 调用 user-service 获取用户信息，不再直接访问 `sys_user` 表。

#### Scenario: 认证加载
- **WHEN** 用户登录时 Spring Security 调用 `loadUserByUsername()`
- **THEN** 通过 `UserServiceClient.getUserByUsernameFull()` 从 user-service 获取含密码哈希的用户信息，构建 `UserDetails`

#### Scenario: user-service 不可用
- **WHEN** user-service 不可达
- **THEN** 认证失败，返回 `UsernameNotFoundException`

### Requirement: auth-center 用户注册

auth-center 的 `RegisterService` SHALL 通过 Feign 调用 user-service 的注册接口创建用户。

#### Scenario: 正常注册流程
- **WHEN** 用户提交注册表单
- **THEN** auth-center 校验验证码后调用 `UserServiceClient.createUser()` 创建用户

### Requirement: client-app 用户信息获取

client-app 的 `UserInfoApiController` SHALL 通过 Feign 调用 user-service 获取完整用户资料，合并 JWT claims 和 user-service 返回的数据。

#### Scenario: 获取用户信息
- **WHEN** 请求 `GET /api/userinfo` 且持有有效 JWT
- **THEN** 从 JWT 提取 username，调用 `UserServiceClient.getUserByUsername()` 获取完整资料返回
