## 1. Java 后端 Dockerfile

- [x] 1.1 创建 `platform/gateway/Dockerfile`（Multi-Stage: Maven + JRE 21 Alpine）
- [x] 1.2 创建 `platform/user-service/Dockerfile`
- [x] 1.3 创建 `platform/resource-api/Dockerfile`
- [x] 1.4 创建 `auth-center/backend/Dockerfile`
- [x] 1.5 创建 `client-app/backend/Dockerfile`

## 2. 前端 Dockerfile 和 Nginx 配置

- [x] 2.1 创建 `auth-center/auth-server-frontend/nginx.conf`（SPA 路由 + API 反向代理到 oauth2-auth:9000）
- [x] 2.2 创建 `auth-center/auth-server-frontend/Dockerfile`（Multi-Stage: Node + Nginx）
- [x] 2.3 创建 `client-app/app-frontend/nginx.conf`（SPA 路由 + API 反向代理到 oauth2-client:8082）
- [x] 2.4 创建 `client-app/app-frontend/Dockerfile`

## 3. Jenkinsfile（Java 后端）

- [x] 3.1 创建 `platform/gateway/Jenkinsfile`（Checkout → Maven Build → Docker Build → Stop Old → Run New → Health Check）
- [x] 3.2 创建 `platform/user-service/Jenkinsfile`
- [x] 3.3 创建 `platform/resource-api/Jenkinsfile`
- [x] 3.4 创建 `auth-center/backend/Jenkinsfile`
- [x] 3.5 创建 `client-app/backend/Jenkinsfile`

## 4. Jenkinsfile（前端）

- [x] 4.1 创建 `auth-center/auth-server-frontend/Jenkinsfile`（Checkout → npm Build → Docker Build → Stop Old → Run New）
- [x] 4.2 创建 `client-app/app-frontend/Jenkinsfile`

## 5. Docker Compose 编排

- [x] 5.1 创建根目录 `docker-compose.yml`（基础设施：MySQL、Redis、Nacos + oauth2-net 网络）
- [x] 5.2 更新 `infra/docker-compose.yml` 或合并到根目录

## 6. 文档

- [x] 6.1 更新 `README.md` 添加 Jenkins 部署说明
