# Feature Specification: FE-16 — Cho nộp lại hồ sơ xác minh và bài đăng sau 7 ngày bị từ chối

**Feature Branch**: `han` (thư mục đặc tả `specs/016-rejection-resubmit-cooldown`)

**Created**: 2026-11-26

**Status**: Draft — đã chốt với người phụ trách (phiên 2026-11-26)

**Input**: Yêu cầu người phụ trách (Hân), 2026-11-26: "Nếu bị từ chối hồ sơ xác minh hoặc bài đăng thì hiện tại không có cách nào nộp lại. Tôi muốn làm phương án cho làm lại sau một khoảng thời gian, nhưng mốc thời gian bị phạt chỉ là 1 tuần."

**Nguồn nghiệp vụ**:

| Nguồn | Nội dung dùng |
| --- | --- |
| SRS UC 3.2, 3.3, 3.11 (Tutor Verification) | Gia sư sửa, nộp hồ sơ; cán bộ học vụ duyệt / từ chối; hiện ghi "Rejection is final" (POST-2 của 3.11). |
| SRS UC 4.1, 4.3, 4.5 (Tutor Introduction Post) | Gia sư gửi / sửa bài; cán bộ học vụ duyệt / từ chối; hiện ghi "A rejected post cannot be resubmitted" (POST-2 của 4.5). |
| SRS GBR-38, GBR-41, GBR-45, GBR-48 | Điều kiện nộp hồ sơ; từ chối là cuối cùng (sẽ thay); hồ sơ luôn giữ giấy tờ tùy thân + chứng chỉ; mỗi gia sư chỉ một bài chờ duyệt. |
| `specs/003-tutor-verification`, mã `features/verification`, `features/tutor_post` | Máy trạng thái xác minh, lịch sử thẩm định, bảng `tutor_posts` (mỗi lần gửi một dòng). |

## Phạm vi

**Trong phạm vi**:
- Hồ sơ xác minh bị từ chối: sau **7 ngày** kể từ lúc cán bộ học vụ từ chối, gia sư được sửa thông tin, tải giấy tờ mới và nộp lại.
- Bài đăng đầu tiên bị từ chối (chưa từng có bài được duyệt): sau **7 ngày** kể từ lúc bị từ chối, gia sư được gửi bài mới.
- Trang của gia sư, email và thông báo trong ứng dụng báo rõ thời điểm được nộp lại.

**Ngoài phạm vi**:
- Bản cập nhật của bài đã duyệt bị từ chối: giữ như hiện tại (gửi bản khác được ngay, vì bản cũ vẫn hiển thị).
- Yêu cầu cập nhật hồ sơ đã phê duyệt bị từ chối: giữ như hiện tại (gửi yêu cầu mới được ngay).
- Giới hạn số lần bị từ chối, tăng dần thời gian phạt, hay khóa vĩnh viễn sau nhiều lần.
- Màn hình mở khóa thủ công cho Manager / Academic Officer / Admin.
- Thay đổi phía cán bộ học vụ (danh sách, bộ lọc, nút duyệt): không đổi.

## Clarifications

### Session 2026-11-26

- Q: Phương án cho làm lại? → A: Người phụ trách chọn "cho làm lại sau một khoảng thời gian".
- Q: Bao lâu? → A: **1 tuần (7 ngày = 168 giờ)** kể từ thời điểm cán bộ học vụ bấm từ chối.
- Q: Áp dụng cho phần nào? → A: Cả hồ sơ xác minh và bài đăng ("2 phần đó").
- Q: Bản cập nhật bài đã duyệt bị từ chối có bị phạt 7 ngày không? → A: Không (quyết định theo nguyên tắc giữ chức năng hiện có: hiện được gửi lại ngay; phạt chỉ áp dụng cho trường hợp đang bị khóa vĩnh viễn).
- Q: Bị từ chối lần nữa thì sao? → A: Tính lại 7 ngày từ lần từ chối mới; không giới hạn số lần.
- Q: Giấy tờ đã bị đánh dấu "Không được chấp nhận" có được tính khi nộp lại? → A: **Không.** Gia sư phải tải giấy tờ mới (tùy thân và chứng chỉ tiếng Nhật). Nếu tính, sau khi duyệt hồ sơ sẽ không có giấy tờ nào "Đã xác minh", trái GBR-45.

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Gia sư nộp lại hồ sơ xác minh sau 7 ngày (Priority: P1)

