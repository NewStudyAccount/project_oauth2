package com.example.appspringboot.client;

import com.example.appspringboot.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * user-service Feign 客户端 —— 声明式调用用户中心 REST API。
 *
 * <p>通过 {@code user-service.url} 配置目标地址（默认 http://localhost:8081）。
 */
@FeignClient(name = "user-service", url = "${user-service.url:http://localhost:8081}")
public interface UserServiceClient {

    /**
     * 根据用户名查询用户信息（不含密码）。
     */
    @GetMapping("/api/users/{username}")
    UserDTO getUserByUsername(@PathVariable("username") String username);
}
