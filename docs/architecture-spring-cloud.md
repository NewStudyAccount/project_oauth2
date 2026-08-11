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
│    │    │ gateway     │  │ auth-server │  │ resource-api│         │    │
│    │    │ :8080       │  │ :9000       │  │ :8083       │         │    │
│    │    └─────────────┘  └─────────────┘  └─────────────┘         │    │
│    │                                                               │    │
│    │    注: app-springboot 和 app-vue 是外部系统示例                │    │
│    │    不注册到 Nacos，直接与 auth-server 交互                    │    │
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
│  │                    外部系统示例（直接接入 SSO）                    │    │
│  │                                                                  │    │
│  │  ┌─────────────────┐    ┌─────────────────┐                     │    │
│  │  │  app-springboot │    │    app-vue       │                     │    │
│  │  │   port: 8082    │    │    port: 5173    │                     │    │
│  │  │                 │    │                  │                     │    │
│  │  │  ┌───────────┐  │    │  ┌───────────┐  │                     │    │
│  │  │  │ Resource  │  │    │  │ 前端 SPA  │  │                     │    │
│  │  │  │ Server    │  │    │  │ (PKCE)    │  │                     │    │
│  │  │  └───────────┘  │    │  └───────────┘  │                     │    │
│  │  │                 │    │                  │                     │    │
│  │  │  ┌───────────┐  │    │  ┌───────────┐  │                     │    │
│  │  │  │ REST API  │  │    │  │ Token 管理│  │                     │    │
│  │  │  │ 示例      │  │    │  │           │  │                     │    │
│  │  │  └───────────┘  │    │  └───────────┘  │                     │    │
│  │  └─────────────────┘    └─────────────────┘                     │    │
│  │                                                                  │    │
│  │  注: 外部系统直接与 auth-server 交互，不依赖 Gateway 和 Nacos    │    │
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
| Nacos Server | 8848 | 服务注册中心（SSO 平台核心） |
| Gateway | 8080 | API 网关（SSO 平台核心） |
| auth-server | 9000 | OAuth2 授权服务（SSO 平台核心） |
| resource-api | 8083 | 资源服务（SSO 平台核心） |
| app-springboot | 8082 | 外部 Web 应用示例（不注册到 Nacos） |
| app-vue | 5173 | 外部前端系统示例（不注册到 Nacos） |
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

### 3.3 app-springboot (port: 8082) - 外部 Web 应用示例

**性质**：外部独立 Web 应用的示例，展示如何直接接入 SSO 平台

**职责**：
- 作为独立的 OAuth2 Client，直接与 auth-server 交互获取 Token
- 提供传统的 Web 页面（Thymeleaf）
- 不依赖 SSO 平台的基础设施（Nacos、Gateway）

**技术栈**：
- Spring Security OAuth2 Client
- Spring MVC (Servlet)
- Thymeleaf 模板

**集成方式**：
- 直接与 auth-server 交互，使用 OAuth2 Authorization Code Flow 获取 Token
- 不注册到 Nacos，不通过 Gateway 路由
- 用户通过浏览器访问，完成登录后显示页面

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

SSO 平台核心服务启动时自动注册到 Nacos：

