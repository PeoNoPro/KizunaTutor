# Tasks: FE-11 — System Administration & Operational Reporting

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/operations.md và quickstart.md trong `specs/011-system-admin-reporting/`; constitution 1.0.0.

**Prerequisites**: Năm câu trả lời clarify đã chốt. Giữ Java17/Spring Boot/Thymeleaf/PostgreSQL hiện có.

**Tests**: Spec yêu cầu kiểm chứng FR-001–018/SC-001–007 và constitution yêu cầu test nghiệp vụ/tích hợp. Viết test hành vi trước implementation tương ứng, xác nhận thất bại do hành vi chưa có, rồi chạy lại. Fixture chỉ DB test cách ly.

**Organization**: Setup → foundation → bốn story → nghiệm thu. Báo cáo US2 phụ thuộc event US3, không tuyên bố tất cả story độc lập về mã nguồn.

## Công việc hiện hành — đồng bộ 2026-11-05

Các dấu [x] cũ là bằng chứng phạm vi trước, không chứng nhận FR-023–029. Rà code hiện tại trước khi làm; chỉ đóng task sau kiểm chứng. Cấu hình học thử, sức khỏe, phân công sự cố trong các phase cũ là lịch sử, không triển khai mới.

- [ ] T077 [FR-023–026] Rà schema/consumer cấu hình; thiết kế mapping theo data-model và contracts; xác định vị trí thông tin chung/footer hiện có, dữ liệu liên hệ do nhóm cung cấp. Không chạy migration trên DB chung khi chỉ rà tài liệu.
- [ ] T078 [FR-023/026] Triển khai API đọc/sửa thông tin nền tảng và API công khai tối thiểu; Admin ACTIVE, kiểm mật khẩu lần đầu/CSRF/version/validation; audit và cấu hình nguyên tử, không dữ liệu giả.
- [ ] T079 [FR-024/026] Form cấu hình và xác nhận trước/sau/lý do; tích hợp các vị trí thông tin chung thực tế, phân biệt thiếu dữ liệu/lỗi; kiểm desktop/mobile. Phụ thuộc T078.
- [ ] T080 [FR-025] Rà/hoàn thiện giới hạn đính kèm hỗ trợ 1–10 MB; frontend và backend cùng nguồn; kiểm dưới/đúng/vượt giới hạn và giữ tệp cũ. Không mở ô chỉnh upload chưa có API.
- [ ] T081 [FR-027] Rà/gỡ nút và API phân công/phân công lại; tự nhận và trả lại đúng quyền/trạng thái/version, giao dịch và event nguyên tử; giữ lịch sử cũ; kiểm hai Admin nhận đồng thời.
- [ ] T082 [FR-028] Rà/hoàn thiện hàng đợi email DB không lưu mật khẩu rõ, retry/trạng thái, căn cứ bổ nhiệm Manager; kiểm chặn API FE-11 trước đổi mật khẩu. API feature khác ghi phụ thuộc FE-01.
- [ ] T083 [FR-029] Rà báo cáo học thử offline và mua gói, bỏ chỉ số/bộ lọc/biểu đồ hoàn tiền; không sửa gói; kiểm phân biệt rỗng/chưa tích hợp/lỗi và nguồn trạng thái hoàn thành học thử.
- [ ] T084 [SC-012–014] Sau T078–T083, nghiệm thu bằng đăng nhập thật và dữ liệu kiểm chứng trên môi trường demo được phép; cập nhật validation, không suy kết quả từ mock hay dấu kiểm cũ.
- [ ] T085 [HOÃN] Banner bảo trì: chờ người dùng tải code mới, rà lại rồi cập nhật hợp đồng/task trước triển khai; không chặn nghiệm thu phạm vi hiện tại.
- [ ] T086 [PHỤ THUỘC] Kiểm điều kiện bàn giao khi điều chuyển/thăng chức sau khi nhóm có hợp đồng dữ liệu công việc đang giữ; không tự sửa repository feature khác hoặc tuyên bố đã hoàn thành.

Các task cũ đang mở: T011/T016/T019 thuộc booking cũ cần rà phụ thuộc, không dùng để chặn cấu hình mới; T042/chính sách và T059/AI hoãn; T048 nghiệm thu cũ chưa hoàn tất; T060 phụ thuộc FE-12. Thứ tự mới: T077 → T078 → T079; T080–T083 theo hợp đồng hiện hành → T084. T085/T086 không tự đánh dấu hoàn thành.

## Format: `[ID] [P?] [Story] Description`

### Ưu tiên phạm vi mới ngày 2026-11-03

Đọc Clarifications 2026-11-03 trong spec.md trước danh sách lịch sử. Task [x] giữ bằng chứng bản cũ, không chứng minh đáp ứng quyền mới. T020/T022–T024 và phần health của T001/T027/T043/T051 không còn yêu cầu phát triển mới. T037–T042 và nghiệm thu chính sách trong T048 tiếp tục hoãn; T059 vẫn hoãn. T060 chỉ còn phụ thuộc FE-12, không chặn Excel sự cố đã được người dùng xác nhận. T055/T056/T058 cần kiểm lại quyền qua T068–T069.

