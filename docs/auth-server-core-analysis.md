# auth-server OAuth2 授权服务核心逻辑分析

## 一、整体架构

该服务基于 **Spring Authorization Server** 构建，核心流程如下：

```
客户端应用 → /oauth2/authorize → 用户登录 → 授权确认(可选) → 颁发 Authorization Code
    → 客户端用 Code 换 Token → 访问 /userinfo 获取用户信息
```

---

## 二、两条安全过滤链（核心入口）

| 过滤链 | 优先级 | 职责 |
|--------|--------|------|
| `authServerFilterChain` | `@Order(1)` | 处理 `/oauth2/*`、OIDC 发现、JWKS 端点 |
| `defaultFilterChain` | `@Order(2)` | 处理表单登录、页面鉴权、CSRF |

**关键点**：`@Order(1)` 的链先匹配，OAuth2 协议端点由框架托管；其余请求走 `@Order(2)` 的常规 Spring Security 链。

---

## 三、客户端注册（双轨制）

客户端通过 **两种方式** 注册：

**1. `application.yml` 静态配置**（Spring 框架内置 `RegisteredClientRepository`）：
- `vue-app` — 公开客户端（PKCE，无 secret）
- `springboot-app` — 机密客户端（client_secret_basic）
- `gateway-app` — 网关客户端
- `third-party-app` — 第三方应用（`require-authorization-consent: true`）

**2. `oauth2_client` 数据库表**（自定义 `ClientService` 管理），用于管理后台动态增删客户端。

> **注意**：当前 `RegisteredClientRepository` 只读取 yml 配置，数据库中的 `oauth2_client` 表仅被 `ClientService` 用于管理展示，并未接入 Spring Authorization Server 的客户端加载流程。

---

## 四、用户认证流程

```
用户提交用户名/密码
    → SecurityConfig.defaultFilterChain 拦截 /login
    → CustomUserDetailsService.loadUserByUsername()
        → 查 sys_user 表（status=1 的用户）
        → 硬编码角色：admin → ROLE_ADMIN + ROLE_USER，其他 → ROLE_USER
    → BCrypt 校验密码（自动添加 {bcrypt} 前缀兼容 DelegatingPasswordEncoder）
```

---

## 五、Token 签发与自定义

**RSA 密钥对**：每次启动随机生成（`keyPair()` Bean），**重启即失效**。

**Token 自定义**（`OAuth2TokenCustomizerConfig`）：
- 在 ID Token 和 Access Token 中注入：`username`、`nickname`、`email`、`phone`、`jti`
- ID Token 设置 `sub` 为用户名

**Token 有效期**：
- Access Token: **30 分钟**
- Refresh Token: **7 天**

---

## 六、授权码流程核心路径

```
1. 客户端重定向用户到:
   GET /oauth2/authorize?client_id=vue-app&response_type=code&scope=openid+profile&redirect_uri=...

2. Spring Authorization Server 检查:
   - 用户是否已登录（未登录 → 重定向 /login）
   - 客户端是否已获用户授权（未授权 → 重定向 /consent）

3. ConsentController 处理授权确认:
   - 检查用户是否有权访问该客户端（AccessControlService.hasAccess）
   - 检查是否已有 consent 记录（有则跳过）
   - 第三方应用(require_consent=1)需用户手动确认

4. 授权成功 → 重定向回客户端带 code 参数

5. 客户端后端用 code + client_id/secret 换取 Token:
   POST /oauth2/token (grant_type=authorization_code)

6. 客户端用 Access Token 请求用户信息:
   GET /userinfo → UserInfoController 返回用户详情
```

---

## 七、SSO 单点登录原理

### 核心机制

关键点：**auth-server 的登录会话是共享的**。