```
┌─────────────────────────────────────────────────────────────┐
│                        Nacos Server                         │
│                        port: 8848                           │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  服务列表（SSO 平台核心）                            │    │
│  │                                                     │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │    │
│  │  │ gateway     │  │ auth-server │  │ resource-api│ │    │
│  │  │ 127.0.0.1   │  │ 127.0.0.1   │  │ 127.0.0.1   │ │    │
│  │  │ :8080       │  │ :9000       │  │ :8083       │ │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘ │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  注: 外部系统（app-springboot、app-vue）不注册到 Nacos      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 服务发现配置

SSO 平台核心服务的 `application.yml` 包含 Nacos 配置：

```yaml
spring:
  application:
    name: <服务名>
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
```

外部系统（如 app-springboot）不需要配置 Nacos，直接使用 auth-server 的 JWKS 端点验证 Token。

### 5.3 动态路由

Gateway 使用 `lb://<服务名>` 进行动态路由，只路由 SSO 平台核心服务：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-server
          uri: lb://auth-server  # 使用服务名，从 Nacos 获取地址
          predicates:
            - Path=/auth/**
        - id: resource-api
          uri: lb://resource-api
          predicates:
            - Path=/api/**
          filters:
            - StripPrefix=1
```

注: 外部系统（如 app-springboot）不通过 Gateway 路由，前端直接调用。

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

**SSO 平台核心服务**：
1. **auth-server** (port 9000)
2. **resource-api** (port 8083)
3. **gateway** (port 8080)

**外部系统示例**（可选，独立启动）：
4. **app-springboot** (port 8082) - 外部后端系统示例
5. **app-vue** (port 5173) - 外部前端系统示例

### 7.3 验证服务注册

1. 访问 Nacos 控制台：http://127.0.0.1:8848/nacos
2. 使用默认账号登录：nacos / nacos
3. 查看服务列表，应看到 3 个 SSO 平台核心服务：
   - gateway
   - auth-server
   - resource-api
4. 外部系统（app-springboot、app-vue）不应出现在服务列表中

### 7.4 验证 Gateway 路由

1. **访问 auth-server 通过 Gateway**

   ```bash
   # 应该能看到 auth-server 的响应
   curl http://gateway.local:8080/auth/
   ```

2. **访问 resource-api 通过 Gateway**

   ```bash
   # 应该需要认证
   curl -v http://gateway.local:8080/api/
   ```

### 7.5 验证外部系统直接接入

1. **启动外部后端系统示例**

   ```bash
   cd app-springboot
   mvn spring-boot:run
   ```

2. **访问公开 API**

   ```bash
   # 应该返回公开数据，不需要 Token
   curl http://localhost:8082/api/public
   ```

3. **访问受保护 API**（需要先获取 Token）

   ```bash
   # 首先获取 Token（通过 app-vue 或直接调用 auth-server）
   # 然后使用 Token 访问
   curl -H "Authorization: Bearer <token>" http://localhost:8082/api/protected
   ```

4. **验证 CORS 配置**

   ```bash
   # 从不同源发起请求，检查 CORS 头
   curl -H "Origin: http://client.a.local:5173" -H "Access-Control-Request-Method: GET" -X OPTIONS http://localhost:8082/api/public
   ```

### 7.6 验证外部前端系统示例

1. **启动外部前端系统示例**

   ```bash
   cd app-vue
   npm install
   npm run dev
   ```

2. **访问前端应用**

   打开浏览器，访问 http://client.a.local:5173

3. **测试登录流程**

   - 点击登录按钮
   - 应重定向到 auth-server 的登录页面
   - 登录成功后应重定向回前端
   - 应显示用户信息

4. **测试调用外部后端 API**

   - 登录后，前端应能调用 app-springboot 的 API
   - 应正确传递 Token
   - 应显示 API 返回的数据

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

### 8.5 外部系统 CORS 错误

**问题**：前端调用外部后端 API 时出现 CORS 错误

**原因**：外部后端未配置 CORS 或配置不正确

**解决**：
1. 检查外部后端的 CORS 配置
2. 确保允许前端的源（Origin）
3. 确保允许 Authorization 头

### 8.6 外部系统 Token 验证失败

**问题**：外部后端返回 401 Unauthorized

**原因**：外部后端无法验证 Token 或 Token 无效

**解决**：
1. 检查外部后端的 `issuer-uri` 配置是否正确
2. 确认 auth-server 的 JWKS 端点可访问
3. 检查 Token 是否过期或被撤销

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
├── app-springboot/                  # 外部后端系统示例
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/appspringboot/
│       └── resources/
│           └── application.yml
├── resource-api/                    # SSO 平台资源服务
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/resourceapi/
│       └── resources/
│           └── application.yml
├── app-vue/                         # 外部前端系统示例
└── docs/                            # 文档
    └── architecture-spring-cloud.md
```

---

## 10. 总结

### 10.1 架构优势

1. **统一入口**：SSO 平台核心服务通过 Gateway 统一入口，便于管理和监控
2. **统一登录**：用户只需登录一次，即可访问所有服务
3. **服务解耦**：各服务独立部署，互不影响
4. **动态扩展**：SSO 平台核心服务注册到 Nacos，Gateway 自动路由
5. **灵活集成**：外部系统可选择通过 Gateway 代理或直接接入 SSO 平台
6. **独立部署**：外部系统可独立部署，不依赖 SSO 平台基础设施

### 10.2 SSO 平台 vs 外部业务系统

| 组件 | 类型 | 说明 |
|------|------|------|
| Gateway | SSO 平台核心 | 统一入口，OAuth2 Client |
| auth-server | SSO 平台核心 | OAuth2 授权服务 |
| resource-api | SSO 平台核心 | 平台资源服务 |
| app-springboot | 外部 Web 应用示例 | 演示外部 Web 应用如何接入 SSO |
| app-vue | 外部前端系统示例 | 演示外部前端如何接入 SSO |

### 10.3 外部业务系统接入方式

外部业务系统接入 SSO 平台有两种方式，根据系统类型选择：

#### 方式一：通过 Gateway 代理（适用于内部微服务）

适用于与 SSO 平台在同一微服务网格内的系统，需要注册到 Nacos。

**架构**：
```
外部系统 → 注册到 Nacos → 通过 Gateway 路由 → 验证 Token
```

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

#### 方式二：OAuth2 Client（适用于外部独立系统）

适用于外部独立部署的系统，不需要注册到 Nacos，只需知道 auth-server 地址。

**架构**：
```
外部系统 → 直接与 auth-server 交互获取 Token
外部系统 → 使用 Token 访问受保护资源
```

**集成步骤**：

1. **添加依赖**

   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-oauth2-client</artifactId>
   </dependency>
   ```

2. **配置 application.yml**

   ```yaml
   spring:
     security:
       oauth2:
         client:
           registration:
             your-app:
               client-id: your-client-id
               client-secret: "{noop}your-client-secret"
               scope: openid,profile,email
               authorization-grant-type: authorization_code
               redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
               client-authentication-method: client_secret_basic
           provider:
             your-app:
               issuer-uri: http://auth.local:9000
   ```

3. **配置 SecurityConfig**

   ```java
   @Configuration
   @EnableWebSecurity
   public class SecurityConfig {
       @Bean
       public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
           http
               .oauth2Login(oauth2 -> oauth2
                   .defaultSuccessUrl("/", true)
               )
               .authorizeHttpRequests(auth -> auth
                   .requestMatchers("/", "/error").permitAll()
                   .anyRequest().authenticated()
               );
           return http.build();
       }
   }
   ```

4. **获取用户信息**

   ```java
   @GetMapping("/")
   public String index(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
       if (oidcUser != null) {
           model.addAttribute("username", oidcUser.getPreferredUsername());
           model.addAttribute("email", oidcUser.getEmail());
       }
       return "index";
   }
   ```

### 10.4 注意事项

1. **Gateway 使用 WebFlux**：不能引入 `spring-boot-starter-web`
2. **下游服务使用 Servlet**：可以正常使用 Spring MVC
3. **Session 存储在 Redis**：支持 Gateway 多实例部署
4. **Token 通过请求头传递**：下游服务验证 JWT Token
5. **接入方式选择**：
   - 内部微服务：使用方式一（通过 Gateway 代理）
   - 外部独立系统：使用方式二（OAuth2 Client）
6. **外部系统类型**：
   - 前端 SPA（如 app-vue）：使用 OAuth2 PKCE 流程
   - Web 应用（如 app-springboot）：使用 OAuth2 Authorization Code Flow

### 10.5 示例项目说明

本项目包含两个示例，演示外部系统如何接入 SSO 平台：

#### app-vue（外部前端系统示例）

- **位置**：`app-vue/`
- **技术栈**：Vue 3 + Vite
- **接入方式**：OAuth2 PKCE 流程（方式二）
- **功能**：
  - 使用 OAuth2 PKCE 流程获取 Token
  - 直接调用 auth-server 的 userinfo 端点获取用户信息
  - 独立管理 Token（存储在 localStorage）
- **启动**：
  ```bash
  cd app-vue
  npm install
  npm run dev
  ```
- **访问**：http://client.a.local:5173

#### app-springboot（外部 Web 应用示例）

- **位置**：`app-springboot/`
- **技术栈**：Spring Boot + Spring Security OAuth2 Client + Thymeleaf
- **接入方式**：OAuth2 Authorization Code Flow（方式二）
- **功能**：
  - 作为 OAuth2 Client，直接与 auth-server 交互获取 Token
  - 提供传统的 Web 页面（Thymeleaf）
  - 不依赖 Gateway 和 Nacos
- **启动**：
  ```bash
  cd app-springboot
  mvn spring-boot:run
  ```
- **访问**：http://localhost:8082

#### 示例架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                      示例项目架构                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  SSO 平台                                                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  auth-server:9000                                        │   │
│  │  - OAuth2 授权服务                                        │   │
│  │  - Token 端点                                            │   │
│  │  - JWKS 端点                                             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  外部系统示例（独立部署，互不依赖）                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                                                         │   │
│  │  app-vue:5173                    app-springboot:8082     │   │
│  │  ┌─────────────────┐            ┌─────────────────┐     │   │
│  │  │  前端 SPA        │            │  Web 应用        │     │   │
│  │  │  (OAuth2 Client) │            │  (OAuth2 Client) │     │   │
│  │  │                 │            │                 │     │   │
│  │  │  1. 获取 Token   │            │  1. 获取 Token   │     │   │
│  │  │  2. 显示信息     │            │  2. 显示页面     │     │   │
│  │  │                 │            │                 │     │   │
│  │  └────────┬────────┘            └────────┬────────┘     │   │
│  │           │                              │              │   │
│  └───────────┼──────────────────────────────┼──────────────┘   │
│              │                              │                   │
│              ▼                              ▼                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   auth-server:9000                       │   │
│  │                   (各自独立接入)                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  注: 两个外部系统各自独立接入 SSO，互不依赖                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 10.6 后续扩展

1. **负载均衡**：同一服务部署多实例，Gateway 自动负载均衡
2. **熔断降级**：集成 Sentinel 实现熔断降级
3. **配置中心**：使用 Nacos 作为配置中心
4. **链路追踪**：集成 SkyWalking 实现链路追踪
5. **多租户支持**：支持多个业务系统独立部署
