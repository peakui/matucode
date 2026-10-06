package com.peakui.matucodesandbox;

import com.peakui.matucodesandbox.model.ExecuteCodeRequest;
import com.peakui.matucodesandbox.model.ExecuteCodeResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Primary
public class MultiLanguageCodeSandbox implements CodeSandbox {

    @Resource
    private JavaDockerCodeSandbox javaDockerCodeSandbox;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return javaDockerCodeSandbox.executeCode(executeCodeRequest);
    }
}