```
用户访问 app-vue
  → 重定向到 auth.local:9000/oauth2/authorize?client_id=vue-app&...
  → auth-server 发现无会话 → 跳转 /login → 用户输入密码
  → auth-server 创建 Session (JSESSIONID cookie 域: auth.local)
  → 颁发 Authorization Code → app-vue 拿到 Token ✅

用户访问 app-springboot
  → 重定向到 auth.local:9000/oauth2/authorize?client_id=springboot-app&...
  → auth-server 发现已有 Session（同一个 JSESSIONID）→ 跳过登录
  → 直接走授权逻辑 → 颁发 Authorization Code → app-springboot 拿到 Token ✅
```

### SSO 流程图

```
app-vue (client.a.local:5173)          auth-server (auth.local:9000)        app-springboot (client.a.local:8082)
        |                                        |                                      |
        |--- 1. 访问 /oauth2/authorize --------->|                                      |
        |<-- 2. 302 重定向到 /login -------------|                                      |
        |--- 3. 用户登录（POST /login）---------->|                                      |
        |    Session 创建 ✅                     |                                      |
        |<-- 4. 302 回 /oauth2/authorize --------|                                      |
        |<-- 5. 302 redirect_uri?code=xxx -------|                                      |
        |--- 6. code 换 Token ----------------->|                                      |
        |<-- 7. 返回 access_token + id_token ----|                                      |
        |                                        |                                      |
        |                                        |<--- 8. 访问 /oauth2/authorize --------|
        |                                        |    Session 已存在，跳过登录 ✅         |
        |                                        |--- 9. 302 redirect_uri?code=yyy ----->|
```

### JSESSIONID

JSESSIONID 是 **Java Web 应用（Servlet）的会话标识 Cookie**，由 Tomcat/Servlet 容器自动生成。

```
浏览器 ←→ Cookie: JSESSIONID=abc123def456 ←→ 服务端 HttpSession 对象
```

- **值**：一个随机字符串，如 `abc123def456`
- **作用**：告诉服务端"这个请求是我上次那个用户发的"
- **存储位置**：服务端内存（或 Redis），浏览器只存一个 ID 引用

在 auth-server 中的流转过程：

```
第一次登录 POST /login
  → Spring Security 验证密码通过
  → 创建 HttpSession，存入用户的 Authentication 对象
  → Tomcat 自动在响应中设置: Set-Cookie: JSESSIONID=xxxx

后续请求 GET /oauth2/authorize
  → 浏览器自动携带: Cookie: JSESSIONID=xxxx
  → Tomcat 找到对应的 HttpSession
  → Spring Security 读取其中的 Authentication → 用户已登录，跳过登录页
```

**两次请求的 JSESSIONID 是一致的**，因为两次请求都发往同一个域 `auth.local:9000`，浏览器按域名管理 Cookie。

### JSESSIONID 与 OAuth2 Token 的区别

| | JSESSIONID | OAuth2 Access Token |
|---|---|---|
| 用途 | auth-server 自身的登录状态 | 客户端访问 API 的凭证 |
| 发给谁 | 浏览器自动管理 | 发给 app-vue / app-springboot |
| 生命周期 | 浏览器关闭或 Session 过期 | 30 分钟（项目配置） |
| 谁验证 | auth-server 的 Tomcat | resource-api 用 JWKS 验签 |

### 注意：退出不同步

当前实现中，**登出不是全局的**。用户在 app-vue 调用 `/logout` 只清除 app-vue 自己的 Token，auth-server 的 Session 仍然存在。如果需要"一处登出、全部退出"，需要实现 **RP-Initiated Logout**（OIDC 登出协议），当前代码未实现此功能。

---

## 八、访问控制

`AccessControlService` 实现了 **用户-客户端粒度** 的访问控制：
- 查 `user_client_access` 表
- **无记录默认允许**（内部应用自动放行）
- 仅当 `allowed=0` 时拒绝

---

## 九、Token 黑名单（主动撤销）

`TokenBlacklistService` 提供双层存储：
- **Redis**（快速查询，自动过期）→ **MySQL**（持久化，定时清理）
- 支持单 Token 撤销和用户级全量撤销
- 每天凌晨 3 点清理过期记录

---

## 十、数据库核心表

