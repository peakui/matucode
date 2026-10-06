package com.peakui.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.auth.model.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户扩展信息 Mapper。
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
