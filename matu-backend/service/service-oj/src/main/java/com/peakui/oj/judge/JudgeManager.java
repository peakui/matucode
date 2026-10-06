package com.peakui.oj.judge;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JudgeManager {

    private final List<JudgeStrategy> judgeStrategies;

    public JudgeManager(List<JudgeStrategy> judgeStrategies) {
        this.judgeStrategies = judgeStrategies;
    }

    public JudgeResult doJudge(String language, JudgeContext context) {
        return judgeStrategies.stream()
                .filter(strategy -> strategy.supports(language))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的判题语言: " + language))
                .judge(context);
    }
}
