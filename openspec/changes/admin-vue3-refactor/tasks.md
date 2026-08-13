## 1. 后端：清理废弃代码

- [x] 1.1 删除 OAuth2Client entity 和 OAuth2ClientMapper
- [x] 1.2 删除 ClientService（MyBatis-Plus 版）
- [x] 1.3 删除 AdminPageController（Thymeleaf 页面控制器）
- [x] 1.4 删除 AdminTokenController
- [x] 1.5 删除 DatabaseClientRepository、DatabaseAuthorizationService、DatabaseConsentService（已注释的代码文件）
- [x] 1.6 删除 templates/admin/** 目录下所有 Thymeleaf 模板
- [x] 1.7 清理 AuthorizationServerConfig 中已注释的自定义实现说明注释

## 2. 后端：客户端管理 DTO 和转换逻辑

- [x] 2.1 创建 ClientDTO 类，包含扁平化的客户端字段（clientId、clientSecret、clientName、clientAuthenticationMethods 列表、authorizationGrantTypes 列表、redirectUris 列表、scopes 列表、requireProofKey、requireAuthorizationConsent、accessTokenTtl 秒、refreshTokenTtl 秒、authorizationCodeTtl 秒、enabled）
- [x] 2.2 创建 ClientConverter 工具类，实现 RegisteredClient → ClientDTO 转换（展开 JSON 字段为扁平字段）
- [x] 2.3 实现 ClientConverter 的 ClientDTO → RegisteredClient 转换（使用 RegisteredClient.builder()，设置 TokenSettings 和 ClientSettings）

## 3. 后端：EnabledCheckingRegisteredClientRepository 包装类

- [x] 3.1 创建 EnabledCheckingRegisteredClientRepository，实现 RegisteredClientRepository 接口，包装 JdbcRegisteredClientRepository
- [x] 3.2 在 findByClientId 和 findById 中检查 client_settings 的 settings.client.enabled 字段，禁用时返回 null
- [x] 3.3 修改 AuthorizationServerConfig，将 JdbcRegisteredClientRepository 包装为 EnabledCheckingRegisteredClientRepository

## 4. 后端：重写 AdminController REST API

- [x] 4.1 重写 AdminController，注入 RegisteredClientRepository 替代 OAuth2ClientMapper
- [x] 4.2 实现 GET /api/admin/clients（列表，遍历所有客户端转为 DTO）
- [x] 4.3 实现 GET /api/admin/clients/{id}（详情）
- [x] 4.4 实现 POST /api/admin/clients（创建，DTO→RegisteredClient→save）
- [x] 4.5 实现 PUT /api/admin/clients/{id}（更新，保留未修改的 secret）
- [x] 4.6 实现 DELETE /api/admin/clients/{id}（删除）
- [x] 4.7 实现 PUT /api/admin/clients/{id}/status（启用/禁用，修改 settings.client.enabled）
- [x] 4.8 实现 GET /api/admin/users（用户列表）
- [x] 4.9 实现 PUT /api/admin/users/{id}/status（用户启用/禁用）
- [x] 4.10 实现 GET /api/admin/access（权限查询）
- [x] 4.11 实现 PUT /api/admin/access（权限设置）
- [x] 4.12 实现 GET /api/admin/audit-logs（审计日志查询）
- [x] 4.13 实现 GET /api/admin/stats（统计概览）

## 5. 后端：安全配置调整

- [x] 5.1 修改 SecurityConfig，确保 /api/admin/** 需要 ADMIN 角色
- [x] 5.2 配置 /api/admin/** 的 CSRF 策略（禁用或使用 Cookie + header 方案）
- [x] 5.3 配置 CORS 允许 admin-vue3 开发服务器跨域访问

## 6. 前端：admin-vue3 项目初始化

- [x] 6.1 在项目根目录创建 admin-vue3 项目（Vite + Vue3）
- [x] 6.2 安装 Element Plus、Vue Router、Axios 依赖
- [x] 6.3 配置 vite.config.ts（host、proxy 到 auth-server:9000）
- [x] 6.4 配置 Axios 实例（baseURL、withCredentials、401 拦截跳转登录）

## 7. 前端：路由和布局

- [x] 7.1 创建 Vue Router 配置（Dashboard、Clients、ClientForm、Users、Access、AuditLogs）
- [x] 7.2 创建 AppLayout 组件（Element Plus Container 布局：顶栏 + 侧栏 + 主区域）
- [x] 7.3 实现登录状态检测，未登录跳转 auth-server /login

## 8. 前端：客户端管理页面

- [x] 8.1 创建 ClientListView 页面（el-table 展示客户端列表，含启用/禁用开关）
- [x] 8.2 创建 ClientFormView 页面（el-form 表单，支持新增和编辑模式）
- [x] 8.3 实现客户端认证方式、授权类型多选组件
- [x] 8.4 实现重定向 URI 和 Scopes 动态添加/删除行
- [x] 8.5 实现客户端删除确认对话框
- [x] 8.6 创建 api/client.js 封装客户端管理 API 调用

## 9. 前端：其他管理页面

- [x] 9.1 创建 DashboardView 页面（统计卡片）
- [x] 9.2 创建 UserListView 页面（用户列表 + 启用/禁用）
- [x] 9.3 创建 AccessView 页面（用户选择 + 权限列表）
- [x] 9.4 创建 AuditLogView 页面（日志表格 + 筛选）
- [x] 9.5 创建 api/user.js、api/access.js、api/audit.js 封装 API 调用

## 10. 登录页美化（Thymeleaf，对齐 Element Plus 视觉风格）

- [x] 10.1 美化 login.html，配色、字体、按钮风格对齐 Element Plus（主色 #409eff、圆角、阴影）
- [x] 10.2 添加 OAuth2 授权流程中的 consent 页面（如不存在则创建），同样对齐 Element Plus 风格
- [x] 10.3 确保登录页在 OAuth2 授权流程和管理后台登录两个场景下都正常工作

## 11. 集成验证

- [ ] 11.1 验证客户端 CRUD 通过 RegisteredClientRepository 正确写入 oauth2_registered_client 表
- [ ] 11.2 验证禁用客户端后 OAuth2 授权流程返回错误
- [ ] 11.3 验证管理后台前后端联调（登录→列表→CRUD→权限→审计）
- [ ] 11.4 验证现有 OAuth2 客户端（vue-app、springboot-app）功能不受影响