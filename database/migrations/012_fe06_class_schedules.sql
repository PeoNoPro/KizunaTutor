-- FE-06: Lịch học cố định hằng tuần của lớp học (learning_spaces).
-- Gia sư chọn lịch khi tạo lớp; lịch được sửa hoặc thêm buổi, KHÔNG xóa (theo nghiệp vụ).
-- day_of_week: 1 = Thứ Hai ... 7 = Chủ nhật (ISO-8601, giống tutor_availabilities).
-- Chạy lại nhiều lần vẫn an toàn.

CREATE TABLE IF NOT EXISTS class_schedules (
    schedule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES learning_spaces(space_id) ON DELETE CASCADE ON UPDATE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_class_schedule_time CHECK (start_time < end_time)
);

CREATE INDEX IF NOT EXISTS idx_class_schedules_space ON class_schedules(space_id);

DROP TRIGGER IF EXISTS trg_class_schedules_updated_at ON class_schedules;
CREATE TRIGGER trg_class_schedules_updated_at BEFORE UPDATE ON class_schedules
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
