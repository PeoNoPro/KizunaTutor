# Implementation Plan: FE-11 — System Administration & Operational Reporting

**Branch**: `huyendtt` thực tế; feature context `011-system-admin-reporting` do setup-plan trả về | **Date**: 2026-10-30 | **Spec**: [spec.md](spec.md)

**Input**: `specs/011-system-admin-reporting/spec.md`, 18 FR, 7 SC, 5 câu trả lời đã xác nhận.

## Summary

### Báo cáo Manager và hồ sơ; AI để sau theo người dùng — 2026-11-01

- Thêm báo cáo tổng hợp theo cohort ngày tạo, trạng thái hiện tại, timezone Việt Nam, kỳ tối đa366ngày. Service công khai mới của FE-04/05 cung cấp tổng hợp chỉ đọc; FE-11 gọi service, không gọi repository xuyên feature. Tổng hợp trong transaction repeatable-read trên datasource nghiệp vụ. Không tái dựng lịch sử trạng thái không có nguồn.
- Mở GET management-report cho Admin/Manager và GET me/profile cho tài khoản nội bộ hiện tại, giữ Admin-only ở tất cả endpoint quản trị còn lại. Manager thấy menu báo cáo/hồ sơ và được chuyển từ dashboard về báo cáo; quyền vẫn kiểm ở backend.
- Trang xem hồ sơ lấy dữ liệu DB của chính phiên hiện tại. Không thêm chỉnh sửa hồ sơ hoặc xử lý mật khẩu thay FE-01/02. Script phát triển hỗ trợ AccountRole Manager để kiểm thử với tài khoản Manager thật ở DB riêng.
- Theo yêu cầu mới nhất của người dùng, bỏ phần triển khai AI khỏi lượt này: không thêm endpoint, migration, bảng hoặc giá trị cấu hình/quota AI. Không gọi dịch vụ AI trả phí.

### Kế hoạch theo phân công FE-11 vừa được duyệt — 2026-11-01

Ưu tiên sửa danh mục vai trò thành 6 vai trò nhưng giữ 4 vai trò gán cho nhân viên. Sau đó thiết kế báo cáo Manager riêng với hợp đồng dữ liệu từ FE-01/04/05; kiểm quyền Manager đọc/Admin vận hành tách biệt, không mở rộng matcher admin hiện tại thành quyền Manager cho toàn bộ API. Chốt danh mục cài đặt/tích hợp/giới hạn AI với FE-08 trước khi thêm tham số. Lối tắt thông báo dùng chức năng FE-12, xuất tệp chỉ làm khi được xác nhận. Những phần này là kế hoạch, không phải tuyên bố đã có mã hoạt động.

### Mở thẳng dashboard và sử dụng hai mục nhân sự — 2026-11-01

- Người dùng yêu cầu duyệt dashboard như admin và làm các mục đang bị khóa. Thêm trang quản lý nhân viên và vai trò nội bộ, dùng khung Figma đã triển khai. Endpoint/validation/version/audit được bổ sung trước trong contracts. Không thêm framework hoặc hệ thống quyền tùy chỉnh không có trong DB.
- CRUD được giới hạn ở tạo/sửa tài khoản nội bộ, khóa/mở và gán vai trò có sẵn; không xóa, không chuyển tài khoản learner/tutor thành nhân sự. Kiểm quyền DB trong transaction, khóa tuần tự thao tác nhân sự, kiểm expectedUpdatedAt, bảo vệ tài khoản đang sử dụng và quản trị cuối. Password BCrypt, must_change_password, không ghi mật khẩu vào audit/response.
- Bổ sung bảng audit riêng bằng migration tăng dần, áp dụng trên DB dev/test cục bộ. Frontend gốc `/` mở dashboard, trang công khai vẫn có đường dẫn riêng. Header lấy tên/ảnh từ DB, ảnh trống dùng chữ viết tắt. Không tạo tên/ảnh theo nhân vật minh họa Figma.
- Đã chạy backend cục bộ ở 8085 cho người dùng duyệt trực tiếp; chỉ khởi động lại sau khi xác minh tiến trình là KizunaBackendApplication và user.dir đúng checkout. Kiểm thử ghi dùng DB QA riêng `kizuna_staffqa_admin_dev`, không thêm tài khoản thử vào DB người dùng đang duyệt.

### Hiển thị danh tính quản trị — 2026-11-01

Theo yêu cầu chạy như một quản trị viên, khung dùng chung tải `/api/admin/operations/me`, hiển thị tên tài khoản DB hiện tại và nhãn quyền quản trị trên desktop/mobile. Phiên phát triển dùng tài khoản được chọn trong DB; không giả tên hoặc quyền. Hai mục quản lý nhân sự/phân quyền chưa có controller/service nghiệp vụ, nên không tuyên bố có thể sử dụng chúng chỉ từ quyền admin.

