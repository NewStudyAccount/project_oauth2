# OAuth2 客户端类型详解：公开客户端 vs 机密客户端

## 概述

OAuth2 定义了两种客户端类型，核心区别在于**能否保密 `client_secret`**：

| | 机密客户端（Confidential） | 公开客户端（Public） |
|---|---|---|
| **定义** | 能安全保管凭证的客户端 | 无法安全保管凭证的客户端 |
| **代码运行位置** | 服务端（用户看不到） | 浏览器 / App（用户可见） |
| **client_secret** | 有 | 无 |
| **认证方式** | client_secret_basic / client_secret_post | none + PKCE |
| **典型代表** | Spring Boot 后端、PHP、Python 服务端 | Vue/React SPA、移动端 App |

---

## 一、公开客户端（Public Client）

### 特点

- 代码运行在浏览器中，用户可以通过 DevTools 查看源码和网络请求
- 无法安全存储 `client_secret`（暴露在前端代码中等于没有）
- 使用 **PKCE**（Proof Key for Code Exchange）替代 `client_secret` 来防止授权码劫持

### 认证流程（授权码 + PKCE）

```
1. 前端生成 code_verifier + code_challenge（SHA256 哈希）
2. 跳转认证中心
   GET /oauth2/authorize
       ?client_id=admin-frontend
       &response_type=code
       &scope=openid profile email
       &redirect_uri=http://app.local:5174/callback
       &code_challenge=xxx
       &code_challenge_method=S256
3. 用户在认证中心登录
4. 认证中心回调前端 /callback?code=yyy
5. 前端用 code + code_verifier 换 Token
   POST /oauth2/token
       code=yyy
       code_verifier=原始值（证明是同一个客户端）
       client_id=admin-frontend（无 secret）
6. 前端拿到 access_token，存入 localStorage
7. 请求后端 API 带 Authorization: Bearer <token>
```

### 客户端注册配置

```sql
INSERT INTO oauth2_registered_client (...)
VALUES (
    'admin-frontend-001',
    'admin-frontend',
    '管理后台前端',
    'none',                          -- 无认证方式（公开客户端）
    'authorization_code,refresh_token',
    'http://auth.local:5174/callback',
    'openid,profile,email',
    '{"settings.client.require-proof-key": true, ...}',  -- 强制 PKCE
    ...
);
```

### 前端代码结构

```
src/
├── utils/
│   ├── pkce.js          # PKCE 工具（生成 code_verifier/code_challenge）
│   └── auth.js          # 授权 URL 构建、Token 交换、刷新、登出
├── stores/
│   └── auth.js          # Pinia store，管理 Token 生命周期
├── views/
│   └── CallbackView.vue # OAuth2 回调页面
├── api/
│   └── index.js         # Axios 拦截器，自动添加 Bearer Token
└── router/
    └── index.js         # 认证检查（localStorage 中是否有 Token）
```

### 前后端通信方式

```
前端 ── Authorization: Bearer <token> ──→ 后端
```

Token 存在前端（localStorage），每次请求通过 HTTP Header 发送给后端。

---

## 二、机密客户端（Confidential Client）

### 特点

- 代码运行在服务器上，用户无法看到 `client_secret`
- 后端负责与认证中心交互，获取并存储 Token
- 前端通过 Session Cookie 与后端通信

### 认证流程（授权码 + client_secret）

```
1. 前端访问后端受保护页面
2. 后端返回 302，重定向到认证中心
   GET /oauth2/authorize
       ?client_id=order-app
       &response_type=code
       &redirect_uri=http://order.com/login/oauth2/code/order-app
3. 用户在认证中心登录
4. 认证中心回调后端 /login/oauth2/code/order-app?code=yyy
5. 后端用 code + client_secret 换 Token
   POST /oauth2/token
       code=yyy
       client_id=order-app
       client_secret=Admin@123（在服务端，前端看不到）
6. 后端将 Token 存入服务端 Session
7. 后端返回 Session Cookie（JSESSIONID）给前端
8. 前端通过 Cookie 调用后端 API
```

### 客户端注册配置

```sql
INSERT INTO oauth2_registered_client (...)
VALUES (
    'order-app-001',
    'order-app',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',  -- 加密后的 secret
    '订单系统',
    'client_secret_basic',           -- 机密客户端认证方式
    'authorization_code,refresh_token',
    'http://order.com/login/oauth2/code/order-app',
    'openid,profile,email',
    '{"settings.client.require-proof-key": false, ...}',  -- 不需要 PKCE
    ...
);
```

