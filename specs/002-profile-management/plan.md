# Kế hoạch triển khai: FE-02 — Quản lý hồ sơ Học viên & Gia sư (Profile Management)

**Nhánh**: `haipt` | **Ngày**: 2026-11-10 | **Đặc tả**: [spec.md](spec.md)  
**Tài liệu thiết kế**: [research.md](research.md) | [data-model.md](data-model.md) | [contracts/profile-api.md](contracts/profile-api.md) | [quickstart.md](quickstart.md)

---

## 1. Tóm tắt kế hoạch (Summary)

Phân hệ Quản lý hồ sơ FE-02 được xây dựng nhằm phục vụ 3 nhóm đối tượng chính theo chuẩn kiến trúc dọc (Vertical Slice):
1. **Học viên (`ROLE_LEARNER`)**: Trang cá nhân độc lập `frontend/pages/learner-profile/` giúp theo dõi và cập nhật thông tin cá nhân, trình độ tiếng Nhật hiện tại (`BEGINNER`, `N5`..`N1`), trình độ mục tiêu và nguyện vọng học tập.
2. **Gia sư (`ROLE_TUTOR`)**: Trang "Thông tin giảng dạy" trong dashboard gia sư `frontend/pages/tutor-teaching-profile/` (route `/tutor/profile`, mục sidebar của `tutor-layout`) cho phép cập nhật tiêu đề giới thiệu, tiểu sử, số năm kinh nghiệm, học phí theo giờ, cấp độ JLPT cao nhất và địa chỉ khu vực chuẩn hóa.
3. **Quản lý vận hành (`ROLE_MANAGER`, `ROLE_ADMIN`)**: Trang quản trị `frontend/pages/manager-users/` hỗ trợ tra cứu, lọc đa tiêu chí (Vai trò, Trạng thái), xem chi tiết hồ sơ và cập nhật trạng thái tài khoản (`ACTIVE`, `SUSPENDED`, `BANNED`) kèm lý do kiểm toán.
4. **Dịch vụ tải ảnh đại diện**: Hỗ trợ tải tệp ảnh từ máy tính (`POST /api/profile/avatar`, `multipart/form-data`) lưu trữ an toàn trên máy chủ kết hợp dán link URL trực tiếp.

---

## 2. Bối cảnh kỹ thuật (Technical Context)

- **Ngôn ngữ & Nền tảng**: Java 17, Spring Boot 3.3.4 (Backend tại cổng 8085)  
- **Frontend**: HTML5, CSS3, Tailwind CSS, Vanilla JavaScript ES6+ Module (Dev server tại cổng 3000)  
- **Cơ sở dữ liệu**: PostgreSQL (Supabase instance), các bảng `users`, `learners`, `tutors` đã có sẵn  
- **Lưu trữ tệp Avatar**: Thư mục lưu trữ tệp cục bộ trên máy chủ cấu hình tại `app.profile.avatar-storage-dir` (mặc định `${user.home}/kizuna-data/avatars`), kích thước tối đa 5MB  
- **Bảo mật & Phân quyền**: Spring Security với `JwtTokenProvider`, RBAC kiểm soát theo vai trò `ROLE_LEARNER`, `ROLE_TUTOR`, `ROLE_MANAGER`, `ROLE_ADMIN`  
- **Kiểm thử**: JUnit 5, Mockito, MockMvc, `node --test`  
- **Hiệu năng & Ràng buộc**: Thời gian phản hồi API < 300ms, không lộ mã lỗi kỹ thuật, 100% tiếng Việt tự nhiên, không viết hoa toàn bộ (No ALL-CAPS)  

---

## 3. Đánh giá Hiến pháp dự án (Constitution Check)

*GATE: Toàn bộ 5 nguyên tắc cốt lõi của Hiến pháp KizunaTutor phải được tuân thủ nghiêm ngặt:*

| Nguyên tắc | Đánh giá tuân thủ | Biện pháp bảo đảm |
| :--- | :---: | :--- |
| **I. Không lộ lỗi kỹ thuật & Không ALL-CAPS** | **ĐẠT (PASS)** | Mọi thông báo lỗi từ Controller/Service được bọc qua Exception Handler tiếng Việt thân thiện; giao diện dùng Sentence case cho mô tả và Title Case cho tiêu đề/nút bấm. |
| **II. Không dữ liệu giả (No Mock Data) & Skeleton Loading** | **ĐẠT (PASS)** | Giao diện nạp dữ liệu thật 100% từ REST API `/api/profile/...`; khi đang tải hiển thị Skeleton Loading (`animate-pulse`); khi không có kết quả hiển thị Empty State trung thực. |
| **III. Phân hệ độc lập & Giao tiếp qua Service** | **ĐẠT (PASS)** | Mã nguồn nằm trọn vẹn trong `com.kizuna.features.profile`; không gọi trực tiếp Repository của feature khác; `core/` chỉ giữ entity gốc. |
| **IV. Chuẩn hóa Trang chủ & Nguồn chân lý Component** | **ĐẠT (PASS)** | Mọi trang frontend mới sử dụng `#header-placeholder` và `#footer-placeholder`; bảng màu Wabi-Sabi ấm (`#FCFBF7`, Lam Chàm `#006194`, viền `#ECE8DE`). |
| **V. Ranh giới nghiệp vụ & Kỷ luật Git** | **ĐẠT (PASS)** | Không can thiệp thanh toán học phí hay tiền lương; kiểm thử đi kèm trước khi đánh dấu hoàn thành; tuyệt đối không tự ý `git push`. |

