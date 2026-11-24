/**
 * Phần form dùng chung giữa popup chọn giờ (tutor-profile) và trang xác nhận đặt lịch (booking-confirm):
 * môn muốn học, trình độ theo hồ sơ, liên hệ học viên, thông báo lỗi thân thiện.
 */
import { API } from './api.js';
import { escapeHtml } from './profile-utils.js';

export const isLoggedIn = () => !!(localStorage.getItem('kizuna_token') || localStorage.getItem('token'));

export function friendlyError(err, fallback) {
  if (err && (err.status === 401)) return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
  if (err && (err.name === 'TypeError' || err.name === 'TimeoutError' || String(err.message || '').toLowerCase().includes('fetch'))) {
    return 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối mạng hoặc thử lại sau.';
  }
  if (err && err.status === 403) return 'Chỉ tài khoản học viên mới có thể gửi yêu cầu này.';
  return (err && err.message) ? err.message : fallback;
}

// Trình độ lấy từ hồ sơ học viên (không bắt nhập lại)
const LEVEL_LABELS = { BEGINNER: 'Mới bắt đầu', N5: 'N5', N4: 'N4', N3: 'N3', N2: 'N2', N1: 'N1' };
let learnerProfilePromise = null;

export function renderLearnerLevel() {
  const boxes = document.querySelectorAll('[data-learner-level]');
  if (!boxes.length || !isLoggedIn()) return;
  if (!learnerProfilePromise) {
    learnerProfilePromise = API.profile.getLearnerProfile().catch(() => null);
  }
  learnerProfilePromise.then((profile) => {
    // Điền sẵn liên hệ (chỉ khi ô còn trống, không ghi đè thứ học viên đã sửa)
    document.querySelectorAll('[data-contact-phone]').forEach((el) => { if (!el.value && profile && profile.phone) el.value = profile.phone; });
    document.querySelectorAll('[data-contact-email]').forEach((el) => { if (!el.value && profile && profile.email) el.value = profile.email; });
    const level = profile && profile.currentLevel ? (LEVEL_LABELS[String(profile.currentLevel).toUpperCase()] || profile.currentLevel) : '';
    const html = level
      ? `Trình độ hiện tại (theo hồ sơ): <strong class="text-[#0F2545]">${escapeHtml(level)}</strong> · <a href="/pages/learner-profile/" target="_blank" class="text-[#006194] font-semibold hover:underline">Cập nhật hồ sơ</a>`
      : `Bạn chưa ghi trình độ trong hồ sơ · <a href="/pages/learner-profile/" target="_blank" class="text-[#006194] font-semibold hover:underline">Cập nhật hồ sơ</a> để gia sư chuẩn bị bài phù hợp`;
    boxes.forEach((b) => { b.innerHTML = html; });
  });
}

/** Các môn gia sư nhận dạy: bảng học phí theo môn của bài đăng; chưa có thì dùng phần chuyên môn. */
export function teachingSubjects(tutor) {
  const prices = Array.isArray(tutor?.priceItems) ? tutor.priceItems : [];
  const fromPrices = prices.filter(p => p && p.level).map(p => String(p.levelLabel || p.level).trim());
  const list = fromPrices.length ? fromPrices
    : (tutor?.specialties || []).map(s => String(s).replace(/\s+/g, ' ').trim());
  return [...new Set(list.filter(Boolean))];
}

/** Vẽ chip chọn môn vào các ô khớp selector; onPick(box, label) chạy khi một chip được bật. */
export function renderGoalPickers(tutor, onPick = null, selector = '[data-goal-picker]') {
  const subjects = teachingSubjects(tutor);
  document.querySelectorAll(selector).forEach((box) => {
    if (box.dataset.ready) return;
    box.dataset.ready = '1';
    if (!subjects.length) {
      box.innerHTML = '<span class="text-xs text-slate-500">Gia sư chưa ghi môn dạy cụ thể — bạn có thể ghi mong muốn trong lời nhắn.</span>';
      return;
    }
    box.innerHTML = subjects.map((s) => `
      <button type="button" data-goal="${escapeHtml(s)}" aria-pressed="false"
        class="px-3 py-1.5 rounded-full text-xs font-semibold ring-1 ring-slate-300 bg-white text-slate-700 hover:ring-[#006194] transition-colors">${escapeHtml(s)}</button>`).join('');
    box.addEventListener('click', (e) => {
      const chip = e.target.closest('[data-goal]');
      if (!chip) return;
      const on = chip.getAttribute('aria-pressed') !== 'true';
      chip.setAttribute('aria-pressed', String(on));
      chip.classList.toggle('bg-[#006194]', on);
      chip.classList.toggle('text-white', on);
      chip.classList.toggle('ring-[#006194]', on);
      chip.classList.toggle('bg-white', !on);
      chip.classList.toggle('text-slate-700', !on);
      if (on && onPick) onPick(box, chip.dataset.goal);
    });
    // Gia sư chỉ dạy một môn thì chọn sẵn
    if (subjects.length === 1) box.querySelector('[data-goal]').click();
  });
}

