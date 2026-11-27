# Tasks: FE-12 — Notification Management

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/notifications.md, quickstart.md trong `specs/012-notification-management/`; constitution1.0.0.

**Prerequisites**: Năm quyết định clarify đã xác nhận; giữ stack dự án hiện có. Đây là danh sách việc chưa thực hiện.

**Tests**: Spec yêu cầu kiểm chứng FR-001–016/SC-001–007. Viết test hành vi trước implementation tương ứng, xác nhận thất bại đúng nguyên nhân rồi chạy lại; PostgreSQL/mail giả lập chỉ môi trường test, không gửi broadcast thật trong bước tạo tài liệu.

**Organization**: Setup → foundation → US1 inbox → US2 preferences → US3 broadcast/history → US4 urgent-all → tích hợp nguồn/nghiệm thu.

## Format: `[ID] [P?] [Story] Description`

- `[P]` chỉ task khác file có thể làm đồng thời sau đủ tiền đề. File chung controller/service/JS phải tuần tự.
- Đường dẫn tương đối từ repo KizunaTutor; file văn bản UTF-8 không BOM. Không đánh dấu hoàn tất chỉ vì đã tạo tài liệu/test fixture.

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Ghi preflight schema/duplicates, current mapping, nguồn học thử/bài tập/payment/support và SMTP hiện có vào `specs/012-notification-management/implementation-readiness.md`; phân biệt stub với commit point thật, ghi đầu mối hoặc công việc phụ thuộc cho từng nguồn, không ghi secret/PII. Áp dụng retry đã chốt: tối đa4lần gọi gồm lần đầu+3retry sau30/120/300giây chỉ cho known-unsent transient; UNKNOWN không retry.
- [ ] T002 Tạo `src/test/resources/application-notification-test.properties` và `src/test/java/com/kizuna/notifications/NotificationTestEnvironment.java` dùng NOTIFICATION_TEST_DB_URL/USERNAME/PASSWORD, fail thiếu hoặc DB không được đánh dấu test; Clock và mail test gateway cách ly, không gửi tới người thật.
- [ ] T003 [P] Viết `src/main/resources/db/changes/README-fe12.md` với SQL tăng dần/version/lock, backup/restore/preflight và psql ON_ERROR_STOP; không chạy source DROP CASCADE hoặc sửa FE-10/11 migration đã áp dụng.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Dữ liệu và quyền chung, chặn mọi story cho tới khi đủ điều kiện.

- [ ] T004 Viết `src/test/java/com/kizuna/notifications/NotificationMigrationTest.java` sau T001–T002 cho schema cũ, duplicate recipient/source, no fake legacy read_at/email outcomes, bảo toàn dữ liệu và rerun migration an toàn.
- [ ] T005 Tạo `src/main/resources/db/changes/fe12_001_notification_delivery.sql` sau T003–T004: sửa mapping cần thiết và thêm deleted_at/received_at, preferences/state, dispatch/cohort/outbox/delivery/attempt theo data-model; UUID PK bảng mới, unique constraints/index, không backfill success/provenance bịa.
- [ ] T006 [P] Viết `src/test/java/com/kizuna/notifications/NotificationSecurityTest.java` cho owner isolation/current role/ACTIVE/deletedAt, admin-only broadcast/history, guest/nhân viên khác bị chặn, scoped CSRF POST/PUT/DELETE và namespace không dưới public `/api/user/**`.
- [ ] T007 Tạo `src/main/java/com/kizuna/service/notifications/NotificationAccessPolicy.java`, mở rộng `src/main/java/com/kizuna/config/SecurityConfig.java` với current DB actor và object checks/scoped CSRF, giữ JWT/OAuth2 ngoài FE-12; chạy T006 và auth regression.
- [ ] T008 [P] Tạo `src/main/java/com/kizuna/exception/NotificationExceptionHandler.java` cho400/401/403/404/409/503 theo contracts, errors tiếng Việt không raw secret/log/key; current account không xác minh được thì fail closed hữu hạn, không lấy dữ liệu người khác.

