# Data Model: FE-05 — Trial Lesson Booking & Connection

**Feature**: FE-05 Trial Lesson Booking & Connection (Đăng ký học thử & Kết nối gia sư)  
**Spec Reference**: `specs/005-trial-booking-connection/spec.md`  
**Date**: 2026-11-04  

---

## 1. Sơ đồ thực thể quan hệ (ERD - Mermaid)

```mermaid
erDiagram
    LEARNERS ||--o{ CONNECTION_REQUESTS : "gửi"
    TUTORS ||--o{ CONNECTION_REQUESTS : "nhận"
    CONNECTION_REQUESTS ||--o| TRIAL_BOOKINGS : "chứa chi tiết lịch"
    LEARNERS ||--o{ TRIAL_BOOKINGS : "tham gia"
    TUTORS ||--o{ TRIAL_BOOKINGS : "giảng dạy"
    TRIAL_BOOKINGS ||--o{ TRIAL_BOOKING_HISTORIES : "ghi vết trạng thái"
    USERS ||--o{ TRIAL_BOOKING_HISTORIES : "thực hiện đổi trạng thái"

    CONNECTION_REQUESTS {
        UUID request_id PK
        UUID learner_id FK
        UUID tutor_id FK
        TEXT initial_message
        VARCHAR status "PENDING, ACCEPTED, DECLINED, CANCELLED, EXPIRED"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ responded_at
    }

    TRIAL_BOOKINGS {
        UUID booking_id PK
        UUID request_id FK
        UUID learner_id FK
        UUID tutor_id FK
        TIMESTAMPTZ scheduled_start_time
        TIMESTAMPTZ scheduled_end_time
        VARCHAR meeting_link
        VARCHAR booking_status "PENDING, CONFIRMED, COMPLETED, CANCELLED, RESCHEDULED"
        BOOLEAN learner_confirmed
        BOOLEAN tutor_confirmed
        BOOLEAN official_arrangement_agreed
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    TRIAL_BOOKING_HISTORIES {
        UUID history_id PK
        UUID booking_id FK
        UUID changed_by_user_id FK
        VARCHAR previous_status
        VARCHAR new_status
        VARCHAR change_reason
        TIMESTAMPTZ created_at
    }
```

---

## 2. Chi tiết các thực thể (Entity Specifications)

### 2.1. Thực thể `ConnectionRequest` (`connection_requests`)

- **Mục đích**: Đại diện cho yêu cầu kết nối và lời nhắn làm quen ban đầu giữa Học viên và Gia sư.
- **Bảng CSDL**: `connection_requests`
- **Các trường**:
  - `request_id` (UUID, PK): Mã định danh duy nhất của yêu cầu kết nối.
  - `learner_id` (UUID, FK -> `learners.learner_id`, NOT NULL): Học viên gửi yêu cầu.
  - `tutor_id` (UUID, FK -> `tutors.tutor_id`, NOT NULL): Gia sư nhận yêu cầu.
  - `initial_message` (TEXT, NULLABLE): Lời nhắn ban đầu (trình độ hiện tại, mục tiêu học tập, yêu cầu hỗ trợ).
  - `status` (`connection_request_status_enum`, NOT NULL, default: `'PENDING'`):
    - `PENDING`: Đang chờ gia sư phản hồi.
    - `ACCEPTED`: Gia sư đã đồng ý kết nối.
    - `DECLINED`: Gia sư từ chối kết nối.
    - `CANCELLED`: Học viên đã hủy yêu cầu.
    - `EXPIRED`: Yêu cầu bị hết hạn do quá thời gian SLA không được xử lý.
  - `created_at` (TIMESTAMPTZ, NOT NULL): Thời điểm gửi yêu cầu.
  - `responded_at` (TIMESTAMPTZ, NULLABLE): Thời điểm gia sư phản hồi.

---

### 2.2. Thực thể `TrialBooking` (`trial_bookings`)

