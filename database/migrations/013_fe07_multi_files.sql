-- FE-06/FE-07: Nhiều tệp cho đề bài và bài nộp.
-- - assignment_files: các tệp đề bài của một bài tập (tối đa 10 tệp, mỗi tệp tối đa 25 MB — kiểm tra ở service).
-- - submission_files: các tệp bài làm của một bài nộp. Mỗi học viên có MỘT bài nộp cho mỗi bài tập,
--   sửa (thêm/xóa/thay tệp) khi còn hạn và chưa được chấm thay vì nộp lại nhiều lần.
-- Dữ liệu cũ (assignments.file_data, assignment_submissions.file_data — 1 tệp) được chép sang bảng mới;
-- cột cũ giữ nguyên, không xóa. Chạy lại nhiều lần vẫn an toàn.

CREATE TABLE IF NOT EXISTS assignment_files (
    file_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES assignments(assignment_id) ON DELETE CASCADE ON UPDATE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_assignment_files_assignment ON assignment_files(assignment_id);

CREATE TABLE IF NOT EXISTS submission_files (
    file_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES assignment_submissions(submission_id) ON DELETE CASCADE ON UPDATE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_submission_files_submission ON submission_files(submission_id);

-- Chép tệp đề bài cũ (1 tệp/bài tập) sang bảng mới
INSERT INTO assignment_files (assignment_id, file_name, file_size, file_data)
SELECT a.assignment_id, a.attachment_url, octet_length(a.file_data), a.file_data
FROM assignments a
WHERE a.attachment_url IS NOT NULL AND a.file_data IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM assignment_files f WHERE f.assignment_id = a.assignment_id);

-- Chép tệp bài làm cũ (1 tệp/lần nộp) sang bảng mới
INSERT INTO submission_files (submission_id, file_name, file_size, file_data)
SELECT s.submission_id, s.attachment_url, octet_length(s.file_data), s.file_data
FROM assignment_submissions s
WHERE s.attachment_url IS NOT NULL AND s.file_data IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM submission_files f WHERE f.submission_id = s.submission_id);
