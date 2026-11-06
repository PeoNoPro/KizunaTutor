# FE-10 migration and recovery

Use PostgreSQL client utilities matching the server. Never run the destructive baseline script.

1. Back up the intended database with `pg_dump -Fc -f fe10-before.dump <connection options>`. Keep backups outside the repository; they may contain private messages and files.
2. Restore into a newly created, explicitly isolated database using `pg_restore --exit-on-error --no-owner --dbname=<isolated database> fe10-before.dump`. Verify table counts before changing the real database.
3. Run `psql <connection options> -v ON_ERROR_STOP=1 -f database/seeds/fe10_support_ticket.sql`. The script uses a transaction advisory lock and a version record. It is additive and safe to rerun.
4. Stop if preflight reports orphan messages, external legacy attachment URLs, or legacy escalation without trustworthy actor/decision history. Reconcile from verified records; never invent authors or decisions. No legacy URL is served by the new API.
5. Verify message counts, sequence uniqueness and version row. Start the backend only after the migration succeeds.

Recovery: stop writes, restore the pre-migration backup into a separate database, verify counts/content and point the application at that verified database. Do not drop or overwrite the working database as a rollback shortcut. Binary attachments increase backup size; budget up to 50 MB per submitted message plus database/backup overhead.

Verification on this workstation: PostgreSQL 18.6 schema-only backup of the development database restored successfully into `kizuna_support_ticket_test` (no user data copied). A subsequent custom-format backup containing only test fixtures restored successfully into `kizuna_restore_ticket_test`: 18 tickets, 30 messages, 2 attachments (146 bytes), 66 audit events at that checkpoint. Backup artifacts stay outside the repository. Credentials belong in environment variables, never in this document.
