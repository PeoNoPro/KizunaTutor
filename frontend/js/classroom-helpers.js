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

// ----------------------------------------------------------------------------
// Phân quyền trang theo vai trò (chỉ để điều hướng giao diện; API vẫn kiểm quyền ở máy chủ)
// ----------------------------------------------------------------------------
export const ROLE_TUTOR = 'ROLE_TUTOR';
export const ROLE_LEARNER = 'ROLE_LEARNER';

/** Trang lớp học tương ứng mỗi vai trò (đường dẫn đầy đủ, chạy được ở cả cổng 3000 và 8085). */
export const CLASSROOM_HOME_BY_ROLE = {
  [ROLE_TUTOR]: '/pages/tutor-classrooms/index.html',
  [ROLE_LEARNER]: '/pages/my-courses/index.html'
};

/** Giải mã phần payload của JWT (không xác minh chữ ký). */
export function decodeJwtPayload(token) {
  if (!token || typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length < 2) return null;
  try {
    let b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    while (b64.length % 4) b64 += '=';
    const bytes = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes));
  } catch (e) {
    return null;
  }
}

/** Vai trò hiện tại: ưu tiên claim "role" trong token, sau đó tới kizuna_user.role. */
export function resolveRole(token, userJson) {
  const payload = decodeJwtPayload(token);
  if (payload && typeof payload.role === 'string' && payload.role) return payload.role;
  try {
    const user = typeof userJson === 'string' ? JSON.parse(userJson) : userJson;
    if (user && typeof user.role === 'string' && user.role) return user.role;
  } catch (e) {
    /* dữ liệu người dùng không đọc được */
  }
  return null;
}

export function getCurrentRole() {
  try {
    return resolveRole(localStorage.getItem('kizuna_token'), localStorage.getItem('kizuna_user'));
  } catch (e) {
    return null;
  }
}

/** Nội dung thông báo khi mở trang không đúng vai trò. */
export function roleDeniedHtml(pageName, requiredRoleLabel, actualRole) {
  const home = CLASSROOM_HOME_BY_ROLE[actualRole];
  const homeLabel = actualRole === ROLE_TUTOR ? 'Đến trang Quản lý lớp học' : 'Đến trang Lớp học của tôi';
  return `
    <div class="border border-black rounded-[5px] bg-white p-6 text-center space-y-4" style="box-shadow:4px 4px 0 0 #000">
      <h1 style="font-family:'Itim',cursive;font-size:24px">Bạn không có quyền truy cập trang này</h1>
      <p>Trang ${escapeHtml(pageName)} chỉ dành cho tài khoản ${escapeHtml(requiredRoleLabel)}.</p>
      <div class="flex flex-wrap justify-center gap-3">
        ${home ? `<a href="${home}" class="inline-flex items-center border border-black rounded-[5px] px-4 py-2 font-semibold bg-white" style="box-shadow:3px 3px 0 0 #000">${homeLabel}</a>` : ''}
        <button type="button" data-switch-account class="inline-flex items-center border border-black rounded-[5px] px-4 py-2 font-semibold bg-white" style="box-shadow:3px 3px 0 0 #000">Đăng nhập bằng tài khoản khác</button>
      </div>
    </div>`;
}

export function clearSessionAndGoToLogin() {
  try {
    localStorage.removeItem('kizuna_token');
    localStorage.removeItem('kizuna_user');
  } catch (e) {
    /* không truy cập được bộ nhớ trình duyệt */
  }
  document.cookie = 'access_token=; Max-Age=0; path=/';
  window.location.href = '/login';
}
