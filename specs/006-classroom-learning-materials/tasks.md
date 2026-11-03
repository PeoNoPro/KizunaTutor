# Tasks: FE-06 — Classroom & Learning Materials Management

**Input**: `specs/006-classroom-learning-materials/` — spec.md, plan.md, research.md, data-model.md, contracts/classrooms.md, quickstart.md; constitution 1.0.0.

**Prerequisites**: 6 câu làm rõ đã chốt (2026-11-03). Bucket `learning-materials` và biến môi trường do người phụ trách chuẩn bị trước T034.

**Tests**: Unit test (JUnit 5 + Mockito) viết cùng/ trước phần triển khai tương ứng; không cần DB thật hay Supabase thật.

## Format: `[ID] [P?] [Story] Description`

- `[P]`: có thể làm song song (khác file, không phụ thuộc task chưa xong).
- Đường dẫn backend viết tắt `cls/` = `backend/src/main/java/com/kizuna/features/classroom/`; `clsTest/` = `backend/src/test/java/com/kizuna/features/classroom/`.

## Phase 1: Setup

- [X] T001 Tạo `database/migrations/006_fe06_learning_materials_description.sql` chỉ gồm `ALTER TABLE learning_materials ADD COLUMN IF NOT EXISTS description TEXT;` kèm chú thích mục đích. **Không chạy**; người phụ trách chạy theo quickstart.
- [X] T002 [P] Thêm dòng `description TEXT,` sau `title` trong khối `CREATE TABLE learning_materials` của `database/DatabasePgAdmin4.txt` (chỉ sửa văn bản; không chạy file).
- [X] T003 [P] Append mục `# 12. CAU HINH SUPABASE STORAGE` vào cuối `backend/src/main/resources/application.properties` theo research R2 (chỉ biến môi trường, không có secret).

## Phase 2: Foundational (chặn mọi user story)

- [X] T004 Viết lại `cls/entity/LearningMaterial.java` theo data-model (material_id, space LAZY, title, description, file_url, file_type, uploaded_by LAZY, created_at `@CreationTimestamp`; không kế thừa BaseEntity).
- [X] T005 [P] Tạo `cls/repository/LearningMaterialRepository.java`: `findBySpace_SpaceIdOrderByCreatedAtDesc`, `findByMaterialIdAndSpace_SpaceId`, `countBySpace_SpaceId`.
- [X] T006 [P] Tạo `cls/repository/LearningSpaceRepository.java`: `findByTutor_TutorIdOrderByCreatedAtDesc`, `findByLearner_LearnerIdOrderByCreatedAtDesc`, `existsByTutor_TutorIdAndLearner_LearnerIdAndStatusIn`, và 2 native query chỉ đọc `connection_requests` theo research R5.
- [X] T007 [P] Tạo `cls/exception/ClassroomException.java` (HttpStatus + message tiếng Việt) và `cls/exception/ClassroomExceptionHandler.java` (`@RestControllerAdvice(assignableTypes = ClassroomController.class)`, `@Order(HIGHEST_PRECEDENCE)`; trả `{success,message,data,timestamp}`; bắt thêm `MissingServletRequestPartException`/`MethodArgumentTypeMismatchException` → 400 tiếng Việt).
- [X] T008 Viết `clsTest/CurrentUserResolverTest.java` (thiếu header, sai token, user không tồn tại, gia sư, học viên, vai trò khác → 403).
- [X] T009 Tạo `cls/util/CurrentUserResolver.java` + record `CurrentActor` theo research R4 (dùng `JwtTokenProvider`, `UserRepository`, `TutorRepository`, `LearnerRepository` — chỉ gọi, không sửa). Chạy T008 đạt.
- [X] T010 Viết `clsTest/MaterialFileValidatorTest.java` (PDF/PNG/JPG/JPEG/MP3 hợp lệ; rỗng; > 10 MB; đuôi sai; đuôi đúng nhưng chữ ký sai; không có đuôi).
- [X] T011 Tạo `cls/util/MaterialFileValidator.java` theo research R3, trả loại chuẩn hóa + đuôi tệp; thông điệp lỗi tiếng Việt. Chạy T010 đạt.
- [X] T012 [P] Tạo `cls/service/MaterialStorageService.java` (interface: `upload(path, bytes, contentType)`, `createSignedUrl(path)`, `delete(path)`, `isConfigured()`) và `cls/service/impl/SupabaseMaterialStorageService.java` (RestClient, timeout, không log key; lỗi → `ClassroomException` 503 "Dịch vụ lưu trữ tạm thời không khả dụng…").
- [X] T013 [P] Tạo DTO trong `cls/dto/`: `ClassroomSummaryResponse`, `ClassroomCreateRequest`, `EligibleLearnerResponse`, `LearningMaterialResponse`, `MaterialFileLinkResponse` theo contracts.

