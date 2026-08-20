# 清理废弃表和代码

## Why

在将用户管理抽取为独立的 user-service 微服务后，auth-center 中仍保留了 3 个不再使用的表和对应的 Entity/Mapper 代码：

- `sys_user`（oauth2_center 库）— 用户数据已迁移到 user_center 库，auth-center 改为 Feign 调用 user-service
- `user_client_consent`（oauth2_center 库）— 被 Spring Authorization Server 内置的 `oauth2_authorization_consent` 表替代
- `sys_user_role` — 从未实现，只有 Entity 类作为占位符

这些废弃代码增加了维护成本和理解难度，需要清理。

## What Changes

### 删除 Entity 类

- 删除 `auth-center/backend/.../entity/SysUser.java`
- 删除 `auth-center/backend/.../entity/UserClientConsent.java`
- 删除 `auth-center/backend/.../entity/SysUserRole.java`

### 删除 Mapper 接口

- 删除 `auth-center/backend/.../repository/SysUserMapper.java`
- 删除 `auth-center/backend/.../repository/UserClientConsentMapper.java`

### 更新 schema.sql

- 从 `auth-center/backend/src/main/resources/db/schema.sql` 中移除 `sys_user`、`user_client_consent`、`sys_user_role` 的建表语句

### 更新 data.sql

- 从 `auth-center/backend/src/main/resources/db/data.sql` 中移除 `sys_user` 的 INSERT 语句（admin 用户数据由 user-service 管理）
- 更新 `user_client_access` 的 INSERT 语句（如果引用了 sys_user）

### 清理孤立引用

- 检查并移除代码中对已删除类的 import 引用

## Capabilities

### New Capabilities

无

### Modified Capabilities

- `auth-server-storage`: 移除不再使用的 Entity 和 Mapper，精简代码

## Impact

- **后端 API 兼容性**: 无变化，这些表和代码从未被 API 使用
- **前端**: 无影响
- **数据库**: oauth2_center 库中可选 DROP 3 张表
- **依赖**: 无变化
- **代码变更**: 删除 5 个文件，修改 2 个 SQL 文件，清理零散 import
