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
public class ExecuteCodeRequest {

    private String language;

    private String code;

    private List<String> inputList;

    private Integer timeLimit;

    private Integer memoryLimit;
}
