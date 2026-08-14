package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_client_access")
public class UserClientAccess {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String clientId;

    /**
     * 1:允许 0:拒绝
     */
    private Integer allowed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
