-- FE-07 Homework, Assessment & Progress Tracking
-- Muc dich: luu noi dung tep bai nop truc tiep trong DB (giong learning_materials.file_data cua FE-06).
-- attachment_url (VARCHAR 500) luu ten tep goc da lam sach.
-- Nguoi phu trach FE-07 da tu chay lenh nay tren Supabase ngay 2026-11-03 (da kiem tra: cot ton tai, cho phep NULL).
-- Idempotent: chay lai nhieu lan khong gay loi, khong xoa du lieu.

ALTER TABLE assignment_submissions ADD COLUMN IF NOT EXISTS file_data BYTEA;
