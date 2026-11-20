# Chuẩn Bị Nhánh và Tạo Pull Request vào dev Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tạo nhánh sạch `feature/learning-support-ai-reading` từ `origin/dev`, tích hợp trọn vẹn các module của bạn và giải quyết triệt để 3 xung đột layout/router/server, commit với thông điệp người dùng yêu cầu và đẩy lên remote để tạo Pull Request.

**Architecture:** Sử dụng Git checkout từ `origin/dev` mới nhất, trích xuất chính xác các module và tệp thuộc phân hệ Hỗ trợ học tập / AI Reading từ nhánh `hm`, ghép mã nguồn chuẩn cho các tệp dùng chung (Sidebar Accordion, SPA Router, Server aliases và UTF-8 fix), kiểm thử không xung đột trước khi commit và push.

**Tech Stack:** Git, JavaScript (ES Modules / Web Components), Node.js, Java Spring Boot, Maven.

**Spec:** `docs/superpowers/specs/2026-11-19-pr-preparation-dev-design.md`

## Global Constraints

- Nhánh cơ sở tuyệt đối là `origin/dev` mới nhất.
- Không làm mất bất kỳ mã nguồn hay cấu hình nào của thành viên khác trên `dev` (Cloudflare R2, Quản lý tài khoản, Đặt lịch, v.v.).
- Khắc phục triệt để lỗi mã hóa UTF-8 trong `frontend/server.js`.
- Cấu hình Sidebar mục "Hỗ trợ học tập" tuân theo cấu trúc Accordion/Dropdown của nhánh `hm`.
- Commit message bắt buộc: `"Áp dụng chuẩn Sidebar nhóm Hỗ trợ học tập, sửa lỗi UTF8,"`.

## Review Focus

- Xung đột ngầm khi merge với `dev`: đảm bảo `git merge-tree` hoặc merge test cho kết quả sạch 100%.
- Lỗi mã hóa UTF-8 BOM hoặc ký tự hỏng (mojibake) trên các file frontend: kiểm tra nội dung hiển thị tiếng Việt chuẩn.
- Các route alias trên `server.js` cho `/reading`, `/ai-reading`: đảm bảo trỏ đúng trang `frontend/pages/ai-reading/index.html`.
- Các dependency trong `backend/pom.xml`: đảm bảo có cả AWS S3/R2 SDK (từ `dev`) và POI/Lombok.
- Giữ nguyên toàn bộ các bộ Unit Test đã viết.

---

### Task 1: Khởi tạo nhánh làm việc sạch từ origin/dev

**Files:**
- Local Git branch: `feature/learning-support-ai-reading`

- [ ] **Step 1: Fetch `origin/dev` mới nhất và tạo nhánh**
Run:
```bash
git fetch origin dev
git checkout -B feature/learning-support-ai-reading origin/dev
```
- [ ] **Step 2: Kiểm tra trạng thái nhánh**
Run: `git status`
Expected: `On branch feature/learning-support-ai-reading, nothing to commit, working tree clean`.

---

### Task 2: Chuyển giao các tệp và module độc lập từ nhánh hm sang nhánh mới

**Files:**
- Copy từ `hm`:
  - `frontend/pages/ai-reading/` (thư mục mới)
  - `frontend/pages/flashcards*/`
  - `frontend/pages/kanji*/`
  - `frontend/pages/shodo-practice/`
  - `frontend/pages/learning-support/`
  - `frontend/pages/ai-writing/`
  - `frontend/pages/grammar/`
  - `frontend/pages/handwriting-search/`
  - `frontend/js/handwriting-search.js`
  - `frontend/js/kanji/`
  - `frontend/assets/vendor/xlsx.full.min.js`
  - Backend Reading Assistant & Learning Support:
    - `backend/src/main/java/com/kizuna/features/learning_support/` (controller, dto, entity, repository, service liên quan)
    - Toàn bộ unit tests mới trong `backend/src/test/java/`
  - Tài liệu & scripts:
    - `database/backup_database.ps1`
    - `docs/BAO_CAO_TU_CHU_DU_LIEU.md`
    - `docs/guidelines/`
    - `specs/001-learning-support-unit-test/`
    - `KizunaTutor_Unit_Test_Document_v1.0.xlsx`
    - `Unit_Test_Bo_Sung_Module5_Hoangvm.xlsx`

