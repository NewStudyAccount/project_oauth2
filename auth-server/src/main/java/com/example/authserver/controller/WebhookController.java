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

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookSubscriberMapper subscriberMapper;
    private final WebhookService webhookService;

    @GetMapping
    public List<WebhookSubscriber> list() {
        return subscriberMapper.selectList(null);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody WebhookSubscriber subscriber) {
        subscriber.setStatus(1);
        subscriberMapper.insert(subscriber);
        return ResponseEntity.ok(Map.of("id", subscriber.getId(), "message", "订阅创建成功"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        subscriberMapper.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "订阅删除成功"));
    }
}
