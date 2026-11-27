-- Bài đăng gia sư: thêm trạng thái "Cần bổ sung" (cán bộ học vụ yêu cầu gia sư sửa và gửi lại).
-- Nội dung yêu cầu lưu ở cột rejection_reason (dùng chung cho lý do từ chối).
-- Chạy SAU 015_tutor_posts.sql. Người phụ trách tự chạy trên Supabase bằng DBeaver.
-- Idempotent: chạy lại nhiều lần không gây lỗi.

ALTER TYPE tutor_post_status_enum ADD VALUE IF NOT EXISTS 'NEEDS_REVISION';
