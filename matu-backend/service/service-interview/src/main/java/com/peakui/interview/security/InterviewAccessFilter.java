package com.peakui.interview.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.common.result.ApiResponse;
import com.peakui.interview.exception.InterviewException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@RequiredArgsConstructor
public class InterviewAccessFilter extends OncePerRequestFilter {
    /** Service-to-service search sync; guarded by X-Search-Sync-Token, not by gateway identity. */
    private static final String SEARCH_SYNC_PATH = "/interview/internal/search-documents";

    private final InterviewSecurityService security;
    private final ObjectMapper mapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        if (SEARCH_SYNC_PATH.equals(path)) return true;
        return !(path.equals("/interview") || path.startsWith("/interview/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        response.setHeader("Cache-Control", "no-store");
        try {
            if (!security.isInternalDashboardRequest()) {
                security.verifyIdentity();
                security.checkQuestionRead("request", 1);
            }
            String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
            if (!security.isInternalDashboardRequest()
                    && (path.startsWith("/interview/internal/") || path.equals("/interview/categories/page"))) {
                security.requireAdmin();
            } else if (!"GET".equals(request.getMethod()) && !"OPTIONS".equals(request.getMethod())) {
                security.requireLogin();
            }
        } catch (InterviewException e) {
            response.setStatus(e.getCode());
            response.setContentType("application/json;charset=UTF-8");
            if (e.getCode() == 429 || e.getCode() == 503)
                response.setHeader("Retry-After", Long.toString(e.getRetryAfter()));
            mapper.writeValue(response.getWriter(), ApiResponse.fail(e.getCode(), e.getMessage()));
            return;
        }
        chain.doFilter(request, response);
    }
}
