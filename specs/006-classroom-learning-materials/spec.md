# Feature Specification: FE-06 — Classroom & Learning Materials Management

**Feature Branch**: `linhtk` (không tạo nhánh mới; nhánh không quyết định thư mục đặc tả).

**Created**: 2026-11-03

**Status**: Draft — đã làm rõ, chờ duyệt tasks

**Input**: Mục 6 "Classroom & Learning Materials Management" trong `Report-3_Software Requirement Specification.docx.pdf`: UC-93 (6.1 Access Classroom), UC-94 (6.2 Update Learning Materials), UC-58 (6.3 View Course), UC-61 (6.4 View Learning Materials), UC-65 (6.7 Upload Learning Materials); quy trình 2.4 Class Management; dịch vụ lưu trữ 5.x (Supabase Storage). Bổ sung theo yêu cầu người phụ trách (2026-11-06), chưa có trong SRS, cần nhóm cập nhật SRS: **Create Classroom** và **Delete Learning Material**.

## Clarifications

### Session 2026-11-03

- Q: Các UC bài tập (UC-63 View Assignments, UC-64 Submit, UC-66 Create & Assign, UC-67 Grade) có thuộc FE-06 không? → A: Không. Giao cho FE-07 (entity `Assignment*` thuộc feature `progress`). FE-06 không tạo/sửa bất cứ gì liên quan bài tập.
- Q: Tệp tài liệu được lưu ở đâu? → A: Supabase Storage (bucket riêng tư). Khóa dịch vụ và cấu hình mới đặt qua biến môi trường; không commit secret.
- Q: Bảng `learning_materials` thiếu cột mô tả mà UC-65/UC-61 yêu cầu — xử lý thế nào? → A: Thêm cột `description TEXT NULL` bằng một migration tối thiểu (được người phụ trách cho phép). Không thay đổi gì khác trong DB.
- Q: Kiểm tra điều kiện "đã kết nối" (UC-93 PRE-2) bằng cách nào? → A: Truy vấn chỉ đọc bảng `connection_requests` (status `ACCEPTED`) đặt trong repository của classroom; không sửa code/entity của `tutor_search`.
- Q: Có được sửa file frontend dùng chung? → A: Được, chỉ **bổ sung** (append) vào `frontend/js/api.js`, `frontend/components/learner-layout/learner-layout.js`, `frontend/server.js`; không sửa/xóa phần sẵn có.
- Q: Xác thực người dùng hiện tại thế nào khi `SecurityConfig` chưa có JWT filter? → A: Tách một lớp `CurrentUserResolver` riêng trong `features/classroom/util`, tự đọc header `Authorization: Bearer` và dùng `JwtTokenProvider` sẵn có; không sửa `core/`.

### Session 2026-11-06

- Q: "Create Classroom" có phải UC riêng không, và có tạo lớp trống trước rồi mới thêm học viên? → A: Tách thành UC riêng trong đặc tả nhưng **giữ nguyên cách làm hiện tại**: tạo lớp = mở lớp cho đúng một học viên đã kết nối (cột `learning_spaces.learner_id` NOT NULL). UC-93 là bước chọn học viên đủ điều kiện trong cùng thao tác. Không đổi DB, không đổi API `POST /classrooms`.
- Q: Có thêm chức năng xóa tài liệu không? → A: Có — **Delete Learning Material**: gia sư của lớp xóa hẳn tài liệu (bản ghi và nội dung tệp), cùng điều kiện như cập nhật tài liệu (lớp `ACTIVE`, hội viên chưa hết hạn), phải xác nhận trước khi xóa. Không có bảng nào tham chiếu `learning_materials` nên xóa không ảnh hưởng dữ liệu khác.
- Q: CORS dùng chung chặn phương thức `DELETE` → làm sao? → A: Người phụ trách cho phép thêm `DELETE` vào `allowedMethods` trong `core/security/SecurityConfig.java` (một dòng; cần báo nhóm vì là file dùng chung).
- Q: Đổi trạng thái lớp, sửa tên lớp, xóa/đóng bài tập, cho phép nộp lại? → A: Không làm trong đợt này.

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Gia sư tạo lớp cho học viên đã kết nối (Priority: P1) — Create Classroom + UC-93

