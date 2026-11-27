# FE-10 schema preflight

Inspected read-only on 2026-11-02: local dedicated development database, PostgreSQL 18.6. No personal records exported.

- Existing `support_tickets` and `ticket_messages` both contain zero rows. No legacy messages or escalation records to convert in this database; migration tests must still cover populated legacy tables.
- Ticket PK and all requester/assignee/manager/context columns exist with snake_case names. Existing UUID-only JPA stubs incorrectly map camelCase columns and do not represent the schema.
- Native ticket category, priority and status enums already exist. No `version` or `sequence_no` columns exist yet.
- `ticket_messages` has `sent_at`, no `created_at`/`updated_at`; it must not inherit BaseEntity.
- Existing user/message parent FKs use DELETE CASCADE; assignee/manager/context links use DELETE SET NULL. FE-10 does not change unrelated deletion behavior and cannot promise immutable history after external hard deletion.
- Context ownership is available through trial_bookings → learner/tutors, tutor_memberships → tutors, payment_transactions → users/plan_id. Membership payment must have plan_id; tuition transactions are excluded.
- Existing legacy attachment_url is nullable and must never be exposed or fetched. Nonempty legacy attachment URLs or incomplete escalations must block migration for manual reconciliation.

The destructive baseline script was not executed. Only additive, transaction-locked SQL is permitted. Verification and fixture insertion use an explicitly isolated `*_ticket_test` database, never the development database.
