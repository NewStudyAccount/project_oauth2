## Why

当前项目中的 `app-springboot` 和 `app-vue` 作为外部系统示例，但其接入方式与实际生产场景不符。示例系统依赖 SSO 平台的基础设施（Nacos、Gateway），而真实的外部独立系统通常不会注册到 SSO 平台的服务网格中。需要改造示例，使其展示正确的外部系统接入方式，即直接与 auth-server 交互验证 Token，不依赖 SSO 平台的基础设施。

## What Changes

- **app-springboot 改造**：
  - 移除 Nacos 依赖，成为独立部署的 Web 应用
  - 配置为 OAuth2 Client，直接与 auth-server 交互
  - 使用 OAuth2 Authorization Code Flow 获取 Token

- **app-vue 改造**：
  - 确保作为独立的前端示例运行
  - 展示如何直接与 auth-server 交互获取 Token

- **Gateway 配置调整**：
  - 移除 `app-springboot` 的路由配置（可选，因为外部系统不再通过 Gateway）

- **文档更新**：
  - 更新架构文档，说明两种接入方式
  - 明确 `app-vue` 和 `app-springboot` 是外部系统示例
  - 提供外部系统接入指南

## Capabilities

### New Capabilities

- `external-webapp-integration`: 外部 Web 应用直接接入 SSO 平台的能力，展示如何作为独立 OAuth2 Client 与 auth-server 交互
- `external-frontend-integration`: 外部前端系统直接接入 SSO 平台的能力，展示如何使用 OAuth2 PKCE 流程获取 Token

### Modified Capabilities

（无）

## Impact

- **代码变更**：
  - `app-springboot/pom.xml`：移除 Nacos 依赖
  - `app-springboot/src/main/java/.../config/SecurityConfig.java`：配置 OAuth2 Client
  - `app-springboot/src/main/resources/application.yml`：配置 OAuth2 Client
  - `gateway/src/main/resources/application.yml`：移除 app-springboot 路由
  - `app-vue/`：保持独立，不调用其他外部系统

- **依赖变更**：
  - `app-springboot` 移除 `spring-cloud-starter-alibaba-nacos-discovery`

- **系统影响**：
  - 外部系统不再依赖 Nacos 和 Gateway
  - 外部系统直接与 auth-server 交互获取 Token
  - 两个外部系统各自独立，互不依赖
