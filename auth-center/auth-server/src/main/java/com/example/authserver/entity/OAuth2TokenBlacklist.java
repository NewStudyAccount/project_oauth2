package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Token 黑名单实体 —— 对应 {@code oauth2_token_blacklist} 表。
 * <p>记录已撤销的 JWT 令牌，配合 {@link com.example.authserver.service.TokenBlacklistService} 实现令牌撤销机制。
 */
@Data
@TableName("oauth2_token_blacklist")
public class OAuth2TokenBlacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** JWT ID（令牌唯一标识，对应 JWT 中的 jti 声明） */
    private String jti;

    /** 原始 Token 值（用于审计追溯） */
    private String tokenValue;

    /** 所属用户 ID */
    private Long userId;

    /** 所属客户端 ID */
    private String clientId;

    /** 撤销原因：admin_revoke=管理员撤销，user_logout=用户登出，password_changed=密码修改 */
    private String reason;

    /** Token 原定过期时间（黑名单记录在此时间后可安全清理） */
    private LocalDateTime expiresAt;

    /** 加入黑名单的时间（由 MyMetaObjectHandler 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