### 后端配置

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          order-app:
            client-id: order-app
            client-secret: "Admin@123"
            scope: openid,profile,email
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/order-app"
            client-authentication-method: client_secret_basic
        provider:
          order-app:
            issuer-uri: http://auth.local:9000
```

### 前后端通信方式

```
前端 ── Cookie: JSESSIONID=xxx ──→ 后端
```

Token 存在后端（Session），前端只持有 Session Cookie。

---

## 三、对比总结

| 维度 | 机密客户端 | 公开客户端 |
|---|---|---|
| **Token 存储位置** | 服务端 Session | 浏览器 localStorage |
| **前端→后端通信** | Session Cookie | Authorization: Bearer Token |
| **前端复杂度** | 低（只管 Cookie） | 高（PKCE + Token 管理） |
| **后端复杂度** | 高（OAuth2 Client + Session） | 低（纯 Resource Server） |
| **安全性** | 高（secret 不暴露） | 中（靠 PKCE 保护） |
| **跨域支持** | 需要同域或代理 | 天然支持（Token 在 Header） |
| **自测方式** | 需要通过 Session 或调试接口 | 直接拿 Token 测试 |
| **Token 过期处理** | 后端自动刷新（用户无感知） | 前端自动刷新或重新登录 |

---

## 四、适用场景

### 公开客户端适用场景

| 场景 | 说明 |
|---|---|
| 前后端分离 SPA | Vue/React 前端 + 独立后端 API |
| 移动端 App（无自有后端） | App 直接调用认证中心 |
| 前后端独立部署 | 不同域名、不同端口 |

### 机密客户端适用场景

| 场景 | 说明 |
|---|---|
| 传统服务端渲染应用 | Spring MVC + Thymeleaf、PHP、JSP |
| 微服务间调用 | 服务端直接通信，无浏览器参与 |
| App + 自有后端 | App 不暴露 secret，后端代理登录 |
| 定时任务 / 脚本 | 运行在服务器上，无人交互（可用 client_credentials 模式） |

---

## 五、微服务架构接入方案

### 推荐方案：前端公开客户端 + Gateway 统一校验

```
前端 SPA（公开客户端，PKCE）
    │
    │ Authorization: Bearer <token>
    ↓
API Gateway
    │ 1. 校验 Token（issuer、aud、签名）
    │ 2. 提取用户信息（userId、username、roles）
    │ 3. 注入请求头转发
    │
    │ X-User-Id: 123
    │ X-Username: admin
    │ X-User-Roles: ROLE_USER,ROLE_ADMIN
    ↓
微服务群（不校验 Token，读请求头获取用户信息）
```

### Gateway 全局过滤器示例

```java
@Component
public class UserInfoRelayFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
            .filter(p -> p instanceof JwtAuthenticationToken)
            .cast(JwtAuthenticationToken.class)
            .map(auth -> {
                Jwt jwt = auth.getToken();
                ServerHttpRequest request = exchange.getRequest().mutate()
                    .header("X-User-Id", jwt.getSubject())
                    .header("X-Username", jwt.getClaimAsString("username"))
                    .build();
                return exchange.mutate().request(request).build();
            })
            .defaultIfEmpty(exchange)
            .flatMap(chain::filter);
    }

    @Override
    public int getOrder() { return -100; }
}
```

### 微服务读取用户信息

```java
@GetMapping("/api/orders")
public List<Order> getOrders(
        @RequestHeader("X-User-Id") String userId,
        @RequestHeader("X-Username") String username) {
    return orderService.findByUserId(userId);
}
```

---

## 六、选择决策树

```
你的项目是前后端分离的吗？
│
├── 否（服务端渲染） → 机密客户端
│
└── 是
    │
    ├── 前后端同域部署？ → 机密客户端
    │
    └── 前后端独立部署？
        │
        ├── 有 API Gateway？
        │   │
        │   └── 是 → 前端公开客户端 + Gateway 校验 + 微服务读请求头
        │
        └── 无 Gateway → 前端公开客户端 + 各后端独立校验 Token
```

---

## 七、SSO 单点登录与 Audience 授权隔离

### 7.1 什么是 SSO

SSO（Single Sign-On，单点登录）的核心定义：**一次登录，多系统访问**。

用户只需在统一认证中心登录一次，即可访问所有接入的系统，无需重复输入密码。

### 7.2 什么是 Audience（aud）

`aud`（Audience）是 JWT 中的一个声明，表示**该 Token 允许被哪个系统使用**。

```json
{
  "iss": "http://auth.local:9000",
  "sub": "admin",
  "aud": ["client-app"],
  "username": "admin",
  "exp": 1234567890
}
```

资源服务校验 Token 时，会检查 `aud` 是否包含自己：

```yaml
# client-app 后端配置
spring.security.oauth2.resourceserver.jwt:
  issuer-uri: http://auth.local:9000
  audiences: client-app  # 只接受 aud 包含 "client-app" 的 Token
