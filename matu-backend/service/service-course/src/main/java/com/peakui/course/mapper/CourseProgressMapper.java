package com.peakui.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.course.model.entity.CourseProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CourseProgressMapper extends BaseMapper<CourseProgress> {

    @Select("SELECT COUNT(DISTINCT user_id) FROM course_progress")
    Long countDistinctStudents();
}
