# FE-10 implementation validation

Date: 2026-11-02. Feature: Support Ticket & Escalation. Working branch retained; no commit/push. Requirement checklist passed 16/16 and was not modified. No before/after extension hooks registered. Read-only analysis and architecture adaptations are recorded in implementation-analysis.md.

## Delivered behavior

- Learner/tutor: create, scoped list/detail, public thread, optional private attachments/context, supplement a resolved request to reopen it, confirm closure.
- CS: scoped queue filters/pagination, active count, claim, reassign, priority, public replies/files, resolution, manager escalation and recipient replacement.
- Manager: current-recipient list/detail/history/download only, private decision and separate return action. Recipient access persists after return until replacement.
- Every command checks current DB account/role, ticket lock and expectedVersion, and commits message/file/state/audit together. Closed requests reject writes. Public responses omit internal fields entirely.
- Internal history stores before/after snapshots and presents readable changes, actor, time and notes. Metadata queries exclude binary, and history metadata never goes into requester responses.
- Related business records are only read. The creation audit retains original context IDs so external SET NULL deletion can be displayed as unavailable without losing the ticket or inventing context data.

## Executed checks

| Check | Evidence/result |
| --- | --- |
| Complete Maven regression | 74 tests, zero failures/errors/skips; 25 FE-10 tests plus existing database, tutor search and FE-11 checks. Both data sources explicitly point to isolated test DBs. |
| Browser | Four role lists; requester/CS/manager detail; 1366px and 390px; real form create; manager decision/return, CS resolution and owner close; Escape/focus restoration; escaped content and wrong-role route refusal. No business response mocking. |
| Transport | Six Node tests passed, including existing FE-11 transport and FE-10 multipart/CSRF. |
| Real HTTP multipart | Valid private upload/download matched bytes; metadata had no content; nosniff/no-store headers; >52 MB request returned 413 and created no partial ticket. |
| Migration | Existing messages retained and sequenced; repeat application safe; orphan, legacy URL and unverifiable escalation rejected. Applied to test DB first. |
| Atomicity/concurrency | Double claim; competing messages; close vs reply; reassignment vs old assignee reply; decision vs recipient replacement; recipient account deactivation; forced final audit failure rolls back message and attachment. |
| Files | JPG/PNG/PDF; five valid files; six rejected; exact 10,000,000-byte valid PDF; over-limit file; exact 20,000,000-pixel image; larger image rejected; 100-page PDF accepted, 101 rejected; encrypted/corrupt/mismatched MIME rejected. |
| Isolation | Native-memory probe confirms whole-process OS limit independent of heap; timeout/crash rejected; missing worker fails closed and temporary files are cleaned. |
| Backup/restore | Schema-only restore without personal data, then full populated test backup restored to a second test DB. Checkpoint: 18 tickets / 30 messages / 2 attachments / 66 audit events. |
| Local run | Launcher verified against real local development accounts, after backup. localhost:8085 is the CS development session; frontend localhost:3000. No support fixtures inserted into development DB. |

Artifacts are outside the repository at `D:\do an 2\.fe10-qa`: acceptance-regression.log, browser-results.json, role screenshots, servlet test output, backup/restore artifacts and explain-final.txt. Screenshots use isolated test fixtures only.

The test harness configurations use TestConfiguration so generic SpringBoot tests do not accidentally discover a second datasource. Existing operations harness behavior remains explicit and isolated.

## Requirement traceability

| Requirements | Automated evidence |
| --- | --- |
| FR-001–004 | SupportTicketWorkflowTest: create/list/detail, owner isolation, Unicode boundary, pagination/filter validation; role browser lists/forms. |
| FR-005–008 | Workflow handling/reopen/closure; SupportTicketPolicyTest; claim/assignment/reply/close concurrency. |
| FR-009–013,019 | Escalation workflow, decision/return separation, reset on recipient replacement, old manager detail/download revoked, history and owner secrecy; concurrency and account deactivation. |
| FR-014–015 | Real ownership/context fixtures, matching plan checks, foreign context rejected, deleted context unavailable, source business statuses unchanged. |
| FR-016–018 | Browser labels/empty/error/escaped content; every route authentication and write CSRF; DB role revocation, optimistic conflicts and rollback. |
| FR-020–021 | Attachment, parser isolation, requester/CS file workflow and real servlet upload/download tests. |

Functional workflows have automated evidence. This is not a claim that all success criteria have received final human acceptance: SC-002/003/004/006/007 have automated coverage, while final acceptance across all agreed scenarios remains part of T057.

## Remaining acceptance — deliberately open

- **T055 / SC-005:** 100 real browser sessions for ten minutes has not run. A runnable harness and isolated 1,000-ticket/2,000-message fixture exist. SQL execution timing is diagnostic, not browser p95. This workstation had approximately 3 GiB free of 16 GiB during verification; no 100-context run is claimed.
- **T057 / SC-001:** ten human participants have not been available; the 9/10 people within three minutes criterion is not accepted by substituting automation.

55/57 tasks complete. Both remaining markers stay unchecked. PostgreSQL 18.6 and Windows isolation were verified; PostgreSQL 16 and other OS isolation were not independently verified. Unsupported parser isolation rejects uploads, while text-only requests remain available.
