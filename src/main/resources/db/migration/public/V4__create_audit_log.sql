-- Global audit trail (spec 4.5). Append-only enforcement (trigger) arrives in Phase 7.
CREATE TABLE public.audit_log (
    id          UUID         PRIMARY KEY,
    actor_type  VARCHAR(30)  NOT NULL,
    -- No FK: tenant users live in tenant schemas and SYSTEM has no row.
    actor_id    UUID,
    -- Snapshot at event time, so the entry stays readable after the user changes or is deleted.
    actor_email VARCHAR(255),
    -- No FK: audit entries must outlive a deleted tenant.
    tenant_id   UUID,
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    -- Text, because targets include UUIDs and natural keys such as permission codes.
    target_id   VARCHAR(100),
    details     JSONB        NOT NULL DEFAULT '{}'::jsonb,
    -- 45 = longest textual IPv6 address. INET would need a custom Hibernate mapping.
    ip          VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_audit_log_actor_type
        CHECK (actor_type IN ('PLATFORM_USER', 'TENANT_USER', 'SYSTEM'))
);

CREATE INDEX idx_audit_log_tenant_created ON public.audit_log (tenant_id, created_at);
