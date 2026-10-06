package com.peakui.oj.judge;

import com.peakui.oj.model.entity.OjProblem;
import com.peakui.oj.model.entity.OjSubmission;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JudgeContext {
    private OjProblem problem;
    private OjSubmission submission;
    private List<JudgeCaseDTO> judgeCases;
    private List<String> outputList;
    private List<com.peakui.oj.judge.sandbox.model.ExecuteCaseResult> executeCaseResults;
    private Integer executionTime;
    private Integer memoryUsed;
    private Integer sandboxStatus;
    private String compileMessage;
    private String sandboxError;
}
