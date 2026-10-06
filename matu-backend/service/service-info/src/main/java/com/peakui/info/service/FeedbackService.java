package com.peakui.info.service;

import com.peakui.common.result.PageResponse;
import com.peakui.info.model.dto.CreateFeedbackRequest;
import com.peakui.info.model.dto.UpdateFeedbackRequest;
import com.peakui.info.model.vo.FeedbackDetailVO;
import com.peakui.info.model.vo.FeedbackListItemVO;

public interface FeedbackService {

    FeedbackDetailVO createFeedback(CreateFeedbackRequest request);

    PageResponse<FeedbackListItemVO> listMyFeedbacks(Long pageNum, Long pageSize);

    FeedbackDetailVO getFeedbackDetail(Long id);

    PageResponse<FeedbackListItemVO> listAdminFeedbacks(Integer type, Integer status, Integer priority,
                                                         Long assigneeId, Long userId, String keyword,
                                                         Long pageNum, Long pageSize);

    FeedbackDetailVO getAdminFeedbackDetail(Long id);

    FeedbackDetailVO updateFeedback(Long id, UpdateFeedbackRequest request);
}
