# auth-server 框架存储扩展讨论

## 一、授权记录与用户授权确认的存储

### 问题：是否必须使用框架自带的表？

**不是必须的。** 框架给了两条路。

### 策略一：用框架自带的 JDBC 实现（当前项目采用的方式）

```java
// AuthorizationServerConfig.java
@Bean
public OAuth2AuthorizationService authorizationService(
        JdbcTemplate jdbc, RegisteredClientRepository repo) {
    return new JdbcOAuth2AuthorizationService(jdbc, repo);
}
```

这种方式要求数据库表结构**必须严格匹配**框架定义的 schema（即 `schema.sql` 中的 `oauth2_authorization` 和 `oauth2_authorization_consent` 两张表）。字段名、列顺序、类型都不能改，因为框架内部是按固定 SQL 查询的。

### 策略二：自己实现接口（可以用任意表结构）

```java
@Bean
public OAuth2AuthorizationService authorizationService(...) {
    return new MyCustomAuthorizationService();  // 你自己的实现
}

@Bean
public OAuth2AuthorizationConsentService consentService(...) {
    return new MyCustomConsentService();        // 你自己的实现
}
```

只需要实现框架定义的两个接口：

```java
public interface OAuth2AuthorizationService {
    void save(OAuth2Authorization authorization);
    void remove(OAuth2Authorization authorization);
    OAuth2Authorization findById(String id);
    OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType);
}

public interface OAuth2AuthorizationConsentService {
    void save(OAuth2AuthorizationConsent consent);
    void remove(OAuth2AuthorizationConsent consent);
    OAuth2AuthorizationConsent findById(String registeredClientId, String principalName);
}
```

实现后你可以：
- 用**任意表结构**存数据库
- 存 **Redis**（高并发场景）
- 存 **MongoDB**
- 存**内存**（开发/测试用）
- 甚至存**文件**

### 对比

| | 策略一：JdbcOAuth2AuthorizationService | 策略二：自定义实现 |
|---|---|---|
| 表结构 | 必须用框架固定的 schema | 自由定义 |
| 实现成本 | 零，直接用 | 需要自己写 CRUD |
| 灵活性 | 低 | 高 |
| 适用场景 | 标准场景、快速开发 | 需要定制存储、对接已有系统 |

### 实际开发建议

大多数项目直接用策略一（框架自带的 JDBC 实现），因为：
- 授权记录是框架内部管理的数据，业务代码很少直接查询
- 两张表的结构虽然字段多，但不需要手动维护
- 除非你有特殊的存储需求（比如分库分表、多数据源、Redis 高速缓存），否则没必要自己实现

---

## 二、客户端管理的存储

### 问题：是否需要使用框架自带的表？

**不是必须的。** 同样两条路。

### 策略一：用框架自带的 JDBC 实现

```java
@Bean
public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbc) {
    return new JdbcRegisteredClientRepository(jdbc);
}
```

要求使用框架固定的 `oauth2_registered_client` 表（`add-registered-client-table.sql` 中已创建），字段结构不能改。

### 策略二：自己实现接口

```java
@Bean
public RegisteredClientRepository registeredClientRepository() {
    return new MyClientRepository();  // 你自己的实现
}
```

只需要实现一个接口：

```java
public interface RegisteredClientRepository {
    void save(RegisteredClient registeredClient);
    RegisteredClient findById(String id);
    RegisteredClient findByClientId(String clientId);
}
```

### 当前项目的问题

项目里同时存在**三套客户端存储**，但互不相通：

| 存储 | 用在什么地方 | 能否影响实际认证 |
|------|-------------|----------------|
| `application.yml` | 框架隐式创建 `InMemoryRegisteredClientRepository` | **能**（运行时实际使用） |
| `oauth2_client` 表 + `ClientService` | 管理后台展示 | **不能**（仅 CRUD 展示） |
| `oauth2_registered_client` 表 | SQL 脚本创建了表，但没注册 Bean | **不能**（空表） |

### 如何打通

最简单的做法：**自己实现 `RegisteredClientRepository`，读 `oauth2_client` 表**。

```java
@Component
@RequiredArgsConstructor
public class DatabaseClientRepository implements RegisteredClientRepository {

    private final OAuth2ClientMapper clientMapper;

    @Override
    public void save(RegisteredClient client) {
        // 转换并存入 oauth2_client 表
    }

    @Override
    public RegisteredClient findById(String id) {
        OAuth2Client entity = clientMapper.selectById(id);
        return toRegisteredClient(entity);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        OAuth2Client entity = clientMapper.selectOne(
            new LambdaQueryWrapper<OAuth2Client>()
                .eq(OAuth2Client::getClientId, clientId));
        return toRegisteredClient(entity);
    }

    // 将自定义实体转为框架的 RegisteredClient 对象
    private RegisteredClient toRegisteredClient(OAuth2Client entity) {
        return RegisteredClient.withId(entity.getId().toString())
                .clientId(entity.getClientId())
                .clientSecret(entity.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(entity.getRedirectUris())
                .scope("openid").scope("profile").scope("email")
                .build();
    }
}
```

注册为 Bean 后，框架自动用它查找客户端，`application.yml` 中的静态配置就可以移除了。管理后台通过 `ClientService` 增删改客户端会**直接生效**。

### 对比

| | yml 静态配置 | JdbcRegisteredClientRepository | 自定义实现 |
|---|---|---|---|
| 表结构 | 无表 | 必须用 `oauth2_registered_client` | 自由（用你已有的 `oauth2_client`） |
| 动态增删 | 不行，改 yml 要重启 | 可以 | 可以 |
| 实现成本 | 零 | 零 | 需写转换逻辑 |
| 与管理后台打通 | 不通 | 需额外适配 | 天然打通 |

---

## 三、总结

Spring Authorization Server 的存储设计遵循**接口抽象**原则：

| 数据类型 | 接口 | 默认 JDBC 实现 | 自定义实现可选存储 |
|----------|------|---------------|-------------------|
| 客户端 | `RegisteredClientRepository` | `JdbcRegisteredClientRepository`（`oauth2_registered_client` 表） | 任意 |
| 授权记录 | `OAuth2AuthorizationService` | `JdbcOAuth2AuthorizationService`（`oauth2_authorization` 表） | 任意 |
| 授权确认 | `OAuth2AuthorizationConsentService` | `JdbcOAuth2AuthorizationConsentService`（`oauth2_authorization_consent` 表） | 任意 |

**核心原则**：框架不关心你怎么存，只要你实现了接口、能正确返回数据就行。自带的 JDBC 实现只是开箱即用的默认方案。
