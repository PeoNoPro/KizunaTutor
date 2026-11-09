/**
 * KizunaTutor - Hàm tiện ích FE-07 (Bài tập, nộp bài, chấm điểm).
 * Hàm thuần, kiểm thử bằng `node --test`. Máy chủ vẫn kiểm tra lại mọi quy tắc.
 */

import { validateMaterialFile, validateMaterialFiles } from './classroom-helpers.js';

const STATE_LABELS = {
  NOT_SUBMITTED: 'Chưa nộp',
  ON_TIME: 'Đúng hạn',
  LATE: 'Nộp muộn',
  OVERDUE_NOT_SUBMITTED: 'Quá hạn – chưa nộp',
  GRADED: 'Đã chấm'
};

const STATE_TONES = {
  NOT_SUBMITTED: 'done',
  ON_TIME: 'active',
  LATE: 'paused',
  OVERDUE_NOT_SUBMITTED: 'paused',
  GRADED: 'active'
};

export function submissionStateLabel(state) {
  return STATE_LABELS[state] || 'Không xác định';
}

export function submissionStateTone(state) {
  return STATE_TONES[state] || 'done';
}

export function formatDueDate(value) {
  if (!value) return 'Không có hạn nộp';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return 'Không có hạn nộp';
  return d.toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

export function formatPoints(value) {
  if (value === null || value === undefined || value === '') return '';
  const n = Number(value);
  if (Number.isNaN(n)) return '';
  return n.toLocaleString('vi-VN', { maximumFractionDigits: 2 });
}

/** ISO → giá trị cho <input type="datetime-local"> theo giờ máy người dùng. */
export function toDateTimeLocalValue(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Giá trị datetime-local → chuỗi ISO (UTC) hoặc null nếu để trống. */
export function fromDateTimeLocalValue(value) {
  if (!value) return null;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d.toISOString();
}

/**
 * @param {{title:string, description:string, dueLocal:string, totalPoints:string}} form
 * @param {{now?: Date, dueUnchangedIso?: string|null}} opts dueUnchangedIso: hạn hiện tại khi sửa (giữ nguyên thì không bắt ở tương lai)
 */
export function validateAssignmentForm(form, { now = new Date(), dueUnchangedIso = null } = {}) {
  const errors = {};
  const title = (form.title || '').trim();
  if (!title) errors.title = 'Vui lòng nhập tiêu đề bài tập.';
  else if (title.length > 150) errors.title = 'Tiêu đề bài tập tối đa 150 ký tự.';
  if ((form.description || '').trim().length > 5000) errors.description = 'Mô tả bài tập tối đa 5000 ký tự.';

  const dueIso = fromDateTimeLocalValue(form.dueLocal);
  if (form.dueLocal && !dueIso) errors.dueDate = 'Hạn nộp không hợp lệ.';
  const unchanged = dueIso && dueUnchangedIso
    && toDateTimeLocalValue(dueUnchangedIso) === form.dueLocal;
  if (dueIso && !unchanged && new Date(dueIso) <= now) errors.dueDate = 'Hạn nộp phải là thời điểm trong tương lai.';

  if (form.totalPoints !== undefined && form.totalPoints !== null && String(form.totalPoints).trim() !== '') {
    const p = Number(String(form.totalPoints).replace(',', '.'));
    if (Number.isNaN(p) || p < 1 || p > 999.99) errors.totalPoints = 'Điểm tối đa phải từ 1 đến 999,99.';
  }
  return errors;
}

/** @returns {string|null} lỗi tiếng Việt hoặc null */
/**
 * Bài nộp cần nội dung hoặc ít nhất một tệp.
 * @param {{content?: string, file?: File, files?: File[]|FileList, keptFiles?: number}} p
 *   keptFiles = số tệp cũ còn giữ lại khi sửa bài nộp
 */
export function validateSubmission({ content, file, files, keptFiles = 0 }) {
  const text = (content || '').trim();
  const list = files ? Array.from(files) : (file ? [file] : []);
  if (!text && !list.length && !keptFiles) return 'Vui lòng nhập nội dung trả lời hoặc chọn tệp bài làm.';
  if (text.length > 5000) return 'Nội dung trả lời tối đa 5000 ký tự.';
  return validateMaterialFiles(list, { required: false, existing: keptFiles });
}

/** @returns {string|null} */
export function validateGrade(value, totalPoints) {
  if (value === null || value === undefined || String(value).trim() === '') return 'Vui lòng nhập điểm.';
  const g = Number(String(value).replace(',', '.'));
  const max = Number(totalPoints);
  if (Number.isNaN(g) || g < 0 || g > max) return `Điểm phải từ 0 đến ${formatPoints(max)}.`;
  return null;
}

export function parsePoints(value) {
  if (value === null || value === undefined || String(value).trim() === '') return null;
  return Number(String(value).replace(',', '.'));
}
