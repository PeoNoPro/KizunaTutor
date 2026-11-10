# Danh mục công việc (Tasks): FE-02 — Quản lý hồ sơ Học viên & Gia sư (Profile Management)

**Tài liệu tham chiếu**: [spec.md](spec.md) | [plan.md](plan.md) | [research.md](research.md) | [data-model.md](data-model.md) | [contracts/profile-api.md](contracts/profile-api.md) | [quickstart.md](quickstart.md)  
**Nhánh**: `haipt`  
**Quy ước định dạng**: `- [ ] [TaskID] [P?] [Story?] Mô tả công việc với đường dẫn tệp cụ thể`  
- `[P]`: Có thể chạy song song (khác tệp, không phụ thuộc task chưa xong)  
- `[Story]`: Gắn nhãn User Story (`[US1]`, `[US2]`, `[US3]`, `[US4]`)  

---

## Phase 1: Thiết lập hạ tầng chung (Setup & Shared Infrastructure)

**Mục tiêu**: Chuẩn bị cấu trúc thư mục, cấu hình lưu trữ tệp và các lớp tiện ích dùng chung cho phân hệ Profile.

- [x] T001 Khởi tạo cấu trúc các package trong `backend/src/main/java/com/kizuna/features/profile/` (`controller/`, `dto/request/`, `dto/response/`, `mapper/`, `service/impl/`)
- [x] T002 [P] Khởi tạo cấu trúc trang frontend cho Học viên tại `frontend/pages/learner-profile/` (`index.html`, `learner-profile.js`) trong khung dashboard học viên (`learner-layout`), mục sidebar "Hồ sơ cá nhân"
- [x] T002b [P] Khởi tạo trang "Thông tin giảng dạy" trong dashboard gia sư tại `frontend/pages/tutor-teaching-profile/` (`index.html`, `tutor-teaching-profile.js`, khung `tutor-layout`, route `/tutor/profile` trong `frontend/server.js`)
- [x] T003 [P] Khởi tạo cấu trúc trang frontend cho Quản lý tại `frontend/pages/manager-users/` (`index.html`, `manager-users.js`)
- [x] T004 [P] Cấu hình thuộc tính thư mục lưu trữ ảnh đại diện `app.profile.avatar-storage-dir` trong `backend/src/main/resources/application.properties`

---

## Phase 2: Nền tảng dùng chung (Foundational Components)

**Mục tiêu**: Xây dựng dịch vụ tải/phục vụ ảnh đại diện và DTO phân trang chuẩn mà các User Story đều phụ thuộc.  
**⚠️ Điều kiện tiên quyết**: Hoàn thành trước khi bước vào các User Story cụ thể.

- [x] T005 [P] Tạo response DTO phân trang dùng chung `PageResponse.java` tại `backend/src/main/java/com/kizuna/features/profile/dto/response/PageResponse.java`
- [x] T006 [P] Tạo DTO phản hồi upload ảnh `AvatarUploadResponseDto.java` (`avatarUrl`, `fileName`, `size`) tại `backend/src/main/java/com/kizuna/features/profile/dto/response/AvatarUploadResponseDto.java`
- [x] T007 Tạo Service lưu trữ tệp ảnh `AvatarStorageService.java` và cài đặt `AvatarStorageServiceImpl.java` (kiểm tra định dạng `jpg`, `jpeg`, `png`, `webp`, dung lượng <= 5MB, lưu tệp an toàn) tại `backend/src/main/java/com/kizuna/features/profile/service/`
- [x] T008 Tạo REST Controller `AvatarController.java` hỗ trợ `POST /api/profile/avatar` (upload ảnh) và `GET /api/profile/avatar/{fileName}` (phục vụ ảnh) tại `backend/src/main/java/com/kizuna/features/profile/controller/AvatarController.java`
- [x] T009 [P] Bổ sung module `api.profile.uploadAvatar` vào tệp giao tiếp API `frontend/js/api.js`

**Điểm kiểm soát (Checkpoint)**: Nền tảng tải ảnh đại diện và phân trang sẵn sàng.

