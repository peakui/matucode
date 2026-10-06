package com.peakui.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.course.model.entity.CourseNote;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseNoteMapper extends BaseMapper<CourseNote> {
}

