package com.pritam.saasbackend.common.api;

import com.pritam.saasbackend.auth.application.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * The single place that turns exceptions raised in Spring MVC into the standard
 * {@link ApiError} body. Extends {@link ResponseEntityExceptionHandler} so every
 * built-in MVC exception (unreadable body, 405, 415, missing parameter, unknown route,
 * validation) keeps its correct status; {@link #handleExceptionInternal} replaces
 * Spring's ProblemDetail body with ours.
 * <p>
 * Client messages are always generic. Exception text, SQL and constraint names are
 * logged server-side only.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException exception, HttpServletRequest request) {
        log.debug("API exception {}: {}", exception.code(), exception.getMessage());
        ErrorCode code = exception.code();
        return respond(ApiError.of(code, code.status().value(), exception.getMessage(), request.getRequestURI()));
    }

    // Temporary: the auth package is reworked in Phase 3, where this becomes an ApiException.
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(HttpServletRequest request) {
        return respond(ApiError.of(ErrorCode.INVALID_CREDENTIALS, request.getRequestURI()));
    }

    // Explicit, so the catch-all below never turns a method-security denial into a 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        log.debug("Access denied on {}: {}", request.getRequestURI(), exception.getMessage());
        return respond(ApiError.of(ErrorCode.ACCESS_DENIED, request.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
        log.debug("Authentication failed on {}: {}", request.getRequestURI(), exception.getMessage());
        return respond(ApiError.of(ErrorCode.AUTHENTICATION_REQUIRED, request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        // The constraint name stays in the log; it can reveal schema details.
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), exception.getMostSpecificCause().getMessage());
        return respond(ApiError.of(ErrorCode.DATA_CONFLICT, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return respond(ApiError.of(ErrorCode.INTERNAL_ERROR, request.getRequestURI()));
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<FieldErrorDetail> fieldErrors = new ArrayList<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.add(new FieldErrorDetail(error.getField(), error.getDefaultMessage())));
        exception.getBindingResult().getGlobalErrors().forEach(error ->
                fieldErrors.add(new FieldErrorDetail(error.getObjectName(), error.getDefaultMessage())));

        return validationFailed(fieldErrors, headers, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<FieldErrorDetail> fieldErrors = new ArrayList<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors errors) {
                errors.getFieldErrors().forEach(error ->
                        fieldErrors.add(new FieldErrorDetail(error.getField(), error.getDefaultMessage())));
            } else {
                String parameter = result.getMethodParameter().getParameterName();
                for (MessageSourceResolvable error : result.getResolvableErrors()) {
                    fieldErrors.add(new FieldErrorDetail(parameter, error.getDefaultMessage()));
                }
            }
        }

        return validationFailed(fieldErrors, headers, request);
    }

    /**
     * Every other Spring MVC exception lands here with its correct status.
     * We keep the status and headers (e.g. {@code Allow} on 405) and swap in our body.
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        if (request instanceof ServletWebRequest servletRequest
                && servletRequest.getResponse() != null
                && servletRequest.getResponse().isCommitted()) {
            return null;
        }

        ErrorCode code = codeFor(exception, statusCode);
        if (statusCode.is5xxServerError()) {
            log.error("MVC error on {}", path(request), exception);
        } else {
            log.debug("Request rejected on {} ({}): {}", path(request), statusCode.value(), exception.getMessage());
        }

        ApiError error = ApiError.of(code, statusCode.value(), code.defaultMessage(), path(request));
        return ResponseEntity.status(statusCode).headers(headers).body(error);
    }

    private static ErrorCode codeFor(Exception exception, HttpStatusCode statusCode) {
        if (exception instanceof NoResourceFoundException) {
            return ErrorCode.NOT_FOUND;
        }
        if (exception instanceof HttpRequestMethodNotSupportedException) {
            return ErrorCode.METHOD_NOT_ALLOWED;
        }
        if (exception instanceof HttpMediaTypeNotSupportedException) {
            return ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        }
        if (statusCode.value() == HttpStatus.BAD_REQUEST.value()) {
            return ErrorCode.MALFORMED_REQUEST;
        }
        if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
            return ErrorCode.NOT_FOUND;
        }
        return statusCode.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.REQUEST_REJECTED;
    }

    private ResponseEntity<Object> validationFailed(
            List<FieldErrorDetail> fieldErrors,
            HttpHeaders headers,
            WebRequest request
    ) {
        ApiError error = ApiError.of(ErrorCode.VALIDATION_FAILED, path(request)).withFieldErrors(fieldErrors);
        return ResponseEntity.status(error.status()).headers(headers).body(error);
    }

    private static ResponseEntity<ApiError> respond(ApiError error) {
        return ResponseEntity.status(error.status()).body(error);
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : "";
    }
}
