package com.pritam.saasbackend.common.api;

/**
 * The one exception type for expected failures. The message is sent to the client,
 * so it must never contain internal details (SQL, schema names, stack traces).
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code) {
        this(code, code.defaultMessage());
    }

    public ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
