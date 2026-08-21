package com.example.authserver.controller;

import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

/**
 * OAuth2 授权同意页面控制器。
 *
 * <p>当客户端请求授权且 {@code requireAuthorizationConsent=true} 时，
 * 授权服务器会重定向到此页面，让用户确认是否同意授予所请求的权限（scopes）。
 * <p>用户确认后，表单提交到 Spring Authorization Server 的内置同意处理端点。
 */
@Controller
public class ConsentController {

    private final RegisteredClientRepository registeredClientRepository;

    public ConsentController(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    /**
     * 展示授权同意页面。
     *
     * <p>接收 OAuth2 授权请求参数，将 scope 翻译为中文描述后渲染到模板。
     * <p>用户在页面上选择同意的 scope 后，表单提交到框架的 /oauth2/authorize 端点完成授权。
     */
    @GetMapping("/consent")
    public String consent(
            @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
            @RequestParam(OAuth2ParameterNames.REDIRECT_URI) String redirectUri,
            @RequestParam(value = OAuth2ParameterNames.STATE, required = false) String state,
            @RequestParam(value = OAuth2ParameterNames.SCOPE, defaultValue = "openid profile email") String scope,
            @RequestParam(value = OAuth2ParameterNames.RESPONSE_TYPE, defaultValue = "code") String responseType,
            Principal principal,
            Model model) {

        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            return "redirect:/error?message=客户端不存在";
        }

        // 将英文 scope 名称翻译为中文描述，提升用户体验
        List<String> scopeList = Arrays.stream(scope.split(" "))
                .map(s -> switch (s) {
                    case "openid" -> "身份标识 (openid)";
                    case "profile" -> "基本资料 (profile)";
                    case "email" -> "邮箱地址 (email)";
                    case "phone" -> "手机号码 (phone)";
                    default -> s;
                })
                .toList();

        model.addAttribute("clientName", client.getClientName());
        model.addAttribute("scopes", scopeList);
        model.addAttribute("client_id", clientId);
        model.addAttribute("redirect_uri", redirectUri);
        model.addAttribute("state", state);
        model.addAttribute("scope", scope);
        model.addAttribute("response_type", responseType);

        return "consent";  // 渲染 consent.html 模板
    }
}