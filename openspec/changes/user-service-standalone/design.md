# user-service 独立部署设计

## Context

`user-service` 是用户中心微服务，提供用户信息查询、注册、管理 REST API。在 `extract-user-center-service` change 中创建该模块时，错误地引入了 Nacos 服务发现依赖。

实际上 `auth-center` 和 `client-app` 通过 Feign 的 `url` 属性直连 `user-service`（`http://localhost:8081`），不经过 Nacos 服务发现。`user-service` 应与 MySQL、Redis 等基础设施一样，作为独立服务部署。

## Goals / Non-Goals

**Goals:**
1. 移除 `user-service` 的 Nacos 依赖和配置
2. 保持 `user-service` 作为纯 Spring Boot 应用独立运行

**Non-Goals:**
- 不改变 `auth-center` 和 `client-app` 的 Feign 调用方式（已是直连）
- 不改变 `user-service` 的 API 接口

## Decisions

### Decision 1: user-service 不使用服务发现

**选择**: user-service 作为独立 Spring Boot 应用，不注册到任何服务注册中心

**替代方案**:
- A: 注册到 Nacos，消费者通过服务名调用
- B: 使用 Consul/Eureka 等其他注册中心

**理由**: user-service 是基础服务，部署实例固定（通常单实例），直连方式更简单可靠。消费者通过配置文件指定 `user-service.url`，部署时按环境调整即可。

## Risks / Trade-offs

- **[权衡] 无法自动负载均衡**: 直连方式不支持客户端负载均衡 → user-service 通常单实例部署，无需负载均衡；如需扩展可通过 Nginx 反向代理实现
- **[权衡] 地址变更需改配置**: 服务地址硬编码在配置文件中 → 通过环境变量 `${USER_SERVICE_URL:localhost:8081}` 可覆盖默认值

## Open Questions

无
