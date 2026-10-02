package com.pritam.saasbackend.common.api;

import org.springframework.http.HttpStatus;

/**
 * Stable, client-facing error codes. The default messages are generic on purpose:
 * they never carry exception text, SQL, schema names or stack traces.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request"),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid or expired token"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access is denied"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type"),
    DATA_CONFLICT(HttpStatus.CONFLICT, "The request conflicts with existing data"),
    REQUEST_REJECTED(HttpStatus.BAD_REQUEST, "The request was rejected"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
