package com.peakui.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.auth.model.entity.VipActivationRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VipActivationRecordMapper extends BaseMapper<VipActivationRecord> {

    @Insert("INSERT IGNORE INTO vip_activation_records (id, activation_key, user_id, days, level, created_at) "
            + "VALUES (#{id}, #{activationKey}, #{userId}, #{days}, #{level}, #{createdAt})")
    int insertIgnore(VipActivationRecord record);

    @Select("SELECT * FROM vip_activation_records WHERE activation_key = #{activationKey} FOR UPDATE")
    VipActivationRecord selectByActivationKeyForUpdate(@Param("activationKey") String activationKey);
}
