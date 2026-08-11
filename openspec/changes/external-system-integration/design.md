## Context

当前项目中，`app-springboot` 和 `app-vue` 作为外部系统示例，但其接入方式与实际生产场景不符。示例系统依赖 SSO 平台的基础设施（Nacos、Gateway），而真实的外部独立系统通常不会注册到 SSO 平台的服务网格中。需要改造示例，使其展示正确的外部系统接入方式。

## Goals / Non-Goals

**Goals:**

- 改造 `app-springboot` 为独立的 Resource Server，不依赖 Nacos 和 Gateway
- 确保 `app-vue` 作为独立的前端示例运行
- 提供清晰的外部系统接入示例，展示两种接入方式
- 更新文档，说明外部系统接入 SSO 平台的最佳实践

**Non-Goals:**

- 不修改 SSO 平台核心服务（gateway、auth-server、resource-api）
- 不实现完整的业务逻辑，仅提供示例
- 不处理外部系统的部署和运维问题

## Decisions

### Decision 1: 外部 Web 应用接入方式

**选择**：配置为 OAuth2 Client，直接与 auth-server 交互

**理由**：
- 外部系统通常不在 SSO 平台的服务网格内
- 作为传统 Web 应用，需要自己处理用户认证
- 使用 OAuth2 Authorization Code Flow 获取 Token

**替代方案**：
- 通过 Gateway 代理：适用于内部微服务，不适用于外部独立系统

### Decision 2: 前端 Token 获取方式

**选择**：使用 OAuth2 Authorization Code Flow with PKCE

**理由**：
- PKCE 提供额外的安全性，防止授权码拦截
- 适用于公开客户端（SPA）
- 是 OAuth2.0 推荐的前端认证方式

**替代方案**：
- Implicit Flow：已不推荐，安全性较低

### Decision 3: CORS 配置

**选择**：在 `app-springboot` 中配置 CORS

**理由**：
- 作为示例，展示如何配置 CORS 以便外部前端调用
- 实际生产中，外部后端需要配置 CORS 允许跨域请求

### Decision 4: 示例 API 设计

**选择**：提供公开和受保护两种 API 端点

**理由**：
- 展示不同场景下的 API 访问控制
- 公开端点无需认证，受保护端点需要 Token
- 更全面地展示 Resource Server 的能力

## Risks / Trade-offs

### Risk 1: 示例代码可能被误用于生产

**风险**：开发者可能直接复制示例代码到生产环境

**缓解**：
- 在代码中添加注释，说明这是示例
- 在文档中强调生产环境需要额外的安全配置
- 不包含生产级的错误处理和日志

### Risk 2: Token 存储安全性

**风险**：前端存储 Token 可能存在安全风险

**缓解**：
- 在示例中使用 localStorage，简化实现
- 在文档中说明生产环境应考虑更安全的存储方式
- 强调 HTTPS 的重要性

### Risk 3: CORS 配置过于宽松

**风险**：示例中的 CORS 配置可能过于宽松

**缓解**：
- 在示例中使用特定的源，而不是通配符
- 在文档中说明生产环境应严格配置 CORS
- 提供 CORS 配置的最佳实践

## Migration Plan

### 部署步骤

1. 更新 `app-springboot` 代码和配置
2. 更新 Gateway 配置（移除 app-springboot 路由）
3. 更新文档
4. 测试示例功能

### 回滚策略

如果出现问题，可以：
1. 恢复 `app-springboot` 的 Nacos 依赖和配置
2. 恢复 Gateway 的路由配置

## Open Questions

（无）
