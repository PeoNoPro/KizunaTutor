-- Đổi dữ liệu đã nạp bằng bản cũ của fe11_test_data.sql sang tên tự nhiên, giữ nguyên số liệu và thao tác đã làm.
-- Chạy một lần; chạy lại không đổi thêm gì. Dữ liệu vẫn nhận biết được qua đuôi @kizunatutor.invalid
-- và mô tả sự cố "Dữ liệu mẫu FE-11", để fe11_test_data_cleanup.sql xóa được khi cần.
BEGIN;
UPDATE users u SET full_name=m.name FROM (VALUES
    ('hocvu01@test.kizuna.invalid','Phạm Minh Anh'),('hocvu02@test.kizuna.invalid','Đỗ Thu Hà'),('hocvu03@test.kizuna.invalid','Vũ Quang Huy'),
    ('cskh01@test.kizuna.invalid','Nguyễn Thị Lan'),('cskh02@test.kizuna.invalid','Trần Đức Long'),('cskh03@test.kizuna.invalid','Lê Thanh Mai'),
    ('hocvien01@test.kizuna.invalid','Hoàng Gia Bảo'),('hocvien02@test.kizuna.invalid','Bùi Khánh Linh'),
    ('giasu01@test.kizuna.invalid','Đặng Hoài Nam'),('giasu02@test.kizuna.invalid','Ngô Phương Thảo'),('giasu03@test.kizuna.invalid','Lý Tuấn Kiệt'),
    ('giasu04@test.kizuna.invalid','Mai Ngọc Trâm'),('giasu05@test.kizuna.invalid','Dương Hải Yến')) AS m(email,name)
WHERE u.email=m.email;
UPDATE users SET email=replace(email,'@test.kizuna.invalid','@kizunatutor.invalid') WHERE email LIKE '%@test.kizuna.invalid';
UPDATE membership_plans SET plan_name=replace(plan_name,'[TEST] ','') WHERE plan_name LIKE '[TEST]%';
UPDATE tutor_credentials SET title=replace(title,'[TEST] Chứng chỉ','Chứng chỉ JLPT') WHERE title LIKE '[TEST]%';
UPDATE payment_transactions SET payment_gateway='VNPay' WHERE payment_gateway='TEST' AND user_id IN (SELECT user_id FROM users WHERE email LIKE '%@kizunatutor.invalid');
UPDATE staff_role_change_requests SET reason='Hoàn thành tốt nhiệm vụ, đề xuất thăng chức' WHERE reason LIKE 'Đề xuất test%';
UPDATE staff_account_audit_events SET reason=replace(reason,'Đề xuất test để thử duyệt','Hoàn thành tốt nhiệm vụ, đề xuất thăng chức') WHERE reason LIKE '%Đề xuất test%';
UPDATE technical_incident_events SET detail=replace(replace(detail,'Dữ liệu test FE-11','Dữ liệu mẫu FE-11'),'Kết quả test: đã khắc phục.','Đã khắc phục.'),
    verification_text=replace(verification_text,'trên môi trường test','sau khi khắc phục'),reason=replace(reason,'Lỗi test tái diễn.','Lỗi tái diễn.')
WHERE incident_id IN (SELECT incident_id FROM technical_incidents WHERE description LIKE 'Dữ liệu test FE-11%');
UPDATE technical_incidents SET description=replace(description,'Dữ liệu test FE-11','Dữ liệu mẫu FE-11') WHERE description LIKE 'Dữ liệu test FE-11%';
UPDATE technical_incidents i SET subject=m.subject FROM (VALUES
    ('[TEST] Sự cố mới ghi nhận','Trang đăng nhập phản hồi chậm'),('[TEST] Sự cố đang xử lý','Không tải được ảnh đại diện'),
    ('[TEST] Sự cố đã giải quyết','Lỗi gửi email thông báo'),('[TEST] Sự cố đã đóng','Mất kết nối cơ sở dữ liệu ban đêm'),
    ('[TEST] Sự cố mở lại, không còn kết quả','Lịch học thử hiển thị sai giờ'),('=1+1 [TEST] Tiêu đề giống công thức','=1+1 hiển thị sai trên báo cáo')) AS m(old_subject,subject)
WHERE i.subject=m.old_subject;
UPDATE technical_incidents SET subject=replace(subject,'[TEST] Sự cố phân trang','Cảnh báo hiệu năng số') WHERE subject LIKE '[TEST] Sự cố phân trang%';
COMMIT;

-- Kết quả phải đều bằng 0
SELECT (SELECT count(*) FROM users WHERE email LIKE '%test.kizuna%') AS email_cu,
       (SELECT count(*) FROM technical_incidents WHERE subject LIKE '%[TEST]%') AS su_co_cu,
       (SELECT count(*) FROM membership_plans WHERE plan_name LIKE '[TEST]%') AS goi_cu,
       (SELECT count(*) FROM payment_transactions WHERE payment_gateway='TEST') AS giao_dich_cu;
