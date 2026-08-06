## ADDED Requirements

### Requirement: 统一认证会话

认证中心 SHALL 维护统一的用户会话，作为 SSO 的基础。

#### Scenario: 用户登录后创建会话
- **WHEN** 用户在认证中心成功登录
- **THEN** 系统创建服务端会话，设置 JSESSIONID Cookie (domain=auth.local)

#### Scenario: 会话有效期
- **WHEN** 用户会话超过配置时间无活动
- **THEN** 系统自动销毁会话，用户需要重新登录

#### Scenario: 会话续期
- **WHEN** 用户在会话有效期内活跃
- **THEN** 系统自动续展会话有效期

### Requirement: 跨域 SSO 登录流程

系统 SHALL 支持跨域 SSO 登录流程。

#### Scenario: 第一个客户端登录
- **WHEN** 用户访问 app-a.local 的受保护页面
- **THEN** 系统重定向到 auth.local，用户登录后，auth.local 创建会话并返回授权码，app-a.local 完成登录

#### Scenario: 第二个客户端自动登录
- **WHEN** 用户已登录 app-a.local，访问 app-b.local 的受保护页面
- **THEN** 系统重定向到 auth.local，auth.local 检测到已有会话，直接返回授权码，app-b.local 无需用户输入密码即可完成登录

#### Scenario: 所有客户端都未登录
- **WHEN** 用户未在任何客户端登录，访问 app-b.local 的受保护页面
- **THEN** 系统重定向到 auth.local，显示登录页面，用户登录后返回授权码

### Requirement: 跨域登出

系统 SHALL 支持跨域登出 (单点登出)。

#### Scenario: 用户主动登出
- **WHEN** 用户在某个客户端点击登出
- **THEN** 系统清除本地会话，并重定向到 auth.local 的登出端点，auth.local 清除统一会话

#### Scenario: 登出后访问其他客户端
- **WHEN** 用户在 auth.local 登出后访问另一个客户端
- **THEN** 系统重定向到 auth.local，auth.local 发现无有效会话，显示登录页面

### Requirement: 域名与 Cookie 配置

系统 SHALL 通过 hosts 文件和域名配置实现跨域隔离。

#### Scenario: hosts 文件配置
- **WHEN** 开发者配置本地 hosts 文件
- **THEN** auth.local、app-a.local、app-b.local 都解析到 127.0.0.1

#### Scenario: Cookie 域名隔离
- **WHEN** 认证中心设置会话 Cookie
- **THEN** Cookie 的 domain 设置为 auth.local，仅在 auth.local 域下有效

### Requirement: 授权码一次性使用

系统 SHALL 确保授权码只能使用一次。

#### Scenario: 授权码正常使用
- **WHEN** 客户端使用授权码换取 Token
- **THEN** 系统验证授权码有效，返回 Token，并标记授权码为已使用

#### Scenario: 授权码重放攻击
- **WHEN** 攻击者截获授权码并尝试再次使用
- **THEN** 系统拒绝请求，撤销该授权码关联的所有 Token
