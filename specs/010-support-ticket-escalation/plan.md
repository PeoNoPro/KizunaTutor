# Implementation Plan: FE-10 — Support Ticket & Escalation

## Implementation alignment — 2026-11-02

CS detail usability refinement (user-requested, frontend only): compose the existing detail screen into a 70/30 conversation/sidebar layout using `cs-ticket-detail.js` and scoped CSS. Reuse the current API, allowedActions and existing command forms; no backend, schema or database execution. Navy CTAs, one summary, file attachment cards and a Vietnamese removable-file picker. CS claim/resolution, information controls and private escalation stay in the right sidebar while public/internal reading tabs remain on the left. The two category-only sidebar shortcuts are consolidated into the existing list filters; the escalated list is labeled “Đã chuyển quản lý”.

Identity limitation: current detail/messages projections omit requester role and per-message sender ID/role. Derive requester name from the existing first-message contract and show unavailable roles honestly. Never infer sender roles from display names or add backend fields for visual completeness. Frontend verification uses isolated mocked API responses only; it does not claim backend/database integration was rerun.

CS dashboard extension authorized by the user: implement Figma frame 1113:39 at `/cs/dashboard`, with a shared CS variation of the existing internal shell. Beige page, 232px sidebar, support queue/category chips, latest manager return and static workflow guidance. The read-only dashboard endpoint aggregates existing ticket data; it does not introduce sample records, new states, booking/payment mutations or notification delivery. Figma sample names/counts are replaced by authenticated identity and actual records; unavailable results use empty states. Figma uppercase role caption is localized to sentence case under project rules.

UI refinement approved by the user: CS/manager detail defaults to the public conversation tab. Ticket acceptance, public resolution and owner closure stay with the conversation. Reassignment, priority, escalation and decision controls belong to a separate internal tab. Only the current escalation summary is displayed there. Prior steps use a collapsed, lazily fetched, paginated history. Switching tabs preserves drafts; no API or business-permission changes.

Read-only analysis and path mapping: [implementation-analysis.md](implementation-analysis.md). Current source uses feature packages under `backend/src/main/java/com/kizuna/features/ticket`, static HTML/CSS/ES modules under `frontend`, shared `core/security/SecurityConfig`, and additive SQL under `database/seeds`. Older SSR/root-package paths below describe the original design and are superseded by this mapping. No business scope changed.

Use native JDBC projections and transaction-aware commands on the application's existing DataSource, coordinated by Spring's existing transaction manager. This avoids serializing entities or loading bytea in list/detail, and allows explicit ticket/escalation/account row locks in the same short transaction. JPA entities map the full schema; no parallel database/persistence platform is introduced. Response projections are explicitly constructed; owner projections omit internal data entirely.

The static frontend obtains CSRF through authenticated `/api/v1/tickets/csrf` and identity/view role through `/api/v1/tickets/me`. All JSON uses the shared ApiResponse envelope. HTML role aliases remain the contract routes.

Uploads on the verified Windows environment use Job Objects: create suspended, cap whole-process committed memory at 256,000,000 bytes, assign before resume, kill on job close. Native-memory and timeout probes verify the cap independently of JVM heap limits. Unsupported isolation fails closed. Packaged deployment must supply `app.ticket.parser.classpath` pointing to exploded application classes and dependencies if its default classpath cannot launch the worker; failure rejects uploads and preserves text-only support.

**Branch**: `001-support-ticket-escalation` (feature context do setup-plan báo) | **Date**: 2026-10-29 | **Spec**: [spec.md](spec.md)

**Input**: `specs/010-support-ticket-escalation/spec.md`, gồm 21 yêu cầu và 5 quyết định đã xác nhận.

## Summary

Mở rộng module phiếu hỗ trợ hiện có cho học viên/gia sư, CS và quản lý. Giữ Spring Boot/Thymeleaf; bổ sung mapping dữ liệu, service phân quyền theo phiếu, vòng đời, lịch sử, chuyển cấp và tệp riêng tư. Chỉ chủ phiếu đóng sau giải quyết. Mọi CS được phân công lại; chỉ assignee đổi quản lý nhận. Quản lý cũ mất quyền ngay khi đổi. Tệp lưu PostgreSQL chung giao dịch để không có lần gửi lưu một phần.

## Technical Context

**Language/Version**: Java 17; Spring Boot 3.3.4 theo pom hiện tại.

**Primary Dependencies**: Spring Web, Security/JWT/OAuth2 hiện có, Thymeleaf, JPA, Validation, PostgreSQL driver, Lombok. Bổ sung parser PDFBox khi triển khai để kiểm tra cấu trúc PDF; không cần service mới.

