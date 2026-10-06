package com.peakui.interview.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.RequestContextFilter;

/** Security filters access the request-scoped HttpServletRequest before DispatcherServlet executes. */
@Configuration
public class InterviewRequestContextConfig {
    @Bean RequestContextFilter interviewRequestContextFilter() { return new RequestContextFilter(); }
    @Bean FilterRegistrationBean<RequestContextFilter> interviewRequestContextRegistration(RequestContextFilter interviewRequestContextFilter) {
        var registration = new FilterRegistrationBean<>(interviewRequestContextFilter);
        registration.setName("interviewRequestContext");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        return registration;
    }
}
