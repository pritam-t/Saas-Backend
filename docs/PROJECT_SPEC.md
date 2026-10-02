# PROJECT SPEC — Multi-Tenant SaaS Backend (Spring Boot)

> Handoff document for Claude Code. Baseline date: **2 October 2026**.
> Owner: Pritam (3rd-year Computer Engineering student). This is the **highlight resume project** for backend internship applications, so correctness, security and explainability matter more than feature count.
> Suggested location in the repo: `docs/PROJECT_SPEC.md`, referenced from `CLAUDE.md` with `@docs/PROJECT_SPEC.md`.

---

## 0. How to use this document (agent operating rules)

Read this whole file first. Then inspect the repository (`pom.xml`, `src/main/resources/db/migration`, existing packages, tests) **before changing anything**, and report any place where this document and the repo disagree. The repo is the truth about what exists; this document is the truth about intent.

### 0.1 Status labels

Every decision in this document carries one label:

| Label | Meaning | What the agent does |
|---|---|---|
| **LOCKED** | Decided by the owner in the current design doc. | Do not change. If it blocks you, stop and ask. |
| **PROPOSED** | Recommended by an engineering review of the design. Treat as approved defaults. | Implement as written. If it conflicts with the repo or proves wrong, stop, explain, and ask. |
| **OPEN** | Not decided. | Do **not** invent it silently. Propose options, ask the owner, then record the answer in section 17. |

### 0.2 Working rules

