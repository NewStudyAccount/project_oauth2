package com.example.authserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.authserver.entity.OAuth2Client;
import com.example.authserver.repository.OAuth2ClientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final OAuth2ClientMapper oauth2ClientMapper;
    private final PasswordEncoder passwordEncoder;

    public List<OAuth2Client> listAll() {
        return oauth2ClientMapper.selectList(null);
    }

    public OAuth2Client getByClientId(String clientId) {
        return oauth2ClientMapper.selectOne(
                new LambdaQueryWrapper<OAuth2Client>()
                        .eq(OAuth2Client::getClientId, clientId)
        );
    }

    public OAuth2Client create(OAuth2Client client) {
        if (client.getClientSecret() != null && !client.getClientSecret().isEmpty()) {
            client.setClientSecret(passwordEncoder.encode(client.getClientSecret()));
        }
        oauth2ClientMapper.insert(client);
        return client;
    }

    public void update(OAuth2Client client) {
        oauth2ClientMapper.updateById(client);
    }

    public void delete(Long id) {
        oauth2ClientMapper.deleteById(id);
    }
}