| 表 | 作用 |
|----|------|
| `sys_user` | 用户表 |
| `oauth2_client` | 客户端注册表（管理用） |
| `oauth2_authorization` | Spring 框架标准表，存储授权码/Token |
| `oauth2_authorization_consent` | Spring 框架标准表，存储用户授权记录 |
| `user_client_access` | 用户-客户端访问权限 |
| `oauth2_token_blacklist` | Token 黑名单 |
| `sys_audit_log` | 审计日志 |

---

## 十一、最小必要实现（4 个核心类）

剥离所有增强功能，只保留 OAuth2 授权码流程能跑通的最少代码。

### 1. AuthorizationServerConfig — 最核心

```java
@Configuration
public class AuthorizationServerConfig {

    // ① 密码编码器（登录验密码必须）
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // ② 授权服务器的安全过滤链
    //    Spring 框架内部自动注册以下端点：
    //    GET  /oauth2/authorize     → 授权码发放（浏览器重定向）
    //    POST /oauth2/token         → 换 Token
    //    GET  /.well-known/openid-configuration → OIDC 发现
    //    GET  /oauth2/jwks          → 公钥
    @Bean
    @Order(1)
    public SecurityFilterChain authServerFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        // 未登录 → 跳 /login
        http.exceptionHandling(ex -> ex
            .defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
        return http.build();
    }

    // ③ RSA 密钥对（签发 JWT 必须）
    @Bean
    public KeyPair keyPair() {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    // ④ JWK 公钥源（客户端验签用）
    @Bean
    public JWKSource<SecurityContext> jwkSource(KeyPair keyPair) {
        RSAKey key = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString()).build();
        return new ImmutableJWKSet<>(new JWKSet(key));
    }

    // ⑤ JWT 解码器（本服务验 Token 用）
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> src) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(src);
    }

    // ⑥ 签发者地址（Token 里的 iss 字段）
    @Bean
    public AuthorizationServerSettings settings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://auth.local:9000").build();
    }

    // ⑦ 授权记录存储（框架需要，用 JDBC 存数据库）
    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbc, RegisteredClientRepository repo) {
        return new JdbcOAuth2AuthorizationService(jdbc, repo);
    }

    // ⑧ 用户授权确认存储
    @Bean
    public OAuth2AuthorizationConsentService consentService(
            JdbcTemplate jdbc, RegisteredClientRepository repo) {
        return new JdbcOAuth2AuthorizationConsentService(jdbc, repo);
    }
}
```

**一句话总结**：这些 Bean 告诉 Spring —— "你是一个授权服务器，用 RSA 签 JWT，用数据库存授权记录，有人未登录就让他去 `/login`"。

### 2. SecurityConfig — 登录表单

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(2)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/error").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")           // 登录页 URL
                .loginProcessingUrl("/login")); // 表单提交 URL
        return http.build();
    }
}
```

**一句话总结**：处理 `/login` 表单提交，验证用户名密码，验证通过后创建 `HttpSession`（即 JSESSIONID）。

### 3. CustomUserDetailsService — 用户从哪来

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper mapper;

    @Override
    public UserDetails loadUserByUsername(String username) {
        SysUser user = mapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getStatus, 1));

        if (user == null) throw new UsernameNotFoundException("不存在");

        return new User(user.getUsername(), user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
```

**一句话总结**：Spring Security 调用它查数据库拿用户信息（用户名、密码、状态），用来校验登录表单。

### 4. application.yml — 客户端注册

```yaml
spring:
  security:
    oauth2:
      authorizationserver:
        client:
          my-app:
            registration:
              client-id: my-app
              client-authentication-methods: [none]        # 公开客户端
              authorization-grant-types: [authorization_code, refresh_token]
              redirect-uris: [http://localhost:5173/callback]
              scopes: [openid, profile, email]
```

**一句话总结**：告诉 Spring "有哪些应用可以来申请授权码"。

### 4 个核心组件的协作流程

