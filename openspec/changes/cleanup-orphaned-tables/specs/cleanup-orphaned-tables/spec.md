## Purpose

清理 auth-center 中不再使用的 Entity、Mapper 和 SQL 脚本。

## ADDED Requirements

### Requirement: 废弃代码清理

auth-center 中不再使用的 Entity 类和 Mapper 接口 SHALL 被删除。

#### Scenario: SysUser 实体删除
- **WHEN** 检查 auth-center 的 entity 包
- **THEN** 不存在 `SysUser.java`（用户数据由 user-service 管理）

#### Scenario: UserClientConsent 实体删除
- **WHEN** 检查 auth-center 的 entity 包
- **THEN** 不存在 `UserClientConsent.java`（被 Spring Authorization Server 内置表替代）

#### Scenario: SysUserRole 实体删除
- **WHEN** 检查 auth-center 的 entity 包
- **THEN** 不存在 `SysUserRole.java`（从未实现的占位符）

#### Scenario: SysUserMapper 删除
- **WHEN** 检查 auth-center 的 repository 包
- **THEN** 不存在 `SysUserMapper.java`

#### Scenario: UserClientConsentMapper 删除
- **WHEN** 检查 auth-center 的 repository 包
- **THEN** 不存在 `UserClientConsentMapper.java`

### Requirement: SQL 脚本更新

auth-center 的 schema.sql 和 data.sql SHALL 不包含已废弃表的定义和数据。

#### Scenario: schema.sql 清理
- **WHEN** 检查 auth-center 的 schema.sql
- **THEN** 不包含 `sys_user`、`user_client_consent`、`sys_user_role` 的 CREATE TABLE 语句

#### Scenario: data.sql 清理
- **WHEN** 检查 auth-center 的 data.sql
- **THEN** 不包含 `sys_user` 的 INSERT 语句

### Requirement: 编译通过

清理后的 auth-center 后端 SHALL 能正常编译。

#### Scenario: 编译验证
- **WHEN** 执行 `mvn compile`
- **THEN** 无编译错误
