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

/** Read-only online-judge tools backed by service-oj. */
@Configuration
@RequiredArgsConstructor
public class OjTools {

    private static final String SERVICE = "service-oj";

    private final UpstreamClient client;
    private final ObjectMapper mapper;

    @Bean
    McpTool ojProblemSearchTool() {
        return new SimpleTool("oj_problem_search",
                "分页检索编程题目，可按关键词、难度筛选，返回题目概要（题号、标题、难度、通过率等）。",
                Schemas.of(mapper)
                        .string("keyword", "题目关键词，可为空", false)
                        .integer("difficulty", "难度：1简单 2中等 3困难", false)
                        .integer("pageNum", "页码，默认1", false)
                        .integer("pageSize", "每页条数，默认10", false)
                        .build(),
                args -> Json.text(mapper, client.getJson(SERVICE, "/oj/classes/problems", Params.of(
                        "keyword", Args.text(args, "keyword"),
                        "difficulty", Args.integer(args, "difficulty"),
                        "pageNum", Args.number(args, "pageNum"),
                        "pageSize", Args.number(args, "pageSize")))));
    }

    @Bean
    McpTool ojProblemDetailTool() {
        return new SimpleTool("oj_problem_detail",
                "按题目ID获取题目详情，包含题面、输入输出格式、样例与难度。",
                Schemas.of(mapper)
                        .string("problemId", "题目ID", true)
                        .build(),
                args -> Json.text(mapper,
                        client.getJson(SERVICE, "/oj/classes/problems/" + Args.requireNumber(args, "problemId"),
                                Params.of())));
    }

    @Bean
    McpTool ojSubmissionListTool() {
        return new SimpleTool("oj_submission_list",
                "分页查询提交记录，可按题目ID、用户ID、判题状态筛选，返回提交概要（状态、语言、耗时等）。",
                Schemas.of(mapper)
                        .string("problemId", "题目ID，可为空", false)
                        .string("userId", "用户ID，可为空", false)
                        .integer("status", "判题状态：可为空", false)
                        .integer("pageNum", "页码，默认1", false)
                        .integer("pageSize", "每页条数，默认20", false)
                        .build(),
                args -> Json.text(mapper, client.getJson(SERVICE, "/oj/classes/submissions", Params.of(
                        "problemId", Args.number(args, "problemId"),
                        "userId", Args.number(args, "userId"),
                        "status", Args.integer(args, "status"),
                        "pageNum", Args.number(args, "pageNum"),
                        "pageSize", Args.number(args, "pageSize")))));
    }

    @Bean
    McpTool ojSubmissionDetailTool() {
        return new SimpleTool("oj_submission_detail",
                "按提交ID获取该次提交的逐测试点判题详情。",
                Schemas.of(mapper)
                        .string("submissionId", "提交ID", true)
                        .build(),
                args -> Json.text(mapper,
                        client.getJson(SERVICE, "/oj/classes/submissions/"
                                + Args.requireNumber(args, "submissionId") + "/details", Params.of())));
    }

}
