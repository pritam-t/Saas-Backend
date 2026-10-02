package com.pritam.saasbackend.audit.persistence;

import com.pritam.saasbackend.audit.domain.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository
        extends JpaRepository<AuditLogEntry, UUID> {
}
