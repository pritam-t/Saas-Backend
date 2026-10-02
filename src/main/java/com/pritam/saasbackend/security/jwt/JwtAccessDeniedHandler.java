package com.pritam.saasbackend.security.jwt;

import com.pritam.saasbackend.common.api.ErrorCode;
import com.pritam.saasbackend.common.api.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Standard error body for authorization failures raised by the filter chain
 * (URL rules). Denials raised inside controllers go through GlobalExceptionHandler.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorResponseWriter errorResponseWriter;

    public JwtAccessDeniedHandler(ErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        errorResponseWriter.write(request, response, ErrorCode.ACCESS_DENIED);
    }
}
