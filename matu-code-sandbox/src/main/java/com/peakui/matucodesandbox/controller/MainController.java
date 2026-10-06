package com.peakui.matucodesandbox.controller;

import com.peakui.matucodesandbox.CodeSandbox;
import com.peakui.matucodesandbox.model.ApiResponse;
import com.peakui.matucodesandbox.model.ExecuteCodeRequest;
import com.peakui.matucodesandbox.model.ExecuteCodeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/judge")
public class MainController {

    private static final String AUTH_REQUEST_HEADER = "auth";

    @Value("${sandbox.auth-secret:}")
    private String authRequestSecret;

    @Resource
    private CodeSandbox codeSandbox;

    @GetMapping("/health")
    public String healthCheck() {
        return "ok";
    }

    @PostMapping("/execute")
    public ApiResponse<ExecuteCodeResponse> execute(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (authRequestSecret == null || authRequestSecret.isEmpty()
                || !authRequestSecret.equals(authHeader)) {
            response.setStatus(403);
            return ApiResponse.error(403, "forbidden");
        }
        if (executeCodeRequest == null) {
            throw new IllegalArgumentException("请求参数为空");
        }
        return ApiResponse.success(codeSandbox.executeCode(executeCodeRequest));
    }
}
