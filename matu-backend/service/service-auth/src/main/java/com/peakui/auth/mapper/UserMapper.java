package com.peakui.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.auth.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // Lock the stable parent row, including when the user has no profile yet.
    @org.apache.ibatis.annotations.Select("SELECT * FROM users WHERE id = #{userId} FOR UPDATE")
    User selectByIdForUpdate(@org.apache.ibatis.annotations.Param("userId") Long userId);
}
