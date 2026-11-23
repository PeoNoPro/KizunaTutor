# Data Model: FE-15 — AI gợi ý chấm câu tự luận

## Bảng mới `exercise_ai_reviews` (migration `database/migrations/036_exercise_ai_reviews.sql`)

| Cột | Kiểu | Ràng buộc | Ý nghĩa |
| --- | --- | --- | --- |
| `review_id` | UUID | PK, mặc định `gen_random_uuid()` | |
| `attempt_id` | UUID | NOT NULL, FK `exercise_attempts(attempt_id)` ON DELETE CASCADE | Lượt làm bài |
| `question_id` | UUID | NOT NULL, FK `exercise_questions(question_id)` ON DELETE CASCADE | Câu hỏi |
| `status` | VARCHAR(10) | NOT NULL, mặc định `PENDING`, CHECK `PENDING`/`DONE`/`FAILED` | Đang chấm / có gợi ý / chưa chấm được |
| `suggested_score` | NUMERIC(6,2) | NULL, CHECK ≥ 0 | Điểm gợi ý (chỉ khi `DONE`) |
| `feedback` | TEXT | NULL | Nhận xét gợi ý (≤ 1.000 ký tự) |
| `model` | VARCHAR(100) | NULL | Model đã dùng (`NULL` khi không gọi AI, ví dụ câu bỏ trống) |
| `error_message` | VARCHAR(300) | NULL | Lỗi nội bộ ngắn (không hiện cho người dùng) |
| `created_at` | TIMESTAMPTZ | NOT NULL, mặc định now | Thời điểm giữ chỗ |
| `updated_at` | TIMESTAMPTZ | NOT NULL, mặc định now | Thời điểm có kết quả |

- `UNIQUE (attempt_id, question_id)` — giữ chỗ: `INSERT ... ON CONFLICT DO NOTHING`, máy nào chèn được dòng thì máy đó gọi AI (FR-002). Chỉ mục của ràng buộc này cũng phục vụ truy vấn theo lượt.
- Không thêm cột vào bảng của FE-07.

### Vòng đời

```
(chưa có dòng) --giữ chỗ--> PENDING --AI hợp lệ / câu trống--> DONE
                                    --AI lỗi / sai định dạng / chưa cấu hình--> FAILED
FAILED, PENDING quá 10 phút --gia sư "Chấm lại bằng AI" (xóa dòng)--> (chưa có dòng)
```

Nhãn giao diện: `PENDING` → "AI đang chấm", `DONE` → "AI đã gợi ý", `FAILED` (hoặc `PENDING` quá 10 phút) → "AI chưa chấm được".

## Dữ liệu đọc (không sửa) của FE-07 / FE-06

- `exercise_attempts`: `status = 'SUBMITTED'`, `submitted_at` trong 7 ngày.
- `exercise_answers`: `response` (JSON `{ "text": "..." }`), `tutor_score IS NULL`.
- `exercise_questions`: `question_type IN ('WRITTEN', 'FILL_BLANK')`, `data.accepted` / `data.answers` (đáp án tham khảo), `data.text` (câu có chỗ trống), `data.sample` (tùy chọn), `prompt`, `points`.
- `online_exercises`: `title`, `instructions`, `space_id`.
- `learning_spaces` → `tutors` → `users` (người nhận thông báo); `learners` → `users` (tên học viên trong thông báo).
