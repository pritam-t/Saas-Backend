package com.pritam.saasbackend.audit.application;

import com.pritam.saasbackend.audit.domain.AuditLogEntry;
import com.pritam.saasbackend.audit.persistence.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class JpaAuditService implements AuditService {

    private final AuditLogRepository auditLogRepository;

    public JpaAuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(AuditEvent event) {
        save(event);
    }

    // A second transaction means a second pool connection while the caller still holds one.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependently(AuditEvent event) {
        save(event);
    }

    private void save(AuditEvent event) {
        auditLogRepository.save(new AuditLogEntry(
                UUID.randomUUID(),
                event.actorType(),
                event.actorId(),
                event.actorEmail(),
                event.tenantId(),
                event.action(),
                event.targetType(),
                event.targetId(),
                event.details(),
                event.ip() != null ? event.ip() : currentRequestIp(),
                OffsetDateTime.now()
        ));
    }

    /**
     * The TCP peer address only. X-Forwarded-For is client-controlled and is not
     * trusted until proxy handling is configured (Phase 8/10).
     */
    private static String currentRequestIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servletAttributes
                ? servletAttributes.getRequest().getRemoteAddr()
                : null;
    }
}
