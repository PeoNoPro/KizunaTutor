/**
 * KizunaTutor - FE-07 Trang "Quản lý bài tập" chung của gia sư: logic thuần (không DOM) để kiểm thử bằng node --test.
 * - Xếp từng bài (nộp tệp / làm trực tiếp) của mọi lớp vào 4 nhóm: Cần chấm, Đang chờ, Quá hạn, Đã chấm.
 * - Tính hạn nộp nhanh: trước buổi học tới của lớp, +3 ngày, +1 tuần.
 */

import { formatDateTime, formatScore } from './exercise-helpers.js';

export const BUCKETS = [
  { key: 'review', label: 'Cần chấm', icon: 'rate_review' },
  { key: 'waiting', label: 'Đang chờ', icon: 'schedule' },
  { key: 'overdue', label: 'Quá hạn', icon: 'warning' },
  { key: 'graded', label: 'Đã chấm', icon: 'check_circle' }
];

const classPage = (spaceId) => `/tutor/classrooms?space=${encodeURIComponent(spaceId)}&view=assignments`;
const editorPage = (spaceId, exerciseId, tab = null) => {
  const q = new URLSearchParams({ space: spaceId, exercise: exerciseId });
  if (tab) q.set('tab', tab);
  return `/pages/exercise-editor/index.html?${q.toString()}`;
};

function base(cls, kind, item) {
  return {
    kind,
    spaceId: cls.spaceId,
    classTitle: cls.title || 'Lớp học',
    learnerName: cls.learnerName || 'Học viên',
    id: kind === 'file' ? item.assignmentId : item.exerciseId,
    title: item.title || 'Bài tập',
    typeLabel: kind === 'file' ? 'Nộp file' : 'Làm trực tiếp'
  };
}

/** Bài nộp tệp (AssignmentSummaryResponse) → một dòng của trang tổng quan. */
export function fileAssignmentRow(cls, a) {
  const row = base(cls, 'file', a);
  const due = a.dueDate ? formatDateTime(a.dueDate) : '';
  row.sortTime = a.dueDate ? new Date(a.dueDate).getTime() : Number.MAX_SAFE_INTEGER;
  row.dateLabel = due ? `Hạn ${due}` : 'Không đặt hạn';
  switch (a.submissionState) {
    case 'GRADED':
      return { ...row, bucket: 'graded', need: `Điểm ${formatScore(a.grade)} / ${formatScore(a.totalPoints)}`, action: { label: 'Xem bài', href: classPage(cls.spaceId) } };
    case 'ON_TIME':
    case 'LATE':
      if (a.canGrade) {
        return { ...row, bucket: 'review', late: a.submissionState === 'LATE', need: a.submissionState === 'LATE' ? 'Chấm bài nộp file · nộp muộn' : 'Chấm bài nộp file',
          action: { label: 'Chấm ngay', href: `${classPage(cls.spaceId)}&grade=${encodeURIComponent(a.assignmentId)}`, primary: true } };
      }
      return { ...row, bucket: 'waiting', need: `Đã nộp · chấm được sau hạn${due ? ` (${due})` : ''}`, action: { label: 'Xem bài', href: classPage(cls.spaceId) } };
    case 'OVERDUE_NOT_SUBMITTED':
      return { ...row, bucket: 'overdue', need: 'Quá hạn, học viên chưa nộp', action: { label: 'Xem bài', href: classPage(cls.spaceId) } };
    default:
      return { ...row, bucket: 'waiting', need: a.opened === false ? 'Chưa tới giờ mở bài' : 'Học viên chưa nộp', action: { label: 'Xem bài', href: classPage(cls.spaceId) } };
  }
}

