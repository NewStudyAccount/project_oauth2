# Spring Cloud 微服务 + Gateway 架构文档

## 1. 架构概览

### 1.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              客户端层                                    │
│                                                                         │
│    ┌───────────────┐         ┌───────────────┐                          │
│    │   app-vue     │         │  其他客户端    │                          │
│    │  (前端应用)   │         │  (浏览器等)   │                          │
│    │  port: 5173   │         │               │                          │
│    └───────────────┘         └───────────────┘                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                              网关层                                      │
│                                                                         │
│    ┌───────────────────────────────────────────────────────────────┐    │
│    │                    Spring Cloud Gateway                       │    │
│    │                    port: 8080                                 │    │
│    │                                                               │    │
│    │    ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │    │
│    │    │ OAuth2      │  │ Token       │  │ 路由        │         │    │
│    │    │ Client      │  │ Relay       │  │ Filter      │         │    │
│    │    │ (统一登录)   │  │ (Token中继) │  │ (请求转发)  │         │    │
│    │    └─────────────┘  └─────────────┘  └─────────────┘         │    │
│    └───────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            注册中心层                                    │
│                                                                         │
│    ┌───────────────────────────────────────────────────────────────┐    │
│    │                      Nacos Server                             │    │
│    │                      port: 8848                               │    │
│    │                                                               │    │
│    │    服务列表:                                                   │    │
│    │    ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │    │
│    │    │ gateway     │  │ auth-server │  │app-springboot│         │    │
│    │    │ :8080       │  │ :9000       │  │ :8082       │         │    │
│    │    └─────────────┘  └─────────────┘  └─────────────┘         │    │
│    │    ┌─────────────┐                                           │    │
│    │    │ resource-api│                                           │    │
│    │    │ :8083       │                                           │    │
│    │    └─────────────┘                                           │    │
│    └───────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            ▼                       ▼                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                              服务层                                      │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                    SSO 统一认证平台                               │    │
│  │                                                                  │    │
│  │  ┌─────────────────┐    ┌─────────────────┐                     │    │
│  │  │   auth-server   │    │   resource-api  │                     │    │
│  │  │   port: 9000    │    │   port: 8083    │                     │    │
│  │  │                 │    │                 │                     │    │
│  │  │  ┌───────────┐  │    │  ┌───────────┐  │                     │    │
│  │  │  │ OAuth2    │  │    │  │ Resource  │  │                     │    │
│  │  │  │ Server    │  │    │  │ Server    │  │                     │    │
│  │  │  │ (授权服务) │  │    │  │ (资源服务)│  │                     │    │
│  │  │  └───────────┘  │    │  └───────────┘  │                     │    │
│  │  │                 │    │                 │                     │    │
│  │  │  ┌───────────┐  │    │  ┌───────────┐  │                     │    │
│  │  │  │ Login     │  │    │  │ 业务逻辑  │  │                     │    │
│  │  │  │ Page      │  │    │  │           │  │                     │    │
│  │  │  └───────────┘  │    │  └───────────┘  │                     │    │
│  │  └─────────────────┘    └─────────────────┘                     │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                    外部业务系统（通过 SSO 集成）                   │    │
│  │                                                                  │    │
│  │  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────┐ │    │
│  │  │  app-springboot │    │   其他系统 A    │    │  其他系统 B │ │    │
│  │  │   port: 8082    │    │                 │    │             │ │    │
│  │  │                 │    │                 │    │             │ │    │
│  │  │  ┌───────────┐  │    │  ┌───────────┐  │    │ ┌─────────┐│ │    │
│  │  │  │ Resource  │  │    │  │ Resource  │  │    │ │Resource ││ │    │
│  │  │  │ Server    │  │    │  │ Server    │  │    │ │Server   ││ │    │
│  │  │  └───────────┘  │    │  └───────────┘  │    │ └─────────┘│ │    │
│  │  │                 │    │                 │    │             │ │    │
│  │  │  ┌───────────┐  │    │  ┌───────────┐  │    │ ┌─────────┐│ │    │
│  │  │  │ 独立业务  │  │    │  │ 独立业务  │  │    │ │独立业务 ││ │    │
│  │  │  │ 逻辑      │  │    │  │ 逻辑      │  │    │ │逻辑     ││ │    │
│  │  │  └───────────┘  │    │  └───────────┘  │    │ └─────────┘│ │    │
│  │  └─────────────────┘    └─────────────────┘    └─────────────┘ │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            数据层                                        │
│                                                                         │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐     │
│  │     MySQL       │    │     Redis       │    │     Nacos DB    │     │
│  │  (业务数据)      │    │  (Session存储)  │    │  (注册数据)     │     │
│  │  port: 3306     │    │  port: 6379     │    │  (内置)         │     │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 服务端口分配

