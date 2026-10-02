---
paths:
  - "src/main/java/**/security/**"
  - "src/main/java/**/tenant/**"
  - "src/main/java/**/tenantuser/**"
  - "src/main/java/**/role/**"
  - "src/main/java/**/audit/**"
  - "src/main/java/**/platformuser/**"
---

# Tenancy and security rules

## Tenant isolation
- Schema name is server-generated: `"t_" + 12 lowercase hex chars from the tenant UUID`. One shared validator for every code path; also validate in the connection provider (regex or `public`).
- Set `search_path` with parameterized `set_config('search_path', ?, false)`. Never string concatenation. Quote identifiers for `CREATE SCHEMA`.
- The connection provider MUST reset `search_path` on release before the connection returns to the pool.
- Hibernate: one EntityManagerFactory. Resolver returns the tenant schema when `TenantContext` is set and `"public"` otherwise. Public entities use `@Table(schema = "public")`. Tenant repositories fail closed with no context.
- `spring.jpa.open-in-view=false` is required.
- `TenantContext` is set after JWT validation and cleared in `finally` on every request. If async is added, propagate context with a `TaskDecorator`.
- Verify Hibernate 7 SPI names against its docs before coding (generic `CurrentTenantIdentifierResolver<T>` / `MultiTenantConnectionProvider<T>`, registered via `HibernatePropertiesCustomizer`).

## Authentication
- Two `SecurityFilterChain` beans: `/platform/**` accepts only `typ=PLATFORM` + platform audience; `/t/**` accepts only `typ=TENANT` + tenant audience.
- JWT claims: `sub`, `typ`, `tid` (UUID, never schema name; absent for platform), `jti`, `iss`, `aud`, `iat`, `exp`. No permissions in the token. Pin the algorithm, key of at least 256 bits from env var.
- BCrypt only; password length 10-72 bytes. Generic login errors, dummy hash compare for unknown users, audit failed logins.
- Refresh tokens: opaque, stored as SHA-256 hash, rotated on every use, reuse revokes the whole family and is audited. `tokens_valid_after` invalidates older access tokens.
- Suspended tenant: `403` with code `TENANT_SUSPENDED`.

## Authorization
- Method security with authorities, e.g. `@PreAuthorize("hasAuthority('USER_READ')")`. Platform operations require `ROLE_SUPER_ADMIN`.
- Escalation rules: assign only roles whose permissions are a subset of the actor's; system roles are immutable; no self-delete or self-disable; the last `TENANT_ADMIN` cannot be removed or demoted.

## Audit
- `AuditService` is called for every sensitive action, in the same transaction for security-critical events. `public.audit_log` is append-only (trigger rejects UPDATE/DELETE). Do not claim "tamper-evident" without a hash chain.

## Errors
- One `@RestControllerAdvice`. Error body: `{ timestamp, status, code, message, path, fieldErrors[] }`. Never leak SQL, stack traces or schema names. DTOs only, never entities.