Gia sư vào "Quản lý lớp học", xem các lớp hiện có, chọn thêm học viên đủ điều kiện, đặt tên lớp và xác nhận. Hệ thống tạo không gian học (`learning_spaces`) ở trạng thái đang hoạt động.

**Why this priority**: Không có lớp thì không có tài liệu, không có trang học của học viên.

**Independent Test**: Với một gia sư có ít nhất một yêu cầu kết nối `ACCEPTED`, mở trang quản lý lớp, thấy học viên đủ điều kiện, tạo lớp, tải lại trang thấy lớp mới; học viên đó biến mất khỏi danh sách đủ điều kiện.

**Acceptance Scenarios**:

1. **Given** gia sư đã đăng nhập, **When** mở "Quản lý lớp học", **Then** thấy danh sách lớp của chính mình (tên lớp, học viên, ngày tạo, trạng thái tiếng Việt) hoặc trạng thái rỗng nếu chưa có lớp.
2. **Given** gia sư có học viên đã chấp nhận kết nối và chưa có lớp đang hoạt động/tạm dừng với nhau, **When** chọn "Thêm học viên vào lớp", **Then** hệ thống chỉ liệt kê những học viên đó.
3. **Given** gia sư chọn học viên và nhập tên lớp hợp lệ (1–150 ký tự, không toàn khoảng trắng), **When** xác nhận, **Then** lớp được tạo với trạng thái "Đang hoạt động" và hiển thị thông báo thành công.
4. **Given** học viên không có kết nối `ACCEPTED` với gia sư, hoặc đã có lớp `ACTIVE`/`PAUSED` với gia sư này, **When** gửi yêu cầu tạo lớp (kể cả gọi API trực tiếp), **Then** bị từ chối với thông báo tiếng Việt, không tạo bản ghi.
5. **Given** gói hội viên của gia sư ở trạng thái hết hạn, **When** tạo lớp, **Then** bị từ chối và giải thích cần gia hạn hội viên.
6. **Given** lỗi kỹ thuật khi lưu (AF-1), **When** tạo lớp, **Then** hiển thị lỗi thân thiện và cho phép thử lại; không có lớp được tạo một phần.

### User Story 2 — Gia sư tải lên tài liệu học tập (Priority: P1) — UC-65

**Independent Test**: Trong một lớp đang hoạt động, tải lên một PDF ≤ 10 MB kèm tiêu đề và mô tả; tải lại trang thấy tài liệu với ngày tải lên và loại tệp; học viên của lớp mở được tệp.

**Acceptance Scenarios**:

1. **Given** lớp đang hoạt động và hội viên gia sư còn hiệu lực, **When** gia sư nhập tiêu đề (bắt buộc, ≤ 150 ký tự), mô tả (tùy chọn, ≤ 1000 ký tự) và chọn một tệp PDF, PNG, JPG hoặc MP3 ≤ 10 MB, **Then** tệp được lưu vào kho lưu trữ, tài liệu gắn với lớp, hiển thị thông báo thành công.
2. **Given** thiếu tiêu đề hoặc tệp, **When** gửi, **Then** báo lỗi dưới trường tương ứng, giữ nội dung đã nhập, không lưu gì.
3. **Given** tệp sai định dạng, nội dung không khớp đuôi tệp, hoặc vượt 10 MB, **When** gửi, **Then** báo lỗi dưới trường tệp, không lưu gì: vượt dung lượng → "Dung lượng tệp vượt quá giới hạn 10 MB. Định dạng cho phép: PDF, PNG, JPG, MP3." (thông điệp tải tệp của SRS); sai định dạng/giả mạo → "Định dạng tệp không được hỗ trợ… Định dạng cho phép: PDF, PNG, JPG, MP3."
4. **Given** lớp không ở trạng thái đang hoạt động hoặc hội viên gia sư hết hạn (AF-1), **When** mở mục tài liệu, **Then** nút tải lên bị ẩn/vô hiệu với giải thích; API cũng từ chối.
5. **Given** không phải gia sư của lớp, **When** gọi API tải lên, **Then** bị từ chối, không lưu tệp.
6. **Given** lưu tệp thành công nhưng lưu bản ghi thất bại, **When** xử lý, **Then** hệ thống xóa tệp vừa tải lên khỏi kho (không để tệp mồ côi) và báo lỗi thân thiện.

### User Story 3 — Xem tài liệu học tập (Priority: P1) — UC-61

