## Why

auth-server 当前存在客户端数据双写问题：管理后台操作的是自定义 `oauth2_client` 表（MyBatis-Plus），而 OAuth2 协议流程使用框架内置的 `oauth2_registered_client` 表（JdbcRegisteredClientRepository），两者之间仅靠迁移脚本单向同步，管理后台的增删改不会反映到框架使用的表上，导致数据不一致和运行时 NPE。同时，管理后台前端使用 Thymeleaf 服务端渲染，交互体验差，需要改造成前后端分离架构。

## What Changes

- **BREAKING**: 废弃 `oauth2_client` 自定义表及其所有相关代码（Entity、Mapper、Service），客户端数据统一使用框架标准表 `oauth2_registered_client`
- **BREAKING**: 废弃 Thymeleaf 管理后台页面（`templates/admin/**`），改用 Vue3 + Element Plus 前后端分离架构
- 新建 `admin-vue3` 前端项目，通过 vite proxy 与 auth-server 通信，使用 auth-server 的 session 认证（方案 b：管理后台是 auth-server 的一部分）
- 重写后端管理 API（`/api/admin/**`），客户端 CRUD 通过 `RegisteredClientRepository` API 操作，由框架自动处理 JSON 字段序列化
- 保留用户管理、权限管理、审计日志功能，改为 REST API 驱动
- Token 管理功能暂不实现

## Capabilities

### New Capabilities
- `admin-rest-api`: 管理后台 REST API 层，包括客户端管理（基于 RegisteredClientRepository）、用户管理、权限管理、审计日志、统计概览
- `admin-vue3-frontend`: Vue3 + Element Plus 管理后台前端，包括客户端 CRUD 表单、用户列表、权限管理、审计日志、仪表盘

### Modified Capabilities

## Impact

- **auth-server 后端**: 移除 OAuth2Client entity/mapper、ClientService、AdminPageController、AdminTokenController；重写 AdminController；新增客户端管理 DTO 和转换逻辑
- **auth-server 安全配置**: `/api/admin/**` 保持 session 认证 + ADMIN 角色，CSRF 对 API 放行或改用 token 方案
- **数据库**: `oauth2_client` 表废弃（不删除），`oauth2_registered_client` 成为唯一客户端数据源；`user_client_access` 表的 `client_id` 字段含义不变
- **新增项目**: `admin-vue3` 前端项目（Vite + Vue3 + Element Plus + Vue Router + Axios）
- **依赖**: auth-server 需确保 `spring-boot-starter-web` 已包含（已有）；admin-vue3 新增 Element Plus 依赖