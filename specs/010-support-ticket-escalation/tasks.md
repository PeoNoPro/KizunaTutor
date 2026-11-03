# Tasks: FE-10 — Support Ticket & Escalation

**Input**: `specs/010-support-ticket-escalation/` — spec.md, plan.md, research.md, data-model.md, contracts/tickets.md, quickstart.md; constitution 1.0.0.

**Prerequisites**: Đã chốt 5 câu trả lời nghiệp vụ. Giữ Java 17/Spring Boot/PostgreSQL và frontend HTML/CSS/ES modules hiện tại; không dựng lại dự án. Ánh xạ đường dẫn và gom lớp kỹ thuật theo [implementation-analysis.md](implementation-analysis.md), không đổi nghiệp vụ.

**Tests**: Có yêu cầu kiểm chứng rõ trong spec (FR-001–021, SC-001–007) và constitution. Các task test bên dưới kiểm tra hợp đồng, phân quyền, transaction và luồng thực tế; viết trước phần triển khai tương ứng, xác nhận thất bại do hành vi chưa có rồi chạy lại sau triển khai. Không buộc test tài liệu hoặc test chỉ sao chép code.

**Organization**: Setup → nền tảng → US1/US2 (P1) → US3/US4 (P2) → nghiệm thu toàn bộ. Task chưa thực hiện đều để `[ ]`. Đã kiểm chứng 55/57 task; T055 (tải 100 trình duyệt/10 phút) và T057 (nghiệm thu 10 người) còn mở. Xem validation.md; không coi SQL/HTTP timing là nghiệm thu render.

## Format: `[ID] [P?] [Story] Description`

- `[P]` là cơ hội làm đồng thời khi các điều kiện đầu vào đã hoàn tất và không sửa chung file; không phải cho phép bỏ qua phụ thuộc.
- Đường dẫn tương đối từ thư mục repo `KizunaTutor`; file mới ghi rõ ở mỗi task. Mọi file văn bản UTF-8 không BOM.
- Cặp file cùng task được sửa như một đơn vị; task có chung service/controller phải thực hiện tuần tự.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Chuẩn bị kiểm thử và triển khai SQL an toàn, không thay dữ liệu vận hành.

- [X] T001 Ghi baseline schema thực tế hoặc schema kiểm thử và đối chiếu `DatabasePgAdmin4.txt`, mapping stub, FK/cascade, enum và dữ liệu legacy vào `specs/010-support-ticket-escalation/schema-preflight.md`; không chạy script có DROP CASCADE, không đưa dữ liệu cá nhân vào báo cáo; ghi rõ phần chưa kiểm tra nếu không có DB.
- [X] T002 Tạo `backend/src/test/resources/application-ticket-test.properties` và `backend/src/test/java/com/kizuna/support/SupportTicketTestEnvironment.java` nhận TICKET_TEST_DB_URL/USERNAME/PASSWORD; từ chối thiếu biến hoặc DB không được đánh dấu kiểm thử, giữ fixture cách ly, không gọi mail/payment thật.
- [X] T003 [P] Thêm dependency PDFBox tương thích Java 17 và Spring Boot trong `backend/pom.xml`, chốt phiên bản sau xác minh tài liệu chính thức khi triển khai; không nâng cấp stack hoặc thêm nền tảng lưu trữ mới.
- [X] T004 [P] Viết `database/migrations/README-fe10.md` với quy trình backup/restore đã thử, psql ON_ERROR_STOP, preflight, bảng phiên bản/khóa giao dịch và lệnh SQL tăng dần; không đưa credential thật vào tài liệu.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Dữ liệu, policy và giao dịch dùng chung. Hoàn tất trước các user story.

