package com.peakui.oj.judge.sandbox;

import com.peakui.oj.judge.sandbox.model.ExecuteCodeRequest;
import com.peakui.oj.judge.sandbox.model.ExecuteCodeResponse;

public interface CodeSandbox {
    ExecuteCodeResponse executeCode(ExecuteCodeRequest request);
}
