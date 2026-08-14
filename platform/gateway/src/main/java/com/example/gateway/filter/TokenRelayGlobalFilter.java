package com.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Token 中继过滤器
 * 从 Session 获取 OAuth2 Token，添加到请求头转发给下游服务
 */
@Component
public class TokenRelayGlobalFilter implements GlobalFilter, Ordered {

    private final ReactiveOAuth2AuthorizedClientService authorizedClientService;

    public TokenRelayGlobalFilter(ReactiveOAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof OAuth2AuthenticationToken)
                .cast(OAuth2AuthenticationToken.class)
                .flatMap(this::loadAccessToken)
                .map(OAuth2AccessToken::getTokenValue)
                .map(tokenValue -> addAuthorizationHeader(exchange, tokenValue))
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    /**
     * 加载 OAuth2 Access Token
     */
    private Mono<OAuth2AccessToken> loadAccessToken(OAuth2AuthenticationToken authentication) {
        String clientRegistrationId = authentication.getAuthorizedClientRegistrationId();
        String principalName = authentication.getName();

        return authorizedClientService.loadAuthorizedClient(clientRegistrationId, principalName)
                .map(OAuth2AuthorizedClient::getAccessToken);
    }

    /**
     * 将 Token 添加到请求头
     */
    private ServerWebExchange addAuthorizationHeader(ServerWebExchange exchange, String tokenValue) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("Authorization", "Bearer " + tokenValue)
                .build();
        return exchange.mutate().request(request).build();
    }

    @Override
    public int getOrder() {
        return -100; // 高优先级，确保在路由之前执行
    }
}
