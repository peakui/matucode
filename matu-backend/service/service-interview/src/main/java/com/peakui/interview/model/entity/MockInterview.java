package com.peakui.interview.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mock_interviews")
public class MockInterview {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String interviewTitle;
    private String position;
    private String company;
    private String questionIds;
    private Integer totalScore;
    private Integer userScore;
    private Integer durationMinutes;
    private Integer actualDuration;
    private Integer status;
    private String reportJson;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
