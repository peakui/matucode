package com.peakui.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.auth.model.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色表 Mapper。
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据用户 ID 查询角色编码集合。
     */
    @Select("""
            SELECT r.role_code
            FROM roles r
            INNER JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.status = 1
            """)
    List<String> selectRoleCodesByUserId(Long userId);

    /**
     * 查询默认用户角色。
     */
    @Select("""
            SELECT *
            FROM roles
            WHERE role_code = 'USER'
            LIMIT 1
            """)
    Role selectDefaultUserRole();
}
