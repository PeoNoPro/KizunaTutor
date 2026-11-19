-- 032: FE-13 Luyện đề chỉ còn chế độ làm theo đề (EXAM); bỏ chế độ trộn ngẫu nhiên (RANDOM).
-- Ràng buộc mới chỉ áp cho dòng thêm/sửa từ nay (NOT VALID): lượt làm RANDOM cũ (nếu có khi chạy thử) được giữ nguyên, không xóa.
-- Chạy lại nhiều lần vẫn an toàn.

ALTER TABLE practice_attempts DROP CONSTRAINT IF EXISTS practice_attempts_mode_check;
ALTER TABLE practice_attempts ADD CONSTRAINT practice_attempts_mode_check CHECK (mode = 'EXAM') NOT VALID;
