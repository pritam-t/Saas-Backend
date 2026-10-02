package com.pritam.saasbackend.tenant.application;

import com.pritam.saasbackend.tenant.api.dto.CreateTenantRequest;
import com.pritam.saasbackend.tenant.api.dto.TenantResponse;
import com.pritam.saasbackend.tenant.domain.Tenant;
import com.pritam.saasbackend.tenant.domain.TenantStatus;
import com.pritam.saasbackend.tenant.persistence.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {

        if (tenantRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException(
                    "Tenant with slug already exists: " + request.slug()
            );
        }

        UUID tenantId = UUID.randomUUID();

        String schemaName =
                "tenant_" + tenantId.toString().replace("-", "");

        OffsetDateTime now = OffsetDateTime.now();

        Tenant tenant = new Tenant(
                tenantId,
                request.name(),
                request.slug(),
                schemaName,
                TenantStatus.ACTIVE,
                now,
                now
        );

        Tenant savedTenant =
                tenantRepository.save(tenant);

        return new TenantResponse(
                savedTenant.getId(),
                savedTenant.getName(),
                savedTenant.getSlug(),
                savedTenant.getStatus(),
                savedTenant.getCreatedAt(),
                savedTenant.getUpdatedAt()
        );
    }
}