- [x] T067 [FR-019] Đã chốt 2026-11-04: Manager đổi trực tiếp Cán bộ học vụ ↔ Chăm sóc khách hàng; thăng lên Manager do Manager đề xuất, Admin duyệt; Admin gán Manager khi tạo; không cấp Admin; hạ chức Manager để sau. Xem Clarifications 2026-11-04 và contracts/operations.md.
- [x] T068 [FR-019] Kiểm thử quyền hiện tại/CSRF/version/audit: Admin không đổi vai trò sau tạo qua mọi đường, Manager không tạo/khóa/mở và chỉ đổi vai trò được duyệt; kiểm tài khoản hiện tại/quản trị cuối, thu hồi quyền. Không mở chung operations cho Manager.
- [x] T069 [FR-019] Sau T067–T068, sửa service/controller/menu/form nhân viên, chuyển thao tác đổi vai trò sang khu vực Manager theo ma trận; chạy kiểm thử và ghi bằng chứng.
- [x] T070 [FR-020] Chốt hợp đồng nhập Excel: mẫu/cột, loại/giới hạn tệp/số dòng, vai trò khởi tạo, cấp mật khẩu, chính sách lô khi có lỗi, xem trước/xác nhận/kết quả. Không ghi đè tài khoản trùng.
- [x] T071 [FR-020] Sau T070, kiểm thử/triển khai tải mẫu, kiểm tra/xem trước/xác nhận tạo; email trùng tệp/DB, thay đổi sau xem trước, quyền/CSRF, giới hạn, lỗi từng dòng và không lộ mật khẩu; kết quả khớp số tạo thực.
- [x] T072 [FR-021] Thiết kế hợp đồng xuất Excel sự cố: Admin, bộ lọc, toàn bộ kết quả qua nhiều trang, cột/ngày/thời điểm chốt, giới hạn tài nguyên, tải tệp/rỗng/lỗi. Không nhập sự cố từ Excel.
- [x] T073 [FR-021] Sau T072, kiểm thử/triển khai xuất Excel sự cố; đối chiếu cột/bộ lọc/quyền/rỗng/lỗi/nội dung giống công thức, không sửa sự cố. Ghi bằng chứng tệp mở bằng Excel hoặc công cụ đọc tương thích.
- [x] T074 [FR-022] Bỏ đúng mục/khối sức khỏe khỏi menu/dashboard Admin; rà liên kết/nguồn gọi trực tiếp, không chuyển khối hoặc xóa dữ liệu/dịch vụ dùng chung ngoài phạm vi. Giữ sự cố/thống kê.
- [x] T075 [FR-022] Đối chiếu nguồn/hợp đồng, triển khai biểu đồ vai trò, tài khoản mới, trạng thái tài khoản và sự cố; phân biệt kỳ/trạng thái hiện tại, thiếu/lỗi/0. Không số online/truy cập giả.
- [x] T076 [SC-008–011] Sau T069/T071/T073–T075, kiểm API/quyền/UI desktop–mobile/nhật ký/import/export trên môi trường được phép; cập nhật validation/ma trận FR/SC. Không đánh dấu xong từ fixture UI hoặc tài liệu. Ghi riêng phụ thuộc FE-12/hồ sơ/booking.

Thứ tự: T067 → T068 → T069; T070 → T071; T072 → T073; T074/T075 theo nguồn/hợp đồng được kiểm tra; tất cả → T076. Task mới chưa triển khai. Lượt chỉnh Speckit không chạy database, sửa schema, commit hoặc push.

- `[P]`: khác file, làm đồng thời sau đủ tiền đề. Không dùng để bỏ dependency.
- Mọi đường dẫn tương đối từ repo KizunaTutor; mọi file văn bản UTF-8 không BOM. Task chung controller/service/JS phải tuần tự.
- Chưa thực hiện thì giữ checkbox trống. Không tự commit/push/deploy trong lượt tạo tasks.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Xác định phụ thuộc và chuẩn bị DB kiểm thử.

- [X] T001 Ghi readiness vào `specs/011-system-admin-reporting/implementation-readiness.md`: schema/config hiện tại, workflow booking thật, approved policy corpus và verified optional health probe. Nêu mâu thuẫn cần chốt giữa DB-down health snapshot với reload quyền DB mỗi request; không bỏ kiểm quyền để health trả thành công. Gate này phải giải quyết trước T023; không ghi credential/PII.
- [X] T002 Tạo `src/test/resources/application-operations-test.properties` và `src/test/java/com/kizuna/operations/OperationsTestEnvironment.java` dùng OPERATIONS_TEST_DB_URL/USERNAME/PASSWORD; fail khi thiếu hoặc DB không được đánh dấu kiểm thử; fixture/clock riêng, không gọi mail/payment/memory write thật.
- [X] T003 [P] Viết `src/main/resources/db/changes/README-fe11.md` với preflight, backup/restore DB test, version/lock và psql ON_ERROR_STOP; không chạy script nguồn có DROP CASCADE hoặc sửa migration FE-10 đã áp dụng.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Migration, actor policy và transaction dùng chung; hoàn tất trước story.

