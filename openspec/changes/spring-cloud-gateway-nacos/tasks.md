# 任务清单：Spring Cloud Gateway + Nacos 集成（方案 B：统一登录）

## 阶段 1：基础设施准备

- [x] **任务 1.1：创建父 POM** - 创建统一的父 POM，管理 Spring Cloud 版本依赖。文件：`pom.xml`（项目根目录）。设置 parent 为 spring-boot-starter-parent 3.2.5，定义 Spring Cloud 2023.0.1 版本，定义 Spring Cloud Alibaba 2023.0.1.0 版本，配置 dependencyManagement，定义子模块：gateway, auth-server, app-springboot, resource-api。

- [ ] **任务 1.2：启动 Nacos Server** - 使用 Docker 启动 Nacos Server。命令：`docker run -d --name nacos -e MODE=standalone -e JVM_XMS=256m -e JVM_XMX=256m -p 8848:8848 -p 9848:9848 nacos/nacos-server:v2.3.0`。验证：访问 http://127.0.0.1:8848/nacos，使用默认账号 nacos/nacos 登录。

- [ ] **任务 1.3：确认 Redis 可用** - 确认 Redis 服务可用（用于 Session 存储）。验证：`redis-cli ping` 返回 PONG。

## 阶段 2：创建 Gateway 模块

- [x] **任务 2.1：创建 Gateway 模块结构** - 创建 gateway 模块目录和 POM 文件。目录结构：gateway/pom.xml, gateway/src/main/java/com/example/gateway/GatewayApplication.java, gateway/src/main/java/com/example/gateway/config/SecurityConfig.java, gateway/src/main/java/com/example/gateway/filter/TokenRelayGlobalFilter.java, gateway/src/main/resources/application.yml。

- [x] **任务 2.2：编写 Gateway POM** - 配置 Gateway 模块的依赖。依赖：spring-cloud-starter-gateway（WebFlux）, spring-cloud-starter-alibaba-nacos-discovery, spring-boot-starter-oauth2-client（响应式）, spring-session-data-redis, spring-boot-starter-data-redis-reactive, spring-boot-starter-actuator。注意：不能引入 spring-boot-starter-web。

- [x] **任务 2.3：编写 Gateway 启动类** - 创建 GatewayApplication.java。注解：@SpringBootApplication, @EnableDiscoveryClient。

- [x] **任务 2.4：编写 Gateway 配置** - 配置路由规则、OAuth2 Client、Session。配置内容：Nacos 地址 127.0.0.1:8848，服务名 gateway，端口 8080，OAuth2 Client gateway-app，Session Redis 存储，路由规则 /auth/** → auth-server, /app/** → app-springboot, /api/** → resource-api。

- [x] **任务 2.5：编写安全配置类** - 创建 SecurityConfig.java（响应式版本）。关键点：使用 @EnableWebFluxSecurity，使用 ServerHttpSecurity，配置 OAuth2 登录，配置公开路径。

- [x] **任务 2.6：编写 Token 中继过滤器** - 创建 TokenRelayGlobalFilter.java。功能：从 Session 获取 OAuth2 Token，添加到请求头 Authorization: Bearer <token>，转发给下游服务。

## 阶段 3：修改 auth-server

- [x] **任务 3.1：添加 Nacos 依赖** - 修改 auth-server/pom.xml，添加 Nacos Discovery 依赖。依赖：spring-cloud-starter-alibaba-nacos-discovery。

- [x] **任务 3.2：添加 Nacos 配置** - 修改 auth-server 的 application.yml。配置：spring.application.name: auth-server, spring.cloud.nacos.discovery.server-addr: ${NACOS_ADDR:127.0.0.1:8848}。

- [x] **任务 3.3：添加启动类注解** - 在 AuthServerApplication.java 添加 @EnableDiscoveryClient。

- [x] **任务 3.4：注册 Gateway OAuth2 Client** - 在 auth-server 注册新的 OAuth2 Client（gateway-app）。配置：client-id: gateway-app, client-secret: gateway-secret, redirect-uri: http://gateway.local:8080/login/oauth2/code/gateway-client。

## 阶段 4：修改 app-springboot

- [x] **任务 4.1：添加 Nacos 依赖** - 修改 app-springboot/pom.xml，添加 Nacos Discovery 依赖。

- [x] **任务 4.2：添加 Nacos 配置** - 修改 app-springboot 的 application.yml。配置：spring.application.name: app-springboot, spring.cloud.nacos.discovery.server-addr: ${NACOS_ADDR:127.0.0.1:8848}。

- [x] **任务 4.3：移除 OAuth2 Client 配置** - 移除 app-springboot 的 OAuth2 Client 配置（由 Gateway 处理登录）。修改：移除 spring.security.oauth2.client 配置，移除 spring-boot-starter-oauth2-client 依赖，改为 Resource Server 配置。

- [x] **任务 4.4：添加 Resource Server 配置** - 配置 app-springboot 为 Resource Server，验证 Gateway 转发的 Token。配置：spring.security.oauth2.resourceserver.jwt.issuer-uri: http://auth.local:9000。

- [x] **任务 4.5：添加启动类注解** - 添加 @EnableDiscoveryClient。

## 阶段 5：修改 resource-api

- [x] **任务 5.1：添加 Nacos 依赖** - 修改 resource-api/pom.xml，添加 Nacos Discovery 依赖。

- [x] **任务 5.2：添加 Nacos 配置** - 修改 resource-api 的 application.yml。

- [x] **任务 5.3：添加启动类注解** - 添加 @EnableDiscoveryClient。

## 阶段 6：配置适配

- [x] **任务 6.1：更新 hosts 文件** - 添加 gateway.local 域名解析。内容：127.0.0.1 gateway.local。

- [x] **任务 6.2：修改 auth-server 的 issuer 配置** - 将 AuthorizationServerSettings 的 issuer 改为 Gateway 地址。配置：issuer: http://gateway.local:8080/auth。

## 阶段 7：测试验证

- [ ] **任务 7.1：启动所有服务** - 按顺序启动所有服务。顺序：1. Nacos Server（已启动）, 2. Redis（已启动）, 3. auth-server, 4. app-springboot, 5. resource-api, 6. gateway。

- [ ] **任务 7.2：验证服务注册** - 检查 Nacos 控制台，确认所有服务已注册。验证：访问 http://127.0.0.1:8848/nacos，查看服务列表，应有 4 个服务：gateway, auth-server, app-springboot, resource-api。

- [ ] **任务 7.3：验证 Gateway 路由** - 测试 Gateway 路由是否正常工作。测试：访问 http://gateway.local:8080/auth/ 应能访问 auth-server，访问 http://gateway.local:8080/app/ 应重定向到登录。

- [ ] **任务 7.4：验证统一登录流程** - 测试完整的统一登录流程。测试步骤：1. 访问 http://gateway.local:8080/app/, 2. 应重定向到 auth-server 登录页面, 3. 登录后应能访问 app-springboot, 4. 再次访问 http://gateway.local:8080/app/ 应无需重新登录, 5. 访问 http://gateway.local:8080/api/ 应能访问 resource-api。

- [ ] **任务 7.5：验证 Token 中继** - 验证 Token 是否正确转发到下游服务。测试：在下游服务打印请求头，确认包含 Authorization: Bearer <token>，确认 Token 有效。
