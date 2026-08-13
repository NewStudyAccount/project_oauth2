## Context

auth-server 的管理后台当前存在架构问题：

1. **数据双写**：管理后台操作自定义 `oauth2_client` 表（MyBatis-Plus），OAuth2 协议使用框架标准 `oauth2_registered_client` 表（JdbcRegisteredClientRepository），两者独立，管理后台的增删改不会同步到框架表
2. **前端技术栈老旧**：使用 Thymeleaf 服务端渲染，交互体验差
3. **之前 NPE 的根因**：手动拼 JSON 迁移脚本缺少 `authorizationCodeTimeToLive` 字段

当前 auth-server 使用 Spring Authorization Server 1.2.4 + Spring Boot 3.2.5，内置 JDBC 实现（JdbcRegisteredClientRepository、JdbcOAuth2AuthorizationService、JdbcOAuth2AuthorizationConsentService）。

## Goals / Non-Goals

**Goals:**
- 消除客户端数据双写，统一使用 `oauth2_registered_client` 作为唯一数据源
- 管理后台改造成 Vue3 + Element Plus 前后端分离架构
- 客户端 CRUD 通过 `RegisteredClientRepository` API 操作，由框架保证 JSON 字段正确性
- 保留用户管理、权限管理、审计日志功能

**Non-Goals:**
- Token 管理功能（暂不实现）
- 修改 OAuth2 协议流程本身
- 修改 `app-vue3-springboot` 或 `app-springboot` 项目
- 网关/Nacos 集成

## Decisions

### D1: 管理后台认证方式 — 方案 b（同域 Session）

**选择**：管理后台 Vue3 应用通过 vite proxy 与 auth-server 同域，直接使用 auth-server 的 form login session 认证。

**备选**：方案 a（管理后台作为独立 OAuth2 客户端走 SSO）。

**理由**：管理后台本质上是 auth-server 的管理界面，不是独立业务应用。方案 b 更简单，不需要额外注册 OAuth2 客户端，不需要处理 token 刷新，与现有 `SecurityConfig` 的 session 认证完全兼容。

### D2: 客户端 CRUD — 通过 RegisteredClientRepository API

**选择**：后端管理 API 不直接操作 `oauth2_registered_client` 表的 SQL，而是通过 `RegisteredClient.builder()` 构建对象后调用 `registeredClientRepository.save()`。

**备选**：直接用 JdbcTemplate 操作 SQL。

**理由**：框架的 `JdbcRegisteredClientRepository.save()` 内部已经正确处理了 `client_settings` 和 `token_settings` 的 JSON 序列化（包括 `@class` 类型信息），可以彻底避免手动拼 JSON 导致的 NPE 问题。

### D3: 客户端列表/详情 — 反序列化 JSON 展开为 DTO

**选择**：读取客户端时，从 `registeredClientRepository.findById()` / `findByClientId()` 获取 `RegisteredClient` 对象，转换为前端友好的 DTO 返回，将 JSON 字段展开为结构化字段。

**理由**：前端不需要知道 `client_settings` 和 `token_settings` 的 JSON 格式，DTO 提供扁平化的字段（如 `requireProofKey`、`accessTokenTtl` 秒数等）。

### D4: 客户端禁用 — client_settings 自定义 key

**选择**：在 `client_settings` JSON 中增加自定义 key `settings.client.enabled`（默认 true），管理后台通过此字段控制客户端启用/禁用。自定义 `RegisteredClientRepository` 包装类在 `findByClientId` 时检查此字段，禁用的客户端返回 null。

**备选**：在 `user_client_access` 层面控制。

**理由**：框架的 `RegisteredClientRepository` 没有"禁用"概念，直接在 client 级别控制更直观。包装类方式对框架侵入最小。

### D5: 前端项目结构 — 独立 admin-vue3 项目

**选择**：在项目根目录新建 `admin-vue3` 目录，使用 Vite + Vue3 + Element Plus + Vue Router + Axios。

**理由**：与现有 `app-vue3-springboot` 项目结构一致，开发时通过 vite proxy 转发 API 请求到 auth-server:9000。

### D6: 登录页保留 Thymeleaf 服务端渲染

**选择**：登录页（`/login`）保留 Thymeleaf 服务端渲染，不改为 Vue SPA。

**备选**：将登录页也改为 admin-vue3 中的 LoginView.vue。

**理由**：这是企业级 IAM 系统的共识做法（Keycloak、Auth0、Okta 均如此）：

1. **安全性** — 登录页是安全框架的核心组件，由 Spring Security `formLogin()` 和 `LoginUrlAuthenticationEntryPoint` 控制。服务端渲染不暴露 CSRF token 到 JS 全局作用域
2. **重定向可靠性** — OAuth2 授权流程中的重定向链路（`/oauth2/authorize` → `/login` → 登录成功 → 回到授权流程）由安全框架完全控制，SPA 路由会与后端重定向机制冲突
3. **兼容性** — 无 JS 环境也能工作（渐进增强）
4. **简单性** — 不需要处理 SPA 路由与后端重定向的冲突、CSRF token 传递、savedRequest 状态管理等复杂问题
5. **性能** — 登录页无需加载整个 SPA 框架，首屏更快

管理后台（admin-vue3）是已登录用户使用的 SPA，与登录页职责完全不同。视觉统一通过样式对齐（配色、字体、按钮风格与 Element Plus 一致）实现，而非技术栈统一。

### D7: 废弃代码清理

**选择**：删除以下代码（不是注释，直接删除）：
- `OAuth2Client` entity
- `OAuth2ClientMapper`
- `ClientService`（MyBatis-Plus 版）
- `AdminPageController`（Thymeleaf 页面控制器）
- `AdminTokenController`
- `DatabaseClientRepository`（已注释的代码）
- `DatabaseAuthorizationService`（已注释的代码）
- `DatabaseConsentService`（已注释的代码）
- `templates/admin/**`（Thymeleaf 模板）

**理由**：这些代码要么已经废弃（注释状态），要么依赖 `oauth2_client` 自定义表，清理后避免混淆。

## Risks / Trade-offs

- **[客户端禁用需要包装类]** → 实现 `EnabledCheckingRegisteredClientRepository` 包装 `JdbcRegisteredClientRepository`，在 `findByClientId` 时检查 `settings.client.enabled`，侵入性小
- **[迁移脚本不再需要]** → 之前 `migrate-to-registered-client.sql` 从 `oauth2_client` 迁移到 `oauth2_registered_client`，现在直接操作 `oauth2_registered_client`，迁移脚本可以保留但不再使用
- **[CSRF 处理]** → 前后端分离后，API 请求需要处理 CSRF。方案：对 `/api/admin/**` 禁用 CSRF（与现有 `/api/**` 规则一致），或使用 CookieCsrfTokenRepository + 前端读取 XSRF-TOKEN
- **[生产部署]** → admin-vue3 build 后的静态文件需要部署到 auth-server 的 classpath/static 下，或由反向代理（如 Nginx）分别代理前端和后端