- [X] T004 Viết `src/test/java/com/kizuna/operations/OperationsMigrationTest.java` sau T001–T002: giữ config/booking legacy, mapping snake_case, baseline không bịa provenance, chạy lại an toàn, missing/invalid config phải báo rõ thay vì seed45/12.
- [X] T005 Tạo `database/seeds/fe11_operations_schema.sql` (trước đây là `fe11_001_operations_schema.sql`) sau T003–T004: version table/lock; config revision/state/audit, incident/event/components, policy documents và snapshot booking theo data-model. UUID cho bảng mới, String PK config giữ nguyên; không DROP dữ liệu hoặc seed policy giả.
- [X] T006 [P] Viết `src/test/java/com/kizuna/operations/OperationsAuthorizationTest.java` cho admin-only operations, learner/tutor-only policies, các vai trò khác/guest bị chặn, inactive/deleted/current DB role thay đổi và CSRF missing/wrong/correct.
- [X] T007 Tạo `src/main/java/com/kizuna/features/admin/operations/service/OperationsActorResolver.java` và `src/main/java/com/kizuna/features/admin/operations/service/OperationsAccessPolicy.java`: reload User/role/ACTIVE/deletedAt mọi read/write; không dùng display fallback hoặc role token cũ cấp quyền; assignee kiểm ở service, allowedActions chỉ là gợi ý UI.
- [X] T008 Cập nhật `src/main/java/com/kizuna/config/SecurityConfig.java` cho FE-11 routes và scoped CSRF POST/PUT, token từ SSR; giữ JWT/OAuth2 và endpoint ngoài FE-11; chạy T006 cùng auth regression.
- [X] T009 [P] Tạo `src/main/java/com/kizuna/features/admin/operations/controller/OperationsExceptionHandler.java` và `src/main/java/com/kizuna/features/admin/operations/service/OperationsCommandExecutor.java`: lỗi400/401/403/404/409/503 tiếng Việt, không raw secret/log/exception; expectedVersion nguyên không âm cho update, locks/version/recheck recipient, mutation+event cùng transaction, stale409 không retry âm thầm, clock inject được.

**Checkpoint**: Foundation đạt kiểm chứng không đồng nghĩa tính năng đã xong.

## Phase 3: User Story 1 — Cấu hình đặt học thử (Priority: P1) — MVP có điều kiện

**Goal**: Chỉ chỉnh hai tham số, có lịch sử/version và áp dụng vào booking mới.

**Independent Test**: Sửa cấu hình rồi tạo booking thật trước/sau; old booking không đổi, boundary advance được kiểm. Thiếu workflow booking phải ghi blocked, evaluator test không thay E2E.

### Tests

- [X] T010 [P] [US1] Viết `src/test/java/com/kizuna/operations/OperationsBookingPolicyContractTest.java`: GET/PUT/history, duration15/120 và advance1/168 hợp lệ, ngoài biên/decimal/blank sai, reason1..2.000 không trắng, hold15 read-only, raw key bị từ chối và stale config409.
- [ ] T011 [P] [US1] Viết `src/test/java/com/kizuna/operations/OperationsBookingPolicyIntegrationTest.java` cho revision snapshot, booking/config concurrency, audit rollback và legacy; tách evaluator unit tests khỏi ca tạo booking thật, không fake pass khi workflow thiếu.

### Implementation

- [X] T012 [US1] Hoàn thiện `src/main/java/com/kizuna/entity/SystemConfiguration.java` và `src/main/java/com/kizuna/repository/SystemConfigurationRepository.java`: “config_key varchar(100) PK String”, “config_value text not null”, “data_type varchar(20) not null”, “description varchar(255) nullable”, updated_at; bỏ BaseEntity khi không có created_at, map snake_case tường minh.
- [X] T013 [US1] Tạo `src/main/java/com/kizuna/features/admin/operations/entity/BookingPolicyRevision.java`, `src/main/java/com/kizuna/features/admin/operations/entity/BookingPolicyState.java`, `src/main/java/com/kizuna/features/admin/operations/entity/ConfigurationAuditEvent.java` và repository tương ứng trong `src/main/java/com/kizuna/features/admin/operations/repository/`: UUID, revision_number “bigint unique tăng đơn điệu”, duration “int CHECK 15..120”, advance “int CHECK 1..168”, singleton state/current_revision/version; changed_by “nullable chỉ baseline”, reason “1..2.000 ký tự không trắng cho mutation”; audit trước/sau bất biến, baseline không bịa tác giả/ngày cũ.
- [X] T014 [US1] Tạo `src/main/java/com/kizuna/features/admin/operations/service/BookingPolicyConfigurationService.java` và request tại `src/main/java/com/kizuna/features/admin/operations/dto/BookingPolicyUpdateRequest.java`; response allowlist trong service, bọc ApiResponse: allowlist duration/advance số nguyên đúng giới hạn, hold15 cố định; lock current state, legacy+revision+audit nguyên tử; missing/invalid editable=false, không fallback45/12 và chặn consumer dùng config invalid.
- [X] T015 [US1] Tạo `src/main/java/com/kizuna/features/booking/service/BookingPolicyEvaluator.java`, bổ sung snapshot trong `src/main/java/com/kizuna/entity/TrialBooking.java`: “policy_revision_id FK nullable cho booking legacy”, duration/advance nullable legacy nhưng booking mới đủ ba trường; start>=serverNow+advance, end=start+duration, cùng lock revision với writer, không client override hoặc tính lại lịch cũ.
- [ ] T016 [US1] Tích hợp evaluator/snapshot vào consumer tạo booking thật ở `src/main/java/com/kizuna/service/impl/TrialBookingServiceImpl.java` và `src/main/java/com/kizuna/repository/TrialBookingRepository.java`; nếu workflow vẫn stub/thiếu ownership hoặc availability, ghi dependency module booking vào `specs/011-system-admin-reporting/implementation-readiness.md`, không tự dựng API giả/thanh toán và không đánh dấu task này hoàn tất.
- [X] T017 [US1] Tạo `src/main/java/com/kizuna/features/admin/operations/controller/OperationsApiController.java` và `src/main/java/com/kizuna/features/admin/operations/controller/OperationsController.java` cho booking-policy GET/PUT/history; DTO/version/CSRF, history20max50, không serialize entity/config key tùy ý.
- [X] T018 [US1] Tạo `src/main/resources/templates/admin/operations/booking-policy.html`, `src/main/resources/static/js/features/admin/operations.js`, `src/main/resources/static/css/features/admin/operations.css`, gắn link vào `src/main/resources/templates/admin/layout/sidebar.html`: labels/units/range, hold15 chỉ đọc, reason/xác nhận/history, conflict giữ text, empty/error riêng, keyboard/focus và không all-caps/raw keys.
- [ ] T019 [US1] Chạy T010–T011 và luồng booking thật theo `specs/011-system-admin-reporting/quickstart.md`; ghi before/after, giới hạn và blocker vào `specs/011-system-admin-reporting/validation.md`; không ghi SC-001 đạt toàn bộ nếu T016 chưa hoàn tất.

