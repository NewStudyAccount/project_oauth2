# OAuth2 SSO 统一认证平台

基于 Spring Authorization Server 的 OAuth2/OIDC 统一认证平台，实现企业内部多系统单点登录，支持第三方应用通过 OAuth2 标准流程接入。

## 架构概览

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          系统架构                                       │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    Auth Server (:9000)                           │   │
│  │                                                                 │   │
│  │   Spring Authorization Server + Spring Security                 │   │
│  │   ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐      │   │
│  │   │ 用户管理  │  │ 客户端管理│  │ 会话管理  │  │ Webhook  │      │   │
│  │   └──────────┘  └──────────┘  └──────────┘  └──────────┘      │   │
│  │                                                                 │   │
│  │   端点:                                                        │   │
│  │   /oauth2/authorize  /oauth2/token  /oauth2/jwks               │   │
│  │   /userinfo  /.well-known/openid-configuration                 │   │
│  │   /login  /logout  /webhook/*                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│            │                    │                    │                  │
│            │                    │                    │                  │
│  ┌─────────▼──────┐  ┌─────────▼──────┐  ┌─────────▼──────┐          │
│  │    app-vue      │  │  app-springboot │  │  resource-api  │          │
│  │   (Vue SPA)     │  │  (后端应用)     │  │  (资源服务器)  │          │
│  │                 │  │                 │  │                │          │
│  │  公开客户端     │  │  机密客户端      │  │  验证JWT       │          │
│  │  PKCE 模式      │  │  授权码模式      │  │  返回业务数据  │          │
│  │  :5173          │  │  :8082          │  │  :8083         │          │
│  └─────────────────┘  └─────────────────┘  └────────────────┘          │
│                                                                         │
│  ┌─────────────────┐  ┌─────────────────┐                              │
│  │     MySQL        │  │     Redis        │                              │
│  │  用户/客户端/    │  │  Token缓存       │                              │
│  │  授权码/权限     │  │  Session存储     │                              │
│  └─────────────────┘  └─────────────────┘                              │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 认证中心 | Spring Authorization Server | 1.2.x |
| Web 框架 | Spring Boot | 3.2.x |
| 安全框架 | Spring Security | 6.2.x |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 7.x |
| 模板引擎 | Thymeleaf | 3.x |
| 前端 | Vue 3 + Vite + Pinia | 3.4+ |
| JWT | Nimbus JOSE JWT | 9.x |

## 快速启动

### 1. 配置 hosts 文件

将以下内容添加到 `C:\Windows\System32\drivers\etc\hosts` (管理员权限):

```
127.0.0.1  auth.local
```

### 2. 创建数据库

```sql
-- 执行 auth-server/src/main/resources/db/schema.sql
-- 执行 auth-server/src/main/resources/db/data.sql
```

或启动 auth-server 时自动执行 (需确保 MySQL 运行且密码正确)。

### 3. 启动认证中心

```bash
cd auth-server
mvn spring-boot:run
```

访问 http://auth.local:9000

### 4. 启动 Vue 前端

```bash
cd app-vue
npm install
npm run dev
```

访问 http://localhost:5173

### 5. 启动 Spring Boot 应用

```bash
cd app-springboot
mvn spring-boot:run
```

访问 http://localhost:8082

### 6. 启动资源服务器

```bash
cd resource-api
mvn spring-boot:run
```

访问 http://localhost:8083

## 测试账号

| 用户名 | 密码 | 说明 |
|--------|------|------|
| admin | Admin@123 | 系统管理员，可访问所有应用 |

## SSO 演示流程

### 流程一：Vue SPA (PKCE 公开客户端)

1. 访问 http://localhost:5173，点击"登录"
2. 在 auth.local 登录页面输入账号密码
3. 登录成功后，Vue 前端显示用户信息
4. 可以查看个人中心、刷新 Token、登出

### 流程二：Spring Boot 应用 (机密客户端)

1. 访问 http://localhost:8082，点击"登录"
2. 在 auth.local 登录页面输入账号密码
3. 登录成功后，自动跳转回应用首页
4. 可以查看个人中心、登出

### 流程三：第三方应用 (需授权确认)

1. 使用 third-party-app 客户端发起授权请求
2. 登录后显示授权确认页面
3. 用户同意授权后，获取授权码
4. 用授权码换取 Token

## 项目结构

```
project_oauth2/
├── auth-server/          # 认证中心 (Spring Boot + Auth Server)
│   ├── src/main/java/
│   │   ├── config/       # 配置类
│   │   ├── controller/   # 控制器
│   │   ├── entity/       # 实体类
│   │   ├── repository/   # MyBatis Mapper
│   │   └── service/      # 业务逻辑
│   └── src/main/resources/
│       ├── db/           # 数据库脚本
│       ├── templates/    # Thymeleaf 模板
│       └── static/       # 静态资源
├── app-vue/              # Vue SPA 前端 (PKCE 公开客户端)
│   └── src/
│       ├── utils/        # 工具函数 (PKCE, Auth)
│       ├── stores/       # Pinia 状态管理
│       └── views/        # 页面组件
├── app-springboot/       # Spring Boot 应用 (机密客户端)
│   └── src/main/java/
│       ├── config/       # OAuth2 Client 配置
│       └── controller/   # 控制器
└── resource-api/         # 资源服务器 (JWT 验证)
    └── src/main/java/
        ├── config/       # Resource Server 配置
        └── controller/   # API 控制器
```

## 端口分配

| 服务 | 端口 | 说明 |
|------|------|------|
| auth-server | 9000 | 认证中心 |
| app-vue | 5173 | Vue SPA 前端 |
| app-springboot | 8082 | Spring Boot 应用 |
| resource-api | 8083 | 资源服务器 |

## OAuth2 流程说明

### 公开客户端 (PKCE)
```
Browser → Vue App → Auth Server → 登录 → 回调 → 换 Token → 完成
```
- 适用于前端 SPA 应用
- 使用 PKCE 保证安全性
- 不需要 client_secret

### 机密客户端 (授权码)
```
Browser → SpringBoot App → Auth Server → 登录 → 回调 → 换 Token → 完成
```
- 适用于后端应用
- 需要 client_secret
- Token 存储在服务端

### 第三方应用 (需确认)
```
Browser → 第三方 App → Auth Server → 登录 → 授权确认 → 回调 → 换 Token → 完成
```
- 需要用户确认授权
- 支持 scope 权限控制

## API 文档

### OAuth2 端点
- `GET /oauth2/authorize` - 授权端点
- `POST /oauth2/token` - Token 端点
- `POST /oauth2/revoke` - Token 撤销
- `GET /oauth2/jwks` - 公钥
- `GET /.well-known/openid-configuration` - OIDC 发现

### 用户信息
- `GET /userinfo` - 获取用户信息 (需要 Bearer Token)

### 管理 API
- `GET /api/admin/users` - 用户列表
- `PUT /api/admin/users/{id}/access` - 设置用户系统权限
- `POST /api/admin/tokens/revoke` - 撤销 Token
- `GET /api/admin/clients` - 客户端列表

### Webhook
- `GET /api/webhooks` - 订阅列表
- `POST /api/webhooks` - 创建订阅
- `DELETE /api/webhooks/{id}` - 删除订阅

### 资源服务器 API
- `GET /api/profile` - 获取用户资料
- `GET /api/resources` - 获取资源列表

## Token 黑名单机制

支持主动撤销 Token：
- 管理员强制踢人下线
- 用户登出时 Token 加入黑名单
- 密码修改时所有 Token 失效

黑名单存储：Redis + MySQL 双写，优先查 Redis