```

### 7.3 无 aud 限制的问题

默认情况下，认证中心签发的 Token 不携带 `aud` 声明（或 `aud` 不受限）。此时**任何 Token 都能访问所有系统**：

```
admin-frontend Token → client-app API  → ✅ 可以访问（无 aud 限制）
admin-frontend Token → order-app API   → ✅ 可以访问（无 aud 限制）
client-app Token     → order-app API   → ✅ 可以访问（无 aud 限制）
```

**风险**：一个 Token 泄露 = 所有系统沦陷。

### 7.4 有 aud 限制后的效果

每个 Token 携带特定的 `aud`，资源服务只接受匹配的 Token：

```
admin-frontend Token（aud: auth-center-admin）
    → auth-center API  → ✅ aud 匹配
    → client-app API   → ❌ aud 不匹配，拒绝
    → order-app API    → ❌ aud 不匹配，拒绝

client-app Token（aud: client-app）
    → client-app API   → ✅ aud 匹配
    → auth-center API  → ❌ aud 不匹配，拒绝
    → order-app API    → ❌ aud 不匹配，拒绝
```

### 7.5 SSO 与 aud 的关系

| 概念 | 含义 | aud 限制是否影响 |
|---|---|---|
| **SSO** | 一次登录，多系统访问 | 不影响 |
| **aud** | Token 的访问范围 | 每个系统用自己 aud 的 Token |

**SSO 是关于"登录"，aud 是关于"授权"。两者不矛盾。**

### 7.6 完整流程：SSO + aud 隔离

```
1. 用户访问 client-app 前端
2. 前端发现未登录 → 跳转认证中心
3. 用户在认证中心登录（SSO 登录，仅此一次）
4. 认证中心签发 Token（aud: client-app），回调前端
5. 前端用 Token 访问 client-app 后端 → ✅ 成功

6. 用户切换到 order-app 前端
7. 前端发现未登录 → 跳转认证中心
8. 认证中心发现用户已登录（SSO 状态有效），无需再次输入密码
9. 认证中心签发 Token（aud: order-app），回调前端
10. 前端用 Token 访问 order-app 后端 → ✅ 成功
```

**用户只在第 3 步输入了一次密码**，后续访问其他系统时认证中心自动完成授权（SSO）。

### 7.7 前端获取不同 aud 的 Token

```javascript
// 用户已在认证中心登录（SSO 状态有效）
// 访问不同系统时，获取该系统专用的 Token

// 访问 client-app
const clientToken = await fetchToken({
  clientId: 'client-app',
  scope: 'openid profile email'
})
// → Token 的 aud 包含 "client-app"

// 访问 order-app
const orderToken = await fetchToken({
  clientId: 'order-app',
  scope: 'openid profile email'
})
// → Token 的 aud 包含 "order-app"

// 两个 Token 都是在同一次 SSO 登录下获取的，无需重新输入密码
```

### 7.8 认证中心签发 aud 的实现

```java
@Bean
public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
    return context -> {
        if (context.getTokenType().getValue().equals("access_token")) {
            // 根据 client_id 设置 aud
            String clientId = context.getRegisteredClient().getClientId();
            context.getClaims().claim("aud", List.of(clientId));
        }
    };
}
```

### 7.9 安全性对比

| | 无 aud 限制 | 有 aud 限制 |
|---|---|---|
| **SSO** | ✅ 一次登录 | ✅ 一次登录 |
| **Token 数量** | 1 个通用 Token | 每个系统 1 个 Token |
| **Token 泄露影响** | 所有系统沦陷 | 仅 1 个系统受影响 |
| **权限隔离** | ❌ 无 | ✅ 有 |
| **最小权限原则** | ❌ 不满足 | ✅ 满足 |

### 7.10 总结

```
SSO（单点登录）          aud（访问控制）
    │                       │
    │ 解决的是：             │ 解决的是：
    │ "用户只需登录一次"     │ "Token 只能访问指定系统"
    │                       │
    └───── 互不矛盾 ────────┘
    
    一次登录 → 获取多个不同 aud 的 Token → 各系统独立校验
```

**最佳实践**：SSO + aud 隔离必须同时实施。SSO 保证用户体验，aud 保证安全性。
