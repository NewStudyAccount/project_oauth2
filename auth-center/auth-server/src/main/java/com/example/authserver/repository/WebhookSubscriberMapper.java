package com.example.authserver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.authserver.entity.WebhookSubscriber;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WebhookSubscriberMapper extends BaseMapper<WebhookSubscriber> {
}
