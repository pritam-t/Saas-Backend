package com.pritam.saasbackend.tenant.application;

import com.pritam.saasbackend.tenant.api.dto.CreateTenantRequest;
import com.pritam.saasbackend.tenant.api.dto.TenantResponse;
import com.pritam.saasbackend.tenant.persistence.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TenantServiceIntegrationTest {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void shouldCreateTenant() {

        CreateTenantRequest request =
                new CreateTenantRequest(
                        "Test Company",
                        "test-company-" + UUID.randomUUID()
                );

        TenantResponse response =
                tenantService.createTenant(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Test Company");
        assertThat(response.slug()).isEqualTo(request.slug());
        assertThat(response.status()).isEqualTo(
                com.pritam.saasbackend.tenant.domain.TenantStatus.ACTIVE
        );
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();

        var savedTenant =
                tenantRepository.findById(response.id());

        assertThat(savedTenant).isPresent();
        assertThat(savedTenant.get().getSchemaName())
                .matches("tenant_[a-f0-9]{32}");
    }

    @Test
    void shouldRejectDuplicateSlug() {

        String slug = "duplicate-" + UUID.randomUUID();

        CreateTenantRequest request =
                new CreateTenantRequest(
                        "First Company",
                        slug
                );

        tenantService.createTenant(request);

        assertThatThrownBy(() ->
                tenantService.createTenant(
                        new CreateTenantRequest(
                                "Second Company",
                                slug
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tenant with slug already exists: " + slug);
    }
}