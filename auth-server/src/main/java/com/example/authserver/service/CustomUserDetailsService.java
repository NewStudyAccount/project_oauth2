package com.example.authserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.authserver.entity.SysUser;
import com.example.authserver.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getStatus, 1)
        );

        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if ("admin".equals(sysUser.getUsername())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new User(
                sysUser.getUsername(),
                sysUser.getPassword(),
                sysUser.getStatus() == 1,
                true, true, true,
                authorities
        );
    }

    /**
     * 根据用户名获取用户信息（含详细信息）
     */
    public SysUser getUserByUsername(String username) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
    }

    /**
     * 根据用户ID获取用户信息
     */
    public SysUser getUserById(Long userId) {
        return sysUserMapper.selectById(userId);
    }
}
