-- 031: Nội dung tệp chuyển sang Cloudflare R2 (khi app.r2.enabled=true).
-- Tệp mới: nội dung nằm trên R2, cột BYTEA trong DB để NULL. Tệp cũ vẫn giữ trong DB và vẫn đọc được.
-- Khóa R2: materials/{file_id}, assignments/{file_id}, submissions/{file_id}, tickets/{attachment_id},
--          verification/{file_url}, avatars/{tên tệp}.
-- Chạy lại nhiều lần vẫn an toàn.

ALTER TABLE learning_material_files ALTER COLUMN file_data DROP NOT NULL;
ALTER TABLE assignment_files ALTER COLUMN file_data DROP NOT NULL;
ALTER TABLE submission_files ALTER COLUMN file_data DROP NOT NULL;
-- ck_ticket_attachments_size (octet_length(content)=size_bytes) cho qua khi content NULL
ALTER TABLE ticket_attachments ALTER COLUMN content DROP NOT NULL;
