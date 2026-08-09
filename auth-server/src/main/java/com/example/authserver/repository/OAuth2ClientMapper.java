package com.example.authserver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.authserver.entity.OAuth2Client;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OAuth2ClientMapper extends BaseMapper<OAuth2Client> {
}
