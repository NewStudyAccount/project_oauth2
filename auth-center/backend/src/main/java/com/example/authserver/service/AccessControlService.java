package com.example.authserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.authserver.entity.UserClientAccess;
import com.example.authserver.repository.UserClientAccessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 访问控制服务 —— 管理用户对 OAuth2 客户端的访问权限。
 *
 * <p>通过 {@code user_client_access} 表记录用户-客户端的访问关系。
 * <p>策略：没有记录时默认允许（适用于内部应用自动授权场景），
 * 有记录时根据 allowed 字段判断（1=允许，0=拒绝）。
 */
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UserClientAccessMapper userClientAccessMapper;

    /**
     * 检查用户是否有权访问指定客户端。
     * <p>无记录时默认允许（内部应用自动授权策略）。
     */
    public boolean hasAccess(Long userId, String clientId) {
        UserClientAccess access = userClientAccessMapper.selectOne(
                new LambdaQueryWrapper<UserClientAccess>()
                        .eq(UserClientAccess::getUserId, userId)
                        .eq(UserClientAccess::getClientId, clientId)
        );
        return access == null || access.getAllowed() == 1;
    }

    /**
     * 设置用户对客户端的访问权限（存在则更新，不存在则新增）。
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