### Nối dữ liệu tài khoản hiện tại — 2026-11-01

Theo yêu cầu lấy dữ liệu trên máy, thêm API chỉ đọc `/api/admin/operations/dashboard/accounts`, yêu cầu quyền quản trị hiện tại. Tổng hợp một truy vấn trên users/roles dùng chung: tài khoản chưa xóa, số đang hoạt động, phân nhóm học viên/gia sư/nhân sự. Không truy cập repository feature khác, không thêm schema hoặc số liệu giả. Frontend nối bốn thẻ và cơ cấu tài khoản với API; nhãn phụ là số hoạt động theo nhóm để không đánh tráo số gói hội viên/lớp học. Chế độ `admin-dev` và DB riêng được giữ nguyên. Không mở rộng sang thống kê lượt truy cập, thiết bị hoặc tài chính.

### Đồng bộ trang quản trị và môi trường phát triển — 2026-11-01

- Theo yêu cầu tiếp theo của người dùng, cả dashboard và năm trang quản trị FE-11 dùng `frontend/components/admin-shell/admin-shell.js` cùng `frontend/styles/admin.css`. Bỏ khung công khai ở các trang quản trị, giữ nguyên nội dung/ID/logic nghiệp vụ; các trang chính sách dành cho học viên/gia sư giữ khung hiện có.
- Thu hẹp cơ chế `dev-login` đã có: chỉ bật trong profile `admin-dev` riêng, server loopback, DB PostgreSQL loopback có tên kết thúc `_admin_dev`. Phiên tạm chỉ cho yêu cầu vận hành không mang token từ loopback; quyền theo DB và CSRF vẫn hoạt động. Thay đổi `core/security` này thuộc yêu cầu trực tiếp cho môi trường quản trị phát triển, không mở quyền cho feature khác.
- `scripts/start-admin-dev.ps1` chuẩn bị bản sao DB cục bộ khi chưa có, không ghi đè DB, không gọi Supabase, không tự migrate. `docs/ADMIN_DEV.md` hướng dẫn chạy/tắt. Kiểm chứng cụ thể tại `validation-admin-shell.md`.

### Bổ sung giao diện tổng quan ngày 2026-11-01

- Theo yêu cầu trực tiếp của người dùng, thêm `frontend/pages/admin-dashboard/` bằng HTML/CSS/JavaScript thuần, lấy frame Figma `1485:5490` làm mẫu. Riêng trang này dùng sidebar, header và footer quản trị của Figma thay cho khung công khai; tái sử dụng font, logo và màu thương hiệu hiện có. Không đổi khung của các trang khác.
- Giữ các khối tài khoản, cơ cấu, báo cáo của mẫu nhưng hiển thị chưa có dữ liệu khi thiếu nguồn; không chép số liệu, danh tính, cam kết an toàn hay hạ tầng mẫu. Các thao tác ngoài FE-11 chưa tích hợp được vô hiệu hóa. Không thêm API hoặc chức năng tài chính/xuất tệp.
- Dùng các API FE-11 hiện có cho sức khỏe, tổng hợp sự cố, danh sách sự cố và lịch sử cấu hình. Liên kết dashboard với các trang quản trị hiện có; hỗ trợ alias `/admin/dashboard` và `/admin/operations` ở dev server.
- Kiểm chứng riêng ở `validation-dashboard.md`; việc này không đóng các phụ thuộc booking/policy/nghiệm thu tổng thể.

Giữ ứng dụng Spring Boot/Thymeleaf. Bổ sung cấu hình hai tham số có version/snapshot; quan sát sức khỏe có nguồn thật và timeout; hồ sơ sự cố tạo thủ công với lịch sử bất biến; báo cáo chỉ về sức khỏe/sự cố; trang đọc chính sách từ nội dung đã duyệt. Không mở trình biên tập chính sách, báo cáo tài chính hoặc điều khiển máy chủ.

## Technical Context

**Language/Version**: Java 17, Spring Boot 3.3.4 theo pom hiện tại.

**Primary Dependencies**: Spring Web, Thymeleaf, Security/JWT/OAuth2, Data JPA, Validation, PostgreSQL driver có sẵn; dùng scheduler, JDBC/RestClient theo stack hiện có. Không thêm platform observability hoặc service mới.

**Storage**: PostgreSQL 16; hoàn thiện system_configurations hiện có, thêm config revision/audit, incident/event, policy documents và snapshot booking. Health snapshot trong bộ nhớ để DB outage không làm mất trang quan sát; không hứa lịch sử telemetry dài hạn.

