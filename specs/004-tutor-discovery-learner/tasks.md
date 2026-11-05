# Tasks: FE-04 — Tutor Discovery, URL Synchronization & Dynamic Routing

**Input**: Design documents from `specs/004-tutor-discovery-learner/`:
- `spec.md` (User Stories US1, US2, US3, US4)
- `plan.md` (Implementation Plan, Architecture, 5 Phases)
- `research.md` (Decisions on Two-Way URL Sync, History API, Popstate, Clean URLs)
- `data-model.md` (TutorFilterState, Query Params Schema, Criteria DTO, Page Response)
- `contracts/tutor-search-contract.md` (REST API Contract `/api/tutors/search`)
- `quickstart.md` (18 Verification Scenarios)
- Project Constitution: `AGENTS.md`, `DESIGN.md`, `CONVENTIONS.md`

**Organization**:
- Tasks được phân tách rõ ràng theo 3 nhóm: **Backend**, **Frontend**, và **Kiểm thử (Testing & QA)**.
- Tiến độ: **100% các task đã hoàn thành và kiểm thử thành công**.

## Format: `[ID] [P?] [Story] Description with exact file path`

- `[P]`: Nhiệm vụ có thể chạy song song (khác file, không phụ thuộc task trước đó chưa xong).
- `[Story]`: User story hoặc nhóm chuyên môn tương ứng (`[BACKEND]`, `[FRONTEND]`, `[US1]`, `[US2]`, `[US3]`, `[US4]`, `[TEST]`).
- Mọi mô tả đều chứa đường dẫn file cụ thể trong dự án.

---

## Phase 1: Setup & Routing Infrastructure (Shared)

**Purpose**: Thiết lập máy chủ web, định tuyến URL ngắn gọn `/tutors` và bảo đảm tài nguyên tĩnh không bị lỗi đường dẫn.

- [x] T001 Cấu hình alias định tuyến cho `/tutors` và `/tutors/` phục vụ file `frontend/pages/tutor-discovery/index.html` trong `frontend/server.js`.
- [x] T002 [P] Đảm bảo quy tắc phục vụ tài nguyên tĩnh (`.js`, `.css`, fonts, SVGs) với đúng MIME type UTF-8 khi người dùng truy cập từ route ngắn `/tutors` trong `frontend/server.js`.
- [x] T003 [P] Chuẩn hóa thẻ `<script type="module" src="/pages/tutor-discovery/tutor-discovery.js"></script>` trong `frontend/pages/tutor-discovery/index.html`.
- [x] T004 Xác nhận route động `/gs/:tutorCode` trong `frontend/server.js` tiếp tục hoạt động ổn định và sẵn sàng tiếp nhận điều hướng từ các thẻ gia sư.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Xây dựng nền tảng kiểm định tham số, chuyển đổi kiểu dữ liệu và cấu trúc quản lý trạng thái đồng bộ trước khi hiện thực logic chi tiết.

- [x] T005 [P] [FRONTEND] Xây dựng module kiểm định tham số URL `validateUrlParams` trong `frontend/pages/tutor-discovery/tutor-discovery.js` theo Whitelist:
  - `jlpt`: `['N1', 'N2', 'N3', 'N4', 'N5', 'all']`
  - `goal`: `['kaiwa', 'jlpt', 'business', 'it']`
  - `format`: `['online', 'offline', 'hybrid']`
  - `price`: `['150-250', '250-400', '400-600']`
  - `schedule`: `['today', 'weekend', 'evening']`
  - `sort`: `['rating', 'price_asc', 'price_desc', 'exp']`
  - `page`: Số nguyên `>= 1`
  - Fallback về giá trị mặc định an toàn khi tham số không hợp lệ, không gây lỗi runtime.
- [x] T006 [FRONTEND] Đóng gói hàm khôi phục trạng thái hoàn chỉnh `syncStateFromUrl()` trích xuất `URLSearchParams` từ `window.location.search` và đồng bộ lên các phần tử UI trong `frontend/pages/tutor-discovery/tutor-discovery.js`.
- [x] T007 [FRONTEND] Hiện thực hàm cập nhật URL hai chiều `updateUrlFromState(shouldPush = true)` trong `frontend/pages/tutor-discovery/tutor-discovery.js`:
  - Loại bỏ các giá trị mặc định (`jlpt=all`, `sort=rating`, `page=1`, chuỗi rỗng) để giữ Clean URL.
  - Sử dụng `window.history.pushState` khi chọn bộ lọc / đổi trang.
  - Sử dụng `window.history.replaceState` khi gõ tìm kiếm.
  - Sử dụng cờ `isPopStateHandling` chống vòng lặp vô tận `State ➔ URL ➔ State`.
