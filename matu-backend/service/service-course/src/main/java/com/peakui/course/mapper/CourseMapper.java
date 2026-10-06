package com.peakui.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.course.model.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM courses WHERE id=#{id} FOR UPDATE")
    Course lockById(@org.apache.ibatis.annotations.Param("id") Long id);
}