**Storage**: PostgreSQL 16 theo Compose; bảng ticket/message hiện hữu và bảng escalation, audit, attachment mới. Binary bytea tách khỏi truy vấn danh sách. Schema thay đổi qua SQL tăng dần có kiểm soát; `ddl-auto: none`.

**Testing**: JUnit 5, Mockito, MockMvc, spring-security-test có sẵn. PostgreSQL database kiểm thử riêng qua profile `ticket-test` dự kiến; không giả định H2 tương đương. Test profile cấu hình bằng biến môi trường, không ghi thông tin truy cập thật.

**Target Platform**: Web server JVM, phát triển Windows/PowerShell, PostgreSQL Docker; trình duyệt desktop/mobile với view đáp ứng hiện tại.

**Project Type**: Ứng dụng web nguyên khối có SSR và REST cho thao tác tương tác.

**Performance Goals**: SC-005: 100 người dùng/10 phút, 1.000 phiếu có trao đổi, p95 mở list/detail ≤2 giây. Đo riêng dữ liệu có tệp, không đưa binary vào payload danh sách.

**Constraints**: UTF-8 không BOM; tiếng Việt; không mock vận hành; 5 tệp/lần, mỗi tệp 10.000.000 byte; kiểm tra account/role hiện tại mỗi request; rollback nội dung/tệp/audit cùng transaction; không public file URL.

**Scale/Scope**: Bốn hành trình, năm nhóm phạm vi FE-10, 21 FR, bốn vai trò (học viên, gia sư, CS, quản lý). Không thanh toán/hoàn tiền thực tế, email, chatbot, SLA tự động hoặc thay kiến trúc.

## Constitution Check

*GATE: Kiểm tra trước research và sau thiết kế Phase 1.*

| Nguyên tắc | Trước thiết kế | Sau thiết kế và bằng chứng |
| --- | --- | --- |
| I. Tiếng Việt, không lộ kỹ thuật | Đạt | DTO công khai riêng, error mapper tiếng Việt, không hiển thị enum/ID thô |
| II. Dữ liệu thật | Đạt | Repository thật; UI có rỗng/lỗi; fixture chỉ DB kiểm thử |
| III. Kiến trúc/quy ước | Đạt | Giữ com.kizuna, controller/service/repository; migration tăng dần; UUID |
| IV. Thiết kế theo phân hệ | Đạt | Learner/tutor theo DESIGN mục 7; CS/manager theo quy định chung; dùng shell hiện hữu nhưng sửa copy vi phạm trong phần FE-10 |
| V. Phạm vi/kiểm chứng | Đạt | Ma trận FR bên dưới và hướng dẫn nghiệm thu; không refactor nghiệp vụ khác |

Không có ngoại lệ constitution. AgentMemory/Kanji không thuộc tính năng này.

## Project Structure

### Documentation (this feature)

```text
specs/010-support-ticket-escalation/
  spec.md
  plan.md
  research.md
  data-model.md
  contracts/tickets.md
  quickstart.md
  checklists/requirements.md
```

`tasks.md` tạo sau bằng `$speckit-tasks`, không thuộc bước này.

### Source Code (repository root)

Các đường dẫn dưới đây là vị trí dự kiến khi triển khai; nhiều thành phần chưa tồn tại.

```text
src/main/java/com/kizuna/
  controller/SupportTicketController.java
  controller/support/SupportTicketPageController.java
  service/ISupportTicketService.java
  service/impl/SupportTicketServiceImpl.java
  service/support/                    # policy, context lookup, attachment validation
  repository/                        # ticket, message, escalation, audit, attachment metadata/content
  entity/                            # hoàn thiện Ticket/Message; thêm escalation/audit/attachment
  dto/request/                       # command và validation
  dto/response/                      # requester/staff/manager projection tách biệt
  config/SecurityConfig.java         # phạm vi CSRF và role của FE-10
  exception/                         # mapper lỗi FE-10
src/main/resources/
  db/changes/                        # SQL tăng dần, preflight và bảng phiên bản
  templates/{learner,tutor,cs,manager}/support/
  templates/fragments/support/
  static/js/support-tickets.js
  static/css/support-tickets.css
src/test/java/com/kizuna/support/
src/test/resources/application-ticket-test.properties
```

**Structure Decision**: Tái sử dụng module hiện có, DTO và service riêng theo trách nhiệm; không thêm module build hoặc frontend repo. Nội dung phiếu ban đầu lưu như message công khai đầu tiên; UI trình bày nó như mô tả phiếu.

## Implementation Sequence

