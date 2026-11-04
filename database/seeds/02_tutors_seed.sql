-- ====================================================================================================
-- KIZUNATUTOR - SEEDER: 5 GIA SƯ MẪU TIẾNG NHẬT (02_tutors_seed.sql)
-- Nạp vào các bảng có sẵn: users, tutors, tutor_memberships, ai_feature_quotas
-- TUYỆT ĐỐI KHÔNG TẠO BẢNG (Không dùng CREATE TABLE / DROP TABLE)
-- Mật khẩu mặc định của tất cả các tài khoản: 123456
-- ====================================================================================================

-- 1. NẠP 5 TÀI KHOẢN NGƯỜI DÙNG (USERS) - VAI TRÒ GIA SƯ (role_id = 5)
INSERT INTO users (user_id, role_id, email, password_hash, full_name, phone, account_status, must_change_password) VALUES
('00000000-0000-0000-0000-000000000011', 5, 'tanaka.hiroshi@kizuna.vn', '$2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq', 'Tanaka Hiroshi', '0905234571', 'ACTIVE', FALSE),
('00000000-0000-0000-0000-000000000012', 5, 'nguyen.mai@kizuna.vn', '$2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq', 'Nguyễn Thị Mai', '0905234572', 'ACTIVE', FALSE),
('00000000-0000-0000-0000-000000000013', 5, 'kobayashi.ken@kizuna.vn', '$2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq', 'Kobayashi Ken', '0905234573', 'ACTIVE', FALSE),
('00000000-0000-0000-0000-000000000014', 5, 'tran.dat@kizuna.vn', '$2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq', 'Trần Văn Đạt', '0905234574', 'ACTIVE', FALSE),
('00000000-0000-0000-0000-000000000015', 5, 'sato.yuka@kizuna.vn', '$2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq', 'Sato Yuka', '0905234575', 'ACTIVE', FALSE)
ON CONFLICT (email) DO UPDATE SET
    role_id = EXCLUDED.role_id,
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    account_status = EXCLUDED.account_status,
    must_change_password = EXCLUDED.must_change_password;

-- 2. NẠP HỒ SƠ 5 GIA SƯ (TUTORS) - ĐÃ PHÊ DUYỆT (verification_status = 'APPROVED')
INSERT INTO tutors (
    tutor_id, user_id, headline, bio, experience_years, hourly_rate, 
    highest_jlpt_level, avg_rating, total_reviews, verification_status, 
    membership_status, is_featured, tutor_state, phone
) VALUES
-- Gia sư 1: Tanaka Hiroshi (Bản xứ Tokyo, JLPT N1, Kaiwa phản xạ thực chiến)
(
    '00000000-0000-0000-0002-000000000011',
    '00000000-0000-0000-0000-000000000011',
    'Giảng viên bản xứ Tokyo • Luyện Kaiwa phản xạ thực chiến • JLPT N1-N2',
    'Xin chào các bạn! Tôi là Tanaka Hiroshi, đến từ Tokyo. Tôi đã có 8 năm kinh nghiệm giảng dạy tiếng Nhật cho sinh viên và kỹ sư Việt Nam. Phong cách dạy vui vẻ, tự nhiên, tập trung chỉnh phát âm và tư duy diễn đạt như người bản xứ.',
    8,
    350000.00,
    'N1',
    4.95,
    32,
    'APPROVED',
    'PREMIUM',
    TRUE,
    'ACTIVE_PAID',
    '0905234571'
),
-- Gia sư 2: Nguyễn Thị Mai (Thủ khoa ĐH Hà Nội, JLPT N1, Luyện thi cấp tốc)
(
    '00000000-0000-0000-0002-000000000012',
    '00000000-0000-0000-0000-000000000012',
    'Thủ khoa Sư phạm tiếng Nhật ĐH Hà Nội • Chuyên luyện thi N3-N1 cấp tốc',
    'Tốt nghiệp xuất sắc chuyên ngành Sư phạm Tiếng Nhật, từng đạt 175/180 JLPT N1. Phương pháp học logic, giải đề có chiến thuật, nắm chắc ngữ pháp và chữ Hán nhớ sâu không quên.',
    5,
    220000.00,
    'N1',
    4.90,
    26,
    'APPROVED',
    'PREMIUM',
    TRUE,
    'ACTIVE_PAID',
    '0905234572'
),
-- Gia sư 3: Kobayashi Ken (IT Comtor & Tiếng Nhật Thương Mại BJT J1+)
(
    '00000000-0000-0000-0002-000000000013',
    '00000000-0000-0000-0000-000000000013',
    'Chuyên gia IT Comtor & Đàm phán thương mại tiếng Nhật BJT J1+ • JLPT N1',
    'Tôi là Kobayashi Ken, cựu chuyên viên tư vấn CNTT tại Tokyo. Giúp các bạn cải thiện tiếng Nhật văn phòng (Keigo), kỹ năng viết email thương mại, đàm phán hợp đồng và phỏng vấn vào công ty công nghệ Nhật Bản.',
    6,
    320000.00,
    'N1',
    5.00,
    15,
    'APPROVED',
    'PREMIUM',
    TRUE,
    'ACTIVE_PAID',
    '0905234573'
),
-- Gia sư 4: Trần Văn Đạt (Cựu du học sinh Osaka, JLPT N2, Luyện giao tiếp N5-N3)
(
    '00000000-0000-0000-0002-000000000014',
    '00000000-0000-0000-0000-000000000014',
    'Cựu du học sinh Osaka • Luyện phát âm chuẩn & Giao tiếp nền tảng N5-N3',
    'Đã có 4 năm sống và làm việc tại Osaka. Hiểu rõ những khó khăn của người Việt khi mới học chữ Hán và ngữ pháp. Bài giảng sinh động, tạo cảm hứng và kiên nhẫn đồng hành cùng học viên từ con số 0.',
    4,
    180000.00,
    'N2',
    4.85,
    21,
    'APPROVED',
    'PREMIUM',
    FALSE,
    'ACTIVE_PAID',
    '0905234574'
),
-- Gia sư 5: Sato Yuka (Cử nhân Ngôn ngữ học Tokyo, Kaiwa & Văn hóa doanh nghiệp)
(
    '00000000-0000-0000-0002-000000000015',
    '00000000-0000-0000-0000-000000000015',
    'Cử nhân Ngôn ngữ học Tokyo • Chuyên luyện Kaiwa & Văn hóa doanh nghiệp Nhật',
    'Chào bạn, mình là Yuka! Mình chuyên dạy giao tiếp hàng ngày, ngữ điệu tự nhiên và cách ứng xử trong môi trường công sở Nhật Bản. Lớp học thoải mái, nhiều chủ đề thực tế thú vị, dễ tiếp thu.',
    5,
    280000.00,
    'N1',
    4.92,
    19,
    'APPROVED',
    'PREMIUM',
    TRUE,
    'ACTIVE_PAID',
    '0905234575'
)
ON CONFLICT (user_id) DO UPDATE SET
    headline = EXCLUDED.headline,
    bio = EXCLUDED.bio,
    experience_years = EXCLUDED.experience_years,
    hourly_rate = EXCLUDED.hourly_rate,
    highest_jlpt_level = EXCLUDED.highest_jlpt_level,
    avg_rating = EXCLUDED.avg_rating,
    total_reviews = EXCLUDED.total_reviews,
    verification_status = EXCLUDED.verification_status,
    membership_status = EXCLUDED.membership_status,
    is_featured = EXCLUDED.is_featured,
    tutor_state = EXCLUDED.tutor_state,
    phone = EXCLUDED.phone;