## Phase 3: User Story 1 — Inbox, đọc và xóa cá nhân (Priority: P1) — MVP

**Goal**: Người dùng sáu vai trò xem/đọc/xóa đúng bản của mình; số chưa đọc chính xác.

**Independent Test**: Fixture có hai recipients của cùng thông báo; userA mở/read/delete không đổi userB hoặc history; UI tải detail thành công mới POST read.

### Tests

- [ ] T009 [P] [US1] Viết `src/test/java/com/kizuna/notifications/NotificationInboxContractTest.java` cho list/detail/type/unread filters, page20max50, newest-first ổn định, owner404, GET không mutate và deleted detail404.
- [ ] T010 [P] [US1] Viết `src/test/java/com/kizuna/notifications/NotificationReadDeleteTest.java` cho first-read timestamp, repeated read/delete, multi-device/race và committed unreadCount không âm; shared source/userB/admin history không bị xóa.

### Implementation

- [ ] T011 [US1] Hoàn thiện `src/main/java/com/kizuna/entity/Notification.java`, `src/main/java/com/kizuna/entity/UserNotification.java` và repositories hiện có: explicit snake_case, bỏ BaseEntity sai schema; title “varchar150 không trắng”, content “text1..10.000 không trắng”, type SYSTEM/TRIAL/ASSIGNMENT/PAYMENT/TICKET; user FK/notification FK, is_read defaultfalse/read_at nullable/deleted_at nullable, “UNIQUE(notification_id,user_id)”, received_at/provenance legacy trung thực.
- [ ] T012 [US1] Hoàn thiện `src/main/java/com/kizuna/service/INotificationService.java` và `src/main/java/com/kizuna/service/impl/NotificationServiceImpl.java`: owner-scoped nondeleted query, conditional first-read/soft-delete chung transaction với count phản ánh committed state; repeat owner DELETE200, read sau delete404, không count decrement mù.
- [ ] T013 [US1] Tạo `src/main/java/com/kizuna/controller/notifications/NotificationController.java` và response DTO tại `src/main/java/com/kizuna/dto/response/notifications/NotificationResponse.java` cho GET list/count/detail, POST read, DELETE; page>=0,size1..50, receivedAt desc/id desc; không recipient/email/internal notes của người khác, actionPath chỉ server allowlist và nơi đến recheck quyền.
- [ ] T014 [US1] Tạo `src/main/java/com/kizuna/controller/notifications/NotificationPageController.java`, fragments tại `src/main/resources/templates/fragments/notifications/inbox.html` và `src/main/resources/templates/fragments/notifications/detail.html`, page wrappers trong `src/main/resources/templates/{admin,manager,cs,academic,tutor,learner}/notifications/`; nối shell/menu/badge thực tế, ghi path đã chỉnh trong `specs/012-notification-management/validation.md`.
- [ ] T015 [US1] Tạo `src/main/resources/static/js/notifications.js` và `src/main/resources/static/css/notifications.css`: auto POST read sau detail load thành công, read-save lỗi giữ count/báo chưa lưu; read button/delete confirmation, polling badge30s, escaped text/keyboard/focus/loading-empty-error; learner/tutor DESIGN mục7, mọi labels tiếng Việt.
- [ ] T016 [US1] Chạy T009–T010 cùng inbox UI, ghi bằng chứng cách ly owner/read/delete và trạng thái lỗi vào `specs/012-notification-management/validation.md`; MVP này chưa có giao hàng broadcast/nguồn thật.

## Phase 4: User Story 2 — Tùy chọn nhận thông báo (Priority: P1)

**Goal**: Lưu toàn bộ ma trận năm nhóm/hai kênh đúng version, default in-app bật/email tắt.

**Independent Test**: GET/PUT/reload/version conflict trên tài khoản riêng; kiểm evaluator gửi bằng test delivery sau khi US3 có worker, không coi GET/PUT là đã tôn trọng preference khi phát.

### Tests

