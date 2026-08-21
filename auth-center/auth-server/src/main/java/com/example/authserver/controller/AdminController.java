package com.example.authserver.controller;

import com.example.authserver.client.UserServiceClient;
import com.example.authserver.dto.ClientConverter;
import com.example.authserver.dto.ClientDTO;
import com.example.authserver.dto.UserDTO;
import com.example.authserver.entity.SysAuditLog;
import com.example.authserver.entity.UserClientAccess;
import com.example.authserver.repository.SysAuditLogMapper;
import com.example.authserver.repository.UserClientAccessMapper;
import com.example.authserver.service.AccessControlService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理后台 API 控制器。
 *
 * <p>提供 OAuth2 客户端管理、用户管理、权限管理、审计日志等管理功能。
 * <p>所有接口需要 ADMIN 角色才能访问（{@code @PreAuthorize("hasRole('ADMIN')")})。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")  // 类级别权限控制：所有接口都需要 ADMIN 角色
public class AdminController {

    private final RegisteredClientRepository registeredClientRepository;
    private final UserServiceClient userServiceClient;
    private final UserClientAccessMapper userClientAccessMapper;
    private final SysAuditLogMapper auditLogMapper;
    private final AccessControlService accessControlService;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // ==================== 客户端管理 ====================

    @GetMapping("/clients")
    public List<ClientDTO> listClients() {
        // JdbcRegisteredClientRepository 没有 findAll，通过 JdbcTemplate 查询所有 ID
        List<String> ids = findAllClientIds();
        return ids.stream()
                .map(registeredClientRepository::findById)
                .filter(client -> client != null)
                .map(ClientConverter::toDTO)
                .toList();
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<ClientDTO> getClient(@PathVariable String id) {
        RegisteredClient client = registeredClientRepository.findById(id);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ClientConverter.toDTO(client));
    }

    @PostMapping("/clients")
    public ResponseEntity<ClientDTO> createClient(@RequestBody ClientDTO dto) {
        if (dto.getClientId() == null || dto.getClientId().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (dto.getAuthorizationGrantTypes() == null || dto.getAuthorizationGrantTypes().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        RegisteredClient client = ClientConverter.toEntity(dto, passwordEncoder::encode);
        registeredClientRepository.save(client);
        return ResponseEntity.ok(ClientConverter.toDTO(client));
    }

    @PutMapping("/clients/{id}")
    public ResponseEntity<ClientDTO> updateClient(@PathVariable String id, @RequestBody ClientDTO dto) {
        RegisteredClient existing = registeredClientRepository.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        RegisteredClient updated = ClientConverter.toEntityForUpdate(dto, existing, passwordEncoder::encode);
        registeredClientRepository.save(updated);
        return ResponseEntity.ok(ClientConverter.toDTO(updated));
    }

    @DeleteMapping("/clients/{id}")
    public ResponseEntity<Map<String, String>> deleteClient(@PathVariable String id) {
        RegisteredClient existing = registeredClientRepository.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        // JdbcRegisteredClientRepository 没有 delete，通过 JdbcTemplate 删除
        deleteClientById(id);
        return ResponseEntity.ok(Map.of("message", "客户端已删除"));
    }

    @PutMapping("/clients/{id}/status")
    public ResponseEntity<Map<String, String>> setClientStatus(@PathVariable String id,
                                                                @RequestBody Map<String, Boolean> body) {
        RegisteredClient existing = registeredClientRepository.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "参数不完整"));
        }
        // 重建 RegisteredClient，只修改 enabled 字段
        ClientDTO dto = ClientConverter.toDTO(existing);
        dto.setEnabled(enabled);
        RegisteredClient updated = ClientConverter.toEntityForUpdate(dto, existing, passwordEncoder::encode);
        registeredClientRepository.save(updated);
        return ResponseEntity.ok(Map.of("message", enabled ? "客户端已启用" : "客户端已禁用"));
    }

    // ==================== 用户管理（通过 user-service） ====================

    @GetMapping("/users")
    public List<UserDTO> listUsers() {
        return userServiceClient.getUsers();
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<Map<String, String>> setUserStatus(@PathVariable Long id,
                                                              @RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "参数不完整"));
        }
        Map<String, String> result = userServiceClient.updateUserStatus(id, Map.of("enabled", enabled));
        return ResponseEntity.ok(result);
    }

    // ==================== 权限管理 ====================

    @GetMapping("/access")
    public List<UserClientAccess> getUserAccess(@RequestParam Long userId) {
        return userClientAccessMapper.selectList(
                new LambdaQueryWrapper<UserClientAccess>()
                        .eq(UserClientAccess::getUserId, userId)
        );
    }

    @PutMapping("/access")
    public ResponseEntity<Map<String, String>> setAccess(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String clientId = (String) body.get("clientId");
        Boolean allowed = (Boolean) body.get("allowed");

        if (clientId == null || allowed == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "参数不完整"));
        }

        accessControlService.setAccess(userId, clientId, allowed);
        return ResponseEntity.ok(Map.of("message", "权限更新成功"));
    }

    // ==================== 审计日志 ====================

    @GetMapping("/audit-logs")
    public List<SysAuditLog> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username) {
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>();
        if (action != null && !action.isEmpty()) {
            wrapper.eq(SysAuditLog::getAction, action);
        }
        if (username != null && !username.isEmpty()) {
            wrapper.eq(SysAuditLog::getUsername, username);
        }
        wrapper.orderByDesc(SysAuditLog::getId).last("LIMIT 200");
        return auditLogMapper.selectList(wrapper);
    }

    // ==================== 统计概览 ====================

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return Map.of(
                "userCount", userServiceClient.getUsers().size(),
                "clientCount", findAllClientIds().size(),
                "auditLogCount", auditLogMapper.selectCount(null)
        );
    }

    // ==================== 辅助方法 ====================

    /**
     * JdbcRegisteredClientRepository 没有 findAll，通过 JdbcTemplate 查询所有 ID
     */
    private List<String> findAllClientIds() {
        return jdbcTemplate.queryForList(
                "SELECT id FROM oauth2_registered_client", String.class);
    }

    private void deleteClientById(String id) {
        jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE id = ?", id);
    }
}