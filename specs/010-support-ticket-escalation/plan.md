# Implementation Plan: FE-10 — Support Ticket & Escalation

**Branch**: `001-support-ticket-escalation` (feature context do setup-plan báo) | **Date**: 2026-10-29 | **Spec**: [spec.md](spec.md)

**Input**: `specs/001-support-ticket-escalation/spec.md`, gồm 21 yêu cầu và 5 quyết định đã xác nhận.

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
specs/001-support-ticket-escalation/
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
