package com.example.authserver.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * OAuth2 客户端数据传输对象 —— 用于管理 API 的请求/响应。
 *
 * <p>将 Spring Authorization Server 的 {@link org.springframework.security.oauth2.server.authorization.client.RegisteredClient}
 * 展开为扁平结构，方便前端表单绑定。
 * <p>与 {@link ClientConverter} 配合完成 DTO ↔ Entity 互转。
 */
@Data
public class ClientDTO {

    /** 客户端内部 ID（数据库主键） */
    private String id;

    /** 客户端标识（OAuth2 client_id，如 "client-a"） */
    private String clientId;

    /** 客户端密钥（创建/更新时传明文，读取时返回 null 以避免泄露） */
    private String clientSecret;

    /** client_id 签发时间 */
    private Instant clientIdIssuedAt;

    /** 客户端显示名称（授权同意页面展示） */
    private String clientName;

    /** 认证方式列表（如 client_secret_basic, client_secret_post, none） */
    private List<String> clientAuthenticationMethods;

    /** 授权类型列表（如 authorization_code, client_credentials, refresh_token） */
    private List<String> authorizationGrantTypes;

    /** 重定向 URI 列表（授权码模式回调地址） */
    private List<String> redirectUris;

    /** 允许的 scope 列表（如 openid, profile, email） */
    private List<String> scopes;

    /** 是否要求 PKCE（Proof Key for Code Exchange，增强授权码安全性） */
    private boolean requireProofKey;

    /** 是否要求用户授权同意（true=每次授权都弹出同意页面） */
    private boolean requireAuthorizationConsent;

    /** Access Token 有效期（秒），默认 1800（30分钟） */
    private long accessTokenTtl;

    /** Refresh Token 有效期（秒），默认 604800（7天） */
    private long refreshTokenTtl;

    /** Authorization Code 有效期（秒），默认 300（5分钟） */
    private long authorizationCodeTtl;

    /** 客户端密钥过期时间，null 表示永不过期 */
    private Instant clientSecretExpiresAt;

    /** 客户端启用/禁用状态（禁用后无法发起授权请求） */
    private boolean enabled = true;

    /** 是否复用刷新令牌：true=每次刷新返回相同的 refresh_token；false=Rotation 模式，每次返回新的，旧的立即失效 */
    private boolean reuseRefreshTokens = true;

    /** ID Token 签名算法（如 RS256、ES256），默认 RS256 */
    private String idTokenSignatureAlgorithm = "RS256";

    /** 访问令牌格式：self-contained（JWT，可自解析）或 reference（Opaque，需回授权服务器校验） */
    private String accessTokenFormat = "self-contained";
}