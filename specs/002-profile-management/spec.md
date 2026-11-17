# Đặc tả tính năng: Quản lý hồ sơ Học viên & Gia sư (Learner & Tutor Profile Management)

**Feature Branch**: `haipt`  
**Feature Code**: `FE-02` (theo SRS KizunaTutor)  
**Created**: 2026-11-10  
**Status**: Draft  
**Input**: Khảo sát nghiệp vụ SRS Section 2 (UC-18, UC-19, UC-20, UC-21) và phân tích đối chiếu mã nguồn hiện tại phục vụ triển khai code sắp tới.

---

## 1. Bối cảnh & Mục tiêu nghiệp vụ

Phân hệ Quản lý hồ sơ Học viên & Gia sư (FE-02) giữ vai trò trung tâm kết nối người học và người dạy trên nền tảng KizunaTutor:
1. **Học viên (Learner)**: Lưu trữ và quản lý lộ trình học tiếng Nhật cá nhân (trình độ hiện tại N5-N1, trình độ mục tiêu, nguyện vọng/mục đích học tập, thông tin liên lạc và ảnh đại diện).
2. **Gia sư (Tutor)**: Quản lý thông tin giảng dạy công khai (tiêu đề giới thiệu, tiểu sử, số năm kinh nghiệm, học phí theo giờ, địa chỉ/khu vực giảng dạy, cấp độ JLPT cao nhất) và theo dõi chất lượng hiển thị hồ sơ đối với người học.
3. **Quản lý vận hành (Manager)**: Tra cứu, giám sát danh sách người dùng (lọc theo vai trò Học viên/Gia sư, trạng thái hoạt động), xem chi tiết hồ sơ và cập nhật trạng thái tài khoản (Kích hoạt, Tạm khóa, Cấm vĩnh viễn) kèm lý do nội bộ để bảo vệ an toàn cho nền tảng.

## Clarifications

### Session 2026-11-10

- Q: Giao diện xem và chỉnh sửa hồ sơ của Học viên và Gia sư nên được tổ chức theo cấu trúc màn hình nào trên Frontend? → A: Option A - Tạo trang riêng `/pages/learner-profile/` cho Học viên; đối với Gia sư, tích hợp nút "Chỉnh sửa hồ sơ" mở Modal/Drawer cập nhật ngay trên trang `/pages/tutor-profile/` khi gia sư xem hồ sơ của chính mình.
- Q: Người dùng (Học viên và Gia sư) cập nhật ảnh đại diện (Avatar) bằng phương thức nào? → A: Option A - Hỗ trợ cả 2 phương thức: Cho phép tải file ảnh từ máy tính lên (Backend lưu trữ tại thư mục tệp máy chủ và sinh URL) HOẶC dán trực tiếp đường dẫn link ảnh hợp lệ.
- Q: Trường địa chỉ Email trên giao diện chỉnh sửa hồ sơ có cho phép người dùng tự do thay đổi không? → A: Option A - Địa chỉ Email là trường chỉ đọc (Read-only / Khóa chỉnh sửa) trên trang Hồ sơ cá nhân; người dùng chỉ được cập nhật Họ tên, Số điện thoại, Ảnh đại diện, Trình độ và Mục tiêu/Tiểu sử.
- Q: Danh mục lựa chọn cho "Trình độ tiếng Nhật hiện tại" (currentLevel) của Học viên nên bao gồm những giá trị nào? → A: Option A - Cho phép chọn: "Mới bắt đầu" (BEGINNER) và 5 cấp độ JLPT chuẩn quốc tế (N5, N4, N3, N2, N1) để phản ánh trung thực năng lực của học viên khi kết nối với gia sư.
- Q: Khi Gia sư cập nhật địa chỉ khu vực giảng dạy, hệ thống nên thiết kế ô nhập địa chỉ theo hình thức nào? → A: Option A - Gia sư chọn Tỉnh/Thành phố từ Dropdown danh mục chuẩn kết hợp ô nhập chi tiết Quận/Huyện/địa chỉ, đảm bảo khớp 100% với bộ lọc địa điểm của trang tìm kiếm gia sư.

