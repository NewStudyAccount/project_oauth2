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

/**
 * Token 黑名单服务 —— 实现 JWT 令牌撤销机制。
 *
 * <p>JWT 本身无状态，签发后无法主动失效。通过维护黑名单（jti 列表），
 * 在校验 Token 时查询黑名单来判断 Token 是否已被撤销。
 *
 * <p>采用 Redis + MySQL 双写策略：
 * <ul>
 *   <li>Redis —— 高速查询，设置 TTL 自动过期</li>
 *   <li>MySQL —— 持久化存储，作为 Redis 的降级兜底</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final OAuth2TokenBlacklistMapper blacklistMapper;
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    /** Redis 黑名单 key 前缀 */
    private static final String BLACKLIST_KEY_PREFIX = "oauth2:blacklist:";

    /**
     * 将 Token 加入黑名单（双写：先 DB，再 Redis）。
     *
     * @param jti        JWT ID（唯一标识）
     * @param tokenValue 原始 Token 值（用于审计）
     * @param userId     所属用户 ID
     * @param clientId   所属客户端 ID
     * @param reason     撤销原因（admin_revoke / user_logout / password_changed）
     * @param expiresAt  Token 原定过期时间（黑名单记录也在此时间后可清理）
     */
    public void addToBlacklist(String jti, String tokenValue, Long userId, String clientId, String reason, LocalDateTime expiresAt) {
        // 写入数据库（持久化）
        OAuth2TokenBlacklist blacklist = new OAuth2TokenBlacklist();
        blacklist.setJti(jti);
        blacklist.setTokenValue(tokenValue);
        blacklist.setUserId(userId);
        blacklist.setClientId(clientId);
        blacklist.setReason(reason);
        blacklist.setExpiresAt(expiresAt);
        blacklistMapper.insert(blacklist);

        // 写入 Redis（高速缓存，TTL 与 Token 过期时间一致）
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
     * 检查 Token 是否在黑名单中（优先查 Redis，降级查 DB）。
     * <p>如果 DB 中查到但 Redis 中没有，会回写 Redis 以加速后续查询（缓存穿透保护）。
     *
     * @param jti JWT ID
     * @return true 表示已被撤销
     */
    public boolean isBlacklisted(String jti) {
        // 优先查 Redis（O(1) 复杂度）
        try {
            Boolean exists = redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti);
            if (Boolean.TRUE.equals(exists)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Redis 查询黑名单失败，降级为 DB 查询", e);
        }

        // 降级查数据库
        OAuth2TokenBlacklist record = blacklistMapper.selectOne(
                new LambdaQueryWrapper<OAuth2TokenBlacklist>()
                        .eq(OAuth2TokenBlacklist::getJti, jti)
        );
        if (record != null) {
            // 回写 Redis，加速后续查询
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
     * 撤销用户的所有 Token（用于管理员强制下线、密码修改等场景）。
     * <p>流程：查询该用户所有有效的 access_token → 逐个加入黑名单 → 从 oauth2_authorization 表删除。
     */
    public void revokeAllUserTokens(Long userId, String reason) {
        // 查询该用户所有有效的 access_token
        List<String> tokenValues = jdbcTemplate.queryForList(
                "SELECT access_token_value FROM oauth2_authorization WHERE principal_name = " +
                        "(SELECT username FROM sys_user WHERE id = ?) AND access_token_value IS NOT NULL",
                String.class, userId
        );

        // 逐个加入黑名单
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

        // 从授权记录表中删除，使 Refresh Token 也失效
        jdbcTemplate.update(
                "DELETE FROM oauth2_authorization WHERE principal_name = " +
                        "(SELECT username FROM sys_user WHERE id = ?)", userId
        );

        log.info("撤销用户 {} 的所有 Token，数量: {}，原因: {}", userId, tokenValues.size(), reason);
    }

    /**
     * 定时清理过期的黑名单记录（每天凌晨 3 点执行）。
     * <p>已过期的 Token 本身已失效，黑名单记录可以安全删除以释放存储空间。
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
