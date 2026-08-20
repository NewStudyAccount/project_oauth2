package com.example.authserver.config;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * 包装 RegisteredClientRepository，在查询时检查 settings.client.enabled 字段。
 * 禁用的客户端在 OAuth2 授权流程中返回 null（视为不存在）。
 */
/**
 * 带启用状态检查的客户端仓库装饰器（Decorator 模式）。
 *
 * <p>包装 {@link RegisteredClientRepository}，在查询时检查客户端的
 * {@code settings.client.enabled} 设置。禁用的客户端返回 null，
 * 使其在 OAuth2 授权流程中表现为"不存在"——拒绝该客户端的一切授权请求。
 *
 * <p>写入操作（save）直接委托给底层仓库，不做过滤。
 */
public class EnabledCheckingRegisteredClientRepository implements RegisteredClientRepository {

    private final RegisteredClientRepository delegate;

    public EnabledCheckingRegisteredClientRepository(RegisteredClientRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        delegate.save(registeredClient);
    }

    @Override
    public RegisteredClient findById(String id) {
        RegisteredClient client = delegate.findById(id);
        return filterDisabled(client);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        RegisteredClient client = delegate.findByClientId(clientId);
        return filterDisabled(client);
    }

    /**
     * 过滤已禁用的客户端。
     * <p>通过 {@code settings.client.enabled} 判断：false 或不存在该设置时默认启用。
     */
    private RegisteredClient filterDisabled(RegisteredClient client) {
        if (client == null) {
            return null;
        }
        Object enabled = client.getClientSettings().getSetting("settings.client.enabled");
        if (enabled != null && !((Boolean) enabled)) {
            return null;  // 客户端已禁用，返回 null 使其在 OAuth2 流程中表现为不存在
        }
        return client;
    }
}