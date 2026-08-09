package com.example.authserver.controller;

import com.example.authserver.entity.SysUser;
import com.example.authserver.service.AuditLogService;
import com.example.authserver.service.RegisterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class RegisterController {

    private final RegisterService registerService;
    private final AuditLogService auditLogService;

    @GetMapping("/register")
    public String registerPage(@RequestParam(required = false) String client_id, Model model) {
        model.addAttribute("client_id", client_id);
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String email,
            @RequestParam String code,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String client_id,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            SysUser user = registerService.register(username, password, confirmPassword, email, code, nickname, client_id);
            auditLogService.logRegister(username, "SUCCESS");
            redirectAttributes.addFlashAttribute("message", "注册成功，请登录");
            return "redirect:/login";
        } catch (Exception e) {
            auditLogService.logRegister(username, "FAILED");
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addAttribute("client_id", client_id);
            return "redirect:/register";
        }
    }

    @PostMapping("/send-code")
    public String sendCode(@RequestParam String email, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        try {
            registerService.sendVerificationCode(email, ip);
            return "redirect:/register?codeSent=true";
        } catch (Exception e) {
            return "redirect:/register?error=" + e.getMessage();
        }
    }
}
