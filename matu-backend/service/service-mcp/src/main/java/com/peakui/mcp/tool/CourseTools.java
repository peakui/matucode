package com.peakui.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.mcp.client.Params;
import com.peakui.mcp.client.UpstreamClient;
import com.peakui.mcp.mcp.Args;
import com.peakui.mcp.mcp.Json;
import com.peakui.mcp.mcp.McpTool;
import com.peakui.mcp.mcp.Schemas;
import com.peakui.mcp.mcp.SimpleTool;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Read-only course tools backed by service-course's public catalog API. */
@Configuration
@RequiredArgsConstructor
public class CourseTools {

    private static final String SERVICE = "service-course";

    private final UpstreamClient client;
    private final ObjectMapper mapper;

    @Bean
    McpTool courseSearchTool() {
        return new SimpleTool("course_search",
                "分页检索平台课程，可按关键词、分类、难度筛选，返回课程列表概要（标题、简介、价格等）。",
                Schemas.of(mapper)
                        .string("keyword", "搜索关键词，可为空", false)
                        .integer("categoryId", "课程分类ID", false)
                        .integer("level", "课程难度等级", false)
                        .integer("pageNum", "页码，默认1", false)
                        .integer("pageSize", "每页条数，默认10", false)
                        .build(),
                args -> Json.text(mapper, client.getJson(SERVICE, "/courses", Params.of(
                        "keyword", Args.text(args, "keyword"),
                        "categoryId", Args.number(args, "categoryId"),
                        "level", Args.integer(args, "level"),
                        "pageNum", Args.number(args, "pageNum"),
                        "pageSize", Args.number(args, "pageSize")))));
    }

    @Bean
    McpTool courseDetailTool() {
        return new SimpleTool("course_detail",
                "按课程ID获取课程详情，包含章节列表与每个章节下的视频列表。",
                Schemas.of(mapper)
                        .string("courseId", "课程ID", true)
                        .build(),
                args -> Json.text(mapper,
                        client.getJson(SERVICE, "/courses/" + Args.requireNumber(args, "courseId"), Params.of())));
    }
}
