package com.peakui.matucodesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeResponse {

    private List<String> outputList;

    private List<JudgeCaseResult> caseResults;

    private Integer status;

    private Long executionTime;

    private Long memoryUsed;

    private String errorMessage;

    private String compileMessage;
}