/** Bài làm trực tiếp đã giao (ExerciseSummary) → một dòng; bài nháp trả null (hiện ở thẻ "Bài nháp"). */
export function exerciseRow(cls, e) {
  if (e.status === 'DRAFT') return null;
  const row = base(cls, 'online', e);
  const closes = e.closesAt ? formatDateTime(e.closesAt) : '';
  row.sortTime = e.closesAt ? new Date(e.closesAt).getTime() : Number.MAX_SAFE_INTEGER;
  row.dateLabel = closes ? `Đóng đề ${closes}` : 'Không đóng đề';
  row.meta = `${e.questionCount} câu · ${formatScore(e.maxScore)} điểm`;
  if (e.pendingGrading) {
    return { ...row, bucket: 'review', need: 'Có câu tự luận chờ chấm', action: { label: 'Chấm ngay', href: editorPage(cls.spaceId, e.exerciseId, 'bai-lam'), primary: true } };
  }
  const done = e.bestScore !== null && e.bestScore !== undefined;
  if (done && !e.inProgressAttemptId) {
    return { ...row, bucket: 'graded', need: `Điểm cao nhất ${formatScore(e.bestScore)} / ${formatScore(e.maxScore)} · ${e.attemptsUsed}/${e.maxAttempts} lượt`,
      action: { label: 'Xem bài làm', href: editorPage(cls.spaceId, e.exerciseId, 'bai-lam') } };
  }
  if (e.inProgressAttemptId) {
    return { ...row, bucket: 'waiting', need: 'Học viên đang làm bài', action: { label: 'Xem bài', href: editorPage(cls.spaceId, e.exerciseId) } };
  }
  if (e.state === 'CLOSED') {
    return { ...row, bucket: 'overdue', need: 'Đã đóng đề, học viên chưa làm', action: { label: 'Xem bài', href: editorPage(cls.spaceId, e.exerciseId) } };
  }
  return { ...row, bucket: 'waiting', need: e.state === 'NOT_OPEN' ? 'Chưa tới giờ mở đề' : 'Học viên chưa làm', action: { label: 'Xem bài', href: editorPage(cls.spaceId, e.exerciseId) } };
}

/** Đếm số dòng theo nhóm. */
export function countBuckets(rows) {
  const out = { review: 0, waiting: 0, overdue: 0, graded: 0 };
  rows.forEach((r) => { out[r.bucket] = (out[r.bucket] || 0) + 1; });
  return out;
}

/** Lọc theo lớp, loại bài, từ khóa (tên bài / học viên / lớp, không phân biệt hoa thường và dấu). */
export function filterRows(rows, { bucket = null, spaceId = '', kind = '', keyword = '' } = {}) {
  const norm = (s) => String(s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase();
  const k = norm(keyword.trim());
  return rows.filter((r) => (!bucket || r.bucket === bucket)
    && (!spaceId || r.spaceId === spaceId)
    && (!kind || r.kind === kind)
    && (!k || norm(`${r.title} ${r.learnerName} ${r.classTitle}`).includes(k)));
}

/** Sắp xếp: cần chấm / đang chờ theo hạn gần nhất trước; quá hạn / đã chấm theo hạn mới nhất trước. */
export function sortRows(rows, bucket) {
  const asc = bucket === 'review' || bucket === 'waiting';
  return [...rows].sort((a, b) => (asc ? a.sortTime - b.sortTime : b.sortTime - a.sortTime));
}

/**
 * Hạn "trước buổi học tới": 1 phút trước buổi học sớm nhất (sau thời điểm now) của lớp.
 * sessions: [{spaceId, date "yyyy-MM-dd", startTime "HH:mm"}] (giờ Việt Nam). Không có buổi nào → null.
 */
export function nextLessonDeadline(sessions, spaceId, now = new Date()) {
  const starts = (sessions || [])
    .filter((s) => s.spaceId === spaceId && s.date && s.startTime)
    .map((s) => new Date(`${s.date}T${s.startTime.slice(0, 5)}:00`))
    .filter((d) => !Number.isNaN(d.getTime()) && d.getTime() > now.getTime() + 60_000)
    .sort((a, b) => a - b);
  return starts.length ? new Date(starts[0].getTime() - 60_000) : null;
}

/** Hạn nhanh "+3 ngày" / "+1 tuần": cuối ngày (23:59) sau số ngày đó. */
export function quickDeadline(days, now = new Date()) {
  const d = new Date(now);
  d.setDate(d.getDate() + days);
  d.setHours(23, 59, 0, 0);
  return d;
}

/** Date → "yyyy-MM-dd" theo giờ máy (dùng cho tham số khoảng ngày của API buổi học). */
export function ymdLocal(d) {
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}
