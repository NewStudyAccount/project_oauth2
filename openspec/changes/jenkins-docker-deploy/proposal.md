## Why

项目当前没有任何 CI/CD 流程，每次部署需要手动构建、手动传输 JAR、手动重启服务。随着服务数量增加（5 个后端 + 2 个前端），手动部署效率低且容易出错。需要通过 Jenkins + Docker 实现自动化部署，每个服务独立 Pipeline，手动触发构建。

## What Changes

- 为 5 个 Java 后端服务创建 Dockerfile（Multi-Stage Build: Maven 构建 + JRE 运行）
- 为 2 个 Vue 前端应用创建 Dockerfile（Multi-Stage Build: Node 构建 + Nginx 运行）
- 为每个前端创建 nginx.conf（SPA 路由 + API 反向代理）
- 为每个服务创建 Jenkinsfile（Checkout → Build → Docker Build → Stop Old → Run New → Health Check）
- 创建根目录 docker-compose.yml 统一编排基础设施（MySQL、Redis、Nacos）
- Docker 容器统一使用 oauth2-net 网络，容器名即服务域名

## Capabilities

### New Capabilities

> 本次变更属于基础设施/工具ing 变更，不涉及应用行为变化，跳过 specs。

### Modified Capabilities

无。本次变更是纯工具ing 变更，应用行为不变。

## Impact

**新增文件（14 个）：**
- `auth-center/backend/Dockerfile`
- `auth-center/backend/Jenkinsfile`
- `auth-center/auth-server-frontend/Dockerfile`
- `auth-center/auth-server-frontend/nginx.conf`
- `auth-center/auth-server-frontend/Jenkinsfile`
- `client-app/backend/Dockerfile`
- `client-app/backend/Jenkinsfile`
- `client-app/app-frontend/Dockerfile`
- `client-app/app-frontend/nginx.conf`
- `client-app/app-frontend/Jenkinsfile`
- `platform/gateway/Dockerfile`
- `platform/gateway/Jenkinsfile`
- `platform/user-service/Dockerfile`
- `platform/user-service/Jenkinsfile`
- `platform/resource-api/Dockerfile`
- `platform/resource-api/Jenkinsfile`

**修改文件（1 个）：**
- `docker-compose.yml`（根目录，统一编排）

**端口映射：**

| 服务 | 宿主机端口 |
|------|-----------|
| MySQL | 3306 |
| Redis | 6379 |
| Nacos | 8848 |
| gateway | 8080 |
| user-service | 8081 |
| client-app/backend | 8082 |
| resource-api | 8083 |
| auth-center/backend | 9000 |
| auth-frontend | 80 |
| app-frontend | 8084 |

**依赖关系：**
- 基础设施（MySQL、Redis、Nacos）必须先启动
- 后端服务依赖基础设施，可并行启动
- 前端依赖后端就绪
