# Tasks: FE-07 — Homework, Assessment & Progress Tracking

**Input**: spec.md, plan.md, data-model.md, contracts/assignments.md. Backend viết tắt `prg/` = `backend/src/main/java/com/kizuna/features/progress/`; test `prgTest/` = `backend/src/test/java/com/kizuna/features/progress/`.

## Phase 1: Setup

- [X] T001 Tạo `database/migrations/008_fe07_assignment_submissions_file_data.sql` (`ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS file_data BYTEA;`) — ghi chú người phụ trách đã chạy; **không chạy lại**.
- [X] T002 [P] Thêm dòng `file_data BYTEA,` sau `attachment_url` trong `CREATE TABLE assignment_submissions` của `database/DatabasePgAdmin4.txt` (không chạy file).
- [X] T003 [P] Đổi `backendPort = 8080` → `8085` trong `frontend/server.js` (Q6).

## Phase 2: Foundational

- [X] T004 Viết lại `prg/entity/Assignment.java` và `prg/entity/AssignmentSubmission.java` theo data-model.
- [X] T005 [P] `prg/repository/AssignmentRepository.java` (theo space, mới nhất trước) và `AssignmentSubmissionRepository.java` (theo assignment+learner theo attempt; `existsByAssignment…AndIsFinalizedTrue`; native `updateFileData`/`findFileData`).
- [X] T006 [P] DTO trong `prg/dto/` theo contracts.
- [X] T007 `prgTest/AssignmentRulesTest.java` — mọi tổ hợp hạn nộp (có/không, còn/quá) × số lần nộp × đã chấm.
- [X] T008 `prg/util/AssignmentRules.java` (hàm thuần). Chạy T007 đạt.
- [X] T009 [P] `prg/exception/AssignmentExceptionHandler.java`.

## Phase 3: US1 + US2 + US6 — Bài tập (UC-66, UC-63, UC-95)

- [X] T010 `prgTest/AssignmentServiceImplTest.java`: tạo hợp lệ/không hợp lệ (tiêu đề, hạn quá khứ, điểm); lớp chỉ xem/không phải gia sư → chặn; list/detail cho gia sư & học viên, người ngoài bị chặn, học viên chỉ thấy bài nộp của mình; sửa khi chưa chấm, chặn khi đã chấm, hạn mới quá khứ bị chặn.
- [X] T011 `prg/service/AssignmentService.java` + `impl/AssignmentServiceImpl.java`. Chạy T010 đạt.

## Phase 4: US3 + US4 + US5 — Nộp, nộp lại, chấm (UC-64, UC-96, UC-67)

- [X] T012 `prgTest/SubmissionServiceImplTest.java`: nộp lần đầu (chữ/tệp/cả hai), thiếu cả hai, tệp sai; nộp lại khi còn hạn; quá hạn nộp lần đầu → muộn; quá hạn nộp lại → chặn; đã chấm → chặn; lớp chỉ xem → chặn; gia sư không nộp được; chấm trước hạn → chặn; chấm sau hạn/không hạn → lưu; chấm lần 2 → chặn; chấm lần nộp cũ → chặn; điểm ngoài khoảng → chặn; học viên không chấm được; mở tệp đúng/sai quyền.
- [X] T013 `prg/service/SubmissionService.java` + `impl/SubmissionServiceImpl.java`. Chạy T012 đạt.

## Phase 5: API

- [X] T014 `prg/controller/AssignmentController.java` (7 endpoint theo contracts). `.\mvnw.cmd test-compile` + chạy test progress & classroom.

## Phase 6: Frontend

- [X] T015 [P] `frontend/js/assignment-helpers.js` (nhãn trạng thái, định dạng hạn nộp, kiểm form bài tập/nộp bài/điểm) + `frontend/tests/assignment-helpers.test.js`.
- [X] T016 Append `API.assignments` vào `frontend/js/api.js` (list, detail, create, update, submit multipart, submissionFile blob, grade).
- [X] T017 `frontend/pages/tutor-classrooms/*`: khối "Bài tập" trong chi tiết lớp (danh sách, nút "Giao bài tập" khi quản lý được, sửa khi `canEdit`, xem các lần nộp, mở tệp, form chấm khi `canGrade`).
- [X] T018 `frontend/pages/my-courses/*`: khối "Bài tập" (danh sách, chi tiết, lịch sử nộp, form nộp/nộp lại khi `canSubmit`, điểm + nhận xét khi đã chấm).

## Phase 7: Verification

- [X] T019 Xóa `.gitkeep` ở thư mục progress đã có file; chạy toàn bộ test backend (classroom + progress) và frontend.
- [ ] T020 (một phần — đã kiểm API thật các trường hợp từ chối/chỉ đọc; chờ người phụ trách thử luồng tạo → nộp → chấm trên web) Chạy backend thật: gọi API với tài khoản demo cho các trường hợp bị từ chối (không ghi dữ liệu thử); người phụ trách thử luồng tạo → nộp → chấm trên web.
- [X] T021 Rà UI (tiếng Việt, không ALL-CAPS, trạng thái rỗng/lỗi), `git diff --stat` đúng danh sách file; không commit/push. Báo cáo Giai đoạn 5.

## Dependencies

Phase 1–2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 (T016 trước T017/T018) → Phase 7.

## Traceability

| FR | Tasks |
|---|---|
| FR-001, FR-010 | T010–T013 |
| FR-002, FR-003 | T010–T011, T017 |
| FR-004 | T010–T011, T017–T018 |
| FR-005–FR-007 | T007–T008, T012–T013, T018 |
| FR-008 | T007–T008, T012–T013, T017 |
| FR-009 | T005, T012–T013 |
| FR-011 | T009, T015 |
