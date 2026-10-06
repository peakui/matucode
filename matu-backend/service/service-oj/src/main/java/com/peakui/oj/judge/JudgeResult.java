package com.peakui.oj.judge;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class JudgeResult {
    private Integer status;
    private Integer executionTime;
    private Integer memoryUsed;
    private Integer passedCases;
    private Integer totalCases;
    private BigDecimal passRate;
    private String errorMessage;
    private List<JudgeCaseResult> caseResults;
}
