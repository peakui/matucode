package com.peakui.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.qa.model.entity.QaComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QaCommentMapper extends BaseMapper<QaComment> {
}
