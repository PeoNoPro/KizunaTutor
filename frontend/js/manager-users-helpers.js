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

export const REASON_MAX = 1000;
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
 * Kiểm tra biểu mẫu đổi trạng thái trước khi gửi. Trả { valid, errors: { status?, reason?, message? }, payload }.
 * payload đã cắt khoảng trắng; lời nhắn trống gửi null.
 */
export function validateStatusForm({ status = '', currentStatus = '', reason = '', message = '' } = {}) {
  const errors = {};
  const cleanReason = String(reason).trim();
  const cleanMessage = String(message).trim();
  if (!status) errors.status = 'Vui lòng chọn trạng thái mới.';
  else if (status === currentStatus) errors.status = 'Tài khoản đã ở trạng thái này.';
  if (!cleanReason) errors.reason = 'Vui lòng nhập lý do nội bộ.';
  else if (cleanReason.length > REASON_MAX) errors.reason = 'Lý do nội bộ tối đa 1.000 ký tự.';
  if (cleanMessage.length > MESSAGE_MAX) errors.message = 'Lời nhắn gửi người dùng tối đa 1.000 ký tự.';
  return {
    valid: Object.keys(errors).length === 0,
    errors,
    payload: { status, expectedStatus: currentStatus, reason: cleanReason, message: cleanMessage || null }
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
