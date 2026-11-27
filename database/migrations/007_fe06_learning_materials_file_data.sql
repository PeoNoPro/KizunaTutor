-- FE-06 Classroom & Learning Materials Management
-- Muc dich: luu truc tiep noi dung tep tai lieu trong DB (thay cho Supabase Storage).
-- file_url (VARCHAR 500, NOT NULL) duoc dung de luu ten tep goc da lam sach.
-- Da duoc nguoi phu trach FE-06 phe duyet va tu chay tren Supabase ngay 2026-11-03.
-- Idempotent: chay lai nhieu lan khong gay loi, khong xoa du lieu.

ALTER TABLE learning_materials ADD COLUMN IF NOT EXISTS file_data BYTEA;