**Checkpoint**: `.\mvnw.cmd test-compile` đạt; T008, T010 đạt.

## Phase 3: US1 — Gia sư mở lớp (P1, UC-93) 🎯 MVP

- [X] T014 [US1] Viết `clsTest/ClassroomServiceImplTest.java` phần US1: liệt kê lớp theo vai trò; eligible learners; tạo lớp hợp lệ; tên trống/quá 150; học viên chưa kết nối → 403; trùng ACTIVE/PAUSED → 409; hội viên EXPIRED → 409; học viên gọi tạo lớp → 403.
- [X] T015 [US1] Tạo `cls/service/ClassroomService.java` + `cls/service/impl/ClassroomServiceImpl.java`: `listMine`, `listEligibleLearners`, `create`, `getDetail`, `loadAccessibleSpace`, `isReadOnly`. Chạy T014 đạt.
- [X] T016 [US1] Tạo `cls/controller/ClassroomController.java` với `GET /api/classrooms`, `GET /api/classrooms/eligible-learners`, `POST /api/classrooms`, `GET /api/classrooms/{spaceId}`.
- [X] T017 [P] [US1] Tạo `frontend/js/classroom-helpers.js` (nhãn trạng thái tiếng Việt, định dạng ngày vi-VN, nhãn loại tệp, kiểm tệp phía client ≤ 10 MB + đuôi, ánh xạ lỗi → thông điệp thân thiện) và `frontend/tests/classroom-helpers.test.js`.
- [X] T018 [US1] Append nhóm `API.classrooms` vào `frontend/js/api.js` (list, eligibleLearners, create, detail, materials, materialFileLink, uploadMaterial, updateMaterial; gắn Bearer token; 401 → `/login`). Không sửa code sẵn có.
- [X] T019 [US1] Tạo `frontend/pages/tutor-classrooms/index.html` + `tutor-classrooms.js`: danh sách lớp, trạng thái rỗng/lỗi + thử lại, hộp thoại "Thêm học viên vào lớp" (chọn học viên đủ điều kiện, nhập tên lớp), thông báo thành công. DESIGN.md §7 (tím trầm gia sư).
- [X] T020 [US1] Append route `/tutor/classrooms` → `/pages/tutor-classrooms/index.html` vào `frontend/server.js`.

**Checkpoint**: US1 chạy độc lập.

## Phase 4: US2 — Tải lên tài liệu (P1, UC-65)

- [X] T021 [US2] Viết `clsTest/LearningMaterialServiceImplTest.java` phần upload: hợp lệ (gọi storage với path `spaces/{id}/{uuid}.ext`, lưu file_type); thiếu tiêu đề/tệp; mô tả > 1000; lớp không ACTIVE → 409; hội viên EXPIRED → 409; không phải gia sư lớp → 403; save lỗi → storage.delete được gọi; storage chưa cấu hình → 503.
- [X] T022 [US2] Tạo `cls/service/LearningMaterialService.java` + `cls/service/impl/LearningMaterialServiceImpl.java` (`upload`). Chạy T021 phần upload đạt.
- [X] T023 [US2] Thêm `POST /api/classrooms/{spaceId}/materials` (multipart) vào `ClassroomController`.
- [X] T024 [US2] Thêm vào trang gia sư: chi tiết lớp + biểu mẫu tải lên (kiểm client, lỗi dưới trường, giữ dữ liệu đã nhập, ẩn/vô hiệu khi chỉ xem kèm giải thích).

## Phase 5: US3 — Xem tài liệu (P1, UC-61)

