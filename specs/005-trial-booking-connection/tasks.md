---
description: "Task list for FE-05 Trial Lesson Booking & Connection implementation"
---

# Tasks: FE-05 — Trial Lesson Booking & Connection (Đăng Ký Học Thử & Kết Nối Gia Sư)

**Input**: Design documents from `/specs/005-trial-booking-connection/` (`spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/trial-booking-api.md`, `quickstart.md`).  
**Prerequisites**: `spec.md`, `plan.md`, `data-model.md`, `contracts/trial-booking-api.md`.  
**Organization**: Tasks are grouped by execution order (việc làm trước xếp lên trước) and prioritized user stories to enable incremental, independently testable delivery.

## Format: `- [ ] [TaskID] [P?] [Story?] Description with file path`
- **[P]**: Parallelizable (different files, no blocking dependencies).
- **[Story]**: User story mapping (`[US1]`, `[US2]`, `[US3]`, `[US4]`).

---

## Phase 1: Setup & JPA Entities (Làm trước tiên)

**Purpose**: Thiết lập nền tảng cấu trúc gói phân hệ `com.kizuna.features.booking` và các thực thể JPA khớp 100% CSDL PostgreSQL (`DatabasePgAdmin4.txt`).

- [x] T001 [P] Tạo và hoàn thiện enum trạng thái `ConnectionRequestStatus.java` (`PENDING`, `ACCEPTED`, `DECLINED`, `CANCELLED`, `EXPIRED`) tại `backend/src/main/java/com/kizuna/features/booking/entity/ConnectionRequestStatus.java`
- [x] T002 [P] Tạo và hoàn thiện enum trạng thái `TrialBookingStatus.java` (`PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`, `RESCHEDULED`) tại `backend/src/main/java/com/kizuna/features/booking/entity/TrialBookingStatus.java`
- [x] T003 [P] Tạo thực thể JPA `ConnectionRequest.java` ánh xạ bảng `connection_requests` (các trường `request_id`, `learner_id`, `tutor_id`, `initial_message`, `status`, `created_at`, `responded_at`) tại `backend/src/main/java/com/kizuna/features/booking/entity/ConnectionRequest.java`
- [x] T004 [P] Cập nhật đầy đủ các trường của thực thể JPA `TrialBooking.java` ánh xạ bảng `trial_bookings` (các trường `booking_id`, `request_id`, `learner_id`, `tutor_id`, `scheduled_start_time`, `scheduled_end_time`, `meeting_link`, `booking_status`, `learner_confirmed`, `tutor_confirmed`, `official_arrangement_agreed`) tại `backend/src/main/java/com/kizuna/features/booking/entity/TrialBooking.java`
- [x] T005 [P] Tạo thực thể JPA `TrialBookingHistory.java` ánh xạ bảng `trial_booking_histories` (các trường `history_id`, `booking_id`, `changed_by_user_id`, `previous_status`, `new_status`, `change_reason`, `created_at`) tại `backend/src/main/java/com/kizuna/features/booking/entity/TrialBookingHistory.java`

---

## Phase 2: Foundational (Repositories & DTOs)

**Purpose**: Xây dựng tầng truy xuất dữ liệu Spring Data JPA và các đối tượng DTO truyền nhận dữ liệu API.