- [X] T005 Viết `backend/src/test/java/com/kizuna/support/SupportTicketMigrationTest.java` kiểm migration schema cũ có dữ liệu, giữ số ticket/message, mapping đúng, chạy lại không nhân bản, orphan/legacy không thể chuyển trung thực phải dừng; dùng PostgreSQL test riêng sau T001–T002. Kiểm backfill sequence_no giữ nguyên message và ràng buộc unique.
- [X] T006 Tạo `database/seeds/fe10_support_ticket.sql` sau T001/T004/T005: SQL tăng dần và bảng phiên bản có khóa; thêm ticket version, bảng audit/escalation/attachment cùng index/FK theo data-model; không DROP dữ liệu; preflight legacy, không bịa tác giả hoặc quyết định; chỉ áp dụng trên DB kiểm thử khi kiểm chứng. Thêm sequence_no NOT NULL và unique(ticket_id, sequence_no), backfill theo data-model.
- [X] T007 Hoàn thiện `backend/src/main/java/com/kizuna/features/ticket/entity/SupportTicket.java` và `backend/src/main/java/com/kizuna/features/ticket/entity/TicketMessage.java`: UUID PK, `user_id` “FK users, bắt buộc; requester bất biến”, `assigned_to` “nullable trước khi nhận”, `escalated_to` “nullable; quản lý có quyền hiện tại, giữ sau return, thay khi chuyển tiếp”, `subject` “varchar(200), không trắng”, priority “LOW, MEDIUM mặc định, HIGH, URGENT”, status “OPEN mặc định, IN_PROGRESS, ESCALATED, RESOLVED, CLOSED”, category ACCOUNT/BILLING/TUTOR/BOOKING/MEMBERSHIP/TECHNICAL/OTHER, version “bigint not null default 0; @Version; mỗi mutation tăng version”; message_text “1–10.000 ký tự, không trắng”, sent_at bất biến; không kế thừa BaseEntity sai schema message. Map ba FK context nullable, created_at/updated_at và cột escalation legacy nội bộ theo data-model. Ánh xạ sequence_no bigint bắt buộc.
- [X] T008 [P] Tạo `backend/src/main/java/com/kizuna/features/ticket/entity/TicketAuditEvent.java` và `backend/src/main/java/com/kizuna/features/ticket/repository/TicketAuditEventRepository.java`: UUID PK, ticket/actor FK, event_type, occurred_at, ticket_version, before_values/after_values JSONB, note nullable; chỉ append, không endpoint sửa/xóa và không ghi secret/binary.
- [X] T009 Mở rộng `backend/src/main/java/com/kizuna/features/ticket/repository/SupportTicketRepository.java` và `backend/src/main/java/com/kizuna/features/ticket/repository/TicketMessageRepository.java` với query khóa ticket, version, pagination/sort ổn định, projection không binary và không entity serialization; phối hợp mapping T007. Query hội thoại ORDER BY sequence_no ASC.
- [X] T010 [P] Viết `backend/src/test/java/com/kizuna/support/SupportTicketPolicyTest.java` cho active/deleted/current DB role, owner isolation, CS read/reassign và manager-recipient; không dùng fallback display identity để cấp quyền.
- [X] T011 Tạo `backend/src/main/java/com/kizuna/features/ticket/service/SupportTicketAccessPolicy.java` và `backend/src/main/java/com/kizuna/features/ticket/service/SupportTicketActorResolver.java` sau T010: reload user từ DB, kiểm ACTIVE/deletedAt/role mỗi thao tác kể cả tải tệp; owner chỉ phiếu mình, CS đọc tất cả, manager chỉ recipient hiện tại; trạng thái/assignee quyết định quyền ghi.
- [X] T012 Tạo `backend/src/main/java/com/kizuna/features/ticket/service/SupportTicketCommandExecutor.java` và `backend/src/main/java/com/kizuna/features/ticket/repository/TicketAuditEventRepository.java`: expectedVersion bắt buộc, khóa theo ticket → escalation → message/attachment, audit cùng transaction, tăng version mọi mutation, stale trả conflict không tự retry; kiểm trạng thái người nhận trong transaction, không cập nhật một phần. Cấp sequence_no dưới khóa ticket trong cùng transaction.
- [X] T013 [P] Tạo `backend/src/main/java/com/kizuna/core/exception/GlobalExceptionHandler.java` và `backend/src/main/java/com/kizuna/core/exception/GlobalExceptionHandler.java` cho 400/401/403/404/409/413 theo contracts; tiếng Việt, 404 chung cho không tồn tại/không có quyền đọc, không stack trace/exception/enum raw ra UI.
- [X] T014 Viết `backend/src/test/java/com/kizuna/support/SupportTicketWorkflowTest.java`, rồi cập nhật `backend/src/main/java/com/kizuna/core/security/SecurityConfig.java` chỉ cho FE-10: role-aware routes, CSRF header/token cho cookie-authenticated POST và multipart, token qua endpoint xác thực cho frontend tách riêng; kiểm thiếu/sai/đúng CSRF và giữ hành vi endpoint ngoài FE-10.

