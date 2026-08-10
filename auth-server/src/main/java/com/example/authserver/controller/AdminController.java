package com.example.authserver.controller;

import com.example.authserver.entity.OAuth2Client;
import com.example.authserver.entity.SysUser;
import com.example.authserver.entity.UserClientAccess;
import com.example.authserver.repository.OAuth2ClientMapper;
import com.example.authserver.repository.SysUserMapper;
import com.example.authserver.repository.UserClientAccessMapper;
import com.example.authserver.service.AccessControlService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final SysUserMapper sysUserMapper;
    private final OAuth2ClientMapper oauth2ClientMapper;
    private final UserClientAccessMapper userClientAccessMapper;
    private final AccessControlService accessControlService;

    @GetMapping("/users")
    public List<SysUser> listUsers() {
        return sysUserMapper.selectList(null);
    }

    @GetMapping("/users/{id}/access")
    public List<UserClientAccess> getUserAccess(@PathVariable Long id) {
        return userClientAccessMapper.selectList(
                new LambdaQueryWrapper<UserClientAccess>()
                        .eq(UserClientAccess::getUserId, id)
        );
    }

    @PutMapping("/users/{id}/access")
    public ResponseEntity<Map<String, String>> setUserAccess(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String clientId = (String) body.get("clientId");
        Boolean allowed = (Boolean) body.get("allowed");

        if (clientId == null || allowed == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "参数不完整"));
        }

        accessControlService.setAccess(id, clientId, allowed);
        return ResponseEntity.ok(Map.of("message", "权限更新成功"));
    }

    @GetMapping("/clients")
    public List<OAuth2Client> listClients() {
        return oauth2ClientMapper.selectList(null);
    }
}
