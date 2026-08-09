package com.example.authserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.authserver.entity.UserClientAccess;
import com.example.authserver.repository.UserClientAccessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UserClientAccessMapper userClientAccessMapper;

    /**
     * 检查用户是否有权访问指定客户端
     */
    public boolean hasAccess(Long userId, String clientId) {
        UserClientAccess access = userClientAccessMapper.selectOne(
                new LambdaQueryWrapper<UserClientAccess>()
                        .eq(UserClientAccess::getUserId, userId)
                        .eq(UserClientAccess::getClientId, clientId)
        );
        // 如果没有记录，默认允许（内部应用自动授权）
        return access == null || access.getAllowed() == 1;
    }

    /**
     * 设置用户对客户端的访问权限
     */
    public void setAccess(Long userId, String clientId, boolean allowed) {
        UserClientAccess existing = userClientAccessMapper.selectOne(
                new LambdaQueryWrapper<UserClientAccess>()
                        .eq(UserClientAccess::getUserId, userId)
                        .eq(UserClientAccess::getClientId, clientId)
        );

        if (existing != null) {
            existing.setAllowed(allowed ? 1 : 0);
            userClientAccessMapper.updateById(existing);
        } else {
            UserClientAccess access = new UserClientAccess();
            access.setUserId(userId);
            access.setClientId(clientId);
            access.setAllowed(allowed ? 1 : 0);
            userClientAccessMapper.insert(access);
        }
    }
}
