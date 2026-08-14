package com.example.authserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ModelAndView handleOAuth2Error(OAuth2AuthenticationException e) {
        log.error("OAuth2认证异常", e);
        String message = e.getError() != null ? e.getError().getDescription() : "认证失败";
        return new ModelAndView("redirect:/error?message=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied() {
        return new ModelAndView("error/no_permission");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(500).body(Map.of("error", "系统内部错误"));
    }
}
