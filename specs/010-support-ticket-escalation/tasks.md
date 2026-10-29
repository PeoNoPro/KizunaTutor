# Tasks: FE-10 — Support Ticket & Escalation

**Input**: `specs/001-support-ticket-escalation/` — spec.md, plan.md, research.md, data-model.md, contracts/tickets.md, quickstart.md; constitution 1.0.0.

**Prerequisites**: Đã chốt 5 câu trả lời nghiệp vụ. Giữ Java 17/Spring Boot/Thymeleaf/PostgreSQL; không dựng lại dự án.

**Tests**: Có yêu cầu kiểm chứng rõ trong spec (FR-001–021, SC-001–007) và constitution. Các task test bên dưới kiểm tra hợp đồng, phân quyền, transaction và luồng thực tế; viết trước phần triển khai tương ứng, xác nhận thất bại do hành vi chưa có rồi chạy lại sau triển khai. Không buộc test tài liệu hoặc test chỉ sao chép code.

**Organization**: Setup → nền tảng → US1/US2 (P1) → US3/US4 (P2) → nghiệm thu toàn bộ. Task chưa thực hiện đều để `[ ]`.

## Format: `[ID] [P?] [Story] Description`

- `[P]` là cơ hội làm đồng thời khi các điều kiện đầu vào đã hoàn tất và không sửa chung file; không phải cho phép bỏ qua phụ thuộc.
- Đường dẫn tương đối từ thư mục repo `KizunaTutor`; file mới ghi rõ ở mỗi task. Mọi file văn bản UTF-8 không BOM.
- Cặp file cùng task được sửa như một đơn vị; task có chung service/controller phải thực hiện tuần tự.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Chuẩn bị kiểm thử và triển khai SQL an toàn, không thay dữ liệu vận hành.

- [ ] T001 Ghi baseline schema thực tế hoặc schema kiểm thử và đối chiếu `DatabasePgAdmin4.txt`, mapping stub, FK/cascade, enum và dữ liệu legacy vào `specs/001-support-ticket-escalation/schema-preflight.md`; không chạy script có DROP CASCADE, không đưa dữ liệu cá nhân vào báo cáo; ghi rõ phần chưa kiểm tra nếu không có DB.
- [ ] T002 Tạo `src/test/resources/application-ticket-test.properties` và `src/test/java/com/kizuna/support/SupportTicketTestEnvironment.java` nhận TICKET_TEST_DB_URL/USERNAME/PASSWORD; từ chối thiếu biến hoặc DB không được đánh dấu kiểm thử, giữ fixture cách ly, không gọi mail/payment thật.
- [ ] T003 [P] Thêm dependency PDFBox tương thích Java 17 và Spring Boot trong `pom.xml`, chốt phiên bản sau xác minh tài liệu chính thức khi triển khai; không nâng cấp stack hoặc thêm nền tảng lưu trữ mới.
- [ ] T004 [P] Viết `src/main/resources/db/changes/README.md` với quy trình backup/restore đã thử, psql ON_ERROR_STOP, preflight, bảng phiên bản/khóa giao dịch và lệnh SQL tăng dần; không đưa credential thật vào tài liệu.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Dữ liệu, policy và giao dịch dùng chung. Hoàn tất trước các user story.

