package com.example.authserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final RegisteredClientRepository registeredClientRepository;

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

    @GetMapping("/")
    public String index(HttpServletRequest request, Model model) {
        model.addAttribute("username", request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "");
        return "index";
    }
}
