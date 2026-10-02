package com.pritam.saasbackend.common.api;

/**
 * One invalid field. The rejected value is deliberately not echoed back,
 * because it may be a password or other sensitive input.
 */
public record FieldErrorDetail(
        String field,
        String message
) {
}