Gia sư bị từ chối hồ sơ thấy trên trang "Hồ sơ xác minh" thời điểm được nộp lại. Trước thời điểm đó, hồ sơ chỉ xem; từ thời điểm đó, gia sư sửa thông tin, gỡ giấy tờ cũ, tải giấy tờ mới và nộp lại; hồ sơ chuyển "Đang chờ thẩm định".

**Why this priority**: Đây là trường hợp gia sư hiện bị khóa vĩnh viễn, không còn cách nào dùng nền tảng.

**Independent Test**: Tạo gia sư có hồ sơ bị từ chối 8 ngày trước → mở trang → sửa được, nộp được; gia sư bị từ chối 2 ngày trước → chỉ xem, thấy ngày mở lại.

**Acceptance Scenarios**:

1. **Given** hồ sơ bị từ chối lúc T, **When** gia sư mở trang trước T + 7 ngày, **Then** hồ sơ chỉ xem, trạng thái "Bị từ chối", lời nhắc cho biết được nộp lại từ HH:mm ngày dd/MM/yyyy.
2. **Given** như trên, **When** gia sư gọi lưu thông tin / tải giấy tờ / nộp trước T + 7 ngày, **Then** hệ thống từ chối với lời nhắn có thời điểm được nộp lại, không thay đổi dữ liệu.
3. **Given** đã qua T + 7 ngày, **When** gia sư mở trang, **Then** biểu mẫu mở để sửa, nút "Nộp lại hồ sơ" hiện, danh sách "còn thiếu" nêu giấy tờ cần tải mới.
4. **Given** đã qua T + 7 ngày và gia sư chỉ còn giấy tờ "Không được chấp nhận", **When** nộp, **Then** hệ thống báo thiếu giấy tờ tùy thân / chứng chỉ, không nộp.
5. **Given** đã qua T + 7 ngày và hồ sơ đủ, **When** gia sư nộp, **Then** hồ sơ "Đang chờ thẩm định", cán bộ học vụ nhận thông báo "Hồ sơ gia sư được nộp lại".
6. **Given** hồ sơ nộp lại bị từ chối lần nữa lúc T2, **Then** thời điểm được nộp lại là T2 + 7 ngày.

---

### User Story 2 — Gia sư gửi bài đăng mới sau 7 ngày bài đầu bị từ chối (Priority: P1)

Gia sư có bài đầu tiên bị từ chối thấy thời điểm được gửi bài mới trên trang "Bài đăng của tôi"; từ thời điểm đó soạn và gửi bài mới (tạo bản mới, bài bị từ chối giữ nguyên trong lịch sử).

**Why this priority**: Gia sư đã trả tiền gói hội viên nhưng không thể xuất hiện trên trang Tìm gia sư.

**Independent Test**: Bài mới nhất bị từ chối 8 ngày trước, chưa có bài đã duyệt → `editable = true`, gửi tạo bản chờ duyệt mới; bị từ chối 1 ngày trước → `editable = false`, có thời điểm mở lại.

**Acceptance Scenarios**:

1. **Given** bài đầu tiên bị từ chối lúc T, **When** gia sư mở trang trước T + 7 ngày, **Then** bài chỉ xem, khung trạng thái nêu lý do và "Bạn có thể gửi bài đăng mới từ HH:mm ngày dd/MM/yyyy".
2. **Given** như trên, **When** gia sư gửi bài trước T + 7 ngày, **Then** hệ thống từ chối với lời nhắn có thời điểm, không lưu.
3. **Given** đã qua T + 7 ngày (gói còn hạn, hồ sơ đã duyệt), **When** gia sư mở trang, **Then** biểu mẫu mở sẵn nội dung bài cũ để sửa, nút "Gửi lại".
4. **Given** như trên, **When** gửi, **Then** tạo một bài mới "Chờ duyệt"; bài bị từ chối giữ nguyên.
5. **Given** bản cập nhật của bài đã duyệt bị từ chối, **Then** gia sư gửi bản khác được ngay (không đổi so với hiện tại).

---

### User Story 3 — Gia sư được báo thời điểm nộp lại qua email và thông báo (Priority: P2)

**Acceptance Scenarios**:

