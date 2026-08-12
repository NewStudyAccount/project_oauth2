## Why

auth-server 当前存在三套客户端存储互不相通的问题：`application.yml` 静态配置（运行时生效）、`oauth2_client` 表（仅管理展示）、`oauth2_registered_client` 表（空表未使用）。同时，授权记录和授权确认使用框架自带的 `JdbcOAuth2AuthorizationService` / `JdbcOAuth2AuthorizationConsentService`，无法灵活定制。此外，auth-server 缺少管理员后台界面，无法通过页面管理用户、客户端和权限。

本次变更将：用自定义实现替代框架默认的 JDBC 存储，打通客户端管理；同时新增管理员后台页面。

## What Changes

- 新增 `DatabaseClientRepository` 实现 `RegisteredClientRepository`，从 `oauth2_client` 表读取客户端配置，替代 `application.yml` 静态配置
- 新增 `DatabaseAuthorizationService` 实现 `OAuth2AuthorizationService`，从 `oauth2_authorization` 表读写授权记录，替代框架的 `JdbcOAuth2AuthorizationService`
- 新增 `DatabaseConsentService` 实现 `OAuth2AuthorizationConsentService`，从 `oauth2_authorization_consent` 表读写授权确认，替代框架的 `JdbcOAuth2AuthorizationConsentService`
- **BREAKING** 移除 `application.yml` 中 `spring.security.oauth2.authorizationserver.client` 配置，客户端完全由数据库管理
- 修改 `AuthorizationServerConfig`，注册新的自定义 Bean
- 新增管理员后台页面：用户管理、客户端管理、权限管理、审计日志、Token 管理
- 新增 `AdminPageController` 处理管理后台页面路由

## Capabilities

### New Capabilities
- `custom-client-storage`: 自定义客户端存储实现，从 oauth2_client 表读取并转换为 RegisteredClient 对象
- `custom-authorization-storage`: 自定义授权记录存储实现，读写 oauth2_authorization 表
- `custom-consent-storage`: 自定义授权确认存储实现，读写 oauth2_authorization_consent 表
- `admin-dashboard`: 管理员后台页面，包含用户管理、客户端管理、权限管理、审计日志、Token 管理

### Modified Capabilities

（无已有 spec 需要修改）

## Impact

- **代码变更**：新增 3 个 Service 类、1 个页面 Controller、6-7 个 Thymeleaf 模板、1 个 CSS 文件
- **配置变更**：移除 yml 中的客户端配置，修改 AuthorizationServerConfig
- **数据库**：表结构不变（oauth2_client、oauth2_authorization、oauth2_authorization_consent）
- **依赖**：无新增依赖
- **兼容性**：客户端数据来源从 yml 切换到数据库，重启后生效。需确保 data.sql 中的初始数据已导入
