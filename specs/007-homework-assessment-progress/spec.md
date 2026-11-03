# Feature Specification: FE-07 — Homework, Assessment & Progress Tracking

**Feature Branch**: `linhtk` (không tạo nhánh mới)

**Created**: 2026-11-03

**Status**: Draft — đã làm rõ, chờ duyệt tasks

**Input**: SRS `Report-3_Software Requirement Specification.docx.pdf`:
- Mục 7 (feature "Homework, Assessment & Progress Tracking"): UC-95 (7.1 Update Assignments), UC-96 (7.2 Resubmit Assignments).
- Chuyển từ mục 6 sang theo thống nhất với người phụ trách: UC-63 (6.5 View Assignments), UC-64 (6.6 Submit Assignment), UC-66 (6.8 Create and Assign Assignment), UC-67 (6.9 Grade Assignment).
- Quy tắc dữ liệu "Assignment submission deadline enforcement"; thông điệp MSG22 (nộp bài thành công), MSG13 (lỗi tệp).

## Clarifications

### Session 2026-11-03

- Q: Phạm vi FE-07? → A: Chỉ UC-63, 64, 66, 67, 95, 96. UC-59, UC-68, UC-69 (Learning Support) không thuộc phạm vi, không đụng tới.
- Q: Lưu tệp bài nộp ở đâu? → A: Cột `assignment_submissions.file_data BYTEA` (người phụ trách đã tự thêm trên DB); `attachment_url` lưu tên tệp gốc đã làm sạch — giống FE-06.
- Q: Nộp muộn và nộp lại? → A: Còn hạn: nộp/nộp lại (sửa tệp đáp án) nhiều lần. Quá hạn: chỉ được nộp 1 lần nếu chưa nộp lần nào, không nộp lại/sửa. Quá hạn vẫn nhận bài, chỉ hiển thị trạng thái "Đúng hạn" / "Nộp muộn".
- Q: Chấm điểm? → A: Gia sư chấm sau khi hết hạn nộp, chấm 1 lần duy nhất (không sửa điểm, không có thao tác "chốt điểm" riêng, không cho nộp lại sau khi chấm).
- Q: Hạn nộp có bắt buộc? → A: Không bắt buộc.
- Q: Điều kiện sửa bài tập (UC-95)? → A: Gia sư sửa được tiêu đề, mô tả, hạn nộp, điểm tối đa khi chưa có bài nào được chấm; hạn nộp mới (nếu có) phải ở tương lai.
- Q: Đính kèm tài liệu vào bài tập (UC-66, tùy chọn)? → A: Không làm. Gia sư hướng dẫn trong mô tả; tài liệu đã có ở mục Tài liệu học tập (FE-06).

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Gia sư tạo và giao bài tập (P1) — UC-66

**Independent Test**: Trong lớp đang hoạt động, gia sư tạo bài tập có/không có hạn; tải lại trang thấy bài tập; học viên của lớp thấy bài tập đó.

1. **Given** lớp đang hoạt động và hội viên gia sư còn hiệu lực, **When** nhập tiêu đề (bắt buộc, ≤ 150), mô tả (tùy chọn, ≤ 5000), hạn nộp (tùy chọn, phải ở tương lai), điểm tối đa (tùy chọn, mặc định 100, 1–999.99) và lưu, **Then** bài tập được tạo gắn với lớp (và học viên duy nhất của lớp), hiển thị thông báo thành công.
2. **Given** thiếu tiêu đề, hạn nộp ở quá khứ hoặc điểm tối đa không hợp lệ, **When** lưu, **Then** báo lỗi dưới trường tương ứng, không tạo bài tập.
3. **Given** lớp không hoạt động / hội viên hết hạn / người gọi không phải gia sư của lớp, **When** tạo, **Then** bị từ chối.

### User Story 2 — Xem bài tập (P1) — UC-63

