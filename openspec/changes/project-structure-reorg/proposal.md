## Why

当前项目 7 个子项目（4 后端 + 3 前端）全部平铺在根目录，前后端混放且缺乏系统边界划分。这导致：同一系统的前后端分散在不同目录，修改时需要跨目录切换上下文；前端项目不在 Maven 构建体系中也没有统一的前端 workspace 管理；`app-vue` 的 API 调用错误地指向了 `app-springboot` 而非 `resource-api` 或 `gateway`；基础设施配置（hosts、docker-compose）散落在根目录。按领域分组可以让每个系统自包含，目录即系统边界。

## What Changes

- **BREAKING**: 将 7 个子项目按系统归属重组到 4 个顶层目录（auth-center、client-app、standalone-app、platform）
- **BREAKING**: 根 pom.xml 的 `<modules>` 路径更新为新目录结构
- 将 `auth-server` 移至 `auth-center/backend/`，`admin-vue3` 移至 `auth-center/frontend/`
- 将 `app-springboot` 移至 `client-app/backend/`，`app-vue3-springboot` 移至 `client-app/frontend/`
- 将 `app-vue` 移至 `standalone-app/frontend/`
- 将 `gateway` 移至 `platform/gateway/`，`resource-api` 移至 `platform/resource-api/`
- 新建 `infra/` 目录，集中管理 `hosts.txt` 和 `docker-compose.yml`
- 修正 `app-vue/src/utils/api.js` 中 `API_BASE_URL`，从 `app-springboot` 改为 `gateway`
- 更新 `architecture.md` 反映新项目结构
- 更新 `.gitignore` 适配新目录层级

## Capabilities

### New Capabilities
- `project-structure`: 按领域分组的项目目录结构规范，定义各系统的目录归属和命名约定
- `infra-config`: 基础设施配置集中管理（hosts、docker-compose、SQL 初始化）

### Modified Capabilities
- `external-frontend-integration`: app-vue 的 API 调用目标从 app-springboot 修正为 gateway

## Impact

- **Maven 构建**: 根 pom 的 `<modules>` 路径变更，所有后端子模块的目录位置改变（模块名和 artifactId 不变）
- **IDE 配置**: 需要重新导入 Maven 项目以识别新路径
- **Git 历史**: 使用 `git mv` 保留文件历史
- **app-vue**: `src/utils/api.js` 中 `API_BASE_URL` 变更，影响 API 调用目标
- **CI/CD**: 如有构建脚本引用原路径，需同步更新
- **openspec changes**: 现有变更中的文件路径引用可能需要更新
- **application.yml / vite.config**: 无需修改，端口和域名配置不变