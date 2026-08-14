package com.example.authserver.config;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * 包装 RegisteredClientRepository，在查询时检查 settings.client.enabled 字段。
 * 禁用的客户端在 OAuth2 授权流程中返回 null（视为不存在）。
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

    private RegisteredClient filterDisabled(RegisteredClient client) {
        if (client == null) {
            return null;
        }
        Object enabled = client.getClientSettings().getSetting("settings.client.enabled");
        if (enabled != null && !((Boolean) enabled)) {
            return null;
        }
        return client;
    }
}