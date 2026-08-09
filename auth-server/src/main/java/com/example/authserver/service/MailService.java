package com.example.authserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@example.com");
            message.setTo(to);
            message.setSubject("统一认证中心 - 验证码");
            message.setText("您的验证码是: " + code + "\n\n验证码有效期为5分钟，请尽快使用。\n\n如非本人操作，请忽略此邮件。");
            mailSender.send(message);
            log.info("验证码邮件已发送到: {}", to);
        } catch (Exception e) {
            log.error("邮件发送失败: {}", to, e);
            // 在开发环境下不抛出异常
            log.info("验证码(开发模式): {} -> {}", to, code);
        }
    }
}
