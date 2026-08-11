## 1. app-springboot 改造

- [x] 1.1 移除 pom.xml 中的 Nacos 依赖（spring-cloud-starter-alibaba-nacos-discovery）
- [x] 1.2 移除 application.yml 中的 Nacos 配置
- [x] 1.3 配置 OAuth2 Client（替代 Resource Server）
- [x] 1.4 更新 SecurityConfig 配置 OAuth2 Login
- [x] 1.5 验证 OAuth2 Client 配置正确

## 2. app-vue 改造

- [x] 2.1 确保 app-vue 独立运行，不依赖其他外部系统

## 3. Gateway 配置调整

- [x] 3.1 移除 Gateway 中 app-springboot 的路由配置
- [x] 3.2 验证其他路由仍然正常工作

## 4. 文档更新

- [x] 4.1 更新架构文档，说明两种接入方式
- [x] 4.2 添加示例项目说明
- [x] 4.3 添加外部系统接入指南

## 5. 测试验证

- [x] 5.1 测试 app-springboot 独立启动
- [x] 5.2 测试 app-vue 登录流程
- [x] 5.3 测试 app-springboot 登录流程
- [x] 5.4 测试两个系统各自独立接入 SSO
