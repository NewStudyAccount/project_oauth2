package com.example.authserver.controller;

import com.example.authserver.entity.WebhookSubscriber;
import com.example.authserver.repository.WebhookSubscriberMapper;
import com.example.authserver.service.WebhookService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Webhook 订阅管理 API。
 *
 * <p>提供 Webhook 订阅的 CRUD 操作。外部系统通过订阅事件类型，
 * 在事件发生时接收 HTTP 回调通知（带 HMAC-SHA256 签名）。
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookSubscriberMapper subscriberMapper;
    private final WebhookService webhookService;

    /** 获取所有 Webhook 订阅列表 */
    @GetMapping
    public List<WebhookSubscriber> list() {
        return subscriberMapper.selectList(null);
    }

    /** 创建新的 Webhook 订阅（默认启用状态） */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody WebhookSubscriber subscriber) {
        subscriber.setStatus(1);  // 默认启用
        subscriberMapper.insert(subscriber);
        return ResponseEntity.ok(Map.of("id", subscriber.getId(), "message", "订阅创建成功"));
    }

    /** 删除 Webhook 订阅 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        subscriberMapper.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "订阅删除成功"));
    }
}
