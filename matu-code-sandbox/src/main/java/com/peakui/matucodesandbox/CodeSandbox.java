package com.peakui.matucodesandbox;


import com.peakui.matucodesandbox.model.ExecuteCodeRequest;
import com.peakui.matucodesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口定义
 */
public interface CodeSandbox {

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