- [x] T006 [P] Tạo `ConnectionRequestRepository.java` hỗ trợ tìm kiếm theo `learner_id`, `tutor_id` và `status` tại `backend/src/main/java/com/kizuna/features/booking/repository/ConnectionRequestRepository.java`
- [x] T007 [P] Tạo `TrialBookingRepository.java` với query kiểm tra chống trùng lịch `existsOverlappingBooking` (kiểm tra khoảng thời gian `start < newEnd AND end > newStart` cho `booking_status IN ('PENDING', 'CONFIRMED')`) tại `backend/src/main/java/com/kizuna/features/booking/repository/TrialBookingRepository.java`
- [x] T008 [P] Tạo `TrialBookingHistoryRepository.java` để lưu vết lịch sử trạng thái tại `backend/src/main/java/com/kizuna/features/booking/repository/TrialBookingHistoryRepository.java`
- [x] T009 [P] Tạo DTO yêu cầu đặt lịch `CreateTrialBookingRequestDTO.java` (`tutorId`, `scheduledStartTime`, `durationMinutes`, `learningMode`, `locationDetails`, `currentLevel`, `learningGoals`, `initialMessage`) tại `backend/src/main/java/com/kizuna/features/booking/dto/request/CreateTrialBookingRequestDTO.java`
- [x] T010 [P] Tạo DTO phản hồi duyệt `RespondTrialBookingRequestDTO.java` (`action`, `meetingLink`, `declineReason`, `note`) tại `backend/src/main/java/com/kizuna/features/booking/dto/request/RespondTrialBookingRequestDTO.java`
- [x] T011 [P] Tạo DTO hủy lịch `CancelTrialBookingRequestDTO.java` (`cancelReason`) tại `backend/src/main/java/com/kizuna/features/booking/dto/request/CancelTrialBookingRequestDTO.java`
- [x] T012 [P] Tạo DTO phản hồi tóm tắt và chi tiết `TrialBookingResponseDTO.java` định dạng tiếng Việt tự nhiên tại `backend/src/main/java/com/kizuna/features/booking/dto/response/TrialBookingResponseDTO.java`

**Checkpoint**: Nền tảng thực thể, repository và DTO đã sẵn sàng để triển khai các User Story độc lập.

---

## Phase 3: User Story 1 - Học viên đăng ký học thử 1-1 (Priority: P1) 🎯 MVP

**Goal**: Cho phép Học viên chọn khung giờ rảnh trên hồ sơ gia sư, chọn thời lượng 25m/50m, hình thức Online/Offline và gửi yêu cầu học thử 0đ thành công.

**Independent Test**: Từ trang `/gs/10001`, học viên chọn khung giờ rảnh, bấm đặt lịch và nhận thông báo thành công. Bản ghi CSDL được tạo với trạng thái `PENDING`.

### Backend Implementation
- [x] T013 [US1] Định nghĩa interface `TrialBookingService.java` với phương thức `createTrialBooking(UUID learnerUserId, CreateTrialBookingRequestDTO request)` tại `backend/src/main/java/com/kizuna/features/booking/service/TrialBookingService.java`
- [x] T014 [US1] Cài đặt `TrialBookingServiceImpl.java` xử lý transaction tạo đồng thời `ConnectionRequest` + `TrialBooking` (trạng thái `PENDING`), ghi vết `TrialBookingHistory` và ném `ConflictException (409)` nếu bị trùng lịch tại `backend/src/main/java/com/kizuna/features/booking/service/impl/TrialBookingServiceImpl.java`
- [x] T015 [US1] Cài đặt endpoint `POST /api/v1/trial-bookings` phân quyền `ROLE_LEARNER` trong `TrialBookingController.java` tại `backend/src/main/java/com/kizuna/features/booking/controller/TrialBookingController.java`
- [x] T016 [US1] Viết unit test cho `createTrialBooking` và kịch bản chống đặt trùng giờ trong `backend/src/test/java/com/kizuna/features/booking/TrialBookingServiceTest.java`

### Frontend Integration
- [x] T017 [US1] Kết nối Modal đặt lịch học thử trên `frontend/pages/tutor-profile/tutor-profile.js` gọi API `POST /api/v1/trial-bookings`, xử lý thông báo thành công hoặc báo lỗi trùng lịch bằng tiếng Việt thân thiện
- [x] T018 [US1] Tích hợp tự động mở Modal đăng nhập nhanh `#authModal` khi khách vãng lai (chưa đăng nhập) bấm nút "Đặt lịch học thử 1-1" trên `frontend/pages/tutor-profile/tutor-profile.js`

**Checkpoint**: Học viên có thể đặt lịch học thử 1-1 thành công, hoàn thành gói tính năng MVP!

---

## Phase 4: User Story 2 - Gia sư tiếp nhận và xử lý yêu cầu học thử (Priority: P1)

