/**
 * KizunaTutor - Lịch giảng dạy hôm nay trên trang Tổng quan gia sư (dùng chung giao diện + test).
 * Gộp buổi học của lớp (GET /api/classrooms/sessions/me) và buổi học thử đã xác nhận (trial bookings).
 */

const pad2 = (n) => String(n).padStart(2, '0');
export const ymd = (d) => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
export const hm = (d) => `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;

/** Buổi học thử đã được xác nhận (không gồm yêu cầu đang chờ, đã từ chối hoặc đã hủy). */
export const TEACHING_TRIAL_STATUSES = ['CONFIRMED', 'RESCHEDULED', 'COMPLETED'];

/** Danh sách từ nhiều dạng phản hồi API (mảng, {items}, {content}, {data: ...}). */
export function listOf(res) {
  return Array.isArray(res) ? res : (res?.items || res?.content || res?.data?.items || res?.data?.content || res?.data || []);
}

/** Gộp buổi của lớp và buổi học thử trong ngày thành một danh sách, sắp theo giờ bắt đầu. */
export function todayLessons(sessions, trials, now) {
  const today = ymd(now);
  const lessons = (sessions || [])
    .filter((s) => s.date === today && s.startTime)
    .map((s) => ({
      start: s.startTime,
      end: s.endTime,
      title: s.classTitle || 'Lớp học',
      partner: s.partnerName,
      kind: 'Lớp học',
      note: s.number && s.totalSessions ? `Buổi ${s.number}/${s.totalSessions}` : (s.extra ? 'Buổi học thêm' : ''),
      href: s.spaceId ? `/tutor/classrooms?space=${encodeURIComponent(s.spaceId)}` : '/tutor/schedule'
    }));
  (trials || []).forEach((b) => {
    if (!b.scheduledStartTime || !TEACHING_TRIAL_STATUSES.includes(b.bookingStatus)) return;
    const start = new Date(b.scheduledStartTime);
    if (Number.isNaN(start.getTime()) || ymd(start) !== today) return;
    const end = b.scheduledEndTime ? new Date(b.scheduledEndTime)
      : new Date(start.getTime() + (b.durationMinutes || 50) * 60000);
    lessons.push({
      start: hm(start),
      end: hm(end),
      title: 'Buổi học thử 1-1',
      partner: b.partnerName,
      kind: 'Học thử',
      note: b.learningMode === 'ONLINE' ? 'Trực tuyến' : 'Trực tiếp',
      href: '/tutor/connections'
    });
  });
  return lessons.sort((a, b) => a.start.localeCompare(b.start));
}

/** Trạng thái buổi so với giờ hiện tại "HH:mm": đã kết thúc / đang diễn ra / sắp diễn ra. */
export function lessonState(lesson, nowHm) {
  if (lesson.end && lesson.end <= nowHm) return { label: 'Đã kết thúc', cls: 'bg-surface-container-high text-on-surface-variant', dim: true };
  if (lesson.start <= nowHm) return { label: 'Đang diễn ra', cls: 'bg-success/10 text-success', dim: false };
  return { label: 'Sắp diễn ra', cls: 'bg-primary-fixed text-primary', dim: false };
}
