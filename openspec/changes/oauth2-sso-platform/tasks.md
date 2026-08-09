# Tasks: OAuth2 SSO 统一认证平台

## Phase 1: Auth Server 基础

### 1.1 项目初始化
- [x] 创建 Spring Boot 项目结构（auth-server）
- [x] 添加依赖：Spring Authorization Server, Spring Security, MyBatis-Plus, Redis, Thymeleaf
- [x] 配置 application.yml（数据库、Redis、Server端口）
- [x] 创建启动类 AuthServerApplication

### 1.2 数据库
- [x] 创建数据库 oauth2_center
- [x] 编写建表 SQL（schema.sql）：sys_user, oauth2_client, oauth2_authorization_code, oauth2_token, user_client_access, user_client_consent, oauth2_token_blacklist, sys_audit_log, webhook_subscriber, webhook_log
- [x] 编写初始数据 SQL（data.sql）：默认管理员、测试客户端
- [x] 配置 MyBatis-Plus（分页、驼峰映射）

### 1.3 用户认证
- [x] 创建 SysUser 实体类
- [x] 创建 SysUserMapper
- [x] 实现 CustomUserDetailsService（从数据库加载用户）
- [x] 配置密码编码器 BCryptPasswordEncoder
- [x] 创建登录页面（login.html，Thymeleaf）
- [x] 配置 SecurityConfig：登录页、静态资源放行

### 1.4 OAuth2 授权服务器配置
- [x] 创建 AuthorizationServerConfig
- [x] 配置 RegisteredClientRepository（从数据库加载）
- [x] 配置 JWKSource（RSA 密钥对）
- [x] 配置 JwtDecoder
- [x] 配置 AuthorizationServerSettings（issuer URI）
- [x] 实现 OAuth2AuthorizationService（数据库存储）
- [x] 实现 OAuth2AuthorizationConsentService

### 1.5 OAuth2 标准端点
- [x] /oauth2/authorize — 授权码端点
- [x] /oauth2/token — Token 端点
- [x] /oauth2/revoke — Token 撤销端点
- [x] /oauth2/jwks — 公钥端点
- [x] /.well-known/openid-configuration — OIDC 发现端点

### 1.6 OIDC UserInfo 端点
- [x] 实现 /userinfo 端点（返回用户信息 JSON）
- [x] 配置 JWT Payload 携带用户信息（username, nickname, email, phone）

### 1.7 审计日志
- [x] 创建 SysAuditLog 实体
- [x] 创建 SysAuditLogMapper
- [x] 实现 AuditLogService
- [x] 在关键操作点记录日志：LOGIN, LOGOUT, AUTHORIZE, TOKEN_ISSUED

### 1.8 异常处理
- [x] 创建 GlobalExceptionHandler
- [x] OAuth2 错误重定向到错误页
- [x] 创建错误页面（error.html）

---

## Phase 2: 客户端接入

### 2.1 客户端注册（数据库）
- [x] 创建 OAuth2Client 实体
- [x] 创建 OAuth2ClientMapper
- [x] 实现 ClientService（CRUD）
- [x] 注册内置客户端：vue-app（PKCE公开客户端）、springboot-app（机密客户端）

### 2.2 app-vue 接入（公开客户端 + PKCE）
- [x] 创建 Vue 项目（app-vue）
- [x] 实现 PKCE 工具（pkce.js）
- [x] 实现 OAuth2 登录流程（跳转 authorize → 回调 → 换 token）
- [x] 实现 Callback 页面
- [x] 实现 Profile 页面（调用 /userinfo）
- [x] 实现登出

### 2.3 app-springboot 接入（机密客户端）
- [x] 创建 Spring Boot 项目（app-springboot）
- [x] 配置 OAuth2 Client（authorization_code 模式）
- [x] 配置 SecurityConfig
- [x] 实现首页（显示用户信息）
- [x] 实现登出

### 2.4 授权确认页
- [x] 创建 consent.html（Thymeleaf）
- [x] 实现授权确认逻辑（第三方应用时 require_consent=true）
- [x] 记录 user_client_consent

---

## Phase 3: 权限控制

### 3.1 用户-系统访问权限
- [x] 创建 UserClientAccess 实体
- [x] 创建 UserClientAccessMapper
- [x] 实现权限检查逻辑
- [x] 在 /authorize 端点前拦截：检查用户是否有权访问该客户端

### 3.2 无权限处理
- [x] 创建无权限错误页（no_permission.html）
- [x] 返回友好提示："您没有权限访问 XXX 系统"

### 3.3 管理 API
- [x] GET /api/admin/users — 用户列表
- [x] PUT /api/admin/users/{id}/access — 设置用户系统权限
- [x] GET /api/admin/clients — 客户端列表

---

## Phase 4: Token 管理与撤销

### 4.1 Token 黑名单
- [x] 创建 OAuth2TokenBlacklist 实体
- [x] 创建 OAuth2TokenBlacklistMapper
- [x] 实现 TokenBlacklistService
  - addToBlacklist(jti, token, reason)
  - isBlacklisted(jti)
  - revokeAllUserTokens(userId)
- [x] JWT 验证时检查黑名单（JwtDecoder 自定义验证）

### 4.2 撤销场景实现
- [x] 管理员强制踢人：POST /api/admin/tokens/revoke
- [x] 用户登出时 Token 加入黑名单
- [x] 密码修改时撤销该用户所有 Token

