package com.peakui.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.auth.model.entity.ExternalIdentity;
import com.peakui.auth.model.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ExternalIdentityMapper extends BaseMapper<ExternalIdentity> {
    @Select("SELECT * FROM users WHERE id = #{id} FOR UPDATE")
    User lockUser(@Param("id") Long id);

    @Insert("INSERT INTO account_deletion_requests(user_id, reason, status, created_at) VALUES (#{id}, #{reason}, 'PENDING_REVIEW', NOW()) ON DUPLICATE KEY UPDATE user_id = VALUES(user_id)")
    void requestDeletion(@Param("id") Long userId, @Param("reason") String reason);
}
