package com.pritam.saasbackend.security.jwt;

import com.pritam.saasbackend.common.api.ErrorCode;
import com.pritam.saasbackend.common.api.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ErrorResponseWriter errorResponseWriter;

    public JwtAuthenticationEntryPoint(ErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        // JwtAuthenticationFilter reports a bad or expired token as BadCredentialsException;
        // a request without a token arrives here from the ExceptionTranslationFilter.
        ErrorCode code = authException instanceof BadCredentialsException
                ? ErrorCode.INVALID_TOKEN
                : ErrorCode.AUTHENTICATION_REQUIRED;

        errorResponseWriter.write(request, response, code);
    }
}
