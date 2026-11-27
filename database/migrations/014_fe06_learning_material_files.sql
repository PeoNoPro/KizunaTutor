-- FE-06: Một tài liệu học tập gồm tiêu đề + mô tả + NHIỀU tệp (tối đa 10 tệp, mỗi tệp tối đa 25 MB — kiểm tra ở service).
-- Trước đây mỗi tài liệu chỉ có 1 tệp (learning_materials.file_data). Tệp cũ được chép sang bảng mới;
-- cột cũ giữ nguyên, không xóa (file_url vẫn lưu tên tệp đầu tiên vì cột NOT NULL). Chạy lại nhiều lần vẫn an toàn.

CREATE TABLE IF NOT EXISTS learning_material_files (
    file_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_id UUID NOT NULL REFERENCES learning_materials(material_id) ON DELETE CASCADE ON UPDATE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_learning_material_files_material ON learning_material_files(material_id);

-- Chép tệp cũ (1 tệp/tài liệu) sang bảng mới
INSERT INTO learning_material_files (material_id, file_name, file_size, file_data, created_at)
SELECT m.material_id, m.file_url, octet_length(m.file_data), m.file_data, m.created_at
FROM learning_materials m
WHERE m.file_data IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM learning_material_files f WHERE f.material_id = m.material_id);
