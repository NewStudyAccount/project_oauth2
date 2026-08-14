## ADDED Requirements

### Requirement: Domain-based directory grouping
项目 SHALL 按系统领域分组为 4 个顶层目录：`auth-center/`、`client-app/`、`standalone-app/`、`platform/`，每个目录代表一个独立的系统或平台层。

#### Scenario: Authentication center system directory
- **WHEN** 查看项目根目录
- **THEN** 存在 `auth-center/` 目录，包含 `backend/`（原 auth-server）和 `frontend/`（原 admin-vue3）

#### Scenario: Client application system directory
- **WHEN** 查看项目根目录
- **THEN** 存在 `client-app/` 目录，包含 `backend/`（原 app-springboot）和 `frontend/`（原 app-vue3-springboot）

#### Scenario: Standalone application directory
- **WHEN** 查看项目根目录
- **THEN** 存在 `standalone-app/` 目录，包含 `frontend/`（原 app-vue），无 backend 子目录

#### Scenario: Platform infrastructure directory
- **WHEN** 查看项目根目录
- **THEN** 存在 `platform/` 目录，包含 `gateway/` 和 `resource-api/` 子目录

### Requirement: Unified sub-directory naming
每个包含前后端的系统目录 SHALL 使用 `backend/` 和 `frontend/` 作为子目录名，而非保留原项目名。

#### Scenario: Backend sub-directory naming
- **WHEN** 系统包含后端服务
- **THEN** 后端代码位于该系统目录下的 `backend/` 子目录

#### Scenario: Frontend sub-directory naming
- **WHEN** 系统包含前端应用
- **THEN** 前端代码位于该系统目录下的 `frontend/` 子目录

### Requirement: Maven modules path update
根 pom.xml 的 `<modules>` SHALL 更新为新的嵌套路径，Maven 构建 SHALL 正常工作。

#### Scenario: Maven module paths reflect new structure
- **WHEN** 查看根 pom.xml 的 modules
- **THEN** 包含 `auth-center/backend`、`client-app/backend`、`platform/gateway`、`platform/resource-api`

#### Scenario: Maven build succeeds after reorganization
- **WHEN** 执行 `mvn validate`
- **THEN** 构建成功，无错误

### Requirement: Module artifactId unchanged
各后端模块的 `<artifactId>` 和 `<groupId>` SHALL 保持不变，仅物理目录位置改变。

#### Scenario: Artifact identifiers preserved
- **WHEN** 查看各子模块的 pom.xml
- **THEN** artifactId 分别为 `auth-server`、`app-springboot`、`gateway`、`resource-api`，与重组前一致

### Requirement: No original flat directories remain
重组后根目录下 SHALL 不保留原有的扁平子项目目录（auth-server/、admin-vue3/、app-springboot/、app-vue3-springboot/、app-vue/、gateway/、resource-api/）。

#### Scenario: Flat directories removed after migration
- **WHEN** 重组完成
- **THEN** 根目录下不存在 `auth-server/`、`admin-vue3/`、`app-springboot/`、`app-vue3-springboot/`、`app-vue/`、`gateway/`、`resource-api/` 目录