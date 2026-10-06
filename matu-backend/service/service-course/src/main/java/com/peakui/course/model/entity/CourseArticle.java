package com.peakui.course.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_articles")
public class CourseArticle {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long courseId;
    private Long chapterId;
    private String title;
    private String content;
    private Integer wordCount;
    private Integer readTime;
    private Integer viewCount;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
