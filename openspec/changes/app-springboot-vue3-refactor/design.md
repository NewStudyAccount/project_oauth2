## Context

app-springboot 当前是一个使用 Thymeleaf 模板的服务端渲染 Web 应用，作为 OAuth2 confidential client 接入 auth-server。后端持有 JWT token（存储在 HTTP session 中），浏览器仅通过 JSESSIONID cookie 保持会话。

改造目标：将前端替换为独立的 Vue3 SPA，后端保持 confidential client 角色不变，通过 BFF 模式为前端提供 JSON API。

## Goals / Non-Goals

**Goals:**
- Vue3 前端独立项目，独立开发、独立部署
- 后端移除 Thymeleaf，变为纯 API 服务
- 保持 OAuth2 confidential client 模式（后端持有 token）
- 开发环境通过 Vite proxy 联调
- 保持现有 API 端点兼容

**Non-Goals:**
- 不改为 PKCE 公共客户端模式
- 不引入新的 OAuth2 客户端注册
- 不改变 auth-server 配置
- 不实现 token 刷新逻辑（session 模式下由 Spring Security 自动处理）

## Decisions

### 1. 前端项目位置：独立目录 `app-vue3-springboot/`

**选择**: 在项目根目录创建新的 `app-vue3-springboot/` 目录，与现有 `app-springboot/` 平级。

**理由**: 保持前后端代码物理分离，各自有独立的构建流程。`app-springboot/` 保留为后端项目。

**替代方案**: 在 `app-springboot/` 内部创建 `frontend/` 子目录 — 会让项目结构变得混乱，且不利于独立部署。

### 2. 前端技术栈：Vue3 + Vite + Vue Router + Axios

**选择**:
- Vue3 (Composition API)
- Vite (构建工具)
- Vue Router (路由)
- Axios (HTTP 客户端)
- 无 UI 框架（保持简洁，使用原生 CSS）

**理由**: 与项目已有的 `app-vue` 保持技术栈一致性，降低学习成本。

### 3. 开发环境代理策略

**选择**: Vite dev server 代理以下路径到后端：
- `/api/**` → `http://localhost:8082`
- `/oauth2/**` → `http://localhost:8082`
- `/login/**` → `http://localhost:8082`
- `/logout` → `http://localhost:8082`

**理由**: 开发时前端在 5173 端口，后端在 8082 端口，通过代理解决跨域。生产环境可通过 Nginx 反向代理统一入口。

### 4. CORS 配置策略

**选择**: 后端仅在开发环境启用 CORS（允许 `http://localhost:5173`），生产环境通过 Nginx 同源部署规避 CORS。

**理由**: 减少生产环境的安全面。开发环境需要 CORS 是因为前后端不同端口。

### 5. CSRF 处理方式

**选择**: 后端配置 `CookieCsrfTokenRepository`，设置 `HttpOnly=false` 使前端可读取 `XSRF-TOKEN` cookie。Axios 默认会自动从 cookie 读取 `XSRF-TOKEN` 并在请求头中携带 `X-XSRF-TOKEN`。

**理由**: 这是 Spring Security + SPA 的标准做法，Axios 原生支持。

### 6. 登出实现

**选择**: 前端调用 `POST /logout`（Spring Security 默认端点），后端使 session 失效。前端收到成功响应后清除本地状态并重定向到首页。暂不实现 SSO 全局登出（跳转 auth-server `/logout`）。

**理由**: 保持简单。SSO 全局登出可后续按需添加。

## Risks / Trade-offs

- **Session 亲和性**: BFF 模式依赖 session，多实例部署需要 session 共享（如 Spring Session + Redis）。→ 当前单实例开发环境无此问题，后续可引入。
- **JWT 密钥重启失效**: auth-server 重启会导致所有已有 token 失效，后端 session 中的 token 也会失效。→ 已有架构限制，非本次改造引入。
- **前端刷新页面**: SPA 刷新页面时需要重新检查登录状态（调用 `/api/userinfo`）。→ 标准 SPA 行为，前端需处理。

## Migration Plan

1. 创建 Vue3 前端项目 `app-vue3-springboot/`
2. 改造 `app-springboot/` 后端（移除 Thymeleaf，配置 CORS/CSRF，增强 API）
3. 前端开发页面和 API 对接
4. 联调验证完整流程
5. 可选：配置 Nginx 统一部署

回退策略：保留 `app-springboot/` 原始模板文件在 git 历史中，可随时恢复。

## Open Questions

（无）
