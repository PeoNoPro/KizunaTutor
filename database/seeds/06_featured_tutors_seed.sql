-- ====================================================================================================
-- DỮ LIỆU MẪU: 6 GIA SƯ TIÊU BIỂU HIỂN THỊ TRÊN TRANG CHỦ (chỉ dùng cho môi trường thử nghiệm / demo)
-- Mỗi gia sư: hồ sơ ĐÃ XÁC MINH (APPROVED) + chứng chỉ JLPT đã duyệt, gói hội viên 12 tháng còn hạn,
-- bài đăng ĐÃ DUYỆT (PUBLISHED) kèm bảng giá, lịch rảnh hằng tuần, is_featured = true.
-- Chưa có học viên, chưa có đánh giá, chưa có lịch học thử / kết nối / lớp học.
-- Mật khẩu mặc định: 123456. Email đuôi @kizuna.vn, payment_gateway = 'SEED' để dễ nhận biết và xóa sau.
-- Chạy lại nhiều lần không tạo trùng (ON CONFLICT / NOT EXISTS).
-- ====================================================================================================

BEGIN;

-- 1. Tài khoản + hồ sơ gia sư
WITH src (n, email, full_name, phone, headline, bio, exp, rate, jlpt, address, dob) AS (VALUES
  (1, 'tanaka.yuki@kizuna.vn',   'Tanaka Yuki',      '0901110001', 'Kaiwa, Phát âm chuẩn Tokyo',
      'Giáo viên bản xứ Tokyo, 6 năm dạy hội thoại cho người Việt. Tập trung phản xạ nói, ngữ điệu tự nhiên và cách dùng kính ngữ trong đời sống.',
      6, 250000, 'N1', 'Hà Nội - Cầu Giấy', DATE '1991-03-12'),
  (2, 'tran.minh.chau@kizuna.vn','Trần Minh Châu',   '0901110002', 'Luyện thi JLPT N3, N2',
      'Thạc sĩ Ngôn ngữ Nhật, từng giảng dạy tại trung tâm Nhật ngữ 5 năm. Lộ trình luyện đề theo từng phần thi, chữa bài chi tiết sau mỗi buổi.',
      5, 200000, 'N1', 'TP. Hồ Chí Minh - Quận 3', DATE '1993-08-21'),
  (3, 'le.hoang.long@kizuna.vn', 'Lê Hoàng Long',    '0901110003', 'Tiếng Nhật IT, Phỏng vấn kỹ sư',
      'Kỹ sư cầu nối (BrSE) 7 năm tại công ty Nhật. Dạy tiếng Nhật chuyên ngành IT, viết email công việc và luyện phỏng vấn xin việc.',
      7, 300000, 'N1', 'Đà Nẵng - Thanh Khê', DATE '1990-11-02'),
  (4, 'suzuki.aiko@kizuna.vn',   'Suzuki Aiko',      '0901110004', 'Nhập môn N5, N4 cho người mới',
      'Giáo viên người Nhật, kiên nhẫn với người mới bắt đầu. Học Hiragana, Katakana và ngữ pháp nền tảng qua tình huống thực tế, vui và dễ nhớ.',
      4, 180000, 'N1', 'Hà Nội - Đống Đa', DATE '1995-06-18'),
  (5, 'pham.thu.trang@kizuna.vn','Phạm Thu Trang',   '0901110005', 'Kanji, Đọc hiểu N2',
      'Du học sinh Đại học Osaka. Phương pháp học Kanji theo bộ thủ và âm Hán Việt, luyện đọc hiểu bài dài với chiến lược làm bài trong thời gian ngắn.',
      4, 220000, 'N1', 'Hải Phòng - Lê Chân', DATE '1996-01-27'),
  (6, 'nguyen.duc.anh@kizuna.vn','Nguyễn Đức Anh',   '0901110006', 'Tiếng Nhật Thương Mại, Kính ngữ',
      '8 năm làm việc tại Nagoya. Dạy tiếng Nhật thương mại: báo cáo, họp, đàm phán và kính ngữ (keigo) cho người đi làm.',
      8, 280000, 'N1', 'TP. Hồ Chí Minh - Bình Thạnh', DATE '1988-09-09')
),
u AS (
  INSERT INTO users (role_id, email, password_hash, full_name, phone, account_status, must_change_password)
  SELECT 5, email, '$2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq', full_name, phone, 'ACTIVE', false
  FROM src
  ON CONFLICT (email) DO NOTHING
  RETURNING user_id, email
)
INSERT INTO tutors (user_id, headline, bio, experience_years, hourly_rate, highest_jlpt_level,
                    verification_status, membership_status, is_featured, tutor_state,
                    phone, date_of_birth, address, submitted_at, verified_at)