- [ ] T017 [P] [US2] Viết `src/test/java/com/kizuna/notifications/NotificationPreferenceTest.java` cho absent true/false cả5nhóm, full matrix/type unique/bool thật, expectedVersion, atomic rollback, conflict và kiểm default không chặn OTP.

### Implementation

- [ ] T018 [US2] Tạo `src/main/java/com/kizuna/entity/NotificationPreference.java`, `src/main/java/com/kizuna/entity/NotificationPreferenceState.java` và repository tương ứng trong `src/main/java/com/kizuna/repository/`: UUID PK, state user_id unique/version @Version, preference “UNIQUE(user_id,type)”, in_app_enabled/email_enabled bool; không tạo bản hàng default giả để làm đầy UI.
- [ ] T019 [US2] Tạo `src/main/java/com/kizuna/service/notifications/NotificationPreferenceService.java` và `src/main/java/com/kizuna/service/notifications/NotificationEligibilityPolicy.java`: whole-matrix transaction/version, defaulttrue/false khi thiếu, recheck active/type preference trước phát; urgent ADMIN+ALL override chỉ IN_APP, email opt-in; producer khác không được tự nâng urgent.
- [ ] T020 [US2] Mở rộng `src/main/java/com/kizuna/controller/notifications/NotificationController.java` cho GET/PUT preferences và tạo `src/main/resources/templates/fragments/notifications/preferences.html`; wrappers/menu sáu shell, CSRF/expectedVersion/save tường minh, không auto-save và không trộn OTP/security emails.
- [ ] T021 [US2] Chạy T017, ghi checkpoint config vào `specs/012-notification-management/validation.md`; phần send-time preference còn chờ T033/T034 để nghiệm thu thực tế, không đánh dấu toàn Story2 xong trước khi worker chứng minh.

## Phase 5: User Story 3 — Gửi nhóm, hẹn giờ và lịch sử (Priority: P2)

**Goal**: Admin preview/confirm/queue, frozen cohort lúc đến hạn, kết quả theo kênh trung thực.

**Independent Test**: Broadcast group fixture accounts, schedule fake clock, SMTP accepted/failure/unknown; restart không mất queue hoặc trùng in-app. Không cần urgent-all hoặc module events để thử group broadcasts.

### Tests

- [ ] T022 [P] [US3] Viết `src/test/java/com/kizuna/notifications/NotificationBroadcastContractTest.java` cho preview không persist, validate limits/roles/channels, queue202, submissionKey same-body dedup/different-body409, history/filter/details và zero audience.
- [ ] T023 [P] [US3] Viết `src/test/java/com/kizuna/notifications/NotificationDispatchRecoveryTest.java` cho scheduled due/cohortfreeze, role changes, claim không vượt slot rảnh, lease60s/token renewal trước phát, mất lease không gửi/không finalize, worker cũ không ghi đè; crash/restart, in-app uniqueness, EMAIL_SENDING hết hạn/timeout→UNKNOWN không resend, known-unsent tối đa4calls/backoff30/120/300s và counter aggregation.

### Implementation

