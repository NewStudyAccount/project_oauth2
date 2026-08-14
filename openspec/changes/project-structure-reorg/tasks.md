## 1. 创建目标目录结构

- [x] 1.1 创建 `auth-center/`、`auth-center/backend/`、`auth-center/frontend/` 目录
- [x] 1.2 创建 `client-app/`、`client-app/backend/`、`client-app/frontend/` 目录
- [x] 1.3 创建 `standalone-app/`、`standalone-app/frontend/` 目录
- [x] 1.4 创建 `platform/` 目录
- [x] 1.5 创建 `infra/` 目录

## 2. 迁移认证中心系统

- [x] 2.1 `git mv auth-server/* auth-center/backend/`（移动 auth-server 全部内容）
- [x] 2.2 `git mv admin-vue3/* auth-center/frontend/`（移动 admin-vue3 全部内容）

## 3. 迁移外部系统A

- [x] 3.1 `git mv app-springboot/* client-app/backend/`（移动 app-springboot 全部内容）
- [x] 3.2 `git mv app-vue3-springboot/* client-app/frontend/`（移动 app-vue3-springboot 全部内容）

## 4. 迁移外部系统B

- [x] 4.1 `git mv app-vue/* standalone-app/frontend/`（移动 app-vue 全部内容）

## 5. 迁移平台层

- [x] 5.1 `git mv gateway/* platform/gateway/`（移动 gateway 全部内容）
- [x] 5.2 `git mv resource-api/* platform/resource-api/`（移动 resource-api 全部内容）

## 6. 迁移基础设施配置

- [x] 6.1 `git mv hosts.txt infra/hosts.txt`（移动 hosts 文件）
- [x] 6.2 创建 `infra/docker-compose.yml`（MySQL + Redis + Nacos 一键启动）

## 7. 更新 Maven 配置

- [x] 7.1 更新根 `pom.xml` 的 `<modules>` 为：`auth-center/backend`、`client-app/backend`、`platform/gateway`、`platform/resource-api`
- [x] 7.2 验证各子模块 pom.xml 的 `<parent>` `<relativePath>` 正确（可能需调整为 `../../pom.xml` 或 `../../../pom.xml`）

## 8. 修正 app-vue API 调用

- [x] 8.1 修改 `standalone-app/frontend/src/utils/api.js` 中 `API_BASE_URL` 从 `http://client.a.local:8082` 改为 `http://gateway.local:8080/api`
- [x] 8.2 在 `infra/hosts.txt` 中添加 `gateway.local` 域名映射
- [x] 8.3 确认 gateway 的 CORS 配置允许 `client.b.local:5173` 来源

## 9. 更新项目配置文件

- [x] 9.1 更新 `.gitignore` 适配新目录层级（确保 `**/node_modules/`、`**/target/` 等规则仍生效）
- [x] 9.2 更新 `architecture.md` 反映新的项目结构

## 10. 验证

- [x] 10.1 执行 `mvn validate` 验证 Maven 构建正常
- [ ] 10.2 验证 `auth-center/frontend/` 的 `npm install` 正常
- [ ] 10.3 验证 `client-app/frontend/` 的 `npm install` 正常
- [ ] 10.4 验证 `standalone-app/frontend/` 的 `npm install` 正常
- [ ] 10.5 清理残留的空目录（原 auth-server/、admin-vue3/ 等）