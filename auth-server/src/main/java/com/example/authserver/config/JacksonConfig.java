package com.example.authserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 配置 - 提供专用的 ObjectMapper 用于 OAuth2 授权数据序列化
 */
@Configuration
public class JacksonConfig {

    public static final String OAUTH2_OBJECT_MAPPER = "oauth2ObjectMapper";

    /**
     * 专用的 ObjectMapper，支持 OAuth2AuthorizationRequest 等复杂对象的序列化/反序列化
     */
    @Bean(OAUTH2_OBJECT_MAPPER)
    public ObjectMapper oauth2ObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 注册 JavaTimeModule 支持 Instant 等时间类型
        objectMapper.registerModule(new JavaTimeModule());

        // 配置多态类型验证器，允许反序列化 Spring Security 和 java.util 类型
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType("org.springframework.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.time.")
                .build();

        // 启用默认类型信息，使 Jackson 在序列化时包含类型信息
        objectMapper.activateDefaultTyping(
                ptv,
                ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE
        );

        return objectMapper;
    }
}
