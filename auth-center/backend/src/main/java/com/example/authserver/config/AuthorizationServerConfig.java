package com.example.authserver.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * OAuth2 授权服务器核心配置类。
 *
 * <p>职责：
 * <ul>
 *   <li>定义授权服务器专用的安全过滤链（{@code @Order(1)}，优先于默认 Web 安全链）</li>
 *   <li>生成 RSA 密钥对并暴露 JWK 源，用于签发 / 校验 JWT</li>
 *   <li>配置 JDBC 持久化的客户端仓库、授权记录服务、授权同意服务</li>
 *   <li>声明授权服务器全局设置（issuer 地址）</li>
 * </ul>
 */
@Configuration
public class AuthorizationServerConfig {

    /**
     * 密码编码器 —— 使用委托模式，自动根据密码前缀（如 {@code {bcrypt}}）选择对应算法。
     * <p>兼容 bcrypt / noop / scrypt 等多种编码格式，新密码默认使用 bcrypt。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * 授权服务器专用安全过滤链（优先级 1，高于默认 Web 链）。
     *
     * <p>处理所有 OAuth2/OIDC 协议端点：
     * <ul>
     *   <li>/oauth2/authorize —— 授权端点</li>
     *   <li>/oauth2/token —— 令牌端点</li>
     *   <li>/oauth2/jwks —— JWK 公钥端点</li>
     *   <li>/.well-known/openid-configuration —— OIDC 发现端点</li>
     * </ul>
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authServerFilterChain(HttpSecurity http) throws Exception {
        // 应用 Spring Authorization Server 默认安全配置
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                // 启用 OIDC（OpenID Connect）支持，注册 OIDC 端点
                .oidc(Customizer.withDefaults())
                // 自定义授权同意页面路径（用户首次授权时展示的确认页面）
                .authorizationEndpoint(endpoint -> endpoint
                        .consentPage("/consent")
                );

        http
                .cors(Customizer.withDefaults())
                // 未认证请求的处理策略：HTML 页面请求重定向到登录页
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                // 启用资源服务器能力，支持用 JWT 校验 /userinfo 等受保护资源
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }

    /**
     * RSA 2048 密钥对，用于 JWT 签名。
     * <p><b>注意：</b>每次应用启动都会重新生成。生产环境应改为从文件 / 密钥库加载持久化密钥。
     */
    @Bean
    public KeyPair keyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * JWK（JSON Web Key）源 —— 将 RSA 密钥对封装为 JWK Set，供 /oauth2/jwks 端点发布公钥。
     * <p>客户端和资源服务器通过此端点获取公钥来验证 JWT 签名。
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource(KeyPair keyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())  // 每次启动生成新的 key ID
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * JWT 解码器 —— 使用 JWK 源来解析和验证 JWT 令牌的签名。
     * <p>被资源服务器过滤链和 /userinfo 端点使用。
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * 授权服务器全局设置 —— 配置 issuer（签发者）URL。
     * <p>此 URL 会出现在签发的 JWT 的 {@code iss} 声明中，也是 OIDC 发现端点的基础地址。
     * <p>客户端和资源服务器会校验 JWT 中的 iss 是否与此处配置一致。
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://auth.local:9000")
                .build();
    }

    /**
     * 客户端仓库 —— 使用 JDBC 持久化到 {@code oauth2_registered_client} 表。
     * <p>外层包装 {@link EnabledCheckingRegisteredClientRepository}，在查询时过滤掉已禁用的客户端。
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new EnabledCheckingRegisteredClientRepository(new JdbcRegisteredClientRepository(jdbcTemplate));
    }

    /**
     * 授权记录服务 —— 使用 JDBC 持久化到 {@code oauth2_authorization} 表。
     * <p>记录每次 OAuth2 授权流程产生的授权码、访问令牌、刷新令牌等信息。
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
                                                            RegisteredClientRepository registeredClientRepository) {
        return new org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService(
                jdbcTemplate, registeredClientRepository);
    }

    /**
     * 授权同意服务 —— 使用 JDBC 持久化到 {@code oauth2_authorization_consent} 表。
     * <p>记录用户对客户端的授权同意（scope 列表），避免重复弹出同意页面。
     */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
                                                                          RegisteredClientRepository registeredClientRepository) {
        return new org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService(
                jdbcTemplate, registeredClientRepository);
    }
}