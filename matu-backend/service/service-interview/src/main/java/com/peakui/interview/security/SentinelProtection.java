package com.peakui.interview.security;

import org.springframework.stereotype.Service;

/** Sentinel 资源定义。HTTP 请求的完整统计由 {@link SentinelProtectionFilter} 执行。 */
@Service
public class SentinelProtection {
    public static final String READ_RESOURCE = "interview:question-read";

    /**
     * 兼容旧调用点，不再单独创建瞬时 entry，避免重复计数和错误的耗时统计。
     * @deprecated HTTP 保护已迁移至过滤器，调用方应删除此调用。
     */
    @Deprecated
    public void checkRead() {
        // 保留二进制/源码兼容；不得在这里再创建 Sentinel entry。
    }
}
