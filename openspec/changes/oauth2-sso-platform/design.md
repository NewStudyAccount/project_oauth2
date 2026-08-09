# Design: OAuth2 SSO 统一认证平台

## 技术栈

| 组件 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 框架 | Spring Boot | 3.2.x | 基础框架 |
| 认证 | Spring Authorization Server | 1.2.x | OAuth2/OIDC 服务器 |
| 安全 | Spring Security | 6.2.x | 安全框架 |
| ORM | MyBatis-Plus | 3.5.x | 数据库访问 |
| 数据库 | MySQL | 8.0+ | 持久化存储 |
| 缓存 | Redis | 7.x | Token/Session/黑名单 |
| 模板 | Thymeleaf | 3.x | 登录页面（先不分离） |
| JWT | Nimbus JOSE JWT | 9.x | JWT 签发/验证 |
| 前端（业务系统） | Vue 3 + Vite | 3.4+ | app-vue |

## 系统架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│   浏览器                                                                │
│     │                                                                   │
│     │  1. 访问业务系统                                                  │
│     ▼                                                                   │
│  ┌──────────────┐                                                       │
│  │   app-vue    │                                                       │
│  │  (公开客户端) │                                                       │
│  │  :5173       │                                                       │
│  └──────┬───────┘                                                       │
│         │  2. 302 跳转登录                                              │
│         ▼                                                               │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    Auth Server (:9000)                            │  │
│  │                                                                  │  │
│  │   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │  │
│  │   │  Controller  │  │  Service    │  │  Repository │             │  │
│  │   │             │  │             │  │             │             │  │
│  │   │ Login       │  │ UserSvc     │  │ UserMapper  │             │  │
│  │   │ Register    │  │ ClientSvc   │  │ ClientMapper│             │  │
│  │   │ Consent     │  │ TokenSvc    │  │ TokenMapper │             │  │
│  │   │ UserInfo    │  │ AuditSvc    │  │ AuditMapper │             │  │
│  │   │ Webhook     │  │ WebhookSvc  │  │             │             │  │
│  │   └─────────────┘  └─────────────┘  └─────────────┘             │  │
│  │                                                                  │  │
│  │   端点:                                                          │  │
│  │   页面: /login /register /consent /error                         │  │
│  │   OAuth2: /oauth2/authorize /oauth2/token /oauth2/jwks           │  │
│  │   API: /userinfo /api/auth/* /webhook/*                          │  │
│  └───────────────────────────┬──────────────────────────────────────┘  │
│                               │                                         │
│              ┌────────────────┼────────────────┐                        │
│              │                │                │                        │
│              ▼                ▼                ▼                        │
│         ┌─────────┐    ┌──────────┐    ┌──────────┐                    │
│         │  MySQL   │    │  Redis   │    │ 邮件服务  │                    │
│         │         │    │          │    │ (注册验证) │                    │
│         │ 用户表   │    │ Token缓存│    └──────────┘                    │
│         │ 客户端表 │    │ 黑名单   │                                     │
│         │ 权限表   │    │ Session  │                                     │
│         │ 审计日志 │    │          │                                     │
│         └─────────┘    └──────────┘                                     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## 核心流程设计

### 1. SSO 登录流程（内部应用，自动授权）

```
Browser         app-vue         Auth Server         MySQL/Redis
  │                │                │                    │
  │──访问─────────▶│                │                    │
  │◀──302──────────│                │                    │
  │   /oauth2/     │                │                    │
  │   authorize    │                │                    │
  │                │                │                    │
  │──登录页面───────────────────────▶│                    │
  │──提交账号密码────────────────────▶│                    │
  │                │                │──验证用户──────────▶│
  │                │                │◀──用户信息──────────│
  │                │                │                    │
  │                │                │──检查 user_        │
  │                │                │  client_access────▶│
  │                │                │◀──allowed=true──────│
  │                │                │                    │
  │                │                │──require_consent?  │
  │                │                │  false → 自动授权  │
  │                │                │                    │
  │                │                │──生成授权码────────▶│
  │                │                │──记录审计日志──────▶│
  │                │                │                    │
  │◀──302 + code───┼────────────────│                    │
  │                │                │                    │
  │──带 code──────▶│                │                    │
  │                │──POST /token──▶│                    │
  │                │                │──验证 code────────▶│
  │                │                │──生成 JWT          │
  │                │                │──存 Redis──────────▶│
  │                │◀──JWT──────────│                    │
  │◀──登录成功─────│                │                    │
```

### 2. 第三方应用授权流程（需用户确认）

```
Browser       第三方App        Auth Server
  │              │                │
  │──访问────────▶│               │
  │◀──302─────────│               │
  │              │                │
  │──登录+授权页面────────────────▶│
  │              │                │
  │  显示:                         │
  │  "XXX应用 想访问您的:          │
  │   基本信息、邮箱"              │
  │                                │
  │  [同意] [拒绝]                │
  │              │                │
  │──点同意──────────────────────▶│
  │              │                │
  │              │                │──记录 consent
  │              │                │──生成 code
  │◀──302 + code─┼────────────────│
  │──code────────▶│               │
  │              │──token────────▶│
  │              │◀──JWT──────────│
```

### 3. Token 撤销流程

```
场景一: 管理员踢人下线
─────────────────────

Admin          Auth Server         Redis              DB
  │                │                 │                  │
  │──撤销Token────▶│                 │                  │
  │                │──加入黑名单────▶│                  │
  │                │──持久化───────────────────────────▶│
  │                │                 │                  │
  │◀──撤销成功─────│                 │                  │

  下次该用户请求:
  │                │                 │                  │
  │──带Token──────▶│──检查黑名单────▶│                  │
  │                │◀──命中！────────│                  │
  │◀──401─────────│                 │                  │


场景二: 用户修改密码
───────────────────

User           Auth Server         Redis              DB
  │                │                 │                  │
  │──修改密码──────▶│                 │                  │
  │                │──撤销该用户所有Token──────────────▶│
  │                │  (加入黑名单)   │                  │
  │◀──修改成功─────│                 │                  │
```

### 4. Webhook 流程

```
Auth Server          OA系统             CRM系统
  │                    │                  │
  │ 用户信息变更       │                  │
  │                    │                  │
  │──查 subscriber 表──│                  │
  │                    │                  │
  │──POST /webhook────▶│                  │
  │  Signature: xxx    │                  │
  │  Body: {changes}   │                  │
  │                    │──验签名          │
  │                    │──更新快照表      │
  │◀──200 OK──────────│                  │
  │                    │                  │
  │──POST /webhook─────┼─────────────────▶│
  │                    │                  │──验签名
  │                    │                  │──更新快照表
  │◀──200 OK──────────┼──────────────────│
  │                    │                  │
  │ 失败时:            │                  │
  │──重试(指数退避)───▶│                  │
```

## 关键配置设计

### Auth Server 安全过滤链

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 链1: OAuth2 Authorization Server (优先级最高)
    @Bean
    @Order(1)
    public SecurityFilterChain authServerFilterChain(HttpSecurity http) {
        // 处理 /oauth2/authorize, /oauth2/token, /oauth2/jwks 等
        // /.well-known/openid-configuration
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        // 未认证时跳转到 /login
        // ...
    }

    // 链2: 默认安全链 (页面 + API)
    @Bean
    @Order(2)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/error", "/css/**", "/js/**").permitAll()
                .requestMatchers("/userinfo").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.loginPage("/login"))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            // ...
    }
}
```

### JWT Payload 设计

```json
{
  "sub": "1",
  "iss": "http://auth.local:9000",
  "aud": "oa-app",
  "iat": 1691234567,
  "exp": 1691238167,
  "jti": "unique-token-id",
  "username": "zhangsan",
  "nickname": "张三",
  "email": "zhang@company.com",
  "phone": "13800138000",
  "scope": "openid profile email"
}
```

### Redis Key 设计

```
┌─────────────────────────────────────────────────────────────┐
│  Redis Key 命名规范                                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Token 缓存:                                                │
│  oauth2:token:{access_token_hash}  → Token详情 (JSON)      │
│  oauth2:refresh:{refresh_token_hash} → Token详情 (JSON)    │
│                                                             │
│  Token 黑名单:                                              │
│  oauth2:blacklist:{jti}  → "1" (过期时间=token过期时间)     │
│                                                             │
│  Session:                                                   │
│  spring:session:sessions:{sessionId} → Session数据         │
│                                                             │
│  验证码:                                                    │
│  register:code:{email}  → 验证码 (5分钟过期)               │
│                                                             │
│  限流:                                                      │
│  rate:register:{ip}  → 计数器 (1小时过期)                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 页面设计

### 登录页 /login

```
┌─────────────────────────────────────────┐
│           统一认证中心                    │
│                                          │
│    ┌──────────────────────────────┐     │
│    │                              │     │
│    │  {clientName} 需要您登录     │     │  ← 动态显示来源系统名
│    │                              │     │
│    │  账号: [________________]    │     │
│    │  密码: [________________]    │     │
│    │                              │     │
│    │  [      登  录      ]        │     │
│    │                              │     │
│    │  还没有账号？立即注册         │     │
│    │  忘记密码？                   │     │
│    │                              │     │
│    └──────────────────────────────┘     │
│                                          │
└─────────────────────────────────────────┘
```

### 注册页 /register

```
┌─────────────────────────────────────────┐
│           注册账号                        │
│                                          │
│    ┌──────────────────────────────┐     │
│    │                              │     │
│    │  用户名: [________________]  │     │
│    │  昵称:   [________________]  │     │
│    │  邮箱:   [________________]  │     │
│    │  验证码: [________] [发送]   │     │
│    │  密码:   [________________]  │     │
│    │  确认密码: [______________]  │     │
│    │                              │     │
│    │  [      注  册      ]        │     │
│    │                              │     │
│    │  已有账号？去登录             │     │
│    │                              │     │
│    └──────────────────────────────┘     │
│                                          │
└─────────────────────────────────────────┘
```

### 授权确认页 /consent（第三方应用用）

```
┌─────────────────────────────────────────┐
│           授权确认                        │
│                                          │
│    ┌──────────────────────────────┐     │
│    │                              │     │
│    │  {clientName} 想访问您的     │     │
│    │  账号信息                    │     │
│    │                              │     │
│    │  该应用将获得以下权限:       │     │
│    │                              │     │
│    │  ☑ 查看您的基本资料          │     │
│    │  ☑ 查看您的邮箱地址          │     │
│    │                              │     │
│    │  [    同意授权    ]          │     │
│    │  [    拒绝        ]          │     │
│    │                              │     │
│    └──────────────────────────────┘     │
│                                          │
└─────────────────────────────────────────┘
```

## API 设计

### 认证相关 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/auth/login | 用户登录 | 否 |
| POST | /api/auth/logout | 用户登出 | 是 |
| POST | /api/auth/register | 用户注册 | 否 |
| POST | /api/auth/send-code | 发送邮箱验证码 | 否 |
| GET | /api/auth/me | 获取当前用户信息 | 是 |

### OAuth2 标准端点

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /oauth2/authorize | 授权端点 | Session |
| POST | /oauth2/token | Token 端点 | client |
| POST | /oauth2/revoke | Token 撤销 | client |
| GET | /oauth2/jwks | 公钥 | 否 |
| GET | /.well-known/openid-configuration | OIDC 发现 | 否 |

### 用户信息

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /userinfo | OIDC UserInfo | Bearer Token |

### 管理 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/admin/users | 用户列表 | 管理员 |
| PUT | /api/admin/users/{id}/access | 设置用户系统权限 | 管理员 |
| POST | /api/admin/tokens/revoke | 强制撤销Token | 管理员 |
| GET | /api/admin/audit-logs | 审计日志查询 | 管理员 |
| GET | /api/admin/clients | 客户端列表 | 管理员 |

### Webhook

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/webhooks | 订阅列表 | 管理员 |
| POST | /api/webhooks | 创建订阅 | 管理员 |
| DELETE | /api/webhooks/{id} | 删除订阅 | 管理员 |

## 异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // OAuth2 错误 → 重定向到错误页
    @ExceptionHandler(OAuth2AuthenticationException.class)
    public String handleOAuth2Error(OAuth2AuthenticationException e) {
        return "redirect:/error?message=" + e.getError().getDescription();
    }

    // 无权限 → 显示无权限页
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied() {
        return "error/no_permission";
    }

    // 其他异常 → JSON 响应
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(500).body(Map.of("error", "系统内部错误"));
    }
}
```
