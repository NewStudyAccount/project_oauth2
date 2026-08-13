## Why

admin-vue3 管理页面在未登录或认证失败时显示空表格，没有任何错误提示。根本原因：

1. **后端返回 302 而非 401** — Spring Security 对未认证的 `/api/**` 请求返回 302 重定向到 `/login`，Vite 代理跟随重定向返回登录页 HTML（200），前端无法区分正常数据和错误响应
2. **前端无路由守卫** — Vue Router 没有检查登录状态，未登录用户也能加载管理页面
3. **前端静默吞掉错误** — 所有 API 调用的 catch 块只打 console.error，页面上不显示任何错误信息

## What Changes

- **后端 SecurityConfig**：为 `/api/**` 请求添加自定义 `AuthenticationEntryPoint`，未认证时返回 `401 JSON`（`{"error":"unauthorized","message":"请先登录"}`），而非 302 重定向
- **前端 axios 拦截器**：增强响应拦截器，统一处理 401 响应并重定向到登录页，处理非 JSON 响应（防止 HTML 响应导致解析错误）
- **前端路由守卫**：在 Vue Router 中添加 `beforeEach` 导航守卫，通过调用轻量级 API 检查登录状态，未登录时自动重定向到登录页
- **前端错误提示**：各页面的 `load()` 函数增加 `ElMessage.error` 错误提示，替代静默失败

## Capabilities

### New Capabilities
- `admin-auth-flow`: 管理后台认证流程 — API 请求返回 401、前端路由守卫、登录状态检查

### Modified Capabilities

## Impact

- `auth-server/src/main/java/.../config/SecurityConfig.java` — 添加自定义 AuthenticationEntryPoint
- `admin-vue3/src/api/index.js` — 增强 axios 拦截器
- `admin-vue3/src/router/index.js` — 添加路由守卫
- `admin-vue3/src/views/ClientListView.vue` — 错误提示
- `admin-vue3/src/views/UserListView.vue` — 错误提示
- `admin-vue3/src/views/AuditLogView.vue` — 错误提示
- `admin-vue3/src/views/AccessView.vue` — 错误提示
- `admin-vue3/src/views/DashboardView.vue` — 错误提示