---

## Phase 3: User Story 1 - Học viên xem và cập nhật hồ sơ cá nhân (Priority: P1) 🎯 MVP

**Mục tiêu**: Học viên xem thông tin cá nhân, cập nhật họ tên, SĐT, ảnh đại diện, trình độ tiếng Nhật (`BEGINNER`, `N5`..`N1`) và mục tiêu học tập.  
**Kiểm chứng độc lập**: Đăng nhập tài khoản Học viên, truy cập `/pages/learner-profile/`, cập nhật thông tin và kiểm tra dữ liệu hiển thị cập nhật chính xác.

### Kiểm thử cho User Story 1
- [x] T010 [P] [US1] Viết Unit Test cho `LearnerProfileServiceTest.java` kiểm tra lấy thông tin và cập nhật hợp lệ/bất hợp lệ tại `backend/src/test/java/com/kizuna/features/profile/LearnerProfileServiceTest.java`
- [x] T011 [P] [US1] Viết MockMvc Test cho `LearnerProfileControllerTest.java` xác nhận phân quyền `ROLE_LEARNER` tại `backend/src/test/java/com/kizuna/features/profile/LearnerProfileControllerTest.java`

### Triển khai Backend cho User Story 1
- [x] T012 [P] [US1] Tạo DTO `LearnerProfileResponseDto.java` (`userId`, `email`, `fullName`, `phone`, `avatarUrl`, `currentLevel`, `targetLevel`, `learningGoals`, `createdAt`, `updatedAt`) tại `backend/src/main/java/com/kizuna/features/profile/dto/response/LearnerProfileResponseDto.java`
- [x] T013 [P] [US1] Tạo Request DTO `UpdateLearnerProfileRequest.java` (`fullName` not blank, `phone` regex 10 số, `currentLevel` BEGINNER/N5..N1, `targetLevel` N5..N1, `learningGoals`) tại `backend/src/main/java/com/kizuna/features/profile/dto/request/UpdateLearnerProfileRequest.java`
- [x] T014 [P] [US1] Tạo mapper chuyển đổi `LearnerProfileMapper.java` giữa `User`, `Learner` và DTOs tại `backend/src/main/java/com/kizuna/features/profile/mapper/LearnerProfileMapper.java`
- [x] T015 [US1] Cài đặt `LearnerProfileService.java` và `LearnerProfileServiceImpl.java` (lấy hồ sơ theo `userId` người dùng hiện tại, cập nhật đồng bộ `users` và `learners`, khóa `email` chỉ đọc) tại `backend/src/main/java/com/kizuna/features/profile/service/`
- [x] T016 [US1] Tạo REST Controller `LearnerProfileController.java` với 2 endpoint: `GET /api/profile/learner/me` và `PUT /api/profile/learner/me` tại `backend/src/main/java/com/kizuna/features/profile/controller/LearnerProfileController.java`

### Triển khai Frontend cho User Story 1
- [x] T017 [US1] Bổ sung các hàm `getLearnerProfile()` và `updateLearnerProfile(data)` vào namespace `api.profile` trong `frontend/js/api.js`
- [x] T018 [US1] Xây dựng giao diện thẻ hồ sơ cá nhân chuẩn Wabi-Sabi (trong khung `learner-layout`, thông tin tài khoản, badge trình độ, khối mục tiêu) tại `frontend/pages/learner-profile/index.html`
- [x] T019 [US1] Xây dựng logic tải dữ liệu, hiển thị Skeleton Loading, mở Modal chỉnh sửa, xác thực định dạng form tiếng Việt và gọi API lưu hồ sơ tại `frontend/pages/learner-profile/learner-profile.js`

**Điểm kiểm soát (Checkpoint)**: User Story 1 hoàn tất đầy đủ cả Backend và Frontend, hoạt động độc lập như một bản MVP.

---

## Phase 4: User Story 2 - Gia sư xem và cập nhật thông tin giảng dạy (Priority: P1)

