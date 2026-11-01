-- ====================================================================================================
-- MIGRATION: BỔ SUNG MÃ GIA SƯ TỰ ĐỘNG TĂNG (tutor_code) CHO BẢNG tutors
-- Khởi tạo sequence tự động tăng từ 10001
-- An toàn: Không xóa bảng, không xóa dữ liệu, giữ nguyên UUID tutor_id và toàn bộ khóa ngoại
-- ====================================================================================================

-- 1. Tạo Sequence tự tăng bắt đầu từ 10001 nếu chưa tồn tại
CREATE SEQUENCE IF NOT EXISTS tutor_code_seq
    START WITH 10001
    INCREMENT BY 1
    MINVALUE 10001
    NO MAXVALUE
    CACHE 1;

-- 2. Bổ sung cột tutor_code kiểu BIGINT vào bảng tutors nếu chưa có
ALTER TABLE tutors 
    ADD COLUMN IF NOT EXISTS tutor_code BIGINT;

-- 3. Gán giá trị mặc định cho cột tutor_code từ sequence
ALTER TABLE tutors 
    ALTER COLUMN tutor_code SET DEFAULT nextval('tutor_code_seq');

-- 4. Tự động cấp mã cho các gia sư hiện tại chưa có mã theo thứ tự khởi tạo
UPDATE tutors 
SET tutor_code = nextval('tutor_code_seq') 
WHERE tutor_code IS NULL;

-- 5. Thiết lập ràng buộc NOT NULL
ALTER TABLE tutors 
    ALTER COLUMN tutor_code SET NOT NULL;

-- 6. Thiết lập ràng buộc UNIQUE (duy nhất)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_tutors_tutor_code'
    ) THEN
        ALTER TABLE tutors ADD CONSTRAINT uq_tutors_tutor_code UNIQUE (tutor_code);
    END IF;
END $$;

-- 7. Tạo chỉ mục để tối ưu tốc độ tra cứu theo mã gia sư
CREATE INDEX IF NOT EXISTS idx_tutors_tutor_code ON tutors(tutor_code);

-- 8. Đồng bộ sequence để đảm bảo mã cấp tiếp theo luôn lớn hơn giá trị lớn nhất hiện có
SELECT setval('tutor_code_seq', GREATEST(COALESCE((SELECT MAX(tutor_code) FROM tutors), 10000), 10000));

