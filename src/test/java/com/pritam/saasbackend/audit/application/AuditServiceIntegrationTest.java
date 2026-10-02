package com.pritam.saasbackend.audit.application;

import com.pritam.saasbackend.audit.domain.ActorType;
import com.pritam.saasbackend.audit.domain.AuditAction;
import com.pritam.saasbackend.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads rows back with plain SQL, so the test checks what is really stored
 * in public.audit_log (including the JSONB column), not just the JPA mapping.
 * Every test uses its own random target id because the container is shared.
 */
@IntegrationTest
class AuditServiceIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldPersistEveryField() {
        UUID actorId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String targetId = UUID.randomUUID().toString();

        auditService.record(new AuditEvent(
                ActorType.PLATFORM_USER,
                actorId,
                "admin@example.com",
                tenantId,
                AuditAction.TENANT_SUSPENDED,
                "TENANT",
                targetId,
                Map.of("reason", "unpaid invoice", "attempt", 2),
                "198.51.100.4"
        ));

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT actor_type, actor_id, actor_email, tenant_id, action, target_type, target_id,
                       details->>'reason' AS reason, (details->>'attempt')::int AS attempt,
                       jsonb_typeof(details) AS details_type, ip, created_at
                FROM public.audit_log
                WHERE target_id = ?
                """, targetId);

        assertThat(row.get("actor_type")).isEqualTo("PLATFORM_USER");
        assertThat(row.get("actor_id")).isEqualTo(actorId);
        assertThat(row.get("actor_email")).isEqualTo("admin@example.com");
        assertThat(row.get("tenant_id")).isEqualTo(tenantId);
        assertThat(row.get("action")).isEqualTo("TENANT_SUSPENDED");
        assertThat(row.get("target_type")).isEqualTo("TENANT");
        assertThat(row.get("reason")).isEqualTo("unpaid invoice");
        assertThat(row.get("attempt")).isEqualTo(2);
        assertThat(row.get("details_type")).isEqualTo("object");
        assertThat(row.get("ip")).isEqualTo("198.51.100.4");
        assertThat(row.get("created_at")).isNotNull();
    }

    @Test
    void shouldStoreSystemEventWithoutActorAndWithEmptyDetails() {
        // Spring's test listener binds a mock request to every test thread;
        // clear it to model a non-HTTP caller (e.g. the startup migration loop).
        RequestContextHolder.resetRequestAttributes();
        String targetId = UUID.randomUUID().toString();

        auditService.record(systemEvent(targetId));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT actor_id, details::text AS details, ip FROM public.audit_log WHERE target_id = ?",
                targetId);

        assertThat(row.get("actor_id")).isNull();
        assertThat(row.get("details")).isEqualTo("{}");
        assertThat(row.get("ip")).isNull();
    }

    @Test
    void recordRollsBackWithTheCallersTransaction() {
        String targetId = UUID.randomUUID().toString();

        transactionTemplate.executeWithoutResult(status -> {
            auditService.record(systemEvent(targetId));
            status.setRollbackOnly();
        });

        assertThat(countByTargetId(targetId)).isZero();
    }

    @Test
    void recordIndependentlySurvivesTheCallersRollback() {
        String targetId = UUID.randomUUID().toString();

        transactionTemplate.executeWithoutResult(status -> {
            auditService.recordIndependently(systemEvent(targetId));
            status.setRollbackOnly();
        });

        assertThat(countByTargetId(targetId)).isEqualTo(1);
    }

    @Test
    void shouldFillIpFromCurrentRequestWhenMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.7");
        request.addHeader("X-Forwarded-For", "10.9.9.9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        String targetId = UUID.randomUUID().toString();

        auditService.record(systemEvent(targetId));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT ip FROM public.audit_log WHERE target_id = ?", String.class, targetId))
                .isEqualTo("203.0.113.7");
    }

    @Test
    void shouldKeepExplicitIpEvenInsideARequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.7");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        String targetId = UUID.randomUUID().toString();

        auditService.record(systemEvent(targetId).withIp("2001:db8::1"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT ip FROM public.audit_log WHERE target_id = ?", String.class, targetId))
                .isEqualTo("2001:db8::1");
    }

    private static AuditEvent systemEvent(String targetId) {
        return new AuditEvent(ActorType.SYSTEM, null, null, null,
                AuditAction.TENANT_PROVISIONING_FAILED, "TENANT", targetId, null, null);
    }

    private int countByTargetId(String targetId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.audit_log WHERE target_id = ?", Integer.class, targetId);
        return count == null ? 0 : count;
    }
}
