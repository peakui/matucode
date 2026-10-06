package com.peakui.interview.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.junit.jupiter.api.Assertions.*;

class InterviewRequestContextConfigTest {
    @Test void earlyFiltersCanResolveScopedRequestAndContextIsCleared() throws Exception {
        var config=new InterviewRequestContextConfig();
        var filter=config.interviewRequestContextFilter();
        var req=new MockHttpServletRequest("GET","/interview/categories");
        filter.doFilter(req,new MockHttpServletResponse(),(request,response)-> {
            assertSame(req,((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest());
        });
        assertNull(RequestContextHolder.getRequestAttributes());
    }
}
