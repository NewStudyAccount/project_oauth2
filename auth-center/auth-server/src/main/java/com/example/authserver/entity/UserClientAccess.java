package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-客户端访问权限实体 —— 对应 {@code user_client_access} 表。
 * <p>记录用户对特定 OAuth2 客户端的访问权限，由 {@link com.example.authserver.service.AccessControlService} 管理。
 */
@Data
@TableName("user_client_access")
public class UserClientAccess {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** OAuth2 客户端 ID */
    private String clientId;

    /** 访问权限：1=允许，0=拒绝 */
    private Integer allowed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
