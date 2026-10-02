package com.pritam.saasbackend.audit.application;

import com.pritam.saasbackend.audit.domain.ActorType;
import com.pritam.saasbackend.audit.domain.AuditAction;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * What happened, who did it, and to what. {@code ip} may be left null:
 * the service fills it from the current HTTP request when there is one.
 */
public record AuditEvent(
        ActorType actorType,
        UUID actorId,
        String actorEmail,
        UUID tenantId,
        AuditAction action,
        String targetType,
        String targetId,
        Map<String, Object> details,
        String ip
) {

    public AuditEvent {
        Objects.requireNonNull(actorType, "actorType is required");
        Objects.requireNonNull(action, "action is required");
        if (actorType == ActorType.SYSTEM && actorId != null) {
            throw new IllegalArgumentException("A SYSTEM actor has no actorId");
        }
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public AuditEvent withIp(String newIp) {
        return new AuditEvent(actorType, actorId, actorEmail, tenantId, action, targetType, targetId, details, newIp);
    }
}