- [ ] T005 Viết `src/test/java/com/kizuna/support/SupportTicketMigrationTest.java` kiểm migration schema cũ có dữ liệu, giữ số ticket/message, mapping đúng, chạy lại không nhân bản, orphan/legacy không thể chuyển trung thực phải dừng; dùng PostgreSQL test riêng sau T001–T002.
- [ ] T006 Tạo `src/main/resources/db/changes/001_fe10_support_ticket.sql` sau T001/T004/T005: SQL tăng dần và bảng phiên bản có khóa; thêm ticket version, bảng audit/escalation/attachment cùng index/FK theo data-model; không DROP dữ liệu; preflight legacy, không bịa tác giả hoặc quyết định; chỉ áp dụng trên DB kiểm thử khi kiểm chứng.
- [ ] T007 Hoàn thiện `src/main/java/com/kizuna/entity/SupportTicket.java` và `src/main/java/com/kizuna/entity/TicketMessage.java`: UUID PK, `user_id` “FK users, bắt buộc; requester bất biến”, `assigned_to` “nullable trước khi nhận”, `escalated_to` “nullable; quản lý có quyền hiện tại, giữ sau return, thay khi chuyển tiếp”, `subject` “varchar(200), không trắng”, priority “LOW, MEDIUM mặc định, HIGH, URGENT”, status “OPEN mặc định, IN_PROGRESS, ESCALATED, RESOLVED, CLOSED”, category ACCOUNT/BILLING/TUTOR/BOOKING/MEMBERSHIP/TECHNICAL/OTHER, version “bigint not null default 0; @Version; mỗi mutation tăng version”; message_text “1–10.000 ký tự, không trắng”, sent_at bất biến; không kế thừa BaseEntity sai schema message. Map ba FK context nullable, created_at/updated_at và cột escalation legacy nội bộ theo data-model.
- [ ] T008 [P] Tạo `src/main/java/com/kizuna/entity/TicketAuditEvent.java` và `src/main/java/com/kizuna/repository/TicketAuditEventRepository.java`: UUID PK, ticket/actor FK, event_type, occurred_at, ticket_version, before_values/after_values JSONB, note nullable; chỉ append, không endpoint sửa/xóa và không ghi secret/binary.
- [ ] T009 Mở rộng `src/main/java/com/kizuna/repository/SupportTicketRepository.java` và `src/main/java/com/kizuna/repository/TicketMessageRepository.java` với query khóa ticket, version, pagination/sort ổn định, projection không binary và không entity serialization; phối hợp mapping T007.
- [ ] T010 [P] Viết `src/test/java/com/kizuna/support/SupportTicketPolicyTest.java` cho active/deleted/current DB role, owner isolation, CS read/reassign và manager-recipient; không dùng fallback display identity để cấp quyền.
- [ ] T011 Tạo `src/main/java/com/kizuna/service/support/SupportTicketAccessPolicy.java` và `src/main/java/com/kizuna/service/support/SupportTicketActorResolver.java` sau T010: reload user từ DB, kiểm ACTIVE/deletedAt/role mỗi thao tác kể cả tải tệp; owner chỉ phiếu mình, CS đọc tất cả, manager chỉ recipient hiện tại; trạng thái/assignee quyết định quyền ghi.
- [ ] T012 Tạo `src/main/java/com/kizuna/service/support/SupportTicketCommandExecutor.java` và `src/main/java/com/kizuna/service/support/SupportTicketAuditService.java`: expectedVersion bắt buộc, khóa theo ticket → escalation → message/attachment, audit cùng transaction, tăng version mọi mutation, stale trả conflict không tự retry; kiểm trạng thái người nhận trong transaction, không cập nhật một phần.
- [ ] T013 [P] Tạo `src/main/java/com/kizuna/exception/SupportTicketExceptionHandler.java` và `src/main/java/com/kizuna/dto/response/SupportTicketErrorResponse.java` cho 400/401/403/404/409/413 theo contracts; tiếng Việt, 404 chung cho không tồn tại/không có quyền đọc, không stack trace/exception/enum raw ra UI.
- [ ] T014 Viết `src/test/java/com/kizuna/support/SupportTicketSecurityTest.java`, rồi cập nhật `src/main/java/com/kizuna/config/SecurityConfig.java` chỉ cho FE-10: role-aware routes, CSRF header/token cho cookie-authenticated POST và multipart, token trên SSR; kiểm thiếu/sai/đúng CSRF và giữ hành vi endpoint ngoài FE-10.

**Checkpoint**: Migration test riêng đạt, policy/current account/CSRF có kiểm chứng; foundation chưa đồng nghĩa tính năng đã xong.

## Phase 3: User Story 1 — Gửi và theo dõi yêu cầu (Priority: P1) — MVP

**Goal**: Học viên/gia sư gửi, xem lại phiếu của mình, kèm tệp riêng tư tùy chọn.

