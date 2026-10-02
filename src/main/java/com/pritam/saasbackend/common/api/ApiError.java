package com.pritam.saasbackend.common.api;

import java.time.Instant;
import java.util.List;

/**
 * The standard error body for every failure (spec 12):
 * {@code { timestamp, status, code, message, path, fieldErrors[] }}.
 * {@code fieldErrors} is always present, empty when there are none.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors
) {

    public ApiError {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static ApiError of(ErrorCode code, int status, String message, String path) {
        return new ApiError(Instant.now(), status, code.name(), message, path, List.of());
    }

    public static ApiError of(ErrorCode code, String path) {
        return of(code, code.status().value(), code.defaultMessage(), path);
    }

    public ApiError withFieldErrors(List<FieldErrorDetail> errors) {
        return new ApiError(timestamp, status, code, message, path, errors);
    }
}
