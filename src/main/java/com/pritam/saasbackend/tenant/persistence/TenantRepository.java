package com.pritam.saasbackend.tenant.persistence;

import com.pritam.saasbackend.tenant.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository
        extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    Optional<Tenant> findBySchemaName(String schemaName);

    boolean existsBySlug(String slug);

    boolean existsBySchemaName(String schemaName);
}