package com.example.authserver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.authserver.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
}