-- 3. KÍCH HOẠT GÓI HỘI VIÊN GIA SƯ HOẠT ĐỘNG (tutor_memberships)
INSERT INTO tutor_memberships (
    membership_id, tutor_id, plan_id, start_date, end_date, 
    status, amount, transaction_ref, payment_status, payment_gateway
) VALUES
('00000000-0000-0000-0004-000000000011', '00000000-0000-0000-0002-000000000011', '00000000-0000-0000-0001-000000000002', CURRENT_DATE, CURRENT_DATE + INTERVAL '90 days', 'ACTIVE', 499000.00, 'KZ_SEED_TUTOR_01', 'SUCCESS', 'VNPay'),
('00000000-0000-0000-0004-000000000012', '00000000-0000-0000-0002-000000000012', '00000000-0000-0000-0001-000000000003', CURRENT_DATE, CURRENT_DATE + INTERVAL '180 days', 'ACTIVE', 899000.00, 'KZ_SEED_TUTOR_02', 'SUCCESS', 'VNPay'),
('00000000-0000-0000-0004-000000000013', '00000000-0000-0000-0002-000000000013', '00000000-0000-0000-0001-000000000003', CURRENT_DATE, CURRENT_DATE + INTERVAL '180 days', 'ACTIVE', 899000.00, 'KZ_SEED_TUTOR_03', 'SUCCESS', 'VNPay'),
('00000000-0000-0000-0004-000000000014', '00000000-0000-0000-0002-000000000014', '00000000-0000-0000-0001-000000000001', CURRENT_DATE, CURRENT_DATE + INTERVAL '30 days', 'ACTIVE', 199000.00, 'KZ_SEED_TUTOR_04', 'SUCCESS', 'VNPay'),
('00000000-0000-0000-0004-000000000015', '00000000-0000-0000-0002-000000000015', '00000000-0000-0000-0001-000000000002', CURRENT_DATE, CURRENT_DATE + INTERVAL '90 days', 'ACTIVE', 499000.00, 'KZ_SEED_TUTOR_05', 'SUCCESS', 'VNPay')
ON CONFLICT (membership_id) DO UPDATE SET
    status = EXCLUDED.status,
    end_date = EXCLUDED.end_date;

-- 4. CẤP HẠN NGẠCH AI VẼ KANJI SHODO (ai_feature_quotas)
INSERT INTO ai_feature_quotas (quota_id, user_id, daily_ai_drawing_limit, used_today, reset_date) VALUES
('00000000-0000-0000-0005-000000000011', '00000000-0000-0000-0000-000000000011', 200, 0, CURRENT_DATE),
('00000000-0000-0000-0005-000000000012', '00000000-0000-0000-0000-000000000012', 200, 0, CURRENT_DATE),
('00000000-0000-0000-0005-000000000013', '00000000-0000-0000-0000-000000000013', 200, 0, CURRENT_DATE),
('00000000-0000-0000-0005-000000000014', '00000000-0000-0000-0000-000000000014', 200, 0, CURRENT_DATE),
('00000000-0000-0000-0005-000000000015', '00000000-0000-0000-0000-000000000015', 200, 0, CURRENT_DATE)
ON CONFLICT (user_id) DO UPDATE SET
    daily_ai_drawing_limit = EXCLUDED.daily_ai_drawing_limit;

