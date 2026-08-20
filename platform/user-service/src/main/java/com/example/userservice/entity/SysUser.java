package com.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体 —— 对应 {@code sys_user} 表。
 * <p>存储用户基本信息，供 auth-center 认证和 client-app 查询使用。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（登录标识，全局唯一） */
    private String username;

    /** 密码（bcrypt 加密存储） */
    private String password;

    /** 显示昵称 */
    private String nickname;

    /** 邮箱（全局唯一） */
    private String email;

    /** 手机号 */
    private String phone;

    /** 账号状态：1=正常，0=禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
