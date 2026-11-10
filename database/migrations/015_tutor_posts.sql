-- Bài đăng gia sư (FE-09 US6): bổ sung trường cho bảng tutor_posts đã tạo ngày 2026-11-04
-- (xem specs/009-membership-payment/data-model.md §6).
-- Bảng gốc có: title, content, status (PENDING_REVIEW | PUBLISHED | REJECTED), rejection_reason, reviewed_by, reviewed_at.
-- Mỗi lần gia sư gửi bài là một dòng; bài hiển thị cho học viên là dòng PUBLISHED mới nhất của gia sư.
-- Người phụ trách tự chạy trên Supabase bằng DBeaver.
-- Idempotent: chạy lại nhiều lần không gây lỗi, không xóa dữ liệu.

ALTER TABLE tutor_posts ADD COLUMN IF NOT EXISTS specialties     VARCHAR(255);
ALTER TABLE tutor_posts ADD COLUMN IF NOT EXISTS teaching_format VARCHAR(20);
ALTER TABLE tutor_posts ADD COLUMN IF NOT EXISTS hourly_rate     NUMERIC(12, 2);

DO $$ BEGIN
  ALTER TABLE tutor_posts ADD CONSTRAINT chk_tutor_posts_teaching_format
    CHECK (teaching_format IS NULL OR teaching_format IN ('ONLINE', 'OFFLINE', 'BOTH'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- Tìm nhanh bài đang hiển thị của từng gia sư
CREATE INDEX IF NOT EXISTS idx_tutor_posts_tutor_status ON tutor_posts (tutor_id, status, reviewed_at DESC);
