package com.pritam.saasbackend.tenant.api.dto;

public record CreateTenantRequest(
        String name,
        String slug
) {
}