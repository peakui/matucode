package com.peakui.course.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.search.SearchSourceItem;
import com.peakui.common.search.SearchSyncAccess;
import com.peakui.common.search.SearchSyncControllerSupport;
import com.peakui.course.mapper.CourseMapper;
import com.peakui.course.model.entity.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CourseSearchSyncController extends SearchSyncControllerSupport {
    private final CourseMapper courseMapper;

    @Value("${search.sync.token:}")
    private String syncToken;

    @GetMapping("/courses/internal/search-documents")
    public ApiResponse<List<SearchSourceItem>> searchDocuments(
            @RequestHeader(value = "X-Search-Sync-Token", required = false) String token,
            @RequestParam(value = "afterId", defaultValue = "0") Long afterId,
            @RequestParam(value = "pageSize", defaultValue = "100") int pageSize) {
        SearchSyncAccess.requireToken(syncToken, token);
        requirePage(afterId, pageSize);
        List<Course> courses = courseMapper.selectList(new QueryWrapper<Course>()
                .select("id", "title", "subtitle", "cover_url", "level", "status", "published_at", "created_at")
                .gt("id", afterId).eq("status", 1).orderByAsc("id").last("LIMIT " + pageSize));
        return ApiResponse.success(courses.stream().map(course -> SearchSourceItem.builder()
                .id(course.getId().toString()).title(course.getTitle()).summary(course.getSubtitle())
                .coverUrl(course.getCoverUrl()).difficulty(course.getLevel()).status(course.getStatus())
                .tags(List.of()).publishedAt(course.getPublishedAt()).createdAt(course.getCreatedAt())
                .build()).toList());
    }
}