---

## 2. Kịch bản người dùng & Kiểm thử chấp nhận (User Scenarios & Testing)

### User Story 1 – Học viên xem và cập nhật hồ sơ cá nhân (Priority: P1)

Là một Học viên đã đăng nhập, tôi muốn xem thông tin hồ sơ của mình và có thể chỉnh sửa mục tiêu học tập, trình độ tiếng Nhật cũng như thông tin cá nhân, để hồ sơ luôn phản ánh đúng nhu cầu học tập thực tế.

- **Giá trị ưu tiên**: Nền tảng cốt lõi giúp cá nhân hóa trải nghiệm học viên và làm cơ sở để gia sư hiểu rõ nhu cầu của học viên khi kết nối.
- **Kiểm thử độc lập**: Đăng nhập với vai trò Học viên, truy cập trang hồ sơ cá nhân, thực hiện cập nhật thông tin và kiểm tra dữ liệu hiển thị đã được cập nhật chính xác.

**Kịch bản chấp nhận**:
1. **Kịch bản 1.1 (Xem hồ sơ học viên thành công)**:
   - **Given**: Học viên đã đăng nhập thành công vào hệ thống.
   - **When**: Học viên mở trang "Hồ sơ cá nhân" từ thanh điều hướng hoặc menu người dùng.
   - **Then**: Hệ thống hiển thị đầy đủ: Ảnh đại diện, Họ và tên, Email, Số điện thoại, Trình độ hiện tại (ví dụ: N5), Trình độ mục tiêu (ví dụ: N3) và Mục tiêu học tập chi tiết.
2. **Kịch bản 1.2 (Cập nhật thông tin hợp lệ)**:
   - **Given**: Học viên đang ở giao diện chỉnh sửa hồ sơ.
   - **When**: Học viên thay đổi họ tên, số điện thoại, đổi trình độ mục tiêu từ N3 sang N2, cập nhật mục tiêu học và nhấn "Lưu thay đổi".
   - **Then**: Hệ thống xác thực dữ liệu, lưu vào cơ sở dữ liệu, hiển thị thông báo thành công "Cập nhật hồ sơ thành công" và làm mới dữ liệu trên màn hình mà không cần tải lại toàn trang.
3. **Kịch bản 1.3 (Dữ liệu không hợp lệ)**:
   - **Given**: Học viên nhập số điện thoại sai định dạng hoặc để trống họ tên.
   - **When**: Học viên bấm "Lưu thay đổi".
   - **Then**: Hệ thống giữ nguyên form, khoanh đỏ trường bị lỗi và hiển thị thông điệp hướng dẫn bằng tiếng Việt (ví dụ: "Số điện thoại không đúng định dạng 10 chữ số"), không gửi dữ liệu sai xuống server.

---

### User Story 2 – Gia sư xem và cập nhật thông tin giảng dạy (Priority: P1)

Là một Gia sư đã đăng nhập, tôi muốn xem trước hồ sơ công khai của mình (như góc nhìn của học viên) và cập nhật thông tin giảng dạy (tiêu đề, tiểu sử, học phí đề xuất, số năm kinh nghiệm, khu vực), để thu hút học viên phù hợp.

- **Giá trị ưu tiên**: Giúp gia sư tự hoàn thiện thương hiệu cá nhân, cập nhật học phí và sẵn sàng tiếp nhận yêu cầu học thử.
- **Kiểm thử độc lập**: Đăng nhập với vai trò Gia sư, xem giao diện hồ sơ cá nhân, bấm "Chỉnh sửa thông tin", lưu thay đổi và kiểm tra giao diện xem trước công khai.

**Kịch bản chấp nhận**:
1. **Kịch bản 2.1 (Xem trước hồ sơ gia sư)**:
   - **Given**: Gia sư đã có tài khoản và hồ sơ trong hệ thống.
   - **When**: Gia sư truy cập màn hình hồ sơ của mình.
   - **Then**: Hệ thống hiển thị giao diện xem trước gồm ảnh đại diện, họ tên, danh hiệu/tiêu đề giảng dạy, số năm kinh nghiệm, mức học phí/giờ, trình độ JLPT cao nhất, địa chỉ khu vực, tiểu sử tóm tắt và đánh giá từ học viên.
