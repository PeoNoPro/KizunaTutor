# Data Model: FE-07

Không tạo bảng, không đổi cột/kiểu/khóa/ràng buộc. Cột duy nhất mới là `assignment_submissions.file_data BYTEA NULL` — người phụ trách đã tự thêm trên DB (2026-11-03, đã kiểm tra). Repo chỉ bổ sung file migration `008` và một dòng trong `DatabasePgAdmin4.txt` để tài liệu khớp DB.

## assignments (sửa mapping `Assignment.java`)

| Cột | Kiểu | Mapping |
|---|---|---|
| assignment_id | UUID PK | `assignmentId` |
| space_id | UUID FK learning_spaces (CASCADE) | `@ManyToOne(LAZY) LearningSpace space` |
| title | VARCHAR(150) NOT NULL | 1–150 |
| description | TEXT NULL | ≤ 5000 (kiểm ở service) |
| due_date | TIMESTAMPTZ NULL | `OffsetDateTime dueDate` — null = không có hạn |
| total_points | NUMERIC(5,2) NOT NULL DEFAULT 100 | `BigDecimal totalPoints` 1–999.99 |
| created_at / updated_at | TIMESTAMPTZ | `BaseEntity` (trigger DB cập nhật updated_at) |

## assignment_submissions (sửa mapping `AssignmentSubmission.java`)

| Cột | Kiểu | Mapping / cách dùng |
|---|---|---|
| submission_id | UUID PK | `submissionId` |
| assignment_id | UUID FK | `@ManyToOne(LAZY) Assignment assignment` |
| learner_id | UUID FK | `@ManyToOne(LAZY) Learner learner` |
| attempt_number | SMALLINT > 0 | 1, 2, 3… |
| submission_content | TEXT NULL | nội dung trả lời ≤ 5000 |
| attachment_url | VARCHAR(500) NULL | tên tệp gốc đã làm sạch; null nếu không nộp tệp |
| file_data | BYTEA NULL | **không ánh xạ**; đọc/ghi bằng native query |
| ai_score, ai_feedback | | không dùng (ngoài phạm vi) |
| grade | NUMERIC(5,2) NULL | điểm |
| tutor_feedback | TEXT NULL | nhận xét |
| is_finalized | BOOLEAN | true khi đã chấm |
| status | assignment_submission_status_enum | `SUBMITTED` khi nộp, `GRADED` khi chấm |
| submitted_at | TIMESTAMPTZ | `@CreationTimestamp`, không sửa |
| graded_at | TIMESTAMPTZ NULL | thời điểm chấm |

Không kế thừa `BaseEntity` (bảng không có created_at/updated_at).

## Quy tắc trạng thái (tính trong service)

```
dueOver = dueDate != null && now > dueDate
graded  = tồn tại submission is_finalized của học viên cho bài tập
canSubmit  = !graded && !readOnly && (!dueOver || attempts == 0)
canGrade   = !graded && latest != null && (dueDate == null || dueOver) && !readOnly
canEdit    = !anyGraded && classroom manageable
onTime(s)  = dueDate == null || s.submittedAt <= dueDate
```

Nhãn hiển thị: Chưa nộp · Đúng hạn · Nộp muộn · Quá hạn – chưa nộp · Đã chấm (+ "Đã nộp lại" khi attempt ≥ 2).
