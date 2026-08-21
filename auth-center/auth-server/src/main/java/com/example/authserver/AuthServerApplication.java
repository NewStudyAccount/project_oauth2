package com.example.authserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * OAuth2 授权服务器启动入口。
 *
 * <p>基于 Spring Authorization Server 构建的统一认证中心，提供：
 * <ul>
 *   <li>OAuth2 授权码模式 / 客户端凭证模式</li>
 *   <li>OpenID Connect (OIDC) 支持</li>
 *   <li>JWT 令牌签发与校验</li>
 *   <li>用户注册、登录、授权同意</li>
 *   <li>客户端管理、权限控制、审计日志</li>
 *   <li>Webhook 事件推送</li>
 * </ul>
 */
@SpringBootApplication
@MapperScan("com.example.authserver.repository")  // 扫描 MyBatis-Plus Mapper 接口
@EnableScheduling  // 启用定时任务（Token 黑名单清理、Webhook 重试）
@EnableFeignClients  // 启用 Feign 客户端（调用 user-service）
public class AuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServerApplication.class, args);
    }
}
