package com.example.authserver.service;

import com.example.authserver.client.UserServiceClient;
import com.example.authserver.dto.UserDTO;
import com.example.authserver.entity.UserClientAccess;
import com.example.authserver.repository.UserClientAccessMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 用户注册服务 —— 处理邮箱验证码注册流程。
 *
 * <p>流程：发送验证码 → 校验参数 → 校验验证码 → 调用 user-service 创建用户 → 自动授权（可选）。
 * <p>使用 Redis 存储验证码（5 分钟过期）和 IP 限流计数（每小时最多 5 次）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterService {

    private final UserServiceClient userServiceClient;
    private final UserClientAccessMapper userClientAccessMapper;
    private final StringRedisTemplate redisTemplate;
    private final MailService mailService;

    private static final String CODE_KEY_PREFIX = "register:code:";       // 验证码 Redis key 前缀
    private static final String RATE_KEY_PREFIX = "rate:register:";       // IP 限流 Redis key 前缀
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");  // 密码强度规则
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");          // 邮箱格式

    /**
     * 发送验证码
     */
    public void sendVerificationCode(String email, String ip) {
        // IP 限流检查
        String rateKey = RATE_KEY_PREFIX + ip;
        String count = redisTemplate.opsForValue().get(rateKey);
        if (count != null && Integer.parseInt(count) >= 5) {
            throw new RuntimeException("发送过于频繁，请稍后再试");
        }

        // 生成验证码
        String code = String.valueOf((int) (Math.random() * 900000 + 100000));

        // 存入 Redis，5分钟过期
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, 5, TimeUnit.MINUTES);

        // 增加限流计数
        redisTemplate.opsForValue().increment(rateKey);
        redisTemplate.expire(rateKey, 1, TimeUnit.HOURS);

        // 发送邮件
        mailService.sendVerificationCode(email, code);

        log.info("验证码已发送到: {}", email);
    }

    /**
     * 用户注册 —— 校验参数和验证码后，调用 user-service 创建用户。
     *
     * @return 注册成功的用户信息（不含密码）
     */
    public UserDTO register(String username, String password, String confirmPassword,
                           String email, String code, String nickname, String clientId) {
        // 参数校验
        if (username == null || username.length() < 3 || username.length() > 50) {
            throw new RuntimeException("用户名长度需要3-50个字符");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new RuntimeException("密码需要8位以上，包含大小写字母和数字");
        }
        if (!password.equals(confirmPassword)) {
            throw new RuntimeException("两次密码输入不一致");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new RuntimeException("邮箱格式不正确");
        }

        // 验证码校验
        String storedCode = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + email);
        if (storedCode == null || !storedCode.equals(code)) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 调用 user-service 创建用户（用户名/邮箱唯一性校验由 user-service 负责）
        UserDTO user;
        try {
            user = userServiceClient.createUser(Map.of(
                    "username", username,
                    "password", password,
                    "email", email,
                    "nickname", nickname != null ? nickname : username
            ));
        } catch (Exception e) {
            // Feign 调用失败，提取错误信息
            throw new RuntimeException("注册失败: " + e.getMessage());
        }

        // 删除已使用的验证码
        redisTemplate.delete(CODE_KEY_PREFIX + email);

        // 如果指定了 clientId，自动授权
        if (clientId != null && !clientId.isEmpty()) {
            UserClientAccess access = new UserClientAccess();
            access.setUserId(user.getId());
            access.setClientId(clientId);
            access.setAllowed(1);
            userClientAccessMapper.insert(access);
        }

        log.info("用户注册成功: {}", username);
        return user;
    }
}
