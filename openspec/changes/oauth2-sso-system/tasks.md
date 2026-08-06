## 1. 项目骨架搭建

- [x] 1.1 创建 Maven 父 POM (project-oauth2)，配置模块和依赖版本管理
- [x] 1.2 创建 auth-server 子模块骨架 (Spring Boot 3.2+)
- [x] 1.3 创建 client-b 子模块骨架 (Spring Boot 3.2+)
- [x] 1.4 创建 resource-server 子模块骨架 (预留)
- [x] 1.5 配置 hosts 文件 (auth.local / app-a.local / app-b.local / api.local)

## 2. 数据库设计与初始化

- [x] 2.1 创建 MySQL 数据库 oauth2_sso
- [x] 2.2 创建 sys_user 表 (用户表，BCrypt 密码)
- [x] 2.3 创建 sys_role 和 sys_user_role 表 (角色关联)
- [x] 2.4 创建 Spring Authorization Server 必需表 (oauth2_registered_client, oauth2_authorization, oauth2_authorization_consent)
- [x] 2.5 插入测试数据 (admin/user 账号，vue-app 和 client-b 客户端注册)
- [x] 2.6 auth-server 配置 MyBatis-Plus + MySQL 数据源

## 3. 认证中心核心 (auth-server)

- [x] 3.1 配置 Spring Authorization Server (AuthorizationServerConfig)
- [x] 3.2 配置 Spring Security 过滤链 (SecurityConfig)
- [x] 3.3 实现自定义登录页面 (Thymeleaf)
- [x] 3.4 实现 CustomUserDetailsService (MyBatis-Plus 查询 sys_user)
- [x] 3.5 实现用户实体 (User, Role) 和 Mapper
- [x] 3.6 配置 JWT Token 签发 (RSA 密钥对)
- [x] 3.7 配置 CORS 策略 (允许 app-a.local 和 app-b.local 跨域)
- [x] 3.8 实现 OIDC UserInfo 端点

## 4. Token 管理

- [x] 4.1 配置 access_token 过期时间 (30 分钟)
- [x] 4.2 配置 refresh_token 过期时间 (7 天)
- [x] 4.3 启用 Refresh Token 轮转 (Rotation)
- [x] 4.4 实现 Token 撤销端点 (POST /oauth2/revoke)
- [x] 4.5 验证 JWKS 端点 (/oauth2/jwks) 正常发布公钥
- [x] 4.6 验证 OIDC Discovery 端点返回正确配置

## 5. Vue 前端客户端 (app-vue)

- [x] 5.1 使用 Vite 创建 Vue 3 项目 (app-vue)
- [x] 5.2 安装依赖 (vue-router, pinia, axios)
- [x] 5.3 实现 OAuth2 PKCE 工具函数 (generateCodeVerifier, generateCodeChallenge)
- [x] 5.4 实现 auth.js 模块 (发起登录, 处理回调, 换 Token, 刷新 Token)
- [x] 5.5 实现 Pinia auth store (Token 存储, 登录状态)
- [x] 5.6 实现 Axios 拦截器 (自动附加 Token, 401 自动刷新)
- [x] 5.7 实现路由守卫 (未登录跳转登录流程)
- [x] 5.8 实现首页 (登录状态展示, 登录/登出按钮)
- [x] 5.9 实现 Profile 页 (解析 id_token 展示用户信息)
- [x] 5.10 配置 Vite 开发服务器 (app-a.local:5173)

## 6. Spring Boot 客户端 (client-b)

- [x] 6.1 配置 Spring OAuth2 Client (application.yml)
- [x] 6.2 实现首页 (登录状态展示)
- [x] 6.3 实现 Profile 页 (用户信息展示)
- [x] 6.4 实现登出功能
- [ ] 6.5 验证 SSO 自动登录 (与 app-vue 共享 auth.local 会话)

## 7. 跨域 SSO 验证

- [ ] 7.1 验证流程: app-vue 登录 → auth.local 创建会话 → client-b 自动登录
- [ ] 7.2 验证流程: 单点登出 → 所有客户端会话失效
- [ ] 7.3 验证流程: 授权码一次性使用 (防重放)
- [ ] 7.4 验证流程: 无效 redirect_uri 拒绝授权

## 8. 生产级加固

- [x] 8.1 配置安全响应头 (CSP, X-Frame-Options, HSTS 等)
- [x] 8.2 实现全局异常处理 (auth-server)
- [ ] 8.3 添加审计日志 (登录事件, Token 签发/撤销)
- [x] 8.4 验证 PKCE 强制开启 (SPA 客户端必须使用 PKCE)
- [x] 8.5 编写 README.md (项目说明, 快速启动, hosts 配置说明)

## 9. Resource Server (预留)

- [ ] 9.1 配置 Spring OAuth2 Resource Server (JWT 验证)
- [ ] 9.2 实现示例 API (/api/hello)
- [ ] 9.3 验证 Token 从 JWKS 端点获取公钥进行验证