- **Mục đích**: Đại diện cho phiên học thử cụ thể với thời gian chốt lịch, hình thức học và liên kết phòng học.
- **Bảng CSDL**: `trial_bookings`
- **Các trường**:
  - `booking_id` (UUID, PK): Mã định danh buổi học thử.
  - `request_id` (UUID, FK -> `connection_requests.request_id`, NULLABLE): Tham chiếu đến yêu cầu kết nối gốc.
  - `learner_id` (UUID, FK -> `learners.learner_id`, NOT NULL): Học viên tham gia buổi học.
  - `tutor_id` (UUID, FK -> `tutors.tutor_id`, NOT NULL): Gia sư giảng dạy buổi học.
  - `scheduled_start_time` (TIMESTAMPTZ, NOT NULL): Thời gian bắt đầu buổi học.
  - `scheduled_end_time` (TIMESTAMPTZ, NOT NULL): Thời gian kết thúc buổi học (bắt buộc > `scheduled_start_time`).
  - `meeting_link` (VARCHAR(500), NULLABLE): Đường link phòng học online (Google Meet, Zoom) hoặc địa chỉ học trực tiếp (Offline).
  - `booking_status` (`trial_booking_status_enum`, NOT NULL, default: `'PENDING'`):
    - `PENDING`: Chờ gia sư xác nhận lịch.
    - `CONFIRMED`: Gia sư đã chấp nhận lịch (khung giờ được chốt cố định).
    - `COMPLETED`: Buổi học thử đã diễn ra thành công.
    - `CANCELLED`: Buổi học đã bị hủy bởi một trong hai bên.
    - `RESCHEDULED`: Buổi học đã được dời sang khung giờ khác.
  - `learner_confirmed` (BOOLEAN, default: FALSE): Học viên đã xác nhận tham dự.
  - `tutor_confirmed` (BOOLEAN, default: FALSE): Gia sư đã xác nhận hoàn thành/tham dự.
  - `official_arrangement_agreed` (BOOLEAN, default: FALSE): Hai bên đã thống nhất chuyển sang học dài hạn sau buổi học thử.
  - `created_at` (TIMESTAMPTZ, NOT NULL): Thời gian tạo lịch.
  - `updated_at` (TIMESTAMPTZ, NOT NULL): Thời gian cập nhật lần cuối.

---

### 2.3. Thực thể `TrialBookingHistory` (`trial_booking_histories`)

- **Mục đích**: Lưu vết lịch sử kiểm toán toàn bộ vòng đời thay đổi trạng thái của buổi học thử.
- **Bảng CSDL**: `trial_booking_histories`
- **Các trường**:
  - `history_id` (UUID, PK): Mã định danh bản ghi lịch sử.
  - `booking_id` (UUID, FK -> `trial_bookings.booking_id`, NOT NULL): Buổi học thử được ghi vết.
  - `changed_by_user_id` (UUID, FK -> `users.user_id`, NULLABLE): Người thực hiện hành động đổi trạng thái.
  - `previous_status` (`trial_booking_status_enum`, NULLABLE): Trạng thái trước khi đổi.
  - `new_status` (`trial_booking_status_enum`, NOT NULL): Trạng thái mới sau khi đổi.
  - `change_reason` (VARCHAR(255), NULLABLE): Lý do đổi trạng thái (ví dụ: lý do từ chối, lý do hủy).
  - `created_at` (TIMESTAMPTZ, NOT NULL): Thời điểm ghi nhận.

---

## 3. Máy trạng thái vòng đời buổi học thử (State Machine)

```mermaid
stateDiagram-v2
    [*] --> PENDING: Học viên đặt lịch học thử (0đ)
    
    PENDING --> CONFIRMED: Gia sư bấm "Chấp nhận" (cung cấp meeting link)
    PENDING --> DECLINED: Gia sư bấm "Từ chối" (kèm lý do)
    PENDING --> CANCELLED: Học viên hủy yêu cầu trước khi duyệt
    PENDING --> EXPIRED: Quá hạn SLA 48h không phản hồi
    
    CONFIRMED --> COMPLETED: Đến giờ học & buổi học hoàn tất
    CONFIRMED --> CANCELLED: Học viên hoặc Gia sư hủy trước giờ học (>24h)
    CONFIRMED --> RESCHEDULED: Hai bên thống nhất đổi khung giờ khác
    
    DECLINED --> [*]
    CANCELLED --> [*]
    EXPIRED --> [*]
    COMPLETED --> [*]
```

---

## 4. Quy tắc nghiệp vụ & Ràng buộc toàn vẹn (Validation Rules)

1. **Ràng buộc thời gian (Time Constraint)**:
   - `scheduled_start_time` phải nằm trong tương lai (tối thiểu cách thời điểm đặt lịch 2 giờ).
   - Khoảng cách giữa `scheduled_start_time` và `scheduled_end_time` phải khớp với thời lượng đã chọn (chính xác 25 phút hoặc 50 phút).
2. **Ràng buộc chồng lấn lịch (Non-overlapping constraint)**:
   - Không được phép có hai bản ghi `trial_bookings` của cùng 1 `tutor_id` ở trạng thái `PENDING` hoặc `CONFIRMED` có khoảng thời gian `[start_time, end_time]` giao nhau.
3. **Ràng buộc quyền hạn (Authorization)**:
   - Chỉ Học viên sở hữu yêu cầu mới có quyền bấm Hủy trước khi duyệt.
   - Chỉ Gia sư nhận yêu cầu mới có quyền Chấp nhận hoặc Từ chối.

