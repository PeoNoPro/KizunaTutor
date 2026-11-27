-- ====================================================================================================
-- KIZUNATUTOR - SEEDER: 5 GIA SƯ NGƯỜI VIỆT KÈM CHỨNG CHỈ ĐÃ XÁC THỰC VÀ LỊCH RẢNH GIẢNG DẠY
-- TUYỆT ĐỐI KHÔNG TẠO BẢNG / KHÔNG DROP TABLE (CHỈ DÙNG LỆNH INSERT)
-- Mật khẩu mặc định của tất cả các tài khoản: 123456 ($2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq)
-- ====================================================================================================

-- 1. NẠP 5 TÀI KHOẢN NGƯỜI DÙNG (USERS) VAI TRÒ GIA SƯ (role_id = 5)
INSERT INTO users (user_id, role_id, email, password_hash, full_name, phone, avatar_url, account_status, must_change_password) VALUES
(
    '00000000-0000-0000-0000-000000000021',
    5,
    'nguyen.mai@kizuna.vn',
    '$2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq',
    'Nguyễn Thị Mai',
    '0912345601',
    'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=400&q=80',
    'ACTIVE',
    FALSE
),
(
    '00000000-0000-0000-0000-000000000022',
    5,
    'tran.dat@kizuna.vn',
    '$2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq',
    'Trần Văn Đạt',
    '0912345602',
    'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80',
    'ACTIVE',
    FALSE
),
(
    '00000000-0000-0000-0000-000000000023',
    5,
    'le.hoangyen@kizuna.vn',
    '$2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq',
    'Lê Hoàng Yến',
    '0912345603',
    'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=400&q=80',
    'ACTIVE',
    FALSE
),
(
    '00000000-0000-0000-0000-000000000024',
    5,
    'pham.minhtuan@kizuna.vn',
    '$2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq',
    'Phạm Minh Tuấn',
    '0912345604',
    'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80',
    'ACTIVE',
    FALSE
),
(
    '00000000-0000-0000-0000-000000000025',
    5,
    'dang.quynhchi@kizuna.vn',
    '$2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq',
    'Đặng Quỳnh Chi',
    '0912345605',
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80',
    'ACTIVE',
    FALSE
)
ON CONFLICT (email) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    avatar_url = EXCLUDED.avatar_url,
    phone = EXCLUDED.phone,
    account_status = EXCLUDED.account_status;

