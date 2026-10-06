package com.peakui.info.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.peakui.common.result.PageResponse;
import com.peakui.info.exception.InfoException;
import com.peakui.info.mapper.SysConfigMapper;
import com.peakui.info.model.dto.CreateSysConfigRequest;
import com.peakui.info.model.dto.UpdateSysConfigRequest;
import com.peakui.info.model.entity.SysConfig;
import com.peakui.info.model.vo.SysConfigVO;
import com.peakui.info.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    @Override
    public List<SysConfigVO> listPublicConfigs(String groupName) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getIsPublic, 1);
        if (StringUtils.hasText(groupName)) {
            wrapper.eq(SysConfig::getGroupName, groupName.trim());
        }
        wrapper.orderByAsc(SysConfig::getGroupName, SysConfig::getConfigKey);
        return sysConfigMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public SysConfigVO getPublicConfig(String configKey) {
        SysConfig sysConfig = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey)
                .eq(SysConfig::getIsPublic, 1));
        if (sysConfig == null) {
            throw new InfoException("配置不存在或不可公开访问");
        }
        return toVO(sysConfig);
    }

    @Override
    public PageResponse<SysConfigVO> listAdminConfigs(String groupName, Integer isPublic, String keyword, Long pageNum, Long pageSize) {
        checkAdmin();
        long currentPage = normalizePageNum(pageNum);
        long currentSize = normalizePageSize(pageSize);
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(groupName)) {
            wrapper.eq(SysConfig::getGroupName, groupName.trim());
        }
        if (isPublic != null) {
            wrapper.eq(SysConfig::getIsPublic, isPublic);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysConfig::getConfigKey, keyword).or().like(SysConfig::getDescription, keyword));
        }
        wrapper.orderByAsc(SysConfig::getGroupName, SysConfig::getConfigKey);

        Page<SysConfig> page = sysConfigMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResponse.of(currentPage, currentSize, page.getTotal(), Collections.emptyList());
        }
        return PageResponse.of(currentPage, currentSize, page.getTotal(), page.getRecords().stream().map(this::toVO).toList());
    }

    @Override
    public SysConfigVO getAdminConfig(String configKey) {
        checkAdmin();
        return toVO(getSysConfig(configKey));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysConfigVO createSysConfig(CreateSysConfigRequest request) {
        checkAdmin();
        String configKey = request.getConfigKey().trim();
        if (sysConfigMapper.selectById(configKey) != null) {
            throw new InfoException("配置键已存在");
        }
        SysConfig sysConfig = new SysConfig();
        sysConfig.setConfigKey(configKey);
        sysConfig.setConfigValue(request.getConfigValue());
        sysConfig.setDescription(normalizeText(request.getDescription()));
        sysConfig.setGroupName(StringUtils.hasText(request.getGroupName()) ? request.getGroupName().trim() : "general");
        sysConfig.setIsPublic(request.getIsPublic() == null ? 0 : request.getIsPublic());
        sysConfig.setUpdatedAt(LocalDateTime.now());
        sysConfigMapper.insert(sysConfig);
        log.info("创建系统配置成功, configKey={}, groupName={}", configKey, sysConfig.getGroupName());
        return toVO(sysConfig);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysConfigVO updateSysConfig(String configKey, UpdateSysConfigRequest request) {
        checkAdmin();
        SysConfig sysConfig = getSysConfig(configKey);
        sysConfig.setConfigValue(request.getConfigValue());
        sysConfig.setDescription(normalizeText(request.getDescription()));
        sysConfig.setGroupName(StringUtils.hasText(request.getGroupName()) ? request.getGroupName().trim() : "general");
        sysConfig.setIsPublic(request.getIsPublic() == null ? 0 : request.getIsPublic());
        sysConfig.setUpdatedAt(LocalDateTime.now());
        sysConfigMapper.updateById(sysConfig);
        log.info("更新系统配置成功, configKey={}, groupName={}", configKey, sysConfig.getGroupName());
        return toVO(sysConfig);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSysConfig(String configKey) {
        checkAdmin();
        getSysConfig(configKey);
        sysConfigMapper.deleteById(configKey);
        log.info("删除系统配置成功, configKey={}", configKey);
    }

    private SysConfig getSysConfig(String configKey) {
        SysConfig sysConfig = sysConfigMapper.selectById(configKey);
        if (sysConfig == null) {
            throw new InfoException("配置不存在");
        }
        return sysConfig;
    }

    private SysConfigVO toVO(SysConfig sysConfig) {
        return SysConfigVO.builder()
                .configKey(sysConfig.getConfigKey())
                .configValue(sysConfig.getConfigValue())
                .description(sysConfig.getDescription())
                .groupName(sysConfig.getGroupName())
                .isPublic(sysConfig.getIsPublic())
                .updatedAt(sysConfig.getUpdatedAt())
                .build();
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }

    private void checkAdmin() {
        StpUtil.checkLogin();
        StpUtil.checkRole("ADMIN");
    }
}
