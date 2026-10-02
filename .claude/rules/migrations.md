---
paths:
  - "src/main/resources/db/**"
  - "src/main/java/**/*Flyway*.java"
  - "src/main/java/**/*Migration*.java"
---

# Database migration rules

- Flyway is the only schema authority. Never edit an applied migration; add the next version.
- Two locations: `db/migration/public` (Spring's default run) and `db/migration/tenant` (run per tenant with the Flyway Java API, its own history table and target schema). Never share version numbers between them.
- Public migrations: use `public.`-qualified names. Tenant migrations: no schema qualifier and no `tenant_id` column (the schema is the tenant), except FKs to `public.permissions(code)`.
- Permission seed lives in repeatable `R__permissions.sql` using `INSERT ... ON CONFLICT`. Every authority used in `@PreAuthorize` must exist there.
- Migrations must be idempotent where feasible (`IF NOT EXISTS`), because provisioning retries re-run them.
- Use `TIMESTAMPTZ` for timestamps (maps to `OffsetDateTime`). Keep unique constraints at DB level even if the app validates.
- Use `lower(email)` unique indexes, not `citext` (extensions install into `public`).
- `tenants.schema_name` must have `CHECK (schema_name ~ '^t_[a-z0-9]{6,40}$')`.
- Startup tenant migration loop: a failure marks only that tenant `MIGRATION_FAILED` and logs it. It must never block other tenants.
- Add or update a Flyway test for every migration: empty DB migrates to head and `ddl-auto=validate` passes.