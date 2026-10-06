package com.peakui.matucodesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeCaseResult {

    private Integer caseNo;

    private Integer status;

    private String output;

    private Long executionTime;

    private Long memoryUsed;

    private Integer exitCode;

    private String errorMessage;

    private String stderr;
}
