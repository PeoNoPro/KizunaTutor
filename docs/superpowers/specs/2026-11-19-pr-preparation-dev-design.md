# Design Doc: Chuẩn Bị Nhánh và Tạo Pull Request vào dev (KizunaTutor)

## 1. Bối cảnh & Mục đích (Intent & Why)
- Nhánh `hm` đã hoàn thiện và push lên git với các tính năng mới:
  - Phân hệ Hỗ trợ học tập (Learning Support): Tra cứu từ vựng, Từ điển Kanji, Tra cứu viết tay AI, Luyện gõ Kana, Phòng luyện Shodo, Minigame rơi chữ Kanji, Flashcards.
  - Tính năng AI Reading (Đọc hiểu Dokkai & phân tích ngữ pháp, từ vựng với AI).
  - Tối ưu UI/UX thanh điều hướng (Sidebar Accordion / Dropdown) dành riêng cho phần Hỗ trợ học tập và SPA Router mượt mà.
  - Bổ sung bộ Unit Tests & tài liệu liên quan.
- Hiện tại nhánh `dev` trên repo `PeoNoPro/KizunaTutor.git` đã có nhiều commit mới từ các thành viên khác (Cloudflare R2, Quản lý tài khoản, Đặt lịch...).
- Cần tạo Pull Request hợp nhất các tính năng trên vào nhánh `dev` mà không làm mất hoặc ghi đè mã nguồn của các thành viên khác, đồng thời xử lý triệt để 3 xung đột (conflict) ở layout, router và server.

## 2. Mục tiêu (Goals), Phi mục tiêu (Non-goals) & Chống mục tiêu (Anti-goals)
- **Mục tiêu (Goals):**
  - Tạo nhánh mới sạch xuất phát từ `origin/dev` mới nhất (ví dụ: `feature/learning-support-ai-reading`).
  - Đưa toàn bộ các file module, controller, service, test, frontend thuộc quyền phụ trách của bạn sang nhánh mới.
  - Tích hợp chuẩn xác giao diện Sidebar phần "Hỗ trợ học tập" (Accordion/Dropdown theo đúng bản thiết kế của bạn) vào `learner-layout.js` và `sidebarRouter.js`.
  - Giữ lại đầy đủ các tính năng, route và dependency mới trên `dev` (như AWS S3/R2 SDK trong `pom.xml`, các trang quản lý học viên/gia sư).
  - Khắc phục lỗi mã hóa ký tự (UTF-8) trên `frontend/server.js` khi thêm alias `/ai-reading`.
  - Push nhánh mới lên GitHub và cung cấp đường dẫn tạo Pull Request trực tiếp vào `dev`.
- **Phi mục tiêu (Non-goals):**
  - Không thay đổi hoặc can thiệp vào logic nghiệp vụ của các module khác (Booking, Tutor Verification, Operations/Incident...).
  - Không ép force-push đè lên nhánh `origin/dev`.
- **Chống mục tiêu (Anti-goals):**
  - Tuyệt đối không xóa hay ghi đè các cấu hình và file của đồng đội trên `dev`.
  - Không để lọt mã rác hoặc tệp bị lỗi font / mojibake vào `dev`.

## 3. Các hướng tiếp cận (Approaches) & Lựa chọn
- **Cách 1 (Được chọn): Tạo nhánh mới sạch từ `origin/dev` và chuyển giao có kiểm soát**
  - Nhánh mới đảm bảo 100% không có conflict ngầm, commit sạch sẽ, review PR rõ ràng, an toàn tuyệt đối cho nhánh chung `dev`.
- **Cách 2 (Loại): Merge trực tiếp `dev` vào `hm` tại local**
  - Lịch sử commit bị đan xen, tệp `server.js` đang dính lỗi mã hóa UTF-8 dễ bị sót ký tự hỏng lên `dev`.

## 4. Chi tiết thực hiện (Implementation Scope)
1. **Khởi tạo nhánh:** Tạo nhánh `feature/learning-support-ai-reading` từ `origin/dev`.
2. **Di chuyển các tệp/thư mục độc lập:**
   - Frontend: `pages/ai-reading/`, `pages/flashcards*`, `pages/kanji*`, `pages/shodo*`, `pages/learning-support/`, `pages/grammar/`, `pages/ai-writing/`, `pages/handwriting-search/`, `js/handwriting-search.js`, `js/kanji/`, `assets/vendor/xlsx.full.min.js`.
   - Backend: Các controller, service, DTO, entity, repository liên quan đến `reading_assistant`, `kanji`, `flashcard`, và toàn bộ các bộ unit test liên quan.
   - Docs & Scripts: `backup_database.ps1`, `docs/guidelines/`, `specs/001-learning-support-unit-test/`, các file Excel test document.
3. **Tích hợp các tệp dùng chung:**
   - `frontend/components/learner-layout/learner-layout.js`: Thay thế cụm menu "Hỗ trợ học tập" dạng Dropdown/Accordion theo đúng bản của bạn, giữ nguyên các menu khác và helper của `dev`.
   - `frontend/js/sidebarRouter.js`: Giữ chuẩn router hỗ trợ submenu (`LEARNER_SUBNAV_*`), `nav-dropdown-toggle` và dynamic script loader của bạn.
   - `frontend/server.js`: Bổ sung các route alias `/reading`, `/ai-reading`, `/learner/reading` trên nền server sạch của `dev`.
   - `backend/pom.xml`: Giữ cả Cloudflare R2 / AWS S3 SDK (từ dev) và POI / các thư viện cần cho module của bạn.
4. **Kiểm tra & Hoàn tất:**
   - Chạy `git status` và `git diff` đối chiếu.
   - Commit với thông điệp rõ ràng, chuẩn convention.
   - Push lên `origin/feature/learning-support-ai-reading`.
   - Hướng dẫn link mở Pull Request trực tiếp trên GitHub vào nhánh `dev`.