**Checkpoint**: Migration test riêng đạt, policy/current account/CSRF có kiểm chứng; foundation chưa đồng nghĩa tính năng đã xong.

## Phase 3: User Story 1 — Gửi và theo dõi yêu cầu (Priority: P1) — MVP

**Goal**: Học viên/gia sư gửi, xem lại phiếu của mình, kèm tệp riêng tư tùy chọn.

**Independent Test**: Đăng nhập learner/tutor, gửi phiếu văn bản và 0–5 tệp, mở lại đúng nội dung; owner khác không xem được, lỗi file rollback toàn bộ. Không cần có CS hay escalation để chứng minh nhận yêu cầu.

### Tests

- [X] T015 [P] [US1] Viết `backend/src/test/java/com/kizuna/support/SupportTicketWorkflowTest.java` cho create/list/detail/messages, giới hạn Unicode/blank/category, phân trang mặc định 20 tối đa 50 và sort; response owner vắng hẳn internal fields; kiểm role khác/owner giả và truy cập ngang (FR-001–004,012,016–017). Phạm vi messages ở US1 là GET, không kiểm POST trao đổi ở checkpoint này.
- [X] T016 [P] [US1] Viết `backend/src/test/java/com/kizuna/support/SupportTicketAttachmentTest.java` cho JPG/JPEG/PNG/PDF hợp lệ, sai MIME/đuôi/content, file hỏng, PDF không mở được, đúng/vượt 10.000.000 byte, 5/6 tệp, direct download sai quyền, header và rollback khi một file lỗi (FR-020–021). Chỉ kiểm tệp lúc tạo phiếu/tải; thêm ca ảnh 20 triệu pixel, PDF 100 trang và vượt giới hạn; tiến trình quá hạn, vượt RAM, chết và thiếu cách ly phải fail-closed, rollback toàn lần gửi.

### Implementation

