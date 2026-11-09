-- FE-06: Dời MỘT buổi học chính thức sang ngày/giờ khác trong CÙNG tuần (chỉ buổi của tuần đó, các tuần khác giữ nguyên).
-- Mỗi dòng = một buổi theo lịch hằng tuần (schedule_id + original_date) đã được gia sư dời sang new_date, start_time–end_time.
-- original_date và new_date luôn thuộc cùng một tuần (Thứ Hai – Chủ nhật) — kiểm tra ở tầng ứng dụng.
-- Xóa buổi học hằng tuần (class_schedules) thì các lần dời của buổi đó bị xóa theo.
-- Chạy lại nhiều lần vẫn an toàn.

CREATE TABLE IF NOT EXISTS class_session_changes (
    change_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id UUID NOT NULL REFERENCES class_schedules(schedule_id) ON DELETE CASCADE ON UPDATE CASCADE,
    original_date DATE NOT NULL,
    new_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_class_session_change_time CHECK (start_time < end_time),
    CONSTRAINT uq_class_session_change UNIQUE (schedule_id, original_date)
);

CREATE INDEX IF NOT EXISTS idx_class_session_changes_new_date ON class_session_changes(new_date);

DROP TRIGGER IF EXISTS trg_class_session_changes_updated_at ON class_session_changes;
CREATE TRIGGER trg_class_session_changes_updated_at BEFORE UPDATE ON class_session_changes
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
