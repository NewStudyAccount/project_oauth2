## ADDED Requirements

### Requirement: 管理后台首页
系统 SHALL 提供管理员后台首页 `/admin`，展示系统概览信息。

#### Scenario: 管理员访问后台
- **WHEN** 管理员用户访问 `/admin`
- **THEN** 系统展示后台首页，包含用户总数、客户端总数、今日登录次数等统计信息

#### Scenario: 非管理员访问后台
- **WHEN** 非管理员用户访问 `/admin`
- **THEN** 系统返回 403 无权限页面

### Requirement: 用户管理页面
系统 SHALL 提供用户管理页面 `/admin/users`，支持查看和管理用户。

#### Scenario: 查看用户列表
- **WHEN** 管理员访问 `/admin/users`
- **THEN** 系统展示用户列表，包含用户名、昵称、邮箱、状态、创建时间

#### Scenario: 启用/禁用用户
- **WHEN** 管理员点击用户的启用/禁用按钮
- **THEN** 系统更新用户状态，禁用后该用户无法登录

### Requirement: 客户端管理页面
系统 SHALL 提供客户端管理页面 `/admin/clients`，支持查看和管理 OAuth2 客户端。

#### Scenario: 查看客户端列表
- **WHEN** 管理员访问 `/admin/clients`
- **THEN** 系统展示客户端列表，包含 client_id、名称、类型、scopes、状态

#### Scenario: 新增客户端
- **WHEN** 管理员填写客户端信息并提交
- **THEN** 系统在 `oauth2_client` 表创建新记录

#### Scenario: 编辑客户端
- **WHEN** 管理员修改客户端信息并提交
- **THEN** 系统更新 `oauth2_client` 表中对应的记录

### Requirement: 用户权限管理页面
系统 SHALL 提供用户权限管理页面 `/admin/access`，支持管理用户对客户端的访问权限。

#### Scenario: 查看用户权限
- **WHEN** 管理员选择一个用户
- **THEN** 系统展示该用户对所有客户端的访问权限列表

#### Scenario: 修改用户权限
- **WHEN** 管理员勾选/取消用户对某客户端的访问权限
- **THEN** 系统更新 `user_client_access` 表

### Requirement: 审计日志查询页面
系统 SHALL 提供审计日志查询页面 `/admin/audit-logs`，支持查看系统操作日志。

#### Scenario: 查看审计日志
- **WHEN** 管理员访问 `/admin/audit-logs`
- **THEN** 系统展示审计日志列表，包含时间、用户、操作类型、详情、IP、状态

#### Scenario: 按条件筛选日志
- **WHEN** 管理员选择筛选条件（用户、操作类型、时间范围）
- **THEN** 系统展示符合条件的日志记录

### Requirement: Token 管理页面
系统 SHALL 提供 Token 管理页面 `/admin/tokens`，支持查看和撤销 Token。

#### Scenario: 查看活跃 Token
- **WHEN** 管理员访问 `/admin/tokens`
- **THEN** 系统展示当前活跃的授权记录列表

#### Scenario: 强制撤销用户 Token
- **WHEN** 管理员点击某用户的"撤销 Token"按钮
- **THEN** 系统将该用户的所有 Token 加入黑名单，用户被踢下线

### Requirement: 管理后台公共布局
系统 SHALL 提供统一的管理后台布局，包含导航栏和侧边栏。

#### Scenario: 后台页面统一布局
- **WHEN** 管理员访问任意后台页面
- **THEN** 页面展示统一的顶部导航栏和左侧菜单（用户管理、客户端管理、权限管理、审计日志、Token 管理）
