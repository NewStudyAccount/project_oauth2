# 设计文档：Spring Cloud Gateway + Nacos 集成（方案 B：统一登录）

## 架构设计

### 1. 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        客户端层                                  │
│  ┌─────────────┐                                                │
│  │  app-vue    │                                                │
│  │  (Frontend) │                                                │
│  └─────────────┘                                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        网关层                                    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              Spring Cloud Gateway                       │    │
│  │              (OAuth2 Client + 路由)                      │    │
│  │                                                         │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │    │
│  │  │ OAuth2      │  │ Token       │  │ 路由        │     │    │
│  │  │ Login       │  │ Relay       │  │ Filter      │     │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘     │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        注册中心层                                │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    Nacos Server                         │    │
│  │                    port: 8848                           │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
            ┌─────────────────┼─────────────────┐
            ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                        服务层                                    │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐          │
│  │ auth-server │    │app-springboot│    │ resource-api │          │
│  │  (OAuth2)   │    │  (下游服务)  │    │  (下游服务)  │          │
│  │  port:9000  │    │  port:8082   │    │  port:8083   │          │
│  └─────────────┘    └─────────────┘    └─────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### 2. 模块结构

```
project_oauth2/
├── pom.xml (新增，父 POM)
├── gateway/                    # 新增：网关模块
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/example/gateway/
│           │   ├── GatewayApplication.java
│           │   ├── config/
│           │   │   └── SecurityConfig.java
│           │   └── filter/
│           │       └── TokenRelayGlobalFilter.java
│           └── resources/
│               └── application.yml
├── auth-server/                # 修改：添加 Nacos 依赖
├── app-springboot/             # 修改：移除 OAuth2 Client，添加 Nacos
└── resource-api/               # 修改：添加 Nacos 依赖
```

### 3. Gateway 模块设计

#### 3.1 依赖配置 (pom.xml)

```xml
<dependencies>
    <!-- Spring Cloud Gateway (基于 WebFlux，不能引入 spring-boot-starter-web) -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

    <!-- Nacos 服务发现 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

    <!-- OAuth2 Client (响应式版本) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>

    <!-- Spring Session Redis (分布式 Session) -->
    <dependency>
        <groupId>org.springframework.session</groupId>
        <artifactId>spring-session-data-redis</artifactId>
    </dependency>

    <!-- Redis Reactive (Session 存储) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
    </dependency>

    <!-- Actuator -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

**注意**：不能引入 `spring-boot-starter-web`，会与 WebFlux 冲突。

#### 3.2 配置文件 (application.yml)

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway

  # Nacos 配置
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}

    # Gateway 路由配置
    gateway:
      routes:
        # auth-server 路由
        - id: auth-server
          uri: lb://auth-server
          predicates:
            - Path=/auth/**
          filters:
            - StripPrefix=1

        # app-springboot 路由
        - id: app-springboot
          uri: lb://app-springboot
          predicates:
            - Path=/app/**
          filters:
            - StripPrefix=1

        # resource-api 路由
        - id: resource-api
          uri: lb://resource-api
          predicates:
            - Path=/api/**
          filters:
            - StripPrefix=1

      # 默认过滤器
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin

  # OAuth2 Client 配置
  security:
    oauth2:
      client:
        registration:
          gateway-client:
            client-id: gateway-app
            client-secret: gateway-secret
            scope: openid,profile,email
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            client-name: Gateway Client
        provider:
          gateway-client:
            issuer-uri: http://auth.local:9000
            authorization-uri: http://auth.local:9000/oauth2/authorize
            token-uri: http://auth.local:9000/oauth2/token
            user-info-uri: http://auth.local:9000/userinfo
            user-name-attribute: sub

  # Session 配置 (Redis)
  session:
    store-type: redis
    timeout: 30m

  # Redis 配置
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
```

#### 3.3 启动类 (GatewayApplication.java)

