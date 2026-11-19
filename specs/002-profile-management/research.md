# Nghiên cứu kỹ thuật & Quyết định thiết kế: FE-02 — Quản lý hồ sơ Học viên & Gia sư (Profile Management)

**Ngày lập**: 2026-11-10  
**Căn cứ**: [spec.md](spec.md) (kèm 5 quyết định làm rõ từ `$speckit-clarify`), Hiến pháp dự án (`constitution.md`), và hiện trạng mã nguồn KizunaTutor.

---

## 1. Hiện trạng mã nguồn liên quan

| Thành phần | Hiện trạng trong dự án | Tác động & Giải pháp cho FE-02 |
| :--- | :--- | :--- |
| `core/entity/User` | Đã có sẵn: `userId`, `email`, `fullName`, `phone`, `avatarUrl`, `accountStatus`, `role`, `mustChangePassword`, `deletedAt`. | Thực thể người dùng trung tâm. Khóa trường `email` ở chế độ chỉ đọc; cập nhật `fullName`, `phone`, `avatarUrl`, `accountStatus`. |
| `core/entity/Learner` | Đã có sẵn: `learnerId`, `user` (OneToOne), `currentLevel`, `targetLevel`, `learningGoals`, `updatedAt`. | Dữ liệu hồ sơ học viên. Hỗ trợ `currentLevel` nhận giá trị `"BEGINNER"` và `"N5"` đến `"N1"`. |
| `core/entity/Tutor` | Đã có sẵn: `tutorId`, `tutorCode`, `user` (OneToOne), `headline`, `bio`, `experienceYears`, `hourlyRate`, `highestJlptLevel`, `address`, `phone`. | Dữ liệu hồ sơ giảng dạy gia sư. Cho phép cập nhật tiêu đề, tiểu sử, số năm kinh nghiệm, học phí, địa chỉ. |
| `features/profile/` | Chỉ có các thư mục con chứa `.gitkeep` (`controller`, `dto`, `mapper`, `service`). | Xây dựng toàn bộ các tầng Controller, Service, DTO, Mapper mới tại đúng feature `profile`. |
| `core/security` | `JwtTokenProvider` đã trích xuất được `email`, `role`, `userId` từ token. `SecurityConfig` hỗ trợ cấu hình kiểm tra phân quyền. | Phân quyền endpoint theo vai trò qua Spring Security: `/api/profile/learner/**` cho `ROLE_LEARNER`, `/api/profile/tutor/**` cho `ROLE_TUTOR`, `/api/profile/users/**` cho `ROLE_MANAGER`/`ROLE_ADMIN`. |
| `frontend/` | Đã có `pages/tutor-profile/` (xem chi tiết & đặt lịch), `pages/home/`, `js/api.js`. | Tạo mới `frontend/pages/learner-profile/` và `frontend/pages/manager-users/`; bổ sung modal chỉnh sửa trên `frontend/pages/tutor-profile/`. |

---

## 2. Các quyết định thiết kế cốt lõi (Decisions & Rationales)

### Quyết định 1: Cấu trúc màn hình giao diện người dùng (Frontend Architecture)
- **Quyết định**:
  - Học viên: Tạo trang độc lập tại `frontend/pages/learner-profile/` (`index.html`, `learner-profile.js`).
  - Gia sư: Tích hợp nút "Chỉnh sửa hồ sơ" và Modal/Drawer cập nhật trực tiếp trên trang `frontend/pages/tutor-profile/`. Nút này chỉ xuất hiện khi người dùng đăng nhập chính là chủ sở hữu của hồ sơ gia sư đó.
  - Quản lý vận hành: Tạo trang Quản lý người dùng tại `frontend/pages/manager-users/` (`index.html`, `manager-users.js`) phục vụ UC-20, UC-21 và UC-1.12.
- **Lý do**:
  - Học viên cần một không gian trang nhã, riêng tư để xem lại tiến trình và mục tiêu tiếng Nhật của mình.
  - Gia sư nhìn thấy ngay giao diện công khai mà học viên sẽ thấy (WYSIWYG); khi bấm sửa và lưu thì giao diện cập nhật ngay lập tức.
  - Tối ưu tái sử dụng các component dùng chung (`header.html`, `footer.html`) theo chuẩn `components_rule.md` và bảng màu Wabi-Sabi (`design.md`).
- **Phương án thay thế đã loại bỏ**: Nhồi nhét toàn bộ vào dashboard làm tăng độ phức tạp của mã nguồn dashboard và khó bảo trì riêng lẻ.

---

