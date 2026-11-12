/**
 * KizunaTutor - Sắp xếp danh sách yêu cầu (học thử & kết nối trực tiếp) cho trang của gia sư và học viên.
 *
 * Mỗi nhóm trạng thái trả lời một câu hỏi khác nhau nên xếp khác nhau:
 *  1. Chờ phản hồi  — gia sư: hạn phản hồi gần nhất lên đầu; học viên: buổi học gần nhất lên đầu.
 *  2. Đã xác nhận   — buổi đã kết thúc nhưng chưa xác nhận hoàn thành lên đầu, sau đó buổi học gần nhất.
 *  3. Lịch sử       — cập nhật gần nhất lên đầu.
 * Tab "Tất cả" giữ thứ tự nhóm: Chờ phản hồi → Đã xác nhận → Lịch sử.
 *
 * Hạn phản hồi (khớp quy tắc tự hết hạn ở backend, BookingLimits):
 *  - Học thử: mốc sớm hơn giữa (lần gửi / đổi lịch gần nhất + 24 giờ) và giờ bắt đầu buổi học.
 *  - Kết nối trực tiếp: mốc sớm hơn giữa (lúc gửi + 24 giờ) và giờ bắt đầu buổi đầu tiên.
 */

export const RESPONSE_HOURS = 24;
const HOUR = 3600 * 1000;
const FAR_FUTURE = Number.MAX_SAFE_INTEGER;

const TRIAL_PENDING = ['PENDING'];
const TRIAL_ACCEPTED = ['CONFIRMED', 'RESCHEDULED'];

function ts(value) {
  const t = Date.parse(value);
  return Number.isNaN(t) ? null : t;
}

// ---------------------------------------------------------------- Học thử

function trialGroup(b) {
  if (TRIAL_PENDING.includes(b.bookingStatus)) return 0;
  if (TRIAL_ACCEPTED.includes(b.bookingStatus)) return 1;
  return 2;
}

/** Thời điểm (ms) yêu cầu học thử đang chờ sẽ tự hết hạn; null nếu không xác định. */
export function trialDeadline(b) {
  const sent = ts(b.updatedAt) ?? ts(b.createdAt);
  const start = ts(b.scheduledStartTime);
  const byResponse = sent != null ? sent + RESPONSE_HOURS * HOUR : null;
  if (byResponse == null) return start;
  if (start == null) return byResponse;
  return Math.min(byResponse, start);
}

function trialEnd(b) {
  const end = ts(b.scheduledEndTime);
  if (end != null) return end;
  const start = ts(b.scheduledStartTime);
  return start != null ? start + (b.durationMinutes || 30) * 60000 : null;
}

/**
 * @param {Array} list danh sách TrialBookingResponseDTO
 * @param {'tutor'|'learner'} viewer
 */
export function sortTrials(list, viewer, now = Date.now()) {
  const key = (b) => {
    const group = trialGroup(b);
    const start = ts(b.scheduledStartTime) ?? FAR_FUTURE;
    if (group === 0) {
      return [group, 0, viewer === 'tutor' ? (trialDeadline(b) ?? FAR_FUTURE) : start];
    }
    if (group === 1) {
      const end = trialEnd(b);
      const ended = end != null && end <= now; // đã học xong nhưng chưa xác nhận hoàn thành → cần xử lý
      return [group, ended ? 0 : 1, start];
    }
    const updated = ts(b.updatedAt) ?? ts(b.createdAt) ?? 0;
    return [group, 0, -updated];
  };
  return sortByKey(list, key);
}

// ---------------------------------------------------------------- Kết nối trực tiếp

function directGroup(r) {
  if (r.status === 'PENDING') return 0;
  if (r.status === 'ACCEPTED') return 1;
  return 2;
}

/** Thời điểm (ms) bắt đầu buổi học đầu tiên của yêu cầu kết nối; null với yêu cầu cũ chỉ có ca hằng tuần. */
export function directFirstSession(r) {
  const s = (r.sessions || [])[0];
  if (s && s.date && s.startTime) return ts(`${s.date}T${s.startTime}:00+07:00`);
  if (r.startDate) return ts(`${r.startDate}T00:00:00+07:00`);
  return null;
}

/** Thời điểm (ms) yêu cầu kết nối đang chờ sẽ tự hết hạn. */
export function directDeadline(r) {
  const sent = ts(r.createdAt);
  const first = directFirstSession(r);
  const byResponse = sent != null ? sent + RESPONSE_HOURS * HOUR : null;
  if (byResponse == null) return first;
  if (first == null) return byResponse;
  return Math.min(byResponse, first);
}

/**
 * @param {Array} list danh sách ConnectionRequestResponseDTO
 * @param {'tutor'|'learner'} viewer
 */
export function sortDirect(list, viewer) {
  const key = (r) => {
    const group = directGroup(r);
    const first = directFirstSession(r) ?? FAR_FUTURE;
    if (group === 0) {
      return [group, 0, viewer === 'tutor' ? (directDeadline(r) ?? FAR_FUTURE) : first];
    }
    if (group === 1) return [group, 0, first];
    const updated = ts(r.respondedAt) ?? ts(r.createdAt) ?? 0;
    return [group, 0, -updated];
  };
  return sortByKey(list, key);
}

// ---------------------------------------------------------------- Hiển thị hạn phản hồi

/**
 * Dòng "Còn 3 giờ để phản hồi" cho thẻ đang chờ (phía gia sư). Đỏ khi còn dưới 6 giờ.
 * @returns {string} HTML, rỗng nếu không xác định được hạn
 */
export function responseDeadlineHtml(deadline, now = Date.now()) {
  if (deadline == null) return '';
  const left = deadline - now;
  let text;
  if (left <= 0) {
    text = 'Đã hết hạn phản hồi';
  } else if (left < HOUR) {
    text = `Còn ${Math.max(1, Math.round(left / 60000))} phút để phản hồi`;
  } else if (left < 48 * HOUR) {
    text = `Còn ${Math.floor(left / HOUR)} giờ để phản hồi`;
  } else {
    text = `Còn ${Math.floor(left / (24 * HOUR))} ngày để phản hồi`;
  }
  const urgent = left < 6 * HOUR;
  const cls = urgent
    ? 'bg-rose-50 text-rose-700 border-rose-200'
    : 'bg-amber-50 text-amber-800 border-amber-200';
  return `<span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full border text-[11px] font-bold ${cls}"
    title="Quá hạn này yêu cầu sẽ tự hết hạn"><span class="material-symbols-outlined text-[14px]">timer</span>${text}</span>`;
}

// ---------------------------------------------------------------- tiện ích

function sortByKey(list, keyOf) {
  return (list || [])
    .map((item, index) => ({ item, index, key: keyOf(item) }))
    .sort((a, b) => {
      for (let i = 0; i < a.key.length; i++) {
        if (a.key[i] !== b.key[i]) return a.key[i] < b.key[i] ? -1 : 1;
      }
      return a.index - b.index;
    })
    .map(x => x.item);
}
