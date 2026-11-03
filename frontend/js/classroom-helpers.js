/**
 * KizunaTutor - Hàm tiện ích dùng chung cho FE-06 (Lớp học & Tài liệu học tập).
 * Hàm thuần, không phụ thuộc DOM để kiểm thử được bằng `node --test`.
 */

export const MAX_MATERIAL_SIZE_BYTES = 10 * 1024 * 1024;
export const ALLOWED_MATERIAL_EXTENSIONS = ['pdf', 'png', 'jpg', 'jpeg', 'mp3'];
export const INVALID_FILE_MESSAGE = 'Dung lượng tệp vượt quá giới hạn 10 MB. Định dạng cho phép: PDF, PNG, JPG, MP3.';
export const UNSUPPORTED_FORMAT_MESSAGE = 'Định dạng tệp không được hỗ trợ. Định dạng cho phép: PDF, PNG, JPG, MP3.';
export const MATERIAL_ACCEPT_ATTR = '.pdf,.png,.jpg,.jpeg,.mp3,application/pdf,image/png,image/jpeg,audio/mpeg';

const STATUS_LABELS = {
  ACTIVE: 'Đang hoạt động',
  PAUSED: 'Tạm dừng',
  COMPLETED: 'Đã hoàn thành',
  CLOSED: 'Đã đóng'
};

const STATUS_TONES = {
  ACTIVE: 'active',
  PAUSED: 'paused',
  COMPLETED: 'done',
  CLOSED: 'done'
};

const FILE_TYPE_LABELS = {
  PDF: 'Tài liệu PDF',
  PNG: 'Hình ảnh PNG',
  JPG: 'Hình ảnh JPG',
  MP3: 'Âm thanh MP3'
};

export function classroomStatusLabel(status) {
  return STATUS_LABELS[status] || 'Không xác định';
}

export function classroomStatusTone(status) {
  return STATUS_TONES[status] || 'done';
}

export function fileTypeLabel(fileType) {
  return FILE_TYPE_LABELS[fileType] || 'Tệp tài liệu';
}

export function formatDateVi(value) {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export function fileExtension(name) {
  if (!name || typeof name !== 'string') return '';
  const dot = name.lastIndexOf('.');
  if (dot < 0 || dot === name.length - 1) return '';
  return name.slice(dot + 1).toLowerCase();
}

/**
 * Kiểm tra sơ bộ phía trình duyệt (máy chủ vẫn kiểm tra lại nội dung tệp).
 * @returns {string|null} thông điệp lỗi tiếng Việt hoặc null nếu hợp lệ
 */
export function validateMaterialFile(file, { required = true } = {}) {
  if (!file) return required ? 'Vui lòng chọn tệp tài liệu.' : null;
  if (!ALLOWED_MATERIAL_EXTENSIONS.includes(fileExtension(file.name))) return UNSUPPORTED_FORMAT_MESSAGE;
  if (typeof file.size !== 'number' || file.size <= 0) return 'Tệp đã chọn không có nội dung. Vui lòng chọn tệp khác.';
  if (file.size > MAX_MATERIAL_SIZE_BYTES) return INVALID_FILE_MESSAGE;
  return null;
}

/** @returns {{title?: string, description?: string}} lỗi theo trường, rỗng nếu hợp lệ */
export function validateMaterialFields({ title, description }) {
  const errors = {};
  const t = (title || '').trim();
  if (!t) errors.title = 'Vui lòng nhập tiêu đề tài liệu.';
  else if (t.length > 150) errors.title = 'Tiêu đề tài liệu tối đa 150 ký tự.';
  if ((description || '').trim().length > 1000) errors.description = 'Mô tả tối đa 1000 ký tự.';
  return errors;
}

export function validateClassroomTitle(title) {
  const t = (title || '').trim();
  if (!t) return 'Vui lòng nhập tên lớp học.';
  if (t.length > 150) return 'Tên lớp học tối đa 150 ký tự.';
  return null;
}

/**
 * Chuyển lỗi bất kỳ thành thông điệp thân thiện; không bao giờ hiển thị mã lỗi kỹ thuật.
 * Máy chủ FE-06 đã trả message tiếng Việt nên ưu tiên dùng khi có.
 */
export function friendlyErrorMessage(err) {
  if (err && typeof err.userMessage === 'string' && err.userMessage.trim()) return err.userMessage;
  if (err && err.status === 401) return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
  if (err && err.status === 403) return 'Bạn không có quyền thực hiện thao tác này.';
  if (err && err.status === 404) return 'Không tìm thấy dữ liệu yêu cầu.';
  return 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối và thử lại.';
}

export function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
