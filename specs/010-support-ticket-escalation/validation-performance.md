# FE-10 performance status

T055 / SC-005 remains unchecked: no 100-browser, ten-minute run has been performed and no browser p95 is claimed.

Prepared and syntax-checked: `backend/src/test/load/support-tickets.js`, test-only SQL dataset and token utility. Dataset applied only to `kizuna_support_ticket_test`: 100 distinct load learners and exactly 1,000 tickets with two initial messages each. Full instructions in the load README.

Diagnostic EXPLAIN on PostgreSQL 18.6 for the real owner-scoped list including staff-name joins: execution 0.282 ms, planning 1.329 ms at this checkpoint. Owner/time index used; no evidence justifies an additional index migration. This is a single SQL measurement, not browser timing or a substitute for SC-005.

Metadata queries explicitly select IDs/names/type/size/time and batch attachments by the current page's message IDs; no content column or per-message metadata round trip. Binary content has its own authorized download query.

Test database size was approximately 13 MB after baseline schema and the load dataset. This is not a capacity estimate for real attachment usage: the per-message file maximum is 50 MB, and binary data increases database and backup size. Populated test backup/restore was verified separately before the large fixture was added.

The workstation had about 3 GiB free memory of 16 GiB during verification. Run the ten-minute browser acceptance on a provisioned test machine, archive separate list/detail render p95 and HTTP timings, compare successful mutations with persisted messages/audit/versions, and only then evaluate SC-005.
