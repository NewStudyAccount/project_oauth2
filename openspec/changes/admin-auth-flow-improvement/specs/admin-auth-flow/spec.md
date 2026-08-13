## Purpose

为 admin-vue3 管理后台建立完整的认证流程：API 请求未认证时返回 401、前端路由守卫拦截未登录访问、页面展示明确错误提示。

## ADDED Requirements

### Requirement: API 请求未认证返回 401
当未认证用户发起 `/api/**` 请求时，后端 SHALL 返回 HTTP 401 状态码和 JSON 响应体，而非 302 重定向。

#### Scenario: 未登录用户请求 API
- **WHEN** 用户未携带有效 Session 向 `/api/admin/stats` 发起 GET 请求
- **THEN** 响应状态码为 401，响应体为 `{"error":"unauthorized","message":"请先登录"}`

#### Scenario: 已登录用户请求 API
- **WHEN** 用户已认证并携带有效 Session 向 `/api/admin/stats` 发起 GET 请求
- **THEN** 响应正常返回 200 和数据（与当前行为一致）

#### Scenario: 非 API 请求未认证
- **WHEN** 用户未认证访问 `/clients` 等页面路由
- **THEN** 仍然重定向到 `/login` 页面（保持现有行为）

### Requirement: 前端路由守卫拦截未登录访问
Vue Router SHALL 在导航前检查用户登录状态，未登录时自动重定向到登录页。

#### Scenario: 未登录用户访问管理页面
- **WHEN** 用户未登录直接访问 `/clients` 等管理路由
- **THEN** 路由守卫检测到未登录，重定向到 `/login`

#### Scenario: 已登录用户访问管理页面
- **WHEN** 用户已登录访问任何管理路由
- **THEN** 正常加载页面，不触发重定向

#### Scenario: 登录状态检查失败
- **WHEN** 登录状态检查 API 返回 401
- **THEN** 将用户标记为未登录，下次导航时重定向到登录页

### Requirement: 页面显示错误提示
当 API 请求失败时，页面 SHALL 显示用户可见的错误提示，而非静默失败。

#### Scenario: 加载客户端列表失败
- **WHEN** `/api/admin/clients` 请求返回非 200 响应
- **THEN** 页面显示错误提示消息（如"加载失败，请重试"）

#### Scenario: 加载用户列表失败
- **WHEN** `/api/admin/users` 请求返回非 200 响应
- **THEN** 页面显示错误提示消息

#### Scenario: 加载审计日志失败
- **WHEN** `/api/admin/audit-logs` 请求返回非 200 响应
- **THEN** 页面显示错误提示消息

#### Scenario: 加载统计数据失败
- **WHEN** `/api/admin/stats` 请求返回非 200 响应
- **THEN** 页面显示错误提示消息

### Requirement: axios 拦截器统一处理认证错误
axios 响应拦截器 SHALL 统一处理 401 响应，自动重定向到登录页。

#### Scenario: API 返回 401
- **WHEN** 任意 API 请求返回 401 状态码
- **THEN** 拦截器将页面重定向到 `/login`

#### Scenario: 响应为非 JSON 格式
- **WHEN** API 返回非 JSON 响应（如 HTML 登录页）
- **THEN** 拦截器识别并处理，不抛出 JSON 解析错误