- [X] T017 [US1] Hoàn thiện `backend/src/main/java/com/kizuna/features/ticket/dto/SupportTicketRequests.java`, tạo `backend/src/main/java/com/kizuna/features/ticket/service/SupportTicketProjection.java` và `backend/src/main/java/com/kizuna/features/ticket/service/SupportTicketProjection.java`: subject 1–200, message 1–10.000 Unicode code points không trắng, category bắt buộc, chỉ context IDs nullable; DTO không nhận requester/role, không có internal fields, metadata file không bytes.
- [X] T018 [US1] Tạo `backend/src/main/java/com/kizuna/features/ticket/entity/TicketAttachment.java` và `backend/src/main/java/com/kizuna/features/ticket/repository/TicketAttachmentRepository.java`: UUID PK, message FK bắt buộc cùng ticket qua composite FK/ràng buộc tương đương, uploader FK, original_name “varchar(255) đã làm sạch để hiển thị”, content_type image/jpeg/image/png/application/pdf, size_bytes “1..10.000.000”, created_at và content bytea; metadata query tuyệt đối không select binary; JDBC content query riêng.
- [X] T019 [US1] Tạo `backend/src/main/java/com/kizuna/features/ticket/service/TicketAttachmentValidator.java`: tối đa 5 tệp, mỗi tệp 1..10.000.000 byte, extension/MIME/content tương ứng, ImageIO và parser PDFBox, kiểm đọc cấu trúc và giới hạn tài nguyên; reject malformed/encrypted không xác minh được; không chỉ tin header, không dùng tên file làm path. Ảnh tối đa 20.000.000 pixel; PDF tối đa 100 trang, từ chối bản mã hóa không đọc được. Kiểm tra cấu trúc bằng ImageIO/PDFBox trong tiến trình con riêng: giới hạn cứng 256 MB RAM (256.000.000 byte) bằng cơ chế hệ điều hành, 10 giây/tệp và tổng 30 giây/lần gửi. -Xmx không thay thế giới hạn RAM toàn tiến trình. Hết hạn/vượt bộ nhớ phải dừng tiến trình, dọn tệp tạm và từ chối toàn bộ lần gửi, không lưu phiếu/message/tệp/audit một phần. Không kiểm tra trong JVM ứng dụng; nếu môi trường chưa áp được cách ly và giới hạn thì từ chối tiếp nhận tệp. Thực hiện kiểm tra tệp trước transaction ghi ngắn, rồi kiểm lại quyền/version/trạng thái dưới khóa trước commit.
- [X] T020 [US1] Cập nhật multipart trong `backend/src/main/resources/application.properties` và thêm `backend/src/main/java/com/kizuna/features/ticket/service/impl/SupportTicketServiceImpl.java`: request tối đa 52.000.000 byte, spill-to-temp thay vì load cả request vào RAM; kiểm tất cả tệp trước commit, bytes/metadata/message/ticket/audit chung transaction; lỗi lưu rollback tất cả, không public URL.
- [X] T021 [US1] Tạo `backend/src/main/java/com/kizuna/features/ticket/service/SupportTicketRelatedContextService.java` với ownership SQL projection theo data-model: participant booking, tutor sở hữu membership, user sở hữu payment có plan hội viên; đối chiếu liên kết đồng thời không mâu thuẫn, không tin stub entity thiếu owner; không sửa dữ liệu booking/payment/membership. Hoàn tất trước chốt T022.
- [X] T022 [US1] Mở rộng `backend/src/main/java/com/kizuna/features/ticket/service/SupportTicketService.java` và `backend/src/main/java/com/kizuna/features/ticket/service/impl/SupportTicketServiceImpl.java` cho create OPEN/MEDIUM, first public message, list/detail/messages owner-only, reload và private download; từ chối context ngoài quyền trước lưu; dùng context validator T021 ngay cả khi UI chọn context đến US4 mới hoàn thiện.
- [X] T023 [US1] Hoàn thiện `backend/src/main/java/com/kizuna/features/ticket/controller/SupportTicketController.java` cho POST/GET base, GET detail/messages và GET attachment theo contracts; multipart payload/files, 201 Location, version/allowedActions, authorized download attachment/nosniff/no-store, không serialize entity.
- [X] T024 [US1] Tạo `frontend/server.js`, `frontend/pages/support-tickets/index.html`, `frontend/pages/support-ticket/index.html`, `frontend/pages/support-tickets/index.html` và `frontend/pages/support-ticket/index.html`; kết nối dữ liệu thật, create/list/detail/attachments, shell hiện có, learner/tutor đúng DESIGN mục 7. Menu learner và tutor có mục “Yêu cầu hỗ trợ”, dẫn lần lượt tới /learner/support/tickets và /tutor/support/tickets; danh sách cá nhân có nút tạo phiếu. Kiểm thử điều hướng và quyền cả hai vai trò, kể cả truy cập trực tiếp/đổi route.
- [X] T025 [US1] Tạo `frontend/js/pages/support-tickets.js`, `frontend/js/pages/support-tickets.js` và `frontend/styles/support-tickets.css` cho render escaped, loading/empty/error khác nhau, CSRF/version/multipart, nhãn tiếng Việt có dấu, giữ text khi lỗi và chọn lại tệp khi cần; nút/field label và keyboard/focus phù hợp.
- [X] T026 [US1] Chạy T015/T016 trên PostgreSQL kiểm thử, kiểm create rollback/audit và mở lại file; ghi kết quả Story 1, lỗi/chưa chạy trung thực vào `specs/010-support-ticket-escalation/validation.md` sau T017–T025. US1 chỉ nghiệm thu tạo phiếu kèm tệp, list/detail, GET hội thoại và tải tệp có phân quyền. POST bổ sung của owner, phản hồi CS và tệp trong hội thoại thuộc US2; chỉ kết luận toàn bộ FR-020 hoàn tất khi checkpoint US1 và US2 đều đạt.

**Checkpoint**: US1 là MVP tiếp nhận/theo dõi, chưa tuyên bố đã xử lý hay chuyển cấp đầy đủ.

## Phase 4: User Story 2 — Nhân viên xử lý và chủ phiếu đóng (Priority: P1)

**Goal**: Nhận/phân công lại, trao đổi, ưu tiên, giải quyết; chỉ chủ phiếu xác nhận đóng.

**Independent Test**: Với phiếu fixture hoặc phiếu US1, CS1 nhận, CS2 phân công lại cho mình; CS1 mất quyền ghi, owner bổ sung sau resolve mở lại, chỉ owner đóng. Không cần US3/US4.

### Tests

