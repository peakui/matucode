package com.peakui.oj.judge;

public interface JudgeStrategy {
    boolean supports(String language);
    JudgeResult judge(JudgeContext context);
}
