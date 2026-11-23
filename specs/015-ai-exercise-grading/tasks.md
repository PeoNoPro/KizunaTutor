---
description: "Danh sách công việc triển khai FE-15 — AI gợi ý chấm câu tự luận của bài làm trực tiếp"
---

# Tasks: FE-15 — AI gợi ý chấm câu tự luận

**Input**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/exercise-ai-reviews-api.md](contracts/exercise-ai-reviews-api.md), [quickstart.md](quickstart.md).

**Tests**: Có. Unit test không chạm DB; migration do người phụ trách chạy bằng DBeaver.

## Format: `[ID] [P?] [Story] Description`

- Viết tắt: `PRG` = `backend/src/main/java/com/kizuna/features/progress`, `PT` = `backend/src/test/java/com/kizuna/features/progress`, `FE` = `frontend`.

---

## Phase 1: Setup

- [X] T001 Migration `database/migrations/036_exercise_ai_reviews.sql` (bảng, ràng buộc, chạy lại an toàn)

## Phase 2: Foundational

- [X] T002 [P] `PRG/util/ExerciseAiPrompt.java`: dựng lời nhắc (khối bài làm tách biệt, cắt 4.000 ký tự), đọc + kiểm tra JSON, làm tròn 0,25, cắt nhận xét
- [X] T003 [P] `PT/ExerciseAiPromptTest.java`
- [X] T004 `PRG/service/impl/ExerciseAiClient.java`: gọi Gemini / chuẩn OpenAI theo `SystemSettingService.aiChoice()`, thời gian chờ, không ghi khóa vào log
- [X] T005 `PRG/repository/ExerciseAiReviewRepository.java` (`JdbcTemplate`): quét câu cần chấm, giữ chỗ, ghi kết quả, đọc theo lượt, xóa để chấm lại, người nhận thông báo
- [X] T006 [P] `PRG/dto/ExerciseAiReviewResponse.java`; `PRG/service/ExerciseAiReviewService.java` (interface)

## Phase 3: User Story 1 — AI tự chấm sau khi nộp (P1)

- [X] T007 [US1] `ExerciseAiReviewServiceImpl.processPending()`: chọn câu (không đáp án, chưa chấm), giữ chỗ, câu trống → 0 điểm không gọi AI, gọi AI, lưu DONE / FAILED
- [X] T008 [US1] `PRG/service/impl/ExerciseAiGradingWorker.java` (`@Scheduled` 60 giây, tắt được bằng `app.exercise-ai.enabled=false`; bảng chưa có → cảnh báo một lần)
- [X] T009 [US1] Test `PT/ExerciseAiReviewServiceImplTest.java` phần xử lý nền

## Phase 4: User Story 2 — Gia sư xem gợi ý (P1)

- [X] T010 [US2] `ExerciseAiReviewServiceImpl.list` / `retry` (quyền theo `ClassroomService`, `PENDING` quá 10 phút → `FAILED`)
- [X] T011 [US2] `PRG/controller/ExerciseAiReviewController.java` (GET, POST retry)
- [X] T012 [US2] Test quyền, chấm lại trong `ExerciseAiReviewServiceImplTest`
- [X] T013 [P] [US2] `FE/js/exercise-ai-review-helpers.js` + `FE/tests/exercise-ai-review-helpers.test.mjs`
- [X] T014 [US2] Nhóm `API.exerciseAiReviews` trong `FE/js/api.js`
- [X] T015 [US2] `FE/js/exercise-ai-review.js` (chèn khối gợi ý, Áp dụng, Chấm lại, tự tải lại)
- [X] T016 [US2] `FE/pages/exercise-editor/exercise-editor.js`: thêm 1 import + 1 dòng gọi ở cuối `renderAttemptDetail`

## Phase 5: User Story 3 — Thông báo (P2)

- [X] T017 [US3] Gửi thông báo nhóm "Bài tập" khi lượt hết câu đang chấm (khóa sự kiện theo lượt) + test

## Phase 6: Polish

- [X] T018 Chạy `test-compile`, test liên quan, `node --test`; kiểm tra chữ giao diện (tiếng Việt, không in hoa, không lộ mã)
- [X] T019 Cập nhật plan.md (ghi chú triển khai), báo người phụ trách các file đã sửa và migration cần chạy
