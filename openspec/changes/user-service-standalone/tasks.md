# user-service 独立部署 — 任务清单

## 1. 移除 Nacos 依赖

- [x] 1.1 从 `platform/user-service/pom.xml` 中移除 `spring-cloud-starter-alibaba-nacos-discovery` 依赖
- [x] 1.2 从 `platform/user-service/src/main/resources/application.yml` 中移除 `spring.cloud.nacos.discovery` 配置块

## 2. 文档更新

- [x] 2.1 更新 `extract-user-center-service/design.md` 中 Decision 2，明确 user-service 不走 Nacos

## 3. 验证

- [ ] 3.1 启动 user-service，确认无 Nacos 相关错误日志
- [ ] 3.2 验证 `GET http://localhost:8081/api/users/admin` 正常返回
