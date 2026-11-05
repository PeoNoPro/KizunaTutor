-- Xóa toàn bộ dữ liệu mẫu do fe11_test_data.sql tạo, gồm cả thao tác đã làm trên các tài khoản đó.
-- Chỉ đụng tài khoản đuôi @kizunatutor.invalid, sự cố có mô tả "Dữ liệu mẫu FE-11" và hai gói mẫu đang tắt.
BEGIN;
DELETE FROM staff_role_change_requests WHERE target_user_id IN (SELECT user_id FROM users WHERE email LIKE '%@kizunatutor.invalid');
DELETE FROM staff_account_audit_events WHERE target_user_id IN (SELECT user_id FROM users WHERE email LIKE '%@kizunatutor.invalid');
DELETE FROM technical_incident_events WHERE incident_id IN (SELECT incident_id FROM technical_incidents WHERE description LIKE 'Dữ liệu mẫu FE-11%');
DELETE FROM incident_components WHERE incident_id IN (SELECT incident_id FROM technical_incidents WHERE description LIKE 'Dữ liệu mẫu FE-11%');
DELETE FROM technical_incidents WHERE description LIKE 'Dữ liệu mẫu FE-11%';
DELETE FROM payment_transactions WHERE user_id IN (SELECT user_id FROM users WHERE email LIKE '%@kizunatutor.invalid');
DELETE FROM membership_plans m WHERE m.plan_name IN ('Gói Tháng','Gói Năm') AND m.is_active=false AND NOT EXISTS (SELECT 1 FROM payment_transactions p WHERE p.plan_id=m.plan_id) AND NOT EXISTS (SELECT 1 FROM tutor_memberships t WHERE t.plan_id=m.plan_id);
DELETE FROM tutor_credentials WHERE tutor_id IN (SELECT t.tutor_id FROM tutors t JOIN users u ON u.user_id=t.user_id WHERE u.email LIKE '%@kizunatutor.invalid');
DELETE FROM tutors WHERE user_id IN (SELECT user_id FROM users WHERE email LIKE '%@kizunatutor.invalid');
DELETE FROM users WHERE email LIKE '%@kizunatutor.invalid';
COMMIT;

-- Kết quả phải đều bằng 0
SELECT (SELECT count(*) FROM users WHERE email LIKE '%@kizunatutor.invalid') AS tai_khoan,
       (SELECT count(*) FROM technical_incidents WHERE description LIKE 'Dữ liệu mẫu FE-11%') AS su_co;