**Independent Test**: Gia sư và học viên của cùng lớp đều thấy danh sách tài liệu và mở được tệp; tài khoản khác không xem được.

**Acceptance Scenarios**:

1. **Given** người dùng là gia sư hoặc học viên của lớp, **When** mở mục tài liệu, **Then** thấy danh sách gồm tiêu đề, mô tả, ngày tải lên, loại tệp, sắp xếp mới nhất trước.
2. **Given** lớp chưa có tài liệu (AF-1), **When** mở mục tài liệu, **Then** hiển thị "Hiện chưa có tài liệu nào trong lớp học này."
3. **Given** chọn một tài liệu, **When** bấm "Mở tài liệu", **Then** hệ thống cấp một liên kết tạm thời (hết hạn sau vài phút) để mở/tải tệp.
4. **Given** không thuộc lớp, **When** truy cập danh sách hoặc liên kết tệp qua API, **Then** bị từ chối và không nhận dữ liệu.
5. **Given** lớp đã kết thúc hoặc hội viên gia sư hết hạn, **When** học viên mở tài liệu, **Then** vẫn xem được (lớp ở chế độ chỉ xem theo quy trình 2.4).

### User Story 4 — Học viên xem khóa học của tôi (Priority: P2) — UC-58

**Independent Test**: Học viên có một lớp mở "Lớp học của tôi", thấy lớp với tên gia sư, ngày bắt đầu, trạng thái; mở chi tiết thấy tài liệu.

**Acceptance Scenarios**:

1. **Given** học viên đã đăng nhập, **When** mở "Lớp học của tôi", **Then** thấy danh sách lớp gồm tên lớp, tên gia sư, ngày bắt đầu, trạng thái; hoặc trạng thái rỗng.
2. **Given** chọn một lớp, **When** mở chi tiết, **Then** thấy thông tin có trong dữ liệu (tên lớp, gia sư, ngày bắt đầu, trạng thái, chế độ chỉ xem nếu có) và danh sách tài liệu (US3).
3. **Given** lỗi tải dữ liệu (AF-1), **When** mở danh sách hoặc chi tiết, **Then** hiển thị lỗi thân thiện và nút "Thử lại".
4. **Given** lớp của học viên khác, **When** truy cập chi tiết, **Then** bị từ chối.

### User Story 5 — Gia sư cập nhật hoặc thay thế tài liệu (Priority: P2) — UC-94

**Independent Test**: Sửa tiêu đề/mô tả một tài liệu, rồi thay tệp; tải lại thấy thông tin mới, mở tệp nhận tệp mới, tệp cũ không còn trong kho.

**Acceptance Scenarios**:

1. **Given** gia sư của lớp đang hoạt động, **When** chọn tài liệu, **Then** biểu mẫu hiển thị thông tin hiện tại.
2. **Given** sửa tiêu đề/mô tả hợp lệ, có hoặc không kèm tệp thay thế hợp lệ, **When** lưu, **Then** cập nhật và hiển thị thông báo cập nhật thành công.
3. **Given** thông tin thiếu hoặc tệp thay thế không hợp lệ (AF-1), **When** lưu, **Then** báo lỗi, giữ nguyên tài liệu cũ.
4. **Given** thay tệp thành công, **When** lưu xong, **Then** tệp cũ bị xóa khỏi kho; nếu lưu bản ghi thất bại, tệp mới bị xóa và tài liệu cũ giữ nguyên.
5. **Given** tài liệu không thuộc lớp được chỉ định, hoặc người gọi không phải gia sư của lớp, hoặc lớp ở chế độ chỉ xem, **When** cập nhật, **Then** bị từ chối.

### User Story 6 — Gia sư xóa tài liệu (Priority: P2) — Delete Learning Material *(bổ sung, chưa có trong SRS)*

**Independent Test**: Gia sư bấm "Xóa" ở một tài liệu, xác nhận; danh sách tải lại không còn tài liệu đó, học viên mở lớp cũng không thấy, gọi API tệp của tài liệu đó trả 404.

**Acceptance Scenarios**:

