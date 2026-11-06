# Implementation Plan: FE-07 — Homework, Assessment & Progress Tracking

**Branch**: `linhtk` | **Date**: 2026-11-03 | **Spec**: [spec.md](spec.md)

## Summary

API bài tập và bài nộp trong `com.kizuna.features.progress`, dùng lại phân quyền lớp của FE-06 (`ClassroomService`, `CurrentUserResolver`) và kiểm tra tệp (`MaterialFileValidator`). Tệp bài nộp lưu trong `assignment_submissions.file_data`. Giao diện: thêm khối "Bài tập" vào trang chi tiết lớp của gia sư (`tutor-classrooms`) và học viên (`my-courses`).

## Technical Context

Java 17 / Spring Boot 3.3.4 / Spring Data JPA / PostgreSQL (Supabase); frontend HTML + ES modules + Tailwind CDN. Không thêm dependency. Test: JUnit 5 + Mockito; `node --test "frontend/tests/*.test.js"`.

## Constitution Check

| Nguyên tắc | Đánh giá |
|---|---|
| I. Tiếng Việt | Message backend/UI tiếng Việt; mã trạng thái chỉ dùng nội bộ, UI ánh xạ nhãn. ✅ |
| II. Dữ liệu thật | Không seed; trạng thái rỗng/lỗi. ✅ |
| III. Kiến trúc | Package-by-feature `progress`; gọi sang `classroom` qua `ClassroomService` (interface) + dùng lại util FE-06 (cùng người phụ trách). Schema: chỉ cột đã được duyệt. ✅ |
| IV. Thiết kế | Dùng lại style retro §7 đã có trong hai trang FE-06. ✅ |
| V. Kiểm chứng | Unit test toàn bộ tổ hợp quy tắc nộp/chấm/sửa. ✅ |

## Files

### Tạo mới (backend `features/progress/`)
```
controller/AssignmentController.java
dto/AssignmentRequest.java  AssignmentSummaryResponse.java  AssignmentDetailResponse.java
dto/SubmissionResponse.java  GradeRequest.java  SubmissionFileContent.java
exception/AssignmentExceptionHandler.java      (@RestControllerAdvice(assignableTypes = AssignmentController.class), xử lý ClassroomException)
repository/AssignmentRepository.java  AssignmentSubmissionRepository.java (+ native query file_data)
service/AssignmentService.java  SubmissionService.java
service/impl/AssignmentServiceImpl.java  SubmissionServiceImpl.java
util/AssignmentRules.java                     (hàm thuần: dueOver, canSubmit, canGrade, onTime, state)
```
Test: `backend/src/test/java/com/kizuna/features/progress/{AssignmentRulesTest, AssignmentServiceImplTest, SubmissionServiceImplTest}.java`
Frontend: `frontend/js/assignment-helpers.js`, `frontend/tests/assignment-helpers.test.js`
DB doc: `database/migrations/008_fe07_assignment_submissions_file_data.sql`

### Sửa
| File | Thay đổi | Lý do |
|---|---|---|
| `progress/entity/Assignment.java`, `AssignmentSubmission.java` | Viết lại mapping đúng bảng | Stub ánh xạ sai, thuộc FE-07 |
| `database/DatabasePgAdmin4.txt` | +1 dòng `file_data BYTEA,` trong `assignment_submissions` | Đồng bộ tài liệu với DB |
| `frontend/js/api.js` | Append `API.assignments` | Q5 đồng ý |
| `frontend/server.js` | Proxy `backendPort` 8080 → 8085 | Q6 đồng ý |
| `frontend/pages/tutor-classrooms/*` | Thêm khối Bài tập + hộp thoại tạo/sửa + xem bài nộp/chấm | Code FE-06 của người phụ trách |
| `frontend/pages/my-courses/*` | Thêm khối Bài tập + hộp thoại chi tiết/nộp bài | Code FE-06 của người phụ trách |
| `.gitkeep` trong các thư mục progress được thêm file | Xóa | Dọn dẹp |

Không sửa: `AssignmentKanjiItem`, `LearningProgressRecord`, `AssignmentSubmissionStatus`, `core/`, `pom.xml`, `SecurityConfig`, module khác.

## Design Notes

1. **Đồng hồ**: service nhận `java.time.Clock` (mặc định `Clock.systemDefaultZone()`, constructor riêng cho test) để test quy tắc hạn nộp.
2. **Giao dịch**: nộp bài = `save` + `updateFileData` trong một `@Transactional` (như FE-06). Chấm điểm khóa theo `is_finalized`; kiểm tra "lần nộp mới nhất" bằng `max(attempt_number)` trong cùng giao dịch.
3. **Đồng thời**: hai lần nộp gần như cùng lúc có thể trùng `attempt_number` (DB không có unique). Chấp nhận rủi ro thấp (mỗi lớp 1 học viên); lấy `max+1` trong transaction.
4. **Tên tệp**: làm sạch giống FE-06 (bỏ đường dẫn/ký tự cấm, ≤ 200 + đuôi đã xác minh).
5. **Lỗi**: dùng `ClassroomException` (đã có) để thông điệp nhất quán; advice riêng gắn với `AssignmentController`.
