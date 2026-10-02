package com.pritam.saasbackend.tenant.api.dto;

import com.pritam.saasbackend.tenant.domain.TenantStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String slug,
        TenantStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}