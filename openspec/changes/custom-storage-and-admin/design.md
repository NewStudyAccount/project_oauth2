## Context

auth-server 使用 Spring Authorization Server 构建 OAuth2 授权服务。当前存在三套客户端存储互不相通：
- `application.yml` 静态配置（运行时实际使用）
- `oauth2_client` 表（仅管理后台 CRUD 展示）
- `oauth2_registered_client` 表（空表未使用）

授权记录和授权确认使用框架自带的 JDBC 实现，表结构为框架固定 schema。管理员无后台页面，只能通过 API 操作。

## Goals / Non-Goals

**Goals:**
- 用自定义实现替代框架默认的 JDBC 存储，实现对三张表的完全控制
- 打通 `oauth2_client` 表与框架的 `RegisteredClientRepository`，使数据库中的客户端配置生效
- 新增管理员后台页面，支持用户、客户端、权限、审计日志、Token 的可视化管理

**Non-Goals:**
- 不修改现有数据库表结构
- 不改变 OAuth2 协议流程
- 不实现前后端分离（仍使用 Thymeleaf）
- 不新增外部依赖

## Decisions

### Decision 1: 自定义 RegisteredClientRepository 读 oauth2_client 表

**选择**：实现 `RegisteredClientRepository` 接口，在 `findByClientId` / `findById` 中查询 `oauth2_client` 表并转换为 `RegisteredClient` 对象。

**转换逻辑**：
- `scopes`（逗号分隔字符串）→ `Set<String>`：`split(",")`
- `grant_types`（逗号分隔字符串）→ `Set<AuthorizationGrantType>`：逐个映射
- `redirect_uris`（逗号分隔字符串）→ `Set<String>`：`split(",")`
- `client_type`（PUBLIC/CONFIDENTIAL）→ `ClientAuthenticationMethod`：
  - PUBLIC → `ClientAuthenticationMethod.NONE`
  - CONFIDENTIAL → `ClientAuthenticationMethod.CLIENT_SECRET_BASIC`
- `access_token_ttl` / `refresh_token_ttl`（秒数）→ `TokenSettings`
- `require_consent`（0/1）→ `ClientSettings`

**替代方案**：修改表结构为 JSON 格式。放弃原因：改动面大，且现有表已有数据。

### Decision 2: 自定义 OAuth2AuthorizationService 读写原表

**选择**：实现 `OAuth2AuthorizationService` 接口，直接读写现有的 `oauth2_authorization` 框架表。表结构不变，只是把框架的 `JdbcOAuth2AuthorizationService` 替换为自定义实现。

**理由**：表结构保持不变，自定义实现提供了后续扩展的灵活性（如自定义序列化、添加额外字段等）。

### Decision 3: 自定义 OAuth2AuthorizationConsentService 读写原表

**选择**：同上，实现接口，读写 `oauth2_authorization_consent` 表。

### Decision 4: 管理后台使用 Thymeleaf 模板

**选择**：管理后台页面使用 Thymeleaf 模板，与现有登录/注册页面保持一致。

**布局方案**：使用 Thymeleaf layout 或 fragment 方式复用公共部分（导航栏、侧边栏）。

## Risks / Trade-offs

- **[风险] 自定义序列化复杂度**：`oauth2_authorization` 表中 `attributes`、`metadata` 等字段存储序列化的 Java 对象。自定义实现需要正确处理这些序列化/反序列化。
  → 缓解：使用 Spring 框架提供的 `JacksonOAuth2AuthorizationService` 序列化工具，或参考 `JdbcOAuth2AuthorizationService` 源码中的序列化方式。

- **[风险] 框架版本升级兼容性**：接口方法可能随版本变化。
  → 缓解：锁定 Spring Authorization Server 版本，升级时重新验证接口。

- **[取舍] save() 方法的 upsert 逻辑**：`OAuth2AuthorizationService.save()` 既用于新增也用于更新，需要实现 upsert 逻辑。
  → 处理：先查询是否存在，存在则更新，不存在则插入。

## Migration Plan

1. 实现三个自定义存储类
2. 在 `AuthorizationServerConfig` 中注册新 Bean（替换旧 Bean）
3. 移除 `application.yml` 中的客户端配置
4. 重启服务，验证 OAuth2 流程正常
5. 验证管理后台功能

**回滚策略**：恢复 `application.yml` 中的客户端配置，恢复使用框架 JDBC Bean。
