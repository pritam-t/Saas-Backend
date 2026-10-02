package com.pritam.saasbackend.common.api;

import com.pritam.saasbackend.security.jwt.JwtAccessDeniedHandler;
import com.pritam.saasbackend.security.jwt.JwtService;
import com.pritam.saasbackend.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static com.pritam.saasbackend.support.web.ErrorScenarioController.LEAKY_CONSTRAINT_NAME;
import static com.pritam.saasbackend.support.web.ErrorScenarioController.LEAKY_INTERNAL_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every failure, whether raised in a controller or in the security filter chain,
 * must produce the standard body: { timestamp, status, code, message, path, fieldErrors[] }.
 * Requests carry real JWTs through the real filter chain.
 */
@IntegrationTest
class ErrorModelIntegrationTest {

    private static final String ISO_INSTANT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtAccessDeniedHandler accessDeniedHandler;

    @Autowired
    private JsonMapper jsonMapper;

    private String bearer;

    @BeforeEach
    void mintToken() {
        bearer = "Bearer " + jwtService.generateAccessToken(UUID.randomUUID(), "PLATFORM_USER");
    }

    @Test
    void authenticatedRequestSucceeds() throws Exception {
        mockMvc.perform(authenticated(get("/test/errors/ok")))
                .andExpect(status().isOk());
    }

    @Test
    void invalidBodyReturnsValidationFailedWithFieldErrors() throws Exception {
        ResultActions result = mockMvc.perform(authenticated(post("/test/errors/validated"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": " ", "code": "too-long"}
                        """));

        assertErrorBody(result, 400, "VALIDATION_FAILED", "/test/errors/validated")
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("name", "code")))
                .andExpect(content().string(not(containsString("too-long"))));
    }

    @Test
    void invalidRequestParameterReturnsValidationFailedWithFieldErrors() throws Exception {
        ResultActions result = mockMvc.perform(authenticated(get("/test/errors/param-validated"))
                .param("size", "0"));

        assertErrorBody(result, 400, "VALIDATION_FAILED", "/test/errors/param-validated")
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }

    @Test
    void unreadableJsonReturnsMalformedRequest() throws Exception {
        ResultActions result = mockMvc.perform(authenticated(post("/test/errors/validated"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{bad"));

        assertErrorBody(result, 400, "MALFORMED_REQUEST", "/test/errors/validated")
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void parameterTypeMismatchReturnsMalformedRequest() throws Exception {
        ResultActions result = mockMvc.perform(authenticated(get("/test/errors/typed"))
                .param("count", "abc"));

        assertErrorBody(result, 400, "MALFORMED_REQUEST", "/test/errors/typed");
    }

    @Test
    void missingParameterReturnsMalformedRequest() throws Exception {
        assertErrorBody(mockMvc.perform(authenticated(get("/test/errors/typed"))),
                400, "MALFORMED_REQUEST", "/test/errors/typed");
    }

    @Test
    void missingTokenReturnsAuthenticationRequired() throws Exception {
        assertErrorBody(mockMvc.perform(get("/test/errors/ok")),
                401, "AUTHENTICATION_REQUIRED", "/test/errors/ok");
    }

    @Test
    void tamperedTokenReturnsInvalidToken() throws Exception {
        String[] parts = bearer.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "tampered." + parts[2];

        assertErrorBody(mockMvc.perform(get("/test/errors/ok").header("Authorization", tampered)),
                401, "INVALID_TOKEN", "/test/errors/ok");
    }

    @Test
    void wrongCredentialsReturnInvalidCredentials() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "nobody-%s@example.com", "password": "wrong-password"}
                        """.formatted(UUID.randomUUID())));

        assertErrorBody(result, 401, "INVALID_CREDENTIALS", "/api/v1/auth/login")
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void accessDeniedInControllerReturnsAccessDenied() throws Exception {
        assertErrorBody(mockMvc.perform(authenticated(get("/test/errors/denied"))),
                403, "ACCESS_DENIED", "/test/errors/denied")
                .andExpect(content().string(not(containsString("internal reason"))));
    }

    @Test
    void accessDeniedInFilterChainReturnsStandardBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/platform/tenants");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("internal reason"));

        JsonNode body = jsonMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(body.propertyNames()).containsExactlyInAnyOrder(
                "timestamp", "status", "code", "message", "path", "fieldErrors");
        assertThat(body.get("code").asString()).isEqualTo("ACCESS_DENIED");
        assertThat(body.get("path").asString()).isEqualTo("/platform/tenants");
        assertThat(body.get("timestamp").asString()).matches(ISO_INSTANT);
        assertThat(response.getContentAsString()).doesNotContain("internal reason");
    }

    @Test
    void apiExceptionKeepsItsCodeAndMessage() throws Exception {
        assertErrorBody(mockMvc.perform(authenticated(get("/test/errors/not-found"))),
                404, "NOT_FOUND", "/test/errors/not-found")
                .andExpect(jsonPath("$.message").value("Widget not found"));
    }

    @Test
    void unknownRouteReturnsNotFound() throws Exception {
        assertErrorBody(mockMvc.perform(authenticated(get("/test/errors/does-not-exist"))),
                404, "NOT_FOUND", "/test/errors/does-not-exist");
    }

    @Test
    void wrongMethodReturnsMethodNotAllowed() throws Exception {
        assertErrorBody(mockMvc.perform(authenticated(delete("/test/errors/ok"))),
                405, "METHOD_NOT_ALLOWED", "/test/errors/ok");
    }

    @Test
    void wrongContentTypeReturnsUnsupportedMediaType() throws Exception {
        ResultActions result = mockMvc.perform(authenticated(post("/test/errors/validated"))
                .contentType(MediaType.TEXT_PLAIN)
                .content("name=x"));

        assertErrorBody(result, 415, "UNSUPPORTED_MEDIA_TYPE", "/test/errors/validated");
    }

    @Test
    void dataIntegrityViolationReturnsConflictWithoutConstraintName() throws Exception {
        assertErrorBody(mockMvc.perform(authenticated(get("/test/errors/conflict"))),
                409, "DATA_CONFLICT", "/test/errors/conflict")
                .andExpect(content().string(not(containsString(LEAKY_CONSTRAINT_NAME))));
    }

    @Test
    void unexpectedExceptionReturnsInternalErrorWithoutLeakingDetails() throws Exception {
        assertErrorBody(mockMvc.perform(authenticated(get("/test/errors/boom"))),
                500, "INTERNAL_ERROR", "/test/errors/boom")
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("secret internal detail"))))
                .andExpect(content().string(not(containsString(LEAKY_INTERNAL_MESSAGE))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", bearer);
    }

    /** Asserts the exact standard shape: these six fields and nothing else. */
    static ResultActions assertErrorBody(ResultActions result, int status, String code, String path)
            throws Exception {
        return result
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.*", hasSize(6)))
                .andExpect(jsonPath("$.timestamp", matchesPattern(ISO_INSTANT)))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
