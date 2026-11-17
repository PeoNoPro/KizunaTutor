-- Yêu cầu ĐANG CHỜ (học thử / kết nối) KHÔNG còn giữ chỗ: chỉ lịch đã được gia sư CHẤP NHẬN mới chiếm giờ.
--   * Nhiều học viên được xin cùng một giờ; gia sư nhận ai thì giờ đó mới bị khóa.
--   * Gia sư nhận một buổi học thử → các yêu cầu học thử khác trùng giờ tự hủy (xử lý trong backend).
-- Ràng buộc chống trùng của schedule_entries (migration 025) chỉ áp dụng cho dòng HOLD / CONFIRMED,
-- nên ở đây đổi: đang chờ → RELEASED (không giữ chỗ), đã chấp nhận → CONFIRMED.
-- Chạy SAU 027_connection_contact_share.sql. Idempotent.

CREATE OR REPLACE FUNCTION schedule_status_of_trial(s TEXT) RETURNS VARCHAR AS $$
    SELECT CASE WHEN s IN ('CONFIRMED', 'RESCHEDULED') THEN 'CONFIRMED'
                ELSE 'RELEASED' END;
$$ LANGUAGE sql IMMUTABLE;

CREATE OR REPLACE FUNCTION schedule_status_of_connection(request_type TEXT, s TEXT) RETURNS VARCHAR AS $$
    SELECT CASE WHEN request_type = 'DIRECT' AND s = 'ACCEPTED' THEN 'CONFIRMED'
                ELSE 'RELEASED' END;
$$ LANGUAGE sql IMMUTABLE;

-- Tính lại trạng thái các dòng đang có theo quy tắc mới
UPDATE schedule_entries e
   SET status = schedule_status_of_trial(tb.booking_status::text), updated_at = now()
  FROM trial_bookings tb
 WHERE e.kind = 'TRIAL' AND e.source_id = tb.booking_id
   AND e.status <> schedule_status_of_trial(tb.booking_status::text);

UPDATE schedule_entries e
   SET status = schedule_status_of_connection(cr.request_type::text, cr.status::text), updated_at = now()
  FROM connection_request_sessions s
  JOIN connection_requests cr ON cr.request_id = s.request_id
 WHERE e.kind = 'COURSE' AND e.source_id = s.session_id
   AND e.status <> schedule_status_of_connection(cr.request_type::text, cr.status::text);
