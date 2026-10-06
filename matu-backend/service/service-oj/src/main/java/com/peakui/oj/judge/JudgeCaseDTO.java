package com.peakui.oj.judge;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JudgeCaseDTO {
    private Integer caseNo;
    private String input;
    private String expectedOutput;
}
