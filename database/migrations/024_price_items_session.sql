-- Bài đăng gia sư: mỗi môn học / cấp độ trong bảng học phí có THỜI LƯỢNG MỘT BUỔI và GIÁ MỘT BUỔI.
-- Phần tử mới của tutor_posts.price_items:
--   {"level": "JLPT_N5", "sessionMinutes": 90, "pricePerSession": 300000, "hourlyRate": 200000}
--   sessionMinutes  : 60 | 90 | 120 (gia sư chọn)
--   pricePerSession : học phí một buổi (gia sư nhập)
--   hourlyRate      : pricePerSession × 60 / sessionMinutes (tự tính; trang Tìm gia sư lọc / sắp xếp theo giá này)
-- Dữ liệu cũ chỉ có hourlyRate: gán thời lượng mặc định 90 phút, giá một buổi = hourlyRate × 1,5
-- (giá theo giờ giữ nguyên, nên kết quả lọc giá trên trang Tìm gia sư không đổi). Gia sư tự sửa lại khi cập nhật bài.
-- Chạy SAU 017_tutor_post_price_items.sql.
-- Idempotent: phần tử đã có sessionMinutes thì bỏ qua; chạy lại nhiều lần không đổi dữ liệu.

UPDATE tutor_posts p
SET price_items = (
    SELECT jsonb_agg(
               CASE WHEN e.item ? 'sessionMinutes' THEN e.item
                    ELSE e.item || jsonb_build_object(
                            'sessionMinutes', 90,
                            'pricePerSession', round((e.item ->> 'hourlyRate')::numeric * 1.5))
               END
               ORDER BY e.ord)
    FROM jsonb_array_elements(p.price_items) WITH ORDINALITY AS e(item, ord)
)
WHERE p.price_items IS NOT NULL
  AND jsonb_typeof(p.price_items) = 'array'
  AND jsonb_array_length(p.price_items) > 0
  AND EXISTS (SELECT 1 FROM jsonb_array_elements(p.price_items) x(item) WHERE NOT (x.item ? 'sessionMinutes'));
