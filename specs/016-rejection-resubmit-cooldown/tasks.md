---
description: "Danh sách công việc triển khai FE-16 — Nộp lại sau 7 ngày bị từ chối"
---

# Tasks: FE-16 — Nộp lại sau 7 ngày bị từ chối

**Input**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/rejection-cooldown-api.md](contracts/rejection-cooldown-api.md), [quickstart.md](quickstart.md).

**Tests**: Có. Unit test không chạm DB.

## Format: `[ID] [P?] [Story] Description`

- Viết tắt: `VER` = `backend/src/main/java/com/kizuna/features/verification`, `POST` = `backend/src/main/java/com/kizuna/features/tutor_post`, `VT` / `PT` = thư mục test tương ứng, `FE` = `frontend`.

---

## Phase 1: Setup

- [x] T001 Tạo thư mục đặc tả, trỏ `.specify/feature.json` tới `specs/016-rejection-resubmit-cooldown`

## Phase 2: Foundational

- [x] T002 `VER/service/VerificationStateMachine.java`: hằng `RESUBMIT_COOLDOWN` (7 ngày), `resubmitAvailableAt(...)` từ lần từ chối mới nhất, overload `isEditable(status, availableAt, now)` / `requireEditable(tutor, availableAt, now)`; giữ hàm cũ
- [x] T003 `VER/service/VerificationException.java`: `rejectedUntil(availableAt)` — mã `VERIFICATION_REJECTED`, lời nhắn có thời điểm

## Phase 3: User Story 1 — Nộp lại hồ sơ xác minh (P1)

- [x] T004 [US1] `VER/dto/response/TutorVerificationResponse.java`: trường `resubmitAvailableAt`
- [x] T005 [US1] `VER/service/VerificationViewAssembler.java`: trả `resubmitAvailableAt`, `editable` theo thời gian chờ; `check` bỏ giấy tờ `REJECTED`
- [x] T006 [US1] `VER/service/impl/TutorVerificationServiceImpl.java`: sửa / giấy tờ / nộp dùng overload mới
- [x] T007 [P] [US1] `VT/VerificationStateMachineTest.java`: cập nhật test "từ chối là cuối cùng" → trong thời gian chờ khóa, đúng mốc 7 ngày mở, không có lịch sử mở
- [x] T008 [P] [US1] `VT/TutorVerificationServiceTest.java`: nộp lại trước / sau 7 ngày; giấy tờ bị từ chối không tính
- [x] T009 [US1] `FE/js/verification-update-helpers.js` + `FE/tests/verification-update-helpers.test.mjs`: lời nhắc hồ sơ bị từ chối theo thời điểm
- [x] T010 [US1] `FE/pages/tutor-verification/tutor-verification.js`: lời nhắc trạng thái, ghi chú chỉ xem, nút "Nộp lại hồ sơ"

## Phase 4: User Story 2 — Gửi bài đăng mới sau 7 ngày (P1)

- [x] T011 [US2] `POST/dto/TutorPostDtos.java`: `MyPostView.resubmitAvailableAt`
- [x] T012 [US2] `POST/service/TutorPostService.java`: `isEditable` theo thời gian chờ, `resubmitAvailableAt`, lời nhắn 409 có thời điểm
- [x] T013 [P] [US2] `PT/TutorPostServiceTest.java`: cập nhật test bài đầu bị từ chối; thêm trước / đúng mốc / không có `reviewedAt`; bản cập nhật bị từ chối vẫn gửi ngay
- [x] T014 [US2] `FE/js/tutor-post-helpers.js` + test: `isUpdateMode` chỉ khi có bản đã duyệt; lời nhắc chờ gửi bài mới
- [x] T015 [US2] `FE/pages/tutor-post/tutor-post.js`: khung trạng thái nêu thời điểm / mở biểu mẫu

## Phase 5: User Story 3 — Email và thông báo (P2)

- [x] T016 [US3] `VER/service/VerificationNotificationService.java` + `VER/service/impl/VerificationReviewServiceImpl.java`: email / thông báo từ chối nêu thời điểm
- [x] T017 [US3] `POST/service/TutorPostMailService.java` + `TutorPostService.reject`: email / thông báo nêu thời điểm hoặc "gửi bản khác ngay"

## Phase 6: Polish

- [x] T018 Chạy `test-compile`, test liên quan, toàn bộ test frontend; ghi kết quả vào plan.md "Ghi chú triển khai"
- [ ] T019 Mở hai trang qua `node frontend/server.js` kiểm tra chữ, trạng thái (theo quickstart)
