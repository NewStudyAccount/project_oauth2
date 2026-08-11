# 提案：集成 Spring Cloud Gateway + Nacos（统一登录架构）

## 概述

将当前的 OAuth2 SSO 项目从独立部署的 Spring Boot 应用升级为 Spring Cloud 微服务架构。引入 Spring Cloud Gateway 作为统一入口和 OAuth2 Client，实现统一登录；使用 Nacos 作为服务注册中心。

## 架构方向

### 选择：方案 B - Gateway 作为 OAuth2 Client 统一登录

```
┌─────────────┐        ┌─────────────────────────────────────────┐
│  浏览器     │───────▶│         Spring Cloud Gateway            │
│             │        │         (OAuth2 Client)                 │
└─────────────┘        │                                         │
                       │  1. 用户访问受保护资源                      │
                       │  2. Gateway 检查 Session                  │
                       │  3. 未登录 → 重定向到 auth-server           │
                       │  4. 登录成功 → 获取 Token                  │
                       │  5. 转发请求 + Token 到下游服务             │
                       └────────────────┬────────────────────────┘
                                        │
                                        ▼
                               ┌─────────────────┐
                               │   auth-server   │
                               │   (授权服务)     │
                               │   处理登录       │
                               │   颁发 Token     │
                               └─────────────────┘
                                        │
                    ┌───────────────────┼───────────────────┐
                    ▼                   ▼                   ▼
             ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
             │app-springboot│    │ resource-api │    │  其他服务    │
             │  (下游服务)  │    │  (下游服务)  │    │             │
             └─────────────┘    └─────────────┘    └─────────────┘
```

### 选择理由

1. **统一登录体验**：用户只需登录一次，即可访问所有下游服务
2. **集中安全管理**：认证逻辑集中在 Gateway，下游服务无需重复实现
3. **架构清晰**：Gateway 作为统一入口，职责明确
4. **易于扩展**：新增下游服务时，无需关心认证逻辑

## 目标

### 主要目标
1. **统一入口**：所有客户端请求通过 Gateway 统一入口访问
2. **统一登录**：Gateway 作为 OAuth2 Client，统一处理用户登录
3. **Token 中继**：Gateway 将获取的 Token 转发给下游服务
4. **服务发现**：使用 Nacos 实现服务自动注册和发现
5. **动态路由**：Gateway 根据服务名动态路由到后端服务

### 非目标
- 不做服务间负载均衡（单实例即可）
- 不做熔断、限流等高级功能（后续可扩展）
- 不改变现有的 OAuth2 授权流程（auth-server 保持不变）
- 不改变现有的数据库结构

## 当前架构

```
┌─────────────┐        ┌─────────────┐        ┌─────────────┐
│  app-vue    │───────▶│ app-springboot│───────▶│ resource-api │
│  (Frontend) │        │  (Client)    │        │  (Resource)  │
└─────────────┘        └──────┬───────┘        └─────────────┘
                              │
                              ▼
                       ┌─────────────┐
                       │ auth-server │
                       │  (OAuth2)   │
                       └─────────────┘

访问方式: 直接访问各服务的 host:port
- auth.local:9000
- client.a.local:8082
```

## 目标架构

```
┌─────────────┐        ┌─────────────────────────────────────────┐
│  app-vue    │───────▶│         Spring Cloud Gateway            │
│  (Frontend) │        │         port: 8080                      │
└─────────────┘        │         (OAuth2 Client)                 │
                       │                                         │
                       │  统一登录：                              │
                       │    - 用户在 Gateway 登录                 │
                       │    - Gateway 管理 Token                  │
                       │    - 自动转发 Token 到下游服务            │
                       │                                         │
                       │  路由规则：                              │
                       │    /auth/**  ──▶ auth-server             │
                       │    /app/**   ──▶ app-springboot          │
                       │    /api/**   ──▶ resource-api            │
                       └────────────────┬────────────────────────┘
                                        │
                                        ▼
                               ┌─────────────────┐
                               │     Nacos       │
                               │  (Service       │
                               │   Registry)     │
                               │  port: 8848     │
                               └────────┬────────┘
                                        │
                    ┌───────────────────┼───────────────────┐
                    ▼                   ▼                   ▼
             ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
             │ auth-server │    │app-springboot│    │ resource-api │
             │  (OAuth2)   │    │  (下游服务)  │    │  (下游服务)  │
             │  port:9000  │    │  port:8082   │    │  port:8083   │
             │             │    │              │    │              │
             │  处理登录    │    │  接收 Token  │    │  接收 Token  │
             │  颁发 Token │    │  业务逻辑    │    │  业务逻辑    │
             └─────────────┘    └─────────────┘    └─────────────┘
```

## 登录流程