**Mục tiêu**: Gia sư mở mục "Thông tin giảng dạy" trong dashboard gia sư (`/tutor/profile`) để xem và cập nhật tiêu đề, tiểu sử, số năm KN, học phí, trình độ JLPT, Tỉnh/Thành và địa chỉ.  
**Kiểm chứng độc lập**: Đăng nhập tài khoản Gia sư, mở dashboard gia sư → "Thông tin giảng dạy" (`/tutor/profile`), chỉnh sửa, lưu dữ liệu và kiểm tra thông tin hiển thị làm mới tức thì.

### Kiểm thử cho User Story 2
- [ ] T020 [P] [US2] Viết Unit Test cho `TutorProfileServiceTest.java` kiểm tra lấy thông tin và cập nhật hồ sơ giảng dạy gia sư tại `backend/src/test/java/com/kizuna/features/profile/TutorProfileServiceTest.java`
- [ ] T021 [P] [US2] Viết MockMvc Test cho `TutorProfileControllerTest.java` xác nhận phân quyền `ROLE_TUTOR` tại `backend/src/test/java/com/kizuna/features/profile/TutorProfileControllerTest.java`

### Triển khai Backend cho User Story 2
- [ ] T022 [P] [US2] Tạo DTO `TutorProfileDetailDto.java` (thông tin gia sư, học phí, tiêu đề, tiểu sử, Tỉnh/Thành, `isOwner`) tại `backend/src/main/java/com/kizuna/features/profile/dto/response/TutorProfileDetailDto.java`
- [ ] T023 [P] [US2] Tạo Request DTO `UpdateTutorProfileRequest.java` (`fullName`, `phone`, `headline`, `bio`, `experienceYears`, `hourlyRate`, `highestJlptLevel`, `province`, `addressDetail`) tại `backend/src/main/java/com/kizuna/features/profile/dto/request/UpdateTutorProfileRequest.java`
- [ ] T024 [P] [US2] Tạo mapper chuyển đổi `TutorProfileMapper.java` giữa `User`, `Tutor` và DTOs tại `backend/src/main/java/com/kizuna/features/profile/mapper/TutorProfileMapper.java`
- [ ] T025 [US2] Cài đặt `TutorProfileService.java` và `TutorProfileServiceImpl.java` (lấy hồ sơ gia sư của chính mình, cập nhật đồng bộ `users` và `tutors`, chuẩn hóa địa chỉ Tỉnh/Thành) tại `backend/src/main/java/com/kizuna/features/profile/service/`
- [ ] T026 [US2] Tạo REST Controller `TutorProfileController.java` với 2 endpoint: `GET /api/profile/tutor/me` và `PUT /api/profile/tutor/me` tại `backend/src/main/java/com/kizuna/features/profile/controller/TutorProfileController.java`

### Triển khai Frontend cho User Story 2
- [ ] T027 [US2] Bổ sung các hàm `getTutorProfile()` và `updateTutorProfile(data)` vào namespace `api.profile` trong `frontend/js/api.js`
- [ ] T028 [US2] Xây dựng giao diện xem và form cập nhật thông tin giảng dạy (Dropdown Tỉnh/Thành, ô nhập học phí, tiêu đề, tiểu sử) tại `frontend/pages/tutor-teaching-profile/index.html` (không sửa trang công khai `tutor-profile`)
- [ ] T029 [US2] Xây dựng logic tại `frontend/pages/tutor-teaching-profile/tutor-teaching-profile.js`: tải hồ sơ của chính gia sư, nạp dữ liệu vào form, xác thực và gọi API cập nhật hồ sơ gia sư

**Điểm kiểm soát (Checkpoint)**: User Story 2 hoàn tất; Gia sư có thể tự quản trị thông tin cá nhân và giảng dạy.

---

## Phase 5: User Story 3 - Quản lý vận hành tra cứu danh sách người dùng (Priority: P2)

**Mục tiêu**: Quản lý vận hành (Manager) tra cứu, lọc phân trang danh sách người dùng (Học viên & Gia sư) theo vai trò, trạng thái và từ khóa.  
**Kiểm chứng độc lập**: Đăng nhập tài khoản Manager, mở `/pages/manager-users/`, chuyển đổi các tab "Tất cả", "Gia sư", "Học viên", tìm kiếm và lọc trạng thái.