1. **Given** gia sư của lớp đang hoạt động, **When** bấm "Xóa" ở một tài liệu, **Then** hiện hộp thoại xác nhận nêu tên tài liệu và cảnh báo không thể khôi phục.
2. **Given** hộp thoại xác nhận, **When** bấm "Hủy", **Then** đóng hộp thoại, tài liệu giữ nguyên.
3. **Given** hộp thoại xác nhận, **When** bấm "Xóa tài liệu", **Then** tài liệu bị xóa hẳn, hiển thị "Xóa tài liệu thành công." và danh sách cập nhật.
4. **Given** tài liệu không thuộc lớp được chỉ định, **When** xóa, **Then** báo không tìm thấy (404), không xóa gì.
5. **Given** người gọi không phải gia sư của lớp, hoặc lớp ở chế độ chỉ xem (lớp không `ACTIVE` hoặc hội viên hết hạn), **When** xóa (kể cả gọi API trực tiếp), **Then** bị từ chối (403/409) với thông báo tiếng Việt; nút "Xóa" không hiển thị trên giao diện.
6. **Given** lỗi kỹ thuật khi xóa, **When** xác nhận, **Then** hiển thị lỗi thân thiện ngay trong hộp thoại, tài liệu giữ nguyên.

### Edge Cases

- Học viên có kết nối `ACCEPTED` với nhiều gia sư: mỗi gia sư mở lớp riêng; danh sách "Lớp học của tôi" hiển thị tất cả.
- Đã có lớp `COMPLETED`/`CLOSED` với học viên: được mở lớp mới (chỉ chặn trùng lớp `ACTIVE`/`PAUSED`).
- Token thiếu/sai/hết hạn: trả lỗi đăng nhập lại, giao diện chuyển về trang đăng nhập.
- Tên tệp có ký tự đặc biệt: không dùng tên gốc làm đường dẫn lưu trữ; đường dẫn do hệ thống sinh.
- Kho lưu trữ chưa cấu hình (thiếu biến môi trường): API tải lên trả lỗi thân thiện "Dịch vụ lưu trữ tạm thời không khả dụng", các chức năng xem danh sách vẫn hoạt động.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống PHẢI xác định người dùng hiện tại từ token đăng nhập và vai trò gia sư/học viên tương ứng cho mọi API FE-06; từ chối khi không xác định được.
- **FR-002**: Gia sư PHẢI xem được danh sách lớp của chính mình; học viên PHẢI xem được danh sách lớp mình tham gia (UC-93 bước 2, UC-58 bước 3).
- **FR-003**: Hệ thống PHẢI liệt kê học viên đủ điều kiện = có yêu cầu kết nối `ACCEPTED` với gia sư và chưa có lớp `ACTIVE`/`PAUSED` với gia sư đó (UC-93 bước 5).
- **FR-004**: Gia sư PHẢI tạo được lớp cho học viên đủ điều kiện với tên lớp 1–150 ký tự; hệ thống kiểm tra lại điều kiện phía máy chủ và chặn trùng (UC-93 bước 7–8).
- **FR-005**: Hệ thống PHẢI chặn tạo lớp, tải lên và cập nhật tài liệu khi gói hội viên của gia sư `EXPIRED` (quy trình 2.4).
- **FR-006**: Hệ thống PHẢI chặn tải lên/cập nhật tài liệu khi lớp không ở trạng thái `ACTIVE` (UC-65 PRE-3, AF-1).
- **FR-007**: Chỉ gia sư và học viên của lớp được xem chi tiết lớp, danh sách tài liệu và liên kết tệp (UC-61 bước 2).
- **FR-008**: Gia sư của lớp PHẢI tải lên được tài liệu gồm tiêu đề (bắt buộc, ≤ 150), mô tả (tùy chọn, ≤ 1000), một tệp (UC-65).
- **FR-009**: Tệp PHẢI là PDF, PNG, JPG/JPEG hoặc MP3; ≤ 10 MB; đuôi tệp, kiểu nội dung và chữ ký nhị phân đầu tệp phải khớp nhau.
- **FR-010**: Tệp PHẢI được lưu trong bucket Supabase Storage riêng tư; CSDL chỉ lưu đường dẫn đối tượng; truy cập tệp qua liên kết ký tạm thời do máy chủ cấp sau khi kiểm quyền.
- **FR-011**: Danh sách tài liệu PHẢI gồm tiêu đề, mô tả, ngày tải lên, loại tệp; có trạng thái rỗng (UC-61 bước 4, AF-1).
- **FR-012**: Gia sư của lớp PHẢI sửa được tiêu đề/mô tả và/hoặc thay tệp; tệp thay thế tuân thủ FR-009; xóa tệp cũ sau khi lưu thành công (UC-94).
- **FR-013**: Lỗi trong quá trình lưu PHẢI không để lại bản ghi hoặc tệp một phần (UC-93 AF-1, UC-65 bước 8).
- **FR-014**: Học viên PHẢI xem được danh sách và chi tiết lớp của mình, có nút thử lại khi lỗi (UC-58).
- **FR-015**: Mọi nội dung hiển thị PHẢI bằng tiếng Việt, không lộ enum/mã lỗi/thông tin kỹ thuật; dữ liệu thật từ API, không dữ liệu giả.
- **FR-016**: Lớp không `ACTIVE` hoặc hội viên gia sư hết hạn PHẢI hiển thị ở chế độ chỉ xem cho cả hai vai trò.
- **FR-017**: Gia sư của lớp PHẢI xóa được tài liệu sau khi xác nhận; xóa hẳn bản ghi và nội dung tệp trong một giao dịch; áp dụng cùng điều kiện quản lý như FR-005, FR-006 (Delete Learning Material).

