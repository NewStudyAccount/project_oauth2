package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("webhook_subscriber")
public class WebhookSubscriber {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String clientId;

    private String eventType;

    private String callbackUrl;

    private String secret;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