- [ ] T024 [US3] Tạo `src/main/java/com/kizuna/entity/NotificationDispatch.java`, `src/main/java/com/kizuna/entity/NotificationDispatchRecipient.java`: UUID FK, confirmed title1..150/content1..10.000, five types, target modes/roles/channels allowlist, scheduled_at nullable/cohort_frozen_at nullable/version/actual count; “UNIQUE(creator_id,submission_key)”, “UNIQUE(dispatch_id,user_id)”; confirmed content/cohort bất biến, previewCount không là actualCount.
- [ ] T025 [US3] Tạo `src/main/java/com/kizuna/entity/NotificationDelivery.java`, `src/main/java/com/kizuna/entity/NotificationDeliveryAttempt.java` và repos tại `src/main/java/com/kizuna/repository/`: state PENDING/CLAIMED/EMAIL_SENDING/IN_APP_AVAILABLE/EMAIL_ACCEPTED/FAILED/SKIPPED/UNKNOWN, lease/worker/next_attempt, “unique(dispatch_id,user_id,channel)”, “UNIQUE(delivery_id,attempt_number)”; attempts safe outcomes, không provider raw error/secrets, không email read receipt giả.
- [ ] T026 [US3] Tạo `src/main/java/com/kizuna/service/notifications/NotificationBroadcastService.java` và `src/main/java/com/kizuna/service/notifications/NotificationCohortService.java`: queue/history atomic, idempotent submissionKey, future timezone validation; khi đến hạn lock/materialize active roles bằng transaction nhất quán, frozen cohort đúng1lần/count thực, recovery không mở rộng danh sách; account bị khóa trước phát ghi skip.
- [ ] T027 [US3] Tạo `src/main/java/com/kizuna/service/notifications/mail/NotificationMailAdapter.java` dùng JavaMailSender transport hiện có, synchronous classified result trong worker, SMTP timeout hữu hạn10s; không gọi OTP @Async void hoặc diagnostic template, không đổi luồng OTP/staff credential và không log secret/nội dung người dùng.
- [ ] T028 [US3] Tạo `src/main/java/com/kizuna/service/notifications/NotificationDeliveryWorker.java`: poll5s, giữ slot rảnh trước claim tối đa min(100,slot đã giữ), FOR UPDATE SKIP LOCKED, lease60s/claim_token riêng; kiểm token/lease và gia hạn60s ngay trước phát bằng conditional update, mất lease không gửi hoặc finalize. Eligibility/active/preferences theo T019, in-app insertion+success cùng transaction; EMAIL_SENDING commit trước call, finalize guarded token/lease/version; timeout/expired sending→UNKNOWN không resend. Known-unsent transient tối đa4calls tổng cộng, retry sau30/120/300s, chỉ transport call tăng attempt_count, permanent/hết attempt4→FAILED.
- [ ] T029 [US3] Tạo `src/main/java/com/kizuna/service/notifications/NotificationDeliverySummaryService.java`: tổng hợp SCHEDULED/QUEUED/SENDING/NEEDS_REVIEW/NO_RECIPIENTS/COMPLETED/PARTIAL/FAILED theo data-model, all-skipped success0, pending/unknown không completed; unique users khác tổng channels; user soft-delete không xóa history metrics.
- [ ] T030 [US3] Tạo `src/main/java/com/kizuna/controller/admin/NotificationBroadcastController.java` và DTO dưới `src/main/java/com/kizuna/dto/request/notifications/`: preview/POST queue202/GET history-detail-deliveries theo contracts, current admin/CSRF, pagination20max50, timestamps/labels Việt; không cancel/edit/manual-retry API.
- [ ] T031 [US3] Tạo pages `src/main/resources/templates/admin/notifications/broadcast-list.html`, `src/main/resources/templates/admin/notifications/broadcast-form.html`, `src/main/resources/templates/admin/notifications/broadcast-detail.html`; gắn admin routes/menu và mở rộng `src/main/resources/static/js/notifications.js` cho preview/group/schedule/confirm/history; queued khác sent, không hiện raw role/SMTP error.
- [ ] T032 [US3] Chạy T022–T023 với PostgreSQL/test SMTP/clock, đối chiếu failure/UNKNOWN/restart/counts theo `specs/012-notification-management/quickstart.md`; ghi evidence vào `specs/012-notification-management/validation.md`.
- [ ] T033 [US3] Bổ sung send-time integration cases vào `src/test/java/com/kizuna/notifications/NotificationPreferenceTest.java`: đổi prefs giữa enqueue/send, known unsent retry và urgent không do producer tự gắn; kiểm OTP ngoài luồng; cập nhật checkpoint Story2 trong `specs/012-notification-management/validation.md` sau worker thật.

## Phase 6: User Story 4 — Thông báo khẩn toàn hệ thống (Priority: P2)

**Goal**: Chỉ admin gửi toàn active users sau xác nhận rõ, bắt buộc in-app/email opt-in.

**Independent Test**: Sáu role active nhận in-app dù tắt; locked/deleted bỏ qua, email chỉ opt-in; sai confirmation/hủy không tạo dispatch, không hẹn giờ urgent.

