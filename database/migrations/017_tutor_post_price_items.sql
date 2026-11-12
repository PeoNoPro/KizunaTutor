-- Bài đăng gia sư: bảng học phí theo môn học / cấp độ.
-- Mỗi phần tử: {"level": "JLPT_N3", "hourlyRate": 200000}; level thuộc JLPT_N5..JLPT_N1, KAIWA, BUSINESS, EJU.
-- hourly_rate (đã có từ 015) lưu mức thấp nhất để trang Tìm gia sư lọc / sắp xếp và hiển thị "Từ ...".
-- Bài cũ chưa có bảng giá thì để NULL (entity TutorPost.priceItems cho phép null).
-- Chạy SAU 015_tutor_posts.sql và 016_tutor_posts_needs_revision.sql. Người phụ trách tự chạy trên Supabase bằng DBeaver.
-- Idempotent: chạy lại nhiều lần không gây lỗi, không đổi dữ liệu đang có.

ALTER TABLE tutor_posts ADD COLUMN IF NOT EXISTS price_items JSONB;
