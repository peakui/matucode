package com.peakui.info.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("feedback_history")
public class FeedbackHistory {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long feedbackId;
    private Long operatorId;
    private Integer fromStatus;
    private Integer toStatus;
    private Integer priority;
    private Long assigneeId;
    private String replyContent;
    private LocalDateTime createdAt;
}
