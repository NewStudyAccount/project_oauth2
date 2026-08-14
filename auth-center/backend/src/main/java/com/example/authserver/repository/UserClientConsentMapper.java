package com.example.authserver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.authserver.entity.UserClientConsent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserClientConsentMapper extends BaseMapper<UserClientConsent> {
}
