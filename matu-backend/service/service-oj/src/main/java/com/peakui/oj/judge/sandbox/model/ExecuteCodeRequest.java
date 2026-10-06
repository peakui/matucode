package com.peakui.oj.judge.sandbox.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExecuteCodeRequest {
    private String language;
    private String code;
    private List<String> inputList;
    private Integer timeLimit;
    private Integer memoryLimit;
}
