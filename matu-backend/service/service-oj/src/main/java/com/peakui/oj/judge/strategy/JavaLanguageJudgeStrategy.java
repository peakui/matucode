package com.peakui.oj.judge.strategy;

import com.peakui.oj.judge.JudgeContext;
import com.peakui.oj.judge.JudgeResult;
import com.peakui.oj.judge.JudgeStrategy;
import org.springframework.stereotype.Component;

@Component
public class JavaLanguageJudgeStrategy implements JudgeStrategy {

    private final DefaultJudgeStrategy defaultJudgeStrategy;

    public JavaLanguageJudgeStrategy(DefaultJudgeStrategy defaultJudgeStrategy) {
        this.defaultJudgeStrategy = defaultJudgeStrategy;
    }

    @Override
    public boolean supports(String language) {
        return "java".equalsIgnoreCase(language);
    }

    @Override
    public JudgeResult judge(JudgeContext context) {
        return defaultJudgeStrategy.judge(context);
    }
}
