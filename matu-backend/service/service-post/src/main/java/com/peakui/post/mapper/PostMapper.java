package com.peakui.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.post.model.entity.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
}
