package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_client_consent")
public class UserClientConsent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String clientId;

    private String scopes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime consentedAt;
}