1. **Given** gia sư hoặc học viên của lớp, **When** mở mục Bài tập, **Then** thấy danh sách gồm tiêu đề, mô tả, hạn nộp, điểm tối đa và trạng thái bài nộp mới nhất ("Chưa nộp", "Đúng hạn", "Nộp muộn", "Quá hạn – chưa nộp", "Đã chấm").
2. **Given** chọn một bài tập, **When** mở chi tiết, **Then** thấy yêu cầu, hạn nộp, các lần nộp (nội dung, tệp, thời điểm, đúng hạn/muộn) và điểm, nhận xét nếu đã chấm.
3. **Given** lớp chưa có bài tập (AF-1), **Then** hiển thị "Hiện chưa có bài tập nào trong lớp học này."
4. **Given** người ngoài lớp, **When** gọi API, **Then** bị từ chối, không nhận dữ liệu.

### User Story 3 — Học viên nộp bài (P1) — UC-64

1. **Given** bài tập chưa được chấm, học viên chưa nộp lần nào, **When** nhập nội dung trả lời và/hoặc chọn tệp PDF/PNG/JPG/MP3 ≤ 10 MB rồi nộp, **Then** lưu lần nộp số 1, trạng thái "Đã nộp", ghi thời điểm nộp, hiển thị "Nộp bài tập thành công." (MSG22).
2. **Given** không có nội dung và không có tệp, hoặc tệp sai định dạng/giả mạo/quá 10 MB (AF-2), **Then** báo lỗi tiếng Việt, không lưu gì.
3. **Given** đã quá hạn và chưa nộp lần nào, **When** nộp, **Then** được nhận và đánh dấu "Nộp muộn".
4. **Given** lớp ở chế độ chỉ xem, **When** nộp, **Then** bị từ chối.
5. **Given** bài tập không tồn tại/không thuộc lớp (AF-1), **Then** báo lỗi và quay về danh sách.

### User Story 4 — Học viên nộp lại (P2) — UC-96

1. **Given** còn hạn (hoặc bài không có hạn) và chưa được chấm, **When** nộp lại, **Then** tạo lần nộp mới (`attempt_number` + 1), lần cũ được giữ, hiển thị "Đã nộp lại".
2. **Given** đã quá hạn, **When** nộp lại, **Then** bị từ chối: "Đã quá hạn nộp bài nên không thể nộp lại."
3. **Given** bài đã được chấm, **When** nộp lại, **Then** bị từ chối.

### User Story 5 — Gia sư chấm điểm (P1) — UC-67

1. **Given** đã hết hạn nộp (hoặc bài không có hạn) và học viên đã nộp, bài chưa được chấm, **When** gia sư nhập điểm (0 ≤ điểm ≤ điểm tối đa) và nhận xét (tùy chọn, ≤ 5000) rồi gửi, **Then** lưu cho **lần nộp mới nhất**: điểm, nhận xét, thời điểm chấm, trạng thái "Đã chấm"; học viên xem được.
2. **Given** chưa hết hạn nộp, **When** chấm, **Then** bị từ chối: "Chỉ được chấm điểm sau khi hết hạn nộp bài."
3. **Given** bài đã được chấm, **When** chấm lại, **Then** bị từ chối (chỉ chấm 1 lần).
4. **Given** điểm thiếu/âm/vượt điểm tối đa, **Then** báo lỗi, không lưu.
5. **Given** lần nộp không phải mới nhất, **When** chấm, **Then** bị từ chối.
6. **Given** lỗi lưu (AF-1), **Then** báo lỗi, kết quả cũ (nếu có) không đổi.

### User Story 6 — Gia sư sửa bài tập (P2) — UC-95

1. **Given** chưa có bài nộp nào được chấm và lớp quản lý được, **When** sửa tiêu đề/mô tả/hạn nộp/điểm tối đa hợp lệ, **Then** lưu thay đổi, hiển thị "Cập nhật bài tập thành công."
2. **Given** hạn nộp mới ở quá khứ hoặc thông tin không hợp lệ (AF-1), **Then** báo lỗi, giữ nguyên bài tập.
3. **Given** đã có bài được chấm, **When** sửa, **Then** bị từ chối: "Bài tập đã có bài được chấm nên không thể chỉnh sửa."

### Edge Cases

- Gia sư gia hạn (hạn mới ở tương lai) sau khi đã quá hạn → học viên lại được nộp/nộp lại theo quy tắc "còn hạn".
- Gia sư xoá hạn nộp (để trống) → bài trở thành "không có hạn".
- Thời điểm so sánh hạn nộp lấy từ máy chủ, không tin giờ của trình duyệt.
- Bài nộp trùng thời điểm hạn: `submitted_at ≤ due_date` là đúng hạn.