### Out of Scope (ghi nhận, không triển khai)

- UC-63/64/66/67 bài tập → FE-07.
- UC-93 bước 9 (thông báo cho học viên): feature `notification` chưa có service; ghi nhận là phụ thuộc, không tự tạo.
- UC-58 "mô tả khóa học, lịch học, hoạt động liên quan": DB không có dữ liệu tương ứng; chỉ hiển thị dữ liệu sẵn có, không thêm cột.
- Đổi trạng thái lớp (tạm dừng/kết thúc), sửa tên lớp: SRS FE-06 không mô tả; người phụ trách chốt không làm (2026-11-06).
- Trang tổng quan/khung giao diện chung cho gia sư: không có trong FE-06; chỉ tạo một trang quản lý lớp độc lập.

### Key Entities

- **LearningSpace** (`learning_spaces`, sẵn có): một lớp giữa một gia sư và một học viên; tên, trạng thái `ACTIVE/PAUSED/COMPLETED/CLOSED`.
- **LearningMaterial** (`learning_materials`): tài liệu thuộc một lớp; tiêu đề, mô tả (cột mới), đường dẫn đối tượng lưu trữ, loại tệp, người tải lên, ngày tải lên.
- **ConnectionRequest** (`connection_requests`, chỉ đọc): căn cứ điều kiện kết nối.
- **Tutor / Learner / User** (core, chỉ đọc): danh tính, tên hiển thị, trạng thái hội viên.

## Success Criteria *(mandatory)*

- **SC-001**: Gia sư tạo được lớp cho học viên đã kết nối trong ≤ 3 thao tác từ trang quản lý lớp.
- **SC-002**: 100% yêu cầu từ người ngoài lớp tới API chi tiết, tài liệu, tệp bị từ chối (kiểm bằng unit test phân quyền).
- **SC-003**: 100% tệp sai định dạng/giả mạo/vượt 10 MB bị từ chối trước khi lưu (kiểm bằng unit test validator).
- **SC-004**: Không có bản ghi/tệp mồ côi khi lỗi lưu (kiểm bằng unit test với kho lưu trữ giả lập).
- **SC-005**: Không có chuỗi tiếng Anh, enum hay mã HTTP nào hiển thị trên hai trang mới (rà soát thủ công + test hàm ánh xạ).
- **SC-006**: Thay đổi CSDL duy nhất là thêm cột `learning_materials.description TEXT NULL`.

## Assumptions

- Một lớp = một `learning_space` gồm đúng một gia sư và một học viên (theo schema). "Thêm học viên vào lớp" = mở lớp mới cho cặp gia sư–học viên.
- Giới hạn 10 MB do nhóm chọn (SRS không nêu số cho tài liệu); định dạng theo thông điệp tải tệp của SRS (PDF, PNG, JPG, MP3).
- Người phụ trách tạo bucket riêng tư `learning-materials` trên Supabase và cấp biến môi trường `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` trước khi kiểm thử tích hợp.
- Người phụ trách chạy (hoặc cho phép chạy) migration thêm cột trên DB Supabase; mã nguồn không chạy DDL tự động (`ddl-auto=none`).
