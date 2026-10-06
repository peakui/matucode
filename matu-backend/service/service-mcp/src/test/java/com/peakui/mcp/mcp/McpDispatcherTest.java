package com.peakui.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class McpDispatcherTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private McpDispatcher dispatcherWith(McpTool... tools) {
        return new McpDispatcher(new ToolRegistry(List.of(tools)), mapper);
    }

    private McpTool echoTool() {
        return new SimpleTool("echo", "回显", mapper.createObjectNode().put("type", "object"), args -> "ok");
    }

    private JsonNode request(String json) throws Exception {
        return mapper.readTree(json);
    }

    @Test
    void listsToolsWithNameDescriptionAndSchema() throws Exception {
        JsonNode response = dispatcherWith(echoTool()).dispatch(
                request("{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/list\",\"params\":{}}"));

        JsonNode tool = response.path("result").path("tools").get(0);
        assertThat(response.path("id").asInt()).isEqualTo(7);
        assertThat(tool.path("name").asText()).isEqualTo("echo");
        assertThat(tool.path("description").asText()).isEqualTo("回显");
        assertThat(tool.path("inputSchema").path("type").asText()).isEqualTo("object");
    }

    @Test
    void callsToolAndWrapsTextResult() throws Exception {
        JsonNode response = dispatcherWith(echoTool()).dispatch(
                request("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"echo\",\"arguments\":{}}}"));

        JsonNode result = response.path("result");
        assertThat(result.path("content").get(0).path("type").asText()).isEqualTo("text");
        assertThat(result.path("content").get(0).path("text").asText()).isEqualTo("ok");
        assertThat(result.path("isError").asBoolean()).isFalse();
    }

    @Test
    void unknownToolBecomesToolLevelError() throws Exception {
        JsonNode response = dispatcherWith(echoTool()).dispatch(
                request("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"nope\",\"arguments\":{}}}"));

        assertThat(response.path("result").path("isError").asBoolean()).isTrue();
        assertThat(response.path("result").path("content").get(0).path("text").asText()).contains("未知工具");
    }

    @Test
    void unknownMethodReturnsJsonRpcError() throws Exception {
        JsonNode response = dispatcherWith(echoTool()).dispatch(
                request("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"bogus/method\",\"params\":{}}"));

        assertThat(response.path("error").path("code").asInt()).isEqualTo(-32601);
    }

    @Test
    void notificationProducesNoResponse() throws Exception {
        assertThat(dispatcherWith(echoTool()).dispatch(
                request("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"))).isNull();
    }
}
