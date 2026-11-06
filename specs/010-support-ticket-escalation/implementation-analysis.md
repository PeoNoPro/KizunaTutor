# FE-10 implementation analysis

Date: 2026-11-02. Requirements checklist: 16 checked, 0 unchecked. No extension hooks registered.

| Finding | Severity | Resolution |
| --- | --- | --- |
| Plan/task paths predate the feature package migration | High | Implement under `backend/src/main/java/com/kizuna/features/ticket/{entity,repository,service,dto,controller}`; tests under `backend/src/test/java/com/kizuna/support`. |
| SSR plan predates the static frontend split | High | HTML/CSS/ES modules under `frontend/pages/support-tickets`, `frontend/pages/support-ticket`; role routes served by `frontend/server.js`. Authenticated CSRF endpoint replaces SSR token injection. No change to business permissions. |
| Shared SecurityConfig is in core/security | Medium | Extend the existing protected chain for `/api/v1/tickets`; do not introduce a second conflicting configuration. |
| SQL migration paths predate database/seeds convention | Medium | SQL in `database/seeds/fe10_support_ticket.sql`; operational instructions in `database/migrations/README-fe10.md`. |
| Local PostgreSQL is 18.6, plan targets 16 | Medium | Record actual test version; do not claim PostgreSQL 16 verification. |
| Upload acceptance needs a whole-process OS resource cap | High | Keep uploads fail closed until independent process limits and parser tests pass. JVM heap limits alone are insufficient. |

All 21 functional requirements have task coverage in the existing traceability table. No additional business requirements inferred. Human acceptance and browser load criteria remain open until measured; automated checks cannot substitute for ten participants.

The path adaptations above apply to corresponding existing task IDs, without adding parallel task lists or changing `spec.md`.
