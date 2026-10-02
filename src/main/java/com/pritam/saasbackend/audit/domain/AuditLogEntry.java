package com.pritam.saasbackend.audit.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * One row of {@code public.audit_log}. Entries are only ever inserted, so the entity
 * implements {@link Persistable} and reports itself as new: Spring Data then calls
 * {@code persist()} instead of {@code merge()}, which avoids a pointless SELECT
 * before every insert of an assigned id.
 */
@Entity
@Table(name = "audit_log", schema = "public")
public class AuditLogEntry implements Persistable<UUID> {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 30)
    private ActorType actorType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AuditAction action;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> details;

    @Column(length = 45)
    private String ip;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Transient
    private boolean isNew = true;

    protected AuditLogEntry() {
    }

    public AuditLogEntry(
            UUID id,
            ActorType actorType,
            UUID actorId,
            String actorEmail,
            UUID tenantId,
            AuditAction action,
            String targetType,
            String targetId,
            Map<String, Object> details,
            String ip,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.actorType = actorType;
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.tenantId = tenantId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.ip = ip;
        this.createdAt = createdAt;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public String getIp() {
        return ip;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
