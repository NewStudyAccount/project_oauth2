package com.example.authserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 登录页面控制器 —— 渲染登录页面和首页。
 */
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final RegisteredClientRepository registeredClientRepository;

    /**
     * 登录页面 —— 如果携带 client_id 参数，显示客户端名称以提升用户体验。
     * <p>用户从客户端跳转到授权服务器登录时，URL 通常带有 client_id 参数。
     */
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String client_id, Model model) {
        if (client_id != null) {
            RegisteredClient client = registeredClientRepository.findByClientId(client_id);
            if (client != null) {
                model.addAttribute("clientName", client.getClientName());
            }
        }
        return "login";
    }

    /** 首页 —— 显示当前登录用户名 */
    @GetMapping("/")
    public String index(HttpServletRequest request, Model model) {
        model.addAttribute("username", request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "");
        return "index";
    }
}