1. **Given** cán bộ học vụ từ chối hồ sơ, **Then** email và thông báo trong ứng dụng gửi gia sư nêu thời điểm được nộp lại (ngày từ chối + 7 ngày).
2. **Given** cán bộ học vụ từ chối bài đầu tiên, **Then** email nêu thời điểm được gửi bài mới; từ chối bản cập nhật thì email nêu gia sư có thể gửi bản khác ngay.

### Edge Cases

- Hồ sơ bị từ chối trước khi có bảng lịch sử (không tìm thấy dòng từ chối): coi như đã hết thời gian chờ, cho nộp lại ngay.
- Bài bị từ chối không có thời điểm duyệt (dữ liệu cũ): coi như đã hết thời gian chờ.
- Gói hội viên hết hạn khi hết 7 ngày: vẫn bị chặn bởi điều kiện gói (thông báo gói hội viên như hiện tại).
- Đúng thời điểm T + 7 ngày: được nộp (mốc tính "từ" thời điểm đó).
- Giờ hiển thị theo Asia/Ho_Chi_Minh.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Thời gian chờ sau khi bị từ chối là 7 ngày, tính từ thời điểm cán bộ học vụ từ chối, áp dụng chung cho hồ sơ xác minh và bài đăng.
- **FR-002**: Hồ sơ xác minh "Bị từ chối" chỉ được sửa thông tin, thêm / sửa / gỡ giấy tờ và nộp khi thời điểm hiện tại ≥ thời điểm được nộp lại.
- **FR-003**: Trong thời gian chờ, mọi thao tác sửa / nộp hồ sơ bị từ chối trả lỗi mã `VERIFICATION_REJECTED` (409) với lời nhắn tiếng Việt có thời điểm được nộp lại.
- **FR-004**: API hồ sơ xác minh của gia sư trả thêm `resubmitAvailableAt` khi hồ sơ "Bị từ chối"; `editable` đúng theo FR-002.
- **FR-005**: Khi kiểm tra điều kiện nộp, giấy tờ "Không được chấp nhận" không được tính là đã nộp.
- **FR-006**: Bài đăng mới nhất "Bị từ chối" mà gia sư chưa có bài đã duyệt chỉ được gửi bài mới khi thời điểm hiện tại ≥ thời điểm bị từ chối + 7 ngày; gửi tạo bài mới, không sửa bài bị từ chối.
- **FR-007**: Trong thời gian chờ, gửi bài trả lỗi 409 với lời nhắn có thời điểm được gửi bài mới.
- **FR-008**: API "Bài đăng của tôi" trả thêm `resubmitAvailableAt` khi bài mới nhất bị từ chối và chưa có bài đã duyệt.
- **FR-009**: Bản cập nhật của bài đã duyệt bị từ chối: giữ hành vi hiện tại (gửi lại ngay, không có `resubmitAvailableAt`).
- **FR-010**: Email và thông báo trong ứng dụng khi từ chối hồ sơ / bài đầu tiên nêu thời điểm được nộp lại; bỏ câu "quyết định cuối cùng / không gửi lại được".
- **FR-011**: Trang của gia sư hiển thị thời điểm được nộp lại khi còn chờ, mở biểu mẫu khi đã hết chờ; chữ tiếng Việt, không in hoa, không lộ mã.

### Key Entities

- **Lần từ chối hồ sơ**: dòng lịch sử thẩm định hành động "Từ chối" mới nhất của gia sư (thời điểm tạo = thời điểm từ chối).
- **Bài đăng bị từ chối**: dòng `tutor_posts` trạng thái "Bị từ chối", thời điểm duyệt = thời điểm từ chối.
- Không thêm bảng, không thêm cột.

## Success Criteria *(mandatory)*

- **SC-001**: 100% gia sư bị từ chối được nộp lại đúng sau 7 ngày (kiểm bằng test ở mốc 7 ngày − 1 phút và đúng 7 ngày).
- **SC-002**: Không thao tác sửa / nộp nào thành công trong thời gian chờ.
- **SC-003**: Gia sư biết chính xác ngày giờ được nộp lại trên trang, email và thông báo.
- **SC-004**: Mọi test cũ của xác minh và bài đăng (trừ test mô tả "từ chối là cuối cùng", được cập nhật theo quy tắc mới) vẫn xanh.

## Assumptions

- Thời gian chờ cố định 7 ngày trong mã (không thêm cấu hình hệ thống).
- Không cần migration.
- Tài liệu SRS do người phụ trách tự sửa theo mục "Ảnh hưởng tới SRS" trong [plan.md](plan.md).
