# 清理废弃表和代码 — 任务清单

## 1. 删除废弃 Entity 类

- [x] 1.1 删除 `auth-center/backend/src/main/java/com/example/authserver/entity/SysUser.java`
- [x] 1.2 删除 `auth-center/backend/src/main/java/com/example/authserver/entity/UserClientConsent.java`
- [x] 1.3 删除 `auth-center/backend/src/main/java/com/example/authserver/entity/SysUserRole.java`

## 2. 删除废弃 Mapper 接口

- [x] 2.1 删除 `auth-center/backend/src/main/java/com/example/authserver/repository/SysUserMapper.java`
- [x] 2.2 删除 `auth-center/backend/src/main/java/com/example/authserver/repository/UserClientConsentMapper.java`

## 3. 更新 SQL 脚本

- [x] 3.1 从 `auth-center/backend/src/main/resources/db/schema.sql` 中移除 `sys_user`、`user_client_consent`、`sys_user_role` 的建表语句
- [x] 3.2 从 `auth-center/backend/src/main/resources/db/data.sql` 中移除 `sys_user` 的 INSERT 语句

## 4. 清理孤立引用

- [x] 4.1 检查并移除代码中对已删除类的 import 引用（RegisterController、OAuth2TokenCustomizerConfig）

## 5. 验证

- [ ] 5.1 编译 auth-center 后端，确认无编译错误
- [ ] 5.2 启动 auth-center，确认功能正常
