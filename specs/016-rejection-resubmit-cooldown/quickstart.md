# Quickstart: FE-16 — Nộp lại sau 3 ngày bị từ chối

## 1. Kiểm tra tự động

```powershell
cd backend
.\mvnw.cmd test "-Dtest=VerificationStateMachineTest,TutorVerificationServiceTest,VerificationReviewServiceTest,TutorPostServiceTest"
cd ..
node --test "frontend/tests/*.test.mjs"
```

## 2. Kiểm tra thủ công (backend 8085, frontend 3000)

Không có DB test; để thử thời gian chờ mà không đợi 3 ngày, dùng một tài khoản gia sư thử và **chỉ trên dữ liệu thử** lùi mốc từ chối bằng DBeaver (người phụ trách tự chạy):

```sql
-- Hồ sơ: lùi dòng từ chối mới nhất của gia sư thử 8 ngày
UPDATE tutor_verification_histories SET created_at = created_at - INTERVAL '8 days'
WHERE history_id = '<history_id dòng REJECT mới nhất>';
-- Bài đăng: lùi thời điểm từ chối 8 ngày
UPDATE tutor_posts SET reviewed_at = reviewed_at - INTERVAL '8 days' WHERE post_id = '<post_id bị từ chối>';
```

### Hồ sơ xác minh
1. Cán bộ học vụ từ chối hồ sơ gia sư thử → gia sư nhận email / thông báo có "từ HH:mm ngày dd/MM/yyyy".
2. Gia sư mở `/tutor/verification`: trạng thái "Bị từ chối", lời nhắc có thời điểm, chỉ xem.
3. Lùi mốc 8 ngày (SQL trên), tải lại: biểu mẫu mở, nút "Nộp lại hồ sơ", mục "còn thiếu" nêu giấy tờ.
4. Nộp khi chưa tải giấy tờ mới → báo thiếu giấy tờ. Tải giấy tờ mới → nộp được → "Đang chờ thẩm định"; cán bộ học vụ thấy "Hồ sơ gia sư được nộp lại".

### Bài đăng
1. Gia sư có gói còn hạn, gửi bài đầu → cán bộ học vụ từ chối → `/tutor/post` hiện lý do và "Bạn có thể gửi bài đăng mới từ ...", chỉ xem.
2. Lùi mốc 8 ngày, tải lại: biểu mẫu mở với nội dung cũ, nút "Gửi lại"; gửi → bài mới "Đang chờ duyệt".
3. Gia sư có bài đã duyệt, gửi bản cập nhật, bị từ chối → vẫn sửa và gửi được ngay (như trước).

Kiểm tra chữ: tiếng Việt, không in hoa toàn bộ, không lộ mã.
