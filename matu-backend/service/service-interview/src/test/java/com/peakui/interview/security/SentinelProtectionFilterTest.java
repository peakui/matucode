package com.peakui.interview.security;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.node.ClusterNode;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** 使用真实 Sentinel slot chain，不启动 Spring/Nacos，也不 mock SphU。 */
class SentinelProtectionFilterTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final SentinelProtectionFilter filter = new SentinelProtectionFilter(mapper);

    @BeforeEach
    @AfterEach
    void resetRules() {
        FlowRuleManager.loadRules(List.of());
        DegradeRuleManager.loadRules(List.of());
        ClusterNode node = ClusterBuilderSlot.getClusterNode(SentinelProtection.READ_RESOURCE);
        if (node != null) {
            node.reset();
        }
        ContextUtil.exit();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/interview", "/interview/questions", "/api/interview/questions",
            "/api/interview;v=1/questions", "/api/%69nterview/questions"})
    void protectedPathsCannotBypassWithContextOrPathParameters(String uri) throws Exception {
        FlowRule rule = new FlowRule(SentinelProtection.READ_RESOURCE).setCount(0);
        FlowRuleManager.loadRules(List.of(rule));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        if (uri.startsWith("/api/")) {
            request.setContextPath("/api");
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("blocked request reached controller"));
        assertBlocked(response, 429, "1");
    }

    @Test
    void unrelatedPathsAreNotProtected() throws Exception {
        FlowRuleManager.loadRules(List.of(new FlowRule(SentinelProtection.READ_RESOURCE).setCount(0)));
        var response = request("/interviews/questions", (req, res) -> ((HttpServletResponse) res).setStatus(204));
        assertEquals(204, response.getStatus());
    }

    @Test
    void realQpsFlowBlocksAfterOneRequest() throws Exception {
        FlowRuleManager.loadRules(List.of(new FlowRule(SentinelProtection.READ_RESOURCE).setCount(1)));
        assertEquals(200, request("/interview/questions", (req, res) -> {}).getStatus());
        assertBlocked(request("/interview/questions", (req, res) -> fail("over QPS limit")), 429, "1");
    }

    @Test
    void completeSlowRequestsOpenCircuitAndReturn503() throws Exception {
        DegradeRule rule = breaker(RuleConstant.DEGRADE_GRADE_RT, 10);
        rule.setSlowRatioThreshold(0.5);
        DegradeRuleManager.loadRules(List.of(rule));
        for (int i = 0; i < 2; i++) {
            assertEquals(200, request("/interview/questions", (req, res) -> {
                assertNotNull(ContextUtil.getContext().getCurEntry());
                try {
                    Thread.sleep(40);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new ServletException(ex);
                }
            }).getStatus());
        }
        assertBlocked(request("/interview/questions", (req, res) -> fail("slow-call circuit open")), 503, "10");
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503})
    void handledServerErrorResponsesOpenExceptionCircuit(int status) throws Exception {
        DegradeRuleManager.loadRules(List.of(breaker(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO, 0.5)));
        for (int i = 0; i < 2; i++) {
            assertEquals(status, request("/interview/questions", (req, res) ->
                    ((HttpServletResponse) res).setStatus(status)).getStatus());
        }
        assertBlocked(request("/interview/questions", (req, res) -> fail("exception circuit open")), 503, "10");
    }

    @Test
    void clientErrorsDoNotOpenExceptionCircuit() throws Exception {
        DegradeRuleManager.loadRules(List.of(breaker(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO, 0.5)));
        for (int i = 0; i < 4; i++) {
            assertEquals(403, request("/interview/questions", (req, res) ->
                    ((HttpServletResponse) res).setStatus(403)).getStatus());
        }
        assertEquals(200, request("/interview/questions", (req, res) -> {}).getStatus());
    }

    @Test
    void escapedExceptionsAreRethrownTracedAndExitEntry() throws Exception {
        DegradeRuleManager.loadRules(List.of(breaker(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO, 0.5)));
        AtomicReference<Entry> captured = new AtomicReference<>();
        IOException failure = new IOException("controller failure");
        for (int i = 0; i < 2; i++) {
            assertSame(failure, assertThrows(IOException.class, () -> request("/interview/questions", (req, res) -> {
                captured.set(ContextUtil.getContext().getCurEntry());
                ((HttpServletResponse) res).setStatus(500);
                throw failure;
            })));
            assertSame(failure, captured.get().getError());
            assertTrue(ContextUtil.getContext() == null || ContextUtil.getContext().getCurEntry() == null);
        }
        assertBlocked(request("/interview/questions", (req, res) -> fail("exception circuit open")), 503, "10");
    }

    private DegradeRule breaker(int grade, double count) {
        DegradeRule rule = new DegradeRule(SentinelProtection.READ_RESOURCE);
        rule.setGrade(grade);
        rule.setCount(count);
        rule.setTimeWindow(10);
        rule.setMinRequestAmount(2);
        rule.setStatIntervalMs(10000);
        return rule;
    }

    private MockHttpServletResponse request(String uri, FilterChain chain) throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", uri), response, chain);
        return response;
    }

    private void assertBlocked(MockHttpServletResponse response, int status, String retry) throws Exception {
        assertEquals(status, response.getStatus());
        assertEquals(retry, response.getHeader("Retry-After"));
        assertTrue(response.getContentType().startsWith("application/json"));
        var body = mapper.readTree(response.getContentAsString());
        assertEquals(status, body.path("code").asInt());
        assertFalse(body.path("message").asText().isBlank());
        assertTrue(body.path("data").isNull());
    }
}
