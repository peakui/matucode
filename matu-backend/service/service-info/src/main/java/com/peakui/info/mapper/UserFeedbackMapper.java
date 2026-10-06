package com.peakui.info.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.info.model.entity.UserFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserFeedbackMapper extends BaseMapper<UserFeedback> {
    @Select("SELECT * FROM user_feedbacks WHERE id = #{id} FOR UPDATE")
    UserFeedback lockById(@Param("id") Long id);
}
