package com.peakui.info.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.common.result.PageResponse;
import com.peakui.info.exception.InfoException;
import com.peakui.info.mapper.PlatformMetricMapper;
import com.peakui.info.model.dto.CreatePlatformMetricRequest;
import com.peakui.info.model.dto.UpdatePlatformMetricRequest;
import com.peakui.info.model.entity.PlatformMetric;
import com.peakui.info.model.vo.PlatformMetricVO;
import com.peakui.info.service.PlatformMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformMetricServiceImpl implements PlatformMetricService {

    private final PlatformMetricMapper platformMetricMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResponse<PlatformMetricVO> listPlatformMetrics(String metricKey, LocalDate startDate, LocalDate endDate,
                                                              Long pageNum, Long pageSize) {
        checkAdmin();
        long currentPage = normalizePageNum(pageNum);
        long currentSize = normalizePageSize(pageSize);
        LambdaQueryWrapper<PlatformMetric> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(metricKey)) {
            wrapper.eq(PlatformMetric::getMetricKey, metricKey.trim());
        }
        if (startDate != null) {
            wrapper.ge(PlatformMetric::getMetricDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(PlatformMetric::getMetricDate, endDate);
        }
        wrapper.orderByDesc(PlatformMetric::getMetricDate).orderByAsc(PlatformMetric::getMetricKey);

        Page<PlatformMetric> page = platformMetricMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResponse.of(currentPage, currentSize, page.getTotal(), Collections.emptyList());
        }
        return PageResponse.of(currentPage, currentSize, page.getTotal(), page.getRecords().stream().map(this::toVO).toList());
    }

    @Override
    public List<PlatformMetricVO> listLatestMetrics() {
        checkAdmin();
        List<PlatformMetric> metrics = platformMetricMapper.selectList(new LambdaQueryWrapper<PlatformMetric>()
                .orderByDesc(PlatformMetric::getMetricDate, PlatformMetric::getCreatedAt));
        Map<String, PlatformMetric> latestByKey = metrics.stream()
                .collect(Collectors.toMap(PlatformMetric::getMetricKey, Function.identity(), (first, ignored) -> first));
        return latestByKey.values().stream()
                .map(this::toVO)
                .sorted((left, right) -> left.getMetricKey().compareTo(right.getMetricKey()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformMetricVO createPlatformMetric(CreatePlatformMetricRequest request) {
        checkAdmin();
        validateJson(request.getExtra(), "扩展维度数据");
        PlatformMetric platformMetric = new PlatformMetric();
        platformMetric.setMetricDate(request.getMetricDate());
        platformMetric.setMetricKey(request.getMetricKey().trim());
        platformMetric.setMetricValue(request.getMetricValue());
        platformMetric.setExtra(normalizeText(request.getExtra()));
        platformMetric.setCreatedAt(LocalDateTime.now());
        try {
            platformMetricMapper.insert(platformMetric);
        } catch (DuplicateKeyException e) {
            log.warn("创建平台指标失败: 该日期的指标已存在, metricKey={}, metricDate={}",
                    platformMetric.getMetricKey(), platformMetric.getMetricDate());
            throw new InfoException("该日期的指标已存在");
        }
        log.info("创建平台指标成功, metricId={}, metricKey={}, metricDate={}",
                platformMetric.getId(), platformMetric.getMetricKey(), platformMetric.getMetricDate());
        return toVO(platformMetric);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformMetricVO updatePlatformMetric(Long id, UpdatePlatformMetricRequest request) {
        checkAdmin();
        validateJson(request.getExtra(), "扩展维度数据");
        PlatformMetric platformMetric = getPlatformMetric(id);
        platformMetric.setMetricValue(request.getMetricValue());
        platformMetric.setExtra(normalizeText(request.getExtra()));
        platformMetricMapper.updateById(platformMetric);
        log.info("更新平台指标成功, metricId={}, metricKey={}, metricDate={}",
                id, platformMetric.getMetricKey(), platformMetric.getMetricDate());
        return toVO(platformMetric);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlatformMetric(Long id) {
        checkAdmin();
        getPlatformMetric(id);
        platformMetricMapper.deleteById(id);
        log.info("删除平台指标成功, metricId={}", id);
    }

    private PlatformMetric getPlatformMetric(Long id) {
        PlatformMetric platformMetric = platformMetricMapper.selectById(id);
        if (platformMetric == null) {
            throw new InfoException("平台指标不存在");
        }
        return platformMetric;
    }

    private void validateJson(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        try {
            objectMapper.readTree(value);
        } catch (Exception e) {
            throw new InfoException(fieldName + "必须是合法JSON");
        }
    }

    private PlatformMetricVO toVO(PlatformMetric platformMetric) {
        return PlatformMetricVO.builder()
                .id(platformMetric.getId())
                .metricDate(platformMetric.getMetricDate())
                .metricKey(platformMetric.getMetricKey())
                .metricValue(platformMetric.getMetricValue())
                .extra(platformMetric.getExtra())
                .createdAt(platformMetric.getCreatedAt())
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
