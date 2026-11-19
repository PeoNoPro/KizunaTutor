/**
 * KizunaTutor - FE-06/FE-07 Trang lớp chia tab (Tổng quan · Bài tập · Tài liệu) cho gia sư và học viên.
 * Logic thuần (không DOM): nhóm theo tuần, phân loại trạng thái bài tập, tìm kiếm không dấu, loại tệp.
 */

import { plainText } from './address-helpers.js';

export const CLASS_TABS = [
  { key: 'tong-quan', label: 'Tổng quan', icon: 'dashboard' },
  { key: 'bai-tap', label: 'Bài tập', icon: 'assignment' },
  { key: 'tai-lieu', label: 'Tài liệu', icon: 'folder_open' }
];

/** Tab hợp lệ từ ?tab= (mặc định Tổng quan). */
export function normalizeTab(value) {
  return CLASS_TABS.some((t) => t.key === value) ? value : 'tong-quan';
}

/** Có chứa từ khóa không (không phân biệt dấu, hoa thường). */
export function matchesKeyword(text, keyword) {
  const k = plainText(keyword);
  return !k || plainText(text).includes(k);
}

// ---------------------------------------------------------------------------- nhóm theo tuần

const DAY = 24 * 60 * 60 * 1000;
const pad = (n) => String(n).padStart(2, '0');
const dm = (d) => `${pad(d.getDate())}/${pad(d.getMonth() + 1)}`;

/** Thứ Hai 00:00 của tuần chứa d (giờ máy). */
export function weekStart(d) {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  const dow = (x.getDay() + 6) % 7; // Thứ Hai = 0
  x.setDate(x.getDate() - dow);
  return x;
}

/**
 * Nhóm mục theo tuần, mới nhất trước: "Tuần này", "Tuần trước", 6 tuần gần nhất theo khoảng ngày,
 * cũ hơn thì theo tháng. Mục không có ngày vào nhóm "Khác" ở cuối.
 * @returns {Array<{key: string, label: string, items: any[]}>}
 */
