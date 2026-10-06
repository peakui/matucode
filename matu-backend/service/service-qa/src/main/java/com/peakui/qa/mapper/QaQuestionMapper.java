package com.peakui.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.qa.model.entity.QaQuestion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QaQuestionMapper extends BaseMapper<QaQuestion> {
}
