---
description: "Danh sách công việc triển khai FE-13 — Kho học liệu cho cán bộ học vụ"
---

# Tasks: FE-13 — Kho học liệu cho cán bộ học vụ

**Input**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/content-import-api.md](contracts/content-import-api.md), [quickstart.md](quickstart.md).

**Tests**: Có (constitution V). Chỉ test không chạm DB; SQL kiểm chứng bằng giao dịch rollback trên Supabase (research §7).

**Ghi chú quy trình**: Phase 1–5 được viết mã trước khi có đặc tả (sai thứ tự Spec Kit) và đã kiểm chứng ngày 2026-11-10; Phase R sửa phần lệch luật dự án mà không đổi chức năng.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: chạy song song được · **[Story]**: US1…US3 theo spec.md
- Viết tắt: `BE` = `backend/src/main/java/com/kizuna`, `LC` = `BE/features/learning_content`, `BT` = `backend/src/test/java/com/kizuna/features/learning_content`, `FE` = `frontend`.

---

## Phase 1: Setup

- [X] T001 Tạo khung phân hệ `LC/{controller,dto,repository,service}` (không tạo bảng, không migration)
- [X] T002 Thêm matcher `/api/academic/content-imports/**` → `ROLE_ACADEMIC_OFFICER` trong `BE/core/security/SecurityConfig.java` (chỉ thêm dòng; cần báo nhóm — plan Complexity Tracking)
- [X] T003 [P] Thêm route `/academic/content-import` trong `BE/core/config/WebMvcConfig.java` và `FE/server.js`
- [X] T004 [P] Thêm mục sidebar "Kho học liệu" trong `FE/components/academic-layout/academic-layout.js`

## Phase 2: Foundational

- [X] T005 `LC/service/ContentType.java`: 3 loại, cột (tên gọi khác, bắt buộc, độ dài), khóa trùng, mô tả mục
- [X] T006 `LC/service/ContentWorkbookReader.java`: chuẩn hóa tiêu đề, tìm tiêu đề trong 5 dòng đầu, đọc mọi sheet hợp lệ, kiểm tra dòng (FR-007, FR-014), giới hạn 50.000 dòng
- [X] T007 `LC/service/ContentImportPlanner.java`: trùng trong tệp, đã có giữ nguyên, mẫu ngữ pháp thuộc mã khác, Kanji mới cần số nét (FR-011…FR-013, FR-017)
- [X] T008 [P] `BT/ContentImportTest.java`: 5 test reader + 6 test planner

## Phase 3: User Story 2 — Nhập từ Excel (P1)

- [X] T010 [US2] `LC/repository/LearningContentStore.java`: đọc khóa hiện có, thêm theo lô, khóa giao dịch theo loại, liên kết `vocabulary_kanjis`, `kanjis ON CONFLICT DO NOTHING`
- [X] T011 [US2] Service nhập: kiểm tra tệp (`.xlsx`, ≤ 15 MB), đọc tệp ngoài giao dịch, `dryRun`, giao dịch nhập (FR-006, FR-009, FR-015, FR-016)
- [X] T012 [US2] `POST /api/academic/content-imports/{type}` trong `LC/controller/ContentImportController.java`; `GET /summary`
- [X] T013 [US2] Tab "Nhập từ Excel" trong `FE/pages/academic-content-import/`; `FE/js/content-import-helpers.js`; `FE/tests/content-import-helpers.test.mjs`; nhóm `academicContent` trong `FE/js/api.js`
- [X] T014 [US2] Đổi loại học liệu thì bỏ chọn tệp; thông báo sai tiêu đề nhắc kiểm tra loại (US2-5)
- [X] T015 [US2] Kiểm chứng trên DB bằng giao dịch rollback: thêm mới 3 loại, mục đã có giữ nguyên, liên kết Kanji (3 liên kết), dry-run 3 tệp mẫu gốc = 0 mới / 0 lỗi (SC-001)

