package com.quickdelivery.abstarct.web;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String code,
        String path
) {
}
