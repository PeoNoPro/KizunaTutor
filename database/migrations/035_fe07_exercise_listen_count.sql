-- FE-07: Đếm số lần nghe của câu nghe ở máy chủ (trước đây chỉ đếm trên trình duyệt, xóa localStorage là nghe lại được).
-- Chạy SAU 033_fe07_exercise_audio. Chạy lại nhiều lần vẫn an toàn. Không sửa bảng cũ.
--
-- Mỗi dòng: một câu nghe trong một lượt làm bài.
-- plays: số lần học viên đã bắt đầu nghe (máy chủ cộng 1 mỗi lần cấp link nghe, không vượt số lần gia sư cho phép).
-- last_played_at / resumes: link nghe hết hạn giữa lúc đang phát thì xin lại link mà không tính thêm lần nghe
--   (tối đa 2 lần mỗi lượt nghe, trong 30 phút từ lúc bắt đầu nghe).
-- question_id không có khóa ngoại: câu hỏi của bài đã có lượt làm bị khóa, không bị xóa / tạo lại.

CREATE TABLE IF NOT EXISTS exercise_listen_counts (
    attempt_id UUID NOT NULL REFERENCES exercise_attempts(attempt_id) ON DELETE CASCADE ON UPDATE CASCADE,
    question_id UUID NOT NULL,
    plays INT NOT NULL DEFAULT 0 CHECK (plays >= 0),
    resumes INT NOT NULL DEFAULT 0 CHECK (resumes >= 0),
    last_played_at TIMESTAMPTZ,
    PRIMARY KEY (attempt_id, question_id)
);