- [X] T027 [P] [US2] Viết `backend/src/test/java/com/kizuna/support/SupportTicketWorkflowTest.java` cho hàng đợi/filter, claim, assignment, priority, public message+files, resolve và owner-only close; mọi CS được chuyển nhau nhưng không được phản hồi thay assignee, không auto-close (FR-004–008,013). Kiểm đầy đủ tệp trong bổ sung owner và phản hồi CS, gồm các ca giới hạn/parser/rollback T016.
- [X] T028 [P] [US2] Viết `backend/src/test/java/com/kizuna/support/SupportTicketConcurrencyTest.java` cho double-claim, stale version, close/message, assignment/reply và lỗi audit/file làm rollback; phân biệt 409 conflict với forbidden và không retry lệnh âm thầm (FR-018). Hội thoại trả sequenceNo và sắp xếp tăng dần theo trường này; sentAt chỉ hiển thị. sequence_no được cấp dưới khóa ticket, UNIQUE(ticket_id, sequence_no). Hai lệnh cùng expectedVersion chỉ một thành công; lệnh stale tải version mới trước khi gửi lại. Kiểm hai message cạnh tranh không trùng số và hiển thị đúng thứ tự ngay khi timestamp trùng.

### Implementation

- [X] T029 [US2] Tạo `backend/src/main/java/com/kizuna/features/ticket/dto/SupportTicketRequests.java` và `backend/src/main/java/com/kizuna/features/ticket/service/SupportTicketProjection.java`: expectedVersion số nguyên không âm bắt buộc, assigneeId/priority theo từng command, resolutionText/message 1–10.000 ký tự không trắng; nội bộ tách DTO owner, audit không secret.
- [X] T030 [US2] Bổ sung trong `backend/src/main/java/com/kizuna/features/ticket/service/impl/SupportTicketServiceImpl.java` claim OPEN → IN_PROGRESS, mọi CS reassign phiếu có assignee chưa CLOSED, priority LOW/MEDIUM/HIGH/URGENT, public reply có tệp, audit và transaction; filter/pagination hàng đợi và GET active CS candidates; giữ trạng thái khi phân công lại.
- [X] T031 [US2] Thực hiện resolve và close tại `backend/src/main/java/com/kizuna/features/ticket/service/impl/SupportTicketServiceImpl.java`: assignee gửi kết quả công khai + IN_PROGRESS → RESOLVED nguyên tử; chỉ owner RESOLVED → CLOSED, không auto-close; owner bổ sung RESOLVED → IN_PROGRESS giữ assignee/lịch sử, CLOSED cấm mọi ghi. Trong giai đoạn này từ chối chuyển cấp chưa triển khai; US3 thêm các transition tương ứng.
- [X] T032 [US2] Mở rộng `backend/src/main/java/com/kizuna/features/ticket/controller/SupportTicketController.java` cho claim/assignment/priority/messages/resolve/close/assignees/history đúng contracts, expectedVersion mọi mutation, history chỉ staff và manager có quyền; cập nhật allowedActions từ policy.
- [X] T033 [US2] Tạo `frontend/pages/support-tickets/index.html`, `frontend/pages/support-ticket/index.html`, mở rộng `frontend/server.js` và `frontend/js/pages/support-tickets.js`: hàng đợi/filter, nhận/chuyển CS, ưu tiên, reply+files, resolve; requester có xác nhận đóng và bổ sung mở lại; xử lý stale giữ text.
- [X] T034 [US2] Tích hợp menu và active-ticket count trong `frontend/components/admin-shell/admin-shell.js`, `frontend/pages/support-tickets/index.html` và `backend/src/main/java/com/kizuna/features/ticket/service/impl/SupportTicketServiceImpl.java`: đếm OPEN/IN_PROGRESS/ESCALATED bằng dữ liệu thật, không coi tất cả phiếu là đang xử lý; sửa nhãn kỹ thuật/all-caps thuộc phần hỗ trợ bị sửa.
- [X] T035 [US2] Chạy T027/T028 và hồi quy US1, kiểm tệp trong phản hồi CS; ghi checkpoint US2 và bằng chứng quyền đóng vào `specs/010-support-ticket-escalation/validation.md`. Chỉ chốt FR-020 khi cả T026 và T035 đạt, gồm tệp trong bổ sung owner và phản hồi CS.

## Phase 5: User Story 3 — Chuyển cấp và quyết định quản lý (Priority: P2)

