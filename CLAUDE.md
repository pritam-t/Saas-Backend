# saas-backend — Claude Code instructions

Multi-tenant SaaS backend (Spring Boot, PostgreSQL, schema-per-tenant). This is the owner's
highlight resume project: correctness, security and explainability matter more than feature count.

**Source of truth for intent:** `docs/PROJECT_SPEC.md`. Do NOT read it all at once.
At the start of a phase, read only: section 0 (rules), the phase row in section 14,
and the sections that phase touches. The repo is the truth about what exists; the spec is the truth about intent.

## Stack (verify in Phase 0)
Java 25, Spring Boot 4.1.x, Spring Data JPA / Hibernate 7, PostgreSQL 18, Flyway, Spring Security + JWT, BCrypt, Maven,
JUnit + Testcontainers (PostgreSQL). Planned: Caffeine, Bucket4j, Docker Compose. Not planned: Redis, OAuth, microservices.

## Commands
- Full build + tests: `./mvnw verify`
- One test class: `./mvnw -Dtest=ClassName test`
- Run app: `./mvnw spring-boot:run` (needs env vars, see `.env.example`)
- Integration tests use Testcontainers, so Docker must be running.

## Package layout
Base package `com.pritam.saasbackend`, organised by capability:
`common`, `tenant`, `platformuser`, `permission`, `tenantuser`, `role`, `audit`, `security` (`jwt`, chains, filters, refresh tokens), `ratelimit`, `config`.
`auth` is **temporary** (pre-spec platform login code); it is folded into platform auth in Phase 3 (spec 6.3).
Migrations: `src/main/resources/db/migration/public/` and `.../tenant/` (separate locations; never reuse a version number across them).

## Hard rules (never break; ask if blocked)
1. Flyway is the only schema authority. Never `ddl-auto=create|update|create-drop`. Never edit an applied migration; add a new one.
2. Never trust a tenant id or schema name from the client. Never concatenate untrusted text into SQL identifiers.
3. Tenant comes from the JWT `tid` claim only (platform cross-tenant access only via `/platform/tenants/{tenantId}/...`, audited).
4. No secrets in the repo or logs. Env vars only; keep `.env.example` current.
5. Integration tests run against real PostgreSQL (Testcontainers), never H2. Do not mock Flyway, `search_path` or security.
6. No new library without a real problem it solves. Say why in the commit message.
7. No abstraction unless it solves a real architectural problem.
8. Do not decide OPEN items (spec section 17) silently. Propose options, ask, then record the answer in the spec.
9. Do not use Lombok `@Data` / `@EqualsAndHashCode` on JPA entities.

## Workflow
- One phase at a time. Do not start the next phase until the current phase's acceptance criteria pass.
- Start every phase in **plan mode**: inspect the repo, then give a plan (files touched, tests first, risks). Wait for the owner's approval before editing.
- Tests first for anything touching tenancy, security or migrations.
- End of phase: `./mvnw verify` green, tick the checklist in spec section 16, summarise what changed and what the owner should be able to explain, then stop.
- Commit style: `feat:`, `fix:`, `test:`, `docs:`, `refactor:`. One logical change per commit.
- If this file, the spec, and the repo disagree, stop and report the disagreement instead of picking one.

## Learning mode (this is a learning project)
The owner must be able to explain every core piece in an interview. For these, do NOT write the full implementation unprompted:
connection provider and `search_path` reset, tenant resolver, provisioning state machine, refresh-token rotation and reuse detection, privilege-escalation rules.
Instead: explain the design and trade-offs, propose a skeleton, and let the owner write or approve it step by step. Boilerplate (DTOs, controllers, most tests) can be written directly.
After each phase, add 3-5 lines to `docs/LEARNING_LOG.md` (what was built, why, trade-off) if the owner asks.

## Resume honesty
Only claim what is built and tested (spec 15.1). "Append-only audit log", not "tamper-evident", unless a hash chain exists.