## Requirements *(mandatory)*

- **FR-001**: Dùng lại xác thực và phân quyền lớp của FE-06: chỉ gia sư/học viên của lớp truy cập bài tập của lớp đó.
- **FR-002**: Gia sư tạo bài tập với tiêu đề (1–150), mô tả (≤ 5000), hạn nộp tùy chọn ở tương lai, điểm tối đa 1–999.99 (mặc định 100) khi lớp quản lý được.
- **FR-003**: Gia sư sửa bài tập khi chưa có bài nộp được chấm; hạn nộp mới (nếu có) ở tương lai.
- **FR-004**: Danh sách bài tập trả về trạng thái nộp mới nhất của học viên; chi tiết trả về toàn bộ lần nộp theo thứ tự.
- **FR-005**: Học viên nộp nội dung chữ (≤ 5000) và/hoặc một tệp PDF/PNG/JPG/MP3 ≤ 10 MB (kiểm tra chữ ký tệp như FE-06); bắt buộc có ít nhất một trong hai.
- **FR-006**: Quy tắc nộp: chưa được chấm; nếu đã quá hạn thì chỉ khi chưa có lần nộp nào; mỗi lần nộp là một dòng mới với `attempt_number` tăng dần.
- **FR-007**: Đúng hạn/nộp muộn tính bằng `submitted_at` so với `due_date`.
- **FR-008**: Gia sư chấm lần nộp mới nhất, một lần duy nhất, sau khi hết hạn (hoặc bất kỳ lúc nào nếu không có hạn); điểm 0…điểm tối đa; nhận xét ≤ 5000.
- **FR-009**: Tệp bài nộp chỉ gia sư/học viên của lớp mở được; danh sách/chi tiết không tải dữ liệu nhị phân.
- **FR-010**: Lớp chỉ xem (không ACTIVE hoặc hội viên hết hạn): mọi thao tác ghi (tạo, sửa, nộp, chấm) bị chặn; xem vẫn được.
- **FR-011**: Mọi thông báo tiếng Việt, không lộ enum/mã lỗi; dữ liệu thật từ API.

### Out of Scope

- UC-59, UC-68, UC-69 (Learning Support).
- Đính kèm tài liệu vào bài tập; chấm điểm AI (`ai_score`, `ai_feedback`, trạng thái `AI_EVALUATED`); bài tập luyện Kanji (`assignment_kanji_items`).
- Thông báo cho học viên/gia sư (UC-66 b9, UC-67 b10, UC-95 b9, UC-96 b9) — chưa có notification service.
- Xoá bài tập (SRS không mô tả).

### Key Entities

- **Assignment** (`assignments`): thuộc một lớp; tiêu đề, mô tả, hạn nộp, điểm tối đa.
- **AssignmentSubmission** (`assignment_submissions`): một lần nộp của học viên; số lần nộp, nội dung, tên tệp (`attachment_url`), nội dung tệp (`file_data`, không ánh xạ), điểm, nhận xét, đã chấm (`is_finalized`), trạng thái, thời điểm nộp/chấm.

## Success Criteria

- **SC-001**: 100% thao tác ghi sai quyền/sai trạng thái bị từ chối (unit test).
- **SC-002**: Quy tắc nộp/nộp lại/chấm đúng bảng ở mục Clarifications trong mọi tổ hợp (còn hạn/quá hạn/không hạn × đã nộp/chưa nộp × đã chấm/chưa chấm) — unit test.
- **SC-003**: Không thay đổi DB ngoài cột `file_data` người phụ trách đã thêm.

## Assumptions

- Bài tập **không có hạn**: học viên nộp/nộp lại bất kỳ lúc nào tới khi được chấm; gia sư chấm bất kỳ lúc nào; sau khi chấm thì khóa.
- Khi chấm: `status = GRADED`, `is_finalized = true`, `graded_at = now`.
- Loại tệp bài nộp suy ra từ phần mở rộng đã xác minh trong `attachment_url` (bảng không có cột loại tệp).
