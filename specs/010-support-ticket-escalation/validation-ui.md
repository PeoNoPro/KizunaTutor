# FE-10 browser verification

## Figma redesign — file YDYN9ibpq0SZO1HzG5y0Is, page 1109:2 (bộ MỚI)

Frames: 1457:7176, 7291, 7707, 7737 (người gửi); 1457:7418, 7805, 7827, 7865 (CS); 1457:7588, 7847 (quản lý). This section supersedes the retro DESIGN section 7 styling and older layout notes below.

Kept from project rules rather than Figma: shared header/footer and `kizuna-logo.png` (FRONTEND_RULES 3–4), sentence-case table headings and sidebar captions, Vietnamese role labels from the mapping table, spec status labels instead of the sample “Đang hỗ trợ”/“Chờ quyết định”, no HT-xxx codes because the API has none. Figma frame 1457:7418 shows the academic-officer menu, so the existing CS shell was kept. Role navigation lists only pages that exist. Manager keeps the existing shared admin shell, and FR-011's separate save-decision and return actions remain two buttons.

`frontend/tests/support-tickets.browser.mjs` passed for four roles against an isolated backend on port 8090 and the test database; `cs-ticket-detail.browser.mjs` and six transport tests passed. `cs-dashboard.browser.mjs` was not re-run.

## CS detail usability refinement — frontend only

Scope: `frontend/js/pages/cs-ticket-detail.js`, scoped `frontend/styles/cs-ticket-detail.css`, direct integration in `support-tickets.js`, and the CS navigation in the existing shared shell. Manager/requester presentation remains on the existing path.

- Desktop conversation/sidebar ratio verified near 70/30 at 1440px. No horizontal overflow at 768px, 390px and 320px. Actual header/logo/sidebar structure retained; no new UI library or inline styles.
- One title/summary shows the requester name and role from the API, creation date and status badge; the ticket UUID is not rendered (FR-016). The sidebar has request information and resolution. Priority/assignment/escalation controls are rendered only for the server's allowedActions.
- Public thread uses safe text nodes, initials, timestamps and wrapping attachment cards. Composer uses “Phản hồi cho người gửi”, a Vietnamese attach button, removable selected files, five-file/10,000,000-byte limits and compact navy send controls.
- Browser assertions cover claim, priority, file selection/removal/limits, failed send with preserved draft/files, duplicate submit prevention, successful UI refresh, internal-only reason/summary payload, history disclosure, escalation restrictions, resolution and closed read-only state.
- Resolve behavior was checked by reading the existing command executor: it adds a public conclusion and transitions to RESOLVED. CS label is therefore “Gửi kết quả và đánh dấu đã giải quyết”. No behavior/API change.
- “Hỗ trợ học thử” and “Hỗ trợ kết nối” were verified as category-filter URLs, not separate processes. Their list filters and dashboard shortcuts remain; duplicate sidebar entries removed. Existing ESCALATED list now reads “Đã chuyển quản lý”.

**Verification:** `node --test --test-isolation=none frontend/tests/operations-api.test.mjs frontend/tests/support-api.test.mjs` passed six tests. `node frontend/tests/cs-ticket-detail.browser.mjs` passed against the frontend with all API requests intercepted by test-only fixtures; no backend or database was started, queried or mutated. Syntax checks and `git diff --check` passed. No schema change, commit or push. Actual database-backed transactions were not rerun in this refinement, per user instruction.

**API update:** staff detail now returns requesterDisplayName/requesterRoleLabel; each message returns senderRoleLabel and fromRequester (derived from the immutable sender ID, which is not exposed). The role label is the sender's current DB role, not a snapshot from send time. No name-based guesses are used.

Screenshots from UI-only fixtures: `D:\do an 2\.fe10-qa\cs-detail-refined-desktop.png` and `cs-detail-refined-mobile.png`.

## CS dashboard — Figma 1113:39

Implemented at `/cs/dashboard` (static page `frontend/pages/cs-dashboard/`), using the exact Figma logo/grid/chat assets under `frontend/assets/cs-dashboard`. Shared internal shell gains a CS variation: 232px white sidebar, beige background, role navigation, current authenticated identity and the Figma two-column queue/manager-feedback layout. Existing CS list/detail reuse this shell; other roles retain their existing shell.

`frontend/tests/cs-dashboard.browser.mjs` passed against the real isolated backend at 1440×1120 and 390px widths: no horizontal overflow, real category counts/filtering, full-queue navigation with matching filters, manager result deep link to the internal tab, and requester role denied. Every visible sidebar asset loaded successfully; logo geometry is 64×64 and all SVGs preserve their native 20×20 size. Screenshots: `.fe10-qa/cs-dashboard-1440.png` and `cs-dashboard-390.png` outside the repository.

Sample Figma names/counts/ticket codes are replaced with actual identity/data. No made-up HT codes, WAITING_USER state or sample records are introduced. Empty feedback/queue use empty states. Role caption uses sentence case per project rules. Notifications remain unavailable until FE-12 is connected.

Full backend regression passed 75 tests, including the CS dashboard API contract/authorization test. Six frontend transport tests and the existing support browser workflow also passed. Source screen and shared assets were visually compared; original load/human acceptance remains open.

## User-approved detail layout refinement

- CS and Manager initially see “Trao đổi với người gửi”. “Xử lý nội bộ” contains current handling information and internal controls; requesters receive no internal tab/data.
- History is closed by default. Opening the internal tab alone makes no history API request; expanding history fetches the first page. An isolated fixture with 24 real audit events verifies 20 rows on page one, next-page navigation and collapsing again.
- Arrow-key navigation changes tab/focus; switching away and back retains draft internal notes. The selected internal tab stays selected after saving a decision.
- Updated browser workflow passed against the real isolated backend: decision → return → CS public resolution → owner closure, plus the existing role/list/create checks.
- Checked 1366px desktop and 390px mobile, no horizontal overflow. New screenshots outside the repository include `cs-internal-1366.png`, `manager-internal-390.png` and role history screenshots. Six existing frontend transport tests also passed. No backend/API changes were needed.

Executed against a real isolated backend/database using `frontend/tests/support-tickets.browser.mjs`; API configuration alone is switched in the test tab, no domain response fixtures intercepted.

- Learner, tutor, CS and manager list pages rendered at 1366×900 and 390×900 with no horizontal document overflow.
- Learner/tutor use the existing shared header/footer and DESIGN section 7: 5px corners, black borders, hard shadows, Itim headings, role color. CS/manager reuse the existing admin/internal shell and authenticated account name.
- Create modal focuses subject, Escape closes it and restores focus to the opener. A real submitted form navigates to its new ticket and reloads persisted content.
- Manager has private decision/return controls and no public reply form. Owner receives no internal-history section or data. CS controls follow current assignee/state.
- Browser completed decision → return → resolution → owner confirmation. Empty, loading and errors remain distinct; wrong-role route is refused. API failure preserves draft text; explicit refresh preserves text and explains file reselection.
- User-entered `<script>` content appeared as text and did not execute. System messages/statuses use Vietnamese labels, not raw enums.
- Attachment links show name, readable type and size; uploader is shown by the public message author. Internal history shows readable before/after changes.

Screenshots: `D:\do an 2\.fe10-qa\learner-detail-390.png`, `cs-detail-1366.png`, role list/detail screenshots and learner-create-390.png. Fixtures contain test identities only. No Figma frame for FE-10 was supplied; the existing project design rules and shared components were followed.
