## Why

需要一个生产级的 OAuth2 单点登录 (SSO) 学习项目，深入理解 OAuth2.1/OIDC 协议在真实场景中的实现方式。跨域 SSO 是企业级应用的常见需求，通过从零搭建认证中心和多客户端，可以完整掌握授权码流程、PKCE 安全增强、Token 生命周期管理等核心概念。

## What Changes

- 搭建基于 Spring Authorization Server 的 OAuth2/OIDC 认证中心
- 实现授权码流程 + PKCE 安全增强
- 实现 JWT Token 签发、刷新、撤销
- 实现自定义登录页面和用户认证 (MyBatis-Plus + MySQL)
- 搭建 Vue3 前端客户端，封装 OAuth2 PKCE 流程
- 搭建 Spring Boot 客户端B，验证跨域 SSO 自动登录
- 预留 Resource Server 模块骨架
- 支持客户端动态注册 (数据库配置)
- 实现 Refresh Token 轮转和 Token 撤销
- CORS 精确配置和安全加固

## Capabilities

### New Capabilities

- `auth-server`: OAuth2/OIDC 认证中心核心能力，包括授权端点、Token 端点、用户认证、客户端管理、JWT 签发与验证
- `oauth2-pkce-client`: Vue3 SPA 前端客户端，实现 OAuth2 PKCE 授权码流程、Token 存储与刷新、用户信息展示
- `sso-client-b`: Spring Boot 服务端客户端，实现 OAuth2 授权码流程，验证跨域 SSO 自动登录能力
- `token-management`: Token 全生命周期管理，包括 Refresh Token 轮转、Token 撤销、Token 存储策略
- `cross-domain-sso`: 跨域 SSO 会话管理，基于 auth-server 统一会话实现多客户端单点登录

### Modified Capabilities

(无，这是全新项目)

## Impact

- **新增模块**: auth-server (Maven 子模块)、app-vue (独立 Vue 项目)、client-b (Maven 子模块)、resource-server (预留骨架)
- **基础设施**: MySQL 8 数据库 (oauth2_sso 库)、hosts 文件域名映射 (auth.local / app-a.local / app-b.local)
- **端口占用**: auth-server(:9000)、app-vue(:5173)、client-b(:8082)、resource-server(:8083)
- **外部依赖**: Spring Authorization Server 1.2+、MyBatis-Plus、MySQL Connector、Vue3/Vite/Pinia/Axios