**Testing**: JUnit 5, MockMvc, spring-security-test; PostgreSQL DB riêng cho migration/locking/report/timezone; fake clock/probe chỉ trong test. Profile dự kiến `operations-test`, dữ liệu không dùng chung môi trường vận hành.

**Target Platform**: JVM server và trình duyệt responsive; dev Windows/PowerShell; PostgreSQL Docker; múi giờ nghiệp vụ Asia/Ho_Chi_Minh.

**Project Type**: Monolith web SSR có API tương tác.

**Performance Goals**: 100 người đồng thời/10 phút, 1.000 sự cố có lịch sử; p95 nội dung list/detail/report hiển thị ≤2 giây. Failure UI ≤5 giây; mỗi health probe timeout 2 giây, collector budget 4 giây. Đo browser riêng với HTTP load metrics.

**Constraints**: Chỉ duration 15–120 phút nguyên, advance 1–168 giờ nguyên; giữ chỗ thanh toán 15 phút chỉ đọc; hai giá trị source 45/12 không ghi đè DB. Snapshot cho booking mới, không tính lại booking cũ. UTF-8 không BOM; UI tiếng Việt, không raw config keys/log/exception. Observer không tự tạo incident.

**Scale/Scope**: Bốn hành trình; admin vận hành, learner/tutor đọc chính sách; phân trang 20 mặc định/max 50. Không tài chính/usage report, email, policy editor, remote terminal, deploy/restart hoặc thực thi thanh toán.

## Constitution Check

*Gate trước Phase 0 và tái kiểm sau Phase 1.*

| Nguyên tắc | Trước nghiên cứu | Sau thiết kế |
| --- | --- | --- |
| I. Tiếng Việt, không lộ backend | Đạt | DTO allowlist, labels thay raw keys, lỗi thân thiện, không log thô |
| II. Dữ liệu thật | Đạt | Probe thực tế, UNKNOWN khi thiếu nguồn, policy empty khi chưa được duyệt; fixture chỉ test |
| III. Kiến trúc/quy ước | Đạt | Giữ controller/service/repository; migration tăng dần, bảng mới UUID, giữ PK config legacy |
| IV. Thiết kế phân hệ | Đạt | Admin theo thiết kế chung/shell hiện tại; learner/tutor policy theo DESIGN mục 7 |
| V. Phạm vi/kiểm chứng | Đạt | Mapping FR/SC, lock/audit/report tests; booking và approved content là dependency ghi rõ |

Không có vi phạm hoặc ngoại lệ constitution. FE-11 không sửa AgentMemory ngoài tích hợp quan sát qua service có sẵn khi có hợp đồng đã kiểm chứng.

## Project Structure

### Documentation (this feature)

```text
specs/011-system-admin-reporting/
  spec.md
  plan.md
  research.md
  data-model.md
  contracts/operations.md
  quickstart.md
  checklists/requirements.md
```

tasks.md tạo ở bước `$speckit-tasks`, không trong lượt này.

### Source Code (repository root)

Vị trí dự kiến khi triển khai; phần lớn file mới chưa có.

```text
src/main/java/com/kizuna/
  controller/admin/OperationsController.java
  controller/admin/OperationsApiController.java
  controller/policy/PolicyDocumentController.java
  service/operations/             # actor policy, config, collector, incident, reporting
  service/policy/                 # read approved documents
  service/booking/BookingPolicyEvaluator.java
  service/impl/TrialBookingServiceImpl.java  # integration boundary, currently stub
  service/ai/AgentMemoryService.java        # verified read-only probe if available
  entity/                        # config mapping, new revision/incident/event/document
  repository/                    # scoped projections, event as-of queries
  dto/request/operations/
  dto/response/operations/
  config/SecurityConfig.java
  exception/OperationsExceptionHandler.java
src/main/resources/
  db/changes/                    # incremental SQL, controlled content import instructions
  templates/admin/operations/
  templates/learner/policies/
  templates/tutor/policies/
  static/js/operations.js
  static/css/operations.css
src/test/java/com/kizuna/operations/
src/test/resources/application-operations-test.properties
src/test/load/operations.js
```

**Structure Decision**: Đặt service chuyên trách trong monolith, tái sử dụng shell/đăng nhập; không dùng TestController hardcode UP cho vận hành. Health/admin dashboard thay serverStatus cố định bằng kết quả collector hoặc UNKNOWN. Tạo link navigation thực tế cho cả admin và learner/tutor.

## Delivery Sequence and Dependencies

