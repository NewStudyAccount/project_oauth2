package com.example.authserver.controller;

import com.example.authserver.entity.SysUser;
import com.example.authserver.repository.SysUserMapper;
import com.example.authserver.service.AuditLogService;
import com.example.authserver.service.TokenBlacklistService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/tokens")
@RequiredArgsConstructor
public class AdminTokenController {

    private final TokenBlacklistService tokenBlacklistService;
    private final SysUserMapper sysUserMapper;
    private final AuditLogService auditLogService;

    /**
     * 管理员强制撤销指定用户的 Token
     */
    @PostMapping("/revoke")
    public ResponseEntity<Map<String, String>> revokeUserTokens(
            @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String reason = (String) body.getOrDefault("reason", "admin_revoke");

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        tokenBlacklistService.revokeAllUserTokens(userId, reason);
        auditLogService.logTokenRevoked(userId, user.getUsername(), "管理员撤销: " + reason);

        return ResponseEntity.ok(Map.of("message", "Token 撤销成功"));
    }
}
