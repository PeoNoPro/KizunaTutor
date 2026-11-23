-- 037 (FE-14): lần đăng nhập gần nhất của mỗi tài khoản, để Quản lý biết tài khoản đã bao lâu không vào hệ thống.
-- Bảng riêng thay vì thêm cột vào users: users có trigger trg_users_updated_at, ghi vào users mỗi lần đăng nhập
-- sẽ làm đổi updated_at (ngày cập nhật tài khoản) dù tài khoản không có thay đổi gì.
-- Chạy lại nhiều lần vẫn an toàn. Không sửa bảng nào khác.
--
-- Mỗi tài khoản một dòng; mỗi lần đăng nhập thành công (mật khẩu, Google, hoặc vừa đăng ký xong) ghi đè last_login_at.
-- Tài khoản chưa đăng nhập lần nào kể từ khi có bảng này thì chưa có dòng (trang hiện "Chưa ghi nhận").

CREATE TABLE IF NOT EXISTS user_login_activity (
    user_id UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    last_login_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_login_activity_time ON user_login_activity(last_login_at);