**Checkpoint**: MVP hoàn chỉnh chỉ khi consumer booking thật có bằng chứng; có thể demo config riêng với phần chưa hoàn tất được ghi rõ.

## Phase 4: User Story 2 — Sức khỏe và báo cáo vận hành (Priority: P1)

**Goal**: Nguồn quan sát thật, trạng thái thiếu/lỗi chính xác, báo cáo từ lịch sử incident.

**Independent Test**: Health dùng clock/probe test, ngắt một nguồn và kiểm timeout/stale/UNKNOWN; report dùng event fixture sau T031–T032 rồi kiểm live sau US3. Không cần thay config/policy.

### Tests

- [X] T020 [P] [US2] Viết `src/test/java/com/kizuna/operations/OperationsHealthTest.java`: collector30s, probe2s/tổng4s, không chồng, observedAt không đổi khi đọc cache, >60s stale, unknown khi thiếu nguồn, không hardcodedUP/auto incident; kỳ vọng DB-auth outage theo gate T001, không tự nới quyền.
- [X] T021 [P] [US2] Viết `src/test/java/com/kizuna/operations/OperationsIncidentReportTest.java`: default7ngày/max366 inclusive, timezone, ngày sai/future reject, distinct resolves, carry-over open, RESOLVED vẫn open, state as-of quá khứ, từng breakdown riêng và source failure không0; query tích hợp chạy sau T031–T032.

### Implementation

- [X] T022 [US2] Tạo `src/main/java/com/kizuna/features/admin/operations/service/OperationsComponentCatalog.java`, `src/main/java/com/kizuna/features/admin/operations/service/OperationsHealthCollector.java` và record Observation bất biến trong collector: component allowlist deployment/no client URL, HEALTHY/DEGRADED/UNAVAILABLE/UNKNOWN, “observedAt nullable khi chưa quan sát”, “responseTimeMs nullable nếu không có phép đo hợp lệ”, reason/scope Việt; finite probes2s/budget4s mỗi30s, không chồng, snapshot memory thread-safe, DEGRADED chỉ từ kết luận nguồn. DB probe pool riêng read-only; optional agent-memory qua `src/main/java/com/kizuna/service/ai/AgentMemoryService.java` chỉ khi verified health contract, không learner read/write làm probe.
- [X] T023 [US2] Sau chốt gate T001, mở rộng `src/main/java/com/kizuna/features/admin/operations/controller/OperationsApiController.java` và `src/main/java/com/kizuna/features/admin/operations/controller/OperationsController.java` cho GET health snapshot; không bỏ current-role checks để DB down trả payload. Nếu identity DB hoàn toàn mất, fail-closed hữu hạn và ghi điều chỉnh thiết kế cần được duyệt; không tuyên bố mọi outage vẫn trả thành công.
- [X] T024 [US2] Tạo `src/main/resources/templates/admin/operations/health.html`, mở rộng `src/main/resources/static/js/features/admin/operations.js`, `src/main/java/com/kizuna/controller/admin/AdminController.java` và `src/main/resources/templates/admin/dashboard.html`: bỏ serverStatus hardcode trong phần tích hợp, poll30s/stale>60s/refresh snapshot, observedAt thật và scope “Tiến trình ứng dụng”; empty/error/UNKNOWN rõ, không tự tạo incident.
- [X] T025 [US2] Sau T031–T032, tạo `src/main/java/com/kizuna/features/admin/operations/repository/OperationsIncidentReportRepository.java` và `src/main/java/com/kizuna/features/admin/operations/service/OperationsIncidentReportService.java`: snapshot/cutoff nhất quán, [start,cutoff), distinct RESOLVE, last event as-of theo seq, state!=CLOSED gồm carry-over, breakdown từng tập, không cộng ba tập làm tổng; lỗi nguồn không0giả.
- [X] T026 [US2] Sau T025, mở rộng `src/main/java/com/kizuna/features/admin/operations/controller/OperationsApiController.java` và `src/main/java/com/kizuna/features/admin/operations/controller/OperationsController.java`, tạo `src/main/resources/templates/admin/operations/reports.html` và UI trong `src/main/resources/static/js/features/admin/operations.js`: GET reports/incidents, ngày hai trường cùng có, default7ngày/max366/no future, labels/định nghĩa/cutoff; không usage/finance/export.
- [X] T027 [US2] Chạy health T020 sau T022–T024 và report T021 sau T025–T026/T036; ghi riêng phần đạt/chưa chạy vào `specs/011-system-admin-reporting/validation.md`; chưa chốt toàn US2 khi report còn đợi incident.

