package com.peakui.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.qa.model.entity.QaCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QaCategoryMapper extends BaseMapper<QaCategory> {
}
