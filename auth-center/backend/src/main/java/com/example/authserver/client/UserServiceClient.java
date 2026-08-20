package com.example.authserver.client;

import com.example.authserver.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * user-service Feign 客户端 —— 声明式调用用户中心 REST API。
 *
 * <p>通过 {@code user-service.url} 配置目标地址（默认 http://localhost:8081）。
 */
@FeignClient(name = "user-service", url = "${user-service.url:http://localhost:8081}")
public interface UserServiceClient {

    /**
     * 根据用户名查询用户（不含密码）。
     */
    @GetMapping("/api/users/{username}")
    UserDTO getUserByUsername(@PathVariable("username") String username);

    /**
     * 根据用户名查询完整用户信息（含密码哈希，仅供认证使用）。
     * <p>返回 Map 而非 UserDTO，因为需要包含 password 字段。
     */
    @GetMapping("/api/users/{username}/full")
    Map<String, Object> getUserByUsernameFull(@PathVariable("username") String username);

    /**
     * 根据 ID 查询用户（不含密码）。
     */
    @GetMapping("/api/users/id/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);

    /**
     * 查询所有用户列表（不含密码）。
     */
    @GetMapping("/api/users")
    List<UserDTO> getUsers();

    /**
     * 创建用户（注册）。
     */
    @PostMapping("/api/users")
    UserDTO createUser(@RequestBody Map<String, String> request);

    /**
     * 更新用户启用/禁用状态。
     */
    @PutMapping("/api/users/{id}/status")
    Map<String, String> updateUserStatus(@PathVariable("id") Long id,
                                          @RequestBody Map<String, Boolean> request);
}
