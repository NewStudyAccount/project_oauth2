## Purpose

user-service 作为独立 Spring Boot 应用部署，不依赖任何服务注册中心。

## MODIFIED Requirements

### Requirement: 独立部署

user-service SHALL 作为纯 Spring Boot 应用运行，不注册到 Nacos 或其他服务注册中心。

#### Scenario: 启动不依赖 Nacos
- **WHEN** Nacos 服务不可用
- **THEN** user-service 正常启动，无连接 Nacos 的错误日志

#### Scenario: 无服务发现依赖
- **WHEN** 检查 user-service 的依赖列表
- **THEN** 不包含 `spring-cloud-starter-alibaba-nacos-discovery` 或任何服务发现客户端

### Requirement: 直连调用

auth-center 和 client-app SHALL 通过 Feign 的 `url` 属性直连 user-service，不经过服务发现。

#### Scenario: Feign 直连配置
- **WHEN** auth-center 或 client-app 调用 user-service
- **THEN** 使用 `${user-service.url:http://localhost:8081}` 硬编码地址，不查询 Nacos 服务列表
