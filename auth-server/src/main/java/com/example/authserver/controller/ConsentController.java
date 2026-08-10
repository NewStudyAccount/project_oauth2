package com.example.authserver.controller;

import com.example.authserver.entity.OAuth2Client;
import com.example.authserver.entity.SysUser;
import com.example.authserver.repository.OAuth2ClientMapper;
import com.example.authserver.service.AccessControlService;
import com.example.authserver.service.CustomOAuth2AuthorizationConsentService;
import com.example.authserver.service.CustomUserDetailsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ConsentController {

    private final OAuth2ClientMapper oauth2ClientMapper;
    private final CustomOAuth2AuthorizationConsentService consentService;
    private final AccessControlService accessControlService;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping("/consent")
    public String consent(
            @RequestParam String client_id,
            @RequestParam String redirect_uri,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "openid profile email") String scope,
            @RequestParam(defaultValue = "code") String response_type,
            Principal principal,
            HttpServletRequest request,
            Model model) {

        OAuth2Client client = oauth2ClientMapper.selectOne(
                new LambdaQueryWrapper<OAuth2Client>()
                        .eq(OAuth2Client::getClientId, client_id)
        );

        if (client == null) {
            return "redirect:/error?message=客户端不存在";
        }

        // 检查用户是否有权访问该客户端
        SysUser user = userDetailsService.getUserByUsername(principal.getName());
        if (user != null && !accessControlService.hasAccess(user.getId(), client_id)) {
            model.addAttribute("message", "您没有权限访问 " + client.getClientName() + " 系统");
            return "error/no_permission";
        }

        // 检查是否已经授权过 (使用 Spring Authorization Server 的 consent 表)
        if (user != null) {
            OAuth2AuthorizationConsent existingConsent = consentService.findById(client_id, principal.getName());
            if (existingConsent != null) {
                // 已授权，直接跳过
                return "redirect:/oauth2/authorize?client_id=" + URLEncoder.encode(client_id, StandardCharsets.UTF_8) +
                        "&redirect_uri=" + URLEncoder.encode(redirect_uri, StandardCharsets.UTF_8) +
                        "&state=" + URLEncoder.encode(state != null ? state : "", StandardCharsets.UTF_8) +
                        "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
                        "&response_type=" + URLEncoder.encode(response_type, StandardCharsets.UTF_8);
            }
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
        model.addAttribute("client_id", client_id);
        model.addAttribute("redirect_uri", redirect_uri);
        model.addAttribute("state", state);
        model.addAttribute("scope", scope);
        model.addAttribute("response_type", response_type);

        return "consent";
    }
}
