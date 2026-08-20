package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体 —— 对应 {@code sys_audit_log} 表。
 * <p>记录系统关键操作（登录、注册、授权、Token 签发/撤销等），用于安全审计和问题追溯。
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户 ID */
    private Long userId;

    /** 操作用户名 */
    private String username;

    /** 涉及的 OAuth2 客户端 ID */
    private String clientId;

    /** 操作类型：LOGIN / LOGOUT / AUTHORIZE / TOKEN_ISSUED / TOKEN_REVOKED / REGISTER / PASSWORD_CHANGED */
    private String action;

    /** 操作详情 */
    private String detail;

    /** 客户端 IP（支持 X-Forwarded-For 代理头） */
    private String ip;

    /** 浏览器 User-Agent */
    private String userAgent;

    /** 操作结果：SUCCESS / FAILED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
