package com.pritam.saasbackend.audit.application;

import com.pritam.saasbackend.audit.domain.ActorType;
import com.pritam.saasbackend.audit.domain.AuditAction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditEventTest {

    @Test
    void requiresActorTypeAndAction() {
        assertThatThrownBy(() -> new AuditEvent(null, null, null, null, AuditAction.LOGOUT, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuditEvent(ActorType.SYSTEM, null, null, null, null, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsSystemActorWithActorId() {
        assertThatThrownBy(() -> new AuditEvent(ActorType.SYSTEM, UUID.randomUUID(), null, null,
                AuditAction.TENANT_CREATED, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultsDetailsToEmptyAndCopiesThem() {
        assertThat(new AuditEvent(ActorType.SYSTEM, null, null, null,
                AuditAction.TENANT_CREATED, null, null, null, null).details()).isEmpty();

        Map<String, Object> details = new HashMap<>(Map.of("k", "v"));
        AuditEvent event = new AuditEvent(ActorType.SYSTEM, null, null, null,
                AuditAction.TENANT_CREATED, null, null, details, null);
        details.put("later", "change");

        assertThat(event.details()).containsOnlyKeys("k");
    }
}
