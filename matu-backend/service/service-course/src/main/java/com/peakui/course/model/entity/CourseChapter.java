package com.peakui.course.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_chapters")
public class CourseChapter {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long courseId;
    private String chapterTitle;
    private String chapterDesc;
    private Integer sortOrder;
    private Integer videoCount;
    private Integer duration;
    private Integer isFreePreview;
    private LocalDateTime createdAt;
}