**Independent Test**: Đăng nhập learner/tutor, gửi phiếu văn bản và 0–5 tệp, mở lại đúng nội dung; owner khác không xem được, lỗi file rollback toàn bộ. Không cần có CS hay escalation để chứng minh nhận yêu cầu.

### Tests

- [ ] T015 [P] [US1] Viết `src/test/java/com/kizuna/support/SupportTicketRequesterContractTest.java` cho create/list/detail/messages, giới hạn Unicode/blank/category, phân trang mặc định 20 tối đa 50 và sort; response owner vắng hẳn internal fields; kiểm role khác/owner giả và truy cập ngang (FR-001–004,012,016–017).
- [ ] T016 [P] [US1] Viết `src/test/java/com/kizuna/support/SupportTicketAttachmentTest.java` cho JPG/JPEG/PNG/PDF hợp lệ, sai MIME/đuôi/content, file hỏng, PDF không mở được, đúng/vượt 10.000.000 byte, 5/6 tệp, direct download sai quyền, header và rollback khi một file lỗi (FR-020–021).

### Implementation

- [ ] T017 [US1] Hoàn thiện `src/main/java/com/kizuna/dto/request/SupportTicketCreateRequest.java`, tạo `src/main/java/com/kizuna/dto/response/SupportTicketRequesterResponse.java` và `src/main/java/com/kizuna/dto/response/SupportTicketPageResponse.java`: subject 1–200, message 1–10.000 Unicode code points không trắng, category bắt buộc, chỉ context IDs nullable; DTO không nhận requester/role, không có internal fields, metadata file không bytes.
- [ ] T018 [US1] Tạo `src/main/java/com/kizuna/entity/TicketAttachment.java` và `src/main/java/com/kizuna/repository/TicketAttachmentRepository.java`: UUID PK, message FK bắt buộc cùng ticket qua composite FK/ràng buộc tương đương, uploader FK, original_name “varchar(255) đã làm sạch để hiển thị”, content_type image/jpeg/image/png/application/pdf, size_bytes “1..10.000.000”, created_at và content bytea; metadata query tuyệt đối không select binary; JDBC content query riêng.
- [ ] T019 [US1] Tạo `src/main/java/com/kizuna/service/support/TicketAttachmentValidator.java`: tối đa 5 tệp, mỗi tệp 1..10.000.000 byte, extension/MIME/content tương ứng, ImageIO và parser PDFBox, kiểm đọc cấu trúc và giới hạn tài nguyên; reject malformed/encrypted không xác minh được; không chỉ tin header, không dùng tên file làm path.
- [ ] T020 [US1] Cập nhật multipart trong `src/main/resources/application.properties` và thêm `src/main/java/com/kizuna/service/support/TicketAttachmentService.java`: request tối đa 52.000.000 byte, spill-to-temp thay vì load cả request vào RAM; kiểm tất cả tệp trước commit, bytes/metadata/message/ticket/audit chung transaction; lỗi lưu rollback tất cả, không public URL.
- [ ] T021 [US1] Tạo `src/main/java/com/kizuna/service/support/SupportTicketContextValidator.java` với ownership SQL projection theo data-model: participant booking, tutor sở hữu membership, user sở hữu payment có plan hội viên; đối chiếu liên kết đồng thời không mâu thuẫn, không tin stub entity thiếu owner; không sửa dữ liệu booking/payment/membership. Hoàn tất trước chốt T022.
- [ ] T022 [US1] Mở rộng `src/main/java/com/kizuna/service/ISupportTicketService.java` và `src/main/java/com/kizuna/service/impl/SupportTicketServiceImpl.java` cho create OPEN/MEDIUM, first public message, list/detail/messages owner-only, reload và private download; từ chối context ngoài quyền trước lưu; dùng context validator T021 ngay cả khi UI chọn context đến US4 mới hoàn thiện.
- [ ] T023 [US1] Hoàn thiện `src/main/java/com/kizuna/controller/SupportTicketController.java` cho POST/GET base, GET detail/messages và GET attachment theo contracts; multipart payload/files, 201 Location, version/allowedActions, authorized download attachment/nosniff/no-store, không serialize entity.
- [ ] T024 [US1] Tạo `src/main/java/com/kizuna/controller/support/SupportTicketPageController.java`, `src/main/resources/templates/learner/support/tickets.html`, `src/main/resources/templates/learner/support/ticket-detail.html`, `src/main/resources/templates/tutor/support/tickets.html` và `src/main/resources/templates/tutor/support/ticket-detail.html`; kết nối dữ liệu thật, create/list/detail/attachments, shell hiện có, learner/tutor đúng DESIGN mục 7.
- [ ] T025 [US1] Tạo `src/main/resources/templates/fragments/support/ticket-thread.html`, `src/main/resources/static/js/support-tickets.js` và `src/main/resources/static/css/support-tickets.css` cho render escaped, loading/empty/error khác nhau, CSRF/version/multipart, nhãn tiếng Việt có dấu, giữ text khi lỗi và chọn lại tệp khi cần; nút/field label và keyboard/focus phù hợp.
- [ ] T026 [US1] Chạy T015/T016 trên PostgreSQL kiểm thử, kiểm create rollback/audit và mở lại file; ghi kết quả Story 1, lỗi/chưa chạy trung thực vào `specs/001-support-ticket-escalation/validation.md` sau T017–T025.

