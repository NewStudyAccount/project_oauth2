package com.example.authserver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.authserver.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