### Tests

- [ ] T034 [P] [US4] Viết `src/test/java/com/kizuna/notifications/NotificationUrgentBroadcastTest.java` cho ALL/urgent/current admin, confirmation chính xác “Xác nhận”, cancel/wrong/no schedule, số preview/actual, six roles active, in-app override duy nhất và email opt-in.

### Implementation

- [ ] T035 [US4] Mở rộng `src/main/java/com/kizuna/service/notifications/NotificationBroadcastService.java` và `src/main/java/com/kizuna/controller/admin/NotificationBroadcastController.java`: ADMIN+ALL+URGENT chỉ gửi ngay/IN_APP bắt buộc, không raw recipient emails, confirmation tiếng Việt và số dự kiến; giữ submission dedup/cohort/eligibility chung, không tạo delivery engine thứ hai.
- [ ] T036 [US4] Mở rộng `src/main/resources/templates/admin/notifications/broadcast-form.html`, `src/main/resources/static/js/notifications.js` và `src/main/java/com/kizuna/service/notifications/NotificationDeliverySummaryService.java` nếu cần labels urgent: cảnh báo rõ toàn hệ thống, cancel không submit, input confirmation, email tôn trọng preference; không all-caps hoặc tuyên bố delivered/read.
- [ ] T037 [US4] Chạy T034 và hồi quy US3/US2 theo `specs/012-notification-management/quickstart.md`, ghi bằng chứng sáu role/locked/default/channel outcomes vào `specs/012-notification-management/validation.md`.

## Phase 7: Integration, Polish & Cross-Cutting Concerns

**Purpose**: Tích hợp nguồn thật của FR-015 và nghiệm thu xuyên suốt, không dùng synthetic event thay bằng chứng source.

