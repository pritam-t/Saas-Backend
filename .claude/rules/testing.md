---
paths:
  - "src/test/**"
---

# Testing rules

- Integration tests use Testcontainers PostgreSQL (version matching production). No H2. Verify the exact Spring Boot 4 Testcontainers annotation and package names against current docs.
- Each run starts from an empty database; Flyway builds the schema.
- Security tests use real JWTs through the real filter chains. `@WithMockUser` alone is not enough.
- Required suites over time: Flyway empty-to-head; tenant migration on two schemas; isolation (same user id in two tenants, cross-tenant read attempts, parallel requests on a pool of size 2, context cleared after exceptions); security matrix (every permission-matrix cell, missing/expired/tampered tokens, wrong audience, suspended tenant, revoked refresh tokens).
- A test asserts every `@PreAuthorize` authority exists in `public.permissions`.
- Mockito only for pure logic.
- Keep the baseline green: never leave failing or `@Disabled` tests without telling the owner.