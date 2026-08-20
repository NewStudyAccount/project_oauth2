# OAuth2 统一认证与单点登录平台

基于 **Spring Cloud + Nacos + Spring Authorization Server** 构建的 OAuth2 SSO 平台，采用 Java 21 + Spring Boot 3.2.5 + Vue 3 技术栈。

---

## 系统架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              前端客户端层                                     │
│                                                                             │
│   admin-vue3 (:5174)          app-vue3 (:5173)         standalone-app       │
│   管理后台前端                  client-app 前端           独立 SPA 前端        │
│   (公开客户端, PKCE)            (公开客户端, PKCE)        (公开客户端, PKCE)    │
└───────────┬─────────────────────────┬─────────────────────────┬─────────────┘
            │                         │                         │
            │ Bearer Token            │ Bearer Token            │ Bearer Token
            ▼                         ▼                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              网关层                                          │
│                                                                             │
│                    Spring Cloud Gateway (:8080)                             │
│                    ├── /auth/** → auth-server (lb)                          │
│                    ├── /api/**  → resource-api (lb)                         │
│                    └── Token 中继过滤器                                      │
│                    └── Nacos 服务发现                                        │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
            ┌─────────────────────┼─────────────────────┐
            ▼                     ▼                     ▼
┌───────────────────┐  ┌───────────────────┐  ┌───────────────────┐
│  auth-server      │  │  resource-api     │  │  app-springboot   │
│  (:9000)          │  │  (:8083)          │  │  (:8082)          │
│  认证中心          │  │  资源服务          │  │  客户端应用        │
│  OAuth2 + OIDC    │  │  JWT 校验          │  │  JWT 校验          │
│  JWT 签发          │  │  受保护 API        │  │  业务 API          │
└────────┬──────────┘  └───────────────────┘  └────────┬──────────┘
         │                                              │
         │ Feign                                        │ Feign
         ▼                                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     user-service (:8081)                         │
│                     用户中心（独立服务，独立数据库）                  │
│                     用户信息查询 / 注册 / 管理                     │
└─────────────────────────────────────────────────────────────────┘
                                  │
            ┌─────────────────────┼─────────────────────┐
            ▼                     ▼                     ▼
┌───────────────────┐  ┌───────────────────┐  ┌───────────────────┐
│  MySQL (:3306)    │  │  Redis (:6379)    │  │  Nacos (:8848)    │
│  oauth2_center    │  │  验证码/限流/      │  │  服务注册/配置      │
│  user_center      │  │  Token 黑名单/Session│ │                   │
└───────────────────┘  └───────────────────┘  └───────────────────┘
```

---

## 认证流程

### 公开客户端（PKCE）登录流程

```
1. 前端生成 code_verifier + code_challenge
2. 跳转 auth-center /oauth2/authorize
3. 用户在认证中心登录
4. 回调前端 /callback?code=xxx
5. 前端用 code + code_verifier 换 Token
6. Token 存入 localStorage
7. 请求 API 带 Authorization: Bearer <token>
8. 后端校验 JWT（issuer + aud + 签名）
```

### JWT Audience 隔离

每个客户端签发的 Token 携带 `aud` 声明，资源服务只接受匹配的 Token：

| 客户端 | aud | 可访问 |
|--------|-----|--------|
| admin-frontend | auth-center-admin | auth-center 管理 API |
| springboot-app | springboot-app | client-app 后端 API |
| vue-app | vue-app | resource-api |

---

## 项目文件结构

```
project_oauth2/
├── pom.xml                                    # 根 Maven POM（聚合 5 个后端模块）
│
├── auth-center/                               # 认证中心
│   ├── backend/                               # Spring Boot 后端（端口 9000）
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/example/authserver/
│   │       │   ├── AuthServerApplication.java
│   │       │   ├── config/                    # 安全配置（7 个文件）
│   │       │   │   ├── AuthorizationServerConfig.java    # OAuth2 授权服务器核心
│   │       │   │   ├── SecurityConfig.java               # Web 安全 + API JWT Resource Server
│   │       │   │   ├── CorsConfig.java                   # 跨域配置
│   │       │   │   ├── OAuth2TokenCustomizerConfig.java  # JWT 自定义声明
│   │       │   │   ├── EnabledCheckingRegisteredClientRepository.java
│   │       │   │   ├── MybatisPlusConfig.java
│   │       │   │   └── MyMetaObjectHandler.java
│   │       │   ├── controller/                # 控制器（7 个文件）
│   │       │   │   ├── AdminController.java
│   │       │   │   ├── ConsentController.java
│   │       │   │   ├── LoginController.java
│   │       │   │   ├── RegisterController.java
│   │       │   │   ├── UserInfoController.java
│   │       │   │   ├── WebhookController.java
│   │       │   │   └── GlobalExceptionHandler.java
│   │       │   ├── service/                   # 服务层（7 个文件）
│   │       │   │   ├── CustomUserDetailsService.java
│   │       │   │   ├── RegisterService.java
│   │       │   │   ├── AccessControlService.java
│   │       │   │   ├── AuditLogService.java
│   │       │   │   ├── TokenBlacklistService.java
│   │       │   │   ├── MailService.java
│   │       │   │   └── WebhookService.java
│   │       │   ├── entity/                    # 实体类（8 个文件）
│   │       │   ├── repository/                # MyBatis-Plus Mapper（7 个文件）
│   │       │   ├── dto/                       # DTO（ClientDTO, UserDTO, ClientConverter）
│   │       │   └── client/                    # Feign 客户端（UserServiceClient）
│   │       └── resources/
│   │           ├── application.yml
│   │           ├── db/                        # SQL 脚本
│   │           ├── static/css/                # 样式
│   │           └── templates/                 # Thymeleaf 页面
│   │
│   └── auth-server-frontend/                  # 管理后台前端（Vue3 + TS + Element Plus）
│       ├── vite.config.ts
│       └── src/
│           ├── api/                           # API 模块（client, user, access, audit）
│           ├── components/AppLayout.vue
│           ├── router/index.js
│           ├── stores/auth.js                 # Pinia Token 管理
│           ├── utils/                         # PKCE + auth 工具
│           └── views/                         # 页面（Dashboard, Client, User, Access, Audit）
│
├── client-app/                                # 客户端应用（前后端分离）
│   ├── backend/                               # Spring Boot 后端（端口 8082）
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/example/appspringboot/
│   │       │   ├── AppSpringbootApplication.java
│   │       │   ├── config/SecurityConfig.java        # 纯 Resource Server
│   │       │   ├── controller/                       # API 控制器
│   │       │   ├── client/UserServiceClient.java     # Feign 调用 user-service
│   │       │   └── dto/UserDTO.java
│   │       └── resources/application.yml
│   │
│   └── app-frontend/                          # Vue3 前端（公开客户端）
│       ├── vite.config.js
│       └── src/
│           ├── api/index.js                   # Axios + Bearer Token
│           ├── router/index.js
│           ├── stores/auth.js                 # Pinia Token 管理
│           ├── utils/                         # PKCE + auth 工具
│           └── views/                         # Home, Profile, Callback
│
├── standalone-app/                            # 独立 SPA（纯前端）
│   └── frontend/                              # Vue3 前端（公开客户端）
│       ├── vite.config.js
│       └── src/
│           ├── router/index.js
│           ├── stores/auth.js
│           ├── utils/                         # api, auth, pkce
│           └── views/                         # Home, Callback, Profile, ApiDemo, NoPermission
│
├── platform/                                  # 平台服务
│   ├── gateway/                               # Spring Cloud Gateway（端口 8080）
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/example/gateway/
│   │       │   ├── GatewayApplication.java
│   │       │   ├── config/SecurityConfig.java         # 响应式安全（WebFlux）
│   │       │   └── filter/TokenRelayGlobalFilter.java # Token 中继
│   │       └── resources/application.yml
│   │
│   ├── resource-api/                          # 资源服务（端口 8083）
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/example/resourceapi/
│   │       │   ├── ResourceApiApplication.java
│   │       │   ├── config/SecurityConfig.java
│   │       │   └── controller/ApiController.java
│   │       └── resources/application.yml
│   │
│   └── user-service/                          # 用户中心（端口 8081）
│       ├── pom.xml
│       └── src/main/
│           ├── java/com/example/userservice/
│           │   ├── UserServiceApplication.java
│           │   ├── controller/UserController.java
│           │   ├── service/UserService.java
│           │   ├── entity/SysUser.java
│           │   └── repository/SysUserMapper.java
│           └── resources/
│               ├── application.yml
│               └── db/                        # schema.sql, migrate-data.sql
│
├── infra/                                     # 基础设施
│   ├── docker-compose.yml                     # MySQL + Redis + Nacos
│   └── hosts.txt                              # 域名映射
│
├── docs/                                      # 文档
│   ├── architecture-spring-cloud.md
│   ├── auth-server-core-analysis.md
│   ├── auth-server-storage-extension.md
│   ├── client-creation-guide.md
│   └── oauth2-client-types.md                 # 公开客户端 vs 机密客户端详解
│
└── openspec/                                  # 变更管理
    └── changes/                               # 变更提案
        ├── extract-user-center-service/
        ├── user-service-standalone/
        ├── admin-api-jwt-auth/
        ├── client-app-public-client/
        └── ...（更多变更）
```

---

## 端口分配

| 端口 | 服务 | 说明 |
|------|------|------|
| 3306 | MySQL | 数据库（oauth2_center, user_center） |
| 5173 | client-app 前端 | Vite 开发服务器 |
| 5174 | admin 前端 | Vite 开发服务器 |
| 6379 | Redis | 缓存 |
| 8080 | Spring Cloud Gateway | API 网关 |
| 8081 | user-service | 用户中心 |
| 8082 | client-app 后端 | Spring Boot |
| 8083 | resource-api | 资源服务 |
| 8848 | Nacos | 服务注册/配置 |
| 9000 | auth-server | 认证中心 |

---

## 域名映射

将以下内容添加到 `C:\Windows\System32\drivers\etc\hosts`（Windows）或 `/etc/hosts`（Linux/Mac）：

```
127.0.0.1  auth.local        # 认证中心
127.0.0.1  client.a.local    # client-app
127.0.0.1  client.b.local    # standalone-app
127.0.0.1  gateway.local     # API 网关
```

---

## 快速启动

### 1. 启动基础设施

```bash
cd infra
docker-compose up -d
```

等待 MySQL、Redis、Nacos 启动完成。

### 2. 初始化数据库

```bash
# 执行 auth-center 的 schema.sql 和 data.sql
mysql -h 192.168.99.100 -u root -p123456 < auth-center/backend/src/main/resources/db/schema.sql
mysql -h 192.168.99.100 -u root -p123456 < auth-center/backend/src/main/resources/db/data.sql

# 执行 user-service 的 schema.sql
mysql -h 192.168.99.100 -u root -p123456 < platform/user-service/src/main/resources/db/schema.sql
```

### 3. 启动后端服务

```bash
# 认证中心
cd auth-center/backend && mvn spring-boot:run

# 用户中心
cd platform/user-service && mvn spring-boot:run

# client-app 后端
cd client-app/backend && mvn spring-boot:run

# 资源服务
cd platform/resource-api && mvn spring-boot:run

# API 网关
cd platform/gateway && mvn spring-boot:run
```

### 4. 启动前端

```bash
# admin 前端
cd auth-center/auth-server-frontend && npm install && npm run dev

# client-app 前端
cd client-app/app-frontend && npm install && npm run dev

# standalone-app 前端
cd standalone-app/frontend && npm install && npm run dev
```

---

## OAuth2 客户端配置

所有客户端配置存储在数据库 `oauth2_registered_client` 表中：

| client_id | 类型 | 认证方式 | PKCE | 用途 |
|-----------|------|----------|------|------|
| admin-frontend | 公开客户端 | none | ✅ | 管理后台前端 |
| springboot-app | 公开客户端 | none | ✅ | client-app 前端 |
| vue-app | 公开客户端 | none | ✅ | standalone-app |
| gateway-app | 机密客户端 | client_secret_basic | ❌ | API 网关 |

---

## 技术栈

| 层 | 技术 |
|---|---|
| **后端框架** | Spring Boot 3.2.5 + Spring Cloud 2023.0.1 + Spring Cloud Alibaba 2023.0.1.0 |
| **认证框架** | Spring Authorization Server（OAuth2 + OIDC） |
| **网关** | Spring Cloud Gateway（响应式 WebFlux） |
| **服务发现** | Nacos |
| **ORM** | MyBatis-Plus（MySQL） |
| **缓存** | Redis（验证码、限流、Token 黑名单、Session） |
| **前端** | Vue 3 + Vite + Pinia + Element Plus |
| **容器化** | Docker Compose（MySQL + Redis + Nacos） |
| **Java 版本** | JDK 21 |

---

## 文档

| 文档 | 说明 |
|------|------|
| [OAuth2 客户端类型详解](docs/oauth2-client-types.md) | 公开客户端 vs 机密客户端、SSO + aud 隔离 |
| [Spring Cloud 架构](docs/architecture-spring-cloud.md) | 微服务架构设计 |
| [认证服务器核心分析](docs/auth-server-core-analysis.md) | auth-server 内部机制 |
| [存储扩展](docs/auth-server-storage-extension.md) | 持久化方案 |
| [客户端创建指南](docs/client-creation-guide.md) | 如何注册新客户端 |
