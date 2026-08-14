## Context

当前项目 `project_oauth2` 包含 7 个子项目，全部平铺在根目录下：

```
project_oauth2/
├── auth-server/          (后端, Maven module)
├── admin-vue3/           (前端, 独立)
├── app-springboot/       (后端, Maven module)
├── app-vue3-springboot/  (前端, 独立)
├── app-vue/              (前端, 独立)
├── gateway/              (后端, Maven module)
├── resource-api/         (后端, Maven module)
├── hosts.txt             (散落根目录)
└── pom.xml               (根 pom, 4 个 modules)
```

系统归属关系：
- **认证中心**: auth-server + admin-vue3
- **外部系统A**: app-springboot + app-vue3-springboot
- **外部系统B**: app-vue (纯前端)
- **平台层**: gateway + resource-api

约束：
- Spring Boot 3.2.5 + Java 21
- Maven 多模块构建
- 各服务端口和域名配置不变
- openspec 变更管理目录不移动

## Goals / Non-Goals

**Goals:**
- 按系统领域分组，同一系统的前后端紧邻
- 目录名即系统边界，降低认知负担
- 修正 app-vue 的 API 调用目标错误
- 集中管理基础设施配置
- 保持 Maven 构建正常工作

**Non-Goals:**
- 不引入 pnpm monorepo（前端项目技术栈不同，无共享需求）
- 不创建 common 后端共享模块（当前后端间零互相引用）
- 不修改任何 application.yml 或 vite.config 中的端口/域名配置
- 不修改 Java 包名或 artifactId
- 不重构现有代码逻辑

## Decisions

### Decision 1: 按领域分组而非按层分组

**选择**: 方案 B（领域优先）— `auth-center/`、`client-app/`、`standalone-app/`、`platform/`

**替代方案**: 方案 A（层优先）— `backend/`、`frontend/` 分开

**理由**: 本项目本质是多系统并行，每个系统相对独立。按领域分组让同一系统的前后端紧邻，修改时上下文完整，认知负担低。按层分组虽然构建一致性好，但同一系统的前后端分在不同顶层目录，需要记住映射关系。

### Decision 2: 后端子目录统一命名为 backend/，前端统一命名为 frontend/

**选择**: 每个系统目录下用 `backend/` 和 `frontend/` 子目录

**替代方案**: 保留原项目名如 `auth-server/`、`admin-vue3/`

**理由**: 统一命名规范，进入任何系统目录都能立刻识别前后端。原命名虽然更具体，但在领域目录下已提供上下文（`auth-center/backend/` 比 `auth-center/auth-server/` 更清晰）。

### Decision 3: app-vue 的 API 调用目标改为 gateway

**选择**: `API_BASE_URL = 'http://gateway.local:8080/api'`

**替代方案A**: 直接调 resource-api (`http://resource-api:8083`)
**替代方案B**: 保持现状调 app-springboot

**理由**: 通过 gateway 调用符合网关统一入口理念，可享受限流/熔断/CORS 统一处理，且只需知道 gateway 地址。直接调 resource-api 绕过了网关，需要额外 CORS 配置。保持现状是错误的（app-vue 和 app-springboot 是独立系统）。

### Decision 4: 基础设施配置移入 infra/ 目录

**选择**: 新建 `infra/` 目录，移入 `hosts.txt`，新建 `docker-compose.yml`

**理由**: 基础设施配置不属于任何业务系统，应独立管理。hosts.txt 当前散落在根目录，docker-compose 未来必定需要。

### Decision 5: Maven 模块路径使用嵌套路径

**选择**: 根 pom 的 `<modules>` 使用嵌套路径如 `auth-center/backend`

**理由**: Maven 完全支持任意深度的模块路径。每个子模块的 `<parent>` 仍指向根 pom，`<artifactId>` 不变，只是物理目录位置变了。

## Risks / Trade-offs

- **[IDE 重新导入]** → 需要重新导入 Maven 项目，IntelliJ 会自动识别新路径
- **[Git 历史断裂风险]** → 使用 `git mv` 而非删除+新建，保留文件历史
- **[现有 openspec changes 路径引用失效]** → tasks.md 中引用的文件路径需手动更新，但现有 change 大部分已完成，影响有限
- **[app-vue 通过 gateway 调用增加一跳]** → 开发环境延迟可忽略，生产环境 gateway 本就是必经之路
- **[前端无 monorepo]** → 三个前端技术栈不同（Vue3/Vue3/Vue2），共享价值低，独立管理更简单

## Migration Plan

1. 使用 `git mv` 逐个移动目录（保留历史）
2. 更新根 pom.xml 的 `<modules>` 路径
3. 修正 app-vue 的 api.js
4. 移动 hosts.txt 到 infra/
5. 更新 .gitignore
6. 更新 architecture.md
7. 执行 `mvn validate` 验证 Maven 构建正常
8. 各前端执行 `npm install` 验证正常

**回滚**: 反向 `git mv` 恢复原目录结构，恢复 pom.xml

## Open Questions

- gateway 的域名当前配置中未定义 `gateway.local`，app-vue 改为通过 gateway 调用后，需要在 hosts.txt 中添加 `127.0.0.1 gateway.local`，并确认 gateway 的 CORS 配置允许 `client.b.local:5173` 的请求