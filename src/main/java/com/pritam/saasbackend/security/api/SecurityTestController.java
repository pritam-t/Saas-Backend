package com.pritam.saasbackend.security.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityTestController {

    @GetMapping("/api/v1/auth/test")
    public String publicEndpoint() {
        return "Public endpoint";
    }

    @GetMapping("/api/v1/protected/test")
    public String protectedEndpoint() {
        return "Protected endpoint";
    }
}