### Quyết định 2: Cơ chế cập nhật và lưu trữ Ảnh đại diện (Avatar)
- **Quyết định**:
  - Hỗ trợ cả 2 hình thức:
    1. **Tải file ảnh lên (`POST /api/profile/avatar`, `multipart/form-data`)**: Tệp ảnh được lưu trữ an toàn trong thư mục cục bộ của máy chủ (`${user.home}/kizuna-data/avatars/` hoặc `./uploads/avatars/`). Backend trả về URL truy cập ảnh (`/api/profile/avatar/{filename}`).
    2. **Đường dẫn URL trực tiếp**: Người dùng có thể dán đường dẫn ảnh hợp lệ (bắt đầu bằng `http://` hoặc `https://`).
  - Kiểm tra an toàn tệp: Dung lượng tối đa 5MB, chỉ chấp nhận đuôi mở rộng `.jpg`, `.jpeg`, `.png`, `.webp`, kiểm tra mime-type và Magic Bytes cơ bản.
- **Lý do**: Đáp ứng đầy đủ câu trả lời làm rõ Q2; tiện lợi cho cả người dùng muốn upload ảnh từ điện thoại/máy tính lẫn người dùng sử dụng ảnh từ URL mạng xã hội.
- **Phương án thay thế đã loại bỏ**: Chỉ dùng URL thì bất tiện cho người dùng không có chỗ lưu trữ ảnh online; chỉ dùng tải tệp thì mất tính linh hoạt của các tài khoản đăng nhập qua mạng xã hội (Google).

---

### Quyết định 3: Tính toàn vẹn của Email và Đồng bộ thông tin cá nhân
- **Quyết định**:
  - Địa chỉ Email là trường **chỉ đọc (Read-only)** trong toàn bộ phân hệ Hồ sơ cá nhân. Không cho phép sửa đổi qua API profile.
  - Khi cập nhật thông tin cá nhân (Họ tên, SĐT, Ảnh đại diện):
    - Học viên: Cập nhật đồng thời vào bảng `users` (`full_name`, `phone`, `avatar_url`) và bảng `learners` (`current_level`, `target_level`, `learning_goals`).
    - Gia sư: Cập nhật đồng thời vào bảng `users` (`full_name`, `phone`, `avatar_url`) và bảng `tutors` (`headline`, `bio`, `experience_years`, `hourly_rate`, `highest_jlpt_level`, `address`, `phone`).
- **Lý do**: Email là Subject của JWT Token và định danh duy nhất trong hệ thống; việc giữ Email chỉ đọc loại trừ nguy cơ mất an toàn phiên đăng nhập. Việc đồng bộ đảm bảo không có sự mâu thuẫn giữa thông tin tài khoản chung (`users`) và thông tin vai trò (`learners`, `tutors`).

---

### Quyết định 4: Chuẩn hóa Trình độ JLPT & Địa chỉ khu vực
- **Quyết định**:
  - **Trình độ của học viên**: Cho phép nhận các giá trị: `"BEGINNER"` (Mới bắt đầu), `"N5"`, `"N4"`, `"N3"`, `"N2"`, `"N1"`.
  - **Địa chỉ của gia sư**: Form cập nhật gồm: Dropdown chọn Tỉnh/Thành phố (danh mục 63 tỉnh thành Việt Nam khớp với API `/api/tutors/locations`) và Ô văn bản nhập chi tiết (Quận/Huyện, phường xã, số nhà). Chuỗi lưu vào database ghép theo định dạng: `"[Tỉnh/Thành] - [Địa chỉ chi tiết]"`.
- **Lý do**:
  - Giúp học viên mới bắt đầu không bị bối rối khi chưa đạt N5.
  - Đảm bảo dữ liệu Tỉnh/Thành của gia sư khớp 100% với bộ lọc địa lý của trang tìm kiếm (`tutor-discovery`).

---

### Quyết định 5: Quản trị người dùng & Thay đổi trạng thái tài khoản (UC-20, UC-21, UC-1.12)
- **Quyết định**:
  - Trang quản trị người dùng dành cho Manager/Admin hỗ trợ truy vấn phân trang `PageResponse<UserProfileListItemDto>` với các tiêu chí lọc:
    - Vai trò (`role`): `ALL`, `ROLE_TUTOR`, `ROLE_LEARNER`.
    - Trạng thái (`status`): `ALL`, `ACTIVE`, `SUSPENDED`, `BANNED`.
    - Từ khóa tìm kiếm (`keyword`): Tìm kiếm không phân biệt hoa thường theo Họ tên hoặc Email.
  - Khi Quản lý đổi trạng thái tài khoản sang `SUSPENDED` hoặc `BANNED`:
    - Bắt buộc nhập lý do hành chính nội bộ (`reason`).
    - Cập nhật `users.account_status`.
    - Ghi nhận thông tin vào log hệ thống để phục vụ kiểm toán an ninh (Audit trail).
- **Lý do**: Tuân thủ tuyệt đối UC-20, UC-21, UC-1.12 và Business Rule GB-05 trong SRS.

