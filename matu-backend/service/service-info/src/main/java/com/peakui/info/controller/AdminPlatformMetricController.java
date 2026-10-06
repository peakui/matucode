package com.peakui.info.controller;

import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.info.model.dto.CreatePlatformMetricRequest;
import com.peakui.info.model.dto.UpdatePlatformMetricRequest;
import com.peakui.info.model.vo.PlatformMetricVO;
import com.peakui.info.service.PlatformMetricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "平台指标管理接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/info/platform-metrics")
public class AdminPlatformMetricController {

    private final PlatformMetricService platformMetricService;

    @Operation(summary = "平台指标列表")
    @GetMapping
    public ApiResponse<PageResponse<PlatformMetricVO>> listPlatformMetrics(@RequestParam(required = false) String metricKey,
                                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                           @RequestParam(defaultValue = "1") Long pageNum,
                                                                           @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(platformMetricService.listPlatformMetrics(metricKey, startDate, endDate, pageNum, pageSize));
    }

    @Operation(summary = "最新平台指标")
    @GetMapping("/latest")
    public ApiResponse<List<PlatformMetricVO>> listLatestMetrics() {
        return ApiResponse.success(platformMetricService.listLatestMetrics());
    }

    @Operation(summary = "创建平台指标")
    @PostMapping
    public ApiResponse<PlatformMetricVO> createPlatformMetric(@Valid @RequestBody CreatePlatformMetricRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), platformMetricService.createPlatformMetric(request));
    }

    @Operation(summary = "更新平台指标")
    @PutMapping("/{id}")
    public ApiResponse<PlatformMetricVO> updatePlatformMetric(@PathVariable Long id,
                                                              @Valid @RequestBody UpdatePlatformMetricRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), platformMetricService.updatePlatformMetric(id, request));
    }

    @Operation(summary = "删除平台指标")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePlatformMetric(@PathVariable Long id) {
        platformMetricService.deletePlatformMetric(id);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }
}
