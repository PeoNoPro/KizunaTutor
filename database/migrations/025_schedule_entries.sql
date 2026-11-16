-- Lịch chung chống trùng (học thử + buổi của khóa kết nối) — PostgreSQL TỰ CHẶN mọi chồng lấn thời gian.
--
-- Mỗi buổi đang chiếm thời gian (học thử đang chờ / đã nhận, buổi của yêu cầu kết nối đang chờ / đã nhận)
-- là một dòng trong schedule_entries với khoảng thời gian thật [bắt đầu, kết thúc).
-- Hai ràng buộc EXCLUDE bảo đảm:
--   * một gia sư không thể có hai buổi chồng lên nhau (kể cả chồng một phần, ví dụ 10:00–12:00 và 11:00–11:30);
--   * một học viên không thể có hai buổi chồng lên nhau (kể cả với hai gia sư khác nhau).
-- Hai buổi nối tiếp (một buổi kết thúc 10:00, buổi kia bắt đầu 10:00) KHÔNG tính là trùng.
--
-- Bảng được TRIGGER tự đồng bộ từ trial_bookings, connection_requests, connection_request_sessions,
-- nên mọi luồng (đặt, nhận, từ chối, hủy, đổi lịch, hết hạn) đều đi qua ràng buộc, không luồng nào quên được.
-- Vi phạm ràng buộc trả lỗi SQLSTATE 23P01; backend đổi thành HTTP 409 "Giờ này vừa có lịch khác".
--
-- Trạng thái: HOLD (đang chờ, vẫn giữ chỗ) | CONFIRMED (đã nhận) | RELEASED (đã nhả chỗ: từ chối, hủy, hết hạn, đã học xong).
-- Lịch lớp (class_schedules) chưa đưa vào bảng này; vẫn kiểm tra trong code như trước.
--
-- Chạy SAU 021_connection_request_sessions.sql và 022_trial_bookings_off_schedule.sql.
-- Idempotent: chạy lại nhiều lần không lỗi (CREATE ... IF NOT EXISTS / OR REPLACE, backfill bỏ qua dòng đã có).

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS schedule_entries (
    entry_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kind        VARCHAR(10) NOT NULL CHECK (kind IN ('TRIAL', 'COURSE')),
    -- TRIAL: trial_bookings.booking_id; COURSE: connection_request_sessions.session_id
    source_id   UUID NOT NULL,
    tutor_id    UUID NOT NULL,
    learner_id  UUID,
    time_range  TSTZRANGE NOT NULL,
    status      VARCHAR(10) NOT NULL CHECK (status IN ('HOLD', 'CONFIRMED', 'RELEASED')),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_schedule_entries_source UNIQUE (kind, source_id),
    CONSTRAINT ex_schedule_entries_tutor_overlap
        EXCLUDE USING gist (tutor_id WITH =, time_range WITH &&) WHERE (status IN ('HOLD', 'CONFIRMED')),
    CONSTRAINT ex_schedule_entries_learner_overlap
        EXCLUDE USING gist (learner_id WITH =, time_range WITH &&)
        WHERE (status IN ('HOLD', 'CONFIRMED') AND learner_id IS NOT NULL)
);

-- ---------------------------------------------------------------- trạng thái nguồn → trạng thái giữ chỗ

CREATE OR REPLACE FUNCTION schedule_status_of_trial(s TEXT) RETURNS VARCHAR AS $$
    SELECT CASE WHEN s = 'PENDING' THEN 'HOLD'
                WHEN s IN ('CONFIRMED', 'RESCHEDULED') THEN 'CONFIRMED'
                ELSE 'RELEASED' END;
$$ LANGUAGE sql IMMUTABLE;

CREATE OR REPLACE FUNCTION schedule_status_of_connection(request_type TEXT, s TEXT) RETURNS VARCHAR AS $$
    SELECT CASE WHEN request_type <> 'DIRECT' THEN 'RELEASED'
                WHEN s = 'PENDING' THEN 'HOLD'
                WHEN s = 'ACCEPTED' THEN 'CONFIRMED'
                ELSE 'RELEASED' END;
$$ LANGUAGE sql IMMUTABLE;

-- ---------------------------------------------------------------- học thử

CREATE OR REPLACE FUNCTION trg_schedule_sync_trial() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM schedule_entries WHERE kind = 'TRIAL' AND source_id = OLD.booking_id;
        RETURN OLD;
    END IF;
    INSERT INTO schedule_entries (kind, source_id, tutor_id, learner_id, time_range, status, updated_at)
    VALUES ('TRIAL', NEW.booking_id, NEW.tutor_id, NEW.learner_id,
            tstzrange(NEW.scheduled_start_time, NEW.scheduled_end_time),
            schedule_status_of_trial(NEW.booking_status::text), now())
    ON CONFLICT (kind, source_id) DO UPDATE
        SET tutor_id = EXCLUDED.tutor_id, learner_id = EXCLUDED.learner_id,
            time_range = EXCLUDED.time_range, status = EXCLUDED.status, updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS schedule_sync_trial ON trial_bookings;
CREATE TRIGGER schedule_sync_trial
    AFTER INSERT OR UPDATE OF booking_status, scheduled_start_time, scheduled_end_time, tutor_id, learner_id OR DELETE
    ON trial_bookings FOR EACH ROW EXECUTE FUNCTION trg_schedule_sync_trial();

-- ---------------------------------------------------------------- buổi của khóa kết nối

