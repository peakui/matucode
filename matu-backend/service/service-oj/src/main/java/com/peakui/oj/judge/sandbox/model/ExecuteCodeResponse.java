package com.peakui.oj.judge.sandbox.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExecuteCodeResponse {
    private List<String> outputList;
    private List<ExecuteCaseResult> caseResults;
    private Integer status;
    private Integer executionTime;
    private Integer memoryUsed;
    private String errorMessage;
    private String compileMessage;
}
