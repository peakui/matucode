package com.peakui.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.peakui.auth.model.entity.Certification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CertificationMapper extends BaseMapper<Certification> {

    @Select("SELECT cert_name AS name, cert_type AS type, COUNT(*) AS count FROM certifications WHERE cert_status = 1 GROUP BY cert_name, cert_type ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> selectCertificationRankings();
}