---

## 4. Cấu trúc thư mục mã nguồn (Project Structure)

### 4.1. Phân hệ Backend (`backend/src/main/java/com/kizuna/features/profile/`)

```text
backend/src/main/java/com/kizuna/features/profile/
├── controller/
│   ├── LearnerProfileController.java        # REST API /api/profile/learner/me
│   ├── TutorProfileController.java          # REST API /api/profile/tutor/me
│   ├── ManagerUserProfileController.java    # REST API /api/profile/users/**
│   └── AvatarController.java                # REST API /api/profile/avatar/**
├── dto/
│   ├── request/
│   │   ├── UpdateLearnerProfileRequest.java
│   │   ├── UpdateTutorProfileRequest.java
│   │   └── UpdateUserStatusRequest.java
│   └── response/
│       ├── LearnerProfileResponseDto.java
│       ├── TutorProfileDetailDto.java
│       ├── UserProfileListItemDto.java
│       ├── UserProfileDetailDto.java
│       ├── AvatarUploadResponseDto.java
│       └── PageResponse.java
├── mapper/
│   ├── LearnerProfileMapper.java
│   ├── TutorProfileMapper.java
│   └── UserProfileMapper.java
└── service/
    ├── LearnerProfileService.java
    ├── TutorProfileService.java
    ├── ManagerUserProfileService.java
    ├── AvatarStorageService.java
    └── impl/
        ├── LearnerProfileServiceImpl.java
        ├── TutorProfileServiceImpl.java
        ├── ManagerUserProfileServiceImpl.java
        └── AvatarStorageServiceImpl.java
```

### 4.2. Giao diện Frontend (`frontend/`)

```text
frontend/
├── js/
│   └── api.js                               # Bổ sung namespace api.profile
├── pages/
│   ├── learner-profile/                     # Hồ sơ cá nhân Học viên trong dashboard học viên (MỚI)
│   ├── tutor-teaching-profile/              # Thông tin giảng dạy trong dashboard gia sư (MỚI)
│   │   ├── index.html
│   │   └── learner-profile.js
│   ├── tutor-profile/                       # Bổ sung nút & Modal sửa hồ sơ Gia sư
│   │   ├── index.html
│   │   └── tutor-profile.js
│   └── manager-users/                       # Trang quản trị người dùng cho Manager (MỚI)
│       ├── index.html
│       └── manager-users.js
└── tests/
    └── profile.test.mjs                     # Test kiểm thử hợp đồng giao diện & API
```

---

## 5. Theo dõi độ phức tạp (Complexity Tracking)

> Tính năng tuân thủ hoàn toàn mô hình kiến trúc chuẩn của dự án; không có vi phạm hay ngoại lệ kiến trúc nào cần bào chữa.

---

## 6. Ghi chú triển khai (Implementation Notes)

### Phase 1 & 2 (T001–T009) — 2026-11-10

- **T001**: Không tạo gói rỗng (BACKEND_RULES mục 2: "gói chưa có mã thì chưa tạo"). `dto/request/` và `mapper/` sẽ có lớp đầu tiên ở Phase 3; đã xóa `.gitkeep` ở các gói đã có lớp Java (`controller/`, `dto/`, `service/`, `service/impl/`).
- **T005 – `PageResponse` riêng của feature**: `core/dto/PageResponse` hiện có dùng trường `items` và không có `last`, không khớp hợp đồng mục 4.1 (`content`, `page`, `size`, `totalElements`, `totalPages`, `last`). Theo `tasks.md`, tạo `features/profile/dto/response/PageResponse` thay vì sửa `core/` (đang được feature khác dùng).
- **T007 – Kiểm tra tệp**: đuôi `jpg/jpeg/png/webp` (chuẩn hóa `jpeg` → `jpg`), dung lượng 1 byte – 5MB, đối chiếu magic bytes (PNG, JPEG, `RIFF....WEBP`). Tên tệp lưu là `UUID.<đuôi>` do hệ thống sinh, không dùng tên gốc; khi đọc chỉ chấp nhận tên đúng mẫu này và nằm trong thư mục lưu (chống path traversal). Thư mục: `app.profile.avatar-storage-dir`, đọc từ biến môi trường `AVATAR_STORAGE_DIR`, mặc định `${user.home}/kizuna-data/avatars`.
- **T008 – Phân quyền**: dùng `@PreAuthorize("isAuthenticated()")` trên `POST /api/profile/avatar` (cùng cách `TrialBookingController`), không sửa `core/security/SecurityConfig`. Vì quy tắc URL hiện là `permitAll`, người chưa đăng nhập nhận **403** "Bạn không có quyền thực hiện thao tác này." thay vì 401. `GET /api/profile/avatar/{fileName}` công khai, trả `Cache-Control: public, max-age=7 ngày` và `X-Content-Type-Options: nosniff`; `Content-Type` có thêm `;charset=UTF-8` do cấu hình `server.servlet.encoding.force=true` của dự án.
- **T002/T003**: chỉ dựng khung trang (khung `learner-layout` cho học viên, xem mục bên dưới; khung `staff-layout` cho quản lý, như các trang quản lý khác). Trạng thái tải, rỗng, lỗi và dữ liệu thật làm ở T018–T019, T036–T037.
- **Kiểm thử**: `AvatarStorageServiceTest` (6 ca), `AvatarControllerTest` (3 ca) tại `backend/src/test/java/com/kizuna/features/profile/` — xanh.