| 服务 | 端口 | 说明 |
|------|------|------|
| Nacos Server | 8848 | 服务注册中心 |
| Gateway | 8080 | API 网关（统一入口） |
| auth-server | 9000 | OAuth2 授权服务 |
| app-springboot | 8082 | 业务应用服务 |
| resource-api | 8083 | 资源服务 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | Session 存储 |

---

## 2. 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.5 | 基础框架 |
| Spring Cloud | 2023.0.1 | 微服务框架 |
| Spring Cloud Gateway | 4.1.x | API 网关（WebFlux） |
| Spring Cloud Alibaba | 2023.0.1.0 | Nacos 集成 |
| Nacos | 2.3.x | 服务注册中心 |
| Spring Security | 6.2.4 | 安全框架 |
| Spring Authorization Server | 1.2.4 | OAuth2 授权服务 |
| Redis | - | Session 存储 |

---

## 3. 服务职责

### 3.1 Gateway (port: 8080)

**职责**：
- 统一入口，所有客户端请求通过 Gateway 访问
- OAuth2 Client，统一处理用户登录
- Token 中继，将获取的 Token 转发给下游服务
- 动态路由，根据路径转发到对应服务

**技术栈**：
- Spring Cloud Gateway (WebFlux - 响应式)
- OAuth2 Client (响应式版本)
- Spring Session Redis

