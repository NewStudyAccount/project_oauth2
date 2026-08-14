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

@Controller
public class ConsentController {

    private final RegisteredClientRepository registeredClientRepository;

    public ConsentController(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

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

        return "consent";
    }
}