## Purpose

将 app-springboot 后端改造为纯 API 服务（BFF 模式），移除服务端模板渲染，保留 OAuth2 confidential client 角色，为 Vue3 前端提供 JSON API。

## ADDED Requirements

### Requirement: CORS 跨域支持
后端 SHALL 配置 CORS 策略，允许前端应用跨域访问 API 端点。

#### Scenario: 前端跨域请求
- **WHEN** 前端从不同 origin 发送 API 请求
- **THEN** 后端 SHALL 在响应中返回正确的 CORS 头（Access-Control-Allow-Origin、Access-Control-Allow-Credentials）

#### Scenario: 预检请求
- **WHEN** 浏览器发送 OPTIONS 预检请求
- **THEN** 后端 SHALL 返回允许的方法和头信息

### Requirement: CSRF Cookie 暴露
后端 SHALL 将 CSRF token 以 cookie 形式暴露给前端，使前端能在 AJAX 请求中携带。

#### Scenario: 前端获取 CSRF token
- **WHEN** 前端发起任意请求
- **THEN** 后端 SHALL 在响应中设置 XSRF-TOKEN cookie（HttpOnly=false，SameSite=Lax），前端可读取并在后续请求头中携带

### Requirement: 登出 API
后端 SHALL 提供 RESTful 登出端点，支持前端通过 AJAX 调用完成登出。

#### Scenario: 已登录用户登出
- **WHEN** 前端发送 POST 请求到登出端点
- **THEN** 后端 SHALL 使当前 session 失效，清除认证信息，返回成功响应

### Requirement: 用户信息 API
后端 SHALL 提供返回当前已认证用户信息的 JSON API 端点。

#### Scenario: 获取当前用户信息
- **WHEN** 已认证用户请求用户信息端点
- **THEN** 后端 SHALL 返回包含 sub、username、email、nickname 的 JSON 响应

#### Scenario: 未认证请求用户信息
- **WHEN** 未认证用户请求用户信息端点
- **THEN** 后端 SHALL 返回 401 状态码

### Requirement: 受保护资源 API
后端 SHALL 保持现有的受保护 API 端点，仅允许已认证用户访问。

#### Scenario: 已认证访问受保护资源
- **WHEN** 已认证用户请求 `/api/protected/**`
- **THEN** 后端 SHALL 返回 JSON 响应

#### Scenario: 未认证访问受保护资源
- **WHEN** 未认证用户请求 `/api/protected/**`
- **THEN** 后端 SHALL 返回 401 状态码

## MODIFIED Requirements

### Requirement: 移除服务端模板渲染
后端 SHALL 不再提供 Thymeleaf 模板渲染功能，所有页面由前端 SPA 渲染。

#### Scenario: 访问原模板页面路径
- **WHEN** 用户直接访问 `/` 或 `/profile`
- **THEN** 后端 SHALL 返回 JSON 响应或重定向，不再返回 HTML 页面
