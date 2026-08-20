# 提取用户中心服务（user-service）

## Why

当前项目中，用户信息（`sys_user` 表）的读写全部耦合在 `auth-center/backend` 中——认证加载、用户注册、JWT 签发、管理员接口均通过 `SysUserMapper` 直接访问数据库。`client-app` 仅从 JWT claims 获取有限的用户信息（nickname/email/phone），无法获取完整的用户资料。

这种架构存在以下问题：
- **职责不清**：认证中心同时承担用户管理和认证两个职责，违反单一职责原则
- **数据耦合**：`client-app` 等外部系统无法独立查询用户信息，只能依赖 JWT 中的有限字段
- **扩展困难**：新增用户字段需要修改 auth-center 的 JWT 自定义器、UserInfo 端点、管理员接口等多处代码

需要将用户信息管理抽取为独立的 `user-service` 微服务，提供统一的用户查询接口，供 `auth-center` 和 `client-app` 调用。

## What Changes

- 新建 `platform/user-service` 模块，提供用户查询、注册、管理 REST API，使用独立数据库 `user_center`
- 在根 `pom.xml` 的 `<modules>` 中注册 `platform/user-service` 模块
- `auth-center/backend` 引入 OpenFeign 依赖，新增 `UserServiceClient` Feign 客户端和 `UserDTO`
- `auth-center` 的 `CustomUserDetailsService.loadUserByUsername()` 改为通过 Feign 调用 user-service 获取用户信息
- `auth-center` 的 `RegisterService.register()` 改为调用 user-service 的注册接口（`POST /api/users`）
- `auth-center` 的 `OAuth2TokenCustomizerConfig` 通过 `CustomUserDetailsService` 间接获取用户信息，无需直接修改调用方式
- `auth-center` 的 `UserInfoController` 改为调用 user-service 获取用户资料
- `auth-center` 的 `AdminController` 用户管理接口（列表、状态切换）改为调用 user-service，移除 `SysUserMapper` 直接依赖
- `client-app/backend` 引入 OpenFeign 依赖，新增 `UserServiceClient` 和 `UserDTO`
- `client-app` 的 `UserInfoApiController` 改为从 JWT 取 username 后调用 user-service 获取完整用户资料
- `standalone-app` 不涉及（纯前端，无后端服务）

## Capabilities

### New Capabilities

- `user-center-service`: 用户中心微服务，提供用户信息查询、用户注册、用户管理 REST API

### Modified Capabilities

- `auth-server-user-auth`: 认证中心用户认证流程，从直接 DB 访问改为调用 user-service
- `client-app-userinfo`: 客户端应用用户信息获取，从纯 JWT claims 改为调用 user-service

## Impact

- **后端 API 兼容性**: user-service 新增 REST API（`/api/users/**`），不影响现有对外接口
- **前端**: 无前端变更，standalone-app 和各管理后台不涉及
- **数据库**: 新增 `user_center` 数据库（`sys_user` 表），`oauth2_center` 中的 `sys_user` 表数据需迁移
- **依赖**: auth-center 和 client-app 新增 `spring-cloud-starter-openfeign` 和 `spring-cloud-starter-loadbalancer`
- **代码变更**: auth-center 的 6 个文件改造，client-app 的 2 个文件改造
- **配置变更**: auth-center 和 client-app 的 `application.yml` 新增 `user-service.url` 配置
