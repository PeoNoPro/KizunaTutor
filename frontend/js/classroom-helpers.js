/**
 * KizunaTutor - Hàm tiện ích dùng chung cho FE-06 (Lớp học & Tài liệu học tập).
 * Hàm thuần, không phụ thuộc DOM để kiểm thử được bằng `node --test`.
 */

export const MAX_MATERIAL_SIZE_BYTES = 25 * 1024 * 1024;
export const MAX_FILES_PER_UPLOAD = 5;
export const ALLOWED_MATERIAL_EXTENSIONS = ['pdf', 'doc', 'docx', 'png', 'jpg', 'jpeg', 'mp3'];
export const FILE_RULES_TEXT = 'Định dạng PDF, DOC, DOCX, PNG, JPG, MP3. Mỗi tệp tối đa 25 MB, tối đa 5 tệp.';
export const INVALID_FILE_MESSAGE = 'Dung lượng tệp vượt quá giới hạn 25 MB. Định dạng cho phép: PDF, DOC, DOCX, PNG, JPG, MP3.';
export const UNSUPPORTED_FORMAT_MESSAGE = 'Định dạng tệp không được hỗ trợ. Định dạng cho phép: PDF, DOC, DOCX, PNG, JPG, MP3.';
export const MATERIAL_ACCEPT_ATTR = '.pdf,.doc,.docx,.png,.jpg,.jpeg,.mp3,application/pdf,application/msword,'
  + 'application/vnd.openxmlformats-officedocument.wordprocessingml.document,image/png,image/jpeg,audio/mpeg';

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
  DOC: 'Tài liệu Word',
  DOCX: 'Tài liệu Word',
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

/**
 * Kiểm tra nhiều tệp chọn cùng lúc.
 * @param {FileList|File[]} files tệp mới chọn
 * @param {{required?: boolean, existing?: number}} opts existing = số tệp đang có (khi thêm vào bài đã có tệp)
 * @returns {string|null} lỗi đầu tiên (kèm tên tệp) hoặc null
 */
export function validateMaterialFiles(files, { required = false, existing = 0 } = {}) {
  const list = Array.from(files || []);
  if (!list.length) return required ? 'Vui lòng chọn ít nhất một tệp.' : null;
  const errors = [];
  if (list.length + existing > MAX_FILES_PER_UPLOAD) {
    errors.push(`Tối đa ${MAX_FILES_PER_UPLOAD} tệp (đang chọn ${list.length + existing} tệp).`);
  }
  // Liệt kê MỌI tệp lỗi (mỗi tệp một dòng) để sửa một lần; có lỗi thì không tệp nào được gửi
  for (const f of list) {
    const err = validateMaterialFile(f, { required: true });
    if (err) errors.push(`Tệp "${f.name}": ${err}`);
  }
  return errors.length ? errors.join('\n') : null;
}

