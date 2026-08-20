package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户角色关联实体 —— 对应 {@code sys_user_role} 表。
 * <p>预留的角色管理表，当前系统通过用户名硬编码判断角色（admin 用户自动获得 ROLE_ADMIN）。
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 角色编码（如 ROLE_ADMIN、ROLE_USER） */
    private String roleCode;
}