- [x] T008 [FRONTEND] Đăng ký lắng nghe sự kiện `window.addEventListener('popstate')` trong `frontend/pages/tutor-discovery/tutor-discovery.js` để tự động khôi phục giao diện và tải dữ liệu khi bấm nút Back/Forward mà không tải lại trang.

---

## Phase 3: Backend Tasks (Search API, Specification & Criteria)

**Purpose**: Hoàn thiện và bảo đảm tầng Backend xử lý đúng các tiêu chí lọc, phân trang, sắp xếp và bảo mật cho API gia sư.

- [x] T009 [P] [BACKEND] Rà soát và hoàn thiện `TutorSearchCriteriaDTO.java` trong `backend/src/main/java/com/kizuna/features/tutor_search/dto/TutorSearchCriteriaDTO.java` hỗ trợ đủ các trường: `keyword`, `jlptLevel`, `goal`, `format`, `minPrice`, `maxPrice`, `schedule`, `sortBy`, `page`, `size`.
- [x] T010 [P] [BACKEND] Rà soát `TutorSummaryResponseDTO.java` và `TutorSearchPageResponse.java` trong `backend/src/main/java/com/kizuna/features/tutor_search/dto/` bảo đảm trả về `tutorCode` (Long), `tutorId` (UUID), `hourlyRate`, `avgRating`, `totalReviews`, `totalPages`, `totalElements`, `pageNumber`, `pageSize`.
- [x] T011 [BACKEND] Cập nhật truy vấn động JPA Criteria trong `backend/src/main/java/com/kizuna/features/tutor_search/repository/TutorSearchSpecification.java`:
  - Quy tắc bất biến GB-06: `verificationStatus = APPROVED`, `accountStatus = ACTIVE`, `deletedAt IS NULL`.
  - Tìm kiếm từ khóa không phân biệt hoa thường theo `fullName`, `headline`, `highestJlptLevel`.
  - Lọc theo trình độ JLPT chính xác khi khác `'all'`.
  - Lọc theo khoảng giá học phí: `hourlyRate >= minPrice` và `hourlyRate <= maxPrice`.
- [x] T012 [BACKEND] Hoàn thiện tầng nghiệp vụ `TutorSearchServiceImpl.java` trong `backend/src/main/java/com/kizuna/features/tutor_search/service/impl/TutorSearchServiceImpl.java`:
  - Ánh xạ thứ tự sắp xếp: `rating`, `price_asc`, `price_desc`, `exp`.
  - Phân trang chuẩn Spring Data JPA: `PageRequest.of(criteria.getPage(), criteria.getSize(), sort)`.
  - Map entity sang `TutorSummaryResponseDTO` bao gồm đầy đủ `tutorCode` và danh sách chuyên môn.
- [x] T013 [BACKEND] Cập nhật controller `TutorSearchController.java` trong `backend/src/main/java/com/kizuna/features/tutor_search/controller/TutorSearchController.java`:
  - Endpoint `GET /api/tutors/search` và `GET /api/tutors` nhận request parameters.
  - Endpoint `GET /api/tutors/featured` lấy top gia sư tiêu biểu.
  - Endpoint `GET /api/tutors/code/{code}` lấy chi tiết theo mã tự tăng.
- [x] T014 [BACKEND] Cấu hình CORS và bảo mật công khai cho các endpoint tìm kiếm gia sư trong `backend/src/main/java/com/kizuna/core/security/SecurityConfig.java`.

---

## Phase 4: Frontend Tasks (UI Controls, Filters, Pagination & API Resilience)

**Purpose**: Hiện thực trải nghiệm người dùng tìm kiếm, lọc tức thì, phân trang hai chiều và bảo đảm tính bền vững khi gọi API.