SELECT u.user_id, s.headline, s.bio, s.exp, s.rate, s.jlpt,
       'APPROVED', 'PREMIUM', true, 'ACTIVE_PAID',
       s.phone, s.dob, s.address, now() - interval '3 days', now() - interval '2 days'
FROM u JOIN src s USING (email);

-- Tập gia sư của file này (dùng cho các bước sau)
CREATE TEMP TABLE seed_tutors ON COMMIT DROP AS
SELECT t.tutor_id, t.hourly_rate, t.headline, t.bio, u.email
FROM tutors t JOIN users u USING (user_id)
WHERE u.email IN ('tanaka.yuki@kizuna.vn', 'tran.minh.chau@kizuna.vn', 'le.hoang.long@kizuna.vn',
                  'suzuki.aiko@kizuna.vn', 'pham.thu.trang@kizuna.vn', 'nguyen.duc.anh@kizuna.vn');

-- 2. Chứng chỉ JLPT N1 đã duyệt
INSERT INTO tutor_credentials (tutor_id, credential_type, title, file_url, verified_status)
SELECT tutor_id, 'JLPT', 'Chứng chỉ JLPT N1', 'https://example.invalid/jlpt-n1.pdf', 'VERIFIED'
FROM seed_tutors s
WHERE NOT EXISTS (SELECT 1 FROM tutor_credentials c WHERE c.tutor_id = s.tutor_id);

-- 3. Gói hội viên 12 tháng còn hạn
INSERT INTO tutor_memberships (tutor_id, plan_id, start_date, end_date, status, amount, payment_status, payment_gateway)
SELECT s.tutor_id, p.plan_id, CURRENT_DATE, (CURRENT_DATE + make_interval(months => p.duration_months))::date,
       'ACTIVE', p.price, 'SUCCESS', 'SEED'
FROM seed_tutors s
CROSS JOIN LATERAL (SELECT plan_id, duration_months, price FROM membership_plans
                    WHERE is_active ORDER BY duration_months DESC LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM tutor_memberships m
                  WHERE m.tutor_id = s.tutor_id AND m.status = 'ACTIVE' AND m.end_date >= CURRENT_DATE);

-- 4. Bài đăng đã duyệt, kèm bảng giá theo cấp độ (buổi 90 phút)
INSERT INTO tutor_posts (tutor_id, title, content, specialties, teaching_format, hourly_rate, price_items, status, reviewed_at)
SELECT s.tutor_id, s.headline, s.bio, s.headline,
       CASE WHEN s.email LIKE 'le.hoang.long%' OR s.email LIKE 'tanaka%' THEN 'ONLINE' ELSE 'BOTH' END,
       s.hourly_rate,
       jsonb_build_array(
         jsonb_build_object('level', 'JLPT_N4', 'hourlyRate', s.hourly_rate, 'sessionMinutes', 90, 'pricePerSession', s.hourly_rate * 1.5),
         jsonb_build_object('level', 'JLPT_N3', 'hourlyRate', s.hourly_rate + 20000, 'sessionMinutes', 90, 'pricePerSession', (s.hourly_rate + 20000) * 1.5)),
       'PUBLISHED', now() - interval '1 day'
FROM seed_tutors s
WHERE NOT EXISTS (SELECT 1 FROM tutor_posts p WHERE p.tutor_id = s.tutor_id);

-- 5. Lịch rảnh hằng tuần (1 = Thứ Hai ... 7 = Chủ nhật), mỗi gia sư 5 khung
INSERT INTO tutor_availabilities (tutor_id, day_of_week, start_time, end_time, is_active)
SELECT s.tutor_id, v.d, v.st::time, v.et::time, true
FROM seed_tutors s
CROSS JOIN (VALUES (1, '19:00', '21:00'), (3, '19:00', '21:00'), (5, '18:00', '21:00'),
                   (6, '08:00', '11:00'), (7, '14:00', '17:00')) v(d, st, et)
WHERE NOT EXISTS (SELECT 1 FROM tutor_availabilities a WHERE a.tutor_id = s.tutor_id);

COMMIT;

-- Kiểm tra
SELECT u.full_name, u.email, t.is_featured, t.verification_status,
       EXISTS (SELECT 1 FROM tutor_memberships m WHERE m.tutor_id = t.tutor_id AND m.status = 'ACTIVE' AND m.end_date >= CURRENT_DATE) AS membership,
       EXISTS (SELECT 1 FROM tutor_posts p WHERE p.tutor_id = t.tutor_id AND p.status = 'PUBLISHED') AS post,
       (SELECT count(*) FROM tutor_availabilities a WHERE a.tutor_id = t.tutor_id AND a.is_active) AS slots
FROM tutors t JOIN users u USING (user_id)
WHERE u.email LIKE '%@kizuna.vn' AND t.is_featured
ORDER BY u.full_name;
