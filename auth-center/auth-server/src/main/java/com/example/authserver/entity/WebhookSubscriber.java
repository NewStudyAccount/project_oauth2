package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Webhook 订阅者实体 —— 对应 {@code webhook_subscriber} 表。
 * <p>外部系统通过注册 Webhook 订阅来接收系统事件通知。
 */
@Data
@TableName("webhook_subscriber")
public class WebhookSubscriber {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的 OAuth2 客户端 ID */
    private String clientId;

    /** 订阅的事件类型（如 user.registered, token.issued） */
    private String eventType;

    /** 回调 URL（事件发生时发送 HTTP POST 到此地址） */
    private String callbackUrl;

    /** 签名密钥（用于 HMAC-SHA256 签名，接收方验证消息真实性） */
    private String secret;

    /** 订阅状态：1=启用，0=禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
