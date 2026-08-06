package com.example.clientb.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        if (oidcUser != null) {
            model.addAttribute("username", oidcUser.getPreferredUsername());
            model.addAttribute("email", oidcUser.getEmail());
            model.addAttribute("name", oidcUser.getFullName());
            model.addAttribute("loggedIn", true);
        } else {
            model.addAttribute("loggedIn", false);
        }
        return "index";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        if (oidcUser == null) {
            return "redirect:/";
        }

        model.addAttribute("username", oidcUser.getPreferredUsername());
        model.addAttribute("email", oidcUser.getEmail());
        model.addAttribute("name", oidcUser.getFullName());
        model.addAttribute("sub", oidcUser.getSubject());
        model.addAttribute("issuer", oidcUser.getIssuer());
        model.addAttribute("accessToken", oidcUser.getIdToken().getTokenValue());
        model.addAttribute("claims", oidcUser.getClaims());
        return "profile";
    }
}