/** "1,2 MB" / "350 KB" */
export function formatFileSize(bytes) {
  const n = Number(bytes) || 0;
  if (n >= 1024 * 1024) return `${(n / (1024 * 1024)).toFixed(1).replace('.', ',')} MB`;
  return `${Math.max(1, Math.round(n / 1024))} KB`;
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

// ----------------------------------------------------------------------------
// Khóa học: ngày bắt đầu + tổng số buổi (lịch tự sinh cho đủ số buổi rồi dừng) — khớp quy tắc máy chủ
// ----------------------------------------------------------------------------
export const MAX_TOTAL_SESSIONS = 500;

/** "yyyy-MM-dd" theo giờ máy */
export function localYmd(date = new Date()) {
  const p = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}`;
}

/**
 * Kiểm tra thông tin khóa học.
 * @param {{startDate?: string, totalSessions?: string|number, minTotal?: number, weeklySlots?: number,
 *          today?: string, checkStartDate?: boolean}} p
 *   minTotal = số buổi đã học; weeklySlots = số buổi trong một tuần của lịch hằng tuần.
 *   Tổng số buổi tối thiểu = max(minTotal, weeklySlots). checkStartDate = false khi không đổi ngày bắt đầu.
 * @returns {{startDate?: string, totalSessions?: string}} lỗi theo từng ô
 */
export function validateCourseFields({ startDate, totalSessions, minTotal = 0, weeklySlots = 0, today = localYmd(), checkStartDate = true }) {
  const errors = {};
  if (checkStartDate) {
    if (!startDate) errors.startDate = 'Vui lòng chọn ngày bắt đầu khóa học.';
    else if (startDate < today) errors.startDate = 'Ngày bắt đầu khóa học phải từ hôm nay trở đi.';
  }
  const raw = String(totalSessions ?? '').trim();
  const n = Number(raw);
  const min = minCourseSessions({ started: minTotal, weeklySlots });
  if (!raw) errors.totalSessions = 'Vui lòng nhập tổng số buổi của khóa học.';
  else if (!Number.isInteger(n)) errors.totalSessions = 'Số buổi phải là số tròn, ví dụ 24.';
  else if (n > MAX_TOTAL_SESSIONS) errors.totalSessions = `Mỗi khóa học có tối đa ${MAX_TOTAL_SESSIONS} buổi.`;
  else if (n < min) errors.totalSessions = minSessionsHint({ started: minTotal, weeklySlots });
  return errors;
}

/** Tổng số buổi tối thiểu của khóa = max(1, số buổi đã học, số buổi trong một tuần). */
export function minCourseSessions({ started = 0, weeklySlots = 0 } = {}) {
  return Math.max(1, started, weeklySlots);
}

/** Câu giải thích vì sao tổng số buổi phải từ mức tối thiểu trở lên. */
export function minSessionsHint({ started = 0, weeklySlots = 0 } = {}) {
  const min = minCourseSessions({ started, weeklySlots });
  if (min <= 1) return 'Khóa học cần có ít nhất 1 buổi.';
  const reason = started >= weeklySlots && started > 0
    ? `Khóa học đã học ${started} buổi`
    : `Lịch học có ${weeklySlots} buổi mỗi tuần`;
  return `${reason} nên tổng số buổi cần từ ${min} buổi trở lên.`;
}

/** Dòng tóm tắt tiến độ khóa học hiển thị trong lớp. */
export function courseSummaryText(c) {
  if (!c) return '';
  if (!c.totalSessions) return 'Lớp chưa có tổng số buổi nên lịch đang lặp lại hằng tuần.';
  const parts = [`Đã học ${c.startedSessions || 0}/${c.totalSessions} buổi`];
  if (c.courseStartDate) parts.push(`bắt đầu ${formatDateVi(c.courseStartDate)}`);
  if (c.courseEndDate) parts.push(`dự kiến kết thúc ${formatDateVi(c.courseEndDate)}`);
  return parts.join(' · ');
}

// ----------------------------------------------------------------------------
// Lịch học hằng tuần (1 = Thứ Hai ... 7 = Chủ nhật; giờ "HH:mm") — khớp quy tắc máy chủ
// ----------------------------------------------------------------------------
export const DAYS_OF_WEEK = [
  { value: 1, label: 'Thứ Hai', short: 'T2' },
  { value: 2, label: 'Thứ Ba', short: 'T3' },
  { value: 3, label: 'Thứ Tư', short: 'T4' },
  { value: 4, label: 'Thứ Năm', short: 'T5' },
  { value: 5, label: 'Thứ Sáu', short: 'T6' },
  { value: 6, label: 'Thứ Bảy', short: 'T7' },
  { value: 7, label: 'Chủ nhật', short: 'CN' }
];
export const MAX_SCHEDULE_SLOTS = 14;

export function dayOfWeekLabel(day) {
  return DAYS_OF_WEEK.find((d) => d.value === Number(day))?.label || '';
}

export function dayOfWeekShort(day) {
  return DAYS_OF_WEEK.find((d) => d.value === Number(day))?.short || '';
}

/** "19:00:00" hoặc "19:00" → số phút trong ngày; null nếu không hợp lệ. */
export function timeToMinutes(value) {
  const m = /^(\d{1,2}):(\d{2})/.exec(String(value || ''));
  if (!m) return null;
  const h = Number(m[1]);
  const min = Number(m[2]);
  if (h > 23 || min > 59) return null;
  return h * 60 + min;
}

export function shortTime(value) {
  const mins = timeToMinutes(value);
  if (mins === null) return '';
  return `${String(Math.floor(mins / 60)).padStart(2, '0')}:${String(mins % 60).padStart(2, '0')}`;
}

/** "1 giờ 30 phút" */
export function durationLabel(start, end) {
  const a = timeToMinutes(start);
  const b = timeToMinutes(end);
  if (a === null || b === null || b <= a) return '';
  const h = Math.floor((b - a) / 60);
  const m = (b - a) % 60;
  return [h ? `${h} giờ` : '', m ? `${m} phút` : ''].filter(Boolean).join(' ');
}

export function sortSchedules(list) {
  return [...(list || [])].sort((x, y) =>
    (x.dayOfWeek - y.dayOfWeek) || ((timeToMinutes(x.startTime) ?? 0) - (timeToMinutes(y.startTime) ?? 0)));
}

/** "T2 19:00–20:30 · T5 18:00–19:00" — dùng trên thẻ lớp học. */
export function summarizeSchedules(list) {
  const sorted = sortSchedules(list);
  if (!sorted.length) return '';
  return sorted.map((s) => `${dayOfWeekShort(s.dayOfWeek)} ${shortTime(s.startTime)}–${shortTime(s.endTime)}`).join(' · ');
}

/** @returns {string|null} lỗi tiếng Việt của một buổi học */
export function validateScheduleSlot(slot) {
  const day = Number(slot?.dayOfWeek);
  if (!Number.isInteger(day) || day < 1 || day > 7) return 'Vui lòng chọn thứ trong tuần cho buổi học.';
  const start = timeToMinutes(slot.startTime);
  const end = timeToMinutes(slot.endTime);
  if (start === null || end === null) return 'Vui lòng nhập giờ bắt đầu và giờ kết thúc của buổi học.';
  if (end <= start) return 'Giờ kết thúc phải sau giờ bắt đầu.';
  if (end - start < 30 || end - start > 240) return 'Mỗi buổi học dài từ 30 phút đến 4 giờ.';
  return null;
}

/** @returns {string|null} lỗi của cả danh sách buổi học khi tạo lớp */
export function validateScheduleList(list) {
  if (!list || !list.length) return 'Vui lòng thêm ít nhất một buổi học trong tuần.';
  if (list.length > MAX_SCHEDULE_SLOTS) return `Mỗi lớp có tối đa ${MAX_SCHEDULE_SLOTS} buổi học mỗi tuần.`;
  for (const slot of list) {
    const err = validateScheduleSlot(slot);
    if (err) return err;
  }
  for (let i = 0; i < list.length; i++) {
    for (let j = i + 1; j < list.length; j++) {
      const a = list[i];
      const b = list[j];
      if (Number(a.dayOfWeek) === Number(b.dayOfWeek)
        && timeToMinutes(a.startTime) < timeToMinutes(b.endTime)
        && timeToMinutes(b.startTime) < timeToMinutes(a.endTime)) {
        return `Các buổi học bị trùng giờ nhau (${dayOfWeekLabel(b.dayOfWeek)}).`;
      }
    }
  }
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