### Thay đổi hướng giao diện theo yêu cầu người dùng — 2026-11-10

- **Lý do**: người dùng yêu cầu học viên và gia sư sửa hồ sơ ngay trong dashboard riêng của mình, không qua trang độc lập hay trang công khai.
- **Học viên**: `learner-profile` chuyển sang khung `learner-layout`; thêm mục "Hồ sơ cá nhân" vào nhóm "Bàn học tập" của `components/learner-layout/learner-layout.js`. Đường dẫn là `/pages/learner-profile/` (cùng cách mục "Lớp học của tôi"), vì matcher `/learner` của mục "Tổng quan" sẽ tô sáng nhầm nếu dùng `/learner/profile`.
- **Gia sư**: bỏ phương án gắn nút "Chỉnh sửa hồ sơ" và modal vào trang công khai `tutor-profile` (research.md quyết định 1). Thay bằng trang mới `pages/tutor-teaching-profile/`, route `/tutor/profile` trong `frontend/server.js`, mục sidebar "Thông tin giảng dạy" trong `components/tutor-layout/tutor-layout.js` (đặt trước "Hồ sơ gia sư" của FE-03). Trường `isOwner` trong hợp đồng giữ nguyên.
- T018, T028, T029 trong `tasks.md` đã cập nhật theo hướng này.

### Phase 3 — User Story 1 (T010–T019) — 2026-11-10

- **Backend**: `LearnerProfileController` (`GET`/`PUT /api/profile/learner/me`, `@PreAuthorize("hasAuthority('ROLE_LEARNER')")`), luôn lấy tài khoản từ phiên đăng nhập, không nhận ID trên đường dẫn. `LearnerProfileServiceImpl` kiểm tra lại vai trò học viên và tài khoản chưa bị xóa, cập nhật `users` (`full_name`, `phone`, `avatar_url`) và `learners` trong một transaction. Email không bao giờ bị sửa.
- **Chưa có bản ghi `learners`**: `GET` trả các trường học tập rỗng (không tạo bản ghi khi chỉ xem); `PUT` tạo bản ghi mới gắn với tài khoản.
- **Số điện thoại trùng** (spec mục 3.2): trả 409 "Số điện thoại này đã được liên kết với một tài khoản khác trong hệ thống.". Kiểm tra bằng `JpaSpecificationExecutor` có sẵn của `UserRepository`, không thêm phương thức vào `core/`.
- **Trường tùy chọn**: `phone`, `avatarUrl`, `currentLevel`, `targetLevel`, `learningGoals` để trống được lưu là `null`. Regex số điện thoại cho phép rỗng: `^((0|\+84)[0-9]{9})?$`. `avatarUrl` chỉ nhận `http(s)://...` hoặc `/api/profile/avatar/<tên tệp>`.
- **Phản hồi `PUT`** dùng chung `LearnerProfileResponseDto` với `GET` (có thêm `createdAt` so với ví dụ ở hợp đồng mục 1.2).
- **Frontend**: `learner-profile` có đủ 4 trạng thái (đang tải, lỗi kèm nút thử lại, nội dung, giá trị trống hiển thị "Chưa chọn"/"Chưa cập nhật"), modal chỉnh sửa có kiểm tra dữ liệu tiếng Việt, tải ảnh lên hoặc dán đường dẫn; sau khi lưu cập nhật `kizuna_user` (tên, ảnh) để header dùng chung hiển thị đúng. Module khởi tạo ngay khi chạy vì `sidebarRouter` import lại module mỗi lần chuyển trang.
- **Kiểm thử**: `LearnerProfileServiceTest` (6 ca), `LearnerProfileControllerTest` (5 ca) — xanh. Lưu ý cho T048: mẫu `-Dtest="*Profile*Test"` còn khớp `com.kizuna.operations.ManagementReportProfileTest` (FE-11, cần `OPERATIONS_TEST_DB_URL`); nên dùng `-Dtest="com.kizuna.features.profile.*Test"`.
