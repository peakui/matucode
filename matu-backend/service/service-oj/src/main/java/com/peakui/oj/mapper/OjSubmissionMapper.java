package com.peakui.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.oj.model.entity.OjSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OjSubmissionMapper extends BaseMapper<OjSubmission> {

    @Select("SELECT COUNT(DISTINCT problem_id) FROM oj_submissions WHERE user_id = #{userId} AND status = #{status}")
    Integer countDistinctProblemsByUserId(@Param("userId") Long userId, @Param("status") Integer status);

    @Select("SELECT COUNT(DISTINCT user_id) FROM oj_submissions")
    Long countDistinctParticipants();
}
