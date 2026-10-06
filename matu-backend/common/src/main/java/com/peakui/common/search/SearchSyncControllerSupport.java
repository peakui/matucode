package com.peakui.common.search;

import com.peakui.common.result.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/** Local handlers keep the existing catch-all advice from changing 400/403 to 500. */
public abstract class SearchSyncControllerSupport {
    protected static void requirePage(Long afterId, int pageSize) {
        if (afterId == null || afterId < 0 || pageSize < 1 || pageSize > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "afterId must be >= 0 and pageSize must be between 1 and 100");
        }
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleSearchStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        return ResponseEntity.status(exception.getStatusCode())
                .body(ApiResponse.fail(status, status == 403 ? "Forbidden" : "Invalid pagination parameters"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleSearchParameterType(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(400, "Invalid pagination parameters"));
    }
}