export function selectedGoals(pickerId) {
  const chips = document.querySelectorAll(`#${pickerId} [data-goal][aria-pressed="true"]`);
  const text = [...chips].map(c => c.dataset.goal).join(', ');
  return text ? text.slice(0, 500) : null;
}

/** Liên hệ học viên gửi cho gia sư; trả về { error } nếu thiếu đồng ý hoặc số điện thoại sai. */
export function readContact(prefix) {
  const phoneEl = document.getElementById(`${prefix}-contact-phone`);
  const emailEl = document.getElementById(`${prefix}-contact-email`);
  const consentEl = document.getElementById(`${prefix}-contact-consent`);
  let phone = phoneEl ? phoneEl.value.replace(/[^0-9+]/g, '') : '';
  if (phone.startsWith('+84')) phone = '0' + phone.slice(3);
  const email = emailEl ? emailEl.value.trim() : '';
  if (!/^0\d{9}$/.test(phone)) {
    if (phoneEl) phoneEl.focus();
    return { error: 'Vui lòng nhập số điện thoại hợp lệ (10 số, bắt đầu bằng 0) để gia sư liên hệ với bạn.' };
  }
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    if (emailEl) emailEl.focus();
    return { error: 'Email liên hệ không hợp lệ.' };
  }
  if (!consentEl || !consentEl.checked) {
    return { error: 'Vui lòng tích đồng ý gửi hồ sơ và liên hệ cho gia sư.' };
  }
  return { contactPhone: phone, contactEmail: email || null, shareContact: true };
}

/** Ngược với parseBookingParams: lựa chọn giờ → query của trang xác nhận. */
export function buildBookingQuery(sel) {
  const q = new URLSearchParams({ type: sel.type, tutorId: sel.tutorId });
  if (sel.type === 'trial') {
    q.set('start', sel.start);
    if (sel.offSchedule) q.set('propose', '1');
  } else {
    q.set('slots', sel.slots.map(s => `${s.dayOfWeek}-${s.startTime}`).join(','));
  }
  return q;
}

/**
 * Đọc lựa chọn giờ từ URL của trang xác nhận. Trả về null nếu thiếu/sai (người dùng có thể sửa URL).
 * trial:   start=YYYY-MM-DDTHH:MM (giờ Việt Nam), propose=1 nếu đề xuất ngoài lịch rảnh
 * connect: slots=dow-HH:MM,... (dow 1=CN..7=T7) — chỉ giờ bắt đầu, độ dài ca do gia sư chọn khi đồng ý
 */
export function parseBookingParams(search, now = new Date()) {
  const q = new URLSearchParams(search);
  const type = q.get('type');
  const tutorId = q.get('tutorId');
  if (!tutorId) return null;
  if (type === 'trial') {
    const start = q.get('start') || '';
    if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(start)) return null;
    const scheduledStartTime = `${start}:00+07:00`;
    const at = new Date(scheduledStartTime);
    if (Number.isNaN(at.getTime()) || at <= now) return null;
    return { type, tutorId, start, scheduledStartTime, offSchedule: q.get('propose') === '1' };
  }
  if (type === 'connect') {
    const slots = (q.get('slots') || '').split(',').filter(Boolean).map((s) => {
      const m = /^([1-7])-(\d{2}):(\d{2})$/.exec(s);
      if (!m || Number(m[2]) > 23 || Number(m[3]) > 59) return null;
      return { dayOfWeek: Number(m[1]), startTime: `${m[2]}:${m[3]}` };
    });
    if (!slots.length || slots.length > 50 || slots.includes(null)) return null;
    return { type, tutorId, slots };
  }
  return null;
}