**Checkpoint**: US1 là MVP tiếp nhận/theo dõi, chưa tuyên bố đã xử lý hay chuyển cấp đầy đủ.

## Phase 4: User Story 2 — Nhân viên xử lý và chủ phiếu đóng (Priority: P1)

**Goal**: Nhận/phân công lại, trao đổi, ưu tiên, giải quyết; chỉ chủ phiếu xác nhận đóng.

**Independent Test**: Với phiếu fixture hoặc phiếu US1, CS1 nhận, CS2 phân công lại cho mình; CS1 mất quyền ghi, owner bổ sung sau resolve mở lại, chỉ owner đóng. Không cần US3/US4.

### Tests

- [ ] T027 [P] [US2] Viết `src/test/java/com/kizuna/support/SupportTicketHandlingContractTest.java` cho hàng đợi/filter, claim, assignment, priority, public message+files, resolve và owner-only close; mọi CS được chuyển nhau nhưng không được phản hồi thay assignee, không auto-close (FR-004–008,013).
- [ ] T028 [P] [US2] Viết `src/test/java/com/kizuna/support/SupportTicketConcurrencyTest.java` cho double-claim, stale version, close/message, assignment/reply và lỗi audit/file làm rollback; phân biệt 409 conflict với forbidden và không retry lệnh âm thầm (FR-018).

### Implementation