2. **Kịch bản 2.2 (Cập nhật thông tin giảng dạy)**:
   - **Given**: Gia sư mở form cập nhật hồ sơ giảng dạy.
   - **When**: Gia sư điều chỉnh tiêu đề ("Gia sư tiếng Nhật giao tiếp thực chiến"), học phí đề xuất ("250.000 đ/giờ"), số năm kinh nghiệm ("3 năm"), khu vực và lưu lại.
   - **Then**: Hệ thống lưu dữ liệu thành công, hiển thị thông báo tiếng Việt thân thiện, và thông tin mới lập tức được phản ánh trên trang tìm kiếm gia sư.
3. **Kịch bản 2.3 (Cảnh báo hồ sơ chưa hoàn thiện)**:
   - **Given**: Gia sư mới được phê duyệt tài khoản nhưng chưa nhập học phí hoặc tiêu đề giảng dạy.
   - **When**: Gia sư xem trang cá nhân.
   - **Then**: Hệ thống hiển thị thông báo nhắc nhở nhẹ nhàng: "Hồ sơ giảng dạy của bạn hiện chưa đầy đủ thông tin học phí và tiêu đề. Vui lòng cập nhật để học viên có thể tìm thấy bạn."

---

### User Story 3 – Quản lý vận hành tra cứu danh sách người dùng (Priority: P2)

Là một Quản lý vận hành (Manager) hoặc Quản trị viên (Admin), tôi muốn tra cứu và xem danh sách toàn bộ Học viên và Gia sư có trên hệ thống, phân loại theo vai trò và trạng thái, để quản lý người dùng nền tảng.

- **Giá trị ưu tiên**: Công cụ quản trị vận hành thiết yếu để giám sát hoạt động cộng đồng và bảo đảm an toàn cho nền tảng.
- **Kiểm thử độc lập**: Đăng nhập bằng tài khoản Manager, truy cập trang Quản lý người dùng, chuyển đổi giữa các tab "Tất cả", "Gia sư", "Học viên", tìm kiếm theo từ khóa và áp dụng bộ lọc trạng thái.

**Kịch bản chấp nhận**:
1. **Kịch bản 3.1 (Hiển thị danh sách phân trang)**:
   - **Given**: Manager đã đăng nhập và có quyền quản lý người dùng.
   - **When**: Manager mở trang "Quản lý người dùng".
   - **Then**: Hệ thống hiển thị bảng dữ liệu gồm: Ảnh & Họ tên, Email, Vai trò (Học viên / Gia sư), Ngày tham gia, Trạng thái tài khoản (Đang hoạt động, Tạm khóa, Đã cấm) kèm tính năng phân trang.
2. **Kịch bản 3.2 (Lọc theo danh mục và từ khóa)**:
   - **Given**: Manager đang ở trang danh sách người dùng.
   - **When**: Manager chọn tab "Gia sư" và nhập từ khóa tên hoặc email vào ô tìm kiếm.
   - **Then**: Bảng danh sách lập tức lọc đúng các gia sư có tên hoặc email trùng khớp, không trộn lẫn tài khoản học viên.
3. **Kịch bản 3.3 (Trạng thái rỗng khi không có kết quả)**:
   - **Given**: Manager tìm kiếm với từ khóa không tồn tại trên hệ thống.
   - **When**: Hệ thống trả về danh sách rỗng.
   - **Then**: Giao diện hiển thị trạng thái rỗng thân thiện: "Không tìm thấy người dùng nào phù hợp với điều kiện tìm kiếm." kèm nút "Xóa bộ lọc".

---

### User Story 4 – Quản lý xem chi tiết hồ sơ & quản lý trạng thái tài khoản (Priority: P2)

