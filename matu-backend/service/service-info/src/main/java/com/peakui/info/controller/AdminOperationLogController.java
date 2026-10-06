package com.peakui.info.controller;

import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.info.model.dto.CreateOperationLogRequest;
import com.peakui.info.model.vo.OperationLogVO;
import com.peakui.info.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "操作日志管理接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/info/operation-logs")
public class AdminOperationLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "操作日志列表")
    @GetMapping
    public ApiResponse<PageResponse<OperationLogVO>> listOperationLogs(@RequestParam(required = false) Long userId,
                                                                        @RequestParam(required = false) String username,
                                                                        @RequestParam(required = false) String module,
                                                                        @RequestParam(required = false) String action,
                                                                        @RequestParam(required = false) String targetType,
                                                                        @RequestParam(required = false) Long targetId,
                                                                        @RequestParam(required = false) Integer result,
                                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                                                        @RequestParam(defaultValue = "1") Long pageNum,
                                                                        @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(operationLogService.listOperationLogs(userId, username, module, action, targetType, targetId, result, startTime, endTime, pageNum, pageSize));
    }

    @Operation(summary = "操作日志详情")
    @GetMapping("/{id}")
    public ApiResponse<OperationLogVO> getOperationLogDetail(@PathVariable Long id) {
        return ApiResponse.success(operationLogService.getOperationLogDetail(id));
    }

    @Operation(summary = "写入操作日志")
    @PostMapping("/internal")
    public ApiResponse<OperationLogVO> createOperationLog(@Valid @RequestBody CreateOperationLogRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), operationLogService.createOperationLog(request));
    }
}
