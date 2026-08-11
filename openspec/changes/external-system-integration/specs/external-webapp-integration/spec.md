## Purpose

定义外部 Web 应用直接接入 SSO 平台的能力，展示如何作为独立 OAuth2 Client 与 auth-server 交互获取 Token，不依赖 SSO 平台的基础设施（Nacos、Gateway）。

## ADDED Requirements

### Requirement: 独立 OAuth2 Client 配置
外部 Web 应用 SHALL 配置为 OAuth2 Client，直接与 auth-server 交互获取 Token，不依赖 Nacos 服务发现。

#### Scenario: 配置 OAuth2 Client
- **WHEN** 外部 Web 应用启动
- **THEN** 系统 SHALL 配置 OAuth2 Client，使用 Authorization Code Flow 获取 Token
- **THEN** 系统 SHALL 不依赖 Nacos 服务发现

### Requirement: 用户认证流程
外部 Web 应用 SHALL 支持完整的用户认证流程，包括登录、获取用户信息、登出。

#### Scenario: 用户登录
- **WHEN** 用户访问受保护页面
- **THEN** 系统 SHALL 重定向到 auth-server 的登录页面
- **THEN** 用户登录成功后，系统 SHALL 获取 Token 并重定向回原页面

#### Scenario: 获取用户信息
- **WHEN** 用户登录成功
- **THEN** 系统 SHALL 从 auth-server 获取用户信息
- **THEN** 系统 SHALL 在页面显示用户信息

#### Scenario: 用户登出
- **WHEN** 用户点击登出
- **THEN** 系统 SHALL 清除本地 Token
- **THEN** 系统 SHALL 重定向到 auth-server 的登出端点

### Requirement: 页面展示
外部 Web 应用 SHALL 提供传统的 Web 页面，展示用户信息和认证状态。

#### Scenario: 首页展示
- **WHEN** 用户访问首页
- **THEN** 系统 SHALL 显示登录状态
- **THEN** 如果用户已登录，系统 SHALL 显示用户信息

#### Scenario: 个人中心展示
- **WHEN** 用户访问个人中心页面
- **THEN** 系统 SHALL 显示用户的详细信息
- **THEN** 如果用户未登录，系统 SHALL 重定向到登录页面
