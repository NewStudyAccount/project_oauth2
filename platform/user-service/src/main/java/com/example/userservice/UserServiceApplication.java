package com.example.userservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户中心服务启动入口。
 *
 * <p>提供用户信息查询、用户注册、用户管理等 REST API，
 * 供 auth-center（认证中心）和 client-app（客户端应用）调用。
 */
@SpringBootApplication
@MapperScan("com.example.userservice.repository")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