**Checkpoint**: Health có thể làm trước US3. Khi tới T025, chuyển làm US3 T028–T035 trước rồi quay lại report; không có dependency cycle.

## Phase 5: User Story 3 — Xử lý sự cố kỹ thuật (Priority: P2)

**Goal hiện hành**: Admin tạo thủ công, tự nhận/trả lại, ghi diễn biến, giải quyết/đóng và giữ lịch sử; thực hiện theo T081. Các task phân công cũ phía dưới chỉ là lịch sử.

**Independent Test**: Create→claim→progress→resolve→resume→resolve→close, reassign thu hồi quyền ghi cũ và event đúng thứ tự. Không cần collector/report hoặc policy documents.

### Tests

- [X] T028 [P] [US3] Viết `src/test/java/com/kizuna/operations/OperationsIncidentContractTest.java` cho toàn bộ incident routes, filter/page/limits/Unicode, admin+assignee, immutable severity/components, CLOSED cấm ghi và recurrence phải link hồ sơ cũ CLOSED.
- [X] T029 [P] [US3] Viết `src/test/java/com/kizuna/operations/OperationsIncidentConcurrencyTest.java` cho double-claim/update, stale409, reassign/progress/close race, recipient account revoke, event-write rollback và seq khi timestamps bằng nhau.

### Implementation

- [X] T030 [US3] Tạo `src/main/java/com/kizuna/features/admin/operations/dto/IncidentCreateRequest.java` và `src/main/java/com/kizuna/features/admin/operations/dto/IncidentCommandRequest.java`: “subject 1..200 ký tự không trắng”, “description 1..10.000 không trắng”, components một hoặc nhiều allowlist; progress/result/verification1..10.000 không trắng, reason reassign/resume1..2.000 không trắng; expectedVersion cho update, không nhận actor giả.
- [X] T031 [US3] Tạo `src/main/java/com/kizuna/features/admin/operations/entity/TechnicalIncident.java` và `src/main/java/com/kizuna/features/admin/operations/entity/TechnicalIncidentEvent.java`: UUID PK/FK, severity LOW/NORMAL/HIGH/CRITICAL bất biến, status NEW/IN_PROGRESS/RESOLVED/CLOSED, reporter bắt buộc, assignee “nullable trước claim”, recurrence_of “UUID self-FK nullable”, version @Version; event “UNIQUE(incident_id,sequence_no)” tăng dưới khóa, snapshot trạng thái/assignee/severity/components, actor/time/detail/reason/verification đúng giới hạn, append-only không secret/log thô.
- [X] T032 [US3] Tạo `src/main/java/com/kizuna/features/admin/operations/repository/TechnicalIncidentRepository.java`, `src/main/java/com/kizuna/features/admin/operations/repository/TechnicalIncidentEventRepository.java` và `src/main/java/com/kizuna/features/admin/operations/service/TechnicalIncidentService.java`: NEW create, claim→IN_PROGRESS, mọi admin reassign chưaCLOSED cho admin active khác có reason; chỉ assignee progress/resolve/resume/close; resolve có result+verification, CLOSED không reopen, recurrence hồ sơ mới; event/version/mutation nguyên tử và repository filter/locks/seq.
- [X] T033 [US3] Mở rộng `src/main/java/com/kizuna/features/admin/operations/controller/OperationsApiController.java` cho create/list/detail/events/claim/assignment/progress/resolve/resume/close theo contracts:201+Location cho create,200+version cho update, list20max50/createdAt desc,id desc; historyseq asc; DTO allowlist, không generic status write/remote command/auto-create.
- [X] T034 [US3] Mở rộng `src/main/java/com/kizuna/features/admin/operations/controller/OperationsController.java`, tạo `src/main/resources/templates/admin/operations/incidents.html` và `src/main/resources/templates/admin/operations/incident-detail.html`; form/filter/actions/history trong `src/main/resources/static/js/features/admin/operations.js`, link tại `src/main/resources/templates/admin/layout/sidebar.html`; stale giữ text, close xác nhận, quyền server không chỉ ẩn nút.
- [X] T035 [US3] Chạy T028–T029 sau T030–T034, đối chiếu state/event/rollback/no unrelated effects booking/payment/account/FE-10; ghi kết quả vào `specs/011-system-admin-reporting/validation.md`.
- [X] T036 [US3] Phối hợp T025–T026 chạy `src/test/java/com/kizuna/operations/OperationsIncidentReportTest.java` với fixture event cách ly và dữ liệu tạo/xử lý thật theo quickstart; so arithmetic kỳ quá khứ, nhiềuRESOLVE/RESUME/carry-over và xác nhận observer không tạo incident; ghi bằng chứng vào `specs/011-system-admin-reporting/validation.md`, rồi chốt report T027.

## Phase 6: User Story 4 — Đọc điều khoản và chính sách (Priority: P2)