-- 2. NẠP HỒ SƠ 5 GIA SƯ (TUTORS) Ở 5 TỈNH/THÀNH PHỐ KHÁC NHAU
INSERT INTO tutors (
    tutor_id, user_id, headline, bio, experience_years, hourly_rate, 
    highest_jlpt_level, avg_rating, total_reviews, verification_status, 
    membership_status, is_featured, tutor_state, phone, address, certificate_urls
) VALUES
-- Gia sư 1: Nguyễn Thị Mai (Hà Nội)
(
    '00000000-0000-0000-0002-000000000021',
    '00000000-0000-0000-0000-000000000021',
    'Thủ khoa Sư phạm tiếng Nhật ĐH Hà Nội • Chuyên luyện thi N3-N1 cấp tốc',
    'Xin chào các bạn học viên! Mình là Nguyễn Thị Mai, tốt nghiệp thủ khoa ngành Sư phạm tiếng Nhật và đạt 175/180 JLPT N1. Với 5 năm kinh nghiệm luyện thi chuyên sâu, mình chú trọng việc nắm vững bản chất ngữ pháp, mẹo giải đề Dokkai thực chiến và phương pháp nhớ Kanji sâu không quên.',
    5,
    250000.00,
    'N1',
    4.95,
    38,
    'APPROVED',
    'PREMIUM',
    TRUE,
    'ACTIVE_PAID',
    '0912345601',
    'Cầu Giấy, Hà Nội',
    'https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?auto=format&fit=crop&w=800&q=80'
),
-- Gia sư 2: Trần Văn Đạt (TP. Hồ Chí Minh)
(
    '00000000-0000-0000-0002-000000000022',
    '00000000-0000-0000-0000-000000000022',
    'Cựu du học sinh Osaka • Luyện phát âm chuẩn & Giao tiếp phản xạ Kaiwa N5-N3',
    'Chào mọi người, mình là Trần Văn Đạt! Từng có 4 năm sống, học tập và làm việc tại Osaka. Mình thấu hiểu những rào cản khi người Việt bắt đầu học tiếng Nhật, đặc biệt là sự e ngại khi giao tiếp. Lớp học của mình sẽ giúp bạn phản xạ tự nhiên như người bản xứ.',
    4,
    200000.00,
    'N2',
    4.88,
    29,
    'APPROVED',
    'PREMIUM',
    TRUE,
    'ACTIVE_PAID',
    '0912345602',
    'Quận 1, TP. Hồ Chí Minh',
    'https://images.unsplash.com/photo-1589330694653-dad6bc01cf0f?auto=format&fit=crop&w=800&q=80'
),
-- Gia sư 3: Lê Hoàng Yến (Đà Nẵng)
(
    '00000000-0000-0000-0002-000000000023',
    '00000000-0000-0000-0000-000000000023',
    'Giảng viên tiếng Nhật Thương mại & Đàm phán BJT J1+ • JLPT N1',
    'Chào các bạn! Tôi là Hoàng Yến, cựu thông dịch viên cao cấp tại Đà Nẵng với 6 năm giảng dạy tiếng Nhật doanh nghiệp. Tôi chuyên đào tạo kính ngữ (Keigo), kỹ năng viết email thương mại, chuẩn bị hồ sơ và phỏng vấn vào các tập đoàn lớn của Nhật Bản.',
    6,
    320000.00,
    'N1',
    5.00,
    42,
    'APPROVED',
    'PREMIUM',
    TRUE,
    'ACTIVE_PAID',
    '0912345603',
    'Hải Châu, Đà Nẵng',
    'https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?auto=format&fit=crop&w=800&q=80'
),
-- Gia sư 4: Phạm Minh Tuấn (Hải Phòng)
(
    '00000000-0000-0000-0002-000000000024',
    '00000000-0000-0000-0000-000000000024',
    'Kỹ sư Cầu nối BrSE & Chuyên gia IT Comtor • Tiếng Nhật chuyên ngành công nghệ',
    'Chào bạn! Mình là Minh Tuấn, BrSE với hơn 5 năm kinh nghiệm làm việc với các đối tác Nhật Bản. Mình đồng hành cùng các bạn học viên có định hướng làm việc trong ngành CNTT, kỹ thuật phần mềm, rèn luyện kỹ năng đọc tài liệu Spec và giao tiếp dự án.',
    5,
    300000.00,
    'N1',
    4.90,
    25,
    'APPROVED',
    'PREMIUM',
    FALSE,
    'ACTIVE_PAID',
    '0912345604',
    'Ngô Quyền, Hải Phòng',
    'https://images.unsplash.com/photo-1589330694653-dad6bc01cf0f?auto=format&fit=crop&w=800&q=80'
),
-- Gia sư 5: Đặng Quỳnh Chi (Cần Thơ)
(
    '00000000-0000-0000-0002-000000000025',
    '00000000-0000-0000-0000-000000000025',
    'Giảng viên Sư phạm trẻ trung • Luyện giao tiếp sơ cấp N5-N3 & Luyện thi du học',
    'Konnichiwa! Mình là Quỳnh Chi. Phong cách dạy học của mình là tương tác sinh động, ứng dụng hình ảnh, video và minigame thực tế giúp học viên không cảm thấy áp lực. Rất vui được đồng hành cùng các bạn trên con đường chinh phục tiếng Nhật!',
    3,
    180000.00,
    'N2',
    4.96,
    31,
    'APPROVED',
    'PREMIUM',
    TRUE,
    'ACTIVE_PAID',
    '0912345605',
    'Ninh Kiều, Cần Thơ',
    'https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?auto=format&fit=crop&w=800&q=80'
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
    phone = EXCLUDED.phone,
    address = EXCLUDED.address,
    certificate_urls = EXCLUDED.certificate_urls;

-- 3. NẠP CHỨNG CHỈ ĐÃ XÁC THỰC (tutor_credentials)
INSERT INTO tutor_credentials (
    credential_id, tutor_id, credential_type, title, file_url, verified_status
) VALUES
-- Mai: JLPT N1 & Bằng Cử nhân
(
    '00000000-0000-0000-0003-000000000021',
    '00000000-0000-0000-0002-000000000021',
    'JLPT_CERTIFICATE',
    'Chứng chỉ Năng lực Nhật ngữ JLPT N1 (175/180)',
    'https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?auto=format&fit=crop&w=800&q=80',
    'VERIFIED'
),
(
    '00000000-0000-0000-0003-000000000022',
    '00000000-0000-0000-0002-000000000021',
    'UNIVERSITY_DEGREE',
    'Bằng Cử nhân Xuất sắc Sư phạm Tiếng Nhật - ĐH Ngoại Ngữ ĐHQGHN',
    'https://images.unsplash.com/photo-1589330694653-dad6bc01cf0f?auto=format&fit=crop&w=800&q=80',
    'VERIFIED'
),
-- Đạt: JLPT N2 & Chứng chỉ nghiệp vụ Japan Foundation
(
    '00000000-0000-0000-0003-000000000023',
    '00000000-0000-0000-0002-000000000022',
    'JLPT_CERTIFICATE',
    'Chứng chỉ Năng lực Nhật ngữ JLPT N2',
    'https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?auto=format&fit=crop&w=800&q=80',
    'VERIFIED'
),
(
    '00000000-0000-0000-0003-000000000024',
    '00000000-0000-0000-0002-000000000022',
    'TEACHING_CERTIFICATE',
    'Chứng nhận Phương pháp Giảng dạy Tiếng Nhật - Japan Foundation',
    'https://images.unsplash.com/photo-1589330694653-dad6bc01cf0f?auto=format&fit=crop&w=800&q=80',
    'VERIFIED'
),
-- Yến: JLPT N1 & BJT J1+
(
    '00000000-0000-0000-0003-000000000025',
    '00000000-0000-0000-0002-000000000023',
    'JLPT_CERTIFICATE',
    'Chứng chỉ Năng lực Nhật ngữ JLPT N1',
    'https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?auto=format&fit=crop&w=800&q=80',
    'VERIFIED'
),
(
    '00000000-0000-0000-0003-000000000026',
    '00000000-0000-0000-0002-000000000023',
    'BJT_CERTIFICATE',
    'Chứng chỉ Tiếng Nhật Thương Mại BJT J1+ (580 điểm)',
    'https://images.unsplash.com/photo-1589330694653-dad6bc01cf0f?auto=format&fit=crop&w=800&q=80',
    'VERIFIED'
),
-- Tuấn: JLPT N1 & Chuẩn Kỹ sư BrSE
(
    '00000000-0000-0000-0003-000000000027',
    '00000000-0000-0000-0002-000000000024',
    'JLPT_CERTIFICATE',
    'Chứng chỉ Năng lực Nhật ngữ JLPT N1',
    'https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?auto=format&fit=crop&w=800&q=80',
    'VERIFIED'
),
(
    '00000000-0000-0000-0003-000000000028',
    '00000000-0000-0000-0002-000000000024',
    'PROFESSIONAL_CERTIFICATE',
    'Chứng chỉ Chuẩn Kỹ sư Cầu nối BrSE / IT Comtor Nhật Bản',
    'https://images.unsplash.com/photo-1589330694653-dad6bc01cf0f?auto=format&fit=crop&w=800&q=80',
    'VERIFIED'
),
-- Chi: JLPT N2 & Nghiệp vụ Sư phạm
(
    '00000000-0000-0000-0003-000000000029',
    '00000000-0000-0000-0002-000000000025',
    'JLPT_CERTIFICATE',
    'Chứng chỉ Năng lực Nhật ngữ JLPT N2',
    'https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?auto=format&fit=crop&w=800&q=80',
    'VERIFIED'
),
(
    '00000000-0000-0000-0003-000000000030',
    '00000000-0000-0000-0002-000000000025',
    'TEACHING_CERTIFICATE',
    'Chứng chỉ Nghiệp vụ Sư phạm Giảng dạy Tiếng Nhật',
    'https://images.unsplash.com/photo-1589330694653-dad6bc01cf0f?auto=format&fit=crop&w=800&q=80',
    'VERIFIED'
)
ON CONFLICT (credential_id) DO UPDATE SET
    title = EXCLUDED.title,
    file_url = EXCLUDED.file_url,
    verified_status = EXCLUDED.verified_status;

-- 4. NẠP LỊCH RẢNH GIẢNG DẠY CỦA GIA SƯ (tutor_availabilities)
-- Quy ước day_of_week: 1 = Chủ nhật, 2 = Thứ hai, 3 = Thứ ba, 4 = Thứ tư, 5 = Thứ năm, 6 = Thứ sáu, 7 = Thứ bảy
INSERT INTO tutor_availabilities (
    availability_id, tutor_id, day_of_week, start_time, end_time, is_active
) VALUES
-- Mai: Thứ 2, 4, 6 (18:30 - 20:30), Chủ nhật (09:00 - 11:00)
('00000000-0000-0000-0006-000000000021', '00000000-0000-0000-0002-000000000021', 2, '18:30:00', '20:30:00', TRUE),
('00000000-0000-0000-0006-000000000022', '00000000-0000-0000-0002-000000000021', 4, '18:30:00', '20:30:00', TRUE),
('00000000-0000-0000-0006-000000000023', '00000000-0000-0000-0002-000000000021', 6, '18:30:00', '20:30:00', TRUE),
('00000000-0000-0000-0006-000000000024', '00000000-0000-0000-0002-000000000021', 1, '09:00:00', '11:00:00', TRUE),

-- Đạt: Thứ 3, 5, 7 (19:00 - 21:00)
('00000000-0000-0000-0006-000000000025', '00000000-0000-0000-0002-000000000022', 3, '19:00:00', '21:00:00', TRUE),
('00000000-0000-0000-0006-000000000026', '00000000-0000-0000-0002-000000000022', 5, '19:00:00', '21:00:00', TRUE),
('00000000-0000-0000-0006-000000000027', '00000000-0000-0000-0002-000000000022', 7, '19:00:00', '21:00:00', TRUE),

-- Yến: Thứ 2, 3, 5 (18:00 - 20:00), Thứ 7 (14:00 - 16:00)
('00000000-0000-0000-0006-000000000028', '00000000-0000-0000-0002-000000000023', 2, '18:00:00', '20:00:00', TRUE),
('00000000-0000-0000-0006-000000000029', '00000000-0000-0000-0002-000000000023', 3, '18:00:00', '20:00:00', TRUE),
('00000000-0000-0000-0006-000000000030', '00000000-0000-0000-0002-000000000023', 5, '18:00:00', '20:00:00', TRUE),
('00000000-0000-0000-0006-000000000031', '00000000-0000-0000-0002-000000000023', 7, '14:00:00', '16:00:00', TRUE),

-- Tuấn: Thứ 2, 4, 6 (19:30 - 21:30), Chủ nhật (15:00 - 17:00)
('00000000-0000-0000-0006-000000000032', '00000000-0000-0000-0002-000000000024', 2, '19:30:00', '21:30:00', TRUE),
('00000000-0000-0000-0006-000000000033', '00000000-0000-0000-0002-000000000024', 4, '19:30:00', '21:30:00', TRUE),
('00000000-0000-0000-0006-000000000034', '00000000-0000-0000-0002-000000000024', 6, '19:30:00', '21:30:00', TRUE),
('00000000-0000-0000-0006-000000000035', '00000000-0000-0000-0002-000000000024', 1, '15:00:00', '17:00:00', TRUE),

-- Chi: Thứ 3, 5, 7 (18:00 - 20:00), Chủ nhật (09:00 - 11:00)
('00000000-0000-0000-0006-000000000036', '00000000-0000-0000-0002-000000000025', 3, '18:00:00', '20:00:00', TRUE),
('00000000-0000-0000-0006-000000000037', '00000000-0000-0000-0002-000000000025', 5, '18:00:00', '20:00:00', TRUE),
('00000000-0000-0000-0006-000000000038', '00000000-0000-0000-0002-000000000025', 7, '18:00:00', '20:00:00', TRUE),
('00000000-0000-0000-0006-000000000039', '00000000-0000-0000-0002-000000000025', 1, '09:00:00', '11:00:00', TRUE)
ON CONFLICT (availability_id) DO UPDATE SET
    day_of_week = EXCLUDED.day_of_week,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    is_active = EXCLUDED.is_active;

