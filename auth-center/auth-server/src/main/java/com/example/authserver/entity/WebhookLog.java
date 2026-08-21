package com.example.authserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Webhook 推送日志实体 —— 对应 {@code webhook_log} 表。
 * <p>记录每次 Webhook 推送的结果，支持重试机制的状态追踪。
 */
@Data
@TableName("webhook_log")
public class WebhookLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订阅者 ID（关联 webhook_subscriber 表） */
    private Long subscriberId;

    /** 事件类型 */
    private String eventType;

    /** 推送的 JSON 载荷 */
    private String payload;

    /** 推送状态：PENDING=待推送，SUCCESS=成功，FAILED=失败，RETRYING=重试中 */
    private String status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 下次重试时间（仅 RETRYING 状态有效） */
    private LocalDateTime nextRetryAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