**Goal**: Cho phép Gia sư xem yêu cầu học thử đang chờ duyệt và thực hiện hành động Chấp nhận (kèm link Google Meet nếu Online) hoặc Từ chối (kèm lý do).

**Independent Test**: Gia sư đăng nhập, vào chi tiết yêu cầu, bấm "Chấp nhận" (nhập link Meet) -> Trạng thái cập nhật thành `CONFIRMED`.

### Backend Implementation
- [x] T019 [US2] Bổ sung phương thức `respondTrialBooking(UUID tutorUserId, UUID bookingId, RespondTrialBookingRequestDTO request)` vào `TrialBookingService.java` và `TrialBookingServiceImpl.java` tại `backend/src/main/java/com/kizuna/features/booking/service/impl/TrialBookingServiceImpl.java`
- [x] T020 [US2] Cài đặt endpoint `PUT /api/v1/trial-bookings/{bookingId}/respond` phân quyền `ROLE_TUTOR` trong `TrialBookingController.java` tại `backend/src/main/java/com/kizuna/features/booking/controller/TrialBookingController.java`
- [x] T021 [US2] Viết unit test cho luồng Chấp nhận và Từ chối yêu cầu học thử trong `backend/src/test/java/com/kizuna/features/booking/TrialBookingRespondTest.java`

### Frontend Integration
- [x] T022 [US2] Cập nhật giao diện và modal xử lý duyệt yêu cầu học thử trên `frontend/pages/connection-requests/index.html` (modal nhập link Google Meet cho Online, lý do từ chối nếu không nhận)
- [x] T023 [US2] Viết hàm xử lý gọi API `PUT /api/v1/trial-bookings/{bookingId}/respond` trên `frontend/pages/connection-requests/connection-requests.js`

**Checkpoint**: Hai vai trò Học viên và Gia sư đã có thể tương tác hai chiều trọn vẹn (Đặt lịch -> Duyệt lịch).

---

## Phase 5: User Story 3 - Danh sách & Quản lý yêu cầu kết nối tập trung (Priority: P2)

**Goal**: Cung cấp màn hình quản lý danh sách các yêu cầu kết nối và buổi học thử cho cả Học viên và Gia sư, phân chia tab rõ ràng (Chờ phản hồi, Đã xác nhận, Lịch sử).

**Independent Test**: Truy cập `http://localhost:3000/connection-requests`, danh sách hiển thị đúng các thẻ yêu cầu, đúng ảnh, tên, ngày giờ, hình thức và nút "Vào phòng học" khi đã xác nhận.

### Backend Implementation
- [x] T024 [P] [US3] Bổ sung phương thức `getMyTrialBookings(UUID userId, String role, String status, Pageable pageable)` và `getTrialBookingDetail(UUID userId, UUID bookingId)` vào `TrialBookingService.java` và `TrialBookingServiceImpl.java` tại `backend/src/main/java/com/kizuna/features/booking/service/impl/TrialBookingServiceImpl.java`
- [x] T025 [P] [US3] Cài đặt endpoint `GET /api/v1/trial-bookings/my-requests` và `GET /api/v1/trial-bookings/{bookingId}` trong `TrialBookingController.java` tại `backend/src/main/java/com/kizuna/features/booking/controller/TrialBookingController.java`

### Frontend Integration
- [x] T026 [US3] Đồng bộ cấu trúc HTML của trang `frontend/pages/connection-requests/index.html` với quy chuẩn `design.md` (chỉ dùng `#header-placeholder`, `#footer-placeholder`, phong cách card Wabi-Sabi)
- [x] T027 [US3] Cài đặt logic render danh sách thẻ yêu cầu kết nối trên `frontend/pages/connection-requests/connection-requests.js` (gọi `GET /api/v1/trial-bookings/my-requests`, hỗ trợ chuyển tab, render trạng thái rỗng Empty State khi chưa có dữ liệu)

**Checkpoint**: Cả hai bên có giao diện theo dõi toàn bộ lịch trình học thử tập trung và chuyên nghiệp.

