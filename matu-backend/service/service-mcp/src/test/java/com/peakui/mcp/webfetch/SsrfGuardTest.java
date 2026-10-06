package com.peakui.mcp.webfetch;

import com.peakui.mcp.McpToolException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SsrfGuardTest {

    private final SsrfGuard guard = new SsrfGuard();

    @Test
    void blocksLoopbackAndPrivateAddresses() {
        assertThatThrownBy(() -> guard.validate("http://127.0.0.1/status")).isInstanceOf(McpToolException.class);
        assertThatThrownBy(() -> guard.validate("http://localhost/status")).isInstanceOf(McpToolException.class);
        assertThatThrownBy(() -> guard.validate("http://192.168.1.1:8848/")).isInstanceOf(McpToolException.class);
        assertThatThrownBy(() -> guard.validate("http://10.0.0.5/")).isInstanceOf(McpToolException.class);
    }

    @Test
    void blocksCloudMetadataEndpoint() {
        assertThatThrownBy(() -> guard.validate("http://169.254.169.254/latest/meta-data/"))
                .isInstanceOf(McpToolException.class);
    }

    @Test
    void blocksNonHttpSchemes() {
        assertThatThrownBy(() -> guard.validate("ftp://example.com/file")).isInstanceOf(McpToolException.class);
        assertThatThrownBy(() -> guard.validate("file:///etc/passwd")).isInstanceOf(McpToolException.class);
    }

    @Test
    void blocksCredentialsInUrl() {
        assertThatThrownBy(() -> guard.validate("http://user:pass@example.com/")).isInstanceOf(McpToolException.class);
    }
}