**Goal**: Learner/tutor đọc đúng bản đã duyệt có hiệu lực, không có editor/publish API.

**Independent Test**: Fixture published/current,draft,future,withdrawn/overlap; đúng vai trò chỉ đọc current. Nghiệm thu nguồn thật cần approved corpus, không thay bằng nội dung AI tự viết.

### Tests

- [X] T037 [P] [US4] Viết `src/test/java/com/kizuna/operations/OperationsPolicyDocumentTest.java` cho role, effective interval, direct access draft/future/withdrawn denied, overlap conflict, empty/error, text escaped và không CRUD/editor endpoints.

### Implementation

- [X] T038 [US4] Tạo `src/main/java/com/kizuna/features/admin/operations/entity/PolicyDocumentVersion.java` và `src/main/java/com/kizuna/features/admin/operations/repository/PolicyDocumentVersionRepository.java`: UUID PK, “title 1..200 không trắng”, “version_label 1..100 không trắng”, approved_content không trắng, DRAFT/PUBLISHED/WITHDRAWN, effective_from bắt buộc/effective_to nullable exclusive>from, source_reference/approved_at; “UNIQUE(document_type,version_label)”, tối đa1bản hiệu lực/type, không entity serialization.
- [X] T039 [US4] Tạo `src/main/java/com/kizuna/features/admin/operations/service/PolicyDocumentReadService.java` và `src/main/java/com/kizuna/features/admin/operations/controller/PolicyDocumentController.java`: GET list/type PUBLISHED và from<=now<to, learner/tutor-only, no corpus empty/detail404, overlap409; không editor/phê duyệt/công bố/bắt ký lại.
- [X] T040 [US4] Viết `src/main/resources/db/changes/README-fe11-policy-import.md` cho nguồn duyệt, provenance/overlap transaction; ghi nguồn thật hoặc blocker vào `specs/011-system-admin-reporting/implementation-readiness.md`. Chỉ nạp khi có tài liệu/quyền phù hợp; thiếu nguồn vẫn hoàn thiện cơ chế đọc/empty nhưng không đánh dấu nghiệm thu nội dung thật đã đạt.
- [X] T041 [US4] Tạo `src/main/resources/templates/learner/policies/list.html`, `src/main/resources/templates/learner/policies/detail.html`, `src/main/resources/templates/tutor/policies/list.html`, `src/main/resources/templates/tutor/policies/detail.html`; gắn đúng view/menu shell thực tế và ghi path menu đã sửa vào `specs/011-system-admin-reporting/validation.md`; DESIGN mục7, escaped text, version/date, tiếng Việt, keyboard và empty/error riêng.
- [ ] T042 [US4] Chạy T037 và nghiệm thu theo `specs/011-system-admin-reporting/quickstart.md`; ghi riêng test fixture, actual approved content và phần blocked vào `specs/011-system-admin-reporting/validation.md`; không claim source acceptance khi chỉ có fixture.

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Đo toàn bộ luồng, giữ bằng chứng và không che phụ thuộc chưa hoàn tất.

- [X] T043 Rà mọi route/current role/account/CSRF và response không raw secret/log/key tại `src/test/java/com/kizuna/operations/OperationsAuthorizationTest.java`; gate DB-auth/health phải có quyết định rõ trước hoàn tất, không mở health payload công khai để qua test outage.
- [X] T044 [P] Tạo `src/test/java/com/kizuna/operations/OperationsViewsTest.java` cho labels/escape/role actions; kiểm thủ công keyboard/focus/responsive/DESIGN và menu đủ luồng, ghi ảnh/bằng chứng không PII vào `specs/011-system-admin-reporting/validation-ui.md`.
- [X] T045 [P] Tạo `src/test/load/operations.js` và `src/test/load/README-operations.md` cho100phiên/10phút trên1.000incident cóevent ở DB riêng; credentials qua env, error/p95 HTTP và browser instrumentation từ navigation/click đến nội dung thực hiển thị; không dùng HTTP latency thay SC-006.
- [X] T046 Chạy T045 và thử source timeout/pool cạn, đo p95hiển thị≤2s và lỗi≤5s, EXPLAIN queryas-of/khôngN+1; chỉ thêm index bằng migration mới khi có bằng chứng; ghi kết quả/giới hạn vào `specs/011-system-admin-reporting/validation-performance.md`.
- [X] T047 Đối chiếu code/migration/profile, cập nhật lệnh thật tại `specs/011-system-admin-reporting/quickstart.md` và hướng dẫn backup/restore/import tại `src/main/resources/db/changes/README-fe11.md`; không sửa FE-10 migrations đã áp dụng hoặc hứa workflow chưa có.
- [ ] T048 Chạy suite phù hợp và tổng hợp FR-001–018/SC-001–007 vào `specs/011-system-admin-reporting/validation.md`; khi có10người learner/tutor thử policy ghi ≥9người tìm đúng trong2phút. Thiếu người/môi trường/booking/corpus thì ghi chưa chạy/blocked, không đóng acceptance toàn tính năng.

## Dependencies & Execution Order

```text
Setup T001–T003 -> Foundation T004–T009
  -> US1 T010–T019 (phụ thuộc booking thật)
  -> US2 health T020–T024
  -> US3 T028–T035 (incident/events)
      -> US2 report T025–T026 + US3 T036
      -> US2 checkpoint T027
  -> US4 T037–T042 (approved corpus cho nghiệm thu nội dung)
  -> Polish T043–T048
```

