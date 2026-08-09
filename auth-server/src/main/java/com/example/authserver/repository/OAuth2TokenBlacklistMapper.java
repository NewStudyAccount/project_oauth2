package com.example.authserver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.authserver.entity.OAuth2TokenBlacklist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OAuth2TokenBlacklistMapper extends BaseMapper<OAuth2TokenBlacklist> {
}
