package com.example.authserver.service;

import com.example.authserver.client.UserServiceClient;
import com.example.authserver.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 自定义用户DetailsService —— 实现 Spring Security 的用户认证加载逻辑。
 *
 * <p>通过 {@link UserServiceClient} 调用 user-service 获取用户信息，
 * 构建 {@link UserDetails} 供认证框架使用。
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserServiceClient userServiceClient;

    /**
     * Spring Security 认证入口 —— 根据用户名加载用户信息。
     * <p>调用 user-service 的 /api/users/{username}/full 接口获取含密码哈希的用户信息。
     * <p>仅加载 status=1（正常状态）的用户，禁用用户无法登录。
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Map<String, Object> userData;
        try {
            userData = userServiceClient.getUserByUsernameFull(username);
        } catch (Exception e) {
            throw new UsernameNotFoundException("用户服务不可用: " + username, e);
        }

        if (userData == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 检查用户状态
        Object statusObj = userData.get("status");
        int status = statusObj instanceof Number ? ((Number) statusObj).intValue() : 0;
        if (status != 1) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 角色分配：所有用户默认拥有 ROLE_USER，admin 用户额外拥有 ROLE_ADMIN
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if ("admin".equals(username)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        String password = (String) userData.get("password");
        // 确保密码有编码器前缀（如 {bcrypt}），兼容 DelegatingPasswordEncoder
        if (password != null && !password.startsWith("{")) {
            password = "{bcrypt}" + password;
        }

        return new User(
                username,
                password,
                status == 1,    // enabled
                true,           // accountNonExpired
                true,           // credentialsNonExpired
                true,           // accountNonLocked
                authorities
        );
    }

    /**
     * 根据用户名获取用户详细信息。
     * <p>被 {@link com.example.authserver.config.OAuth2TokenCustomizerConfig} 和
     * {@link com.example.authserver.controller.UserInfoController} 使用。
     *
     * @return UserDTO（不含密码）
     */
    public UserDTO getUserByUsername(String username) {
        return userServiceClient.getUserByUsername(username);
    }

    /**
     * 根据用户ID获取用户信息。
     *
     * @return UserDTO（不含密码）
     */
    public UserDTO getUserById(Long userId) {
        return userServiceClient.getUserById(userId);
    }
}
