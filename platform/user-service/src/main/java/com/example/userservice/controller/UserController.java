package com.example.userservice.controller;

import com.example.userservice.entity.SysUser;
import com.example.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户中心 REST API。
 *
 * <p>提供用户查询、注册、管理等接口，供 auth-center 和 client-app 调用。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 根据用户名查询用户信息（不含密码）。
     */
    @GetMapping("/{username}")
    public ResponseEntity<SysUser> getUserByUsername(@PathVariable String username) {
        SysUser user = userService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    /**
     * 根据用户名查询完整用户信息（含密码哈希，仅供认证使用）。
     */
    @GetMapping("/{username}/full")
    public ResponseEntity<SysUser> getUserByUsernameFull(@PathVariable String username) {
        SysUser user = userService.getUserByUsernameFull(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    /**
     * 根据 ID 查询用户信息（不含密码）。
     */
    @GetMapping("/id/{id}")
    public ResponseEntity<SysUser> getUserById(@PathVariable Long id) {
        SysUser user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    /**
     * 查询所有用户列表（不含密码）。
     */
    @GetMapping
    public List<SysUser> listUsers() {
        return userService.listUsers();
    }

    /**
     * 创建用户（注册）。
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");
        String nickname = body.get("nickname");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名和密码不能为空"));
        }

        try {
            SysUser user = userService.createUser(username, password, email, nickname);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 更新用户启用/禁用状态。
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id,
                                               @RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "参数不完整"));
        }

        SysUser user = userService.updateUserStatus(id, enabled);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", enabled ? "用户已启用" : "用户已禁用"));
    }
}
