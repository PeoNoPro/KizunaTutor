-- 1. NẠP 3 HỌC VIÊN ĐÃ ĐÁNH GIÁ
INSERT INTO users (user_id, role_id, email, password_hash, full_name, phone, account_status, must_change_password) VALUES
('00000000-0000-0000-0000-000000000021', 6, 'long.nguyen@kizuna.vn', '$2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq', 'Nguyễn Hoàng Long', '0912345601', 'ACTIVE', FALSE),
('00000000-0000-0000-0000-000000000022', 6, 'huong.tran@kizuna.vn', '$2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq', 'Trần Thu Hương', '0912345602', 'ACTIVE', FALSE),
('00000000-0000-0000-0000-000000000023', 6, 'duc.pham@kizuna.vn', '$2a$10$x0xl4z2EEPhuOlEoTRzZUOo5gwdjlItWLUaWKgrbIbIaKQUTa7Kiq', 'Phạm Minh Đức', '0912345603', 'ACTIVE', FALSE)
ON CONFLICT (user_id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone;

-- 2. NẠP BẢN GHI LEARNERS
INSERT INTO learners (learner_id, user_id, current_level, target_level, learning_goals, updated_at) VALUES
('00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0000-000000000021', 'N3', 'N2', 'Luyện Kaiwa phản xạ thực chiến', CURRENT_TIMESTAMP),
('00000000-0000-0000-0004-000000000002', '00000000-0000-0000-0000-000000000022', 'N2', 'N1', 'Luyện thi JLPT N1 cấp tốc', CURRENT_TIMESTAMP),
('00000000-0000-0000-0004-000000000003', '00000000-0000-0000-0000-000000000023', 'N3', 'BJT', 'Tiếng Nhật Thương mại & Doanh nghiệp IT', CURRENT_TIMESTAMP)
ON CONFLICT (learner_id) DO NOTHING;

-- 3. NẠP ĐÁNH GIÁ (TUTOR_REVIEWS) CHO YAMADA KENJI (10001)
INSERT INTO tutor_reviews (review_id, learner_id, tutor_id, rating, comment, created_at) VALUES
('00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0002-000000000001', 5.0, 'Thầy Kenji phát âm cực chuẩn Tokyo, sửa từng lỗi luyến âm và trọng âm (accent) rất kiên nhẫn. Sau 2 tháng học 1-1, mình đã tự tin phỏng vấn với sếp Nhật!', '2025-04-15 10:30:00+07'),
('00000000-0000-0000-0007-000000000002', '00000000-0000-0000-0004-000000000002', '00000000-0000-0000-0002-000000000001', 5.0, 'Phương pháp giảng dạy ngữ pháp và Đọc hiểu Dokkai rất logic, có hệ thống slide rõ ràng. Thầy luôn chuẩn bị bài kỹ và phản hồi bài tập về nhà rất nhanh.', '2025-04-02 14:15:00+07'),
('00000000-0000-0000-0007-000000000003', '00000000-0000-0000-0004-000000000003', '00000000-0000-0000-0002-000000000001', 5.0, 'Buổi học thử 1-1 đã tạo ấn tượng rất tốt, phong cách sư phạm chuẩn chỉ và lịch sự. Cảm ơn thầy và Kizuna!', '2025-03-20 09:00:00+07')
ON CONFLICT (review_id) DO UPDATE SET
    rating = EXCLUDED.rating,
    comment = EXCLUDED.comment,
    created_at = EXCLUDED.created_at;


-- 4. NẠP CHỨNG CHỈ CHO YAMADA KENJI (10001)
INSERT INTO tutor_credentials (credential_id, tutor_id, credential_type, title, file_url, verified_status) VALUES
('00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0002-000000000001', 'JLPT_CERTIFICATE', 'Chứng chỉ Năng lực Nhật ngữ Quốc tế JLPT N1', 'https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?auto=format&fit=crop&w=800&q=80', 'VERIFIED'),
('00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0002-000000000001', 'TEACHING_CERTIFICATE', 'Chứng chỉ Sư phạm Giảng dạy Tiếng Nhật Tokyo', 'https://images.unsplash.com/photo-1589330694653-dad6bc01cf0f?auto=format&fit=crop&w=800&q=80', 'VERIFIED')
ON CONFLICT (credential_id) DO UPDATE SET
    title = EXCLUDED.title,
    file_url = EXCLUDED.file_url,
    verified_status = EXCLUDED.verified_status;