- [ ] **Step 1: Dùng `git checkout hm -- <paths>` để kéo các tệp độc lập**
- [ ] **Step 2: Kiểm tra danh sách staged/unstaged files**
Run: `git status --short`
Expected: Các file thuộc module của bạn xuất hiện ở trạng thái added/modified, không có file của module khác.

---

### Task 3: Tích hợp chuẩn xác 4 tệp dùng chung (Giải quyết triệt để 3 conflict)

**Files:**
- Modify: `frontend/components/learner-layout/learner-layout.js`
- Modify: `frontend/js/sidebarRouter.js`
- Modify: `frontend/server.js`
- Modify: `backend/pom.xml`

- [ ] **Step 1: Tích hợp `frontend/components/learner-layout/learner-layout.js`**
  - Cập nhật nhóm menu "Hỗ trợ học tập" với 4 trụ cột dạng Accordion/Dropdown từ nhánh `hm`.
  - Giữ lại các mục menu khác của `dev` (Lớp học, Quản lý, Tài khoản...).
  - Thêm xử lý `nav-dropdown-toggle` và hiển thị chevron mũi tên.
- [ ] **Step 2: Tích hợp `frontend/js/sidebarRouter.js`**
  - Giữ nguyên các class `LEARNER_SUBNAV_*`.
  - Hỗ trợ sự kiện click vào dropdown menu không bị reload hay chặn điều hướng.
  - Tự động mở dropdown cha khi trang con tương ứng đang active.
  - Đồng bộ modal và dynamic script loader.
- [ ] **Step 3: Khắc phục lỗi UTF-8 và tích hợp alias trên `frontend/server.js`**
  - Bắt đầu từ file sạch UTF-8 không BOM của `dev`.
  - Bổ sung URL_ALIASES cho reading:
    ```javascript
    '/reading': '/pages/ai-reading/index.html',
    '/reading.html': '/pages/ai-reading/index.html',
    '/ai-reading': '/pages/ai-reading/index.html',
    '/ai-reading.html': '/pages/ai-reading/index.html',
    '/learner/reading': '/pages/ai-reading/index.html',
    '/learner/ai-reading': '/pages/ai-reading/index.html',
    '/ai-reading.js': '/pages/ai-reading/ai-reading.js',
    ```
- [ ] **Step 4: Kiểm tra `backend/pom.xml`**
  - Đảm bảo giữ nguyên block AWS SDK BOM / S3 dependency từ `dev`.
  - Đảm bảo có Apache POI và các dependency cần thiết cho module của bạn.

---

### Task 4: Kiểm chứng không xung đột (Verification)

- [ ] **Step 1: Kiểm tra test merge với origin/dev**
Run:
```bash
git merge-tree $(git merge-base HEAD origin/dev) HEAD origin/dev
```
Expected: Không có dòng `changed in both` gây ra `CONFLICT`.
- [ ] **Step 2: Kiểm tra mã hóa file tiếng Việt**
Kiểm tra `frontend/server.js` và `frontend/components/learner-layout/learner-layout.js` hiển thị rõ chữ tiếng Việt có dấu, không có ký tự lỗi.

---

### Task 5: Commit, Push lên Remote và Tạo Pull Request URL

- [ ] **Step 1: Thêm toàn bộ các thay đổi và commit**
Run:
```bash
git add .
git commit -m "Áp dụng chuẩn Sidebar nhóm Hỗ trợ học tập, sửa lỗi UTF8,"
```
- [ ] **Step 2: Push nhánh mới lên remote GitHub**
Run:
```bash
git push -u origin feature/learning-support-ai-reading
```
- [ ] **Step 3: Sinh liên kết tạo Pull Request trên GitHub**
Cung cấp URL tạo PR từ `feature/learning-support-ai-reading` vào nhánh đích `dev`:
`https://github.com/PeoNoPro/KizunaTutor/compare/dev...feature/learning-support-ai-reading?expand=1`