**Goal**: Chuyển quản lý cụ thể, lưu quyết định, trả assignee; đổi manager có lý do và thu hồi quyền người cũ.

**Independent Test**: Dùng phiếu IN_PROGRESS có assignee từ fixture/US2; chuyển manager1, ghi quyết định, đổi manager2, kiểm manager1 mất quyền tệp/chi tiết và manager2 phải có quyết định mới; return về CS hiện tại, owner không thấy nội bộ.

### Tests

- [X] T036 [P] [US3] Viết `backend/src/test/java/com/kizuna/support/SupportTicketWorkflowTest.java` cho chuyển/lý do/tóm tắt, private manager list/detail, decision khác return, đổi manager chỉ assignee, quyền manager cũ bị thu hồi, quyết định theo revision, CS đổi khi pending và secrecy owner (FR-009–013,019).
- [X] T037 [P] [US3] Viết `backend/src/test/java/com/kizuna/support/SupportTicketConcurrencyTest.java` cho unique pending, stale decision/return khi đổi recipient, transaction rollback và race account ngừng hoạt động; PostgreSQL thật và không mất lịch sử.

### Implementation

- [X] T038 [US3] Tạo `backend/src/main/java/com/kizuna/features/ticket/entity/TicketEscalation.java` và `backend/src/main/java/com/kizuna/features/ticket/repository/SupportTicketRepository.java`: UUID PK/FK bắt buộc, reason/handling_summary “text 1–10.000 ký tự không trắng”, returned_at nullable, pending boolean, recipient_revision khởi đầu 0, decision_text nullable nhưng khi có 1–10.000 không trắng, decision_by/decision_revision/decided_at nullable; unique partial ticket WHERE pending=true; quyết định cũ snapshot audit trước reset.
- [X] T039 [US3] Tạo `backend/src/main/java/com/kizuna/features/ticket/dto/SupportTicketRequests.java` và `backend/src/main/java/com/kizuna/features/ticket/service/SupportTicketProjection.java` cho managerId/expectedVersion, reason/handlingSummary/decisionText có giới hạn như T038; manager projection chỉ recipient hiện tại, không generic entity JSON.
- [X] T040 [US3] Mở rộng `backend/src/main/java/com/kizuna/features/ticket/service/impl/SupportTicketServiceImpl.java`: assignee IN_PROGRESS → ESCALATED chọn manager active; giữ CS, pending duy nhất, audit; managers candidates có role/status đúng; current manager lưu decision riêng rồi return ESCALATED → IN_PROGRESS khi decision_by/revision khớp, không gửi công khai/đóng.
- [X] T041 [US3] Thêm đổi recipient trong `backend/src/main/java/com/kizuna/features/ticket/service/impl/SupportTicketServiceImpl.java`: chỉ assignee khi ESCALATED, lý do 1–10.000 không trắng, manager active khác người cũ; revision tăng, lưu history cũ, bỏ decision hiện hành, chỉ manager mới đọc/tải được; return dùng assignee mới nếu CS đổi lúc chờ.
- [X] T042 [US3] Hoàn thiện policy recipient và race trong `backend/src/main/java/com/kizuna/features/ticket/service/SupportTicketAccessPolicy.java` cùng `backend/src/main/java/com/kizuna/features/ticket/service/impl/SupportTicketServiceImpl.java`: giữ quyền current manager sau return đến khi thay, không tin token role cũ hoặc URL tải cũ; lịch sử/audit không đi vào owner DTO; khóa/kiểm quyền nhất quán mỗi download.
- [X] T043 [US3] Mở rộng `backend/src/main/java/com/kizuna/features/ticket/controller/SupportTicketController.java` cho GET managers, POST escalations/current recipient/current decision/current return và manager-scoped GET list/detail/history; 409 stale, không mở endpoint ghi status tùy ý.
- [X] T044 [US3] Tạo `frontend/pages/support-tickets/index.html`, `frontend/pages/support-ticket/index.html`, mở rộng CS detail tại `frontend/pages/support-ticket/index.html`, page controller tại `frontend/server.js` và `frontend/js/pages/support-tickets.js`: chuyển/đổi quản lý, decision/return riêng, nội bộ tách biệt; kiểm tra thực tế shell manager và gắn link tại template navigation đang dùng, ghi path đã sửa vào validation.
- [X] T045 [US3] Chạy T036/T037 và hồi quy US1–US2; kiểm URL tệp đã biết sau đổi quản lý, manager cũ/manager khác không thấy phiếu; ghi kết quả Story 3 vào `specs/010-support-ticket-escalation/validation.md`.

