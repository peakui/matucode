package com.peakui.oj.judge;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JudgeCaseResult {
    private Integer caseNo;
    private String input;
    private String expectedOutput;
    private String actualOutput;
    private Integer status;
    private Integer executionTime;
    private Integer memoryUsed;
    private String errorMessage;
}
