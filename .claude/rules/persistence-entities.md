---
paths:
  - "src/main/java/**/domain/**"
  - "src/main/java/**/persistence/**"
---

# Entity and persistence rules

- No Lombok `@Data` or `@EqualsAndHashCode` on JPA entities. Equality by id, carefully.
- Timestamps are `OffsetDateTime` mapped to `TIMESTAMPTZ`.
- Public entities (`Tenant`, `PlatformUser`, `Permission`, `AuditLog`, `RefreshToken`) declare `@Table(schema = "public")`.
- Tenant-schema entities never carry a `tenant_id` column.
- Entity must match the Flyway schema exactly. `ddl-auto=validate` is the safety net; fix mismatches with a new migration, never by editing an applied one.
- Never expose entities in API responses. Map to DTOs.