1. Work on **one bounded phase at a time** (section 14). Do not start the next phase until the current phase's acceptance criteria pass.
2. After each phase: run the full test suite (`./mvnw verify` or `mvn verify`), commit with a clear message (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`), and tick the checklist in section 16.
3. **Flyway is the only schema authority.** Never use `spring.jpa.hibernate.ddl-auto=create|update|create-drop`. Never edit a migration that has already been applied; add a new one.
4. Never trust a tenant id or schema name supplied by the client. Never concatenate untrusted text into SQL identifiers.
5. No secrets in the repo. Secrets come from environment variables; provide `.env.example`.
6. Add a library only when it solves a real problem, and say why in the commit message.
7. Prefer integration tests against a real PostgreSQL (Testcontainers) over mocks for anything touching schemas, `search_path`, Flyway or security.
8. Introduce abstractions only when they solve a real architectural problem (owner's stated preference).
9. When something in this document is ambiguous, ask one focused question instead of guessing.
10. Keep this document updated: when a decision is made, move it from OPEN to LOCKED/PROPOSED in sections 5, 8 and 17.

---

## 1. Project goal

Build a **production-oriented, multi-tenant SaaS backend** with Spring Boot that demonstrates understanding of authentication, authorization, tenant isolation, persistence, migrations, testing, auditing and clean architecture. It must be more than CRUD.

Core capabilities (all **LOCKED** unless noted):

- Multiple independent tenants in **one PostgreSQL database**.
- **Schema-per-tenant** isolation for tenant-owned data.
- Platform-wide **SUPER_ADMIN** identities stored in `public.platform_users`.
- Tenant users stored **inside their tenant schema**.
- **Global, platform-managed permission definitions** (reference data).
- **Tenant-specific roles** and role assignments; one user can have multiple roles.
- **JWT** authentication with **Spring Security**; **BCrypt** password hashing.
- SUPER_ADMIN can operate across tenants where authorized.
- **Global audit log** (`public.audit_log`) for platform users, tenant users and system actions.
- **Flyway**-controlled database evolution.
- Integration tests for persistence, services, security and business rules.
- Architecture: **modular monolith** with domain / application / persistence / security boundaries.

### 1.1 Non-goals (LOCKED)

- No OAuth / social login.
- No microservices; no unnecessary distributed-system complexity.
- The *core system* must not depend on a cloud service (this does not forbid deploying it; see section 10 and open question Q1).
- Tenants cannot create or delete global permission definitions.
- No CRUD-only implementation.

---

## 2. Technology stack

Verified against `pom.xml` and the Spring Boot 4.1.1 BOM in Phase 0 (2 Oct 2026). `pom.xml` is the source of truth; update this table when it changes.

| Technology | Version | Use | Notes |
|---|---|---|---|
| Java | 25 | Runtime | |
| Spring Boot | 4.1.1 | Framework | Spring Framework 7.0.9 |
| Spring Data JPA, Hibernate ORM | 7.4.5.Final | Persistence | Multi-tenancy SPI used in Phase 5 |
| PostgreSQL | server 18.3, JDBC driver 42.7.13 | Database | Tests run on the `postgres:18` image. PostgreSQL 18 has built-in `uuidv7()`; optional for ids |
| Flyway | 12.4.0 | Migrations | With `flyway-database-postgresql` (required on Flyway 10+) |
| Spring Security | 7.1.1 | AuthN/AuthZ | |
| JJWT (`jjwt-api`, `-impl`, `-jackson`) | 0.12.6 | JWT | Version pinned in `pom.xml` (not Boot-managed). `jjwt-jackson` uses Jackson 2; Boot 4 manages Jackson 3.1.5 |
| BCrypt | (Spring Security) | Passwords | |
| JUnit Jupiter, Spring Boot Test, Mockito | 6.0.3, 4.1.1, 5.23.0 | Tests | |
| Testcontainers (PostgreSQL) | 2.0.5 | Integration tests | Added in Phase 1. Artifacts `testcontainers-postgresql`, `testcontainers-junit-jupiter`; class `org.testcontainers.postgresql.PostgreSQLContainer`; Boot `@ServiceConnection` |
| Caffeine | not yet added | **PROPOSED** | In-process caches (tenant registry, permissions) |
| Bucket4j | not yet added | **PROPOSED** | Rate limiting, in-memory (see section 10) |
| Docker, Docker Compose | not yet added | **PROPOSED** (was in original blueprint) | |
| Maven | 3.9.16 (wrapper) | Build | |
| Lombok | removed in Phase 1 | | Was declared but unused |

---

## 3. Architecture

Shared PostgreSQL database, schema-per-tenant.

```
public schema                              tenant schema (one per tenant, e.g. t_3f9a1c07b2de)
-------------                              ------------------------------------------------
tenants          (registry)                users
platform_users   (SUPER_ADMIN identities)  roles
permissions      (global reference data)   role_permissions  -> public.permissions(code)
audit_log        (global, append-only)     user_roles
refresh_tokens   (PROPOSED)                (future business entities)
flyway_schema_history (public migrations)  flyway_schema_history (per tenant)
```

### 3.1 Request flow (PROPOSED design)

```
Tenant request:   Client -> [IP rate limit] -> Tenant JWT filter -> TenantContext filter
                  -> Hibernate tenant resolver -> connection provider (set search_path) -> tenant schema
Platform request: Client -> [IP rate limit] -> Platform JWT filter -> public schema (+ explicit tenant targeting)
```

Two separate `SecurityFilterChain` beans, selected with `securityMatcher`:

- `/platform/**` : platform chain. Accepts only tokens with `typ=PLATFORM` and the platform audience.
- `/t/**` : tenant chain. Accepts only tokens with `typ=TENANT` and the tenant audience.
- Public: `/platform/auth/login`, `/t/{tenantSlug}/auth/login`, `/auth/refresh`, `/actuator/health`.

---

## 4. Data model

Column lists marked **PROPOSED** are the review's recommended shape; the design doc left them open.

### 4.1 `public.tenants` (LOCKED fields, PROPOSED status values)

| Column | Type | Rule |
|---|---|---|
| id | UUID | PK, generated by JPA/application |
| name | VARCHAR(100) | required. Currently globally unique; **PROPOSED:** drop the unique constraint (two companies can share a name). Slug stays unique. |
| slug | VARCHAR(100) | required, globally unique; regex `^[a-z][a-z0-9-]{2,40}$`; reserved slugs rejected |
| schema_name | VARCHAR(100) | required, globally unique, **server-generated** (see 8.6); DB `CHECK (schema_name ~ '^t_[a-z0-9]{6,40}$')` |
| status | VARCHAR(30) | LOCKED: `ACTIVE`, `SUSPENDED`. **PROPOSED additions:** `PROVISIONING`, `PROVISIONING_FAILED`, `MIGRATION_FAILED`, `DELETING` |
| created_at, updated_at | TIMESTAMPTZ | required, map to `OffsetDateTime` |

Java entity: `com.pritam.saasbackend.tenant.domain.Tenant` (id, name, slug, schemaName, status, createdAt, updatedAt) plus `TenantStatus`.

### 4.2 `public.platform_users` (columns PROPOSED)

`id UUID PK`, `email VARCHAR(255) NOT NULL` with a unique index on `lower(email)`, `password_hash VARCHAR(100) NOT NULL` (BCrypt), `status VARCHAR(30)` (`ACTIVE`, `DISABLED`), `platform_role VARCHAR(30) NOT NULL DEFAULT 'SUPER_ADMIN'` with a CHECK constraint, `tokens_valid_after TIMESTAMPTZ`, `created_at`, `updated_at`. The exact existing shape is whatever the repo has; reconcile with a migration, do not rewrite history.

### 4.3 `public.permissions` (LOCKED concept, PROPOSED seed)

`id UUID`, `code VARCHAR(100) NOT NULL UNIQUE`, `description`, `created_at`. Platform-managed reference data, seeded by a Flyway **repeatable** migration `R__permissions.sql` using insert-on-conflict.

Proposed initial codes: `USER_READ`, `USER_CREATE`, `USER_UPDATE`, `USER_DELETE`, `ROLE_READ`, `ROLE_MANAGE`, `ROLE_ASSIGN`, `TENANT_READ` (view own tenant config), `AUDIT_READ` (view own tenant's audit entries; optional).

Rule: every authority string used in `@PreAuthorize` must exist in this table. Enforce with a test.

### 4.4 Tenant schema tables (PROPOSED)

- `users`: `id UUID PK`, `email` (unique index on `lower(email)`, unique per tenant only), `password_hash`, `status` (`ACTIVE`, `DISABLED`), `tokens_valid_after TIMESTAMPTZ`, `created_at`, `updated_at`. Do **not** add a `tenant_id` column (the schema is the tenant).
- `roles`: `id UUID PK`, `name VARCHAR(100) UNIQUE`, `description`, `system_role BOOLEAN NOT NULL DEFAULT false` (system roles are immutable and undeletable), timestamps.
- `role_permissions`: `role_id` FK to `roles`, `permission_code VARCHAR(100)` FK to `public.permissions(code)`, PK `(role_id, permission_code)`. Referencing the natural key `code` is more portable than a UUID.
- `user_roles`: `user_id` FK, `role_id` FK, PK `(user_id, role_id)`.

Trade-off to document in the README: a cross-schema FK to `public.permissions` means a tenant schema cannot be restored in isolation from `public`. This matters for per-tenant GDPR export/restore.

Default roles seeded into every new tenant: `TENANT_ADMIN` (all tenant permissions, `system_role=true`) and `TENANT_USER` (no admin permissions; "own profile" is an ownership check on `/t/me`, not a permission).

### 4.5 `public.audit_log` (LOCKED concept, PROPOSED columns)

`id UUID`, `actor_type` (`PLATFORM_USER`, `TENANT_USER`, `SYSTEM`), `actor_id UUID NULL` (**deliberately no FK**: tenant users live in dynamic schemas, SYSTEM has no row), `actor_email` (snapshot), `tenant_id UUID NULL`, `action VARCHAR`, `target_type`, `target_id`, `details JSONB`, `ip`, `created_at TIMESTAMPTZ`. Index `(tenant_id, created_at)`.

Proposed event catalog: `PLATFORM_LOGIN_SUCCESS/FAILURE`, `TENANT_USER_LOGIN_SUCCESS/FAILURE`, `TENANT_CREATED`, `TENANT_PROVISIONING_FAILED`, `TENANT_SUSPENDED`, `TENANT_ACTIVATED`, `USER_CREATED`, `USER_DELETED`, `USER_DISABLED`, `ROLE_CREATED`, `ROLE_UPDATED`, `ROLE_DELETED`, `ROLE_ASSIGNED`, `ROLE_REVOKED`, `TOKEN_REFRESH_REUSE_DETECTED`, `CROSS_TENANT_ACCESS`, `LOGOUT`.

### 4.6 `public.refresh_tokens` (PROPOSED)

`id`, `subject_type` (PLATFORM/TENANT), `subject_id`, `tenant_id NULL`, `token_hash` (SHA-256 of an opaque 256-bit random token; never store the raw token), `family_id`, `expires_at`, `revoked_at`, `replaced_by`, `created_at`. Index on `token_hash`.

---

## 5. Locked architectural decisions (LOCKED)

| Decision | Choice |
|---|---|
| Multi-tenancy | Schema-per-tenant in one PostgreSQL database |
| Tenant users | Inside their tenant schema |
| Platform users | `public.platform_users` |
| SUPER_ADMIN | Direct cross-tenant operation when authorized |
| Permission model | Global permission definitions + tenant-specific roles |
| Roles | Multiple roles per tenant user |
| Permission ownership | Platform-managed reference data |
| Audit | Global `public.audit_log` with `actor_type`, `actor_id`, `actor_email`, nullable `tenant_id` |
| Authentication | JWT + Spring Security |
| Password hashing | BCrypt |
| Migrations | Flyway |
| Identifiers | UUID |

---

## 6. Current implementation state (as of 2 Oct 2026)

> **Update after Phase 1 (2 Oct 2026).** Sections 6, 6.1 and 6.2 below describe the state *before* Phase 0/1 and are kept as history.
> Phase 0 found that the committed repo could not migrate at all: `V1` and `V5` both created `public.tenants`, and there were two `V7` files (`Found more than one migration with version 7`, all 10 tests erroring). With owner approval the public migrations were squashed into `db/migration/public/V1..V3` (V1 = final `tenants` table; V5, V6, both V7 deleted), `spring.flyway.locations` is explicit, `open-in-view=false`, tests run on Testcontainers `postgres:18` with a `test` profile, and `Tenant` ids are assigned (no `@GeneratedValue`). Baseline: **15/15 tests green** (the original 10 plus 5 in `FlywaySchemaIntegrationTest`). Local dev databases must be dropped and recreated once.

Done (pre-Phase 1):

- Spring Boot project `saas-backend` generated and running; PostgreSQL connected.
- Flyway integrated; currently at **version 6** (`6 - add tenant slug` applied).
- `Tenant` entity and `TenantStatus` implemented. The `public.tenants` table has id, name, slug, schema_name and status-related data.
- Packages visible in tests: `tenant`, `permission`, `platformuser`, `security.jwt`.

Test baseline (10 tests: 8 passed, 0 assertion failures, 2 errors):

| Test class | Result |
|---|---|
| PermissionRepositoryIntegrationTest | 2 passed |
| PlatformUserRepositoryIntegrationTest | 1 passed |
| PlatformUserServiceIntegrationTest | 1 passed |
| SaasBackendApplicationTests | 1 passed |
| JwtServiceIntegrationTest | 3 passed |
| TenantServiceIntegrationTest | **2 errors** |

### 6.1 Blocking issue

`ERROR: column t1_0.created_at does not exist`. The `Tenant` entity declares `created_at` and `updated_at`, but `public.tenants` does not have them. Hibernate selects `t1_0.id, t1_0.created_at, t1_0.name, t1_0.schema_name, t1_0.slug, t1_0.status, t1_0.updated_at`.

Correct approach: reconcile through a **new Flyway migration** (next version, expected `V7`). Do not edit an applied migration. Do not let Hibernate alter the schema.

### 6.2 Known cleanup items

Lombok/Unsafe deprecation warnings, Mockito's future agent requirement, and `spring.jpa.open-in-view` (see 9.3: this one is required, not cosmetic).

Phase 1: Lombok removed (it was unused) and `open-in-view=false` set. Mockito's agent warning is still open.

### 6.3 Repo findings (Phase 0) and target phase

Found while reconciling this document with the repo. Deliberately **not** changed in Phase 1.

| Finding | Target phase |
|---|---|
| ~~`security/api/SecurityTestController` exposes `/api/v1/auth/test` and `/api/v1/protected/test` in production code~~ | **Done in Phase 2:** deleted; replaced by the test-only `ErrorScenarioController` |
| `auth` package (login at `/api/v1/auth/login`, `AuthenticationService`, empty `AuthService`) is not in the planned package layout | Phase 3: fold into platform auth / `security` |
| Duplicate empty classes `auth/dto/LoginRequest` and `auth/dto/LoginResponse` (the real records live in `auth/api`) | Phase 3 |
| `TenantController` at `/api/tenants` is reachable by any authenticated token, with no role check | Phase 4: replaced by `POST /platform/tenants` |
| `TenantService` builds schema names as `tenant_` + 32 hex chars; `.claude/rules` require `t_` + 12 hex chars and `CHECK (schema_name ~ '^t_[a-z0-9]{6,40}$')` (no CHECK added yet, by owner decision) | Phase 4: schema naming/validator |
| `uk_tenants_name` (unique tenant display name) kept for now | Phase 4: revisit |
| `public.permissions` has no `created_at` (4.3 lists one) | Phase 4: with `R__permissions.sql` |
| `PlatformUserServiceIntegrationTest` sits in the `platformuser.persistence` package but tests the application service | Phase 3 |
| `show-sql` and `format_sql` are on in the main `application.yml` | Phase 10: move to a dev-only profile (proposed) |
| The JVM time zone is sent to PostgreSQL. `postgres:18` rejects the Windows legacy name `Asia/Calcutta`; tests pin `-Duser.timezone=UTC` in Surefire, but the app is unpinned | Phase 10: pin the time zone for the Docker/Compose runtime (proposed) |
| `GlobalExceptionHandler` (in `common`) handles `auth.application.InvalidCredentialsException` explicitly, so `common` depends on `auth` | Phase 3: make it an `ApiException(INVALID_CREDENTIALS)` and drop the handler |
| Exceptions thrown by filters other than the security entry point / access-denied handler still get Spring Boot's default `/error` body, not `ApiError` | Phase 8 (API contract polish): custom `ErrorController` if any such filter is added (proposed) |
| `AuditService.recordIndependently` (`REQUIRES_NEW`) holds a second pool connection while the caller's is open | Phase 5: keep in mind for the pool-of-2 isolation test |
| Hibernate's JSON format mapper for `audit_log.details` is not pinned (Jackson 2 from `jjwt-jackson` and Jackson 3 from Boot are both on the classpath); the JSONB round-trip test passes | Pin `hibernate.type.json_format_mapper` only if the round-trip test ever breaks |
| `POST /api/tenants` with a duplicate slug throws `IllegalArgumentException`, which now returns a clean 500 `INTERNAL_ERROR` | Phase 4: replaced by `/platform/tenants` with a 409 domain code |

---

## 7. Immediate task: Phase 1 (do this first)

1. Inspect the real table (`\d public.tenants`) and the entity. List **every** difference (missing columns, nullability, types, lengths, unique constraints, defaults), not only `created_at`.
2. Add one new migration (for example `V7__reconcile_tenant_timestamps.sql`):
   ```sql
   ALTER TABLE public.tenants ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();
   ALTER TABLE public.tenants ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
   ```
   plus any other differences found in step 1. Entity `OffsetDateTime` fields must map to `TIMESTAMP WITH TIME ZONE`.
3. Set `spring.jpa.hibernate.ddl-auto=validate` so this class of mismatch fails at startup instead of inside two tests. Fix every further mismatch it reveals with migrations.
4. Set `spring.jpa.open-in-view=false`.
5. Introduce **Testcontainers PostgreSQL 18** for integration tests (the Boot service-connection support) so each run starts from an empty database and Flyway rebuilds the schema. Verify the exact Boot 4 package and annotation names against its docs.
6. Add a test: "Flyway migrates an empty database to head and the Hibernate mapping validates".
7. Run the full suite. **Green baseline = 10/10 passing.** Commit.

If the owner explicitly approves squashing the 6 early migrations into a clean `V1` (acceptable only because nothing is released), do that instead, but only on explicit approval.

---

## 8. Decisions to lock before more feature code (all PROPOSED)

### 8.1 Public entities vs. tenant routing

Once Hibernate multi-tenancy exists, every session needs a tenant identifier. `Tenant`, `PlatformUser`, `Permission`, `AuditLog` and `RefreshToken` have none, so their repositories would break (they pass today only because multi-tenancy is not built).

Decision: **one EntityManagerFactory**; the `CurrentTenantIdentifierResolver` returns the tenant schema when a `TenantContext` is set and `"public"` when it is not. Public entities declare `@Table(schema = "public")`. Tenant-scoped repositories must **fail closed** when no context is set (there is no unqualified table in `public`, so queries error rather than leak). Alternative if this proves awkward: two EntityManagerFactories with separate `@EnableJpaRepositories` packages. Ask before switching.

Verify the Hibernate 7 SPI details (generic `CurrentTenantIdentifierResolver<T>` and `MultiTenantConnectionProvider<T>`, registering them through Spring Boot's `HibernatePropertiesCustomizer`; the old `hibernate.multiTenancy` setting no longer exists).

### 8.2 JWT claims (decide now; `JwtService` already has 3 tests)

- Claims: `sub` (user UUID), `typ` (`PLATFORM` | `TENANT`), `tid` (tenant UUID, **never** the schema name; absent for platform tokens), `jti`, `iss`, `aud`, `iat`, `exp`.
- Separate **audiences** (`saas-platform`, `saas-tenant`), and preferably separate signing keys, so a tenant token cannot be replayed on platform endpoints.
- Algorithm pinned (reject `none` and algorithm confusion); key of at least 256 bits from an environment variable; validate `iss`, `aud`, `exp`.
- Do **not** put permissions in the token. Resolve them server-side (cached, see 8.8).
- Access token TTL default 15 minutes; refresh token 7 days (both configurable).

Adapt the existing `JwtService` and its tests rather than rewriting from scratch.

### 8.3 How SUPER_ADMIN acts across tenants

Explicit, never an implicit tenant switch: `/platform/tenants/{tenantId}/...`. The platform chain resolves the target tenant from the path, verifies it exists and is eligible, and sets `TenantContext` only for that call. Every such access writes an audit entry with `actor_type=PLATFORM_USER` and the affected `tenant_id` (`CROSS_TENANT_ACCESS` or the specific action). Platform tokens are rejected on `/t/**` and tenant tokens on `/platform/**`.

### 8.4 Tenant user login and tenant resolution

Tenant users log in at `POST /t/{tenantSlug}/auth/login` (email + password). The slug is resolved through `public.tenants` (cached), the tenant must be `ACTIVE`, credentials are checked inside that tenant's schema, and the issued token carries `tid`. On every tenant request the tenant is taken from the **token only**, never from a header or body. A suspended tenant's requests are rejected (`403`, code `TENANT_SUSPENDED`), checked against a short-TTL cache.

### 8.5 Tenant lifecycle and atomic provisioning

Schema creation, Flyway and seeding are not one atomic operation. Use a state machine on `tenants.status`:

1. Validate request (name, slug, admin email, initial password policy).
2. Transaction 1: insert tenant row, status `PROVISIONING`, with a server-generated `schema_name`. Unique constraints settle races.
3. `CREATE SCHEMA` with the validated, quoted identifier.
4. Run the tenant Flyway migrations on that schema.
5. Seed default roles (`TENANT_ADMIN`, `TENANT_USER`) and their permissions, and create the first tenant admin.
6. Transaction 2: set `ACTIVE`; write audit `TENANT_CREATED`.

On failure at steps 3-5: set `PROVISIONING_FAILED`, store the error text, audit it, and expose an idempotent retry/cleanup. Every step must be safe to repeat (`CREATE SCHEMA IF NOT EXISTS`, Flyway is idempotent, seeding uses upserts). Tenant creation is **platform-provisioned by SUPER_ADMIN** (`POST /platform/tenants`); there is no public self-signup.

### 8.6 Schema naming and injection safety

`schema_name = "t_" + 12 lowercase hex characters derived from the tenant UUID` (for example `t_3f9a1c07b2de`). Do not derive it from the name or slug (they can change). Defence in depth: DB CHECK constraint, a single Java validator shared by every code path, quoted identifiers, and parameterized `set_config('search_path', ?, false)` instead of string concatenation. PostgreSQL identifiers are limited to 63 bytes.

### 8.7 Tenant schema migrations

- Separate Flyway locations: `db/migration/public` (Spring's default Flyway run) and `db/migration/tenant` (run per tenant). The blueprint's duplicate `V1__` names across the two are a trap.
- Use the Flyway Java API per tenant (configure the target schema, locations, and its own history table).
- On application startup, iterate all `ACTIVE` tenants and migrate each. A failure marks that tenant `MIGRATION_FAILED` and logs it; it must not take down every tenant.
- Optionally record the applied tenant-migration version on the registry row for observability.

### 8.8 Revocation without Redis

- Short-lived access tokens (15 min).
- **Refresh tokens**: opaque, stored hashed in `public.refresh_tokens`, rotated on every use, with **reuse detection** (reusing a rotated token revokes the whole family and audits it).
- `tokens_valid_after` per user (platform and tenant): a token whose `iat` is earlier than this value is rejected. Updated on password change, role change, user disable/delete.
- Logout revokes the refresh-token family. The access token stays valid until it expires (at most 15 min); document this trade-off. An optional in-memory `jti` denylist can shorten that window.
- **Caffeine** caches with short TTL (30-60 s) and explicit eviction on change: tenant registry (slug and id lookup, status) and per-user `(tenantId, userId) -> permission set` and `tokens_valid_after`. Caveat: with more than one app instance these caches are not shared; document it, and introduce Redis only if multi-instance deployment becomes a goal.

---

## 9. Improvements to what is already written (all PROPOSED)

### 9.1 Testing and database hygiene
- `ddl-auto=validate` (see section 7).
- Testcontainers PostgreSQL 18 for all integration tests (real `search_path`, real schemas, real Flyway). Do not use H2.
- `@WithMockUser` is not enough for security tests; also test with real JWTs through the real filter chains.

### 9.2 Connection safety
- The `MultiTenantConnectionProvider` must **reset `search_path` on release** (`RESET search_path`) before the connection returns to the pool, or the pool leaks tenants. Write a test for this: a pool of size 2, many parallel requests alternating between two tenants, assert no request ever sees the other tenant's data.
- For tenant sessions set `search_path` to the tenant schema only (public entities are schema-qualified). Avoid PostgreSQL extensions that install into `public` (use `lower(email)` unique indexes instead of `citext`).
- Defence in depth: the provider re-validates the identifier against the schema-name regex (or equals `public`).

### 9.3 Open-in-view
`spring.jpa.open-in-view=false` is **required**: with it enabled a connection can be bound before tenant context is resolved and held for the whole request.

### 9.4 Schema constraints
DB `CHECK` on `schema_name`; unique constraints stay at DB level even when the app validates; avoid Lombok `@Data`/`@EqualsAndHashCode` on JPA entities; take versions from `pom.xml` rather than test output; optionally use PostgreSQL 18's `uuidv7()` for index-friendly ids.

### 9.5 Permissions as reference data
Repeatable migration `R__permissions.sql` (insert-on-conflict). A test asserts that every authority referenced in `@PreAuthorize` exists in `public.permissions`, and vice versa that seeded codes are used or documented.

### 9.6 Audit
- Write the `AuditService` interface early (synchronous at first) so every phase emits events instead of retrofitting them. Security-critical events should be written in the same transaction as the action, or via an outbox, so they cannot be silently lost. If async is added later, propagate context with a `TaskDecorator`, because `TenantContext` and `SecurityContext` are ThreadLocals.
- Make the table **append-only**: a `BEFORE UPDATE OR DELETE` trigger that raises an exception. A stronger production hardening is a separate DB role without UPDATE/DELETE, noted in the README as future work.
- "Tamper-evident" may only be claimed with a hash chain (`prev_hash`, `row_hash`, per-tenant chain serialized with an advisory lock). Otherwise say "append-only".
- Audit failed logins too.

### 9.7 Smaller notes
Tenant `name` uniqueness may be dropped (slug is the identifier). Tests currently depend on environment state; Testcontainers fixes this.

---

## 10. What the original blueprint had (and what to do with it)

The owner's original 5-week blueprint planned a different stack and scope. The current design intentionally changed several decisions. This section prevents the agent from either re-adding things blindly or forgetting valuable ones.

| Original blueprint item | Current design status | Recommendation |
|---|---|---|
| **Redis**: tenant config cache (`@Cacheable`, 10-min TTL), JWT blacklist | Not in the stack | **Replace** with Caffeine caches + DB refresh tokens + `tokens_valid_after` (8.8). Add Redis only if multi-instance is required. |
| **Bucket4j** per-tenant rate limiting, 429 + `Retry-After` | Absent | **Keep (PROPOSED)**: in-memory buckets. Per-IP limit on login endpoints (brute force) and a per-tenant request limit. In-memory buckets are per instance; document it. Per-tenant plan/rate-limit columns existed in the blueprint and are not in the current `Tenant`; use a configured default and decide in Q5 whether to add a column. |
| **Docker**: Compose (PostgreSQL + Redis), multi-stage Dockerfile, prod compose | Absent | **Keep (PROPOSED)**: Compose with PostgreSQL (and app), multi-stage Dockerfile, env-var injection, `.env.example`. |
| **Railway deployment** with live URL | Absent; conflicts with the non-goal "no cloud dependency" | **Ask (Q1).** Recommended: deploy a demo in the final phase. A live URL is valuable for a highlight project. Check the current free-tier/trial terms of the chosen host. |
| **Spring Actuator** (`/health`, `/metrics`, custom metrics) | Absent | **Keep (PROPOSED)**: public `/actuator/health` only; everything else authenticated/platform-only. Custom metrics: active tenants gauge, audit events counter. Do **not** tag metrics with tenant id (cardinality). |
| **`@Async` audit logging** | Replaced by global audit design | Synchronous/same-transaction first (9.6). |
| **JWT `jti` blacklist in Redis on logout** | Not present | Replaced by refresh-token revocation + `tokens_valid_after` (8.8). |
| **3-level hierarchy** `SUPER_ADMIN > ADMIN > USER` | Replaced | Platform identity (`SUPER_ADMIN`) + tenant roles composed of global permissions. No hierarchy. |
| **Public `POST /api/tenants`, public `/auth/register`** | Replaced | Platform-provisioned tenant creation; users created by tenant admins. |
| **Tenant id from header or JWT** | Replaced | Token claim only; slug used at login (8.4). |
| **Flyway 9 / Spring Boot 3.2 / Spring Security "3.2.x"** | Outdated | Use the versions in `pom.xml` (Boot 4.1.1 etc.). |
| **Postman collection, README with architecture diagram, 2-min demo video, tagged `v1.0.0`** | Roadmap Phase 10 | **Keep.** |
| **JUnit + Mockito only** | Replaced | Add Testcontainers. |

### 10.1 Blueprint flaws the review found (do not reproduce them)

1. `CREATE SCHEMA tenant_{name}` / `SET search_path` built by string concatenation (SQL injection).
2. `search_path` not reset on connection release (pool leaks tenants).
3. Public entities queried through the tenant-routed session without a plan.
4. `@Async` audit writes losing `TenantContext`/`SecurityContext`.
5. Tenant taken from a client header; no way to identify the tenant at login.
6. SUPER_ADMIN stored in tenant schemas; matrix contradicted the endpoint list (public tenant registration vs SUPER_ADMIN-only; ADMIN "view own tenant config" vs SUPER_ADMIN-only endpoint).
7. Public self-registration with unspecified role (privilege escalation risk).
8. Roles stored in the JWT with no refresh or revocation strategy (stale roles).
9. "Tamper-evident" claim with a plain table.
10. In-memory buckets presented as distributed rate limiting; tenant-only limits leave login open to brute force.
11. Non-atomic tenant onboarding leaving orphan schemas.
12. Duplicate `V1__` migration names for public and tenant scripts.
13. "10,000 tenants is fine" and "GDPR is just `DROP SCHEMA`" overclaims: thousands of schemas work, but 10k+ strains catalog size, backups and migration fan-out; audit rows live in `public` and must be handled for export/erasure.

---

## 11. Security rules

- BCrypt hashes only; strength 10-12; password policy minimum 10 characters; BCrypt truncates at 72 bytes, so enforce a maximum of 72 bytes.
- Generic login errors (no user enumeration) and a dummy hash comparison when the user does not exist.
- Rate limit login per IP and per account; audit failures.
- Authorization is authenticated identity + explicitly granted permissions. Method security: `@PreAuthorize("hasAuthority('USER_READ')")` style. Platform operations require authority `ROLE_SUPER_ADMIN`.
- **Privilege escalation rules:** a tenant user may only assign roles whose permissions are a subset of their own; system roles are immutable; a user cannot delete/disable themselves; the last holder of `TENANT_ADMIN` cannot be removed or demoted.
- **IDOR:** tenant-scoped endpoints derive the tenant only from the token; cross-tenant access exists only under `/platform/tenants/{tenantId}/...` and is audited.
- Tenant context is never trusted from a client-supplied tenant id or schema name. A tenant user must not be able to switch schema by changing request data.
- `TenantContext` is set after JWT validation and cleared in a `finally` block on every request.
- Secrets from environment variables; no secrets in logs; error responses never leak SQL or stack traces.

---

## 12. API surface (PROPOSED; adjust in Phase 8 contracts)

Standard error body: `{ timestamp, status, code, message, path, fieldErrors[] }`, produced by a single `@RestControllerAdvice`. Use DTOs with bean validation; never expose entities.

Implemented in Phase 2: `common.api.ApiError`, `ErrorCode` (the code list and statuses), `ApiException`, `GlobalExceptionHandler`, and `ErrorResponseWriter` for filter-level errors. Conventions: `.claude/rules/api-conventions.md`.

**Platform (`/platform/**`, SUPER_ADMIN)**

| Method | Path | Purpose |
|---|---|---|
| POST | `/platform/auth/login` | Platform login |
| POST | `/auth/refresh` | Rotate refresh token (both realms) |
| POST | `/platform/auth/logout` | Revoke refresh family |
| POST | `/platform/tenants` | Provision tenant (name, slug, adminEmail, initialPassword) |
| GET | `/platform/tenants`, `/platform/tenants/{tenantId}` | List/details (paginated) |
| PATCH | `/platform/tenants/{tenantId}` | Update name/status (suspend/activate) |
| POST | `/platform/tenants/{tenantId}/retry-provisioning` | Idempotent retry |
| GET | `/platform/permissions` | List global permissions |
| GET | `/platform/audit?tenantId=&from=&to=&page=&size=` | Paginated, date-filtered audit |
| ANY | `/platform/tenants/{tenantId}/users/**` | Explicit cross-tenant user administration (later phase) |

**Tenant (`/t/**`)**

| Method | Path | Permission |
|---|---|---|
| POST | `/t/{tenantSlug}/auth/login` | public |
| POST | `/t/auth/logout` | authenticated |
| GET, PUT | `/t/me` | authenticated (own profile) |
| GET | `/t/users`, `/t/users/{id}` | `USER_READ` |
| POST | `/t/users` | `USER_CREATE` |
| PUT | `/t/users/{id}/roles` | `ROLE_ASSIGN` |
| DELETE | `/t/users/{id}` | `USER_DELETE` (audited) |
| GET | `/t/roles` | `ROLE_READ` |
| POST, PUT, DELETE | `/t/roles`, `/t/roles/{id}` | `ROLE_MANAGE` |
| GET | `/t/permissions` | `ROLE_READ` (available global permissions) |
| GET | `/t/tenant` | `TENANT_READ` (own tenant config) |
| GET | `/t/audit` | `AUDIT_READ` (optional) |

Public: `GET /actuator/health`.

### 12.1 Permission matrix (adapted from the blueprint)

| Capability | TENANT_USER | TENANT_ADMIN | Platform SUPER_ADMIN |
|---|---|---|---|
| Read/update own profile | yes | yes | n/a (platform realm) |
| Read/create/delete users in tenant | no | yes | yes (explicit tenant path) |
| Assign/revoke roles | no | yes (within escalation rules) | yes |
| Create/manage roles | no | yes | yes |
| View own tenant config | no | yes | yes |
| Modify tenant config/status | no | no | yes |
| Provision tenants | no | no | yes |
| View global audit log | no | no | yes |
| Cross-tenant access | no | no | yes (audited) |

---

## 13. Package structure and conventions (PROPOSED)

Base package `com.pritam.saasbackend`. Modular monolith organized by capability, each with `domain / application / persistence / web` separation where it earns its place:

`common` (errors, DTO helpers, validators) · `tenant` (registry, provisioning, context, Hibernate SPI) · `platformuser` · `permission` · `tenantuser` (users, in tenant schema) · `role` · `audit` · `security` (`jwt`, filter chains, filters, refresh tokens) · `ratelimit` · `config`.

Optional: ArchUnit tests enforcing module boundaries. Migrations: `db/migration/public/` and `db/migration/tenant/`.

---

## 14. Roadmap (revised order, one phase at a time)

Changes from the design doc's roadmap: DTO/exception handling and the audit service are pulled forward (earlier controllers need them), and the tenant-isolation proof moves right after tenant-context resolution.

| Phase | Goal | Acceptance criteria |
|---|---|---|
| 0 | Reconcile this doc with the repo; verify versions from `pom.xml`; list discrepancies | Written discrepancy list; section 2 corrected |
| 1 | Fix Tenant entity ↔ Flyway mismatch; `ddl-auto=validate`; `open-in-view=false`; Testcontainers | **10/10 tests green**, empty-DB migration test passes |
| 2 | Cross-cutting foundation: error model + `@RestControllerAdvice`, DTO/validation conventions, `AuditService` interface (sync), `common` validators | Standard error body on all failures; audit entry written in a test |
| 3 | Platform user model finalized (migration), platform login, finalized JWT claims, platform filter chain, refresh tokens + `tokens_valid_after` | SUPER_ADMIN can log in/refresh/logout; tenant-type token rejected on `/platform/**`; reuse detection tested |
| 4 | Tenant provisioning: schema naming/validator, `PROVISIONING` state machine, tenant Flyway location, permission seed `R__permissions.sql` (default roles reference `public.permissions(code)` by FK), minimal tenant `users`/`roles`/`user_roles`/`role_permissions` tables and entities, seeding default roles + first admin, `POST /platform/tenants`, startup tenant migration loop | Tenant created end-to-end; forced failure leaves `PROVISIONING_FAILED` and retry succeeds; injection attempts rejected |
| 5 | Tenant context + Hibernate multi-tenancy (resolver, connection provider with `search_path` reset, public-entity handling), tenant login by slug, tenant filter chain, status enforcement. **Then the isolation proof**: two tenants, same-id users, parallel requests on a pool of 2 | Isolation + pool-leak tests green; `TenantContext` cleared on every request |
| 6 | Tenant user and role management on the Phase 4 tables, role-permission mapping logic, RBAC enforcement, permission cache, escalation rules | Multiple roles per user work; privilege-escalation and last-admin cases rejected; authority/seed consistency test |
| 7 | Audit completion: full event catalog, append-only trigger, `/platform/audit` with pagination/date filter (and optional `/t/audit`), optional hash chain | Every sensitive action audited; UPDATE/DELETE on audit rejected |
| 8 | Rate limiting (Bucket4j, per-IP login, per-tenant), Actuator (health public, metrics protected), API contract polish and OpenAPI docs (optional) | 429 with `Retry-After`; health reachable, metrics protected |
| 9 | Security test expansion: role/permission matrix tests, cross-tenant negative tests, JWT tampering/expiry/audience tests | All matrix cells covered by tests |
| 10 | Docker (multi-stage Dockerfile, Compose, `.env.example`), optional CI (GitHub Actions running `verify`), deployment (Q1), README (architecture diagram, multi-tenancy strategy comparison, permission matrix, security notes, limitations), Postman collection, demo video, tag `v1.0.0` | Fresh clone runs with one Compose command; README complete |

### 14.1 Testing strategy

- Unit tests (Mockito) for pure logic only.
- Integration tests on Testcontainers PostgreSQL for repositories, Flyway (public and tenant), provisioning, multi-tenancy, security.
- Isolation suite: same user id in two tenants; cross-tenant read attempts; parallel requests on a tiny pool; context cleared after exceptions.
- Security suite: every cell of the permission matrix, missing/expired/tampered tokens, wrong audience, suspended tenant, revoked refresh tokens.
- Migration suite: empty DB to head; tenant migration applied to two schemas; failing migration marks only that tenant.

---

## 15. Definition of done (project level, LOCKED from the design doc plus PROPOSED additions)

- A tenant can be created and receives a unique, controlled PostgreSQL schema; the registry accurately records it.
- SUPER_ADMIN can authenticate and perform authorized cross-tenant administration.
- Tenant users can authenticate only within their owning tenant.
- A user can have multiple roles; roles resolve to globally defined permissions.
- Cross-tenant access is rejected when unauthorized.
- Passwords are BCrypt-hashed; JWT authentication and authorization work end to end.
- Important administrative/security actions are written to `public.audit_log`.
- Flyway can reproduce the schema from an empty database.
- Integration tests cover persistence, service, security and tenant-isolation rules.
- The architecture and its trade-offs can be explained in a technical interview.
- **PROPOSED additions:** Docker Compose one-command startup; README with diagram and permission matrix; demo; tagged release.

### 15.1 Resume claim rules

Only claim what is built and tested. In particular: say "append-only audit log" unless a hash chain exists; do not claim Redis, distributed rate limiting or "zero risk" of cross-tenant leaks; say "reduced". Add concrete numbers once they exist (test counts, isolation scenarios, a load-test p95 figure).

### 15.2 Interview talking points to document in the README

Why schema-per-tenant (and the trade-offs vs. database-per-tenant and shared-schema `tenant_id`); the pool/`search_path` hazard and how it is tested; why tokens carry `tid` but not permissions; the revocation window and refresh-token rotation; the cross-schema FK trade-off; scaling limits (thousands of schemas fine, 10k+ strains catalog, backups, migration fan-out); single-instance caches.

---

## 16. Progress checklist (agent: update after each phase)

- [x] Phase 0: reconcile doc with repo (discrepancies in 6 and 6.3; section 2 corrected)
- [x] Phase 1: green baseline (15/15: original 10 + empty-DB schema test), validate mode, Testcontainers
- [x] Phase 2: error model, DTO conventions, audit interface (74/74 tests)
- [ ] Phase 3: platform auth, JWT claims, refresh tokens
- [ ] Phase 4: tenant provisioning and tenant migrations
- [ ] Phase 5: tenant context, Hibernate multi-tenancy, isolation proof
- [ ] Phase 6: tenant users, roles, permissions, RBAC
- [ ] Phase 7: audit completion
- [ ] Phase 8: rate limiting, Actuator
- [ ] Phase 9: security test expansion
- [ ] Phase 10: Docker, deploy, README, demo, release

---

## 17. Open questions (agent must ask the owner; record answers here)

| # | Question | Default if the owner says "your call" |
|---|---|---|
| Q1 | Deploy a public demo (Railway or another host), or keep it local-only? | Deploy in Phase 10 |
| Q2 | Platform user login identity: email or username? | Email |
| Q3 | Tenant login identifier: slug in path (`/t/{slug}/auth/login`) or subdomain? | Slug in path |
| Q4 | First tenant admin credentials: supplied in the provisioning request, or generated one-time password? | Supplied in request |
| Q5 | Add `plan` / `rate_limit` columns to `tenants` (as in the blueprint), or a global default limit? | Global default |
| Q6 | Token lifetimes (access 15 min / refresh 7 days) acceptable? | Yes |
| Q7 | Add Redis later for multi-instance caches, or stay single-instance? | Stay single-instance |
| Q8 | Implement the audit hash chain (true tamper-evidence), or only append-only? | Append-only first, hash chain optional |
| Q9 | Build tenant-level audit viewing (`/t/audit`) or platform-only? | Platform-only first |

---

## 18. Kickoff prompt (paste into Claude Code)

```
Read docs/PROJECT_SPEC.md completely. Then inspect the repository (pom.xml, db/migration,
packages, tests) and give me a short discrepancy list between the spec and the repo (Phase 0).
Do not change code yet. After I confirm, execute Phase 1 only: reconcile public.tenants with the
Tenant entity via a new Flyway migration, switch to ddl-auto=validate and open-in-view=false,
add Testcontainers PostgreSQL, and get all tests green. Follow the operating rules in section 0.
Stop at the end of the phase, update the checklist in section 16, and summarize what changed.
```
