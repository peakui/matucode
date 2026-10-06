package com.peakui.info.service;

import com.peakui.common.result.PageResponse;
import com.peakui.info.model.dto.CreateSysConfigRequest;
import com.peakui.info.model.dto.UpdateSysConfigRequest;
import com.peakui.info.model.vo.SysConfigVO;

import java.util.List;

public interface SysConfigService {

    List<SysConfigVO> listPublicConfigs(String groupName);

    SysConfigVO getPublicConfig(String configKey);

    PageResponse<SysConfigVO> listAdminConfigs(String groupName, Integer isPublic, String keyword, Long pageNum, Long pageSize);

    SysConfigVO getAdminConfig(String configKey);

    SysConfigVO createSysConfig(CreateSysConfigRequest request);

    SysConfigVO updateSysConfig(String configKey, UpdateSysConfigRequest request);

    void deleteSysConfig(String configKey);
}
