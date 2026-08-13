## ADDED Requirements

### Requirement: 管理后台前端项目结构
系统 SHALL 在项目根目录创建 `admin-vue3` 前端项目，使用 Vite + Vue3 + Element Plus + Vue Router + Axios。开发时通过 vite proxy 将 API 请求转发到 auth-server:9000。

#### Scenario: 项目初始化
- **WHEN** 开发者进入 admin-vue3 目录并执行 `npm run dev`
- **THEN** Vite 开发服务器启动在指定端口，API 请求通过 proxy 转发到 auth-server

### Requirement: 管理后台登录
管理后台 SHALL 检测用户登录状态。未登录时 SHALL 跳转到 auth-server 的登录页面（`/login`）。登录成功后 SHALL 自动返回管理后台。

#### Scenario: 未登录访问
- **WHEN** 用户访问管理后台任意页面且未登录
- **THEN** 系统跳转到 `auth.local:9000/login`，登录成功后返回管理后台

#### Scenario: 已登录访问
- **WHEN** 用户已登录并访问管理后台
- **THEN** 系统直接显示管理后台页面

### Requirement: 管理后台布局
管理后台 SHALL 使用 Element Plus 的 Container 布局，包含顶部导航栏和左侧菜单。菜单项 SHALL 包括：系统概览、客户端管理、用户管理、权限管理、审计日志。

#### Scenario: 布局展示
- **WHEN** 管理员登录后进入管理后台
- **THEN** 系统显示顶部导航栏（含用户名和登出按钮）和左侧菜单

### Requirement: 系统概览页面
管理后台 SHALL 提供系统概览页面，展示用户数、客户端数、审计日志数等统计卡片。

#### Scenario: 查看概览
- **WHEN** 管理员点击"系统概览"菜单
- **THEN** 系统调用 `GET /api/admin/stats` 并展示统计卡片

### Requirement: 客户端管理列表页
管理后台 SHALL 提供客户端列表页面，展示所有已注册客户端的表格，包含列：Client ID、名称、认证方式、授权类型、Scopes、状态、操作（编辑/删除/启用禁用）。SHALL 支持新增客户端按钮。

#### Scenario: 查看客户端列表
- **WHEN** 管理员点击"客户端管理"菜单
- **THEN** 系统调用 `GET /api/admin/clients` 并展示客户端表格

### Requirement: 客户端新增/编辑表单
管理后台 SHALL 提供客户端新增和编辑表单，包含以下字段：
- Client ID（新增时可编辑，编辑时只读）
- Client Secret（新增时必填/可选，编辑时为空表示不修改）
- 客户端名称
- 认证方式（多选：client_secret_basic / client_secret_post / none）
- 授权类型（多选：authorization_code / refresh_token / client_credentials）
- 重定向 URI（动态添加多行）
- Scopes（动态添加多行）
- Access Token 有效期（秒）
- Refresh Token 有效期（秒）
- Authorization Code 有效期（秒）
- 是否需要 PKCE（开关）
- 是否需要授权确认（开关）
- 状态（启用/禁用）

#### Scenario: 新增客户端
- **WHEN** 管理员点击"新增客户端"按钮并填写表单后提交
- **THEN** 系统调用 `POST /api/admin/clients`，成功后返回列表页并提示成功

#### Scenario: 编辑客户端
- **WHEN** 管理员点击客户端的"编辑"按钮，修改字段后提交
- **THEN** 系统调用 `PUT /api/admin/clients/{id}`，成功后返回列表页并提示成功

#### Scenario: 表单校验
- **WHEN** 管理员提交表单但缺少必填字段
- **THEN** 系统在表单中显示校验错误提示，不发送请求

### Requirement: 客户端删除确认
管理后台 SHALL 在删除客户端前弹出确认对话框。

#### Scenario: 删除客户端
- **WHEN** 管理员点击"删除"按钮
- **THEN** 系统弹出确认对话框，确认后调用 `DELETE /api/admin/clients/{id}`

### Requirement: 客户端启用/禁用切换
管理后台 SHALL 在客户端列表中提供启用/禁用开关，切换时调用 `PUT /api/admin/clients/{id}/status`。

#### Scenario: 禁用客户端
- **WHEN** 管理员将客户端的启用开关设为关闭
- **THEN** 系统调用 `PUT /api/admin/clients/{id}/status`，body 为 `{"enabled": false}`

### Requirement: 用户管理页面
管理后台 SHALL 提供用户列表页面，展示所有系统用户的表格，包含列：ID、用户名、昵称、邮箱、状态、操作（启用/禁用）。

#### Scenario: 查看用户列表
- **WHEN** 管理员点击"用户管理"菜单
- **THEN** 系统调用 `GET /api/admin/users` 并展示用户表格

### Requirement: 权限管理页面
管理后台 SHALL 提供权限管理页面，左侧选择用户，右侧展示该用户对各客户端的访问权限，支持切换允许/拒绝。

#### Scenario: 查看用户权限
- **WHEN** 管理员选择一个用户
- **THEN** 系统调用 `GET /api/admin/access?userId={id}` 并展示权限列表

#### Scenario: 修改权限
- **WHEN** 管理员切换某个客户端的权限
- **THEN** 系统调用 `PUT /api/admin/access` 更新权限

### Requirement: 审计日志页面
管理后台 SHALL 提供审计日志页面，展示审计日志表格，支持按操作类型和用户名筛选。

#### Scenario: 查看审计日志
- **WHEN** 管理员点击"审计日志"菜单
- **THEN** 系统调用 `GET /api/admin/audit-logs` 并展示日志表格

#### Scenario: 筛选审计日志
- **WHEN** 管理员选择操作类型或输入用户名
- **THEN** 系统带筛选参数重新请求并展示结果