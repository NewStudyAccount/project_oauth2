## 1. 自定义客户端存储

- [x] 1.1 创建 `DatabaseClientRepository` 类，实现 `RegisteredClientRepository` 接口
- [x] 1.2 实现 `findByClientId` 方法：查询 `oauth2_client` 表并转换为 `RegisteredClient` 对象
- [x] 1.3 实现 `findById` 方法
- [x] 1.4 实现 `save` 方法（客户端新增/更新时同步到数据库）
- [x] 1.5 实现转换逻辑：scopes/grant_types/redirect_uris 逗号分隔字符串 → 集合
- [x] 1.6 实现转换逻辑：client_type → ClientAuthenticationMethod
- [x] 1.7 实现转换逻辑：access_token_ttl/refresh_token_ttl → TokenSettings
- [x] 1.8 实现转换逻辑：require_consent → ClientSettings

## 2. 自定义授权记录存储

- [x] 2.1 创建 `DatabaseAuthorizationService` 类，实现 `OAuth2AuthorizationService` 接口
- [x] 2.2 实现 `findById` 方法：查询 `oauth2_authorization` 表
- [x] 2.3 实现 `findByToken` 方法：按 token 值和类型查询
- [x] 2.4 实现 `save` 方法：新增/更新授权记录（upsert 逻辑）
- [x] 2.5 实现 `remove` 方法：删除授权记录
- [x] 2.6 实现 attributes/metadata 字段的序列化/反序列化

## 3. 自定义授权确认存储

- [x] 3.1 创建 `DatabaseConsentService` 类，实现 `OAuth2AuthorizationConsentService` 接口
- [x] 3.2 实现 `findById` 方法：查询 `oauth2_authorization_consent` 表
- [x] 3.3 实现 `save` 方法：新增/更新授权确认记录
- [x] 3.4 实现 `remove` 方法：删除授权确认记录

## 4. 配置更新

- [x] 4.1 修改 `AuthorizationServerConfig`：注册 `DatabaseClientRepository` Bean
- [x] 4.2 修改 `AuthorizationServerConfig`：注册 `DatabaseAuthorizationService` Bean
- [x] 4.3 修改 `AuthorizationServerConfig`：注册 `DatabaseConsentService` Bean
- [x] 4.4 移除 `application.yml` 中 `spring.security.oauth2.authorizationserver.client` 配置

## 5. 管理后台 - 后端

- [x] 5.1 创建 `AdminPageController`：管理后台页面路由
- [x] 5.2 实现管理后台首页 `/admin`：统计信息接口
- [x] 5.3 实现用户管理页面 `/admin/users`：用户列表、启用/禁用
- [x] 5.4 实现客户端管理页面 `/admin/clients`：客户端列表、新增、编辑
- [x] 5.5 实现权限管理页面 `/admin/access`：用户权限查看和修改
- [x] 5.6 实现审计日志页面 `/admin/audit-logs`：日志列表、筛选
- [x] 5.7 实现 Token 管理页面 `/admin/tokens`：活跃 Token 查看、强制撤销

## 6. 管理后台 - 前端

- [x] 6.1 创建 `admin/layout.html` 公共布局模板（导航栏 + 侧边栏）
- [x] 6.2 创建 `admin/index.html` 后台首页
- [x] 6.3 创建 `admin/users.html` 用户管理页面
- [x] 6.4 创建 `admin/clients.html` 客户端管理页面
- [x] 6.5 创建 `admin/access.html` 权限管理页面
- [x] 6.6 创建 `admin/audit-logs.html` 审计日志页面
- [x] 6.7 创建 `admin/tokens.html` Token 管理页面
- [x] 6.8 创建 `static/css/admin.css` 后台样式

## 7. 安全与权限

- [x] 7.1 修改 `SecurityConfig`：放行管理后台静态资源路径
- [x] 7.2 确保管理后台页面仅 ROLE_ADMIN 可访问
