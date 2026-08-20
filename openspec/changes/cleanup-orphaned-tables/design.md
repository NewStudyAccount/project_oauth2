# 清理废弃表和代码设计

## Context

auth-center 中存在 3 个废弃的表及其代码：

| 表名 | Entity | Mapper | 废弃原因 |
|------|--------|--------|----------|
| `sys_user`（oauth2_center） | SysUser.java | SysUserMapper.java | 用户数据已迁移到 user-center，auth-center 通过 Feign 调用 user-service |
| `user_client_consent` | UserClientConsent.java | UserClientConsentMapper.java | 被 Spring Authorization Server 内置的 `oauth2_authorization_consent` 替代 |
| `sys_user_role` | SysUserRole.java | 无 | 从未实现，角色通过硬编码判断 |

## Goals / Non-Goals

**Goals:**
1. 删除不再使用的 Entity 类和 Mapper 接口
2. 从 schema.sql 中移除对应的建表语句
3. 从 data.sql 中移除 sys_user 的种子数据
4. 清理代码中对已删除类的引用

**Non-Goals:**
- 不删除数据库中的实际表（可选，由用户手动执行）
- 不改变任何业务逻辑

## Decisions

### Decision 1: sys_user 的种子数据处理

**选择**: 从 data.sql 中移除 sys_user 的 INSERT 语句

**理由**: 用户数据现在由 user-service 管理，auth-center 不再需要种子数据。admin 用户的创建应由 user-service 的 schema.sql/data.sql 负责。

### Decision 2: user_client_access 的外键引用

**选择**: 保留 user_client_access 表和代码，但移除其 INSERT 语句中对 sys_user 的引用

**理由**: user_client_access 仍在使用（AccessControlService），但它通过 user_id 字段关联用户，不依赖本地 sys_user 表。

## Risks / Trade-offs

- **[风险] 误删活跃代码**: 清理前需确认无调用链 → 已通过代码分析确认这些类无注入无调用
- **[权衡] 保留数据库表**: 代码删除后数据库表仍存在 → 可选手动 DROP，不影响功能
