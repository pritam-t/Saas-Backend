package com.pritam.saasbackend.support.web;

import com.pritam.saasbackend.common.api.ApiException;
import com.pritam.saasbackend.common.api.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoints that trigger each failure type of the error model.
 * Lives in src/test, so component scanning registers it in test contexts only;
 * it is never packaged into the application jar.
 */
@RestController
@RequestMapping("/test/errors")
public class ErrorScenarioController {

    public static final String LEAKY_INTERNAL_MESSAGE = "secret internal detail SELECT * FROM public.tenants";
    public static final String LEAKY_CONSTRAINT_NAME = "uk_secret_constraint_name";

    public record ScenarioRequest(
            @NotBlank String name,
            @Size(max = 5) String code
    ) {
    }

    @GetMapping("/ok")
    public String ok() {
        return "ok";
    }

    @PostMapping("/validated")
    public String validated(@Valid @RequestBody ScenarioRequest request) {
        return "ok";
    }

    @GetMapping("/typed")
    public String typed(@RequestParam int count) {
        return "ok";
    }

    @GetMapping("/param-validated")
    public String paramValidated(@RequestParam @Min(1) int size) {
        return "ok";
    }

    @GetMapping("/not-found")
    public String notFound() {
        throw new ApiException(ErrorCode.NOT_FOUND, "Widget not found");
    }

    @GetMapping("/boom")
    public String boom() {
        throw new IllegalStateException(LEAKY_INTERNAL_MESSAGE);
    }

    @GetMapping("/conflict")
    public String conflict() {
        throw new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"" + LEAKY_CONSTRAINT_NAME + "\"");
    }

    @GetMapping("/denied")
    public String denied() {
        throw new AccessDeniedException("internal reason");
    }
}
