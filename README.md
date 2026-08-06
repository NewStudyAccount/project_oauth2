# OAuth2 SSO 学习项目

生产级跨域单点登录 (SSO) 系统，基于 Spring Authorization Server + Vue3 + MyBatis-Plus 实现。

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                        浏览器                                │
│                                                             │
│  ┌───────────────┐              ┌───────────────┐           │
│  │ app-a.local   │              │ app-b.local   │           │
│  │ Vue SPA :5173 │              │ Spring Boot   │           │
│  │ (PKCE)        │              │ :8082         │           │
│  └───────┬───────┘              └───────┬───────┘           │
│          │                              │                   │
│          └──────────┬───────────────────┘                   │
│                     ▼                                       │
│          ┌──────────────────┐                               │
│          │ auth.local:9000  │                               │
│          │ 认证中心          │                               │
│          │ Spring Auth Server│                              │
│          └──────────────────┘                               │
└─────────────────────────────────────────────────────────────┘
```

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 认证中心 | Spring Authorization Server | 1.2+ |
| Web 框架 | Spring Boot | 3.2+ |
| ORM | MyBatis-Plus | 3.5+ |
| 数据库 | MySQL | 8.0+ |
| 前端 | Vue 3 + Vite | 3.4+ |
| 状态管理 | Pinia | 2.1+ |
| HTTP 客户端 | Axios | 1.7+ |

## 快速启动

### 1. 配置 hosts 文件

将以下内容添加到 `C:\Windows\System32\drivers\etc\hosts` (管理员权限):

```
127.0.0.1  auth.local
127.0.0.1  app-a.local
127.0.0.1  app-b.local
127.0.0.1  api.local
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

访问 http://app-a.local:5173

### 5. 启动 Client-B

```bash
cd client-b
mvn spring-boot:run
```

访问 http://app-b.local:8082

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | ROLE_USER, ROLE_ADMIN |
| user | 123456 | ROLE_USER |

## SSO 演示流程

1. 访问 http://app-a.local:5173，点击"登录"
2. 在 auth.local 登录页面输入账号密码
3. 登录成功后，Vue 前端显示用户信息
4. 打开新标签页，访问 http://app-b.local:8082
5. **无需输入密码**，Client-B 自动登录成功！

这就是跨域 SSO 的效果。

## 项目结构

```
project-oauth2/
├── auth-server/          # 认证中心 (Spring Boot + Auth Server)
├── app-vue/              # Vue SPA 客户端 (PKCE)
├── client-b/             # Spring Boot 客户端 (OAuth2 Client)
├── resource-server/      # 资源服务器 (预留)
└── hosts.txt             # hosts 配置说明
```

## 端口分配

| 服务 | 端口 | 域名 |
|------|------|------|
| auth-server | 9000 | auth.local |
| app-vue | 5173 | app-a.local |
| client-b | 8082 | app-b.local |
| resource-server | 8083 | api.local |
