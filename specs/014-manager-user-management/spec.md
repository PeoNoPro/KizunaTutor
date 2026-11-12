# Feature Specification: FE-14 — Quản lý người dùng (gia sư, học viên) cho Quản lý vận hành

**Feature Branch**: `han` (thư mục đặc tả `specs/014-manager-user-management`)

**Created**: 2026-11-13

**Status**: Draft — đã tự làm rõ theo SRS (phiên 2026-11-13)

**Input**: Yêu cầu người phụ trách (Hân), 2026-11-13: "thêm phần của manager đó là thêm phần quản lý người dùng nữa, làm theo đúng quy trình, không được chỉnh sửa code của người khác làm mất chức năng của họ."

**Nguồn nghiệp vụ**: SRS `Report-3_Software Requirement Specification (7).docx`:

| UC | Tên trong SRS | Mục đặc tả SRS | Tác nhân |
| --- | --- | --- | --- |
| UC-20 | View Tutor and Learner List | II.2.3 | Manager |
| UC-21 | View Tutor and Learner Details | II.2.4 | Manager |
| UC-12 | Update Tutor and Learner Status | II.1.12 | Manager |

Thực thể `User` (SRS 3.2): trạng thái vòng đời Active · Inactive · Suspended/Locked. Quy tắc "Soft delete / Account deactivation only": tài khoản không bao giờ bị xóa hẳn, chỉ chuyển trạng thái.

## Phạm vi

**Trong phạm vi**: trang "Quản lý người dùng" của Quản lý vận hành gồm danh sách gia sư / học viên (UC-20), xem chi tiết hồ sơ (UC-21), đổi trạng thái tài khoản Hoạt động / Tạm khóa / Khóa vĩnh viễn kèm lý do nội bộ, lời nhắn tùy chọn gửi người dùng, ghi nhật ký và gửi email (UC-12).

**Ngoài phạm vi**: sửa thông tin hồ sơ người dùng; xóa tài khoản; quản lý tài khoản nhân viên (UC-09…UC-11 — đã thuộc FE-11); thay đổi luồng đăng nhập (đã chặn tài khoản không hoạt động — FE-01); thay đổi trang "Nhật ký thao tác" của Quản trị viên (FE-11); hồ sơ xác minh gia sư (FE-03) và gói hội viên (FE-09) — chỉ hiển thị trạng thái tóm tắt.

## Clarifications

### Session 2026-11-13 (người phụ trách yêu cầu không hỏi lại; các điểm dưới đây chọn theo SRS và hệ thống hiện có)

- Q: Các trạng thái Quản lý được chọn? → A: Theo SRS UC-12 bước 3: **Đang hoạt động**, **Tạm khóa**, **Khóa vĩnh viễn** (giá trị đang có trong cơ sở dữ liệu: ACTIVE, SUSPENDED, BANNED).
- Q: Tài khoản "Chờ kích hoạt" (đăng ký chưa xác nhận OTP) có được đổi trạng thái không? → A: **Không.** Kích hoạt tài khoản là việc của luồng đăng ký (FE-01); Quản lý chỉ xem. Tránh việc mở tài khoản chưa xác minh email.
- Q: "Vô hiệu hóa mọi phiên đăng nhập" (UC-12 bước 8) làm thế nào? → A: Hệ thống đã đọc lại trạng thái tài khoản ở mỗi yêu cầu; tài khoản không còn "Đang hoạt động" bị từ chối ngay ở yêu cầu kế tiếp và không đăng nhập lại được. Không cần lưu thêm gì.
- Q: "Activity Audit Log" lưu ở đâu? → A: Bảng nhật ký hệ thống đã có (thực thể SystemAuditLog). Quản lý xem lại được lịch sử đổi trạng thái của từng người trong trang chi tiết (POST-2 của UC-12: có dấu vết rõ ràng).
- Q: Thông báo tới người dùng? → A: Email tự động (UC-12 bước 10). Không gửi thông báo trong ứng dụng vì tài khoản bị khóa không vào được hệ thống để đọc.
- Q: Hai Quản lý cùng đổi trạng thái một người? → A: Người lưu sau nhận thông báo trạng thái đã bị thay đổi và phải tải lại; không ghi đè im lặng.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Quản lý xem danh sách gia sư và học viên (Priority: P1)

Quản lý mở trang "Quản lý người dùng", chọn nhóm "Gia sư" hoặc "Học viên", xem bảng gồm họ tên, email, ngày đăng ký, trạng thái tài khoản; tìm theo tên hoặc email và lọc theo trạng thái.