**关键配置**：
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-server
          uri: lb://auth-server
          predicates:
            - Path=/auth/**
          filters:
            - StripPrefix=1
        - id: app-springboot
          uri: lb://app-springboot
          predicates:
            - Path=/app/**
          filters:
            - StripPrefix=1
        - id: resource-api
          uri: lb://resource-api
          predicates:
            - Path=/api/**
          filters:
            - StripPrefix=1
```

### 3.2 auth-server (port: 9000)

**职责**：
- OAuth2 授权服务，处理用户认证和授权
- 颁发 Access Token、Refresh Token、ID Token
- 提供登录页面
- 管理 OAuth2 客户端注册

**技术栈**：
- Spring Authorization Server
- Spring Security
- Spring MVC (Servlet)
- MyBatis-Plus + MySQL

### 3.3 app-springboot (port: 8082) - 外部业务系统

**性质**：独立的业务系统，通过 OAuth2 SSO 集成到统一认证平台

**职责**：
- 独立的业务应用，不属于 SSO 平台核心
- 作为 Resource Server，验证 Gateway 转发的 Token
- 提供独立的业务 API
- 通过 SSO 实现单点登录

**技术栈**：
- Spring Security OAuth2 Resource Server
- Spring MVC (Servlet)
- JWT Token 验证

**集成方式**：
- 注册到 Nacos，通过 Gateway 路由
- 使用 SSO 平台颁发的 Token 进行认证
- 不需要自己实现登录逻辑

### 3.4 resource-api (port: 8083)

**职责**：
- 资源服务
- Resource Server，验证 Gateway 转发的 Token
- 提供资源 API

**技术栈**：
- Spring Security OAuth2 Resource Server
- Spring MVC (Servlet)
- JWT Token 验证

---

## 4. OAuth2 统一登录流程

### 4.1 流程图

```
┌─────────┐     ┌─────────┐     ┌─────────────┐     ┌─────────────┐
│ Browser │     │ Gateway │     │ auth-server │     │ 下游服务    │
└────┬────┘     └────┬────┘     └──────┬──────┘     └──────┬──────┘
     │               │                 │                   │
     │ 1. GET /app   │                 │                   │
     │──────────────▶│                 │                   │
     │               │                 │                   │
     │               │ 2. 检查 Session  │                   │
     │               │    未登录       │                   │
     │               │                 │                   │
     │ 3. 302        │                 │                   │
     │◀──────────────│                 │                   │
     │ 重定向到       │                 │                   │
     │ /oauth2/authorize               │                   │
     │               │                 │                   │
     │ 4. GET /oauth2/authorize        │                   │
     │────────────────────────────────▶│                   │
     │               │                 │                   │
     │ 5. 返回登录页面│                 │                   │
     │◀────────────────────────────────│                   │
     │               │                 │                   │
     │ 6. POST /login│                 │                   │
     │────────────────────────────────▶│                   │
     │               │                 │                   │
     │ 7. 返回 code  │                 │                   │
     │◀────────────────────────────────│                   │
     │ 重定向到       │                 │                   │
     │ Gateway       │                 │                   │
     │               │                 │                   │
     │ 8. GET /login/oauth2/code/gateway-client           │
     │──────────────▶│                 │                   │
     │               │                 │                   │
     │               │ 9. 用 code 换 token                 │
     │               │────────────────▶│                   │
     │               │                 │                   │
     │               │ 10. 返回 token  │                   │
     │               │◀────────────────│                   │
     │               │                 │                   │
     │               │ 11. 存储 token 到 Session            │
     │               │    (Redis)      │                   │
     │               │                 │                   │
     │ 12. 重定向到 /app                │                   │
     │◀──────────────│                 │                   │
     │               │                 │                   │
     │ 13. GET /app  │                 │                   │
     │──────────────▶│                 │                   │
     │               │                 │                   │
     │               │ 14. 从 Session 获取 token            │
     │               │    添加 Authorization: Bearer <token>│
     │               │────────────────────────────────────▶│
     │               │                 │                   │
     │               │ 15. 返回响应    │                   │
     │               │◀────────────────────────────────────│
     │               │                 │                   │
     │ 16. 返回页面   │                 │                   │
     │◀──────────────│                 │                   │
     │               │                 │                   │
```

### 4.2 关键步骤说明

1. **用户访问**：浏览器访问 `http://gateway.local:8080/app/`
2. **检查登录**：Gateway 检查 Session，发现用户未登录
3. **重定向**：Gateway 返回 302，重定向到 auth-server 的授权端点
4. **授权请求**：浏览器访问 `http://auth.local:9000/oauth2/authorize`
5. **登录页面**：auth-server 返回登录页面
6. **用户登录**：用户提交登录表单
7. **返回 code**：auth-server 返回授权码，重定向到 Gateway
8. **回调处理**：Gateway 接收授权码
9. **换 token**：Gateway 用授权码向 auth-server 请求 token
10. **获取 token**：auth-server 返回 access_token、refresh_token、id_token
11. **存储 session**：Gateway 将 token 存储到 Redis Session
12. **重定向**：Gateway 重定向用户到原始请求的页面
13. **再次访问**：浏览器再次访问 `/app/`
14. **Token 中继**：Gateway 从 Session 获取 token，添加到请求头
15. **转发请求**：Gateway 将请求转发给下游服务
16. **返回响应**：下游服务返回响应，Gateway 返回给浏览器

---

## 5. 服务注册与发现

### 5.1 Nacos 服务注册

所有服务启动时自动注册到 Nacos：

```
┌─────────────────────────────────────────────────────────────┐
│                        Nacos Server                         │
│                        port: 8848                           │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  服务列表                                            │    │
│  │                                                     │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │    │
│  │  │ gateway     │  │ auth-server │  │app-springboot│ │    │
│  │  │ 127.0.0.1   │  │ 127.0.0.1   │  │ 127.0.0.1   │ │    │
│  │  │ :8080       │  │ :9000       │  │ :8082       │ │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘ │    │
│  │  ┌─────────────┐                                   │    │
│  │  │ resource-api│                                   │    │
│  │  │ 127.0.0.1   │                                   │    │
│  │  │ :8083       │                                   │    │
│  │  └─────────────┘                                   │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 服务发现配置

每个服务的 `application.yml` 都包含：

```yaml
spring:
  application:
    name: <服务名>
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
```

### 5.3 动态路由

Gateway 使用 `lb://<服务名>` 进行动态路由：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-server
          uri: lb://auth-server  # 使用服务名，从 Nacos 获取地址
          predicates:
            - Path=/auth/**
```

---

## 6. Token 中继机制

### 6.1 Token 中继流程

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Gateway   │     │  Redis      │     │  下游服务   │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       │ 1. 从 Session 获取│                   │
       │    OAuth2 Token   │                   │
       │──────────────────▶│                   │
       │                   │                   │
       │ 2. 返回 Token     │                   │
       │◀──────────────────│                   │
       │                   │                   │
       │ 3. 添加到请求头   │                   │
       │    Authorization: │                   │
       │    Bearer <token> │                   │
       │──────────────────────────────────────▶│
       │                   │                   │
       │ 4. 下游服务验证 Token                 │
       │                   │                   │
       │ 5. 返回响应       │                   │
       │◀──────────────────────────────────────│
       │                   │                   │
```

### 6.2 Token 中继过滤器

```java
@Component
public class TokenRelayGlobalFilter implements GlobalFilter, Ordered {

    private final ReactiveOAuth2AuthorizedClientService authorizedClientService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof OAuth2AuthenticationToken)
                .cast(OAuth2AuthenticationToken.class)
                .flatMap(this::loadAccessToken)
                .map(OAuth2AccessToken::getTokenValue)
                .map(tokenValue -> addAuthorizationHeader(exchange, tokenValue))
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    private Mono<OAuth2AccessToken> loadAccessToken(OAuth2AuthenticationToken authentication) {
        String clientRegistrationId = authentication.getAuthorizedClientRegistrationId();
        String principalName = authentication.getName();
        return authorizedClientService.loadAuthorizedClient(clientRegistrationId, principalName)
                .map(OAuth2AuthorizedClient::getAccessToken);
    }

    private ServerWebExchange addAuthorizationHeader(ServerWebExchange exchange, String tokenValue) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("Authorization", "Bearer " + tokenValue)
                .build();
        return exchange.mutate().request(request).build();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
```

---

## 7. 验证步骤

### 7.1 前置条件

1. **hosts 文件配置**

   将以下内容添加到 `C:\Windows\System32\drivers\etc\hosts`：

   ```
   127.0.0.1  auth.local
   127.0.0.1  client.a.local
   127.0.0.1  gateway.local
   ```

2. **启动 Nacos Server**

   ```bash
   docker run -d --name nacos \
     -e MODE=standalone \
     -e JVM_XMS=256m \
     -e JVM_XMX=256m \
     -p 8848:8848 \
     -p 9848:9848 \
     nacos/nacos-server:v2.3.0
   ```

3. **确认 Redis 可用**

   ```bash
   redis-cli ping
   # 应返回 PONG
   ```

### 7.2 启动服务

按以下顺序启动服务：

1. **auth-server** (port 9000)
2. **app-springboot** (port 8082)
3. **resource-api** (port 8083)
4. **gateway** (port 8080)

### 7.3 验证服务注册

1. 访问 Nacos 控制台：http://127.0.0.1:8848/nacos
2. 使用默认账号登录：nacos / nacos
3. 查看服务列表，应看到 4 个服务：
   - gateway
   - auth-server
   - app-springboot
   - resource-api

### 7.4 验证 Gateway 路由

1. **访问 auth-server 通过 Gateway**

   ```bash
   # 应该能看到 auth-server 的响应
   curl http://gateway.local:8080/auth/
   ```

2. **访问 app-springboot 通过 Gateway**

   ```bash
   # 应该重定向到登录页面
   curl -v http://gateway.local:8080/app/
   ```

### 7.5 验证统一登录流程

1. **打开浏览器**，访问 http://gateway.local:8080/app/

2. **应重定向到登录页面**：
   - URL 变为 http://auth.local:9000/login
   - 显示登录表单

3. **输入用户名密码**：
   - 用户名：admin
   - 密码：（你的密码）

4. **登录成功后**：
   - 应重定向回 http://gateway.local:8080/app/
   - 应能看到 app-springboot 的页面

5. **再次访问**：
   - 直接访问 http://gateway.local:8080/app/
   - 应无需重新登录（Session 有效）

6. **访问其他服务**：
   - 访问 http://gateway.local:8080/api/
   - 应能访问 resource-api 的资源

### 7.6 验证 Token 中继

1. **在下游服务添加日志**

   在 `app-springboot` 的 Controller 中添加：

   ```java
   @GetMapping("/api/test")
   public Map<String, Object> test(@AuthenticationPrincipal Jwt jwt) {
       Map<String, Object> result = new HashMap<>();
       result.put("sub", jwt.getSubject());
       result.put("claims", jwt.getClaims());
       return result;
   }
   ```

2. **访问测试接口**

   ```bash
   # 通过 Gateway 访问
   curl http://gateway.local:8080/app/api/test
   ```

3. **验证响应**

   应返回 JWT Token 的 claims，包含：
   - `sub`：用户名
   - `username`：用户名
   - `nickname`：昵称
   - `email`：邮箱

---

## 8. 故障排查

### 8.1 Gateway 启动失败

**问题**：`The Issuer "xxx" provided in the configuration metadata did not match the requested issuer "xxx"`

**原因**：auth-server 的 issuer 配置与 Gateway 配置不一致

**解决**：确保两者使用相同的 issuer URI

### 8.2 下游服务启动失败

**问题**：`NoClassDefFoundError: OAuth2LoginAuthenticationFilter`

**原因**：SecurityConfig 中使用了 `.oauth2Login()`，但已移除 `spring-boot-starter-oauth2-client`

**解决**：更新 SecurityConfig，使用 `.oauth2ResourceServer()` 替代

### 8.3 服务注册失败

**问题**：服务无法注册到 Nacos

**原因**：Nacos Server 未启动或地址配置错误

**解决**：
1. 确认 Nacos Server 已启动
2. 检查 `spring.cloud.nacos.discovery.server-addr` 配置

### 8.4 Token 验证失败

**问题**：下游服务返回 401 Unauthorized

**原因**：JWT Token 无效或 issuer 不匹配

**解决**：
1. 检查 Token 是否正确传递
2. 检查下游服务的 `issuer-uri` 配置

---

## 9. 目录结构

```
project_oauth2/
├── pom.xml                          # 父 POM（管理 Spring Cloud 版本）
├── gateway/                         # API 网关
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/gateway/
│       │   ├── GatewayApplication.java
│       │   ├── config/
│       │   │   └── SecurityConfig.java
│       │   └── filter/
│       │       └── TokenRelayGlobalFilter.java
│       └── resources/
│           └── application.yml
├── auth-server/                     # OAuth2 授权服务
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/authserver/
│       └── resources/
│           └── application.yml
├── app-springboot/                  # 业务应用服务
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/appspringboot/
│       └── resources/
│           └── application.yml
├── resource-api/                    # 资源服务
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/resourceapi/
│       └── resources/
│           └── application.yml
├── app-vue/                         # 前端应用
└── docs/                            # 文档
    └── architecture-spring-cloud.md
```

---

## 10. 总结

### 10.1 架构优势

1. **统一入口**：所有请求通过 Gateway，便于管理和监控
2. **统一登录**：用户只需登录一次，即可访问所有服务
3. **服务解耦**：各服务独立部署，互不影响
4. **动态扩展**：新增服务只需注册到 Nacos，Gateway 自动路由
5. **SSO 集成**：外部业务系统通过 SSO 平台实现单点登录

### 10.2 SSO 平台 vs 外部业务系统

| 组件 | 类型 | 说明 |
|------|------|------|
| Gateway | SSO 平台核心 | 统一入口，OAuth2 Client |
| auth-server | SSO 平台核心 | OAuth2 授权服务 |
| resource-api | SSO 平台核心 | 平台资源服务 |
| app-springboot | 外部业务系统 | 独立业务，通过 SSO 集成 |
| 其他系统 A/B | 外部业务系统 | 独立业务，通过 SSO 集成 |

### 10.3 外部业务系统集成指南

**集成步骤**：

1. **添加依赖**

   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
   </dependency>
   <dependency>
       <groupId>com.alibaba.cloud</groupId>
       <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
   </dependency>
   ```

2. **配置 application.yml**

   ```yaml
   spring:
     application:
       name: your-app-name
     cloud:
       nacos:
         discovery:
           server-addr: ${NACOS_ADDR:127.0.0.1:8848}
     security:
       oauth2:
         resourceserver:
           jwt:
             issuer-uri: http://auth.local:9000
   ```

3. **添加 @EnableDiscoveryClient**

   ```java
   @SpringBootApplication
   @EnableDiscoveryClient
   public class YourApplication {
       // ...
   }
   ```

4. **配置 SecurityConfig**

   ```java
   @Configuration
   @EnableWebSecurity
   public class SecurityConfig {
       @Bean
       public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
           http
               .csrf(csrf -> csrf.disable())
               .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
               .authorizeHttpRequests(auth -> auth
                   .requestMatchers("/", "/error").permitAll()
                   .anyRequest().authenticated()
               )
               .sessionManagement(session -> session
                   .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
               );
           return http.build();
       }
   }
   ```

5. **在 Gateway 添加路由**

   ```yaml
   spring:
     cloud:
       gateway:
         routes:
           - id: your-app-name
             uri: lb://your-app-name
             predicates:
               - Path=/your-app/**
             filters:
               - StripPrefix=1
   ```

### 10.4 注意事项

1. **Gateway 使用 WebFlux**：不能引入 `spring-boot-starter-web`
2. **下游服务使用 Servlet**：可以正常使用 Spring MVC
3. **Session 存储在 Redis**：支持 Gateway 多实例部署
4. **Token 通过请求头传递**：下游服务验证 JWT Token
5. **外部系统只需配置 Resource Server**：不需要实现登录逻辑

### 10.5 后续扩展

1. **负载均衡**：同一服务部署多实例，Gateway 自动负载均衡
2. **熔断降级**：集成 Sentinel 实现熔断降级
3. **配置中心**：使用 Nacos 作为配置中心
4. **链路追踪**：集成 SkyWalking 实现链路追踪
5. **多租户支持**：支持多个业务系统独立部署
