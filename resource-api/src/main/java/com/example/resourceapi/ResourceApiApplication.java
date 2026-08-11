package com.example.resourceapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ResourceApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceApiApplication.class, args);
    }
}
