# Implementation Plan: FE-05 — Trial Lesson Booking & Connection (Đăng Ký Học Thử & Kết Nối Gia Sư)

**Branch**: `haipt` | **Date**: 2026-11-04 | **Spec**: [specs/005-trial-booking-connection/spec.md](spec.md)

**Input**: Feature specification từ `/specs/005-trial-booking-connection/spec.md` kết hợp tài liệu Capstone Project Report 3 (UC-5.1 đến UC-5.11) và CSDL hiện có.

---

## Summary

Xây dựng luồng kết nối và đăng ký học thử 1-1 trọn vẹn giữa Học viên và Gia sư:
1. **Frontend**:
   - Tích hợp Modal đặt lịch học thử chuẩn trên hồ sơ gia sư (`/pages/tutor-profile/`), cho phép chọn thời lượng (25m/50m), hình thức học (Online/Offline), nhập mục tiêu và gửi yêu cầu.
   - Xây dựng giao diện quản lý danh sách yêu cầu kết nối (`/pages/connection-requests/`) cho cả Học viên và Gia sư, phân chia tab rõ ràng (Chờ phản hồi, Đã xác nhận, Lịch sử), hỗ trợ gia sư Duyệt/Từ chối và cấp link phòng học trực tuyến.
2. **Backend**:
   - Xây dựng phân hệ `com.kizuna.features.booking` với đầy đủ Entity JPA (`ConnectionRequest`, `TrialBooking`, `TrialBookingHistory`), Repository, Service và Controller RESTful `/api/v1/trial-bookings`.
   - Áp dụng kiểm tra chống đặt trùng giờ thời gian thực (Time-range overlap check) ngăn chặn 100% race conditions.
   - Tuân thủ nghiêm ngặt Hiến pháp KizunaTutor: Buổi học thử là 0đ (không thu tiền đặt cọc), giao diện 100% tiếng Việt tự nhiên, không rò rỉ mã lỗi kỹ thuật.

---

## Technical Context

**Language/Version**: Java 17 (Backend), JavaScript ES6+ Vanilla Module & HTML5/CSS3 (Frontend).  
**Primary Dependencies**: 
- Backend: Spring Boot 3.3.x, Spring Data JPA, Hibernate, Spring Security (JWT), Lombok, PostgreSQL Driver.
- Frontend: Tailwind CSS CDN, Google Material Symbols Outlined, Google Fonts (Plus Jakarta Sans).  
**Storage**: PostgreSQL 16 (Supabase Mumbai instance), kết nối qua Spring Data JPA. Các bảng chính: `connection_requests`, `trial_bookings`, `trial_booking_histories`.  
**Testing**: JUnit 5, Mockito, Spring Boot Test (`mvn test`), Node test runner (`node --test`).  
**Target Platform**: Web Responsive (Hỗ trợ Desktop màn hình chuẩn 1240px và Mobile viewport).  
**Project Type**: Web Application (Monorepo gồm `backend/` và `frontend/`).  
**Performance Goals**: Thời gian phản hồi API tạo/duyệt lịch học thử < 500ms; hiển thị danh sách yêu cầu < 300ms; chống trùng lịch tuyệt đối.  
**Constraints**: 
- Không thu phí học thử qua VNPay (Học thử 0đ).
- Không dùng framework SPA nặng (React/Vue).
- Không để lộ Exception, mã HTTP raw hay enum kỹ thuật lên UI.
- Tuân thủ quy tắc Component dùng chung (`#header-placeholder`, `#footer-placeholder`).  
**Scale/Scope**: Áp dụng cho 2 Actor chính (Học viên & Gia sư); quản lý hàng nghìn phiên học thử và yêu cầu kết nối đồng thời.

---

## Constitution Check

*GATE: Đã đối chiếu và kiểm tra tuân thủ 5 nguyên tắc Hiến pháp KizunaTutor (`constitution.md` v1.1.0):*

| Nguyên tắc | Đánh giá tuân thủ | Giải trình chi tiết |
| :--- | :---: | :--- |
| **I. Giao diện tiếng Việt tự nhiên, không lộ mã kỹ thuật & không viết hoa toàn bộ** | **ĐẠT (PASS)** | Toàn bộ DTO, nhãn trạng thái (Chờ xác nhận, Đã xác nhận, Đã từ chối, Đã hủy) và thông báo lỗi hiển thị bằng tiếng Việt thân thiện. Cấm tuyệt đối ALL-CAPS và tên enum thô. |
| **II. Dữ liệu thật, cấm dữ liệu ảo và dùng trạng thái rỗng trung thực** | **ĐẠT (PASS)** | Kết nối trực tiếp vào bảng thật `connection_requests` và `trial_bookings`. Dùng Skeleton Loading khi chờ API và render Empty State chuẩn khi danh sách trống. |
| **III. Kiến trúc phân hệ độc lập & giao tiếp công khai qua Service** | **ĐẠT (PASS)** | Gói gọn toàn bộ trong package `com.kizuna.features.booking`. Các feature khác (như `tutor_search`, `classroom`) chỉ được tương tác qua `TrialBookingService` công khai, không gọi trực tiếp Repository. |
| **IV. Giao diện chuẩn hóa theo Trang Chủ & Component dùng chung** | **ĐẠT (PASS)** | Áp dụng đúng theme Wabi-Sabi x EdTech từ `design.md`. Sử dụng duy nhất một nguồn Header/Footer nạp qua placeholder. |
| **V. Ranh giới nghiệp vụ & Kỷ luật Git tuyệt đối** | **ĐẠT (PASS)** | Học thử là 0đ (không thu học phí hay tiền cọc qua VNPay). AI không tự ý commit hay git push khi chưa được người dùng cho phép. |