Là một Quản lý vận hành, tôi muốn xem chi tiết hồ sơ của một học viên hoặc gia sư cụ thể và có quyền cập nhật trạng thái tài khoản (Khóa, Mở khóa, Cấm) kèm ghi chú lý do, để xử lý các vi phạm hoặc hỗ trợ người dùng.

- **Giá trị ưu tiên**: Đảm bảo quyền kiểm soát an ninh, thực thi quy chế nền tảng và minh bạch lịch sử xử lý.
- **Kiểm thử độc lập**: Chọn một người dùng trong danh sách, mở xem chi tiết, chọn đổi trạng thái tài khoản sang "Tạm khóa", nhập lý do và kiểm tra tài khoản đó bị từ chối truy cập.

**Kịch bản chấp nhận**:
1. **Kịch bản 4.1 (Xem chi tiết hồ sơ từ trang quản trị)**:
   - **Given**: Manager chọn một người dùng trong danh sách và bấm "Xem chi tiết".
   - **When**: Hệ thống mở trang/modal chi tiết.
   - **Then**: Hiển thị đầy đủ thông tin định danh: Họ tên, Email, Số điện thoại, Vai trò, Ngày tạo, Thông tin giảng dạy (đối với gia sư) hoặc Mục tiêu học tập (đối với học viên), Lịch sử trạng thái tài khoản.
2. **Kịch bản 4.2 (Tạm khóa tài khoản kèm lý do)**:
   - **Given**: Người dùng có hành vi vi phạm quy chuẩn nền tảng.
   - **When**: Manager bấm "Đổi trạng thái", chọn "Tạm khóa", nhập lý do "Nhiều lần hủy lịch không báo trước" và bấm "Xác nhận".
   - **Then**: Hệ thống cập nhật trạng thái tài khoản sang `SUSPENDED`, ghi nhận log hành động của Manager vào hệ thống audit log, và hủy bỏ phiên làm việc hiện tại của tài khoản đó.
3. **Kịch bản 4.3 (Mở khóa kích hoạt lại tài khoản)**:
   - **Given**: Tài khoản đang ở trạng thái Tạm khóa sau khi đã giải trình khiếu nại thành công.
   - **When**: Manager đổi trạng thái về "Đang hoạt động" và xác nhận.
   - **Then**: Tài khoản được khôi phục quyền đăng nhập bình thường.

---

## 3. Các trường hợp biên & Xử lý lỗi (Edge Cases)

1. **Bảo mật và Phân quyền (Zero Backend Leaks & RBAC)**:
   - Học viên chỉ có quyền xem/sửa hồ sơ học viên của chính mình.
   - Gia sư chỉ có quyền xem/sửa thông tin giảng dạy của chính mình.
   - Nếu một học viên cố tình gọi API cập nhật hồ sơ của học viên khác bằng cách đổi ID trên đường dẫn, hệ thống BẮT BUỘC chặn lại ở tầng Controller/Security và trả về lỗi 403 Forbidden.
   - 100% thông báo lỗi trả về cho người dùng phải là tiếng Việt chuẩn mực; tuyệt đối không xuất hiện các chuỗi kỹ thuật như `AccessDeniedException`, `500 Server Error`, hay `user_id`.
2. **Trùng lặp thông tin định danh**:
   - Khi cập nhật số điện thoại hoặc email, nếu số điện thoại/email đó đã được tài khoản khác sử dụng, hệ thống phải báo lỗi rõ ràng: "Số điện thoại này đã được liên kết với một tài khoản khác trong hệ thống."
3. **Xử lý ảnh đại diện (Avatar URL / Upload)**:
   - Hệ thống chấp nhận đường dẫn ảnh hợp lệ (HTTP/HTTPS) hoặc tải ảnh lên với định dạng ảnh phổ biến (`jpg`, `jpeg`, `png`, `webp`), dung lượng tối đa 5MB. Từ chối tệp không đúng định dạng với thông báo lịch sự.