### Kiểm thử cho User Story 3
- [ ] T030 [P] [US3] Viết Unit Test cho `ManagerUserProfileServiceTest.java` kiểm tra truy vấn phân trang và tìm kiếm tại `backend/src/test/java/com/kizuna/features/profile/ManagerUserProfileServiceTest.java`
- [ ] T031 [P] [US3] Viết MockMvc Test cho `ManagerUserProfileControllerTest.java` xác nhận phân quyền `ROLE_MANAGER`/`ROLE_ADMIN` tại `backend/src/test/java/com/kizuna/features/profile/ManagerUserProfileControllerTest.java`

### Triển khai Backend cho User Story 3
- [ ] T032 [P] [US3] Tạo DTO `UserProfileListItemDto.java` (`userId`, `email`, `fullName`, `phone`, `avatarUrl`, `role`, `roleBadge`, `accountStatus`, `createdAt`) tại `backend/src/main/java/com/kizuna/features/profile/dto/response/UserProfileListItemDto.java`
- [ ] T033 [US3] Cài đặt `ManagerUserProfileService.java` và `ManagerUserProfileServiceImpl.java` hỗ trợ hàm `getUsers(role, status, keyword, pageable)` với JPA Specification/Query tại `backend/src/main/java/com/kizuna/features/profile/service/`
- [ ] T034 [US3] Tạo REST Controller `ManagerUserProfileController.java` với endpoint `GET /api/profile/users` tại `backend/src/main/java/com/kizuna/features/profile/controller/ManagerUserProfileController.java`

### Triển khai Frontend cho User Story 3
- [ ] T035 [US3] Bổ sung hàm `getUsers(params)` vào namespace `api.profile` trong `frontend/js/api.js`
- [ ] T036 [US3] Thiết kế giao diện bảng danh sách người dùng, thanh tìm kiếm, tab phân loại và dropdown lọc trạng thái tại `frontend/pages/manager-users/index.html`
- [ ] T037 [US3] Xây dựng logic phân trang, lọc dữ liệu thời gian thực và hiển thị Empty State khi không có kết quả tại `frontend/pages/manager-users/manager-users.js`

**Điểm kiểm soát (Checkpoint)**: User Story 3 hoàn tất; Manager có công cụ tra cứu người dùng toàn diện.

---

## Phase 6: User Story 4 - Quản lý xem chi tiết hồ sơ & cập nhật trạng thái tài khoản (Priority: P2)

**Mục tiêu**: Quản lý xem thông tin chi tiết một tài khoản và có quyền thay đổi trạng thái tài khoản (`ACTIVE`, `SUSPENDED`, `BANNED`) kèm lý do hành chính bắt buộc.  
**Kiểm chứng độc lập**: Từ danh sách người dùng, mở Modal chi tiết, đổi trạng thái sang Tạm khóa kèm lý do, kiểm tra tài khoản bị chặn đăng nhập.

### Kiểm thử cho User Story 4
- [ ] T038 [P] [US4] Viết Unit Test cho cập nhật trạng thái tài khoản và xác thực lý do bắt buộc tại `backend/src/test/java/com/kizuna/features/profile/ManagerUserStatusTest.java`

### Triển khai Backend cho User Story 4
- [ ] T039 [P] [US4] Tạo DTO `UserProfileDetailDto.java` (thông tin tài khoản chi tiết + chi tiết học tập/giảng dạy) tại `backend/src/main/java/com/kizuna/features/profile/dto/response/UserProfileDetailDto.java`
- [ ] T040 [P] [US4] Tạo Request DTO `UpdateUserStatusRequest.java` (`status` not null, `reason` not blank từ 5-500 ký tự) tại `backend/src/main/java/com/kizuna/features/profile/dto/request/UpdateUserStatusRequest.java`
- [ ] T041 [US4] Bổ sung hàm `getUserDetail(userId)` và `updateUserStatus(userId, request, managerEmail)` vào `ManagerUserProfileServiceImpl.java` (cập nhật `account_status`, ghi log audit) tại `backend/src/main/java/com/kizuna/features/profile/service/impl/ManagerUserProfileServiceImpl.java`
- [ ] T042 [US4] Bổ sung 2 endpoint vào `ManagerUserProfileController.java`: `GET /api/profile/users/{id}` và `PUT /api/profile/users/{id}/status` tại `backend/src/main/java/com/kizuna/features/profile/controller/ManagerUserProfileController.java`

