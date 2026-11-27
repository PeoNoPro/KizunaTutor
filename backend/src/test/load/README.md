# FE-10 load acceptance

Node 24 and Chrome remote debugging are used; no new runtime dependency. Run only against an independently started backend on localhost:8090, pointing to a database ending `_ticket_test`. Never point it to a development/shared database.

1. Apply the additive migration, then `prepare-support-tickets.sql` with psql `-v ON_ERROR_STOP=1`. The fixture refuses databases without the test suffix. It creates 100 learners, 1,000 tickets, and 2,000 initial public messages; no operational fixtures are inserted.
2. Set `TICKET_LOAD_SIGNING_KEY` to the isolated server key and run `node src/test/load/write-test-tokens.js` from backend. This test-only utility writes 100 distinct sessions to ignored `target/support-load-tokens.json`. Set `TICKET_LOAD_TOKENS_FILE` to that file. Never commit tokens/signing keys or enable test token issuance in application runtime.
3. Start Chrome headless with a separate test profile, remote debugging port 9340, and `--disable-background-timer-throttling --disable-renderer-backgrounding --disable-backgrounding-occluded-windows` so inactive test targets can render. Provision memory/CPU for 100 browser contexts; do not reuse the user's regular browser profile.
4. Set `TICKET_TEST_DB_URL`, `TICKET_LOAD_API=http://localhost:8090`, `TICKET_LOAD_TOKENS_FILE`, optionally `TICKET_LOAD_CDP` and `TICKET_LOAD_OUTPUT`; from backend run `node src/test/load/support-tickets.js`.

The script creates 100 isolated browser contexts, warms all sessions, then keeps all 100 active for ten minutes. It measures list/detail separately from navigation through completed render (including two animation frames). HTTP timing is reported separately. Every fifth round posts a real owner reply; API/UI failures count as errors, never as successful samples. It exits unsuccessfully if errors occur or either browser p95 exceeds two seconds.

Afterward compare `successfulMutations` with the increase in MESSAGE_SENT audit count, message count and ticket versions for fixture owners `fe10-load-%@test.invalid`. Inspect EXPLAIN for the actual scoped list/detail queries and confirm metadata queries do not select content. Archive results outside the repository; report browser timing separately from HTTP.

Status: script prepared and syntax checked. The 100-browser/ten-minute run and mutation/audit reconciliation have not been executed; SC-005 is not accepted. Ten-person usability acceptance is a separate check.
