package com.pritam.saasbackend.audit.domain;

/**
 * Audit event catalog (spec 4.5). Stored by name, so renaming a constant
 * changes what future rows contain; add new constants instead.
 */
public enum AuditAction {
    PLATFORM_LOGIN_SUCCESS,
    PLATFORM_LOGIN_FAILURE,
    TENANT_USER_LOGIN_SUCCESS,
    TENANT_USER_LOGIN_FAILURE,
    TENANT_CREATED,
    TENANT_PROVISIONING_FAILED,
    TENANT_SUSPENDED,
    TENANT_ACTIVATED,
    USER_CREATED,
    USER_DELETED,
    USER_DISABLED,
    ROLE_CREATED,
    ROLE_UPDATED,
    ROLE_DELETED,
    ROLE_ASSIGNED,
    ROLE_REVOKED,
    TOKEN_REFRESH_REUSE_DETECTED,
    CROSS_TENANT_ACCESS,
    LOGOUT
}
