package com.pritam.saasbackend.audit.application;

/**
 * Synchronous audit trail (spec 9.6). Every phase emits events through this
 * interface instead of retrofitting audit later.
 */
public interface AuditService {

    /**
     * Joins the caller's transaction: the action and its audit entry commit
     * or roll back together. Use for security-critical changes.
     */
    void record(AuditEvent event);

    /**
     * Commits in its own transaction, so the entry survives a rollback of the
     * caller. Use for events about failures, e.g. a failed login.
     */
    void recordIndependently(AuditEvent event);
}
