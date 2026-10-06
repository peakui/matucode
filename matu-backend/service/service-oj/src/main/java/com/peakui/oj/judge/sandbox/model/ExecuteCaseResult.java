package com.peakui.oj.judge.sandbox.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecuteCaseResult {
    private Integer caseNo;
    private Integer status;
    private String output;
    private Integer executionTime;
    private Integer memoryUsed;
    private Integer exitCode;
    private String errorMessage;
    private String stderr;
}
