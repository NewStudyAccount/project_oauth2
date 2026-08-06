package com.example.authserver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.authserver.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT u.* FROM sys_user u WHERE u.username = #{username}")
    SysUser selectByUsername(String username);

    @Select("SELECT r.name FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectRoleNamesByUserId(Long userId);
}
