## 1. 后端 — API 认证入口点

- [x] 1.1 在 `SecurityConfig` 中创建 `DelegatingAuthenticationEntryPoint` 内部类，`/api/**` 返回 401 JSON，其他请求重定向到 `/login`
- [x] 1.2 在 `defaultFilterChain` 中通过 `http.exceptionHandling().authenticationEntryPoint(...)` 注入自定义入口点
- [x] 1.3 验证 `formLogin` 的 `loginProcessingUrl("/login")` POST 登录流程不受影响

## 2. 前端 — axios 拦截器增强

- [x] 2.1 在 `admin-vue3/src/api/index.js` 中增强响应拦截器，处理 401 响应并重定向到 `/login`
- [x] 2.2 添加非 JSON 响应检测（Content-Type 为 text/html 时视为认证失败），重定向到 `/login`

## 3. 前端 — 路由守卫

- [x] 3.1 在 `admin-vue3/src/router/index.js` 中添加 `beforeEach` 导航守卫
- [x] 3.2 实现登录状态检查逻辑：调用 `/api/admin/stats`，401 视为未登录，200 视为已登录
- [x] 3.3 添加内存缓存避免每次导航都请求 API，401 时清除缓存

## 4. 前端 — 错误提示

- [x] 4.1 `DashboardView.vue`：catch 块中添加 `ElMessage.error("加载统计数据失败")`
- [x] 4.2 `ClientListView.vue`：catch 块中添加 `ElMessage.error("加载客户端列表失败")`
- [x] 4.3 `UserListView.vue`：catch 块中添加 `ElMessage.error("加载用户列表失败")`
- [x] 4.4 `AuditLogView.vue`：catch 块中添加 `ElMessage.error("加载审计日志失败")`
- [x] 4.5 `AccessView.vue`：catch 块中添加 `ElMessage.error("加载权限列表失败")`
