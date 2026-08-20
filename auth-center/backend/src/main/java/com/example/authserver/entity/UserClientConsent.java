package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户授权同意记录 —— 对应 {@code user_client_consent} 表。
 * <p>记录用户对 OAuth2 客户端授予的 scope 列表，
 * 配合 Spring Authorization Server 的 {@code OAuth2AuthorizationConsentService} 使用。
 */
@Data
@TableName("user_client_consent")
public class UserClientConsent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** OAuth2 客户端 ID */
    private String clientId;

    /** 已同意的 scope 列表（空格分隔，如 "openid profile email"） */
    private String scopes;

    /** 授权同意时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime consentedAt;
}