### 4.3 Redis 集成
- [x] 黑名单优先查 Redis，查不到再查 DB
- [x] 黑名单写入时同步写 Redis + DB
- [x] 过期的黑名单记录定期清理

---

## Phase 5: 用户注册

### 5.1 注册功能
- [x] 创建注册页面（register.html）
- [x] 创建 RegisterController
- [x] 实现 RegisterService
  - 参数校验（用户名唯一、邮箱格式、密码强度）
  - 密码加密存储
  - 自动授权当前客户端

### 5.2 邮箱验证码
- [x] 实现邮件发送（MailService）
- [x] 验证码存 Redis（5分钟过期）
- [x] 验证码校验
- [x] 防刷：IP 限流（每小时最多 5 次）

### 5.3 注册安全
- [x] 用户名/邮箱格式校验
- [x] 密码强度要求（8位以上，含大小写+数字）
- [x] CSRF 防护

---

## Phase 6: 资源服务器

### 6.1 resource-api 项目
- [x] 创建 Spring Boot 项目（resource-api）
- [x] 配置 JWT 资源服务器（指向 Auth Server 的 jwks 端点）
- [x] 配置 SecurityConfig

### 6.2 业务 API 示例
- [x] GET /api/profile — 获取用户资料
- [x] GET /api/resources — 获取资源列表（示例）
- [x] 配置 scope 权限控制（如 SCOPE_profile 才能访问 /api/profile）

### 6.3 Token 黑名单检查
- [x] 资源服务器验证 JWT 时检查黑名单
- [x] 被撤销的 Token 返回 401

---

## Phase 7: Webhook 机制

### 7.1 Webhook 订阅管理
- [x] 创建 WebhookSubscriber 实体
- [x] 创建 WebhookSubscriberMapper
- [x] 实现 WebhookService（订阅管理）
- [x] 创建管理 API：GET/POST/DELETE /api/webhooks

### 7.2 事件发送
- [x] 实现事件触发：用户信息变更时发送 Webhook
- [x] 请求格式：POST {callback_url} + HMAC 签名
- [x] 签名算法：HMAC-SHA256(secret, timestamp + "." + body)

### 7.3 失败重试
- [x] 创建 WebhookLog 实体
- [x] 创建 WebhookLogMapper
- [x] 实现指数退避重试（1min, 5min, 30min, 2h）
- [x] 定时任务：扫描待重试的 Webhook

### 7.4 接收端示例
- [ ] 在 app-springboot 中实现 Webhook 接收端点
- [ ] 验证签名
- [ ] 更新本地用户快照表

---

## Phase 8: 前端完善

### 8.1 app-vue 完善
- [x] 首页（Home.vue）— 登录/未登录状态
- [x] 个人中心（Profile.vue）— 显示用户信息
- [x] Token 自动刷新（refresh_token）
- [x] 无权限提示页
- [x] 登出功能

### 8.2 Auth Server 页面完善
- [x] 登录页美化（Bootstrap/CSS）
- [x] 注册页美化
- [x] 授权确认页美化
- [x] 错误页美化
- [x] 显示来源系统 Logo 和名称（动态）

### 8.3 审计日志查询页
- [ ] 管理员查看审计日志
- [ ] 按用户/操作类型/时间筛选

---

## 依赖关系

```
Phase 1 (Auth Server 基础)
    │
    ├── Phase 2 (客户端接入) ── 依赖 Phase 1 完成
    │       │
    │       └── Phase 3 (权限控制) ── 依赖 Phase 2 完成
    │
    ├── Phase 4 (Token撤销) ── 依赖 Phase 1 完成
    │
    ├── Phase 5 (用户注册) ── 依赖 Phase 1 完成
    │
    └── Phase 6 (资源服务器) ── 依赖 Phase 1 完成
            │
            └── Phase 7 (Webhook) ── 依赖 Phase 2 + Phase 6 完成

Phase 8 (前端完善) ── 依赖 Phase 2 + Phase 3 + Phase 4 + Phase 5
```

## 验收标准

### Phase 1 验收
- [ ] Auth Server 启动成功
- [ ] 可以访问登录页面
- [ ] 管理员可以登录
- [ ] 可以访问 /oauth2/authorize 并获得授权码
- [ ] 可以用授权码换到 JWT token
- [ ] 可以访问 /userinfo 获取用户信息
- [ ] 审计日志正常记录

### Phase 2 验收
- [ ] app-vue 可以通过 PKCE 流程登录
- [ ] app-springboot 可以通过授权码流程登录
- [ ] 第三方应用显示授权确认页
- [ ] 内部应用自动授权（无确认页）

### Phase 3 验收
- [ ] 无权限用户访问被拒绝的系统，显示无权限页
- [ ] 管理员可以通过 API 设置用户系统权限

### Phase 4 验收
- [ ] 管理员可以撤销指定用户的 Token
- [ ] 被撤销的 Token 无法访问受保护资源
- [ ] 用户修改密码后，旧 Token 失效

### Phase 5 验收
- [ ] 用户可以自助注册
- [ ] 邮箱验证码正常发送和验证
- [ ] 注册成功自动登录

### Phase 6 验收
- [ ] resource-api 可以验证 JWT
- [ ] 带有效 Token 可以访问 API
- [ ] 无 Token 或无效 Token 返回 401

### Phase 7 验收
- [ ] 用户信息变更触发 Webhook
- [ ] 接收端正确验签并处理
- [ ] 失败自动重试

### Phase 8 验收
- [ ] 所有页面美观可用
- [ ] Token 自动刷新正常
- [ ] 审计日志可查询