## Phase 4: User Story 1 — Xem kho (P1)

- [X] T016 [US1] `LC/repository/ContentBrowseStore.java#listItems`: tìm kiếm (thoát `%` `_`), lọc JLPT, sắp xếp LEVEL/NEWEST, phân trang
- [X] T017 [US1] `GET /{type}/items`; tab "Danh sách" + hộp thoại chi tiết; test helper cột / hiển thị
- [X] T018 [US1] Kiểm chứng trên DB: tổng 6.701 / 7.811 / 526, lọc N3, tìm "gặp" (25), "憎" (1), từ khóa có `%_` không lỗi

## Phase 5: User Story 3 — Lịch sử nhập (P2)

- [X] T019 [US3] Ghi `system_audit_logs` trong cùng giao dịch nhập; `GET /history`, `GET /history/{id}`; tab "Lịch sử nhập" + hộp thoại chi tiết; kiểm chứng ghi / đọc / rollback trên DB

---

## Phase R: Sửa cho khớp luật dự án (không đổi chức năng)

- [X] T020 Tách service thành interface `LC/service/LearningContentService.java` + `LC/service/impl/LearningContentServiceImpl.java` (BACKEND_RULES §2–3); xóa `ContentImportService.java`
- [X] T021 [P] Đổi tên DTO theo `*Response`, mỗi DTO một file trong `LC/dto/`: `ContentImportResponse`, `ImportIssueResponse`, `ImportHistoryResponse`, `ImportHistoryDetailResponse`, `ContentTypeSummaryResponse`, `ContentSummaryResponse`; xóa `ContentImportDtos.java`
- [X] T022 Controller bọc phản hồi bằng `core/dto/ApiResponse`, trang dữ liệu dùng `core/dto/PageResponse` (contracts §0); bỏ `PageResult` riêng
- [X] T023 `FE/js/api.js` nhóm `academicContent` đọc `data` của phản hồi bọc (cùng lúc với T022)
- [X] T024 [P] Đổi tên repository theo hậu tố `Repository`: `LearningContentRepository`, `ContentImportHistoryRepository`
- [X] T030 Khung chờ tải (skeleton `animate-pulse`) cho bảng danh sách và lịch sử (constitution II.4, FR-022)
- [X] T031 Chuyển bộ tệp thử + README + `xoa_du_lieu_thu.sql` từ `database/test-import/` sang `backend/src/test/resources/content-import/` (AGENTS luật 7); cập nhật đường dẫn — **2026-11-12: đã xóa bộ tệp thử theo yêu cầu người phụ trách.**
- [X] T032 `BT/ContentImportFixturesTest.java`: chạy reader + planner trên bộ tệp thử (mục "đã có" lấy từ tệp `_1`) và so với kết quả trong README (SC-002) — **2026-11-12: đã xóa cùng bộ tệp thử; còn ContentImportTest (11 test).**
- [X] T033 Chạy `.\mvnw.cmd test-compile`, `.\mvnw.cmd test "-Dtest=ContentImport*"`, `node --test "frontend/tests/*.test.mjs"`; khởi động lại backend sạch; kiểm tra API trả `401` khi chưa đăng nhập — 2026-11-10: test-compile qua; ContentImport* 22/22 xanh; frontend 31/31; backend chạy lại, 3 API trả 401. **Chưa kiểm chứng lại trên DB sau Phase R** (mất kết nối Supabase lúc chạy) — SQL không đổi so với lần kiểm chứng T015/T018/T019; cần chạy lại kịch bản quickstart khi có mạng.

## Dependencies

- Phase 2 → Phase 3, 4, 5. T022 và T023 phải làm cùng lúc. T031 → T032. T033 cuối cùng.
- Việc cần nhóm xác nhận (không phải task mã): các mục Complexity Tracking trong plan.md; bổ sung 3 UC vào SRS (spec A-6).
