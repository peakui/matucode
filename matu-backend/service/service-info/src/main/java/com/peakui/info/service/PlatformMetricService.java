package com.peakui.info.service;

import com.peakui.common.result.PageResponse;
import com.peakui.info.model.dto.CreatePlatformMetricRequest;
import com.peakui.info.model.dto.UpdatePlatformMetricRequest;
import com.peakui.info.model.vo.PlatformMetricVO;

import java.time.LocalDate;
import java.util.List;

public interface PlatformMetricService {

    PageResponse<PlatformMetricVO> listPlatformMetrics(String metricKey, LocalDate startDate, LocalDate endDate,
                                                       Long pageNum, Long pageSize);

    List<PlatformMetricVO> listLatestMetrics();

    PlatformMetricVO createPlatformMetric(CreatePlatformMetricRequest request);

    PlatformMetricVO updatePlatformMetric(Long id, UpdatePlatformMetricRequest request);

    void deletePlatformMetric(Long id);
}