- [ ] T029 [US2] Tạo `src/main/java/com/kizuna/dto/request/SupportTicketHandlingRequest.java` và `src/main/java/com/kizuna/dto/response/SupportTicketStaffResponse.java`: expectedVersion số nguyên không âm bắt buộc, assigneeId/priority theo từng command, resolutionText/message 1–10.000 ký tự không trắng; nội bộ tách DTO owner, audit không secret.
- [ ] T030 [US2] Bổ sung trong `src/main/java/com/kizuna/service/impl/SupportTicketServiceImpl.java` claim OPEN → IN_PROGRESS, mọi CS reassign phiếu có assignee chưa CLOSED, priority LOW/MEDIUM/HIGH/URGENT, public reply có tệp, audit và transaction; filter/pagination hàng đợi và GET active CS candidates; giữ trạng thái khi phân công lại.
- [ ] T031 [US2] Thực hiện resolve và close tại `src/main/java/com/kizuna/service/impl/SupportTicketServiceImpl.java`: assignee gửi kết quả công khai + IN_PROGRESS → RESOLVED nguyên tử; chỉ owner RESOLVED → CLOSED, không auto-close; owner bổ sung RESOLVED → IN_PROGRESS giữ assignee/lịch sử, CLOSED cấm mọi ghi. Trong giai đoạn này từ chối chuyển cấp chưa triển khai; US3 thêm các transition tương ứng.
- [ ] T032 [US2] Mở rộng `src/main/java/com/kizuna/controller/SupportTicketController.java` cho claim/assignment/priority/messages/resolve/close/assignees/history đúng contracts, expectedVersion mọi mutation, history chỉ staff và manager có quyền; cập nhật allowedActions từ policy.
- [ ] T033 [US2] Tạo `src/main/resources/templates/cs/support/tickets.html`, `src/main/resources/templates/cs/support/ticket-detail.html`, mở rộng `src/main/java/com/kizuna/controller/support/SupportTicketPageController.java` và `src/main/resources/static/js/support-tickets.js`: hàng đợi/filter, nhận/chuyển CS, ưu tiên, reply+files, resolve; requester có xác nhận đóng và bổ sung mở lại; xử lý stale giữ text.
- [ ] T034 [US2] Tích hợp menu và active-ticket count trong `src/main/resources/templates/cs/layout/sidebar.html`, `src/main/resources/templates/cs/dashboard.html` và `src/main/java/com/kizuna/controller/cs/CsController.java`: đếm OPEN/IN_PROGRESS/ESCALATED bằng dữ liệu thật, không coi tất cả phiếu là đang xử lý; sửa nhãn kỹ thuật/all-caps thuộc phần hỗ trợ bị sửa.
- [ ] T035 [US2] Chạy T027/T028 và hồi quy US1, kiểm tệp trong phản hồi CS; ghi checkpoint US2 và bằng chứng quyền đóng vào `specs/001-support-ticket-escalation/validation.md`.

## Phase 5: User Story 3 — Chuyển cấp và quyết định quản lý (Priority: P2)

**Goal**: Chuyển quản lý cụ thể, lưu quyết định, trả assignee; đổi manager có lý do và thu hồi quyền người cũ.

**Independent Test**: Dùng phiếu IN_PROGRESS có assignee từ fixture/US2; chuyển manager1, ghi quyết định, đổi manager2, kiểm manager1 mất quyền tệp/chi tiết và manager2 phải có quyết định mới; return về CS hiện tại, owner không thấy nội bộ.

### Tests

- [ ] T036 [P] [US3] Viết `src/test/java/com/kizuna/support/SupportTicketEscalationContractTest.java` cho chuyển/lý do/tóm tắt, private manager list/detail, decision khác return, đổi manager chỉ assignee, quyền manager cũ bị thu hồi, quyết định theo revision, CS đổi khi pending và secrecy owner (FR-009–013,019).
- [ ] T037 [P] [US3] Viết `src/test/java/com/kizuna/support/SupportTicketEscalationConcurrencyTest.java` cho unique pending, stale decision/return khi đổi recipient, transaction rollback và race account ngừng hoạt động; PostgreSQL thật và không mất lịch sử.

### Implementation