### Triển khai Frontend cho User Story 4
- [ ] T043 [US4] Bổ sung `getUserDetail(id)` và `updateUserStatus(id, data)` vào namespace `api.profile` trong `frontend/js/api.js`
- [ ] T044 [US4] Thêm Modal "Xem chi tiết người dùng" và Modal "Cập nhật trạng thái tài khoản" (kèm ô nhập lý do) vào `frontend/pages/manager-users/index.html`
- [ ] T045 [US4] Xây dựng logic gọi API xem chi tiết, submit đổi trạng thái và làm mới badge trạng thái trong bảng dữ liệu tại `frontend/pages/manager-users/manager-users.js`

**Điểm kiểm soát (Checkpoint)**: User Story 4 hoàn tất; hoàn chỉnh năng lực quản trị người dùng cho Quản lý vận hành.

---

## Phase 7: Hoàn thiện & Rà soát toàn diện (Polish & Cross-Cutting Concerns)

**Mục tiêu**: Rà soát chất lượng hiển thị, ngôn ngữ tiếng Việt, kiểm tra bảo mật và chạy toàn bộ kịch bản kiểm chứng.

- [ ] T046 [P] Rà soát 100% nhãn, nút bấm, thông báo lỗi và toast trên cả 3 trang (`learner-profile`, `tutor-profile`, `manager-users`), đảm bảo không có chữ viết hoa toàn bộ (No ALL-CAPS) và không rò rỉ mã lỗi kỹ thuật
- [ ] T047 [P] Viết test kiểm thử hợp đồng giao diện `frontend/tests/profile.test.mjs`
- [ ] T048 Chạy lệnh biên dịch và kiểm thử tự động toàn bộ backend: `.\mvnw.cmd test -pl backend -Dtest="*Profile*Test"`
- [ ] T049 Thực hiện toàn bộ 3 kịch bản kiểm chứng thực tế trên trình duyệt theo hướng dẫn trong [quickstart.md](quickstart.md)

---

## Thứ tự phụ thuộc & Kế hoạch thực hiện (Dependencies & Execution Order)

### Phụ thuộc giữa các Phase
1. **Phase 1 (Setup)**: Không phụ thuộc, bắt đầu ngay.
2. **Phase 2 (Foundational)**: Phụ thuộc Phase 1. Cần hoàn tất trước khi triển khai các User Story.
3. **Phase 3 (User Story 1 - MVP)**: Phụ thuộc Phase 2. Triển khai độc lập hoàn chỉnh cho Học viên.
4. **Phase 4 (User Story 2)**: Phụ thuộc Phase 2. Triển khai độc lập hoàn chỉnh cho Gia sư.
5. **Phase 5 (User Story 3)**: Phụ thuộc Phase 2. Triển khai bảng danh sách người dùng cho Manager.
6. **Phase 6 (User Story 4)**: Phụ thuộc Phase 5. Bổ sung xem chi tiết và đổi trạng thái.
7. **Phase 7 (Polish)**: Phụ thuộc toàn bộ các story đã hoàn thành.

### Cơ hội chạy song song (Parallel Opportunities)
- Các task có đánh dấu `[P]` trong cùng một Phase có thể thực hiện song song (khác tệp mã nguồn).
- Sau khi hoàn thành Phase 2 (Foundational), Developer có thể làm **User Story 1 (Học viên)** và **User Story 2 (Gia sư)** song song vì hai story này tác động vào hai màn hình và controller tách biệt.

