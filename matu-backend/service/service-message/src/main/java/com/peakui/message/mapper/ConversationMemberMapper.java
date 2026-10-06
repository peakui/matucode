package com.peakui.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.message.model.entity.ConversationMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMemberMapper extends BaseMapper<ConversationMember> {
}
