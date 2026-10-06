package com.peakui.common.search;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchSyncAccessTest {
    @Test
    void acceptsOnlyExactConfiguredToken() {
        assertDoesNotThrow(() -> SearchSyncAccess.requireToken("sync-secret", "sync-secret"));
        for (String expected : new String[] {null, "", " ", "sync-secret"}) {
            for (String actual : new String[] {null, "", "wrong", "sync-secret "}) {
                ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                        () -> SearchSyncAccess.requireToken(expected, actual));
                assertEquals(403, exception.getStatusCode().value());
            }
        }
    }
}
