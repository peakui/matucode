package com.peakui.oj.judge.sandbox;

import com.peakui.common.result.ApiResponse;
import com.peakui.oj.exception.OjException;
import com.peakui.oj.judge.JudgeStatusEnum;
import com.peakui.oj.judge.sandbox.model.CodeSandboxProperties;
import com.peakui.oj.judge.sandbox.model.ExecuteCaseResult;
import com.peakui.oj.judge.sandbox.model.ExecuteCodeRequest;
import com.peakui.oj.judge.sandbox.model.ExecuteCodeResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "oj.sandbox", name = "type", havingValue = "remote")
public class RemoteCodeSandbox implements CodeSandbox {

    private final RemoteCodeSandboxClient remoteCodeSandboxClient;
    private final CodeSandboxProperties properties;

    public RemoteCodeSandbox(RemoteCodeSandboxClient remoteCodeSandboxClient,
                             CodeSandboxProperties properties) {
        this.remoteCodeSandboxClient = remoteCodeSandboxClient;
        this.properties = properties;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new OjException("未配置远程判题服务地址 oj.sandbox.base-url");
        }
        try {
            ApiResponse<ExecuteCodeResponse> response = remoteCodeSandboxClient.executeCode(request);
            if (response == null) {
                return systemError("远程判题服务无响应");
            }
            if (response.getCode() != null && response.getCode() != 0) {
                return systemError(response.getMessage());
            }
            if (response.getData() == null) {
                return systemError("远程判题服务返回数据为空");
            }
            return response.getData();
        } catch (Exception ex) {
            return systemError("远程判题调用失败: " + ex.getMessage());
        }
    }

    private ExecuteCodeResponse systemError(String message) {
        List<ExecuteCaseResult> caseResults = new ArrayList<>();
        return ExecuteCodeResponse.builder()
                .outputList(List.of())
                .caseResults(caseResults)
                .status(JudgeStatusEnum.SYSTEM_ERROR.getCode())
                .executionTime(0)
                .memoryUsed(0)
                .errorMessage(message)
                .compileMessage(null)
                .build();
    }
}
