/**
 * KizunaTutor - Hàm thuần cho trang Quản lý người dùng (FE-14: UC-20, UC-21, UC-12). Không đụng DOM, test bằng node --test.
 * Nhãn trạng thái hiển thị luôn lấy từ API; ở đây chỉ chọn màu và kiểm tra biểu mẫu.
 */

export const CATEGORIES = [
  { value: 'TUTOR', label: 'Gia sư' },
  { value: 'LEARNER', label: 'Học viên' }
];

/** Bộ lọc trạng thái (giá trị gửi API → nhãn hiển thị). */
export const STATUS_FILTERS = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'PENDING', label: 'Chờ kích hoạt' },
  { value: 'ACTIVE', label: 'Đang hoạt động' },
  { value: 'SUSPENDED', label: 'Tạm khóa' },
  { value: 'BANNED', label: 'Khóa vĩnh viễn' }
];

export const MESSAGE_MAX = 1000;
export const KEYWORD_MAX = 100;

/** Màu nhãn trạng thái tài khoản: success | warning | danger | neutral. */
export function accountStatusTone(status) {
  if (status === 'ACTIVE') return 'success';
  if (status === 'PENDING') return 'warning';
  if (status === 'SUSPENDED' || status === 'BANNED') return 'danger';
  return 'neutral';
}

/** Thông báo khi danh sách rỗng: có điều kiện tìm thì theo SRS UC-20 AF-1, không thì báo nhóm chưa có ai. */
export function emptyMessage(category, { keyword = '', status = '' } = {}) {
  if (String(keyword).trim() || status) return 'Không tìm thấy người dùng phù hợp với điều kiện tìm kiếm.';
  return category === 'LEARNER' ? 'Chưa có học viên nào đăng ký.' : 'Chưa có gia sư nào đăng ký.';
}

/**
 * Kiểm tra biểu mẫu đổi trạng thái trước khi gửi. Trả { valid, errors: { status?, message? }, payload }.
 * payload đã cắt khoảng trắng; lời nhắn trống gửi null.
 */
export function validateStatusForm({ status = '', currentStatus = '', message = '' } = {}) {
  const errors = {};
  const cleanMessage = String(message).trim();
  if (!status) errors.status = 'Vui lòng chọn trạng thái mới.';
  else if (status === currentStatus) errors.status = 'Tài khoản đã ở trạng thái này.';
  if (cleanMessage.length > MESSAGE_MAX) errors.message = 'Lời nhắn gửi người dùng tối đa 1.000 ký tự.';
  return {
    valid: Object.keys(errors).length === 0,
    errors,
    payload: { status, expectedStatus: currentStatus, message: cleanMessage || null }
  };
}

/** Câu xác nhận theo trạng thái mới, để Quản lý biết hệ quả trước khi bấm. */
export function statusConsequence(status) {
  if (status === 'SUSPENDED') return 'Người dùng sẽ bị đăng xuất ngay và không thể đăng nhập cho đến khi được mở lại.';
  if (status === 'BANNED') return 'Người dùng sẽ bị đăng xuất ngay và không thể đăng nhập nữa.';
  if (status === 'ACTIVE') return 'Người dùng sẽ đăng nhập và sử dụng hệ thống lại như bình thường.';
  return '';
}

/** Giá trị trống hiển thị "Chưa cập nhật". */
export function orDash(value) {
  return value === null || value === undefined || String(value).trim() === '' ? 'Chưa cập nhật' : String(value);
}

/** Không đăng nhập từ số ngày này trở lên thì đánh dấu "lâu không hoạt động". */
export const INACTIVE_DAYS = 30;

/**
 * Lần đăng nhập gần nhất → { text, idle }.
 * text: "Chưa ghi nhận" | "Vừa xong" | "15 phút trước" | "3 giờ trước" | "12 ngày trước" | "2 tháng trước" | "1 năm trước".
 * idle = true khi đã INACTIVE_DAYS ngày trở lên không đăng nhập.
 */
export function lastLoginInfo(value, now = new Date()) {
  const time = value ? new Date(value).getTime() : NaN;
  if (!Number.isFinite(time)) return { text: 'Chưa ghi nhận', idle: false };
  const minutes = Math.max(0, Math.floor((now.getTime() - time) / 60000));
  const days = Math.floor(minutes / 1440);
  let text;
  if (minutes < 1) text = 'Vừa xong';
  else if (minutes < 60) text = `${minutes} phút trước`;
  else if (minutes < 1440) text = `${Math.floor(minutes / 60)} giờ trước`;
  else if (days < 60) text = `${days} ngày trước`;
  else if (days < 365) text = `${Math.floor(days / 30)} tháng trước`;
  else text = `${Math.floor(days / 365)} năm trước`;
  return { text, idle: days >= INACTIVE_DAYS };
}

/**
 * Nút đổi trạng thái theo trạng thái hiện tại (Quản lý chỉ Tạm khóa hoặc Mở lại; không chọn trạng thái):
 * Đang hoạt động → "Tạm khóa tài khoản"; Tạm khóa / Khóa vĩnh viễn → "Mở lại tài khoản"; Chờ kích hoạt → không có nút.
 */
export function lockAction(accountStatus) {
  if (accountStatus === 'ACTIVE') {
    return { target: 'SUSPENDED', label: 'Tạm khóa tài khoản', confirm: 'Xác nhận tạm khóa', tone: 'danger' };
  }
  if (accountStatus === 'SUSPENDED' || accountStatus === 'BANNED') {
    return { target: 'ACTIVE', label: 'Mở lại tài khoản', confirm: 'Xác nhận mở lại', tone: 'primary' };
  }
  return null;
}