**Why this priority**: Là điểm vào của mọi thao tác quản lý người dùng (UC-20); dùng được độc lập.

**Independent Test**: Đăng nhập tài khoản Quản lý, mở trang, đổi qua lại hai nhóm, tìm một email có thật, lọc "Tạm khóa"; số dòng khớp dữ liệu trong hệ thống.

**Acceptance Scenarios**:

1. **Given** hệ thống có gia sư và học viên, **When** Quản lý mở trang, **Then** hệ thống hiển thị danh sách nhóm mặc định (Gia sư) có phân trang, mới đăng ký trước, kèm tổng số.
2. **Given** đang xem danh sách, **When** chọn nhóm "Học viên", **Then** danh sách chỉ còn học viên và quay về trang đầu.
3. **Given** đang xem danh sách, **When** nhập tên hoặc email, hoặc chọn trạng thái, **Then** danh sách chỉ còn người phù hợp.
4. **Given** không có ai phù hợp, **When** tìm kiếm, **Then** hệ thống hiển thị "Không tìm thấy người dùng phù hợp với điều kiện tìm kiếm." (AF-1).
5. **Given** lỗi khi tải, **When** mở trang, **Then** hệ thống báo lỗi và cho thử lại (AF-2).
6. **Given** người dùng không phải Quản lý vận hành, **When** gọi chức năng, **Then** hệ thống từ chối truy cập.

---

### User Story 2 - Quản lý xem chi tiết hồ sơ gia sư / học viên (Priority: P1)

Từ danh sách, Quản lý chọn "Xem chi tiết" để xem thông tin cá nhân và tài khoản của người đó, rồi quay lại danh sách.

**Why this priority**: UC-21; cần để Quản lý đánh giá trước khi đổi trạng thái.

**Independent Test**: Mở chi tiết một gia sư và một học viên; thông tin hiển thị khớp hồ sơ; đóng chi tiết quay về đúng trang danh sách đang xem.

**Acceptance Scenarios**:

1. **Given** đang ở danh sách, **When** chọn "Xem chi tiết" một người, **Then** hệ thống hiển thị họ tên, email, số điện thoại, ảnh đại diện (nếu có), vai trò, trạng thái tài khoản, ngày đăng ký, lần cập nhật gần nhất.
2. **Given** người đó là gia sư, **Then** chi tiết có thêm: mã gia sư, tiêu đề giới thiệu, số năm kinh nghiệm, trình độ JLPT cao nhất, điểm đánh giá và số lượt đánh giá, giai đoạn hồ sơ gia sư, địa chỉ, ngày sinh, ngày được duyệt.
3. **Given** người đó là học viên, **Then** chi tiết có thêm: trình độ hiện tại, trình độ mục tiêu, mục tiêu học tập, tỉnh / thành, địa chỉ.
4. **Given** người đó đã từng bị đổi trạng thái, **Then** chi tiết có lịch sử đổi trạng thái (thời gian, người thực hiện, từ → sang, lý do nội bộ, lời nhắn đã gửi), mới nhất trước.
5. **Given** lỗi khi tải chi tiết hoặc tài khoản không còn tồn tại, **Then** hệ thống báo lỗi và cho thử lại (AF-1).
6. **Given** đang xem chi tiết, **When** đóng, **Then** quay về danh sách với bộ lọc và trang đang xem.

---

### User Story 3 - Quản lý đổi trạng thái tài khoản (Priority: P1)

Từ danh sách hoặc chi tiết, Quản lý bấm "Cập nhật trạng thái", chọn trạng thái mới, nhập lý do nội bộ (bắt buộc) và lời nhắn gửi người dùng (tùy chọn), xác nhận. Hệ thống cập nhật trạng thái, chặn ngay quyền truy cập nếu là Tạm khóa / Khóa vĩnh viễn, ghi nhật ký, gửi email và báo thành công.

**Why this priority**: UC-12 — công cụ thực thi chính sách nền tảng.

**Independent Test**: Tạm khóa một tài khoản thử → tài khoản đó đang đăng nhập bị từ chối ở thao tác kế tiếp và không đăng nhập lại được; mở lại "Đang hoạt động" → đăng nhập lại được; lịch sử ghi đúng hai lần đổi.

**Acceptance Scenarios**:

1. **Given** tài khoản đang hoạt động, **When** Quản lý chọn "Tạm khóa", nhập lý do và xác nhận, **Then** trạng thái thành "Tạm khóa", danh sách / chi tiết làm mới và hiển thị "Đã cập nhật trạng thái tài khoản thành Tạm khóa."
2. **Given** tài khoản vừa bị Tạm khóa hoặc Khóa vĩnh viễn, **When** người đó thao tác tiếp hoặc đăng nhập lại, **Then** hệ thống từ chối (bước 8).
3. **Given** tài khoản đang Tạm khóa, **When** Quản lý chọn "Đang hoạt động", **Then** người đó đăng nhập lại được và email báo đã khôi phục quyền truy cập (AF-1).
4. **Given** biểu mẫu đổi trạng thái đang mở, **When** Quản lý bấm Hủy hoặc đóng, **Then** không có gì thay đổi (AF-2).
5. **Given** Quản lý để trống lý do hoặc chọn trùng trạng thái hiện tại, **When** xác nhận, **Then** hệ thống không lưu và chỉ rõ chỗ cần sửa.
6. **Given** đổi trạng thái thành công, **Then** có một bản ghi nhật ký gồm Quản lý thực hiện, trạng thái cũ / mới, lý do nội bộ, lời nhắn, thời gian (bước 9, POST-2).
7. **Given** đổi trạng thái thành công, **Then** người dùng nhận email nêu trạng thái mới và lời nhắn (nếu có); email không chứa lý do nội bộ (bước 10).
8. **Given** tài khoản đang "Chờ kích hoạt", **Then** không có thao tác đổi trạng thái và hệ thống giải thích lý do.
9. **Given** hai Quản lý cùng đổi trạng thái một người, **When** người thứ hai xác nhận, **Then** hệ thống báo trạng thái vừa được thay đổi, yêu cầu tải lại, không ghi đè.
10. **Given** gửi email thất bại, **Then** trạng thái vẫn được lưu; lỗi chỉ ghi vào log hệ thống.

---

### Edge Cases

