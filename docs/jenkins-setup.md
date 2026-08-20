# Jenkins 部署配置指南

## 前置条件

- Jenkins 已安装并配置好以下工具：
  - Maven（名称：`Maven` 或 `Maven 3`）
  - Node.js（名称：`NodeJS`）
  - Docker（确保 Jenkins 用户有 Docker 执行权限）
- Git 仓库可访问：`https://github.com/NewStudyAccount/project_oauth2.git`
- 目标服务器已安装 Docker 并创建 `oauth2-net` 网络

```bash
# 创建 Docker 网络
docker network create oauth2-net
```

---

## Jenkins 项目配置

### 创建步骤

```
1. Jenkins → 新建任务
2. 输入任务名（见下表）
3. 选择 "Pipeline" → 确定
4. Pipeline 配置：
   - Definition: Pipeline script from SCM
   - SCM: Git
   - Repository URL: https://github.com/NewStudyAccount/project_oauth2.git
   - Branch Specifier: */master
   - Script Path: （见下表）
5. 保存
```

### 项目列表

| Jenkins 任务名 | Script Path | 说明 | 端口 |
|---------------|-------------|------|------|
| `oauth2-gateway` | `platform/gateway/Jenkinsfile` | API 网关 | 8080 |
| `oauth2-auth` | `auth-center/backend/Jenkinsfile` | 认证中心 | 9000 |
| `oauth2-user` | `platform/user-service/Jenkinsfile` | 用户中心 | 8081 |
| `oauth2-client` | `client-app/backend/Jenkinsfile` | 客户端后端 | 8082 |
| `oauth2-resource` | `platform/resource-api/Jenkinsfile` | 资源服务 | 8083 |
| `oauth2-auth-ui` | `auth-center/auth-server-frontend/Jenkinsfile` | 认证前端 (Nginx) | 80 |
| `oauth2-app-ui` | `client-app/app-frontend/Jenkinsfile` | 应用前端 (Nginx) | 8084 |

---

## 部署顺序

### 第一步：启动基础设施

```bash
# 在项目根目录执行
docker-compose up -d
```

等待 MySQL、Redis、Nacos 启动完成（约 30 秒）。

### 第二步：初始化数据库

```bash
mysql -h <服务器IP> -u root -p123456 < auth-center/backend/src/main/resources/db/schema.sql
mysql -h <服务器IP> -u root -p123456 < auth-center/backend/src/main/resources/db/data.sql
mysql -h <服务器IP> -u root -p123456 < platform/user-service/src/main/resources/db/schema.sql
```

### 第三步：部署后端服务

在 Jenkins 中依次点击 "Build Now"（可并行）：

1. `oauth2-auth` → 认证中心
2. `oauth2-user` → 用户中心
3. `oauth2-client` → 客户端后端
4. `oauth2-resource` → 资源服务
5. `oauth2-gateway` → API 网关

### 第四步：部署前端

1. `oauth2-auth-ui` → 认证前端
2. `oauth2-app-ui` → 应用前端

---

## Pipeline 流程

每个 Jenkinsfile 执行以下阶段：

```
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│ Checkout │──▶│  Build   │──▶│  Docker  │──▶│  Stop    │──▶│   Run    │──▶│  Health  │
│  (Git)   │   │          │   │  Build   │   │   Old    │   │   New    │   │  Check   │
└──────────┘   └──────────┘   └──────────┘   └──────────┘   └──────────┘   └──────────┘
```

- **后端**：Maven Build → Docker Build → Stop Old → Run New → Health Check
- **前端**：npm Build → Docker Build → Stop Old → Run New → Health Check

---

## 访问地址

部署完成后，通过以下地址访问：

| 服务 | 访问地址 |
|------|---------|
| 认证前端 | `http://<服务器IP>` |
| 应用前端 | `http://<服务器IP>:8084` |
| API 网关 | `http://<服务器IP>:8080` |
| 认证中心 | `http://<服务器IP>:9000` |
| Nacos 控制台 | `http://<服务器IP>:8848/nacos` |

---

## 容器管理

```bash
# 查看所有容器
docker ps -a

# 查看容器日志
docker logs -f oauth2-auth

# 重启容器
docker restart oauth2-auth

# 进入容器
docker exec -it oauth2-auth sh

# 停止所有业务容器
docker stop oauth2-auth oauth2-user oauth2-gateway oauth2-client oauth2-resource oauth2-auth-ui oauth2-app-ui

# 启动基础设施
docker-compose up -d
```

---

## 端口分配总览

| 端口 | 容器名 | 服务 |
|------|--------|------|
| 3306 | oauth2-mysql | MySQL |
| 6379 | oauth2-redis | Redis |
| 8848 | oauth2-nacos | Nacos |
| 80 | oauth2-auth-ui | 认证前端 |
| 8080 | oauth2-gateway | API 网关 |
| 8081 | oauth2-user | 用户中心 |
| 8082 | oauth2-client | 客户端后端 |
| 8083 | oauth2-resource | 资源服务 |
| 8084 | oauth2-app-ui | 应用前端 |
| 9000 | oauth2-auth | 认证中心 |