export function groupByWeek(items, getDate, now = new Date()) {
  const thisWeek = weekStart(now).getTime();
  const groups = new Map();
  const sorted = [...(items || [])].sort((a, b) => {
    const ta = new Date(getDate(a) || 0).getTime() || 0;
    const tb = new Date(getDate(b) || 0).getTime() || 0;
    return tb - ta;
  });
  sorted.forEach((it) => {
    const raw = getDate(it);
    const d = raw ? new Date(raw) : null;
    let key;
    let label;
    if (!d || Number.isNaN(d.getTime())) {
      key = 'none';
      label = 'Khác';
    } else {
      const ws = weekStart(d);
      const weeksAgo = Math.round((thisWeek - ws.getTime()) / (7 * DAY));
      if (weeksAgo <= 0) {
        key = 'w0';
        label = `Tuần này · ${dm(new Date(thisWeek))} – ${dm(new Date(thisWeek + 6 * DAY))}`;
      } else if (weeksAgo === 1) {
        key = 'w1';
        label = `Tuần trước · ${dm(ws)} – ${dm(new Date(ws.getTime() + 6 * DAY))}`;
      } else if (weeksAgo < 6) {
        key = `w${weeksAgo}`;
        label = `${dm(ws)} – ${dm(new Date(ws.getTime() + 6 * DAY))}`;
      } else {
        key = `m${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
        label = `Tháng ${d.getMonth() + 1}/${d.getFullYear()}`;
      }
    }
    if (!groups.has(key)) groups.set(key, { key, label, items: [] });
    groups.get(key).items.push(it);
  });
  const list = [...groups.values()];
  const none = list.findIndex((g) => g.key === 'none');
  if (none >= 0) list.push(list.splice(none, 1)[0]);
  return list;
}

// ---------------------------------------------------------------------------- trạng thái bài tập

/** Chip lọc của gia sư. */
export const TUTOR_STATUSES = [
  { key: 'review', label: 'Cần chấm' },
  { key: 'open', label: 'Đang mở' },
  { key: 'draft', label: 'Nháp' },
  { key: 'done', label: 'Đã chấm / đã đóng' },
  { key: 'all', label: 'Tất cả' }
];

/** Chip lọc của học viên. */
export const LEARNER_STATUSES = [
  { key: 'todo', label: 'Cần làm' },
  { key: 'waiting', label: 'Chờ chấm' },
  { key: 'graded', label: 'Đã có điểm' },
  { key: 'all', label: 'Tất cả' }
];

/**
 * Trạng thái của một bài với gia sư.
 * @param {{kind: 'file'|'online', a?: object, e?: object}} it
 * @returns {'review'|'open'|'draft'|'done'}
 */
export function tutorStatus(it) {
  if (it.kind === 'online') {
    const e = it.e;
    if (e.status === 'DRAFT') return 'draft';
    if (e.pendingGrading) return 'review';
    if (e.state === 'CLOSED') return 'done';
    return 'open';
  }
  const a = it.a;
  if ((a.submissionState === 'ON_TIME' || a.submissionState === 'LATE') && a.canGrade) return 'review';
  if (a.submissionState === 'GRADED' || a.submissionState === 'OVERDUE_NOT_SUBMITTED') return 'done';
  return 'open';
}

/**
 * Trạng thái của một bài với học viên.
 * @returns {'todo'|'waiting'|'graded'|'other'}
 */
export function learnerStatus(it) {
  if (it.kind === 'online') {
    const e = it.e;
    if (e.inProgressAttemptId || (e.canStart && !e.attemptsUsed)) return 'todo';
    if (e.pendingGrading) return 'waiting';
    if (e.bestScore !== null && e.bestScore !== undefined) return 'graded';
    return 'other';
  }
  const a = it.a;
  if (a.submissionState === 'NOT_SUBMITTED') return 'todo';
  if (a.submissionState === 'ON_TIME' || a.submissionState === 'LATE') return 'waiting';
  if (a.submissionState === 'GRADED') return 'graded';
  return 'other';
}

/** Đếm theo trạng thái (kèm 'all'). */
export function countStatuses(items, statusOf) {
  const out = { all: (items || []).length };
  (items || []).forEach((it) => { const s = statusOf(it); out[s] = (out[s] || 0) + 1; });
  return out;
}

/** Hạn của một bài (bài làm trực tiếp: giờ đóng đề; nộp tệp: hạn nộp). */
export function deadlineOf(it) {
  return it.kind === 'online' ? it.e.closesAt || null : it.a.dueDate || null;
}

/** "còn 4 ngày" / "còn 5 giờ" / "hết hạn hôm nay" / "" (không có hạn hoặc đã qua). */
export function timeLeftLabel(iso, now = new Date()) {
  if (!iso) return '';
  const ms = new Date(iso).getTime() - now.getTime();
  if (Number.isNaN(ms) || ms <= 0) return '';
  const hours = Math.floor(ms / (60 * 60 * 1000));
  if (hours < 1) return 'còn dưới 1 giờ';
  if (hours < 24) return `còn ${hours} giờ`;
  return `còn ${Math.floor(hours / 24)} ngày`;
}

/** Bài cần làm: hạn gần nhất trước, bài không có hạn ở cuối. */
export function sortByDeadline(items) {
  const t = (it) => {
    const d = deadlineOf(it);
    const v = d ? new Date(d).getTime() : NaN;
    return Number.isNaN(v) ? Number.MAX_SAFE_INTEGER : v;
  };
  return [...(items || [])].sort((a, b) => t(a) - t(b));
}

// ---------------------------------------------------------------------------- tài liệu

export const FILE_KINDS = [
  { key: 'pdf', label: 'PDF', icon: 'picture_as_pdf' },
  { key: 'word', label: 'Word', icon: 'description' },
  { key: 'image', label: 'Ảnh', icon: 'image' },
  { key: 'audio', label: 'Âm thanh', icon: 'audio_file' }
];

/** Loại của một tệp theo fileType (PDF, DOCX, PNG, MP3…) hoặc đuôi tên tệp. */
export function fileKind(file) {
  const t = String(file?.fileType || String(file?.fileName || '').split('.').pop() || '').toUpperCase();
  if (t === 'PDF') return 'pdf';
  if (t === 'DOC' || t === 'DOCX') return 'word';
  if (['PNG', 'JPG', 'JPEG', 'GIF', 'WEBP'].includes(t)) return 'image';
  if (['MP3', 'WAV', 'M4A', 'OGG'].includes(t)) return 'audio';
  return 'other';
}

export function fileKindIcon(file) {
  return FILE_KINDS.find((k) => k.key === fileKind(file))?.icon || 'draft';
}

/** Tài liệu khớp bộ lọc: loại tệp ('' = tất cả) và từ khóa trên tiêu đề, mô tả, tên tệp. */
export function filterMaterials(materials, { kind = '', keyword = '' } = {}) {
  return (materials || []).filter((m) => (!kind || (m.files || []).some((f) => fileKind(f) === kind))
    && matchesKeyword(`${m.title} ${m.description || ''} ${(m.files || []).map((f) => f.fileName).join(' ')}`, keyword));
}

/** Số tài liệu theo loại tệp (một tài liệu nhiều loại thì tính vào mỗi loại). */
export function countMaterialKinds(materials) {
  const out = { all: (materials || []).length };
  (materials || []).forEach((m) => {
    new Set((m.files || []).map(fileKind)).forEach((k) => { out[k] = (out[k] || 0) + 1; });
  });
  return out;
}

/** Tài liệu "mới": đăng trong 7 ngày gần đây. */
export function isRecent(iso, now = new Date()) {
  const t = new Date(iso || 0).getTime();
  return !Number.isNaN(t) && now.getTime() - t < 7 * DAY && t <= now.getTime() + DAY;
}

/** Buổi học sắp tới gần nhất của lớp từ danh sách buổi cụ thể (API sessions). */
export function nextSessionOf(sessions, spaceId, now = new Date()) {
  const list = (sessions || [])
    .filter((s) => s.spaceId === spaceId && s.date && s.startTime)
    .map((s) => ({ ...s, at: new Date(`${s.date}T${String(s.endTime || s.startTime).slice(0, 5)}:00`) }))
    .filter((s) => !Number.isNaN(s.at.getTime()) && s.at.getTime() > now.getTime())
    .sort((a, b) => a.at - b.at);
  return list[0] || null;
}