- ID nhóm theo story, không bắt chạy T025 trước dependency T031–T032. US3 không chờ report, nên có thể tạo event trước rồi tích hợp report; không có vòng phụ thuộc.
- Setup: T001/T002 trước T004; T003 trước T005. Foundation T006/T009 khác file; T007 trước T008. Mọi story chờ foundation đạt.
- US1: T010/T011 song song → T012/T013 → T014 → T015 → T016 → T017/T018 → T019. Booking blocked không cản story khác nhưng không được tick T016/T019.
- US2: T020/T021 tests khác file; health T022→T023→T024, T023 chờ gate T001; report T025 chờ T031/T032, T026 sau T025; T027 phần report đợi T036. Không bỏ kiểm quyền vì DB outage.
- US3: T028/T029 trước implementation; T030/T031 → T032 → T033/T034 → T035. T036 sau T025/T026/T035 hoàn tất report end-to-end.
- US4: T037 → T038 → T039 → T040/T041 → T042. Thu thập nguồn duyệt có thể bắt đầu sớm, không chạy import trước schema.
- Task cùng OperationsApiController/OperationsController/operations.js hoặc file validation phải tuần tự. T044/T045 khác file có thể song song; T046 sau T045, T047 theo code thực, T048 chốt cuối bằng bằng chứng.

## Parallel Examples per Story

| Story | Task có thể song song sau đủ tiền đề | Điểm hợp nhất |
| --- | --- | --- |
| US1 | T010 contract / T011 transaction-snapshot tests | Trước implementation và T019 |
| US2 | T020 health tests / T021 report tests | Health riêng; report chờ event US3 |
| US3 | T028 contract / T029 concurrency tests | Trước T030–T035 |
| US4 | T037 tests / phần xác minh tài liệu duyệt của T040 | Trước source acceptance T042 |

Ví dụ là cơ hội chia công việc, không khởi chạy agent trong lượt tạo tasks. Phần import T040 vẫn chờ model/schema.

## Implementation Strategy

1. Giải quyết readiness/gate bảo mật, setup DB riêng và migration/policy trước.
2. MVP US1 chỉ hoàn chỉnh khi config được consumer booking thật dùng. Demo config/evaluator riêng phải ghi rõ chưa có E2E.
3. Health độc lập → incident → quay lại report lịch sử; không tạo dữ liệu giả để làm đầy dashboard.
4. Policy read-only có empty state, nguồn duyệt nạp theo quy trình tài liệu; không editor hoặc văn bản pháp lý tự tạo.
5. Nghiệm thu load/browser/quyền và hồi quy; chạy `$speckit-analyze` trước `$speckit-implement` theo constitution.

## Requirement Traceability

| Requirement | Tasks |
| --- | --- |
| FR-001 | T006–T008, T023, T033, T039, T043 |
| FR-002–003 | T010, T012–T014, T017–T019 |
| FR-004 | T011, T013–T016, T019 |
| FR-005 | T010, T014, T018–T019 |
| FR-006 | T009–T011, T014, T029, T032, T035 |
| FR-007–008 | T001, T020, T022–T024, T027 |
| FR-009–010 | T021, T025–T027, T031–T032, T036 |
| FR-011–014 | T028–T036 |
| FR-015 | T037–T042 |
| FR-016–017 | T009, T018, T023–T024, T026, T034, T039–T044 |
| FR-018 | T009, T011, T014, T020, T029, T032, T035, T043 |
| SC-001 | T010–T011, T016, T019 |
| SC-002 | T020, T022–T024, T027 |
| SC-003 | T021, T025–T027, T036 |
| SC-004 | T028–T036 |
| SC-005 | T006–T008, T037, T039, T043 |
| SC-006 | T045–T046 |
| SC-007 | T041–T042, T044, T048 |

## Notes

### Giao diện theo Figma MỚI ngày 2026-11-03

- [x] T066 Khung Quản trị viên/Quản lý theo Figma 1457:8800, 1457:8958, 1457:8628: header, menu vai trò, tab, dòng danh sách, hộp thoại nhân sự và ghi nhận sự cố (`frontend/styles/admin-portal.css`, `admin-shell.js`). Khung CS giữ nguyên. Các điểm giữ theo spec/rule ghi ở `validation-ui.md`.

### Rà soát nghiệp vụ ngày 2026-11-03

- [x] T062 Thêm mục “Chính sách” vào menu học viên/gia sư (`frontend/js/main.js`, menu trang hỗ trợ). Việc này mở đường vào trang, chưa thay T042/T048.
- [x] T063 Tạo nhật ký chung chỉ đọc: `GET /audit-log` và trang `frontend/pages/operations-logs/`, lọc theo nhóm, người thực hiện, ngày; sidebar “Nhật ký hệ thống” trỏ về đây.
- [x] T064 Trả địa chỉ gốc về trang chủ công khai theo FRONTEND_RULES; dashboard vào qua `/admin/dashboard`.
- [x] T065 Lệnh sự cố từ chối trường không thuộc thao tác để lịch sử bất biến không nhận nội dung ngoài kiểm tra.

### Bổ sung theo phạm vi nhóm đã duyệt ngày 2026-11-01

