package com.example.authserver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.authserver.entity.UserClientAccess;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserClientAccessMapper extends BaseMapper<UserClientAccess> {
}
