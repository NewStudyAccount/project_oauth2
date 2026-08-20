package com.example.userservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.userservice.entity.SysUser;
import com.example.userservice.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务 —— 提供用户查询、注册、管理等业务逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper sysUserMapper;

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 根据用户名查询用户（不含密码）。
     */
    public SysUser getUserByUsername(String username) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
        if (user != null) {
            user.setPassword(null);  // 不返回密码
        }
        return user;
    }

    /**
     * 根据用户名查询完整用户信息（含密码哈希，仅供认证使用）。
     */
    public SysUser getUserByUsernameFull(String username) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
    }

    /**
     * 根据 ID 查询用户（不含密码）。
     */
    public SysUser getUserById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    /**
     * 查询所有用户列表（不含密码）。
     */
    public List<SysUser> listUsers() {
        List<SysUser> users = sysUserMapper.selectList(null);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    /**
     * 创建用户（注册）。
     *
     * @return 创建成功的用户（不含密码）
     * @throws RuntimeException 用户名或邮箱已存在时抛出异常
     */
    public SysUser createUser(String username, String password, String email, String nickname) {
        // 用户名唯一性检查
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 邮箱唯一性检查
        if (email != null && !email.isEmpty()) {
            count = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getEmail, email)
            );
            if (count > 0) {
                throw new RuntimeException("邮箱已被注册");
            }
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(PASSWORD_ENCODER.encode(password));
        user.setNickname(nickname != null ? nickname : username);
        user.setEmail(email);
        user.setStatus(1);
        sysUserMapper.insert(user);

        user.setPassword(null);  // 不返回密码
        log.info("用户注册成功: {}", username);
        return user;
    }

    /**
     * 更新用户启用/禁用状态。
     *
     * @return 更新后的用户（不含密码），用户不存在返回 null
     */
    public SysUser updateUserStatus(Long id, boolean enabled) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return null;
        }
        user.setStatus(enabled ? 1 : 0);
        sysUserMapper.updateById(user);
        user.setPassword(null);
        return user;
    }
}