- [x] T015 [FRONTEND] [US1] Tích hợp cập nhật URL qua `replaceState` vào sự kiện Debounce 300ms của ô tìm kiếm `#search-input` trong `frontend/pages/tutor-discovery/tutor-discovery.js`.
- [x] T016 [FRONTEND] [US1] Bổ sung gọi `updateUrlFromState(true)` (pushState) vào sự kiện `click` của các chip JLPT (`.jlpt-chip`) trong `frontend/pages/tutor-discovery/tutor-discovery.js`.
- [x] T017 [FRONTEND] [US1] Bổ sung gọi `updateUrlFromState(true)` (pushState) vào sự kiện `change` của 4 dropdown bộ lọc (`#filter-goal`, `#filter-format`, `#filter-price`, `#filter-schedule`) trong `frontend/pages/tutor-discovery/tutor-discovery.js`.
- [x] T018 [FRONTEND] [US2] Bổ sung gọi `updateUrlFromState(true)` (pushState) vào sự kiện `change` của dropdown sắp xếp `#sort-select` trong `frontend/pages/tutor-discovery/tutor-discovery.js`.
- [x] T019 [FRONTEND] [US1] Cập nhật hàm xóa chip `window.removeFilter(key)` trong khối Active Filter Chips để cập nhật URL qua `pushState` trong `frontend/pages/tutor-discovery/tutor-discovery.js`.
- [x] T020 [FRONTEND] [US1] Cập nhật hàm `resetAllFilters()` cho nút *"Đặt lại bộ lọc"* và *"Xóa tất cả bộ lọc"* để xóa hết query params trên URL trong `frontend/pages/tutor-discovery/tutor-discovery.js`.
- [x] T021 [FRONTEND] [US1] Hiện thực phân trang (Pagination) hai chiều Frontend ↔ Backend trong `frontend/pages/tutor-discovery/tutor-discovery.js`:
  - Chuyển đổi `page = state.currentPage - 1` (0-indexed) khi gọi API.
  - Nhận `TutorSearchPageResponse`, hiển thị thông tin dải "Hiển thị X - Y trong tổng số Z gia sư".
  - Render động danh sách nút số trang (1, 2, 3...) và nút Trước / Sau; vô hiệu hóa nút tương ứng khi ở trang đầu hoặc trang cuối.
  - Khi bấm chuyển trang: cập nhật `state.currentPage`, gọi `pushState` (`?page=...`), cuộn mượt lên đầu danh sách và gọi API.
  - Xử lý tình huống biên: nếu `currentPage > totalPages`, tự động đặt lại `currentPage = 1`.
- [x] T022 [FRONTEND] [US3] Cập nhật hàm `renderTutorCard(tutor)` trong `frontend/pages/tutor-discovery/tutor-discovery.js`:
  - Kết xuất liên kết xem chi tiết hồ sơ gia sư dạng dynamic route `/gs/${tutor.tutorCode}`.
  - Hiển thị đầy đủ thông tin: họ tên, avatar, điểm sao, số review, badge đã duyệt, JLPT level, năm kinh nghiệm, học phí theo giờ và nút kết nối.
- [x] T023 [FRONTEND] [US1] Bổ sung màn hình Error thân thiện bằng tiếng Việt kèm nút *"Thử lại"* và Cơ chế chống Race Condition qua `currentRequestId` trong `frontend/pages/tutor-discovery/tutor-discovery.js`.

---

## Phase 5: Home Deep Linking Tasks (Navigation & Shortcuts)

**Purpose**: Kết nối liền mạch giữa Trang chủ và Trang tìm kiếm gia sư có sẵn các query parameters.

- [x] T024 [P] [US4] Cập nhật liên kết thanh điều hướng Header và nút "Tìm gia sư ngay" trong `frontend/pages/home/index.html` trỏ về route ngắn `/tutors`.
- [x] T025 [US4] Kết nối khối Quick Filter tại Hero Section trong `frontend/pages/home/index.html` và `frontend/pages/home/home.js`:
  - Khi người dùng chọn Mục tiêu, Hình thức, Mức học phí và bấm nút "Tìm kiếm gia sư", tạo URL tương ứng (ví dụ: `/tutors?goal=kaiwa&format=online`) và điều hướng sang trang tìm kiếm.
- [x] T026 [P] [US4] Kết nối các chip cấp độ JLPT (N1–N5) và danh mục chuyên đề trên Trang chủ trong `frontend/pages/home/index.html` trỏ về `/tutors` và `/tutors?goal=kaiwa`.
- [x] T027 [US4] Đảm bảo các thẻ trong khối Gia sư tiêu biểu (`featured-tutors-grid`) trong `frontend/pages/home/home.js` kết xuất liên kết hồ sơ dạng `/gs/${tutor.tutorCode}`.

---

## Phase 6: Testing & Quality Assurance (Backend, Frontend & URL Sync)

**Purpose**: Thực thi 18 kịch bản kiểm thử tích hợp bao phủ toàn diện Backend, Frontend và URL Synchronization theo `quickstart.md`.

