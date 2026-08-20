package com.example.authserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.authserver.entity.WebhookLog;
import com.example.authserver.entity.WebhookSubscriber;
import com.example.authserver.repository.WebhookLogMapper;
import com.example.authserver.repository.WebhookSubscriberMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Webhook 事件推送服务。
 *
 * <p>当系统发生关键事件（如用户注册、Token 签发）时，向已订阅的外部系统发送 HTTP 回调通知。
 *
 * <p>特性：
 * <ul>
 *   <li>HMAC-SHA256 签名 —— 通过 X-Webhook-Signature 头传递，接收方可用密钥验证消息真实性</li>
 *   <li>指数退避重试 —— 失败后按 1/5/30/120 分钟间隔重试，最多 4 次</li>
 *   <li>定时扫描重试队列 —— 每分钟检查一次待重试的 Webhook</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookSubscriberMapper subscriberMapper;
    private final WebhookLogMapper logMapper;
    private final ObjectMapper objectMapper;

    /** 重试间隔（分钟）：第1次1分钟后、第2次5分钟后、第3次30分钟后、第4次120分钟后 */
    private static final int[] RETRY_DELAYS = {1, 5, 30, 120};

    /**
     * 发送 Webhook 事件
     */
    public void sendEvent(String eventType, Map<String, Object> data) {
        List<WebhookSubscriber> subscribers = subscriberMapper.selectList(
                new LambdaQueryWrapper<WebhookSubscriber>()
                        .eq(WebhookSubscriber::getEventType, eventType)
                        .eq(WebhookSubscriber::getStatus, 1)
        );

        for (WebhookSubscriber subscriber : subscribers) {
            sendToSubscriber(subscriber, eventType, data);
        }
    }

    private void sendToSubscriber(WebhookSubscriber subscriber, String eventType, Map<String, Object> data) {
        try {
            String timestamp = String.valueOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
            String body = objectMapper.writeValueAsString(Map.of(
                    "event", eventType,
                    "timestamp", LocalDateTime.now().toString(),
                    "data", data
            ));

            // 计算签名
            String signature = sign(timestamp + "." + body, subscriber.getSecret());

            // 发送请求
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(subscriber.getCallbackUrl()))
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Signature", "sha256=" + signature)
                    .header("X-Webhook-Event", eventType)
                    .header("X-Webhook-Timestamp", timestamp)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 记录日志
            WebhookLog webhookLog = new WebhookLog();
            webhookLog.setSubscriberId(subscriber.getId());
            webhookLog.setEventType(eventType);
            webhookLog.setPayload(body);
            webhookLog.setStatus(response.statusCode() >= 200 && response.statusCode() < 300 ? "SUCCESS" : "FAILED");
            webhookLog.setRetryCount(0);
            logMapper.insert(webhookLog);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                scheduleRetry(subscriber, eventType, data, 1);
            }
        } catch (Exception e) {
            log.error("Webhook 发送失败: {}", subscriber.getCallbackUrl(), e);
            scheduleRetry(subscriber, eventType, data, 1);
        }
    }

    private void scheduleRetry(WebhookSubscriber subscriber, String eventType, Map<String, Object> data, int retryCount) {
        if (retryCount > RETRY_DELAYS.length) {
            log.error("Webhook 重试次数耗尽: {}", subscriber.getCallbackUrl());
            return;
        }

        WebhookLog retryLog = new WebhookLog();
        retryLog.setSubscriberId(subscriber.getId());
        retryLog.setEventType(eventType);
        try {
            retryLog.setPayload(objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            retryLog.setPayload("{}");
        }
        retryLog.setStatus("RETRYING");
        retryLog.setRetryCount(retryCount);
        retryLog.setNextRetryAt(LocalDateTime.now().plusMinutes(RETRY_DELAYS[retryCount - 1]));
        logMapper.insert(retryLog);
    }

    /**
     * 定时任务：扫描待重试的 Webhook
     */
    @Scheduled(fixedDelay = 60000)
    public void processRetryQueue() {
        List<WebhookLog> retryLogs = logMapper.selectList(
                new LambdaQueryWrapper<WebhookLog>()
                        .eq(WebhookLog::getStatus, "RETRYING")
                        .le(WebhookLog::getNextRetryAt, LocalDateTime.now())
        );

        for (WebhookLog retryLog : retryLogs) {
            WebhookSubscriber subscriber = subscriberMapper.selectById(retryLog.getSubscriberId());
            if (subscriber == null || subscriber.getStatus() != 1) {
                retryLog.setStatus("FAILED");
                logMapper.updateById(retryLog);
                continue;
            }

            try {
                String timestamp = String.valueOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
                String signature = sign(timestamp + "." + retryLog.getPayload(), subscriber.getSecret());

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(subscriber.getCallbackUrl()))
                        .header("Content-Type", "application/json")
                        .header("X-Webhook-Signature", "sha256=" + signature)
                        .header("X-Webhook-Event", retryLog.getEventType())
                        .header("X-Webhook-Timestamp", timestamp)
                        .POST(HttpRequest.BodyPublishers.ofString(retryLog.getPayload()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    retryLog.setStatus("SUCCESS");
                    logMapper.updateById(retryLog);
                } else {
                    scheduleRetry(subscriber, retryLog.getEventType(), Map.of(), retryLog.getRetryCount() + 1);
                    retryLog.setStatus("FAILED");
                    logMapper.updateById(retryLog);
                }
            } catch (Exception e) {
                log.error("Webhook 重试失败: {}", subscriber.getCallbackUrl(), e);
                scheduleRetry(subscriber, retryLog.getEventType(), Map.of(), retryLog.getRetryCount() + 1);
                retryLog.setStatus("FAILED");
                logMapper.updateById(retryLog);
            }
        }
    }

    /**
     * HMAC-SHA256 签名
     */
    private String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("签名计算失败", e);
        }
    }
}
