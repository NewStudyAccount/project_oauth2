## Context

当前 admin-vue3 管理页面在未登录时显示空表格，无任何错误提示。原因链：

1. Spring Security 默认对未认证请求返回 302 重定向到 `/login`
2. Vite 开发代理跟随 302 重定向，最终返回登录页 HTML（200 状态码）
3. axios 拦截器只处理 401，对 200 + HTML 响应无法识别
4. Vue 组件 catch 块只打 console.error，页面上不显示错误

现有安全配置：
- `AuthorizationServerConfig`（Order 1）：OAuth2 端点链，使用 `LoginUrlAuthenticationEntryPoint`
- `SecurityConfig`（Order 2）：默认链，处理页面 + API，formLogin 使用默认入口点

## Goals / Non-Goals

**Goals:**
- API 请求未认证时返回 401 JSON，而非 302 重定向
- 前端在路由级别拦截未登录访问
- 用户在操作失败时看到明确的错误提示
- 保持现有 OAuth2 授权流程和登录页面不变

**Non-Goals:**
- 不改变 OAuth2 授权端点（`/oauth2/**`）的行为
- 不改变登录/注册页面的渲染方式
- 不引入新的前端依赖
- 不改变 session 管理机制

## Decisions

### Decision 1: 自定义 AuthenticationEntryPoint 在 SecurityConfig 中

**选择**：在 `SecurityConfig.defaultFilterChain` 中注入自定义 `AuthenticationEntryPoint`，对 `/api/**` 返回 401 JSON，其他请求重定向到登录页。

**替代方案**：
- **A) Vite 代理层拦截 302**：在 `vite.config.ts` 的 proxy configure 中检测 302 响应并返回 401。缺点：仅限开发环境，生产部署不生效。
- **B) 全局 Filter 拦截**：写一个 Servlet Filter 在 Security Filter 之前检查。缺点：与 Spring Security 的 ExceptionTranslationFilter 职责重叠。
- **C) 前端检测 HTML 响应**：axios 拦截器检查 Content-Type 是否为 text/html。缺点：不够可靠，且无法区分正常 HTML 页面和错误响应。

**理由**：方案最直接，在 Spring Security 的认证入口点层面解决问题，对所有环境生效。与 `AuthorizationServerConfig` 的 OAuth2 端点链互不影响（Order 1 vs Order 2）。

**实现要点**：
- 创建 `DelegatingAuthenticationEntryPoint` 内部类
- 通过 `http.exceptionHandling().authenticationEntryPoint(...)` 注入
- `formLogin` 会创建自己的入口点，但 `exceptionHandling` 的设置会覆盖默认行为
- 需要验证 `formLogin` 的 `loginProcessingUrl("/login")` 仍然正常工作

### Decision 2: 前端路由守卫使用 API 探测

**选择**：在 `router.beforeEach` 中调用 `GET /api/admin/stats` 检查登录状态，缓存结果避免每次导航都请求。

**替代方案**：
- **A) 前端维护 login 状态变量**：登录成功后设 flag，刷新页面后丢失。缺点：无法处理 session 过期。
- **B) 调用 `/userinfo` 端点**：已有此端点但需要 JWT token，admin 页面使用 session 认证不适用。
- **C) 新增 `/api/admin/me` 端点**：轻量级端点仅检查认证状态。缺点：需要额外后端改动。

**理由**：`/api/admin/stats` 已存在且足够轻量，返回 401 表示未登录，200 表示已登录。缓存结果（存储在 sessionStorage 或内存变量中）避免频繁请求。

### Decision 3: 错误提示使用 Element Plus ElMessage

**选择**：在各 Vue 组件的 catch 块中使用 `ElMessage.error()` 显示错误。

**理由**：项目已引入 Element Plus，`ElMessage` 是标准的消息提示组件，不阻塞用户操作，3 秒后自动消失。

## Risks / Trade-offs

- **[风险] formLogin 入口点冲突** → `exceptionHandling().authenticationEntryPoint()` 可能与 `formLogin` 内部创建的入口点产生优先级冲突。缓解：测试 `/login` 页面的 POST 登录流程是否正常。
- **[风险] 路由守卫首次加载延迟** → 每次刷新页面都需要调用 API 检查登录状态。缓解：使用内存变量缓存，仅在 401 时清除缓存。
- **[权衡] stats 端点被用于认证检查** → 不是语义上最合适的端点，但避免了新增端点的额外工作。
