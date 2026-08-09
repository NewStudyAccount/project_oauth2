package com.example.authserver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.authserver.entity.WebhookLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WebhookLogMapper extends BaseMapper<WebhookLog> {
}