- Từ khóa có ký tự `%`, `_`, `\` → tìm đúng ký tự đó, không lỗi.
- Từ khóa quá 100 ký tự, số trang âm, kích thước trang ngoài 10/20/50 → hệ thống tự đưa về giá trị hợp lệ, không lỗi.
- Mã người dùng không tồn tại, đã bị xóa mềm, hoặc là tài khoản nhân viên → "Không tìm thấy tài khoản gia sư hoặc học viên này." (không lộ việc tài khoản nhân viên tồn tại).
- Gia sư chưa có hồ sơ gia sư / học viên chưa có hồ sơ học viên → vẫn xem được thông tin tài khoản; phần hồ sơ hiện "Chưa cập nhật".
- Lý do quá 1.000 ký tự, lời nhắn quá 1.000 ký tự → báo lỗi tại ô tương ứng.
- Mất kết nối khi đang lưu → báo lỗi, giữ nguyên nội dung đã nhập để thử lại; trạng thái cũ không đổi (POST của AF-2).

## Requirements *(mandatory)*

### Functional Requirements

**Danh sách (UC-20)**

- **FR-001**: Hệ thống PHẢI cho Quản lý xem danh sách theo nhóm Gia sư hoặc Học viên, mỗi dòng gồm họ tên, email, ngày đăng ký, trạng thái tài khoản; phân trang 10 / 20 / 50 dòng, mới đăng ký trước, có tổng số.
- **FR-002**: Hệ thống PHẢI cho tìm theo họ tên hoặc email (không phân biệt hoa thường, tối đa 100 ký tự) và lọc theo trạng thái (Chờ kích hoạt, Đang hoạt động, Tạm khóa, Khóa vĩnh viễn).
- **FR-003**: Danh sách chỉ gồm tài khoản gia sư, học viên chưa bị xóa; không bao giờ có tài khoản nhân viên.

**Chi tiết (UC-21)**

- **FR-004**: Hệ thống PHẢI hiển thị thông tin tài khoản và hồ sơ theo vai trò như User Story 2, kịch bản 1–3. Không bao giờ trả mật khẩu, mã băm hay token.
- **FR-005**: Chi tiết PHẢI có lịch sử đổi trạng thái của người đó (tối đa 20 lần gần nhất), mới nhất trước.
- **FR-006**: Xem danh sách và chi tiết không làm thay đổi dữ liệu (POST-2 của UC-20, UC-21).

**Đổi trạng thái (UC-12)**

- **FR-007**: Hệ thống PHẢI cho đổi giữa Đang hoạt động, Tạm khóa, Khóa vĩnh viễn; trạng thái mới phải khác trạng thái hiện tại. Tài khoản Chờ kích hoạt không được đổi.
- **FR-008**: Lý do nội bộ bắt buộc (1–1.000 ký tự); lời nhắn gửi người dùng tùy chọn (tối đa 1.000 ký tự).
- **FR-009**: Khi trạng thái mới không phải Đang hoạt động, tài khoản PHẢI mất quyền truy cập ngay từ yêu cầu kế tiếp và không đăng nhập được; khi về Đang hoạt động, đăng nhập lại được.
- **FR-010**: Cập nhật trạng thái và ghi nhật ký PHẢI cùng thành công hoặc cùng không xảy ra. Nhật ký gồm: Quản lý thực hiện, người bị đổi, trạng thái cũ, trạng thái mới, lý do nội bộ, lời nhắn, thời điểm.
- **FR-011**: Sau khi lưu thành công, hệ thống PHẢI gửi email cho người dùng nêu trạng thái mới (khôi phục quyền truy cập khi về Đang hoạt động) và lời nhắn nếu có; không chứa lý do nội bộ. Email lỗi không hoàn tác thay đổi.
- **FR-012**: Nếu trạng thái đã bị thay đổi kể từ lúc Quản lý mở biểu mẫu, hệ thống PHẢI từ chối và yêu cầu tải lại.
- **FR-013**: Hủy hoặc đóng biểu mẫu không thay đổi dữ liệu.

**Chung**

- **FR-014**: Chỉ Quản lý vận hành dùng được các chức năng của feature này; API tự từ chối vai trò khác.
- **FR-015**: Mọi nhãn, thông báo, lỗi bằng tiếng Việt tự nhiên, không viết hoa toàn bộ, không lộ mã vai trò, mã trạng thái, tên bảng.
- **FR-016**: Khi đang tải có khung chờ tải (skeleton); không có dữ liệu có trạng thái rỗng; lỗi tải có thông báo và nút thử lại.

### Key Entities *(include if feature involves data)*

- **Tài khoản người dùng** (đã có): họ tên, email (duy nhất), số điện thoại, ảnh đại diện, vai trò, trạng thái tài khoản, ngày đăng ký, ngày cập nhật, ngày xóa mềm.
- **Hồ sơ gia sư** (đã có): mã gia sư, tiêu đề, kinh nghiệm, JLPT cao nhất, điểm đánh giá, giai đoạn hồ sơ, địa chỉ, ngày sinh, ngày duyệt.
- **Hồ sơ học viên** (đã có): trình độ hiện tại, mục tiêu, mục tiêu học tập, tỉnh / thành, địa chỉ.
- **Nhật ký hệ thống** (đã có): người thực hiện, loại thao tác, đối tượng, chi tiết, thời điểm. Mỗi lần đổi trạng thái là một bản ghi.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Quản lý tìm được một gia sư hoặc học viên bất kỳ bằng tên hoặc email trong tối đa 3 thao tác.
- **SC-002**: Sau khi Tạm khóa / Khóa vĩnh viễn, 100% yêu cầu tiếp theo của tài khoản đó bị từ chối và đăng nhập lại thất bại; sau khi mở lại, đăng nhập thành công.
- **SC-003**: Mỗi lần đổi trạng thái thành công tạo đúng một bản ghi nhật ký; lần hủy, lần lỗi, lần bị từ chối không tạo bản ghi nào.
- **SC-004**: Danh sách trang đầu hiển thị trong dưới 2 giây với dữ liệu hiện có.
- **SC-005**: 100% chữ trên màn hình bằng tiếng Việt, không chữ in hoa toàn bộ, không lộ mã kỹ thuật.

## Assumptions

- A-1: Tài khoản Quản lý vận hành đã có (5 tài khoản tại 2026-11-13); nhãn vai trò trên màn hình là "Quản lý vận hành".
- A-2: Nhãn trạng thái: Chờ kích hoạt (PENDING), Đang hoạt động (ACTIVE), Tạm khóa (SUSPENDED), Khóa vĩnh viễn (BANNED — SRS "Permanently Banned").
- A-3: Mọi Quản lý xem được lịch sử đổi trạng thái do Quản lý khác thực hiện.
- A-4: Chỉ có cơ sở dữ liệu Supabase dùng chung; nghiệm thu thao tác ghi trên dữ liệu thật phải dùng tài khoản thử và trả trạng thái về như cũ.
- A-5: Gửi email dùng bộ gửi thư chung đã có của hệ thống.