- [ ] T038 Viết `src/test/java/com/kizuna/notifications/NotificationOutboxIntegrationTest.java`, tạo `src/main/java/com/kizuna/entity/NotificationOutboxEvent.java`, `src/main/java/com/kizuna/service/notifications/NotificationEventPort.java` và consumer trong `src/main/java/com/kizuna/service/notifications/NotificationOutboxWorker.java`: event_key stable unique, source transaction rollback khôngevent, payload tối thiểu/recipients authorized, atomic dispatch+processed, duplicate không trùng; không public emit API hoặc producer override urgency.
- [ ] T039 Tích hợp payment-confirmed outbox vào `src/main/java/com/kizuna/service/tutor/impl/TutorMembershipServiceImpl.java` tại processVnPayReturn sau verify signature/successful transition, cùng transaction membership/tutor; stable membership/transaction event identity, guard callback lặp để không lặp business effects. Kiểm bằng `src/test/java/com/kizuna/notifications/NotificationMembershipPaymentEventTest.java`; không phát từ create URL/PENDING/direct test activate hoặc sửa payment engine ngoài boundary cần thiết.
- [ ] T040 Tích hợp trial created/confirmed/cancelled trong transaction thật ở `src/main/java/com/kizuna/service/impl/TrialBookingServiceImpl.java`; test tại `src/test/java/com/kizuna/notifications/NotificationTrialEventsTest.java` kiểm committed event/outbox cùng transaction, rollback không phát, participant recipient đúng và repeat không trùng. Nếu vẫn stub, ghi commit-point/đầu mối hoặc task phụ thuộc vào `specs/012-notification-management/implementation-readiness.md`; không fake API và không đánh dấu task hoàn tất.
- [ ] T041 Tích hợp assignment published/graded trong transaction thật ở `src/main/java/com/kizuna/service/impl/AssignmentServiceImpl.java`; test tại `src/test/java/com/kizuna/notifications/NotificationAssignmentEventsTest.java` cho learner được giao/chủ bài đã chấm, rollback và event dedup. Ghi nguồn còn stub/đầu mối hoặc task phụ thuộc vào `specs/012-notification-management/implementation-readiness.md`, giữ chưa hoàn tất nếu chưa có publish/grade thật.
- [ ] T042 Tích hợp support public reply/resolved trong transaction thật ở `src/main/java/com/kizuna/service/impl/SupportTicketServiceImpl.java`; test tại `src/test/java/com/kizuna/notifications/NotificationSupportEventsTest.java` cho requester, rollback/dedup, nội dung công khai không có escalation notes. Nếu FE-10 chưa triển khai source, ghi điểm tích hợp/đầu mối hoặc task phụ thuộc vào `specs/012-notification-management/implementation-readiness.md`, không dùng event fixture để đóng task.
- [ ] T043 Viết/chạy `src/test/java/com/kizuna/notifications/NotificationBusinessEventsTest.java` cho từng nguồn thực ở T039–T042, event lặp/rollback/scope revocation/deep-link quyền; ghi riêng adapter fixture vs E2E source thật trong `specs/012-notification-management/validation.md`, FR-015 chưa đạt nếu một nhóm vẫn thiếu source.
- [ ] T044 Rà security/DTO/PII/logging tại `src/test/java/com/kizuna/notifications/NotificationSecurityTest.java`, bổ sung toàn bộ role/current account/CSRF và object namespaces; kiểm escaping, safe actionPath và không lộ ticket private notes/operational secrets trong email/inbox/history.
- [ ] T045 [P] Tạo `src/test/java/com/kizuna/notifications/NotificationViewsTest.java`, kiểm thủ công menu/badge/list/detail/preferences sáu shells và admin broadcasts; loading/empty/error, keyboard/focus, sentence case, learner/tutor DESIGN mục7, ghi bằng chứng khôngPII vào `specs/012-notification-management/validation-ui.md`.
- [ ] T046 [P] Tạo `src/test/load/notifications.js` và `src/test/load/README-notifications.md`:100users/10phút,10.000personal notifications,100recipient dispatch, credentials env/test mail only; HTTP metrics riêng browser render timing cho SC-006, không coi enqueue là gửi xongemail.
- [ ] T047 Chạy T046 và kiểm EXPLAIN/count/history/restart theo `specs/012-notification-management/quickstart.md`; đo p95render≤2s/enqueue≤2s, requestfailure≤5s và scheduler start≤60s, chỉ thêm index bằng migration mới khi đo chứng minh cần; ghi dữ liệu thật vào `specs/012-notification-management/validation-performance.md`.
- [ ] T048 Cập nhật lệnh/profile/migration thật tại `specs/012-notification-management/quickstart.md` và `src/main/resources/db/changes/README-fe12.md`; chạy regression thích hợp và tổng hợp FR-001–016/SC-001–007 vào `specs/012-notification-management/validation.md`. Khi có10người thử inbox, ghi≥9người hoàn thành≤2phút; thiếu người/source/SMTP thì chưa chạy/blocked, không claim hoàn tất toàn feature.

## Dependencies & Execution Order

```text
Setup T001–T003 -> Foundation T004–T008
  -> US1 T009–T016 (MVP inbox)
  -> US2 T017–T021 (preference config)
  -> US3 T022–T032 (dispatch engine)
      -> T033 (send-time preference, chốt US2)
      -> US4 T034–T037 (urgent-all)
  -> T038–T043 (transactional source integration)
  -> T044–T048 (regression/UI/load/acceptance)
```

- T004 sau T001/T002; T005 sau T003/T004; T006/T008 khác file; T007 theo security tests. Mọi story chờ foundation.
- US1 tests T009/T010 song song → model T011 → service T012 → API T013 → UI T014/T015 → checkpoint T016.
- US2 T017 → T018 → T019 → T020 → config check T021; send-time integration chờ T028/T033, không tạo vòng phụ thuộc.
- US3 T022/T023 song song; T024/T025 → T026/T027 → T028 → T029 → T030/T031 → T032; T028 phụ thuộc preference T019 và inbox T011/T012. T001 xác nhận điều kiện triển khai; T023/T028 tuân thủ retry4calls và lease/token đã chốt, không cần quyết định retry mới.
- US4 T034 → T035/T036 → T037, chỉ reuse worker và policy, không bypass email opt-in.
- T038 theo engine T028 và migration; từng source T039–T042 sau event port, có test và gate riêng; T043 sau đủ bốn nguồn thật. Có thể kiểm/test port trước nguồn nhưng không đóng source acceptance. Source bị chặn không ngăn demo admin broadcast, vẫn chặn hoàn tất FE-12.
- Controller/JS/source chung sửa tuần tự. T045/T046 tách file có thể song song; T047 sau load script, T048 sau regression/evidence, chỉ ghi đạt khi đã chạy.