- [X] T025 [US3] Bổ sung test vào `LearningMaterialServiceImplTest`: list cho gia sư/học viên của lớp; người ngoài → 403; space không tồn tại → 404; signed URL cho tài liệu thuộc lớp; tài liệu khác lớp → 404; học viên lớp chỉ xem vẫn được xem.
- [X] T026 [US3] Thêm `list`, `getFileLink` vào service; `GET /api/classrooms/{spaceId}/materials` và `GET …/materials/{materialId}/file` vào controller. Chạy T025 đạt.
- [X] T027 [US3] Trang gia sư: danh sách tài liệu (tiêu đề, mô tả, ngày tải lên, loại tệp), trạng thái rỗng "Hiện chưa có tài liệu nào trong lớp học này.", nút "Mở tài liệu" (mở signed URL tab mới).

## Phase 6: US4 — Học viên xem lớp của tôi (P2, UC-58)

- [X] T028 [US4] Tạo `frontend/pages/my-courses/index.html` + `my-courses.js` trong learner shell: danh sách lớp (tên lớp, gia sư, ngày bắt đầu, trạng thái), chi tiết + tài liệu (dùng lại helper), lỗi + "Thử lại", nhãn "Chỉ xem" khi readOnly.
- [X] T029 [US4] Append mục menu "Lớp học của tôi" (`/pages/my-courses/`) và `PAGE_META['my-courses']` vào `frontend/components/learner-layout/learner-layout.js`; append route `/my-courses` vào `frontend/server.js`. `sidebarRouter.js` tự tải mọi link nội bộ (đã kiểm) → không sửa; nếu khi chạy thử phát sinh nhu cầu sửa thì **dừng và hỏi**.

## Phase 7: US5 — Cập nhật/thay tài liệu (P2, UC-94)

- [X] T030 [US5] Bổ sung test: sửa tiêu đề/mô tả không kèm tệp; thay tệp hợp lệ → xóa tệp cũ sau lưu; tệp thay thế sai → giữ nguyên; save lỗi → xóa tệp mới, giữ tệp cũ; tài liệu không thuộc lớp → 404; không phải gia sư/chỉ xem → 403/409.
- [X] T031 [US5] Thêm `update` vào service và `PUT /api/classrooms/{spaceId}/materials/{materialId}` (multipart) vào controller. Chạy T030 đạt.
- [X] T032 [US5] Trang gia sư: nút "Chỉnh sửa" mở biểu mẫu điền sẵn, tùy chọn thay tệp, thông báo "Cập nhật tài liệu thành công.".

## Phase 8: Polish & Verification

- [X] T033 Xóa `.gitkeep` trong các thư mục classroom đã có file; `.\mvnw.cmd test-compile` và chạy toàn bộ test classroom; `node --test "frontend/tests/*.test.js"`.
- [ ] T034 (một phần — đã kiểm API thật trừ phần Supabase Storage; chờ bucket + biến môi trường) Kiểm thử tích hợp thủ công theo quickstart (cần bucket + env + migration đã chạy). Nếu chưa có, ghi rõ phần chưa xác minh.
- [X] T035 Rà soát UI: tiếng Việt, không ALL-CAPS, không enum/mã HTTP, không dữ liệu giả, trạng thái rỗng/lỗi, responsive 400px.
- [X] T036 `git diff --stat` xác nhận chỉ chạm file trong danh sách plan; không commit/push. Viết báo cáo Giai đoạn 5.

## Dependencies

- Phase 1 → Phase 2 → US1 → (US2 → US3) ; US4 phụ thuộc US3 backend (T026); US5 phụ thuộc US2.
- T009 trước mọi service; T011, T012 trước T022.
- `ClassroomController` (T016, T023, T026, T031) sửa tuần tự.
- Trang gia sư (T019, T024, T027, T032) sửa tuần tự.

## Traceability

| FR | Tasks |
|---|---|
| FR-001 | T008–T009 |
| FR-002 | T014–T016, T019, T028 |
| FR-003, FR-004 | T006, T014–T016, T019 |
| FR-005, FR-006, FR-016 | T014–T015, T021–T022, T024, T028, T030 |
| FR-007 | T015, T025–T026 |
| FR-008, FR-009 | T010–T011, T021–T024 |
| FR-010 | T012, T026 |
| FR-011 | T025–T027 |
| FR-012 | T030–T032 |
| FR-013 | T021, T030 |
| FR-014 | T028–T029 |
| FR-015 | T007, T017, T035 |