4. **Bảo vệ quyền riêng tư địa chỉ gia sư (Business Rule GB-15)**:
   - Khi hiển thị công khai trên hồ sơ gia sư, trường địa chỉ chỉ hiển thị Quận/Huyện, Tỉnh/Thành phố; không hiển thị số nhà hoặc tọa độ chi tiết nhằm bảo vệ an toàn cá nhân cho gia sư.
5. **Hủy phiên đăng nhập khi bị Khóa/Cấm (Business Rule GB-05)**:
   - Khi Manager chuyển trạng thái tài khoản sang `SUSPENDED` hoặc `BANNED`, token đăng nhập hiện tại của người dùng lập tức mất hiệu lực, người dùng không thể thực hiện thêm bất kỳ hành vi nào trên nền tảng.

---

## 4. Yêu cầu chức năng (Functional Requirements)

- **FR-001**: Hệ thống PHẢI cung cấp API cho phép người dùng đang đăng nhập lấy thông tin hồ sơ của chính mình theo vai trò (`ROLE_LEARNER` hoặc `ROLE_TUTOR`).
- **FR-002**: Hệ thống PHẢI cho phép học viên cập nhật các trường: Họ và tên, Số điện thoại, Ảnh đại diện, Trình độ hiện tại, Trình độ mục tiêu, và Mục tiêu học tập.
- **FR-003**: Hệ thống PHẢI cho phép gia sư cập nhật các trường: Họ và tên, Tiêu đề giảng dạy (Headline), Tiểu sử (Bio), Số năm kinh nghiệm, Học phí đề xuất mỗi giờ, Trình độ JLPT cao nhất, Số điện thoại và Địa chỉ/Khu vực giảng dạy (gồm chọn Tỉnh/Thành phố từ danh mục chuẩn và nhập chi tiết Quận/Huyện).
- **FR-004**: Hệ thống PHẢI kiểm tra tính hợp lệ của định dạng số điện thoại Việt Nam (10 chữ số bắt đầu bằng đầu số hợp lệ) và các cấp độ tiếng Nhật hợp lệ của học viên (gồm tùy chọn "Mới bắt đầu" - `BEGINNER` và các cấp độ JLPT từ `N5` đến `N1`).
- **FR-005**: Hệ thống PHẢI cung cấp API phân trang danh sách người dùng cho Quản lý vận hành (`ROLE_MANAGER`, `ROLE_ADMIN`), hỗ trợ lọc theo vai trò (`ALL`, `ROLE_TUTOR`, `ROLE_LEARNER`), theo trạng thái tài khoản (`ACTIVE`, `SUSPENDED`, `BANNED`), và tìm kiếm theo tên hoặc email.
- **FR-006**: Hệ thống PHẢI cung cấp API lấy thông tin chi tiết một người dùng cụ thể dành cho Quản lý vận hành.
- **FR-007**: Hệ thống PHẢI cung cấp API cập nhật trạng thái tài khoản (`account_status`) của người dùng dành cho Quản lý vận hành, bắt buộc nhập lý do thay đổi.
- **FR-008**: Hệ thống PHẢI ghi nhận nhật ký hoạt động (Audit log) mỗi khi Quản lý thay đổi trạng thái tài khoản người dùng.
- **FR-009**: Giao diện người dùng PHẢI có màn hình xem và cập nhật hồ sơ cá nhân cho Học viên (`frontend/pages/learner-profile/`).
- **FR-010**: Giao diện người dùng PHẢI có cơ chế chuyển đổi giữa chế độ xem công khai và chế độ chỉnh sửa thông tin trên trang hồ sơ của Gia sư.
- **FR-011**: Giao diện người dùng PHẢI có màn hình Quản lý người dùng dành cho Quản lý vận hành (`frontend/pages/manager-users/`).
- **FR-012**: Toàn bộ nhãn, nút bấm, thông báo trạng thái rỗng và thông báo lỗi trên giao diện PHẢI hiển thị bằng tiếng Việt chuẩn mực, tuân thủ nguyên tắc không viết hoa toàn bộ (No ALL-CAPS).
- **FR-013**: Tuyệt đối không sử dụng dữ liệu tĩnh/giả trong luồng chạy thật; dữ liệu người dùng phải được nạp từ REST API thật và hiển thị Skeleton Loading trong lúc tải.
- **FR-014**: Hệ thống PHẢI cung cấp API tải tệp ảnh đại diện (`POST /api/profile/avatar`, `multipart/form-data`), kiểm tra định dạng ảnh (`jpg`, `jpeg`, `png`, `webp`) và giới hạn dung lượng tối đa 5MB, lưu trữ an toàn và trả về đường dẫn URL ảnh, đồng thời vẫn cho phép cập nhật trực tiếp qua chuỗi link ảnh hợp lệ.
- **FR-015**: Hệ thống PHẢI cố định địa chỉ Email ở chế độ chỉ đọc (Read-only) trong phân hệ Hồ sơ cá nhân; API cập nhật hồ sơ không cho phép thay đổi địa chỉ Email để bảo vệ tính toàn vẹn xác thực tài khoản.