- [x] T056 Danh mục vai trò hiển thị đủ 6, bổ sung cờ staffAssignable; chỉ 4 vai trò nội bộ xuất hiện trong biểu mẫu nhân viên; kiểm API từ chối gán learner/tutor. Maven 6 test và browser với DB QA thật đạt; API cục bộ 8085 trả 6 vai trò/4 có thể gán.
- [x] T057 Hợp đồng báo cáo dùng cohort ngày tạo/trạng thái hiện tại, không snapshot cuối kỳ lịch sử; tỷ lệ null khi mẫu số0. Nguồn qua public service FE-04/05; kiểm bằng DB test có trạng thái và mốc ngày cụ thể.
- [x] T058 Triển khai API/UI báo cáo tổng hợp Admin/Manager, kiểm số liệu và quyền; Manager không đọc staff/cấu hình. Browser với hai tài khoản thật trên DB QA riêng đạt.
- [ ] T059 Để sau theo yêu cầu trực tiếp của người dùng: cấu hình tích hợp/giới hạn AI. Chưa thêm endpoint/schema/giá trị quota; cần chốt với FE-08 trước khi tiếp tục.
- [ ] T060 Tích hợp lối tắt thông báo với FE-12 khi có hợp đồng; xác nhận riêng yêu cầu xuất tệp trước khi triển khai.
- [x] T061 Mở Hồ sơ cá nhân từ sidebar/tên header; chỉ đọc hồ sơ của tài khoản nội bộ đang dùng. Kiểm quyền và thử tham số userId không xem được người khác; kiểm Admin/Manager và desktop/mobile.

- [x] T055 Theo yêu cầu mở thẳng dashboard để dùng như admin: trang nhân viên/vai trò thực, danh tính tên/ảnh DB, điều hướng và lối tắt hoạt động. Tạo/sửa/tìm, khóa/mở, gán vai trò và lịch sử atomic có kiểm quyền/version/CSRF. Kiểm Maven và browser qua DB cục bộ thật, xem `validation-staff-administration.md`.

- [x] T054 Hiển thị tên và vai trò của quản trị viên hiện tại từ DB trên khung quản trị, gồm phiên phát triển; kiểm thử người chưa đăng nhập và vai trò khác không đọc được `/me`.

- [x] T053 Theo yêu cầu bổ sung, lấy số đếm tài khoản từ DB hiện tại cho bốn thẻ dashboard và cơ cấu tài khoản; kiểm quyền, loại tài khoản đã xóa, phân biệt hoạt động/không hoạt động, không dùng dữ liệu giả hoặc thay lỗi bằng 0. Kiểm chứng tại `validation-account-summary.md`.

### Đồng bộ khung quản trị và chạy phát triển — 2026-11-01

- [x] T051 Tách khung sidebar/header/footer dùng chung và áp dụng cho dashboard, cấu hình, sức khỏe, danh sách/chi tiết sự cố, báo cáo. Kiểm menu active và desktop/mobile.
- [x] T052 Giới hạn phiên quản trị phát triển bằng profile, loopback, DB riêng; giữ kiểm quyền DB và CSRF. Thêm script chạy cùng hướng dẫn. Kiểm thử bảo mật và luồng trình duyệt với backend/DB cục bộ thật; xem `validation-admin-shell.md`.

### Bổ sung theo yêu cầu dashboard Figma ngày 2026-11-01

- [x] T049 Tạo `frontend/pages/admin-dashboard/index.html`, CSS riêng và JavaScript module theo Figma 1485:5490, dùng tài nguyên cục bộ và nguồn FE-11 thật; không bịa số liệu khi thiếu nguồn.
- [x] T050 Bổ sung liên kết tổng quan vào các trang vận hành, alias dev server; kiểm tra trình duyệt desktop/mobile, trạng thái rỗng/lỗi/quyền/timeout, lọc sự cố và dữ liệu quá hạn. Bằng chứng và giới hạn ở `validation-dashboard.md`.

Danh sách bao phủ công việc không chứng minh đã thực hiện. T001 gate health/auth, T016 booking và T040/T042 approved corpus cần bằng chứng riêng. Khi gate lộ xung đột spec/plan, ghi phương án sửa để chốt trước triển khai phần phụ thuộc; không tự giảm bảo mật hoặc sửa quy tắc bất biến.

## Trạng thái triển khai 2026-11-01

43/48 task đã thực hiện; T011/T016/T019 chờ consumer booking thật, T042 chờ tài liệu đã duyệt và T048 chờ nghiệm thu tổng thể. T011 hiện chỉ có evaluator subset. T046 đã ghi kết quả tải và các giới hạn, không đồng nghĩa SC-006 đã chốt. Xem validation.md, validation-ui.md và validation-performance.md. Không sửa marker trong checklists.

## Cập nhật 2026-11-10

- Xóa tài khoản nhân viên (xóa mềm, không đổi DB): `StaffAccountService.delete`, `StaffAccountRepository.softDelete/removeUpcomingShifts`, POST `/staff/{id}/delete`, nút "Xóa" và hộp xác nhận ở `pages/admin-staff`. Đã kiểm giao diện bằng trình duyệt với Admin; chưa thực hiện xóa trên dữ liệu thật, chưa có test tự động.
- Sidebar Quản lý thêm mục "Doanh thu & giao dịch" (`/pages/operations-revenue/`).
- `fetchAPI` tăng thời gian chờ chung 4,5 giây → 12 giây.
