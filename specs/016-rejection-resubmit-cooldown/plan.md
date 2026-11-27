# Implementation Plan: FE-16 — Nộp lại sau 7 ngày bị từ chối

**Branch**: `han` | **Date**: 2026-11-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/016-rejection-resubmit-cooldown/spec.md`

## Summary

Bỏ quy tắc "từ chối là cuối cùng" ở hồ sơ xác minh và bài đăng đầu tiên, thay bằng thời gian chờ 7 ngày tính từ lúc cán bộ học vụ từ chối. Backend tính thời điểm được nộp lại từ dữ liệu sẵn có (lịch sử thẩm định, `tutor_posts.reviewed_at`), mở khóa sửa / nộp khi hết chờ, trả trường `resubmitAvailableAt` cho trang gia sư và nêu thời điểm trong email / thông báo. Frontend hiển thị thời điểm và mở biểu mẫu khi hết chờ. Không migration, không endpoint mới.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.4); JavaScript ES6+ module.

**Primary Dependencies**: Không thêm. Dùng `TutorVerificationHistoryRepository` (đã có hàm tìm dòng mới nhất theo hành động), `TutorPostRepository`.

**Storage**: PostgreSQL Supabase dùng chung — không đổi schema.

**Testing**: JUnit 5 + Mockito: cập nhật / thêm test ở `VerificationStateMachineTest`, `TutorVerificationServiceTest`, `TutorPostServiceTest`. `node --test` cho `tutor-post-helpers` và `verification-update-helpers`.

**Target Platform**: Backend 8085, frontend Node 3000.

**Project Type**: Web application.

**Constraints**: Giao diện tiếng Việt, không in hoa, không lộ mã; giữ nguyên chức năng hiện có; chỉ sửa feature của người phụ trách (verification, tutor_post).

**Scale/Scope**: 2 feature backend, 2 trang frontend, 2 file hàm hỗ trợ frontend.

## Constitution Check

| Nguyên tắc | Đánh giá |
| --- | --- |
| Nghiệp vụ theo SRS / spec | Thay GBR-41 theo quyết định người phụ trách 2026-11-26 (spec Clarifications); SRS cần cập nhật (mục dưới) |
| Chỉ sửa phần của mình | Chỉ `features/verification`, `features/tutor_post`, trang `tutor-verification`, `tutor-post` và helper của chúng — đều do người phụ trách làm |
| Feature khác qua Service công khai | Không thêm phụ thuộc chéo (research R3) |
| Không dữ liệu giả | Thời điểm tính từ dữ liệu thật |
| Test đi cùng mã | Có |
| Hợp đồng API | Chỉ thêm 1 trường mỗi phản hồi, ghi trong [contracts](contracts/rejection-cooldown-api.md) |
| Không đổi `core/`, `application.properties`, schema | Đúng |

## Project Structure

```text
backend/src/main/java/com/kizuna/features/verification/
├── service/VerificationStateMachine.java            (hằng 7 ngày, tính thời điểm, overload isEditable / requireEditable)
├── service/VerificationException.java               (lỗi chờ nộp lại có thời điểm)
├── service/VerificationViewAssembler.java           (trả resubmitAvailableAt, editable mới, bỏ giấy tờ REJECTED khi kiểm tra)
├── service/VerificationNotificationService.java     (email từ chối nêu thời điểm)
├── service/impl/TutorVerificationServiceImpl.java   (dùng overload mới khi sửa / nộp)
├── service/impl/VerificationReviewServiceImpl.java  (thông báo trong ứng dụng khi từ chối nêu thời điểm)
└── dto/response/TutorVerificationResponse.java      (trường resubmitAvailableAt)
backend/src/main/java/com/kizuna/features/tutor_post/
├── service/TutorPostService.java                    (thời gian chờ bài đầu bị từ chối, lời nhắn, thông báo)
├── service/TutorPostMailService.java                (email từ chối nêu thời điểm / gửi bản khác ngay)
└── dto/TutorPostDtos.java                           (MyPostView.resubmitAvailableAt)
backend/src/test/java/com/kizuna/features/verification/VerificationStateMachineTest.java
backend/src/test/java/com/kizuna/features/verification/TutorVerificationServiceTest.java
backend/src/test/java/com/kizuna/features/tutor_post/TutorPostServiceTest.java
frontend/js/tutor-post-helpers.js                    (isUpdateMode chỉ khi có bản đã duyệt; lời nhắc chờ)
frontend/js/verification-update-helpers.js           (lời nhắc hồ sơ bị từ chối)
frontend/pages/tutor-post/tutor-post.js
frontend/pages/tutor-verification/tutor-verification.js
frontend/tests/tutor-post-helpers.test.mjs
frontend/tests/verification-update-helpers.test.mjs
```

## Ảnh hưởng tới SRS (người phụ trách tự sửa)

| Chỗ | Sửa thành |
| --- | --- |
| GBR-41 | A rejected profile, or a rejected first post, can be resubmitted 7 days after the rejection. A rejected update of an approved post can be resubmitted at once. |
| UC 3.2 PRE-2 | The profile is "Not submitted", "Needs more information", or "Rejected" and 7 days have passed since the rejection. |
| UC 3.2 AF-3 | If it was rejected less than 7 days ago, the form is read-only and the system shows when the tutor can resubmit (MSG-164). |
| UC 3.3 PRE-2 | Như UC 3.2 PRE-2. |
| UC 3.11 POST-2 | The tutor can edit and resubmit a rejected profile 7 days after the rejection, with new documents. |
| UC 4.3 PRE-2 | Thêm: "The post is "Rejected", has no approved version, and 7 days have passed since the rejection." |
| UC 4.3 AF-2 | Rejected, no approved version, less than 7 days: editing is locked and the system shows when the tutor can send a new post (MSG-165). |
| UC 4.5 POST-2 | A rejected first post can be replaced by a new post 7 days after the rejection. |
| BF-01, BF-02 | Nhánh "Reject" vẫn kết thúc luồng; ghi chú "tutor may resubmit after 7 days". |
| System Messages | MSG-164: "Your profile was rejected. You can edit and resubmit it from [time]." · MSG-165: "Your post was rejected. You can send a new post from [time]." |

## Ghi chú triển khai

2026-11-26:

- Backend: chỉ sửa `features/verification` (8 file) và `features/tutor_post` (3 file) như Project Structure; không đổi constructor nào, giữ các hàm cũ (`isEditable(status)`, `requireEditable(tutor)`, `submit(tutor)`, email 4 tham số) và thêm overload. Không migration, không sửa `core/`, `application.properties`.
- Frontend: helper `formatResubmitTime`, `rejectedFirstPostHint` (bài đăng), `rejectedProfileHint`, `rejectedReadOnlyNote` (hồ sơ); `isUpdateMode` chỉ coi bài bị từ chối là "bản cập nhật" khi còn bản đã duyệt.
- Kiểm tra:
  - Test liên quan (`VerificationStateMachineTest`, `TutorVerificationServiceTest`, `VerificationReviewServiceTest`, `VerificationSummaryTest`, `TutorPostServiceTest`, `TutorPostMailServiceTest`, `TutorPostSummaryTest`): 209 test, 0 failure, 0 error.
  - Toàn bộ backend: 1390 test, 0 failure, 72 error — đúng 72 lỗi môi trường đã có trước (thiếu `OPERATIONS_TEST_DB_URL` / `TICKET_TEST_DB_URL`, đường dẫn Excel máy khác, parser isolation).
  - Frontend `node --test "frontend/tests/*.test.mjs"`: 105/106; test đỏ duy nhất là `learning-tools-access` (thiếu `/learner/practice-history` trong `LOCKED_PATHS`, lỗi cũ của dev, không thuộc feature này).
- Chưa xác minh: mở hai trang trên trình duyệt với dữ liệu thật (T019) — làm theo [quickstart.md](quickstart.md) mục 2.

## Complexity Tracking

| Lệch | Vì sao | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Hằng 7 ngày lặp ở 2 feature | Giữ verification và tutor_post độc lập (luật 3) | Dùng chung hằng tạo phụ thuộc từ tutor_post sang nội bộ verification |
