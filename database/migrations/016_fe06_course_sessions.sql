-- FE-06: Khóa học có số buổi giới hạn (lịch không lặp vô hạn) + hủy / thêm buổi riêng trong một tuần.
-- Chạy SAU migration 015. Chạy lại nhiều lần vẫn an toàn.
--
-- 1) learning_spaces.course_start_date: ngày bắt đầu khóa (gia sư chọn khi tạo lớp).
--    learning_spaces.total_sessions: tổng số buổi của khóa; NULL = lớp tạo trước đây, lịch vẫn lặp hằng tuần.
-- 2) class_schedules.active_from / active_until: khoảng thời gian một buổi hằng tuần còn hiệu lực.
--    Xóa / đổi giờ buổi hằng tuần chỉ áp dụng từ thời điểm đó trở đi; các buổi đã diễn ra vẫn được giữ và tính vào khóa.
-- 3) class_session_changes.cancelled: gia sư hủy riêng buổi của một tuần (buổi bị hủy không tính, khóa tự bù thêm buổi ở cuối).
-- 4) class_extra_sessions: buổi gia sư thêm riêng trong một tuần (được tính vào tổng số buổi).

-- 1) Khóa học
ALTER TABLE learning_spaces ADD COLUMN IF NOT EXISTS course_start_date DATE;
ALTER TABLE learning_spaces ADD COLUMN IF NOT EXISTS total_sessions INTEGER;
UPDATE learning_spaces
SET course_start_date = (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
WHERE course_start_date IS NULL;
ALTER TABLE learning_spaces DROP CONSTRAINT IF EXISTS chk_learning_space_total_sessions;
ALTER TABLE learning_spaces ADD CONSTRAINT chk_learning_space_total_sessions
    CHECK (total_sessions IS NULL OR total_sessions BETWEEN 1 AND 500);

-- 2) Thời gian hiệu lực của buổi hằng tuần
ALTER TABLE class_schedules ADD COLUMN IF NOT EXISTS active_from TIMESTAMPTZ;
ALTER TABLE class_schedules ADD COLUMN IF NOT EXISTS active_until TIMESTAMPTZ;
UPDATE class_schedules cs
SET active_from = (ls.course_start_date::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh')
FROM learning_spaces ls
WHERE ls.space_id = cs.space_id AND cs.active_from IS NULL;
UPDATE class_schedules SET active_from = created_at WHERE active_from IS NULL;
ALTER TABLE class_schedules ALTER COLUMN active_from SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE class_schedules ALTER COLUMN active_from SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_class_schedules_space_active ON class_schedules(space_id) WHERE active_until IS NULL;

-- 3) Hủy riêng buổi của một tuần (dòng hủy không cần ngày/giờ mới)
ALTER TABLE class_session_changes ADD COLUMN IF NOT EXISTS cancelled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE class_session_changes ALTER COLUMN new_date DROP NOT NULL;
ALTER TABLE class_session_changes ALTER COLUMN start_time DROP NOT NULL;
ALTER TABLE class_session_changes ALTER COLUMN end_time DROP NOT NULL;
ALTER TABLE class_session_changes DROP CONSTRAINT IF EXISTS chk_class_session_change_time;
ALTER TABLE class_session_changes ADD CONSTRAINT chk_class_session_change_time CHECK (
    cancelled OR (new_date IS NOT NULL AND start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)
);

-- 4) Buổi thêm riêng trong một tuần
CREATE TABLE IF NOT EXISTS class_extra_sessions (
    extra_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES learning_spaces(space_id) ON DELETE CASCADE ON UPDATE CASCADE,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_class_extra_session_time CHECK (start_time < end_time)
);
CREATE INDEX IF NOT EXISTS idx_class_extra_sessions_space_date ON class_extra_sessions(space_id, session_date);

DROP TRIGGER IF EXISTS trg_class_extra_sessions_updated_at ON class_extra_sessions;
CREATE TRIGGER trg_class_extra_sessions_updated_at BEFORE UPDATE ON class_extra_sessions
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