CREATE OR REPLACE FUNCTION trg_schedule_sync_session() RETURNS TRIGGER AS $$
DECLARE
    cr RECORD;
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM schedule_entries WHERE kind = 'COURSE' AND source_id = OLD.session_id;
        RETURN OLD;
    END IF;
    SELECT tutor_id, learner_id, request_type::text AS request_type, status::text AS status
      INTO cr FROM connection_requests WHERE request_id = NEW.request_id;
    INSERT INTO schedule_entries (kind, source_id, tutor_id, learner_id, time_range, status, updated_at)
    VALUES ('COURSE', NEW.session_id, cr.tutor_id, cr.learner_id,
            tstzrange((NEW.session_date + NEW.start_time) AT TIME ZONE 'Asia/Ho_Chi_Minh',
                      (NEW.session_date + NEW.end_time) AT TIME ZONE 'Asia/Ho_Chi_Minh'),
            schedule_status_of_connection(cr.request_type, cr.status), now())
    ON CONFLICT (kind, source_id) DO UPDATE
        SET time_range = EXCLUDED.time_range, status = EXCLUDED.status, updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS schedule_sync_session ON connection_request_sessions;
CREATE TRIGGER schedule_sync_session
    AFTER INSERT OR UPDATE OF session_date, start_time, end_time OR DELETE
    ON connection_request_sessions FOR EACH ROW EXECUTE FUNCTION trg_schedule_sync_session();

CREATE OR REPLACE FUNCTION trg_schedule_sync_connection() RETURNS TRIGGER AS $$
BEGIN
    UPDATE schedule_entries e
       SET status = schedule_status_of_connection(NEW.request_type::text, NEW.status::text), updated_at = now()
      FROM connection_request_sessions s
     WHERE s.request_id = NEW.request_id AND e.kind = 'COURSE' AND e.source_id = s.session_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS schedule_sync_connection ON connection_requests;
CREATE TRIGGER schedule_sync_connection
    AFTER UPDATE OF status ON connection_requests
    FOR EACH ROW EXECUTE FUNCTION trg_schedule_sync_connection();

-- ---------------------------------------------------------------- dữ liệu đang có (chỉ các buổi đang giữ chỗ, chưa kết thúc)
-- ON CONFLICT DO NOTHING KHÔNG ghi cột: bỏ qua cả dòng đã có (UNIQUE) lẫn dòng trùng giờ với dòng khác (EXCLUDE, 23P01).
-- Dữ liệu cũ (trước khi có ràng buộc) có thể đang chồng giờ: migration không dừng giữa chừng, các dòng bị bỏ qua
-- được đếm và báo ở cuối file để người chạy dọn.

INSERT INTO schedule_entries (kind, source_id, tutor_id, learner_id, time_range, status)
SELECT 'TRIAL', tb.booking_id, tb.tutor_id, tb.learner_id,
       tstzrange(tb.scheduled_start_time, tb.scheduled_end_time), schedule_status_of_trial(tb.booking_status::text)
  FROM trial_bookings tb
 WHERE tb.booking_status::text IN ('PENDING', 'CONFIRMED', 'RESCHEDULED')
   AND tb.scheduled_end_time > now()
ON CONFLICT DO NOTHING;

INSERT INTO schedule_entries (kind, source_id, tutor_id, learner_id, time_range, status)
SELECT 'COURSE', s.session_id, cr.tutor_id, cr.learner_id,
       tstzrange((s.session_date + s.start_time) AT TIME ZONE 'Asia/Ho_Chi_Minh',
                 (s.session_date + s.end_time) AT TIME ZONE 'Asia/Ho_Chi_Minh'),
       schedule_status_of_connection(cr.request_type::text, cr.status::text)
  FROM connection_request_sessions s
  JOIN connection_requests cr ON cr.request_id = s.request_id
 WHERE cr.request_type::text = 'DIRECT' AND cr.status::text IN ('PENDING', 'ACCEPTED')
   AND (s.session_date + s.end_time) > (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')
ON CONFLICT DO NOTHING;

-- Báo số buổi bị bỏ qua vì trùng giờ. Các buổi này chưa có dòng giữ chỗ: lần lưu sau (đổi lịch, đổi link học…)
-- trigger sẽ thêm dòng và báo trùng giờ, nên cần hủy / dời một trong hai buổi. Xem danh sách bằng hai câu
-- SELECT trong khối dưới (bỏ phần COUNT).
DO $$
DECLARE
    skipped_trials   INT;
    skipped_sessions INT;
BEGIN
    SELECT COUNT(*) INTO skipped_trials
      FROM trial_bookings tb
     WHERE tb.booking_status::text IN ('PENDING', 'CONFIRMED', 'RESCHEDULED')
       AND tb.scheduled_end_time > now()
       AND NOT EXISTS (SELECT 1 FROM schedule_entries e WHERE e.kind = 'TRIAL' AND e.source_id = tb.booking_id);
    SELECT COUNT(*) INTO skipped_sessions
      FROM connection_request_sessions s
      JOIN connection_requests cr ON cr.request_id = s.request_id
     WHERE cr.request_type::text = 'DIRECT' AND cr.status::text IN ('PENDING', 'ACCEPTED')
       AND (s.session_date + s.end_time) > (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')
       AND NOT EXISTS (SELECT 1 FROM schedule_entries e WHERE e.kind = 'COURSE' AND e.source_id = s.session_id);
    IF skipped_trials + skipped_sessions > 0 THEN
        RAISE WARNING '025_schedule_entries: bỏ qua % buổi học thử và % buổi của khóa kết nối vì trùng giờ với lịch khác',
            skipped_trials, skipped_sessions;
    END IF;
END $$;
