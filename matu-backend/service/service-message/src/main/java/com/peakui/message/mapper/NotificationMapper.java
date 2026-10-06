package com.peakui.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.message.model.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
