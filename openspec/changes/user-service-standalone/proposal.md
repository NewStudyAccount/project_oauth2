# user-service 独立部署（移除 Nacos 依赖）

## Why

当前 `user-service` 模块在 `pom.xml` 中引入了 `spring-cloud-starter-alibaba-nacos-discovery` 依赖，并在 `application.yml` 中配置了 Nacos 服务发现。但根据架构设计，`user-service` 应该是一个独立部署的基础服务，不依赖 Nacos 服务注册中心。

`auth-center` 和 `client-app` 通过 Feign 直连方式（硬编码 `url`）调用 `user-service`，无需服务发现。保留 Nacos 依赖会带来以下问题：
- 增加不必要的依赖复杂度（Nacos 客户端、心跳机制等）
- 如果 Nacos 不可用，user-service 启动可能报错或产生大量错误日志
- 与其他独立基础设施（MySQL、Redis）的部署方式不一致

## What Changes

- 从 `platform/user-service/pom.xml` 中移除 `spring-cloud-starter-alibaba-nacos-discovery` 依赖
- 从 `platform/user-service/src/main/resources/application.yml` 中移除 `spring.cloud.nacos.discovery` 配置块
- 更新 `openspec/changes/extract-user-center-service/design.md` 中 Decision 2 的描述，明确 user-service 不走 Nacos

## Capabilities

### New Capabilities

（无新增）

### Modified Capabilities

- `user-center-service`: 移除 Nacos 服务注册，作为独立 Spring Boot 应用部署

## Impact

- **后端 API 兼容性**: 无变化，API 接口不变
- **前端**: 无影响
- **数据库**: 无影响
- **依赖**: 移除 `spring-cloud-starter-alibaba-nacos-discovery`，减少约 10 个传递依赖
- **代码变更**: 仅 pom.xml 和 application.yml，无 Java 代码变更
- **配置变更**: 移除 Nacos 连接配置
