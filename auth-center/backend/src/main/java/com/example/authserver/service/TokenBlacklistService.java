package com.example.authserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.authserver.entity.OAuth2TokenBlacklist;
import com.example.authserver.repository.OAuth2TokenBlacklistMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final OAuth2TokenBlacklistMapper blacklistMapper;
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    private static final String BLACKLIST_KEY_PREFIX = "oauth2:blacklist:";

    /**
     * 将 Token 加入黑名单
     */
    public void addToBlacklist(String jti, String tokenValue, Long userId, String clientId, String reason, LocalDateTime expiresAt) {
        // 写入数据库
        OAuth2TokenBlacklist blacklist = new OAuth2TokenBlacklist();
        blacklist.setJti(jti);
        blacklist.setTokenValue(tokenValue);
        blacklist.setUserId(userId);
        blacklist.setClientId(clientId);
        blacklist.setReason(reason);
        blacklist.setExpiresAt(expiresAt);
        blacklistMapper.insert(blacklist);

        // 写入 Redis（设置过期时间）
        try {
            long ttl = Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_KEY_PREFIX + jti, "1", ttl, TimeUnit.SECONDS
                );
            }
        } catch (Exception e) {
            log.warn("Redis 写入黑名单失败，降级为 DB 查询", e);
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    public boolean isBlacklisted(String jti) {
        // 优先查 Redis
        try {
            Boolean exists = redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti);
            if (Boolean.TRUE.equals(exists)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Redis 查询黑名单失败，降级为 DB 查询", e);
        }

        // 查数据库
        OAuth2TokenBlacklist record = blacklistMapper.selectOne(
                new LambdaQueryWrapper<OAuth2TokenBlacklist>()
                        .eq(OAuth2TokenBlacklist::getJti, jti)
        );
        if (record != null) {
            // 回写 Redis
            try {
                long ttl = Duration.between(LocalDateTime.now(), record.getExpiresAt()).getSeconds();
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + jti, "1", ttl, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                // ignore
            }
            return true;
        }
        return false;
    }

    /**
     * 撤销用户的所有 Token
     */
    public void revokeAllUserTokens(Long userId, String reason) {
        List<String> tokenValues = jdbcTemplate.queryForList(
                "SELECT access_token_value FROM oauth2_authorization WHERE principal_name = " +
                        "(SELECT username FROM sys_user WHERE id = ?) AND access_token_value IS NOT NULL",
                String.class, userId
        );

        for (String tokenValue : tokenValues) {
            String jti = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
            try {
                OAuth2TokenBlacklist blacklist = new OAuth2TokenBlacklist();
                blacklist.setJti(jti);
                blacklist.setTokenValue(tokenValue);
                blacklist.setUserId(userId);
                blacklist.setReason(reason);
                blacklist.setExpiresAt(expiresAt);
                blacklistMapper.insert(blacklist);

                long ttl = Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + jti, "1", ttl, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                log.warn("撤销 Token 失败: {}", tokenValue, e);
            }
        }

        jdbcTemplate.update(
                "DELETE FROM oauth2_authorization WHERE principal_name = " +
                        "(SELECT username FROM sys_user WHERE id = ?)", userId
        );

        log.info("撤销用户 {} 的所有 Token，数量: {}，原因: {}", userId, tokenValues.size(), reason);
    }

    /**
     * 定期清理过期的黑名单记录
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpired() {
        int deleted = blacklistMapper.delete(
                new LambdaQueryWrapper<OAuth2TokenBlacklist>()
                        .lt(OAuth2TokenBlacklist::getExpiresAt, LocalDateTime.now())
        );
        log.info("清理过期黑名单记录: {} 条", deleted);
    }
}