- [ ] T038 [US3] Tạo `src/main/java/com/kizuna/entity/TicketEscalation.java` và `src/main/java/com/kizuna/repository/TicketEscalationRepository.java`: UUID PK/FK bắt buộc, reason/handling_summary “text 1–10.000 ký tự không trắng”, returned_at nullable, pending boolean, recipient_revision khởi đầu 0, decision_text nullable nhưng khi có 1–10.000 không trắng, decision_by/decision_revision/decided_at nullable; unique partial ticket WHERE pending=true; quyết định cũ snapshot audit trước reset.
- [ ] T039 [US3] Tạo `src/main/java/com/kizuna/dto/request/SupportTicketEscalationRequest.java` và `src/main/java/com/kizuna/dto/response/SupportTicketManagerResponse.java` cho managerId/expectedVersion, reason/handlingSummary/decisionText có giới hạn như T038; manager projection chỉ recipient hiện tại, không generic entity JSON.
- [ ] T040 [US3] Mở rộng `src/main/java/com/kizuna/service/impl/SupportTicketServiceImpl.java`: assignee IN_PROGRESS → ESCALATED chọn manager active; giữ CS, pending duy nhất, audit; managers candidates có role/status đúng; current manager lưu decision riêng rồi return ESCALATED → IN_PROGRESS khi decision_by/revision khớp, không gửi công khai/đóng.
- [ ] T041 [US3] Thêm đổi recipient trong `src/main/java/com/kizuna/service/impl/SupportTicketServiceImpl.java`: chỉ assignee khi ESCALATED, lý do 1–10.000 không trắng, manager active khác người cũ; revision tăng, lưu history cũ, bỏ decision hiện hành, chỉ manager mới đọc/tải được; return dùng assignee mới nếu CS đổi lúc chờ.
- [ ] T042 [US3] Hoàn thiện policy recipient và race trong `src/main/java/com/kizuna/service/support/SupportTicketAccessPolicy.java` cùng `src/main/java/com/kizuna/service/support/TicketAttachmentService.java`: giữ quyền current manager sau return đến khi thay, không tin token role cũ hoặc URL tải cũ; lịch sử/audit không đi vào owner DTO; khóa/kiểm quyền nhất quán mỗi download.
- [ ] T043 [US3] Mở rộng `src/main/java/com/kizuna/controller/SupportTicketController.java` cho GET managers, POST escalations/current recipient/current decision/current return và manager-scoped GET list/detail/history; 409 stale, không mở endpoint ghi status tùy ý.
- [ ] T044 [US3] Tạo `src/main/resources/templates/manager/support/tickets.html`, `src/main/resources/templates/manager/support/ticket-detail.html`, mở rộng CS detail tại `src/main/resources/templates/cs/support/ticket-detail.html`, page controller tại `src/main/java/com/kizuna/controller/support/SupportTicketPageController.java` và `src/main/resources/static/js/support-tickets.js`: chuyển/đổi quản lý, decision/return riêng, nội bộ tách biệt; kiểm tra thực tế shell manager và gắn link tại template navigation đang dùng, ghi path đã sửa vào validation.
- [ ] T045 [US3] Chạy T036/T037 và hồi quy US1–US2; kiểm URL tệp đã biết sau đổi quản lý, manager cũ/manager khác không thấy phiếu; ghi kết quả Story 3 vào `specs/001-support-ticket-escalation/validation.md`.

## Phase 6: User Story 4 — Hỗ trợ đặt học thử và kết nối gia sư (Priority: P2)

**Goal**: Context hỗ trợ đủ để tư vấn, filter yêu cầu booking/kết nối, không thực thi nghiệp vụ bên ngoài ticket.

**Independent Test**: Dùng phiếu booking/kết nối fixture; lọc, mở context hợp lệ, gửi hướng dẫn; kiểm booking/payment/membership không đổi. Chuyển cấp dùng lại US3, không tạo luồng thứ hai.

### Tests

- [ ] T046 [P] [US4] Viết `src/test/java/com/kizuna/support/SupportTicketContextTest.java` cho booking participant, tutor membership, payment hội viên của đúng owner, context người khác/không còn tồn tại và liên kết mâu thuẫn; so sánh trạng thái đối tượng trước/sau xử lý, không phụ thuộc stub entity thiếu owner (FR-014–015).
- [ ] T047 [P] [US4] Viết `src/test/java/com/kizuna/support/SupportTicketConnectionSupportTest.java` cho filter BOOKING/TUTOR, tư vấn và chuyển cấp cùng phiếu, không tạo booking/kết nối/hoàn tiền/cấp hội viên.

### Implementation

