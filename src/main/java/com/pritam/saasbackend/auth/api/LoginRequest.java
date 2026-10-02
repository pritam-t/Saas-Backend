package com.pritam.saasbackend.auth.api;

public record LoginRequest(
        String email,
        String password
) {
}