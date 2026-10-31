# Tasks: FE-04 — Tutor Discovery & Search (Learner)

**Input**: `specs/004-tutor-discovery-learner/` — spec.md, plan.md; constitution 1.0.0; FRONTEND_RULES.md.

**Prerequisites**: Đã hoàn tất 5 quyết định làm rõ (Clarifications). Đã tích hợp giao diện Figma vào `frontend/tutors.html`.

**Organization**: Setup → Nền tảng Mock Data → US1 & US2 (P1: Lọc tức thì & Sắp xếp) → US3 (P2: Chi tiết hồ sơ & Lịch rảnh) → US4 (P2: Gia sư tiêu biểu) → Tích hợp Backend REST API & Supabase → Nghiệm thu toàn diện.

## Format: `[ID] [P?] [Story] Description`

- `[P]`: Có thể thực hiện song song khi không phụ thuộc hoặc không sửa chung file.
- `[Story]`: User story tương ứng (`US1`, `US2`, `US3`, `US4`, `BACKEND`, `POLISH`).
- Mọi đường dẫn file là đường dẫn cụ thể trong repository.

---

## Phase 1: Setup & Shared Infrastructure

**Purpose**: Thiết lập môi trường chạy thử và các tài nguyên dùng chung cho phân hệ Frontend.

- [x] T001 Tạo trang danh sách tìm kiếm gia sư `frontend/tutors.html` từ bản thiết kế Figma, liên kết `css/global.css`.
- [x] T002 Tạo server cục bộ `frontend/server.js` (Node.js native) chạy cổng 3000 để phục vụ các trang HTML tĩnh.
- [x] T003 [P] Cập nhật liên kết điều hướng hai chiều giữa `frontend/home.html` và `frontend/tutors.html`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Xây dựng cấu trúc dữ liệu mẫu (Mock Data) và cấu hình điều khiển nền tảng.

- [x] T004 Xây dựng tập dữ liệu mẫu `frontend/js/data/tutors.mock.js` chứa 6 hồ sơ gia sư tiêu biểu (Yamamoto Kenji, Đặng Quỳnh Chi, Takahashi Rena, Nguyễn Hoàng Nam, Suzuki Daisuke, Trần Mai Phương) với đầy đủ các trường: `jlptLevel`, `hourlyRate`, `learningGoals`, `teachingFormat`, `availability`, `rating`, `reviewsCount`.
- [x] T005 Khởi tạo khung điều khiển `frontend/js/pages/tutors.js` quản lý trạng thái (`state`) bộ lọc và kết nối với các thẻ HTML trong `tutors.html`.

**Checkpoint**: Nền tảng dữ liệu và khung điều khiển sẵn sàng để kiểm thử các User Story.

---

## Phase 3: User Story 1 — Tìm kiếm & Lọc gia sư tức thì (Priority: P1) 🎯 MVP

**Goal**: Học viên tìm kiếm từ khóa và lọc gia sư tức thì (Live Filtering) qua 5 tiêu chí: Mục tiêu học tập, Trình độ JLPT (N5–N1), Hình thức học (Online/Offline), Mức học phí/giờ, và Khung giờ rảnh.

**Independent Test**: Mở `http://localhost:3000/tutors.html`, gõ từ khóa hoặc chọn bất kỳ bộ lọc nào, danh sách thẻ gia sư tự động cập nhật trong < 300ms. Khi không có kết quả, hiển thị Empty State (MSG01) kèm nút xóa bộ lọc.

- [x] T006 [US1] Hiện thực hàm lọc đa tiêu chí (AND logic) trong `frontend/js/pages/tutors.js`:
  - Từ khóa tìm kiếm đối sánh trên: Họ tên, Chuyên môn chính, Cấp độ JLPT.
  - Bộ lọc Trình độ JLPT qua các chip pill (`N1` đến `N5` và `Tất cả`).
  - Bộ lọc Mục tiêu học tập (`kaiwa`, `jlpt`, `business`, `it`).
  - Bộ lọc Hình thức học (`online`, `offline`, `hybrid`).
  - Bộ lọc Khoảng học phí (`150-250`, `250-400`, `400-600`).
  - Bộ lọc Lịch rảnh (`today`, `weekend`, `evening`).
- [x] T007 [US1] Tích hợp cơ chế Debounce 300ms cho ô nhập từ khóa `#search-input` để tối ưu trải nghiệm gõ phím.
- [x] T008 [US1] Xây dựng khối hiển thị chip bộ lọc đang áp dụng (`#active-filter-chips`) với nút `x` xóa nhanh từng tiêu chí.
- [x] T009 [US1] Hiện thực chức năng *"Đặt lại bộ lọc"* (`#reset-filter-btn`) đưa toàn bộ ô tìm kiếm và select về trạng thái ban đầu.
- [x] T010 [US1] Hiện thực màn hình rỗng chuẩn (**Empty State - MSG01**) hiển thị thông báo lịch sự khi không có gia sư nào khớp điều kiện.

---

## Phase 4: User Story 2 — Sắp xếp kết quả tìm kiếm (Priority: P1)

**Goal**: Cho phép học viên linh hoạt sắp xếp danh sách gia sư theo đánh giá cao nhất, học phí tăng/giảm dần hoặc số năm kinh nghiệm.

**Independent Test**: Thay đổi lựa chọn trong dropdown `#sort-select`, danh sách gia sư tái sắp xếp ngay lập tức mà không cần tải lại trang.