- [ ] T048 [US4] Tạo `src/main/java/com/kizuna/service/support/SupportTicketRelatedContextService.java` và `src/main/java/com/kizuna/repository/SupportTicketContextRepository.java` tái sử dụng validator T021: SQL projection đối chiếu schema thật cho context candidates phân trang/chi tiết tối thiểu, user ownership và payment plan hội viên; missing trả unavailable, không bịa dữ liệu hoặc lộ bank/gateway payload.
- [ ] T049 [US4] Mở rộng `src/main/java/com/kizuna/controller/SupportTicketController.java` cho GET related-contexts và GET ticket related-context đúng hợp đồng; owner candidates không cho nhập userId để chọn dữ liệu người khác, staff/manager chỉ context của phiếu được phép.
- [ ] T050 [US4] Tạo `src/main/resources/templates/fragments/support/related-context.html`, tích hợp vào form/detail tại `src/main/resources/templates/learner/support/tickets.html`, `src/main/resources/templates/tutor/support/tickets.html`, `src/main/resources/templates/cs/support/ticket-detail.html`, `src/main/resources/templates/manager/support/ticket-detail.html` và `src/main/resources/static/js/support-tickets.js`: chọn context tùy chọn từ nguồn thật, filter booking/kết nối và hướng dẫn, không có nút thực thi hoàn tiền/đổi lịch ngoài phạm vi.
- [ ] T051 [US4] Chạy T046/T047 cùng kiểm chứng Story 4 trong `specs/001-support-ticket-escalation/quickstart.md`; ghi bằng chứng đối tượng liên quan không bị đổi và chuyển cấp giữ nguyên phiếu vào `specs/001-support-ticket-escalation/validation.md`.

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Kiểm chứng xuyên suốt, hiệu năng, tài liệu vận hành; không thêm tính năng ngoài spec.

- [ ] T052 Kiểm đầy đủ ma trận quyền của mọi GET/POST/tệp, account/role revocation và CSRF bằng `src/test/java/com/kizuna/support/SupportTicketSecurityTest.java`; bổ sung test dữ liệu nội bộ vắng khỏi response owner, xung đột concurrency còn thiếu, không chỉ kiểm ẩn nút.
- [ ] T053 [P] Tạo `src/test/java/com/kizuna/support/SupportTicketViewsTest.java` cho SSR label/escaped content/empty/error/role buttons; kiểm thủ công responsive, keyboard/focus, sentence case và learner/tutor DESIGN mục 7, ghi ảnh/bằng chứng không có dữ liệu nhạy cảm vào `specs/001-support-ticket-escalation/validation-ui.md`.
- [ ] T054 [P] Tạo `src/test/load/support-tickets.js` và `src/test/load/README.md` cho 100 phiên/10 phút trên 1.000 phiếu có trao đổi, credentials qua môi trường, dataset chỉ test; đo p95 list/detail ≤2 giây, error và đối chiếu mutation/audit, không nhận API fail là thành công.
- [ ] T055 Chạy load T054 trên DB kiểm thử, xem EXPLAIN để điều chỉnh index trong `src/main/resources/db/changes/002_fe10_query_indexes.sql` chỉ khi đo chứng minh cần; kiểm metadata không select binary/N+1 và ghi p95, dung lượng/backup impact vào `specs/001-support-ticket-escalation/validation-performance.md`.
- [ ] T056 Rà lại migration/backup/restore/legacy URL không public và vận hành bằng `src/main/resources/db/changes/README.md`; xác nhận script SQL thật, profile ticket-test và lệnh Maven trong `specs/001-support-ticket-escalation/quickstart.md` khớp code sau triển khai, ghi phần bị chặn thay vì bịa kết quả.
- [ ] T057 Chạy toàn bộ test phù hợp theo `specs/001-support-ticket-escalation/quickstart.md`, tổ chức nghiệm thu 10 người cho SC-001 khi có người tham gia và tổng hợp FR-001–021/SC-001–007 vào `specs/001-support-ticket-escalation/validation.md`; chưa có người hoặc môi trường thì đánh dấu chưa chạy, không đóng task này như đã đạt.

## Dependencies & Execution Order

### Phase Dependencies

```text
Setup (T001–T004)
  -> Foundation (T005–T014)
    -> US1 (T015–T026): MVP nhận và theo dõi
      -> US2 (T027–T035): xử lý, owner đóng
        -> US3 (T036–T045): chuyển cấp
        -> US4 (T046–T051): context/tư vấn, chuyển cấp tích hợp sau US3
          -> Polish (T052–T057)
```

Đây là phụ thuộc triển khai thật, không tuyên bố mọi story hoàn toàn độc lập về mã nguồn. Mỗi story có thể được nghiệm thu riêng bằng fixture test hợp lệ sau các tiền đề của nó. US4 context có thể bắt đầu sau US2 nhưng phần chuyển cấp nghiệm thu sau US3. Các task dùng chung service/controller/JS không làm đồng thời.