- [x] T028 [P] [TEST] [BACKEND] Viết và chạy kiểm thử tự động `TutorSearchApiTest.java` trong `backend/src/test/java/com/kizuna/TutorSearchApiTest.java` (Passed 4/4 tests).
- [x] T029 [TEST] [FRONTEND] Kiểm thử Kịch bản 1: Lọc đơn JLPT N2 tại `/tutors` ➔ URL đổi thành `?jlpt=N2`, pill N2 active, danh sách lọc đúng.
- [x] T030 [TEST] [FRONTEND] Kiểm thử Kịch bản 2: Lọc kết hợp nhiều tiêu chí (`jlpt=N2&goal=kaiwa&format=online&sort=price_asc`) ➔ URL và danh sách kết hợp chính xác.
- [x] T031 [TEST] [FRONTEND] Kiểm thử Kịch bản 3: Nhập từ khóa với Debounce 300ms ➔ URL cập nhật qua `replaceState`, chỉ có 1 bước trong History, danh sách khớp từ khóa.
- [x] T032 [TEST] [FRONTEND] Kiểm thử Kịch bản 4: Tải lại trang (F5) ➔ Các control, dropdown, chip JLPT và danh sách kết quả khôi phục nguyên vẹn 100%.
- [x] T033 [TEST] [FRONTEND] Kiểm thử Kịch bản 5: Bấm nút Quay lại (Back) của trình duyệt ➔ URL và giao diện quay lại trạng thái bộ lọc trước mà không tải lại toàn trang.
- [x] T034 [TEST] [FRONTEND] Kiểm thử Kịch bản 6: Bấm nút Tiến lên (Forward) của trình duyệt ➔ URL và giao diện tiến đến trạng thái bộ lọc tiếp theo.
- [x] T035 [TEST] [FRONTEND] Kiểm thử Kịch bản 7: Xóa 1 chip trong Active Filter Chips ➔ Chỉ xóa đúng tiêu chí đó, giữ nguyên tiêu chí khác, URL cập nhật và danh sách nạp lại.
- [x] T036 [TEST] [FRONTEND] Kiểm thử Kịch bản 8: Đặt lại tất cả bộ lọc qua nút *"Đặt lại bộ lọc"* ➔ URL về `/tutors`, toàn bộ điều khiển về mặc định và danh sách nạp đủ tất cả gia sư.
- [x] T037 [TEST] [FRONTEND] Kiểm thử Kịch bản 9: Chuyển trang Phân trang ➔ URL cập nhật `?page=...` qua `pushState`, danh sách hiển thị đúng và cuộn màn hình mượt.
- [x] T038 [TEST] [FRONTEND] Kiểm thử Kịch bản 10: Xử lý URL chứa tham số không hợp lệ ➔ Module validation fallback an toàn về mặc định, không làm ứng dụng phát sinh lỗi.
- [x] T039 [TEST] [FRONTEND] Kiểm thử Kịch bản 11: Tìm kiếm không có kết quả ➔ Hiển thị đúng Empty State MSG01, ẩn thanh phân trang và nút xóa bộ lọc hoạt động tốt.
- [x] T040 [TEST] [FRONTEND] Kiểm thử Kịch bản 12: Gián đoạn kết nối Backend ➔ Giao diện hiển thị thông báo lỗi thân thiện bằng tiếng Việt và nút *"Thử lại"* hoạt động.
- [x] T041 [TEST] [FRONTEND] Kiểm thử Kịch bản 13: Thao tác bộ lọc liên tục ➔ Cơ chế `currentRequestId` ngăn request cũ trả về muộn không ghi đè kết quả của request mới nhất.
- [x] T042 [TEST] [FRONTEND] Kiểm thử Kịch bản 14: Truy cập và F5 tại route hồ sơ gia sư `/gs/10001` ➔ Hiển thị thông tin gia sư bình thường, không bị kẹt vòng xoay loading.
- [x] T043 [TEST] [FRONTEND] Kiểm thử Kịch bản 15: Deep Linking từ Trang chủ sang `/tutors` ➔ Quick Filter và chip cấp độ JLPT kích hoạt đúng bộ lọc.
- [x] T044 [TEST] [FRONTEND] Kiểm thử Kịch bản 16: Kiểm tra giá trị biên của các khoảng giá ➔ Hoạt động ổn định với logic phân loại.
- [x] T045 [TEST] [FRONTEND] Kiểm thử Kịch bản 17: Truy cập trực tiếp cả `/tutors` và `/tutors/` ➔ Web server nạp đúng trang tìm kiếm trả về HTTP 200 OK.
- [x] T046 [TEST] [FRONTEND] Kiểm thử Kịch bản 18: Kiểm tra tài nguyên tĩnh (JS, CSS, Font, SVG) ➔ 100% tài nguyên trả về HTTP 200, 0 lỗi 404.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Rà soát chất lượng trải nghiệm, bản địa hóa giao diện và tuân thủ các quy tắc cốt lõi của dự án.

- [x] T047 [P] Rà soát Responsive Design trên 3 kích thước: Mobile (375px), Tablet (768px), Desktop (1240px) cho trang `/tutors`.
- [x] T048 Rà soát toàn bộ chữ hiển thị trên giao diện theo `AGENTS.md` và `DESIGN.md`: 100% tiếng Việt tự nhiên, không lộ mã enum (`ROLE_*`, `APPROVED`), không có chữ IN HOA TOÀN BỘ.
- [x] T049 Mở Browser Console kiểm tra toàn bộ luồng tìm kiếm đảm bảo 0 lỗi JavaScript runtime và 0 warning nghiêm trọng.
