## MODIFIED Requirements

### Requirement: Standalone app API target
standalone-app (app-vue) 的 API 调用 SHALL 通过 gateway 进行，而非直接调用 app-springboot。

#### Scenario: API base URL points to gateway
- **WHEN** 查看 `standalone-app/frontend/src/utils/api.js` 中的 `API_BASE_URL`
- **THEN** 其值为 gateway 的 API 路由地址（如 `http://gateway.local:8080/api`）

#### Scenario: API base URL no longer points to app-springboot
- **WHEN** 查看 `standalone-app/frontend/src/utils/api.js` 中的 `API_BASE_URL`
- **THEN** 其值不包含 `client.a.local:8082` 或 `app-springboot` 相关地址

#### Scenario: Gateway CORS allows standalone app origin
- **WHEN** standalone-app 前端（client.b.local:5173）通过 gateway 调用 API
- **THEN** gateway 的 CORS 配置允许该来源的跨域请求