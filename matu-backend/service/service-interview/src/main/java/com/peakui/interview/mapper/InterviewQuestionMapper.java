package com.peakui.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.interview.model.entity.InterviewQuestion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewQuestionMapper extends BaseMapper<InterviewQuestion> {
}
