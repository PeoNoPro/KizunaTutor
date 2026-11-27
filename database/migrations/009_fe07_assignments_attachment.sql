-- FE-07 Homework, Assessment & Progress Tracking
-- Muc dich: cho phep gia su dinh kem mot tep de bai (PDF, PNG, JPG, MP3; toi da 10 MB) khi giao bai tap.
-- attachment_url (VARCHAR 500) luu ten tep goc da lam sach; file_data (BYTEA) luu noi dung tep
-- (giong learning_materials.file_data cua FE-06 va assignment_submissions.file_data cua FE-07).
-- Idempotent: chay lai nhieu lan khong gay loi, khong xoa du lieu.

ALTER TABLE assignments ADD COLUMN IF NOT EXISTS attachment_url VARCHAR(500);
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS file_data BYTEA;