---

## Phase 6: User Story 4 - Hủy hoặc Đổi lịch học thử trước giờ học (Priority: P3)

**Goal**: Cho phép Học viên hoặc Gia sư hủy lịch hẹn học thử trước thời hạn quy định (trước 24h) và giải phóng khung giờ rảnh.

**Independent Test**: Bấm "Hủy lịch" trên thẻ buổi học sắp diễn ra, nhập lý do hủy -> Trạng thái cập nhật thành `CANCELLED` và khung giờ được mở lại.

### Backend Implementation
- [x] T028 [US4] Bổ sung phương thức `cancelTrialBooking(UUID userId, UUID bookingId, CancelTrialBookingRequestDTO request)` vào `TrialBookingService.java` và `TrialBookingServiceImpl.java` (kiểm tra quyền sở hữu, cập nhật trạng thái `CANCELLED`, ghi vết `TrialBookingHistory`) tại `backend/src/main/java/com/kizuna/features/booking/service/impl/TrialBookingServiceImpl.java`
- [x] T029 [US4] Cài đặt endpoint `PUT /api/v1/trial-bookings/{bookingId}/cancel` trong `TrialBookingController.java` tại `backend/src/main/java/com/kizuna/features/booking/controller/TrialBookingController.java`

### Frontend Integration
- [x] T030 [US4] Thêm nút "Hủy lịch" và hộp thoại xác nhận hủy kèm ô nhập lý do trên `frontend/pages/connection-requests/index.html` và `connection-requests.js`

**Checkpoint**: Hoàn thiện toàn bộ 4 User Story của phân hệ kết nối học thử!

---

## Phase 7: Polish, Verification & Cross-Cutting Concerns (Hoàn thiện & Kiểm chứng)

**Purpose**: Kiểm thử tích hợp toàn diện, rà soát tuân thủ Hiến pháp và xác minh theo hướng dẫn `quickstart.md`.

- [x] T031 [P] Rà soát 100% văn bản giao diện tiếng Việt, kiểm tra không có chữ IN HOA TOÀN BỘ (No ALL-CAPS) và không để lộ mã lỗi kỹ thuật theo Rule 1 & 2 của `AGENTS.md`
- [x] T032 [P] Chạy toàn bộ test backend với `.\mvnw.cmd test` và kiểm tra biên dịch không lỗi `.\mvnw.cmd test-compile`
- [x] T033 Chạy kiểm chứng toàn bộ 4 kịch bản thực tế theo tài liệu [`quickstart.md`](quickstart.md) trên trình duyệt
- [x] T034 [P] Cập nhật tài liệu và đánh dấu hoàn thành checklist [`checklists/requirements-review.md`](checklists/requirements-review.md)

---

## Dependencies & Execution Order

### Thứ tự thực hiện các Phase (Việc làm trước xếp lên trước):
1. **Phase 1: Setup & JPA Entities** (Bắt buộc làm đầu tiên để có cấu trúc bảng CSDL).
2. **Phase 2: Foundational** (Tạo Repositories, truy vấn chống trùng giờ và DTOs).
3. **Phase 3: User Story 1 (P1 - MVP)** (Học viên gửi yêu cầu học thử 0đ & kết nối modal).
4. **Phase 4: User Story 2 (P1)** (Gia sư duyệt Chấp nhận / Từ chối).
5. **Phase 5: User Story 3 (P2)** (Danh sách quản lý lịch tập trung trên `/connection-requests`).
6. **Phase 6: User Story 4 (P3)** (Hủy lịch hẹn trước giờ học).
7. **Phase 7: Polish & Kiểm chứng** (Chạy test xanh và nghiệm thu theo `quickstart.md`).

---

## Notes
- Toàn bộ công việc tuân thủ nghiêm ngặt Hiến pháp KizunaTutor: Học thử là 0đ, không mock data, tiếng Việt tự nhiên 100%.
- Không tự ý `git push`, commit được thực hiện an toàn trên nhánh `haipt`.

