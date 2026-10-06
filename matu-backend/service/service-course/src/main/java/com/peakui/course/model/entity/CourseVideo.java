package com.peakui.course.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_videos")
public class CourseVideo {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long chapterId;
    private String videoTitle;
    private String videoDesc;
    private String videoUrl;
    private String coverUrl;
    private Integer duration;
    private Long fileSize;
    private String resolution;
    private Integer sortOrder;
    private Integer playCount;
    private Integer isFreePreview;
    private Integer status;
    private LocalDateTime createdAt;
}
