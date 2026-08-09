package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("webhook_log")
public class WebhookLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long subscriberId;

    private String eventType;

    private String payload;

    /**
     * PENDING, SUCCESS, FAILED, RETRYING
     */
    private String status;

    private Integer retryCount;

    private LocalDateTime nextRetryAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