- [x] T011 [US2] Hiện thực hàm sắp xếp `sortTutors()` trong `frontend/js/pages/tutors.js`:
  - Mặc định: `rating` (Điểm sao trung bình cao nhất, kết hợp số lượt review).
  - Tùy chọn: `price_asc` (Học phí thấp đến cao).
  - Tùy chọn: `price_desc` (Học phí cao đến thấp).
  - Tùy chọn: `exp` (Nhiều năm kinh nghiệm giảng dạy nhất).
- [x] T012 [US2] Gắn sự kiện `change` trên dropdown `#sort-select` để tự động kích hoạt sắp xếp và render lại lưới gia sư.

---

## Phase 5: User Story 3 — Chi tiết hồ sơ & Lịch rảnh gia sư (Priority: P2)

**Goal**: Cung cấp giao diện xem chi tiết hồ sơ gia sư, bao gồm tiểu sử, video giới thiệu, chứng chỉ được xác thực và ma trận lịch rảnh dạy trong tuần.

**Independent Test**: Nhấp nút *"Xem hồ sơ"* trên bất kỳ thẻ gia sư nào, modal/trang chi tiết mở ra hiển thị đầy đủ thông tin chuyên môn và các ca rảnh.

- [x] T013 [P] [US3] Thiết kế modal chi tiết hồ sơ trong `frontend/tutors.html` hiển thị chi tiết giáo viên, giới thiệu, chứng chỉ JLPT và phương pháp dạy.
- [x] T014 [US3] Xây dựng widget lịch rảnh 7 ngày trong tuần (Availability Calendar Matrix) hiển thị trực quan các ca dạy (Sáng, Chiều, Tối) theo chuẩn giờ VN (GMT+7).
- [x] T015 [US3] Thiết kế modal *"Gửi yêu cầu kết nối"* (`#connect-modal`) cho phép học viên nhập mục tiêu học, trình độ hiện tại và lời nhắn gửi gia sư kèm thông báo xác nhận lịch sự.

---

## Phase 6: User Story 4 — Đề xuất Gia sư tiêu biểu (Priority: P2)

**Goal**: Hiển thị khối Gia sư tiêu biểu (Featured Tutors có đánh giá ≥ 4.9 sao) trên trang chủ và liên kết sang trang tìm kiếm.

**Independent Test**: Mở `home.html`, khối gia sư tiêu biểu hiển thị đúng 4 gia sư có đánh giá cao nhất, bấm vào xem hồ sơ chuyển hướng đúng sang `tutors.html`.

- [x] T016 [US4] Đồng bộ danh sách gia sư tiêu biểu giữa `home.html` và tập mock data `tutors.mock.js`.
- [x] T017 [US4] Cập nhật các nút CTA *"Xem tất cả gia sư"* trên `home.html` trỏ về `tutors.html`.

---

## Phase 7: Backend REST API Integration (Spring Boot & PostgreSQL Supabase)

**Purpose**: Xây dựng kiến trúc tầng Backend kết nối cơ sở dữ liệu PostgreSQL Supabase thật.

- [x] T018 [P] [BACKEND] Tạo DTO `TutorSearchCriteriaDTO`, `TutorSummaryResponseDTO` và `TutorSearchPageResponse` trong `backend/src/main/java/com/kizuna/features/tutor_search/dto/`.
- [x] T019 [BACKEND] Tạo `TutorSearchSpecification.java` sử dụng JPA Criteria API để lọc động theo: `jlptLevel`, `hourlyRate`, `status = 'APPROVED'`, `keyword` và `accountStatus = 'ACTIVE'`.
- [x] T020 [BACKEND] Tạo `TutorSearchService` (Interface & Impl) và `TutorSearchRepository` (kế thừa `JpaSpecificationExecutor`) trong `backend/src/main/java/com/kizuna/features/tutor_search/`.
- [x] T021 [BACKEND] Xây dựng `TutorSearchController.java` với các endpoint:
  - `GET /api/tutors/search` (phân trang, lọc động).
  - `GET /api/tutors/featured` (danh sách gia sư tiêu biểu).
  - `GET /api/tutors/{id}` (chi tiết hồ sơ công khai).
  - Cấu hình `SecurityConfig.java` cho phép truy cập công khai không chặn CSRF.
- [x] T022 [BACKEND] Viết Unit/Integration test `TutorSearchApiTest.java` kiểm tra các bộ lọc và phân trang kết nối trực tiếp Supabase (`BUILD SUCCESS`).
- [x] T023 [BACKEND] Cập nhật `frontend/js/pages/tutors.js` tích hợp hàm `fetchTutorsFromApi()` gọi `API_CONFIG.BASE_URL + '/tutors/search'` với fallback tự động sang mock data nếu backend chưa bật.

---

## Phase 8: Polish & Cross-Cutting Verification

**Purpose**: Đảm bảo chất lượng trải nghiệm, tính tương thích thiết bị và tuân thủ các quy tắc bất biến.

- [x] T024 Kiểm tra Responsive trên 3 kích thước màn hình: Mobile (375px), Tablet (768px), Desktop (1240px).
- [x] T025 Rà soát toàn bộ UI Copywriting đảm bảo 100% tiếng Việt tự nhiên, không có chữ IN HOA TOÀN BỘ (trừ từ viết tắt JLPT, IT, BJT).
- [ ] T026 Kiểm tra console browser đảm bảo 0 lỗi JavaScript, 0 cảnh báo tài nguyên bị thiếu.

