package com.example.authserver.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户数据传输对象 —— 从 user-service 获取的用户信息。
 * <p>不含密码字段，用于安全地在服务间传递用户数据。
 */
@Data
public class UserDTO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
