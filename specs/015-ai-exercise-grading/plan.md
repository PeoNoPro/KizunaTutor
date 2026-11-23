# Implementation Plan: FE-15 — AI gợi ý chấm câu tự luận của bài làm trực tiếp

**Branch**: `han` | **Date**: 2026-11-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/015-ai-exercise-grading/spec.md`

## Summary

Sau khi học viên nộp lượt làm bài trực tiếp (FE-07), bộ xử lý nền tìm các câu tự luận gia sư không nhập đáp án, gửi từng câu cho AI (cấu hình AI của Admin), kiểm tra JSON trả về và lưu **gợi ý** điểm + nhận xét vào bảng mới `exercise_ai_reviews`; xong một lượt thì báo gia sư (FE-12). Gia sư xem gợi ý ở tab "Bài làm của học viên", bấm "Áp dụng gợi ý" để điền ô điểm / nhận xét, rồi lưu bằng chức năng chấm có sẵn của FE-07. Mã mới nằm trong `com.kizuna.features.progress` (mở rộng FE-07) dưới dạng file mới; theo yêu cầu bổ sung 2026-11-19, logic chấm FE-07 được sửa tối thiểu để lượt có câu điền khuyết / tự luận chờ gia sư duyệt.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.4); JavaScript ES6+ module.

**Primary Dependencies**: Spring Web, Spring JDBC (`JdbcTemplate`), Spring Scheduling (đã bật), Jackson, `RestTemplate` (đã có). Dịch vụ công khai dùng lại: `ClassroomService` (quyền lớp), `SystemSettingService.aiChoice()` (cấu hình AI), `NotificationEvents` (thông báo), `ExerciseQuestionEngine.needsManualGrading` (nhận biết câu không đáp án). Không thêm thư viện.

**Storage**: PostgreSQL Supabase dùng chung. Bảng mới `exercise_ai_reviews` ([data-model.md](data-model.md)); người phụ trách tự chạy migration `036` bằng DBeaver.

**Testing**: JUnit 5 + Mockito + AssertJ: `ExerciseAiPromptTest` (lời nhắc, kiểm tra JSON), `ExerciseAiReviewServiceImplTest` (chọn câu, giữ chỗ, AI lỗi, thông báo, quyền, chấm lại). `node --test` cho `frontend/js/exercise-ai-review-helpers.js`. Không test chạm DB (không có DB test).

**Target Platform**: Backend 8085, frontend Node 3000.

**Project Type**: Web application.

**Performance Goals**: Gợi ý có trong ≤ 2 phút sau khi nộp (SC-001). Truy vấn quét mỗi phút, tối đa 20 câu/lần.

**Constraints**: AI chỉ gợi ý (AGENTS luật 8); không ghi đè điểm gia sư (BACKEND_RULES 8.3); không lộ khóa AI vào log; giao diện tiếng Việt, không in hoa.

**Scale/Scope**: 1 bảng, 2 endpoint, 1 bộ xử lý nền, 1 module giao diện chèn vào trang FE-07.

## Constitution Check

| Nguyên tắc | Đánh giá |
| --- | --- |
| Nghiệp vụ theo SRS / spec | Dựa UC-67 + luật 8 + yêu cầu người phụ trách; ghi rõ ở spec |
| Chỉ sửa phần của mình | Backend FE-07 không đổi; frontend FE-07 thêm 2 dòng (Complexity Tracking) |
| Service interface + Impl, controller mỏng | `ExerciseAiReviewService` / `ExerciseAiReviewServiceImpl`, `ExerciseAiReviewController` |
| Feature khác qua Service công khai | `ClassroomService`, `SystemSettingService`, `NotificationEvents` |
| Không dữ liệu giả | Chỉ dữ liệu thật; chưa có gợi ý thì hiện trạng thái tương ứng |
| Test đi cùng mã | Có (mục Testing) |
| Bí mật | Khóa AI chỉ đọc từ cấu hình, gửi qua header, không ghi log |

## Project Structure

```text
database/migrations/036_exercise_ai_reviews.sql                       (mới)
backend/src/main/java/com/kizuna/features/progress/
├── controller/ExerciseAiReviewController.java                         (mới)
├── dto/ExerciseAiReviewResponse.java                                  (mới)
├── repository/ExerciseAiReviewRepository.java                         (mới, JdbcTemplate)
├── service/ExerciseAiReviewService.java                               (mới, interface)
├── service/impl/ExerciseAiReviewServiceImpl.java                      (mới)
├── service/impl/ExerciseAiGradingWorker.java                          (mới, @Scheduled)
├── service/impl/ExerciseAiClient.java                                 (mới, gọi AI)
└── util/ExerciseAiPrompt.java                                         (mới, lời nhắc + kiểm tra JSON)
backend/src/test/java/com/kizuna/features/progress/
├── ExerciseAiPromptTest.java                                          (mới)
└── ExerciseAiReviewServiceImplTest.java                               (mới)
frontend/js/exercise-ai-review-helpers.js                              (mới)
frontend/js/exercise-ai-review.js                                      (mới)
frontend/js/api.js                                                     (thêm nhóm API.exerciseAiReviews)
frontend/pages/exercise-editor/exercise-editor.js                      (thêm 1 import + 1 dòng gọi)
frontend/tests/exercise-ai-review-helpers.test.mjs                     (mới)
```

## Ghi chú triển khai (2026-11-19)

- Backend: 8 file mới trong `features/progress` (xem Project Structure); không sửa file FE-07 nào. Bộ xử lý nền tắt được bằng `app.exercise-ai.enabled=false`, đổi chu kỳ bằng `app.exercise-ai.poll-ms` (mặc định 60000) — chỉ đọc giá trị mặc định trong mã, không thêm vào `application.properties`.
- Frontend: thêm khối `API.exerciseAiReviews` cuối phần FE-07 trong `api.js`; `exercise-editor.js` thêm 1 import + 1 dòng gọi (có chú thích "FE-15 (Hân)"). Trang tự tải lại tối đa 20 lần (≈ 5 phút) khi còn câu đang chờ; lượt nộp quá 7 ngày không hiện "AI sẽ chấm".
- Kiểm tra: `ExerciseAiPromptTest` 17, `ExerciseAiReviewServiceImplTest` 22 test xanh; toàn bộ backend 1076 test, 0 failure, 105 error cũ (operations / support cần DB test); frontend 130/130.
- Chạy thật với Supabase khi **chưa** chạy migration 036: backend khởi động bình thường, bộ xử lý nền cảnh báo một lần; API trả 503 "Chức năng AI chấm bài chưa sẵn sàng..."; tài khoản không phải gia sư của lớp bị 403. Câu SQL quét / người nhận thông báo đã chạy thử (chỉ đọc) trên DB thật.
- Bổ sung lần 2 (2026-11-19): câu điền khuyết + tự luận có đáp án cũng qua AI / chờ duyệt; 1080 test backend, 0 failure, 105 error cũ; frontend 131/131.
- Bổ sung lần 3 (2026-11-20, merge dev có PR #27–#30): lấy nguyên code FE-07 của Linh; FE-15 chọn câu theo `awaitsReview` của Linh (thêm cột `correct` vào truy vấn quét); `exercise-editor.js` (bản mới của Linh) chỉ thêm lại 1 import + 1 dòng gọi; migration đổi tên `032_exercise_ai_reviews.sql` → `036_exercise_ai_reviews.sql` (trùng số với `032_practice_exam_mode_only.sql` của nhóm).
- Chưa xác minh: luồng có AI thật (cần chạy migration 036 và có khóa AI) — làm theo [quickstart.md](quickstart.md) mục 2.

## Complexity Tracking

| Lệch | Vì sao | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| ~~Sửa `OnlineExerciseServiceImpl` / `ExerciseQuestionEngine` / test FE-07 / `exercise-take.js`~~ — **đã bỏ 2026-11-20**: sau khi merge PR #29, #30 lấy nguyên code của Linh, FE-15 dùng quy tắc chờ chấm của Linh | Người phụ trách yêu cầu câu điền khuyết / tự luận luôn chờ gia sư duyệt (kể cả bài "Tự động chấm") và đồng ý sửa code FE-07 | Chỉ thêm file mới không chặn được việc FE-07 chốt điểm ngay khi nộp |
| Thêm 2 dòng vào `exercise-editor.js` (FE-07, của Linh) | Khối gợi ý phải nằm trong màn chấm của FE-07 | Trang riêng cho gợi ý buộc gia sư chuyển qua lại hai màn khi chấm |
| Cấu hình AI đọc từ `SystemSettingService.aiChoice()` thay vì `app.ai.*` (BACKEND_RULES 8.5) | Hệ thống đã quản lý AI ở trang Admin (FE-11), FE-08 cũng dùng nguồn này | Đọc `app.ai.*` sẽ có hai nơi cấu hình AI khác nhau |
| Quét định kỳ thay vì sự kiện sau commit | Không phải sửa `OnlineExerciseServiceImpl` (FE-07) | Sự kiện sau commit cần thêm mã vào 3 điểm nộp bài của FE-07 |
| Lớp gọi AI mới thay vì dùng `AiWritingServiceImpl` | Lớp đó là của FE-08, các hàm gọi AI là `private` | Đổi phạm vi truy cập / tách lớp ở FE-08 là sửa mã người khác |
