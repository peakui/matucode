package com.peakui.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.message.model.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
