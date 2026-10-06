package com.peakui.course.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_reviews")
public class CourseReview {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long courseId;
    private Integer rating;
    private String content;
    private Integer likeCount;
    private Integer isVerifiedPurchase;
    private Integer status;
    private LocalDateTime createdAt;
}
