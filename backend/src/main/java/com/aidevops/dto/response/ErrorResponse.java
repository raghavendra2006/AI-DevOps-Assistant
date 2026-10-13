package com.aidevops.dto.response;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
    int status,
    String error,
    String message,
    String path,
    Map<String, String> validationErrors,
    Instant timestamp
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, path, null, Instant.now());
    }

    public ErrorResponse(int status, String error, String message, String path, Map<String, String> validationErrors) {
        this(status, error, message, path, validationErrors, Instant.now());
    }
}