```java
package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

#### 3.4 安全配置 (SecurityConfig.java)

```java
package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity  // 注意：响应式版本
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            // CSRF 配置（Gateway 通常禁用 CSRF）
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            // 授权配置
            .authorizeExchange(exchanges -> exchanges
                // 公开路径
                .pathMatchers(
                    "/",
                    "/login",
                    "/oauth2/**",
                    "/auth/**",
                    "/error",
                    "/actuator/**",
                    "/css/**",
                    "/js/**",
                    "/images/**"
                ).permitAll()
                // 其他路径需要认证
                .anyExchange().authenticated()
            )

            // OAuth2 登录配置
            .oauth2Login(oauth2 -> oauth2
                .authenticationSuccessHandler(
                    new RedirectServerAuthenticationSuccessHandler("/app"))
                .authenticationFailureHandler(
                    new RedirectServerAuthenticationFailureHandler("/error?login_failed"))
            )

            // OAuth2 客户端配置（用于 Token 中继）
            .oauth2Client(oauth2 -> {})

            // 头部配置
            .headers(headers -> headers
                .frameOptions(frame -> frame.mode(
                    ServerHttpSecurity.HeaderSpec.FrameOptionsSpec.Mode.SAMEORIGIN))
            );

        return http.build();
    }
}
```

#### 3.5 Token 中继过滤器 (TokenRelayGlobalFilter.java)

```java
package com.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TokenRelayGlobalFilter implements GlobalFilter, Ordered {

    private final ReactiveOAuth2AuthorizedClientService authorizedClientService;

    public TokenRelayGlobalFilter(ReactiveOAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .filter(authentication -> authentication instanceof OAuth2AuthenticationToken)
                .cast(OAuth2AuthenticationToken.class)
                .flatMap(authentication -> {
                    String clientRegistrationId = authentication.getAuthorizedClientRegistrationId();
                    return authorizedClientService.loadAuthorizedClient(
                        clientRegistrationId, authentication.getName());
                })
                .map(OAuth2AuthorizedClient::getAccessToken)
                .map(accessToken -> {
                    // 将 Token 添加到请求头
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .header("Authorization", "Bearer " + accessToken.getTokenValue())
                            .build();
                    return exchange.mutate().request(request).build();
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return -100; // 高优先级，确保在路由之前执行
    }
}
```

### 4. 下游服务改造

#### 4.1 auth-server

**改动**：
- 添加 Nacos 依赖
- 添加 Nacos 配置
- 添加 `@EnableDiscoveryClient` 注解
- 注册新的 OAuth2 Client（`gateway-app`）

**不动**：
- 保持现有的 OAuth2 授权服务逻辑
- 保持现有的登录页面
- 保持现有的 Token 端点

#### 4.2 app-springboot

**改动**：
- 添加 Nacos 依赖
- 添加 Nacos 配置
- 移除 OAuth2 Client 配置（由 Gateway 处理登录）
- 改为从请求头获取 Token

**新角色**：
- 从 OAuth2 Client 变为 Resource Server
- 验证 Gateway 转发的 Token

#### 4.3 resource-api

**改动**：
- 添加 Nacos 依赖
- 添加 Nacos 配置
- 配置为 Resource Server，验证 Token

### 5. 路由规则

| 路径模式 | 目标服务 | 说明 |
|---------|---------|------|
| `/auth/**` | auth-server | OAuth2 认证服务（登录、授权、Token） |
| `/app/**` | app-springboot | 客户端应用 |
| `/api/**` | resource-api | 资源服务 |

**路由示例**：
```
gateway.local:8080/auth/oauth2/authorize  →  auth-server:9000/oauth2/authorize
gateway.local:8080/auth/login              →  auth-server:9000/login
gateway.local:8080/auth/oauth2/token       →  auth-server:9000/oauth2/token
gateway.local:8080/app/profile             →  app-springboot:8082/profile
gateway.local:8080/api/resources           →  resource-api:8083/resources
```

### 6. Session 管理

使用 Redis 存储 Session：

```
┌─────────────┐        ┌─────────────┐        ┌─────────────┐
│   Gateway   │───────▶│    Redis    │◀───────│  其他实例   │
│  (实例1)    │        │  (Session)  │        │  (实例2)    │
└─────────────┘        └─────────────┘        └─────────────┘
```

**优点**：
- 支持 Gateway 多实例部署
- Session 持久化
- 自动过期清理

### 7. 安全配置对比

| 配置项 | Servlet 版本 | Reactive 版本 |
|--------|-------------|---------------|
| 注解 | `@EnableWebSecurity` | `@EnableWebFluxSecurity` |
| 配置类 | `WebSecurityConfigurerAdapter` | `SecurityWebFilterChain` |
| HTTP 安全 | `HttpSecurity` | `ServerHttpSecurity` |
| 授权配置 | `.authorizeRequests()` | `.authorizeExchange()` |
| 路径匹配 | `.antMatchers()` | `.pathMatchers()` |
| OAuth2 登录 | `.oauth2Login()` | `.oauth2Login()` |

## 关键设计决策

### 决策 1：Gateway 作为 OAuth2 Client

**选择**：Gateway 统一处理 OAuth2 登录

**理由**：
- 用户体验好，只需登录一次
- 下游服务无需重复实现登录逻辑
- 集中管理认证

### 决策 2：使用 Redis 存储 Session

**选择**：Redis 作为 Session 存储

**理由**：
- 支持分布式部署
- 已有 Redis 基础设施
- 性能好，可靠性高

### 决策 3：Token 中继方式

**选择**：通过 GlobalFilter 将 Token 添加到请求头

**理由**：
- 标准的 Bearer Token 方式
- 下游服务容易验证
- 符合 OAuth2 规范

### 决策 4：路径前缀设计

**选择**：使用 `/auth/`, `/app/`, `/api/` 前缀

**理由**：
- 清晰区分不同服务
- 便于路由配置
- 支持未来扩展

## 文件变更清单

### 新增文件
1. `pom.xml` - 父 POM（管理 Spring Cloud 版本）
2. `gateway/pom.xml` - Gateway 模块 POM
3. `gateway/src/main/java/.../GatewayApplication.java` - 启动类
4. `gateway/src/main/java/.../config/SecurityConfig.java` - 安全配置
5. `gateway/src/main/java/.../filter/TokenRelayGlobalFilter.java` - Token 中继
6. `gateway/src/main/resources/application.yml` - 配置文件

### 修改文件
1. `auth-server/pom.xml` - 添加 Nacos 依赖
2. `auth-server/src/main/resources/application.yml` - 添加 Nacos 配置
3. `auth-server/src/main/java/.../AuthServerApplication.java` - 添加注解
4. `auth-server/src/main/java/.../config/AuthorizationServerConfig.java` - 注册 Gateway Client
5. `app-springboot/pom.xml` - 添加 Nacos 依赖，移除 OAuth2 Client
6. `app-springboot/src/main/resources/application.yml` - 改为 Resource Server
7. `resource-api/pom.xml` - 添加 Nacos 依赖
8. `resource-api/src/main/resources/application.yml` - 添加 Nacos 配置
9. `hosts.txt` - 添加 gateway.local
