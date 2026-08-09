package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oauth2_client")
public class OAuth2Client {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String clientId;

    private String clientSecret;

    private String clientName;

    /**
     * INTERNAL:内部应用 THIRD_PARTY:第三方应用
     */
    private String clientType;

    private String scopes;

    private String grantTypes;

    private String redirectUris;

    /**
     * 是否需要用户确认授权: 0:不需要(内部) 1:需要(第三方)
     */
    private Integer requireConsent;

    private Integer accessTokenTtl;

    private Integer refreshTokenTtl;

    /**
     * 1:正常 0:禁用
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