---

## Project Structure

### Documentation (this feature)

```text
specs/005-trial-booking-connection/
├── plan.md              # Kế hoạch triển khai tổng thể
├── spec.md              # Đặc tả yêu cầu chi tiết (FE-05)
├── research.md          # Quyết định kỹ thuật và giải pháp chống trùng lịch
├── data-model.md        # Mô hình ERD, định nghĩa thực thể và máy trạng thái
├── quickstart.md        # Kịch bản kiểm chứng End-to-End
├── checklists/
│   └── requirements.md  # Checklist chất lượng đặc tả
└── contracts/
    └── trial-booking-api.md # Hợp đồng API RESTful chi tiết
```

### Source Code (repository root)

```text
backend/src/main/java/com/kizuna/features/booking/
├── controller/
│   └── TrialBookingController.java         # REST Controller /api/v1/trial-bookings
├── service/
│   ├── TrialBookingService.java            # Public Interface
│   └── impl/
│       └── TrialBookingServiceImpl.java    # Xử lý nghiệp vụ đặt lịch, chống trùng, duyệt/hủy
├── repository/
│   ├── ConnectionRequestRepository.java    # Thao tác CSDL connection_requests
│   ├── TrialBookingRepository.java         # Thao tác CSDL trial_bookings & kiểm tra overlap
│   └── TrialBookingHistoryRepository.java  # Ghi vết lịch sử trạng thái
├── entity/
│   ├── ConnectionRequest.java              # JPA Entity connection_requests
│   ├── TrialBooking.java                   # JPA Entity trial_bookings
│   ├── TrialBookingHistory.java            # JPA Entity trial_booking_histories
│   ├── ConnectionRequestStatus.java        # Enum: PENDING, ACCEPTED, DECLINED, CANCELLED, EXPIRED
│   └── TrialBookingStatus.java             # Enum: PENDING, CONFIRMED, COMPLETED, CANCELLED, RESCHEDULED
└── dto/
    ├── request/
    │   ├── CreateTrialBookingRequestDTO.java
    │   ├── RespondTrialBookingRequestDTO.java
    │   └── CancelTrialBookingRequestDTO.java
    └── response/
        ├── TrialBookingSummaryResponseDTO.java
        └── TrialBookingDetailResponseDTO.java

frontend/pages/
├── tutor-profile/
│   ├── index.html                          # Modal đặt lịch 1-1 (Preply-style date/slot picker)
│   └── tutor-profile.js                    # Gọi POST /api/v1/trial-bookings
├── connection-requests/
│   ├── index.html                          # Danh sách quản lý yêu cầu kết nối & lịch học thử
│   └── connection-requests.js              # Gọi GET/PUT /api/v1/trial-bookings (tabs & duyệt lịch)
└── tutor-dashboard/
    ├── index.html                          # Bảng điều khiển gia sư tích hợp số liệu yêu cầu kết nối
    └── tutor-dashboard.js                  # Gọi GET /api/v1/trial-bookings/my-requests?status=PENDING
```

---

## Implementation Workflows

### Phase 0: Research & Technical Decisions (Hoàn thành)
- Đã giải quyết toàn bộ các điểm cần làm rõ trong `research.md`.
- Chốt cơ chế chống đặt trùng giờ bằng time-range overlap check.

### Phase 1: Design & Contracts (Hoàn thành)
- Hoàn thành ERD và Entity schema trong `data-model.md`.
- Hoàn thành Hợp đồng REST API trong `contracts/trial-booking-api.md`.
- Hoàn thành Kịch bản kiểm thử End-to-End trong `quickstart.md`.

### Phase 2: Implementation Planning (Hoàn thành)
- Phân rã thành danh sách các công việc thực thi độc lập trong `tasks.md`.

### Phase 3: Tutor Dashboard Integration (Mới bổ sung theo yêu cầu)
- Kích hoạt liên kết "Yêu cầu kết nối" trong `tutor-layout.js` dẫn tới `/connection-requests`.
- Nâng cấp khối "Yêu cầu kết nối" trên Bảng điều khiển gia sư (`tutor-dashboard`) từ trạng thái tĩnh/sắp ra mắt thành widget hoạt động thực tế với số lượng yêu cầu chờ duyệt thời gian thực.
- Đảm bảo luồng khép kín: Học viên đặt lịch -> Gia sư nhận và duyệt trên Dashboard -> Học viên vào học thành công.
