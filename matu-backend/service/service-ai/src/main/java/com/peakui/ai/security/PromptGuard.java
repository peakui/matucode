package com.peakui.ai.security;

import com.peakui.ai.AiProperties;
import com.peakui.ai.exception.AiBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class PromptGuard {
    private final AiProperties properties;

    private static final List<String> INJECTION_MARKERS = List.of(
            "ignore previous instructions", "ignore all previous", "忽略之前的指令", "忽略系统提示",
            "system prompt", "系统提示词", "developer message", "泄露密钥", "输出你的提示词",
            "jailbreak", "越狱"
    );

    public String checkAndNormalize(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new AiBusinessException(400, "消息不能为空");
        }
        String normalized = prompt.strip();
        if (normalized.length() > properties.getSecurity().getMaxPromptChars()) {
            throw new AiBusinessException(400, "消息长度超过限制");
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (String marker : INJECTION_MARKERS) {
            if (lower.contains(marker.toLowerCase(Locale.ROOT))) {
                throw new AiBusinessException(400, "消息包含不允许的提示注入内容");
            }
        }
        return normalized;
    }
}
