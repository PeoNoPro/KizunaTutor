-- FE-03 Tutor Verification
-- Muc dich: luu truc tiep noi dung giay to xac minh trong DB (giong FE-06 learning_materials.file_data)
-- de moi may chay backend dung chung DB deu xem duoc tep. Truoc day tep nam tren o cung cua may tai len.
-- file_url (VARCHAR 500, NOT NULL) van giu khoa tep dang <uuid>.<pdf|png|jpg> de xac dinh loai tep.
-- Ban ghi cu co file_data = NULL van doc tu o cung (giai doan chuyen tiep).
-- Nguoi phu trach FE-03 tu chay tren Supabase bang DBeaver.
-- Idempotent: chay lai nhieu lan khong gay loi, khong xoa du lieu.

ALTER TABLE tutor_credentials ADD COLUMN IF NOT EXISTS file_data BYTEA;
