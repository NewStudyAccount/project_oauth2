package com.example.authserver.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class ClientDTO {

    private String id;

    private String clientId;

    /**
     * 创建/更新时传入明文，读取时返回 null（不暴露已加密的 secret）
     */
    private String clientSecret;

    private Instant clientIdIssuedAt;

    private String clientName;

    private List<String> clientAuthenticationMethods;

    private List<String> authorizationGrantTypes;

    private List<String> redirectUris;

    private List<String> scopes;

    private boolean requireProofKey;

    private boolean requireAuthorizationConsent;

    /**
     * Access Token 有效期（秒）
     */
    private long accessTokenTtl;

    /**
     * Refresh Token 有效期（秒）
     */
    private long refreshTokenTtl;

    /**
     * Authorization Code 有效期（秒）
     */
    private long authorizationCodeTtl;

    /**
     * 客户端密钥过期时间，null 表示永不过期
     */
    private Instant clientSecretExpiresAt;

    /**
     * 客户端启用/禁用状态
     */
    private boolean enabled = true;

    /**
     * 是否复用刷新令牌。true=每次刷新返回相同的refresh_token；false=Rotation模式，每次返回新的，旧的立即失效
     */
    private boolean reuseRefreshTokens = true;

    /**
     * ID Token 签名算法，如 RS256、ES256 等
     */
    private String idTokenSignatureAlgorithm = "RS256";

    /**
     * 访问令牌格式：self-contained（JWT）或 reference（Opaque）
     */
    private String accessTokenFormat = "self-contained";
}