# 用户中心服务抽取 — 任务清单

## 1. 新建 user-service 模块

- [x] 1.1 创建 `platform/user-service/pom.xml`（spring-boot-starter-web, mybatis-plus, mysql, lombok, nacos-discovery）
- [x] 1.2 创建启动类 `UserServiceApplication.java`
- [x] 1.3 创建用户实体 `SysUser.java`（从 auth-center 迁移字段定义）
- [x] 1.4 创建 Mapper 接口 `SysUserMapper.java`
- [x] 1.5 创建 Service 层 `UserService.java`（查询、注册、列表、状态管理）
- [x] 1.6 创建 Controller 层 `UserController.java`（REST API）
- [x] 1.7 创建 `application.yml`（端口 8081，数据库 user_center，Nacos 配置）
- [x] 1.8 创建 `db/schema.sql`（sys_user 建表语句）
- [x] 1.9 在根 `pom.xml` 的 `<modules>` 中注册 `platform/user-service`

## 2. auth-center 改造

- [x] 2.1 在 `auth-center/backend/pom.xml` 添加 OpenFeign 和 LoadBalancer 依赖
- [x] 2.2 在 `AuthServerApplication.java` 添加 `@EnableFeignClients`
- [x] 2.3 创建 `UserDTO.java`（用户数据传输对象，不含 password）
- [x] 2.4 创建 `UserServiceClient.java` Feign 客户端接口
- [x] 2.5 改造 `CustomUserDetailsService`：`loadUserByUsername()` 调用 `UserServiceClient.getUserByUsernameFull()`，`getUserByUsername()` 调用 `UserServiceClient.getUserByUsername()`，移除 `SysUserMapper` 注入
- [x] 2.6 改造 `RegisterService`：`register()` 调用 `UserServiceClient.createUser()`，移除 `SysUserMapper` 直接写入
- [x] 2.7 改造 `UserInfoController`：调用 `UserServiceClient` 获取用户信息
- [x] 2.8 改造 `AdminController`：`listUsers()` 和 `setUserStatus()` 调用 `UserServiceClient`，移除 `SysUserMapper` 注入
- [x] 2.9 更新 `application.yml` 添加 `user-service.url` 配置

## 3. client-app 改造

- [x] 3.1 在 `client-app/backend/pom.xml` 添加 OpenFeign 和 LoadBalancer 依赖
- [x] 3.2 在 `AppSpringbootApplication.java` 添加 `@EnableFeignClients`
- [x] 3.3 创建 `UserDTO.java`
- [x] 3.4 创建 `UserServiceClient.java` Feign 客户端接口
- [x] 3.5 改造 `UserInfoApiController`：从 JWT 取 username，调用 `UserServiceClient.getUserByUsername()` 获取完整资料
- [x] 3.6 更新 `application.yml` 添加 `user-service.url` 配置

## 4. 数据库准备

- [x] 4.1 在 `user_center` 库创建 `sys_user` 表（SQL 脚本：`platform/user-service/src/main/resources/db/schema.sql`）
- [x] 4.2 将 `oauth2_center.sys_user` 现有数据迁移到 `user_center.sys_user`（SQL 脚本：`platform/user-service/src/main/resources/db/migrate-data.sql`）
- [x] 4.3 验证数据迁移完整性（迁移脚本包含验证查询）

## 5. 验证

- [ ] 5.1 启动 user-service，验证 `GET /api/users/admin` 返回用户信息
- [ ] 5.2 启动 user-service，验证 `POST /api/users` 注册接口
- [ ] 5.3 启动 auth-center + user-service，验证登录认证流程
- [ ] 5.4 启动 auth-center + user-service，验证 JWT 中包含 nickname/email/phone
- [ ] 5.5 启动 auth-center + user-service，验证 `/userinfo` 端点
- [ ] 5.6 启动 auth-center + user-service，验证管理员用户列表接口
- [ ] 5.7 启动 client-app + user-service + auth-center，验证 `/api/userinfo` 返回完整用户资料
