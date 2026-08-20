# 用户中心服务抽取设计

## Context

当前项目是一个基于 Spring Cloud + Nacos 的 OAuth2 SSO 平台，包含以下后端模块：

- `auth-center/backend`（端口 9000）— OAuth2 授权服务器，直接访问 `oauth2_center` 库的 `sys_user` 表
- `client-app/backend`（端口 8082）— OAuth2 Client + Resource Server，仅从 JWT claims 获取用户信息
- `platform/gateway`（端口 8080）— Spring Cloud Gateway
- `platform/resource-api`（端口 8083）— JWT 资源服务

`auth-center` 中涉及 `SysUser` 的代码分布在 6 个文件中：
- `CustomUserDetailsService` — 认证加载（loadUserByUsername）+ 用户查询
- `RegisterService` — 用户注册（写入 sys_user）
- `OAuth2TokenCustomizerConfig` — JWT 签发时注入用户字段
- `UserInfoController` — `/userinfo` 端点
- `AdminController` — 用户列表、状态管理
- `SysUserMapper` — MyBatis-Plus Mapper 接口

`client-app` 中涉及用户信息的代码：
- `UserInfoApiController` — 从 JWT/OIDC 提取 username/email/nickname

## Goals / Non-Goals

**Goals:**
1. 将用户信息查询能力抽取为独立的 `user-service` 微服务
2. `auth-center` 通过 Feign 调用 user-service 获取用户信息（认证、JWT 签发、用户管理）
3. `client-app` 通过 Feign 调用 user-service 获取完整用户资料
4. user-service 使用独立数据库 `user_center`，与 `oauth2_center` 分离
5. user-service 注册到 Nacos 服务发现，支持负载均衡

**Non-Goals:**
- 不迁移 `standalone-app`（纯前端，无后端）
- 不迁移 auth-center 的认证逻辑本身（密码校验仍在 auth-center 完成，只是数据来源改为 user-service）
- 不改变 OAuth2 协议流程和 Token 格式
- 不为 user-service 添加 API 认证（开发阶段暂不做）

## Decisions

### Decision 1: user-service 模块位置

**选择**: 放在 `platform/user-service/` 目录下

**替代方案**:
- A: 放在根目录下 `user-service/`
- B: 放在 `auth-center/user-service/`（作为 auth-center 的子模块）

**理由**: 与 `platform/gateway`、`platform/resource-api` 保持一致，`platform` 目录存放平台级基础服务。

### Decision 2: 服务间通信方式

**选择**: 使用 OpenFeign 声明式 HTTP 客户端

**替代方案**:
- A: 直接使用 `RestTemplate` / `RestClient`
- B: gRPC

**理由**: OpenFeign 是 Spring Cloud 生态的标准方案，声明式接口更清晰，与项目现有的 Spring Cloud 技术栈一致。虽然当前选择直连方式（不走 Nacos），但 Feign 的 `url` 属性支持硬编码地址，后续可平滑切换到服务发现。

### Decision 3: auth-center 用户数据访问方式

**选择**: auth-center 移除 `SysUserMapper`，所有用户数据通过 Feign 调用 user-service 获取

**替代方案**:
- A: auth-center 保留 SysUserMapper，与 user-service 共享同一数据库
- B: auth-center 保留 SysUserMapper 但只读，user-service 负责写入

**理由**: 方案 A 违背微服务独立数据库原则；方案 B 存在数据一致性风险。完全移除直接 DB 访问，auth-center 只负责认证逻辑和 OAuth2 协议处理。

### Decision 4: 注册功能归属

**选择**: user-service 提供 `POST /api/users` 注册接口，auth-center 的 `RegisterService` 调用它完成注册

**替代方案**:
- A: 注册逻辑留在 auth-center，auth-center 直接写 user-service 的数据库

**理由**: 用户数据完全由 user-service 管理，注册作为用户创建的入口应该在 user-service。auth-center 仅负责验证码校验（Redis）和审计日志记录。

### Decision 5: Feign 客户端模块组织

**选择**: auth-center 和 client-app 各自独立定义 `UserServiceClient` 接口和 `UserDTO`

**替代方案**:
- A: 抽取公共模块 `user-service-api`（包含 Feign 接口 + DTO），两个消费者依赖它

**理由**: 当前只有两个消费者，公共模块会增加构建复杂度。各自定义更简单，后续如果消费者增多再考虑抽取。

### Decision 6: 数据库迁移策略

**选择**: 在 user-service 的 `schema.sql` 中包含建表语句，手动迁移现有数据

**替代方案**:
- A: 使用 Flyway/Liquibase 自动迁移
- B: 两个服务共享同一数据库

**理由**: 项目当前使用手动 SQL 脚本管理 schema（`auth-center/backend/src/main/resources/db/`），保持一致。数据量小，手动迁移即可。

## Risks / Trade-offs

- **[风险] 网络调用增加延迟**: 认证流程从本地 DB 查询变为 HTTP 调用，增加约 5-20ms 延迟 → 对于认证场景可接受，后续可通过缓存优化
- **[风险] user-service 不可用导致认证失败**: user-service 宕机时 auth-center 无法认证用户 → 开发阶段暂不做降级，生产环境需添加熔断和降级策略
- **[风险] 密码哈希通过 HTTP 传输**: `/api/users/{username}/full` 返回密码哈希用于认证 → 仅限内网调用，开发阶段暂不做 API 认证，生产环境需添加 API Key 或 mTLS
- **[权衡] 重复定义 Feign 接口**: auth-center 和 client-app 各自维护 UserServiceClient → 增加少量重复代码，但降低模块耦合度

## Open Questions

- user-service 是否需要支持批量查询接口（如 `POST /api/users/batch`），供 auth-center 批量获取用户信息？
- 生产环境是否需要引入 Sentinel 或 Resilience4j 做熔断降级？

## Migration Plan

1. 创建 user-service 模块并初始化数据库
2. 部署 user-service 并验证 API 可用性
3. 改造 auth-center，切换到 Feign 调用
4. 改造 client-app，切换到 Feign 调用
5. 迁移 `oauth2_center.sys_user` 数据到 `user_center.sys_user`

**回滚策略**: auth-center 保留 `SysUser` 实体和 `SysUserMapper` 的代码（但不注入），如需回滚只需恢复 `@MapperScan` 和 `CustomUserDetailsService` 的直接 DB 调用逻辑。
