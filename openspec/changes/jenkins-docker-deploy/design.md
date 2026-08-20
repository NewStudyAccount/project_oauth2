## Context

项目是一个基于 Spring Cloud 的 OAuth2 SSO 微服务系统，包含 5 个 Java 后端服务和 2 个 Vue 前端应用。当前没有任何 CI/CD 流程，基础设施（MySQL、Redis、Nacos）已有 docker-compose.yml。需要为所有业务服务添加 Docker 容器化支持和 Jenkins 自动化部署。

## Goals / Non-Goals

**Goals:**
- 每个服务独立 Dockerfile，支持 Multi-Stage Build（减小镜像体积）
- 每个服务独立 Jenkinsfile，手动触发构建
- 统一 Docker 网络（oauth2-net），容器名即服务域名
- 前端通过 Nginx 反向代理访问后端 API
- 一键启动基础设施（docker-compose.yml）

**Non-Goals:**
- 不实现自动触发（Webhook/Push Trigger）
- 不实现多环境（dev/staging/prod）
- 不搭建私有 Docker Registry（本地构建即可）
- 不迁移配置到 Nacos 配置中心
- 不实现 Kubernetes 部署

## Decisions

### 1. Dockerfile 采用 Multi-Stage Build

**决定**: 所有 Dockerfile 使用两阶段构建

**理由**:
- Java 服务：Maven 构建阶段 + JRE 运行阶段，镜像从 ~800MB 降至 ~200MB
- 前端：Node 构建阶段 + Nginx 运行阶段，镜像约 ~30MB
- 构建环境与运行环境分离，提高安全性

**替代方案**:
- 单阶段构建（简单但镜像大，包含完整 JDK/Node）
- 使用 Jib Maven Plugin（无需 Dockerfile，但灵活性低）

### 2. 前端使用 Nginx 容器独立部署

**决定**: 每个前端应用打包为独立的 Nginx 容器

**理由**:
- 前后端独立部署，互不影响
- Nginx 处理 SPA 路由（try_files）和 API 反向代理
- 可独立扩展和更新

**替代方案**:
- 前端打包到后端 static 目录（耦合，不灵活）
- 使用 CDN 部署前端（增加复杂度）

### 3. Jenkins Pipeline 使用声明式语法

**决定**: 使用 Declarative Pipeline（pipeline { }）而非 Scripted Pipeline

**理由**:
- 语法更清晰，易于维护
- 内置阶段可视化
- 更好的错误处理和重试机制

### 4. 容器命名和服务发现

**决定**: 使用固定容器名，Docker 网络内通过容器名访问

**理由**:
- 容器名即域名，无需额外配置
- 与现有 Nacos 服务发现兼容（服务注册时使用容器名）
- 简单直接，适合单机部署

**服务间访问方式**:
```
auth-server → http://user-service:8081 (Feign 调用)
gateway → lb://auth-server (Nacos 服务发现)
frontend → http://auth-server:9000 (Nginx 反向代理)
```

### 5. 端口映射策略

**决定**: 每个服务映射固定端口到宿主机

**理由**:
- 便于调试和访问
- 端口分配清晰，无冲突

**端口分配**:

| 服务 | 容器端口 | 宿主机端口 |
|------|---------|-----------|
| MySQL | 3306 | 3306 |
| Redis | 6379 | 6379 |
| Nacos | 8848 | 8848 |
| gateway | 8080 | 8080 |
| user-service | 8081 | 8081 |
| client-app | 8082 | 8082 |
| resource-api | 8083 | 8083 |
| auth-server | 9000 | 9000 |
| auth-ui | 80 | 80 |
| app-ui | 80 | 8084 |

### 6. 启动顺序管理

**决定**: 使用 docker-compose depends_on 管理基础设施启动顺序，后端服务通过健康检查等待依赖就绪

**理由**:
- 基础设施（MySQL → Nacos）有明确依赖关系
- 后端服务可并行启动，通过重试机制等待依赖
- 前端最后启动，等待后端就绪

## Risks / Trade-offs

**[风险] 服务启动顺序依赖** → 后端服务启动时 Nacos/MySQL 可能未就绪
- 缓解: 在 application.yml 中配置重试机制（spring.cloud.nacos.config.retry.enabled）
- 缓解: Jenkinsfile 中添加健康检查步骤

**[风险] 单点故障** → 所有服务部署在同一台虚拟机
- 缓解: 当前为开发/测试环境，可接受
- 后续: 可扩展到多节点 Docker Swarm 或 K8s

**[风险] 配置硬编码** → 数据库密码等敏感信息在配置文件中
- 缓解: 当前为开发环境，后续可通过环境变量或 Nacos 配置中心管理

**[权衡] 镜像体积 vs 构建速度** → Multi-Stage Build 增加构建时间但减小镜像
- 可接受: 构建频率低（手动触发），镜像体积更重要

**[权衡] 固定端口 vs 动态端口** → 固定端口可能冲突但便于管理
- 可接受: 单机部署，端口资源充足
