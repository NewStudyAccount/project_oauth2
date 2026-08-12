## Why

app-springboot 当前是一个传统的服务端渲染 Web 应用（Thymeleaf 模板），页面渲染和 API 逻辑耦合在同一个 Spring Boot 进程中。为了符合现代前后端分离的开发模式，需要将其改造为 Vue3 独立前端 + Spring Boot 后端 API 的架构，采用 BFF 模式（Backend for Frontend）：后端保持 confidential client 角色完成 OAuth2 流程，前端通过 cookie/session 调用后端 API。

## What Changes

- **新建 Vue3 前端项目** `app-vue3-springboot/`：独立的 Vue3 + Vite 项目，包含首页、用户资料页、登录/登出功能
- **改造 app-springboot 后端**：
  - 删除 Thymeleaf 依赖和模板文件
  - 删除 `HomeController` 的页面渲染方法
  - 移除 `thymeleaf-extras-springsecurity6` 依赖
  - 保留并增强 REST API 端点
  - 配置 CORS 允许前端跨域访问
  - 确保 CSRF token 能被前端获取（cookie 方式）
- **OAuth2 流程不变**：后端仍然是 `springboot-app` confidential client，token 存储在服务端 session
- **前端通过 Vite 代理**解决开发环境跨域问题

## Capabilities

### New Capabilities
- `vue3-spa-frontend`: Vue3 前端应用，包含首页展示、用户资料页、登录/登出流程、与后端 API 的交互
- `bff-api-backend`: 后端改造为纯 API 服务，提供用户信息、受保护资源等 JSON 端点，处理 CORS 和 CSRF

### Modified Capabilities
（无现有 spec 需要修改）

## Impact

- **受影响代码**：`app-springboot/` 下的 `pom.xml`、`SecurityConfig.java`、`HomeController.java`、Thymeleaf 模板
- **新增代码**：`app-vue3-springboot/` 整个 Vue3 项目
- **API 变化**：现有 `/api/**` 端点保持不变，新增登出 API 端点
- **依赖变化**：后端移除 Thymeleaf 相关依赖，新增 Vue3 前端项目依赖（Node.js）
- **部署变化**：从单体部署变为前后端独立部署（开发环境通过 Vite proxy 联调）
- **hosts 配置**：仍需 `client.a.local` 指向 127.0.0.1