1. Preflight schema; mapping ticket/message; SQL tăng dần và kiểm thử migration giữ dữ liệu. Không chạy file dựng lại chứa DROP CASCADE.
2. Policy account/role/object, state machine, transaction/version, audit; đủ luồng gửi/theo dõi/nhận/phản hồi/giải quyết/chủ phiếu đóng.
3. Phân công CS và escalation/decision/return/reassign manager; kiểm tra thu hồi quyền và cạnh tranh.
4. Attachment transactional bytea, parser, giới hạn multipart, authorized download; giữ văn bản khi lỗi UI.
5. View theo vai trò, filter/pagination và context lookup chỉ đọc; cải chính số đếm active tickets trong CS dashboard khi tích hợp, không đếm tất cả phiếu là đang xử lý.
6. Chạy bộ kiểm chứng và đo tải SC-005; ghi kết quả thực tế, không đánh dấu hoàn tất dựa vào tài liệu.

## Validation and Requirement Coverage

| Yêu cầu | Thiết kế/kiểm chứng |
| --- | --- |
| FR-001–004 | Create/query DTO, owner scope, filter/page boundaries, validation MockMvc |
| FR-005–008 | Policy + versioned commands, assignment, priority, đầy đủ chuyển đổi cho phép/cấm, owner-only close |
| FR-009–013,019 | Escalation/decision/audit, manager-only scope, reassign revocation, internal field absence |
| FR-014–015 | Ownership projection booking/membership/payment; không có thay đổi nghiệp vụ ngoài ticket |
| FR-016–018 | UI tiếng Việt/empty/error, auth+CSRF matrix, rollback/concurrency PostgreSQL |
| FR-020–021 | Parser/limits/bytea transaction, download và thu hồi quyền; metadata không mang binary |
| SC-001–007 | Nghiệm thu quickstart, 10 người dùng thử, đầy đủ hành trình và load run báo cáo p95 |

## Complexity Tracking

Không có vi phạm cần ngoại lệ. Lưu binary trong PostgreSQL tăng dung lượng backup; giới hạn truy vấn, stream từng tệp và theo dõi storage. Quyết định và phương án thay thế nằm trong [research.md](research.md).

## Phase Outputs and Readiness

Phase 0 hoàn tất ở research; Phase 1 hoàn tất ở data-model, contracts và quickstart. Không còn placeholder chặn thiết kế. Migration, dependency parser, profile kiểm thử và code chỉ được tạo ở bước triển khai; chưa chạy test ứng dụng trong lượt lập kế hoạch.

## Cập nhật thiết kế và nghiệm thu FE-10

US1 chỉ nghiệm thu tạo phiếu kèm tệp, list/detail, GET hội thoại và tải tệp có phân quyền. POST bổ sung của owner, phản hồi CS và tệp trong hội thoại thuộc US2; chỉ kết luận toàn bộ FR-020 hoàn tất khi checkpoint US1 và US2 đều đạt.

Hội thoại trả sequenceNo và sắp xếp tăng dần theo trường này; sentAt chỉ hiển thị. sequence_no được cấp dưới khóa ticket, UNIQUE(ticket_id, sequence_no). Hai lệnh cùng expectedVersion chỉ một thành công; lệnh stale tải version mới trước khi gửi lại. Kiểm hai message cạnh tranh không trùng số và hiển thị đúng thứ tự ngay khi timestamp trùng.

Ảnh tối đa 20.000.000 pixel; PDF tối đa 100 trang, từ chối bản mã hóa không đọc được. Kiểm tra cấu trúc bằng ImageIO/PDFBox trong tiến trình con riêng: giới hạn cứng 256 MB RAM (256.000.000 byte) bằng cơ chế hệ điều hành, 10 giây/tệp và tổng 30 giây/lần gửi. -Xmx không thay thế giới hạn RAM toàn tiến trình. Hết hạn/vượt bộ nhớ phải dừng tiến trình, dọn tệp tạm và từ chối toàn bộ lần gửi, không lưu phiếu/message/tệp/audit một phần. Không kiểm tra trong JVM ứng dụng; nếu môi trường chưa áp được cách ly và giới hạn thì từ chối tiếp nhận tệp. Thực hiện kiểm tra tệp trước transaction ghi ngắn, rồi kiểm lại quyền/version/trạng thái dưới khóa trước commit.

SC-005: 100 người dùng đồng thời trong 10 phút, 1.000 phiếu có trao đổi; đo riêng p95 danh sách và chi tiết từ thao tác mở trang trên trình duyệt thật đến khi render hoàn tất, mỗi loại ≤2 giây. Báo cáo HTTP timing riêng để chẩn đoán; không dùng HTTP thay phép đo render.

Menu learner và tutor có mục “Yêu cầu hỗ trợ”, dẫn lần lượt tới /learner/support/tickets và /tutor/support/tickets; danh sách cá nhân có nút tạo phiếu. Kiểm thử điều hướng và quyền cả hai vai trò, kể cả truy cập trực tiếp/đổi route.
