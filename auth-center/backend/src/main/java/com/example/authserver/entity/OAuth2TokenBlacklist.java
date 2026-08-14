package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oauth2_token_blacklist")
public class OAuth2TokenBlacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String jti;

    private String tokenValue;

    private Long userId;

    private String clientId;

    /**
     * 撤销原因: admin_revoke, user_logout, password_changed
     */
    private String reason;

    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
