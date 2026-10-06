package com.peakui.info.service;

import com.peakui.common.result.PageResponse;
import com.peakui.info.model.dto.CreateOperationLogRequest;
import com.peakui.info.model.vo.OperationLogVO;

import java.time.LocalDateTime;

public interface OperationLogService {

    PageResponse<OperationLogVO> listOperationLogs(Long userId, String username, String module, String action,
                                                   String targetType, Long targetId, Integer result,
                                                   LocalDateTime startTime, LocalDateTime endTime,
                                                   Long pageNum, Long pageSize);

    OperationLogVO getOperationLogDetail(Long id);

    OperationLogVO createOperationLog(CreateOperationLogRequest request);
}
