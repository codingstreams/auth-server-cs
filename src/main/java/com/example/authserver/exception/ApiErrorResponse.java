package com.example.authserver.exception;

import java.time.Instant;

public record ApiErrorResponse(
    String errorCode,
    String message,
    int status,
    Instant timestamp
) {
}
