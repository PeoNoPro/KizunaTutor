-- DỮ LIỆU MẪU (chỉ dùng cho môi trường thử nghiệm / demo)
-- Mục đích: trang Tìm gia sư có dữ liệu ngay, không cần từng gia sư mua gói và đăng bài.
-- Chạy SAU migration 015_tutor_posts.sql. Chạy lại nhiều lần không tạo trùng.
--
-- Bước 1: tặng gói hội viên rẻ nhất đang mở bán cho mọi gia sư đã duyệt hồ sơ mà chưa có gói còn hạn.
-- Bước 2: tạo bài đăng ĐÃ DUYỆT từ thông tin sẵn có của gia sư (headline, bio, học phí) cho gia sư chưa có bài.

BEGIN;

-- Bước 1: gói hội viên mẫu (amount = 0, payment_gateway = 'SEED' để dễ nhận biết và xóa sau)
INSERT INTO tutor_memberships (tutor_id, plan_id, start_date, end_date, status, amount, payment_status, payment_gateway)
SELECT t.tutor_id,
       p.plan_id,
       CURRENT_DATE,
       (CURRENT_DATE + make_interval(months => p.duration_months))::date,
       'ACTIVE',
       0,
       'SUCCESS',
       'SEED'
FROM tutors t
CROSS JOIN LATERAL (
    SELECT plan_id, duration_months
    FROM membership_plans
    WHERE is_active
    ORDER BY duration_months, price
    LIMIT 1
) p
WHERE t.verification_status = 'APPROVED'
  AND NOT EXISTS (
      SELECT 1 FROM tutor_memberships m
      WHERE m.tutor_id = t.tutor_id AND m.status = 'ACTIVE' AND m.end_date >= CURRENT_DATE
  );

UPDATE tutors t
SET tutor_state = 'ACTIVE_PAID',
    membership_status = 'PREMIUM'
WHERE t.verification_status = 'APPROVED'
  AND EXISTS (
      SELECT 1 FROM tutor_memberships m
      WHERE m.tutor_id = t.tutor_id AND m.status = 'ACTIVE' AND m.end_date >= CURRENT_DATE
  );

-- Bước 2: bài đăng đã duyệt, lấy từ thông tin hồ sơ hiện có
INSERT INTO tutor_posts (tutor_id, title, content, specialties, teaching_format, hourly_rate, status, reviewed_at)
SELECT t.tutor_id,
       LEFT(COALESCE(NULLIF(TRIM(t.headline), ''), 'Gia sư tiếng Nhật ' || COALESCE(t.highest_jlpt_level, '')), 150),
       COALESCE(NULLIF(TRIM(t.bio), ''),
                'Gia sư tiếng Nhật trên KizunaTutor. Kết nối để trao đổi lộ trình học phù hợp với mục tiêu của bạn.'),
       LEFT(NULLIF(TRIM(t.headline), ''), 255),
       'ONLINE',
       COALESCE(t.hourly_rate, 200000),
       'PUBLISHED',
       now()
FROM tutors t
WHERE t.verification_status = 'APPROVED'
  AND NOT EXISTS (SELECT 1 FROM tutor_posts p WHERE p.tutor_id = t.tutor_id);

COMMIT;

-- Kiểm tra: số gia sư sẽ hiện ở trang Tìm gia sư
-- SELECT COUNT(*) FROM tutors t
-- WHERE t.verification_status = 'APPROVED'
--   AND EXISTS (SELECT 1 FROM tutor_posts p WHERE p.tutor_id = t.tutor_id AND p.status = 'PUBLISHED')
--   AND EXISTS (SELECT 1 FROM tutor_memberships m WHERE m.tutor_id = t.tutor_id AND m.status = 'ACTIVE' AND m.end_date >= CURRENT_DATE);

-- Gỡ dữ liệu mẫu (nếu cần):
-- DELETE FROM tutor_memberships WHERE payment_gateway = 'SEED';
-- DELETE FROM tutor_posts WHERE reviewed_by IS NULL AND status = 'PUBLISHED';
