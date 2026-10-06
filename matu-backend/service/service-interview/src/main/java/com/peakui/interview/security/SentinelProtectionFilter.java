package com.peakui.interview.security;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.peakui.common.result.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 为当前同步 MVC 接口包裹完整请求，包含控制器、异常处理器及响应序列化的耗时。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class SentinelProtectionFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;

    public SentinelProtectionFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 使用去除 context-path、解码并去除分号参数的应用路径，不能直接判断 requestURI 前缀。
        String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        return !("/interview".equals(path) || path.startsWith("/interview/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        final Entry entry;
        try {
            entry = SphU.entry(SentinelProtection.READ_RESOURCE, EntryType.IN);
        } catch (BlockException blocked) {
            boolean degraded = blocked instanceof DegradeException;
            int status = degraded ? HttpServletResponse.SC_SERVICE_UNAVAILABLE : 429;
            int retryAfter = degraded && blocked.getRule() instanceof com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule rule
                    ? Math.max(1, rule.getTimeWindow()) : 1;
            response.setStatus(status);
            response.setHeader(HttpHeaders.RETRY_AFTER, Integer.toString(retryAfter));
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiResponse.fail(status,
                    degraded ? "面试题服务暂时不可用，请稍后重试" : "面试题请求过于频繁，请稍后重试"));
            return;
        }

        boolean exceptionTraced = false;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException | Error failure) {
            exceptionTraced = true;
            Tracer.traceEntry(failure, entry);
            throw failure;
        } finally {
            try {
                // MVC advice 等可能将异常转换成响应；此时仍需统计为失败，且每个请求只计一次。
                if (!exceptionTraced && response.getStatus() >= 500) {
                    Tracer.traceEntry(new ServerErrorResponse(response.getStatus()), entry);
                }
            } finally {
                entry.exit();
            }
        }
    }

    private static final class ServerErrorResponse extends RuntimeException {
        private ServerErrorResponse(int status) {
            super("HTTP response status " + status, null, false, false);
        }
    }
}
