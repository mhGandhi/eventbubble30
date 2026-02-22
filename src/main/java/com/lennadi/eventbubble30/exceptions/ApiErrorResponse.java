package com.lennadi.eventbubble30.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lennadi.eventbubble30.security.AuthState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record ApiErrorResponse(
        String message,
        String path,
        Map<String, String> validationErrors,
        AuthState authState,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<String> stacktrace
) {
    public ApiErrorResponse(String msg, String path, AuthState authState) {
        this(msg, path, null, authState, null);
    }
}