```
浏览器访问 /oauth2/authorize?client_id=my-app
    │
    ▼
① AuthorizationServerConfig（@Order(1) 过滤链）
    → Spring 框架内置逻辑拦截 /oauth2/* 端点
    → 检查用户是否已登录
    │
    ├─ 未登录 → ② SecurityConfig（@Order(2)）→ 重定向 /login
    │          → 用户提交表单
    │          → ③ CustomUserDetailsService.loadUserByUsername() → 查 DB 验密码
    │          → 验证通过 → 创建 Session → 回到 /oauth2/authorize
    │
    ├─ 已登录 → 检查 ④ application.yml 中的 client 配置
    │          → client_id 合法？redirect_uri 匹配？scope 允许？
    │          → 全部通过 → 生成 Authorization Code → 302 回调 redirect_uri?code=xxx
    │
客户端用 code 调用 POST /oauth2/token
    → ① 中的框架逻辑验 code → 签发 JWT（用 keyPair 签名）
    → 返回 access_token + id_token + refresh_token
```

---

## 十二、代码必要性总览

| 你的代码 | 是否必要 | 去掉会怎样 |
|----------|---------|-----------|
| `AuthorizationServerConfig` | **必须** | 没有授权服务器 |
| `SecurityConfig` | **必须** | 无法登录 |
| `CustomUserDetailsService` | **必须** | 无法验证用户 |
| `application.yml client 配置` | **必须** | 无客户端可授权 |
| `schema.sql` 中 3 张表 | **必须** | 框架无法存授权记录 |
| `SysUser` + `SysUserMapper` | **必须** | 无法查用户 |
| `OAuth2TokenCustomizerConfig` | 可选 | Token 中没有自定义字段 |
| `TokenBlacklistService` | 可选 | 无法主动撤销 Token |
| `ConsentController` | 可选 | 第三方应用无授权确认页 |
| `AccessControlService` | 可选 | 无用户级访问控制 |
| `RegisterController` | 可选 | 无用户注册 |
| `AdminController` | 可选 | 无管理后台 |
| `AuditLogService` | 可选 | 无审计日志 |
| `WebhookService` | 可选 | 无事件推送 |
| `oauth2_client` 表 + `ClientService` | 可选 | 数据库管理的客户端不生效（当前未接入） |
| `user_client_access` 表 | 可选 | 无访问权限控制 |
| `oauth2_token_blacklist` 表 | 可选 | 无 Token 黑名单 |

---

## 十三、扩展功能模块

| 模块 | 职责 |
|------|------|
| `RegisterService` | 用户注册（含邮箱验证码、IP 限流、密码强度校验） |
| `AuditLogService` | 审计日志（LOGIN/LOGOUT/AUTHORIZE/TOKEN_ISSUED 等事件） |
| `WebhookService` | Webhook 事件推送（HMAC-SHA256 签名、指数退避重试） |
| `MailService` | 邮件验证码发送 |

这些属于运维和管理层面的增强功能，不参与 OAuth2 核心授权流程。

---

## 十四、关键设计特点

1. **JWT 非持久化**：RSA 密钥每次启动重新生成，重启后所有旧 Token 失效
2. **双轨客户端管理**：yml 静态配置 + 数据库动态管理（但未完全打通）
3. **内部/第三方应用区分**：通过 `require_consent` 控制是否需要用户手动授权
4. **Token 自定义 Claims**：将用户业务字段（nickname/email/phone）直接嵌入 JWT，避免频繁查库

---

## 十五、关键发现：客户端注册双轨未打通

项目同时存在两套客户端存储：
1. **`application.yml`** → Spring 框架的 `RegisteredClientRepository`（运行时实际使用）
2. **`oauth2_client` 表** → 自定义 `ClientService`（仅管理展示用）

`add-registered-client-table.sql` 脚本尝试通过 `oauth2_registered_client` 表桥接两者，但当前 `AuthorizationServerConfig` 中并未注册 `JdbcRegisteredClientRepository` Bean，所以 **数据库中的客户端变更不会影响实际的 OAuth2 认证行为** —— 这是一个需要后续整合的架构点。
