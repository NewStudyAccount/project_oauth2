package com.example.authserver.service;

import com.example.authserver.entity.SysAuditLog;
import com.example.authserver.repository.SysAuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SysAuditLogMapper auditLogMapper;

    public void log(Long userId, String username, String clientId, String action, String detail, String status) {
        try {
            HttpServletRequest request = getCurrentRequest();

            SysAuditLog auditLog = new SysAuditLog();
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setClientId(clientId);
            auditLog.setAction(action);
            auditLog.setDetail(detail);
            auditLog.setStatus(status);

            if (request != null) {
                auditLog.setIp(getClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }

            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }

    public void logLogin(String username, String status, HttpServletRequest request) {
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setUsername(username);
        auditLog.setAction("LOGIN");
        auditLog.setStatus(status);
        auditLog.setIp(getClientIp(request));
        auditLog.setUserAgent(request.getHeader("User-Agent"));
        auditLogMapper.insert(auditLog);
    }

    public void logLogout(Long userId, String username) {
        log(userId, username, null, "LOGOUT", "用户登出", "SUCCESS");
    }

    public void logAuthorize(Long userId, String username, String clientId, String status) {
        log(userId, username, clientId, "AUTHORIZE", "授权请求", status);
    }

    public void logTokenIssued(Long userId, String username, String clientId) {
        log(userId, username, clientId, "TOKEN_ISSUED", "Token签发", "SUCCESS");
    }

    public void logTokenRevoked(Long userId, String username, String reason) {
        log(userId, username, null, "TOKEN_REVOKED", reason, "SUCCESS");
    }

    public void logRegister(String username, String status) {
        log(null, username, null, "REGISTER", "用户注册", status);
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
