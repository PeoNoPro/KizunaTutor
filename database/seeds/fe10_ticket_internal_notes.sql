-- Ghi chú nội bộ cho yêu cầu hỗ trợ (2026-11-12). Chạy SAU fe10_support_ticket.sql.
-- Chạy lại được nhiều lần; không xóa hay sửa dữ liệu cũ.
-- Ghi chú lưu ở bảng riêng, không nằm trong ticket_messages, nên người gửi yêu cầu không bao giờ đọc được:
-- danh sách tin nhắn, bản xem trước, thông báo, email và thời gian chờ phản hồi đều không đụng tới bảng này.
BEGIN;
CREATE TABLE IF NOT EXISTS ticket_internal_notes(
  note_id uuid PRIMARY KEY,
  ticket_id uuid NOT NULL REFERENCES support_tickets(ticket_id),
  author_id uuid NOT NULL REFERENCES users(user_id),
  note_text text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_ticket_internal_notes_text CHECK (length(btrim(note_text)) BETWEEN 1 AND 5000)
);
CREATE INDEX IF NOT EXISTS idx_ticket_internal_notes_ticket ON ticket_internal_notes(ticket_id,created_at);
COMMIT;
