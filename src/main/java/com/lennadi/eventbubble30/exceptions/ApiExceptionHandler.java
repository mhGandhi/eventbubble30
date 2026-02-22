package com.lennadi.eventbubble30.exceptions;

import com.lennadi.eventbubble30.security.AuthState;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.lennadi.eventbubble30.features.controller")
public class ApiExceptionHandler {

    // --- Helper --------------------------------------------------------------

    private AuthState getAuthState(HttpServletRequest request) {
        Object attr = request.getAttribute("jwt_state");
        return (attr instanceof AuthState) ? (AuthState) attr : AuthState.UNKNOWN;
    }

    // --- Validation Errors ----------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                errors.put(err.getField(), err.getDefaultMessage())
        );

        ApiErrorResponse response = new ApiErrorResponse(
                "Validation failed",
                request.getRequestURI(),
                errors,
                getAuthState(request),
                null
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleMissingBody(HttpMessageNotReadableException ex, HttpServletRequest request) {

        ApiErrorResponse response = new ApiErrorResponse(
                "HttpMessage nicht lesbar (fehlender Body?)",
                request.getRequestURI(),
                getAuthState(request)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // --- ResponseStatusException --------------------------------------------

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatusException(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                ex.getReason(),
                request.getRequestURI(),
                getAuthState(request)
        );

        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    // --- Wrong Method --------------------------------------------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                ex.getMessage(),
                request.getRequestURI(),
                getAuthState(request)
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    // --- 404 Not Found -------------------------------------------------------

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                ex.getMessage(),
                request.getRequestURI(),
                getAuthState(request)
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAnyException(Exception e, HttpServletRequest request) {
        boolean admin = isAdmin();

        ApiErrorResponse body = new ApiErrorResponse(
                admin ? e.getMessage() : HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                request.getRequestURI(),
                null,
                getAuthState(request),
                Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).toList()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) return false;

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ADMIN"));
    }
}
