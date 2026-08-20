package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体 —— 对应 {@code sys_user} 表。
 * <p>存储用户基本信息和认证凭据，是 Spring Security 认证和 JWT 令牌的数据来源。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（登录标识，全局唯一） */
    private String username;

    /** 密码（bcrypt 加密存储，格式如 $2a$10$...） */
    private String password;

    /** 显示昵称 */
    private String nickname;

    /** 邮箱（注册时验证，全局唯一） */
    private String email;

    /** 手机号 */
    private String phone;

    /** 账号状态：1=正常，0=禁用（禁用后无法登录） */
    private Integer status;

    /** 创建时间（由 MyMetaObjectHandler 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（由 MyMetaObjectHandler 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
