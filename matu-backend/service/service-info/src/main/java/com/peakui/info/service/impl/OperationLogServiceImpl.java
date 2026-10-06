package com.peakui.info.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.common.result.PageResponse;
import com.peakui.info.exception.InfoException;
import com.peakui.info.mapper.OperationLogMapper;
import com.peakui.info.model.dto.CreateOperationLogRequest;
import com.peakui.info.model.entity.OperationLog;
import com.peakui.info.model.vo.OperationLogVO;
import com.peakui.info.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResponse<OperationLogVO> listOperationLogs(Long userId, String username, String module, String action,
                                                          String targetType, Long targetId, Integer result,
                                                          LocalDateTime startTime, LocalDateTime endTime,
                                                          Long pageNum, Long pageSize) {
        checkAdmin();
        long currentPage = normalizePageNum(pageNum);
        long currentSize = normalizePageSize(pageSize);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(action)) {
            wrapper.eq(OperationLog::getAction, action);
        }
        if (StringUtils.hasText(targetType)) {
            wrapper.eq(OperationLog::getTargetType, targetType);
        }
        if (targetId != null) {
            wrapper.eq(OperationLog::getTargetId, targetId);
        }
        if (result != null) {
            wrapper.eq(OperationLog::getResult, result);
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getCreatedAt, endTime);
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);

        Page<OperationLog> page = operationLogMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResponse.of(currentPage, currentSize, page.getTotal(), Collections.emptyList());
        }
        return PageResponse.of(currentPage, currentSize, page.getTotal(), page.getRecords().stream().map(this::toVO).toList());
    }

    @Override
    public OperationLogVO getOperationLogDetail(Long id) {
        checkAdmin();
        return toVO(getOperationLog(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OperationLogVO createOperationLog(CreateOperationLogRequest request) {
        checkAdmin();
        validateJson(request.getDetail(), "操作详情");
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(request.getUserId());
        operationLog.setUsername(normalizeText(request.getUsername()));
        operationLog.setModule(request.getModule().trim());
        operationLog.setAction(request.getAction().trim());
        operationLog.setTargetType(normalizeText(request.getTargetType()));
        operationLog.setTargetId(request.getTargetId());
        operationLog.setDetail(normalizeText(request.getDetail()));
        operationLog.setIpAddress(request.getIpAddress().trim());
        operationLog.setUserAgent(normalizeText(request.getUserAgent()));
        operationLog.setResult(request.getResult() == null ? 1 : request.getResult());
        operationLog.setErrorMsg(normalizeText(request.getErrorMsg()));
        operationLog.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(operationLog);
        log.info("创建操作日志成功, logId={}, userId={}, module={}, action={}",
                operationLog.getId(), operationLog.getUserId(), operationLog.getModule(), operationLog.getAction());
        return toVO(operationLog);
    }

    private OperationLog getOperationLog(Long id) {
        OperationLog operationLog = operationLogMapper.selectById(id);
        if (operationLog == null) {
            throw new InfoException("操作日志不存在");
        }
        return operationLog;
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

    private OperationLogVO toVO(OperationLog operationLog) {
        return OperationLogVO.builder()
                .id(operationLog.getId())
                .userId(operationLog.getUserId())
                .username(operationLog.getUsername())
                .module(operationLog.getModule())
                .action(operationLog.getAction())
                .targetType(operationLog.getTargetType())
                .targetId(operationLog.getTargetId())
                .detail(operationLog.getDetail())
                .ipAddress(operationLog.getIpAddress())
                .userAgent(operationLog.getUserAgent())
                .result(operationLog.getResult())
                .errorMsg(operationLog.getErrorMsg())
                .createdAt(operationLog.getCreatedAt())
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
