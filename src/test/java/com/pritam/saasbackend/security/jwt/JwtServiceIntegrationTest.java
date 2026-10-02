package com.pritam.saasbackend.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.pritam.saasbackend.support.IntegrationTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class JwtServiceIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldGenerateAccessToken() {

        UUID userId = UUID.randomUUID();

        String token =
                jwtService.generateAccessToken(
                        userId,
                        "PLATFORM_USER"
                );

        assertThat(token).isNotBlank();

        assertThat(token.split("\\."))
                .hasSize(3);
    }

    @Test
    void shouldGenerateAndReadClaims() {

        UUID userId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(
                userId,
                "PLATFORM_USER"
        );

        UUID extractedUserId =
                jwtService.extractUserId(token);

        String extractedUserType =
                jwtService.extractUserType(token);

        assertThat(extractedUserId)
                .isEqualTo(userId);

        assertThat(extractedUserType)
                .isEqualTo("PLATFORM_USER");
    }

    @Test
    void shouldRejectTamperedToken() {

        UUID userId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(
                userId,
                "PLATFORM_USER"
        );

        String[] parts = token.split("\\.");

        String tamperedToken =
                parts[0] + "." +
                        parts[1] + "tampered." +
                        parts[2];

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> jwtService.extractUserId(tamperedToken)
        ).isInstanceOf(Exception.class);
    }
}