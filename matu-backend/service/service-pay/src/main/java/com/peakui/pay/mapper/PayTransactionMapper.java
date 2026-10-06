package com.peakui.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.pay.model.entity.PayTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayTransactionMapper extends BaseMapper<PayTransaction> {
}