## Phase 6: User Story 4 — Hỗ trợ đặt học thử và kết nối gia sư (Priority: P2)

**Goal**: Context hỗ trợ đủ để tư vấn, filter yêu cầu booking/kết nối, không thực thi nghiệp vụ bên ngoài ticket.

**Independent Test**: Dùng phiếu booking/kết nối fixture; lọc, mở context hợp lệ, gửi hướng dẫn; kiểm booking/payment/membership không đổi. Chuyển cấp dùng lại US3, không tạo luồng thứ hai.

### Tests

- [X] T046 [P] [US4] Viết `backend/src/test/java/com/kizuna/support/SupportTicketWorkflowTest.java` cho booking participant, tutor membership, payment hội viên của đúng owner, context người khác/không còn tồn tại và liên kết mâu thuẫn; so sánh trạng thái đối tượng trước/sau xử lý, không phụ thuộc stub entity thiếu owner (FR-014–015).
- [X] T047 [P] [US4] Viết `backend/src/test/java/com/kizuna/support/SupportTicketWorkflowTest.java` cho filter BOOKING/TUTOR, tư vấn và chuyển cấp cùng phiếu, không tạo booking/kết nối/hoàn tiền/cấp hội viên.

### Implementation

- [X] T048 [US4] Tạo `backend/src/main/java/com/kizuna/features/ticket/service/SupportTicketRelatedContextService.java` và `backend/src/main/java/com/kizuna/features/ticket/repository/SupportTicketContextRepository.java` tái sử dụng validator T021: SQL projection đối chiếu schema thật cho context candidates phân trang/chi tiết tối thiểu, user ownership và payment plan hội viên; missing trả unavailable, không bịa dữ liệu hoặc lộ bank/gateway payload.
- [X] T049 [US4] Mở rộng `backend/src/main/java/com/kizuna/features/ticket/controller/SupportTicketController.java` cho GET related-contexts và GET ticket related-context đúng hợp đồng; owner candidates không cho nhập userId để chọn dữ liệu người khác, staff/manager chỉ context của phiếu được phép.
- [X] T050 [US4] Tạo `frontend/js/pages/support-tickets.js`, tích hợp vào form/detail tại `frontend/pages/support-tickets/index.html`, `frontend/pages/support-tickets/index.html`, `frontend/pages/support-ticket/index.html`, `frontend/pages/support-ticket/index.html` và `frontend/js/pages/support-tickets.js`: chọn context tùy chọn từ nguồn thật, filter booking/kết nối và hướng dẫn, không có nút thực thi hoàn tiền/đổi lịch ngoài phạm vi.
- [X] T051 [US4] Chạy T046/T047 cùng kiểm chứng Story 4 trong `specs/010-support-ticket-escalation/quickstart.md`; ghi bằng chứng đối tượng liên quan không bị đổi và chuyển cấp giữ nguyên phiếu vào `specs/010-support-ticket-escalation/validation.md`.

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Kiểm chứng xuyên suốt, hiệu năng, tài liệu vận hành; không thêm tính năng ngoài spec.

