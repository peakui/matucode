package com.peakui.info.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.info.feign.AuthUserFeignClient;
import com.peakui.info.mapper.*;
import com.peakui.info.model.dto.UpdateFeedbackRequest;
import com.peakui.info.model.entity.*;
import com.peakui.info.service.impl.FeedbackServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class FeedbackHistoryTest {
    private final UserFeedbackMapper feedbacks = mock(UserFeedbackMapper.class);
    private final FeedbackHistoryMapper history = mock(FeedbackHistoryMapper.class);
    private final FeedbackServiceImpl service = new FeedbackServiceImpl(feedbacks, history,
            mock(AuthUserFeignClient.class), new ObjectMapper(), mock(HttpServletRequest.class));

    private UserFeedback feedback(int status) {
        UserFeedback value = new UserFeedback();
        value.setId(10L); value.setUserId(1L); value.setStatus(status); value.setPriority(1);
        value.setReplyContent("旧回复"); value.setRepliedAt(LocalDateTime.now().minusDays(1));
        when(feedbacks.lockById(10L)).thenReturn(value);
        return value;
    }

    @Test void revisedReplyUpdatesTimestampAndAppendsHistory() {
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            UserFeedback value = feedback(0); LocalDateTime oldTime = value.getRepliedAt();
            UpdateFeedbackRequest request = new UpdateFeedbackRequest(); request.setReplyContent("新的回复"); request.setStatus(2);
            service.updateFeedback(10L, request);
            assertTrue(value.getRepliedAt().isAfter(oldTime)); assertNotNull(value.getResolvedAt());
            verify(history).insert(argThat((FeedbackHistory item) -> item.getFromStatus() == 0
                    && item.getToStatus() == 2 && item.getOperatorId() == 2L && "新的回复".equals(item.getReplyContent())));
        }
    }

    @Test void reopeningClearsResolutionTime() {
        try (var auth = mockStatic(StpUtil.class)) {
            UserFeedback value = feedback(2); value.setResolvedAt(LocalDateTime.now());
            UpdateFeedbackRequest request = new UpdateFeedbackRequest(); request.setStatus(1);
            service.updateFeedback(10L, request); assertNull(value.getResolvedAt());
        }
    }

    @Test void noOpDoesNotWriteHistory() {
        try (var auth = mockStatic(StpUtil.class)) {
            feedback(1); UpdateFeedbackRequest request = new UpdateFeedbackRequest(); request.setStatus(1); request.setReplyContent("旧回复");
            service.updateFeedback(10L, request);
            verify(history, never()).insert(any(FeedbackHistory.class));
            verify(feedbacks, never()).updateById(any(UserFeedback.class));
        }
    }

    @Test void nonAdminCannotMutateFeedback() {
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(() -> StpUtil.checkRole("ADMIN")).thenThrow(new RuntimeException("denied"));
            assertThrows(RuntimeException.class, () -> service.updateFeedback(10L, new UpdateFeedbackRequest()));
            verifyNoInteractions(feedbacks, history);
        }
    }
}