---

## 5. Thực thể dữ liệu chính (Key Entities)

- **User**: Thực thể người dùng gốc lưu trữ tài khoản, email, mật khẩu mã hóa, vai trò (`Role`), họ tên, số điện thoại, ảnh đại diện, trạng thái tài khoản (`account_status`: `ACTIVE`, `SUSPENDED`, `BANNED`).
- **LearnerProfile (Learner)**: Lưu trữ thông tin chuyên biệt của học viên, liên kết 1-1 với `User`, bao gồm: trình độ hiện tại (`current_level`), trình độ mục tiêu (`target_level`), mục tiêu học tập (`learning_goals`).
- **TutorProfile (Tutor)**: Lưu trữ thông tin chuyên biệt của gia sư, liên kết 1-1 với `User`, bao gồm: tiêu đề giới thiệu (`headline`), tiểu sử chi tiết (`bio`), kinh nghiệm giảng dạy (`experience_years`), học phí đề xuất (`hourly_rate`), trình độ JLPT cao nhất (`highest_jlpt_level`), đánh giá trung bình (`avg_rating`), tổng số lượt đánh giá (`total_reviews`), trạng thái xác minh và địa chỉ khu vực (`address`).

---

## 6. Tiêu chí thành công (Success Criteria)

- **SC-001**: 100% học viên và gia sư có thể truy cập và cập nhật thông tin cá nhân của mình thành công trong thời gian dưới 1 phút.
- **SC-002**: Mọi thay đổi thông tin trên hồ sơ có hiệu lực ngay lập tức và được phản ánh trên toàn bộ các trang liên quan (trang tìm kiếm, trang chi tiết, thanh điều hướng) mà không có độ trễ bất thường.
- **SC-003**: 100% các thao tác thay đổi trạng thái tài khoản của Quản lý được ghi log đầy đủ với danh tính người thực hiện, thời gian và lý do.
- **SC-004**: Không có bất kỳ rò rỉ mã lỗi kỹ thuật, tên biến cơ sở dữ liệu hay văn bản viết hoa toàn bộ nào trên giao diện của cả 3 nhóm đối tượng sử dụng.
- **SC-005**: Tốc độ phản hồi của API tra cứu và cập nhật hồ sơ đạt dưới 300ms trong điều kiện vận hành thông thường.

---

## 7. Giả định & Ràng buộc (Assumptions)

- Người dùng đã đăng ký và có tài khoản kích hoạt trên hệ thống (đã hoàn thành bước xác thực email OTP ở phân hệ Auth).
- Cơ sở hạ tầng Supabase PostgreSQL và các bảng `users`, `learners`, `tutors` đã sẵn sàng và liên kết thông qua khóa ngoại chuẩn xác.
- Tính năng xác minh bằng cấp/CCCD của gia sư thuộc về phân hệ Tutor Verification (FE-03) và đã có API riêng (`/api/tutor/verification`).
- Tính năng đánh giá sao của học viên dành cho gia sư thuộc về phân hệ Tìm kiếm & Lớp học sau khi hoàn thành khóa học; hồ sơ gia sư chỉ hiển thị số liệu tổng hợp (`avg_rating`, `total_reviews`).

