package com.peakui.oj.judge.sandbox;

import com.peakui.common.result.ApiResponse;
import com.peakui.oj.judge.sandbox.model.ExecuteCodeRequest;
import com.peakui.oj.judge.sandbox.model.ExecuteCodeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "remoteCodeSandboxClient", url = "${oj.sandbox.base-url:}")
public interface RemoteCodeSandboxClient {

    @PostMapping("${oj.sandbox.execute-path:/api/judge/execute}")
    ApiResponse<ExecuteCodeResponse> executeCode(@RequestBody ExecuteCodeRequest request);
}
