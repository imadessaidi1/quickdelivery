package com.quickdelivery.abstarct.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class BusinessExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatusCode statusCode = exception.getStatusCode();
        String message = isBlank(exception.getReason())
                ? defaultMessage(statusCode)
                : exception.getReason();
        return ResponseEntity
                .status(statusCode)
                .body(toErrorResponse(statusCode, message, request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        String message = isBlank(exception.getMessage())
                ? "Invalid request"
                : exception.getMessage();
        return ResponseEntity
                .badRequest()
                .body(toErrorResponse(HttpStatus.BAD_REQUEST, message, request));
    }

    private ApiErrorResponse toErrorResponse(HttpStatusCode statusCode, String message, HttpServletRequest request) {
        return new ApiErrorResponse(
                Instant.now(),
                statusCode.value(),
                reasonPhrase(statusCode),
                message,
                resolveCode(message),
                request == null ? null : request.getRequestURI()
        );
    }

    private String defaultMessage(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            return "Request failed";
        }
        if (status.is4xxClientError()) {
            return "Invalid request";
        }
        return "Service unavailable";
    }

    private String reasonPhrase(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status == null ? statusCode.toString() : status.getReasonPhrase();
    }

    private String resolveCode(String message) {
        if (isBlank(message)) {
            return null;
        }
        String trimmed = message.trim();
        return trimmed.matches("[A-Za-z][A-Za-z0-9_.-]*") ? trimmed : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
