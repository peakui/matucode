package com.peakui.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.post.model.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
