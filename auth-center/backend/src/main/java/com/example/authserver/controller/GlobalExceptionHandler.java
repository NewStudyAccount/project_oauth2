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

/**
 * 全局异常处理器 —— 统一处理控制器层异常，返回友好的错误响应。
 *
 * <p>区分三种异常类型：
 * <ul>
 *   <li>OAuth2 认证异常 → 重定向到错误页面</li>
 *   <li>权限不足 → 渲染无权限页面</li>
 *   <li>其他异常 → 返回 500 JSON</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** OAuth2 认证异常（如无效的授权码、过期的 Token 等）→ 重定向到错误页 */
    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ModelAndView handleOAuth2Error(OAuth2AuthenticationException e) {
        log.error("OAuth2认证异常", e);
        String message = e.getError() != null ? e.getError().getDescription() : "认证失败";
        return new ModelAndView("redirect:/error?message=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }

    /** 权限不足（如普通用户访问管理接口）→ 渲染无权限页面 */
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied() {
        return new ModelAndView("error/no_permission");
    }

    /** 其他未预期异常 → 返回 500 JSON（不暴露内部错误详情） */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(500).body(Map.of("error", "系统内部错误"));
    }
}