1. Preflight schema/mapping, identity policy, scoped CSRF, PostgreSQL test profile và migration tăng dần có phiên bản/lock. Không chạy script dựng lại chứa DROP CASCADE.
2. Config revision/audit và evaluator; snapshot/config consumption tại điểm tạo booking thật. Nếu chưa có booking workflow, ghi blocker US1 E2E, không coi evaluator test thay thế acceptance.
3. Collector độc lập DB, application heartbeat, DB probe, optional agent-memory probe qua hợp đồng được xác minh; status/age/refresh UI, không auto incident.
4. Incident/event, claim/reassign/update/resolve/reopen-before-close/close; audit versioned atomic commands, báo cáo as-of lịch sử.
5. Policy read-only với nguồn tài liệu đã duyệt, effective versions, empty/conflict state; import ngoài UI theo quy trình được duyệt. Không tự viết văn bản pháp lý.
6. Kiểm toàn bộ quyền, snapshot/concurrency, ngày/epoch/timezone/report arithmetic và hồi quy UI; đo tải/browser, ghi kết quả thật.

## Validation and Requirement Coverage

| Yêu cầu | Thiết kế và bằng chứng dự kiến |
| --- | --- |
| FR-001,017 | ActorResolver reload DB, scoped policy/CSRF, role revocation matrix, Vietnamese DTO/UI tests |
| FR-002–006 | Allowlist hai trường, boundaries, version/atomic config audit, booking snapshot before/after và race |
| FR-007–008 | Collector real probes, UNKNOWN/stale age, failed DB while snapshot endpoint alive, manual-only incidents |
| FR-009–010 | Timezone/date validation, as-of event fold, distinct resolved count, carry-over open, source failure vs zero |
| FR-011–014 | Incident lifecycle, assignee policy, seq audit, immutable closed, repeat incident linkage |
| FR-015–016 | Approved published effective query, hidden draft/future/direct access, no corpus → empty |
| FR-018 | Mutation/event chung transaction, rollback failure injection, no unrelated side effects |
| SC-001–007 | quickstart scenarios, 10 người thử đọc policy, load/browser timing và báo cáo số liệu độc lập |

## Complexity Tracking

Không có ngoại lệ cần ghi nhận. Snapshot config và incident event là cần thiết cho quy tắc không hồi tố và tái lập báo cáo; bảng trạng thái hiện tại đơn lẻ không đáp ứng được. Không chọn platform observability mới cho phạm vi capstone.

## Phase Outputs and Readiness

### Điều chỉnh khi triển khai 2026-10-31

- Theo AGENTS.md hiện hành, mã FE-11 mới đặt trong `com.kizuna.features.admin.operations`; DTO tại `dto`, entity/repository/service/controller theo feature. Evaluator tại `com.kizuna.features.booking.service`, gọi public service `BookingPolicySnapshotProvider`, không gọi repository xuyên feature. Projection trạng thái shell gia sư tại `com.kizuna.features.profile.service.TutorShellStateService`, chỉ đọc và không tạo hồ sơ khi mở chính sách.
- Asset tại `static/js/features/admin/operations.js` và `static/css/features/admin/operations.css`. Các entity legacy giữ vị trí cũ. Revision/audit/document không kế thừa BaseEntity vì schema không có cặp created_at/updated_at; TechnicalIncident dùng BaseEntity.
- API thành công bọc `ApiResponse(success,message,data,timestamp)` theo AGENTS.md mới; các projection trong contracts nằm trong data. Lỗi có thông báo Việt, fieldErrors khi phù hợp. Lỗi mở trang có view HTML riêng.
- Identity/probe pool riêng, hữu hạn; snapshot collector độc lập DB nhưng API không bỏ kiểm quyền. Identity DB không khả dụng thì fail-closed, trả lỗi hữu hạn. Không còn cam kết trả health payload thành công trong mọi trường hợp DB outage.
- Lối tắt từ sức khỏe chỉ điền sẵn thành phần vào biểu mẫu tạo sự cố, quản trị viên vẫn xác nhận thủ công. Báo cáo có đối chiếu phân trang theo metric/cutoff, giữ cùng kỳ và thời điểm chốt.
- Xem implementation-readiness.md, validation.md, validation-ui.md và validation-performance.md để biết phần đã kiểm và phần bị phụ thuộc. Tài liệu thiết kế không thay bằng chứng nghiệm thu.

Phase 0: research.md giải quyết các lựa chọn thiết kế. Phase 1: data-model.md, contracts/operations.md và quickstart.md. Thiết kế sẵn sàng chia tasks, nhưng hoàn tất giao hàng phụ thuộc luồng tạo booking thật và tài liệu policy được duyệt. Chưa chạy migration, sửa code hoặc kiểm thử ứng dụng.
