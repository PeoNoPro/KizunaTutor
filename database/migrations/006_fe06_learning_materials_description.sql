-- FE-06 Classroom & Learning Materials Management
-- Muc dich: bo sung cot mo ta cho tai lieu hoc tap (UC-65 Upload / UC-61 View Learning Materials).
-- Thay doi duy nhat, da duoc nguoi phu trach FE-06 phe duyet ngay 2026-11-03.
-- Idempotent: chay lai nhieu lan khong gay loi, khong xoa du lieu.

ALTER TABLE learning_materials ADD COLUMN IF NOT EXISTS description TEXT;