## Parallel Examples per Story

| Story | Việc có thể làm song song khi đủ tiền đề | Hợp nhất |
| --- | --- | --- |
| US1 | T009 inbox contracts và T010 read/delete race | Trước model/service và T016 |
| US2 | T017 preference tests và rà UI labels theo contract trong T020 | UI wiring vẫn chờ T019 |
| US3 | T022 submit/history contracts và T023 worker recovery tests | Trước T024–T032 |
| US4 | T034 tests và dựng phần cảnh báo tĩnh T036 theo contract | Wiring/final test chờ T035 |

Ví dụ chỉ là cơ hội chia việc, không tự chạy agent trong lượt tạo tasks; không sửa cùng file đồng thời.

## Implementation Strategy

1. Setup/preflight/DB test/policy trước; không seed success hoặc dữ liệu giả trong vận hành.
2. MVP US1 giúp người dùng quản lý inbox thực, chưa có nguồn thật thì chỉ demo test environment với nhãn rõ.
3. Thêm US2 rồi US3, kiểm preference khi gửi thực, schedule/restart và SMTP ambiguity.
4. Thêm urgent-all với xác nhận và giới hạn override đã chốt; tích hợp bốn sources theo transaction thật.
5. Nghiệm thu load/browser/role/evidence; chạy `$speckit-analyze` trước `$speckit-implement` theo constitution. Không commit/push/gửi email trong bước tasks.

## Requirement Traceability

| Requirement | Tasks |
| --- | --- |
| FR-001 | T006–T008, T013, T030, T035, T044 |
| FR-002–004 | T009–T016 |
| FR-005–006 | T017–T021, T028, T033–T037 |
| FR-007–010 | T022–T026, T028, T030–T032, T035 |
| FR-011–013 | T023, T025, T027–T032, T036–T037 |
| FR-014 | T007, T019, T026, T028, T039–T044 |
| FR-015 | T038–T043 |
| FR-016 | T008, T013–T015, T020, T031, T036, T044–T045 |
| SC-001 | T006, T009–T010, T016–T017, T044 |
| SC-002 | T019, T028, T033–T037 |
| SC-003 | T023, T026, T028, T032, T047 |
| SC-004 | T022–T023, T025–T028, T032, T038–T043 |
| SC-005 | T023, T027–T029, T032, T037 |
| SC-006 | T046–T047 |
| SC-007 | T045, T048 |

## Notes

Bao phủ task không chứng minh source/SMTP đã chạy. Retry/lease đã chốt trong research/model/contracts và T023/T028; readiness T001 cùng từng source T039–T042 phải có bằng chứng riêng. Thiếu nguồn vẫn giữ task chưa hoàn tất, không tự hạ tiêu chí hoặc bypass quyền để checkbox đạt.

## Cập nhật 2026-11-10

- Sửa lỗi không tạo được thông báo tự động: `NotificationEvents.Writer` bỏ `Propagation.NESTED` (JPA không hỗ trợ savepoint), tự đặt savepoint JDBC; đã xác nhận trên DB có đợt `origin=EVENT` đầu tiên sau khi sửa. `NotificationEventsTest` 5/5 đạt.
- Thêm sự kiện FE-10 "Yêu cầu hỗ trợ cần bàn giao" → mọi Quản lý.
- Sidebar: "Thông báo" ở cuối, trên thẻ tài khoản (CS bỏ mục trùng; gia sư chuyển xuống cuối, bỏ chuông cạnh logo trên máy tính). Học viên, Cán bộ học vụ chưa đổi.