```
浏览器          Gateway           auth-server        下游服务
  │                │                  │                  │
  │ 1. GET /app    │                  │                  │
  │───────────────▶│                  │                  │
  │                │                  │                  │
  │                │ 2. 检查 Session   │                  │
  │                │    未登录         │                  │
  │                │                  │                  │
  │ 3. 302 重定向  │                  │                  │
  │◀───────────────│                  │                  │
  │    到 /oauth2/authorize           │                  │
  │                │                  │                  │
  │ 4. GET /oauth2/authorize          │                  │
  │──────────────────────────────────▶│                  │
  │                │                  │                  │
  │ 5. 返回登录页面 │                  │                  │
  │◀──────────────────────────────────│                  │
  │                │                  │                  │
  │ 6. POST /login │                  │                  │
  │──────────────────────────────────▶│                  │
  │                │                  │                  │
  │ 7. 返回 code   │                  │                  │
  │◀──────────────────────────────────│                  │
  │    到 redirect_uri                 │                  │
  │                │                  │                  │
  │ 8. GET /login/oauth2/code/auth-server               │
  │───────────────▶│                  │                  │
  │                │                  │                  │
  │                │ 9. 用 code 换 token                  │
  │                │─────────────────▶│                  │
  │                │                  │                  │
  │                │ 10. 返回 token    │                  │
  │                │◀─────────────────│                  │
  │                │                  │                  │
  │                │ 11. 存储 token 到 Session            │
  │                │                  │                  │
  │ 12. 重定向到 /app                  │                  │
  │◀───────────────│                  │                  │
  │                │                  │                  │
  │ 13. GET /app   │                  │                  │
  │───────────────▶│                  │                  │
  │                │                  │                  │
  │                │ 14. 从 Session 获取 token            │
  │                │    转发请求 + Authorization: Bearer token
  │                │─────────────────────────────────────▶│
  │                │                  │                  │
  │                │ 15. 返回响应      │                  │
  │                │◀─────────────────────────────────────│
  │                │                  │                  │
  │ 16. 返回页面   │                  │                  │
  │◀───────────────│                  │                  │
```

## 技术栈

| 组件 | 当前版本 | 目标版本 | 说明 |
|------|---------|---------|------|
| Spring Boot | 3.2.5 | 3.2.5 (不变) | 基础框架 |
| Spring Cloud | 无 | 2023.0.1 | 微服务框架 |
| Spring Cloud Gateway | 无 | 4.1.x | API 网关（WebFlux） |
| Spring Cloud Alibaba | 无 | 2023.0.1.0 | Nacos 集成 |
| Nacos | 无 | 2.3.x | 服务注册中心 |
| Redis | 已有 | 已有 | Session 存储 |

## 技术挑战

### 1. 响应式 vs Servlet

| 组件 | 技术栈 | 说明 |
|------|--------|------|
| Gateway | WebFlux (Reactive) | 必须使用响应式版本的 Spring Security |
| auth-server | Spring MVC (Servlet) | 使用标准 Servlet 版本 |
| app-springboot | Spring MVC (Servlet) | 使用标准 Servlet 版本 |
| resource-api | Spring MVC (Servlet) | 使用标准 Servlet 版本 |

**关键点**：Gateway 使用 Reactive，下游服务使用 Servlet，两者不冲突。

### 2. Session 管理

Gateway 需要管理用户的登录状态（Session），推荐使用 Redis：
- 支持分布式部署
- 支持 Session 共享
- 已有 Redis 基础设施

### 3. Token 中继

Gateway 需要将获取的 OAuth2 Token 转发给下游服务：
- 从 Session 获取 Token
- 添加到请求头 `Authorization: Bearer <token>`
- 下游服务验证 Token

## 影响范围

### 新增模块
- `gateway` - Spring Cloud Gateway 网关服务（OAuth2 Client）

### 修改模块
- `auth-server` - 添加 Nacos 依赖，注册到 Nacos
- `app-springboot` - 添加 Nacos 依赖，移除 OAuth2 Client 配置（由 Gateway 处理）
- `resource-api` - 添加 Nacos 依赖，接收 Gateway 转发的 Token

### 新增基础设施
- Nacos Server（Docker 部署）
- Redis（已有，用于 Session 存储）

## 风险和缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Reactive 学习曲线 | 中 | Gateway 配置相对简单，复杂逻辑在下游服务 |
| OAuth2 流程中断 | 高 | 保持 auth-server 不变，只改 Gateway 和客户端配置 |
| Session 管理复杂 | 中 | 使用 Redis，配置简单 |
| 版本不兼容 | 中 | 使用官方推荐的版本组合 |

## 成功标准

1. ✅ 所有服务注册到 Nacos 并能互相发现
2. ✅ 用户通过 Gateway 统一登录
3. ✅ Token 自动转发到下游服务
4. ✅ OAuth2 SSO 流程正常工作
5. ✅ 现有功能不受影响
