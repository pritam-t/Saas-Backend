---
paths:
  - "src/main/java/**/api/**"
  - "src/main/java/**/application/**"
  - "src/main/java/**/common/**"
---

# API, DTO and error conventions (Phase 2)

## DTOs
- Request and response DTOs are `record`s in `<module>.api.dto`, named `XxxRequest` / `XxxResponse`. Never return entities.
- Controllers take `@Valid @RequestBody`. Put constraints on the record components (`@NotBlank`, `@Email`, `@ValidSlug`, `@ValidPassword`, ...).
- Shared validators live in `common.validation`. They treat `null` as valid; add `@NotNull`/`@NotBlank` explicitly when a field is required.
- Passwords: `@ValidPassword` (10+ characters, max 72 UTF-8 bytes). Never `@Size` for passwords.

## Errors
- Every failure returns `ApiError`: `{ timestamp, status, code, message, path, fieldErrors[] }`. `fieldErrors` is always an array.
- Expected failures: `throw new ApiException(ErrorCode.X, "safe message")`. Add a new `ErrorCode` constant (with its status) when a phase needs a domain code; do not create exception subclasses per case.
- `GlobalExceptionHandler` is the only `@RestControllerAdvice`. Do not add `@ExceptionHandler` methods in controllers.
- Messages are client-facing: never include exception text, SQL, constraint or schema names, or stack traces. Log details server-side.
- Errors raised in filters (before MVC) must be written with `ErrorResponseWriter`, never with hand-built JSON.
- Every new error path gets a test asserting the full body shape (see `ErrorModelIntegrationTest.assertErrorBody`).

## Audit
- Sensitive actions call `AuditService.record(...)` inside the action's transaction.
- Events about failures that roll back (failed login, denied access) use `AuditService.recordIndependently(...)`.
- Leave `AuditEvent.ip` null inside HTTP requests; the service takes the remote address. Never trust `X-Forwarded-For` until proxy handling is configured.
