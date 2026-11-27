# Phase 0 Research & Technical Decisions: FE-05 — Trial Lesson Booking & Connection

**Feature**: FE-05 Trial Lesson Booking & Connection (Đăng ký học thử & Kết nối gia sư)  
**Spec Reference**: `specs/005-trial-booking-connection/spec.md`  
**Date**: 2026-11-04  

---

## 1. Nghiên cứu & Quyết định kỹ thuật

### Quyết định 1: Quan hệ và vòng đời giữa `connection_requests` và `trial_bookings`

- **Vấn đề**: Report 3 mô tả cả Yêu cầu kết nối (Connection Request) và Đặt lịch học thử (Trial Lesson Booking). Trong cơ sở dữ liệu `DatabasePgAdmin4.txt`, hai bảng này được liên kết thông qua khóa ngoại `trial_bookings.request_id -> connection_requests.request_id`.
- **Quyết định chọn**: 
  - Khi học viên bấm xác nhận trên Modal đặt lịch học thử từ trang hồ sơ gia sư, hệ thống sẽ thực hiện một transaction nguyên tử (Atomic Transaction):
    1. Tạo 1 bản ghi `connection_requests` với `status = 'PENDING'`, lưu `initial_message` (mục tiêu học tập + lời nhắn).
    2. Tạo 1 bản ghi `trial_bookings` với `request_id` vừa tạo, `booking_status = 'PENDING'`, lưu thời gian bắt đầu, thời gian kết thúc, hình thức học (`ONLINE` hoặc `OFFLINE`), và địa chỉ (nếu học trực tiếp).
    3. Ghi vết vào `trial_booking_histories` với `new_status = 'PENDING'`, `change_reason = 'Học viên khởi tạo yêu cầu học thử'`.
- **Lý do**: Đảm bảo toàn vẹn dữ liệu, tận dụng tối đa schema đã thiết kế trong database mà không cần sửa đổi schema; cho phép học viên và gia sư theo dõi vừa theo góc độ "kết nối trao đổi" vừa theo góc độ "lịch học cụ thể".
- **Giải pháp thay thế đã loại bỏ**: Chỉ dùng 1 bảng `trial_bookings` bỏ qua `connection_requests` -> Loại bỏ vì phá vỡ cấu trúc cơ sở dữ liệu và workflow 2.3 của Report 3.

---

### Quyết định 2: Chống xung đột đặt trùng lịch (Race Condition & Double-Booking)

- **Vấn đề**: Trường hợp hai học viên cùng mở modal và bấm đặt cùng một khung giờ của một gia sư gần như đồng thời.
- **Quyết định chọn**:
  - Tại tầng Repository của Backend, áp dụng kiểm tra chồng lấn thời gian (Time-range overlap check):
    ```sql
    WHERE tutor_id = :tutorId
      AND booking_status IN ('PENDING', 'CONFIRMED')
      AND scheduled_start_time < :newEndTime
      AND scheduled_end_time > :newStartTime
    ```
  - Nếu tồn tại bản ghi thỏa mãn điều kiện trên, hệ thống ném ngoại lệ `ConflictException("Khung giờ này vừa có người đăng ký. Vui lòng chọn khung giờ khác.")`, trả về mã HTTP `409 Conflict`.
  - Kết hợp transaction `@Transactional(isolation = Isolation.READ_COMMITTED)` để đảm bảo tính nhất quán.
- **Lý do**: Ngăn chặn 100% tình trạng gia sư bị đặt trùng giờ mà không làm chậm hệ thống hoặc gây deadlock.

---

### Quyết định 3: Định dạng API RESTful và Phân quyền (RBAC)

- **Vấn đề**: Cần xác thực và phân quyền chính xác giữa Học viên (`ROLE_LEARNER`) và Gia sư (`ROLE_TUTOR`).
- **Quyết định chọn**:
  - `POST /api/v1/trial-bookings`: Chỉ dành cho `ROLE_LEARNER` (lấy `learner_id` từ token JWT đã đăng nhập).
  - `GET /api/v1/trial-bookings/my-requests`: Tự động nhận diện vai trò: nếu là Learner thì lấy các buổi học do mình đặt; nếu là Tutor thì lấy các yêu cầu học viên gửi cho mình. Hỗ trợ lọc theo `status`.
  - `GET /api/v1/trial-bookings/{id}`: Kiểm tra quyền sở hữu (chỉ Learner hoặc Tutor của buổi học đó mới được xem chi tiết).
  - `PUT /api/v1/trial-bookings/{id}/respond`: Chỉ dành cho `ROLE_TUTOR` sở hữu buổi học để Chấp nhận (`CONFIRMED`, gửi kèm `meetingLink` nếu Online) hoặc Từ chối (`DECLINED`, gửi kèm `declineReason`).
  - `PUT /api/v1/trial-bookings/{id}/cancel`: Học viên hoặc Gia sư có thể hủy trước giờ học kèm lý do hủy.
- **Lý do**: Tuân thủ chuẩn RESTful, an toàn bảo mật IDOR (Insecure Direct Object References), gọn nhẹ và tái sử dụng code tối đa.

---

### Quyết định 4: Hỗ trợ linh hoạt hình thức Online và Offline

- **Vấn đề**: Người dùng xác nhận cần hỗ trợ cả học Online qua Google Meet và học Offline tại nhà/địa điểm hẹn.
- **Quyết định chọn**:
  - Lưu trường `meeting_link` trong `trial_bookings` cho Online (link Google Meet do gia sư cung cấp hoặc hệ thống sinh tự động).
  - Đối với Offline, lưu thông tin địa điểm vào cột `meeting_link` dưới dạng định dạng text tiền tố (hoặc lưu vào trường ghi chú / địa chỉ trong request).
  - Trên giao diện `connection-requests.html`, nếu là Online thì hiển thị nút màu xanh *"Vào phòng học"* mở tab mới; nếu là Offline thì hiển thị icon ghim vị trí `location_on` kèm địa chỉ rõ ràng.

---

## 2. Best Practices & Ràng buộc tuân thủ

1. **Hiến pháp KizunaTutor**:
   - Tuyệt đối không có luồng trừ tiền, đặt cọc hay thu phí học thử. Học thử là 0đ.
   - Toàn bộ thông báo lỗi và phản hồi trả về qua DTO bằng tiếng Việt tự nhiên.
2. **Components & Design Rule**:
   - Trang `/pages/connection-requests/index.html` và `/pages/tutor-profile/index.html` tuân thủ nạp `#header-placeholder` và `#footer-placeholder`.
   - Màu sắc và phong cách giao diện tuân theo `design.md` (Wabi-Sabi, Lam chàm `#006194`, nền `#FCFBF7`).