### Detailed Gates

- T006 sau T001/T004/T005; T007 theo migration; T008 có thể song song T007, T010/T013 độc lập sau setup; T009 sau T007, T011 sau T010, T012 sau repository/policy/audit, T014 kiểm chứng trước mở endpoint.
- US1: T015/T016 song song; T017–T020 triển khai theo phụ thuộc; T021 validator context hoàn tất trước T022 service, cả hai trước T023. UI T024–T025 sau hợp đồng ổn định, T026 chốt toàn story.
- US2: T027/T028 trước implementation; T029 → T030 → T031 → T032 → T033 → T034 → T035. US1 đã cung cấp attachment cho reply.
- US3: T036/T037 trước implementation; T038/T039 → T040 → T041 → T042 → T043 → T044 → T045. T041 không dùng quyết định manager cũ cho người mới.
- US4: T046/T047 trước implementation; T048 sau validator US1; T049 → T050 → T051. Không sửa entity/payment service ngoài projection cần thiết.
- T052/T053/T054 có thể tách file; T055 sau T054, T056 sau migration thực; T057 chốt bằng bằng chứng và mọi blocker rõ ràng.

## Parallel Examples per Story

| Story | Công việc song song sau khi có tiền đề | Điểm hợp nhất |
| --- | --- | --- |
| US1 | T015 requester contract và T016 attachment tests | Trước T017–T023 và chạy T026 |
| US2 | T027 handling contract và T028 concurrency tests | Trước T029–T032 và chạy T035 |
| US3 | T036 escalation contract và T037 escalation concurrency | Trước T038–T043 và chạy T045 |
| US4 | T046 context authorization và T047 connection support | Trước T048–T050 và chạy T051 |

Các ví dụ là hướng dẫn thực thi, không tự khởi chạy agent hay task trong lượt tạo tài liệu.

## Implementation Strategy

1. Hoàn tất setup/foundation, chạy kiểm chứng baseline/migration trước mở luồng người dùng.
2. Giao MVP US1 có văn bản, tệp riêng tư và theo dõi; demo nhận yêu cầu, chưa quảng bá luồng xử lý chưa làm.
3. Bổ sung US2 để có vòng hỗ trợ hoàn chỉnh: nhân viên giải quyết, người gửi đóng.
4. Bổ sung US3 cho quyết định quản lý rồi US4 cho context/tư vấn cụ thể; kiểm hồi quy các story trước.
5. Nghiệm thu xuyên suốt và hiệu năng; dùng `$speckit-analyze` trước `$speckit-implement` theo constitution. Không tự commit/push/publish do việc tạo tasks.

## Requirement Traceability

| Spec | Task chính |
| --- | --- |
| FR-001–003 | T015, T017, T022–T026 |
| FR-004 | T009, T015, T027, T030, T033, T043 |
| FR-005–008 | T027–T035 |
| FR-009–011 | T036–T045 |
| FR-012–013 | T008, T011–T013, T015, T032, T036, T040–T045, T052 |
| FR-014–015 | T021, T046–T051 |
| FR-016–017 | T010–T014, T023–T025, T033–T034, T042, T052–T053 |
| FR-018 | T012, T028, T035, T037, T045 |
| FR-019 | T036–T045 |
| FR-020–021 | T003, T016, T018–T020, T023, T027, T042, T052 |
| SC-001–007 | T026, T035, T045, T051–T057 |

## Notes

- Không điền dấu hoàn tất cho công việc chỉ mới lên kế hoạch. Các task test có thể tạo fixture trong DB cách ly, không chèn mock vào vận hành.
- Ràng buộc chi tiết vẫn nằm trong data-model/contracts; task không cho phép bỏ kiểm quyền/tệp/transaction để giao diện chạy được.
- Không tạo tasks.md thứ hai, không sửa nguồn ứng dụng hoặc tự triển khai trong lượt `$speckit-tasks` này.
