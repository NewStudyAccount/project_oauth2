package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String clientId;

    /**
     * LOGIN, LOGOUT, AUTHORIZE, TOKEN_ISSUED, TOKEN_REVOKED, REGISTER, PASSWORD_CHANGED
     */
    private String action;

    private String detail;

    private String ip;

    private String userAgent;

    /**
     * SUCCESS, FAILED
     */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