- [X] T052 Kiểm đầy đủ ma trận quyền của mọi GET/POST/tệp, account/role revocation và CSRF bằng `backend/src/test/java/com/kizuna/support/SupportTicketWorkflowTest.java`; bổ sung test dữ liệu nội bộ vắng khỏi response owner, xung đột concurrency còn thiếu, không chỉ kiểm ẩn nút.
- [X] T053 [P] Tạo `frontend/tests/support-tickets.browser.mjs` cho HTML/DOM label/escaped content/empty/error/role buttons; kiểm thủ công responsive, keyboard/focus, sentence case và learner/tutor DESIGN mục 7, ghi ảnh/bằng chứng không có dữ liệu nhạy cảm vào `specs/010-support-ticket-escalation/validation-ui.md`.
- [X] T054 [P] Tạo `backend/src/test/load/support-tickets.js` và `backend/src/test/load/README.md` cho 100 phiên/10 phút trên 1.000 phiếu có trao đổi, credentials qua môi trường, dataset chỉ test; đo p95 list/detail ≤2 giây, error và đối chiếu mutation/audit, không nhận API fail là thành công. SC-005: 100 người dùng đồng thời trong 10 phút, 1.000 phiếu có trao đổi; đo riêng p95 danh sách và chi tiết từ thao tác mở trang trên trình duyệt thật đến khi render hoàn tất, mỗi loại ≤2 giây. Báo cáo HTTP timing riêng để chẩn đoán; không dùng HTTP thay phép đo render.
- [ ] T055 Chạy load T054 trên DB kiểm thử, xem EXPLAIN để điều chỉnh index trong `database/seeds/fe10_query_indexes.sql` chỉ khi đo chứng minh cần; kiểm metadata không select binary/N+1 và ghi p95, dung lượng/backup impact vào `specs/010-support-ticket-escalation/validation-performance.md`. Ghi riêng p95 render trình duyệt và p95 HTTP; thiếu phép đo trình duyệt thì SC-005 chưa đạt.
- [X] T056 Rà lại migration/backup/restore/legacy URL không public và vận hành bằng `database/migrations/README-fe10.md`; xác nhận script SQL thật, profile ticket-test và lệnh Maven trong `specs/010-support-ticket-escalation/quickstart.md` khớp code sau triển khai, ghi phần bị chặn thay vì bịa kết quả.
- [ ] T057 Chạy toàn bộ test phù hợp theo `specs/010-support-ticket-escalation/quickstart.md`, tổ chức nghiệm thu 10 người cho SC-001 khi có người tham gia và tổng hợp FR-001–021/SC-001–007 vào `specs/010-support-ticket-escalation/validation.md`; chưa có người hoặc môi trường thì đánh dấu chưa chạy, không đóng task này như đã đạt.

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

## CS dashboard extension — Figma 1113:39

- [X] CSD001 Read the Figma design-to-code guidance and high-fidelity frame, download the exact logo/grid/chat assets, build responsive HTML/CSS/JS dashboard and reuse the internal shell as a CS variation.
- [X] CSD002 Add a CS-only read endpoint with real active counts, four recent queue items, category filtering and latest valid manager return for the current assignee; support category/status menu links and result deep links without changing business states or inserting operational fixtures.
- [X] CSD003 Verify endpoint authorization/counts/stale-return exclusion and desktop/mobile browser behavior, asset sizes, filters, result navigation and role isolation. Complete Maven regression: 75 tests passed; six frontend transport tests and both dashboard/support browser harnesses passed.

Original load/human acceptance tasks T055 and T057 remain open; this dashboard extension does not claim those criteria.

## Notes

- FE-10 pages restyled to the Figma “MỚI” frames and FRONTEND_RULES; list tabs use a multi-value status filter. See validation-ui.md. T055/T057 remain open.

- Spec-alignment fixes after review: status labels “Mới tiếp nhận”/“Đã chuyển quản lý”; related-context labels with type, identifying detail and state; requester/sender roles in API and CS detail; ticket UUID no longer rendered; requester list shows submitted time and initial content; closed-ticket guidance to create a new request; text with only invisible characters rejected on server and client. T055/T057 remain open.

- CS detail usability refinement completed under the user's frontend-only constraints: scoped two-column layout, summary, message/attachment cards, Vietnamese removable-file picker, bounded sending/retry behavior, permissions-driven sidebar forms and category-menu consolidation. Six frontend transport tests and the database-free browser harness passed. No backend/schema/database command, commit or push performed. Missing sender identity/role fields are documented in validation-ui.md and were not fabricated.

- User-approved UI refinement completed: two staff detail tabs, current internal state only, collapsed/lazy history with 20 steps per page. Verified with real browser CS/manager flows at desktop/mobile sizes, keyboard tab navigation, draft retention and a history spanning multiple pages. Existing T055/T057 acceptance markers remain unchanged.

- Không điền dấu hoàn tất cho công việc chỉ mới lên kế hoạch. Các task test có thể tạo fixture trong DB cách ly, không chèn mock vào vận hành.
- Ràng buộc chi tiết vẫn nằm trong data-model/contracts; task không cho phép bỏ kiểm quyền/tệp/transaction để giao diện chạy được.
- Không tạo tasks.md thứ hai, không sửa nguồn ứng dụng hoặc tự triển khai trong lượt `$speckit-tasks` này.
