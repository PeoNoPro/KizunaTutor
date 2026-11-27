/**
 * Trang xác nhận đặt lịch: học viên đã chọn giờ trong popup ở trang gia sư (giờ truyền qua URL),
 * ở đây nhập liên hệ / môn / lời nhắn rồi gửi yêu cầu học thử hoặc kết nối.
 */
import { API } from '../../js/api.js';
import { escapeHtml } from '../../js/profile-utils.js';
import { closeModal } from '../../components/modal/modal.js';
import {
  isLoggedIn, friendlyError, renderGoalPickers, renderLearnerLevel,
  selectedGoals, readContact, parseBookingParams, buildBookingQuery
} from '../../js/booking-form.js';
import { createBookingPickers, TRIAL_DURATION_MINUTES, toMinutes } from '../../js/booking-pickers.js';

const DAY_NAMES = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'];
// dayOfWeek của server: 1 = Chủ nhật ... 7 = Thứ bảy
const dowName = (dow) => DAY_NAMES[dow - 1] || '';

const $ = (id) => document.getElementById(id);

function showError(msg) {
  $('bc-error-message').textContent = msg;
  $('bc-error-alert').classList.toggle('hidden', !msg);
  if (msg) $('bc-error-alert').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function showFatal(title, text, href) {
  $('bc-loading').innerHTML = `
    <div class="p-10 text-center bg-white rounded-2xl border border-[#E5DECB] space-y-3">
      <h3 class="text-lg font-bold">${escapeHtml(title)}</h3>
      <p class="text-sm text-slate-500">${escapeHtml(text)}</p>
      <a href="${href}" class="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-[#006194] text-white text-sm font-semibold">Quay lại</a>
    </div>`;
}

function renderAvatar(box, tutor) {
  const name = String(tutor.fullName || '').trim();
  const initials = name ? name.split(/\s+/).map(p => p[0]).filter(Boolean).slice(-2).join('').toUpperCase() : 'GS';
  const fallback = `<div class="w-14 h-14 rounded-xl bg-sky-100 text-sky-800 font-bold flex items-center justify-center text-sm">${escapeHtml(initials)}</div>`;
  if (!tutor.avatarUrl) { box.innerHTML = fallback; return; }
  const img = document.createElement('img');
  img.src = tutor.avatarUrl;
  img.alt = 'Avatar gia sư';
  img.className = 'w-14 h-14 rounded-xl object-cover border border-[#E5DECB]';
  img.onerror = () => { box.innerHTML = fallback; };
  box.replaceChildren(img);
}

function row(label, value) {
  return `<div class="flex items-center justify-between gap-3"><span class="text-slate-500 font-medium">${label}</span><span class="font-bold text-right">${value}</span></div>`;
}

const MONTHS = ['TH1', 'TH2', 'TH3', 'TH4', 'TH5', 'TH6', 'TH7', 'TH8', 'TH9', 'TH10', 'TH11', 'TH12'];

/** Ô lịch nhỏ (tháng + ngày) cạnh giờ bắt đầu, như thẻ "Trial lesson details". */
function dateBadge(monthLabel, day) {
  return `<div class="w-14 shrink-0 rounded-xl border border-slate-200 bg-slate-50 text-center py-1.5">
    <div class="text-[11px] font-semibold text-rose-600 uppercase">${monthLabel}</div>
    <div class="text-2xl font-bold leading-none mt-0.5">${day}</div>
  </div>`;
}

document.addEventListener('DOMContentLoaded', async () => {
  // Quay lại trang trước nếu đến từ trong site; mở thẳng link thì về trang gia sư / danh sách gia sư
  const backTutorId = new URLSearchParams(window.location.search).get('tutorId');
  $('bc-back').addEventListener('click', () => {
    let sameSite = false;
    try { sameSite = !!document.referrer && new URL(document.referrer).origin === window.location.origin; } catch { /* referrer lỗi */ }
    if (sameSite && window.history.length > 1) { window.history.back(); return; }
    window.location.href = backTutorId
      ? `/pages/tutor-profile/?id=${encodeURIComponent(backTutorId)}`
      : '/pages/tutor-discovery/';
  });

  let params = parseBookingParams(window.location.search);
  if (!params) {
    showFatal('Thông tin đặt lịch không hợp lệ', 'Giờ đã chọn bị thiếu hoặc đã qua. Vui lòng chọn lại giờ ở trang gia sư.', '/pages/tutor-discovery/');
    return;
  }
  if (!isLoggedIn()) {
    window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`;
    return;
  }

  const isTrial = params.type === 'trial';

  let tutor;
  try {
    tutor = await API.tutors.getDetail(params.tutorId);
  } catch (err) {
    console.error('Lỗi khi tải gia sư:', err);
  }
  if (!tutor) {
    showFatal('Không thể tải thông tin gia sư', 'Vui lòng kiểm tra lại đường truyền hoặc chọn gia sư khác.', '/pages/tutor-discovery/');
    return;
  }

  // ---- Đổ dữ liệu
  document.title = `${isTrial ? 'Xác nhận học thử' : 'Xác nhận kết nối'} - ${tutor.fullName || 'Gia sư'} | KizunaTutor`;
  renderAvatar($('bc-avatar'), tutor);
  $('bc-tutor-name').textContent = tutor.fullName || 'Gia sư';
  $('bc-title').textContent = isTrial ? 'Xác nhận đặt lịch học thử 1–1' : 'Xác nhận yêu cầu kết nối';
  $('bc-details-title').textContent = isTrial ? 'Chi tiết buổi học thử' : 'Ca học hằng tuần';
  $('bc-details-note').textContent = isTrial
    ? 'Miễn phí học thử 1-1 · Giờ chỉ được giữ khi gia sư đồng ý'
    : 'Giờ chỉ được giữ khi gia sư chấp nhận yêu cầu';
  /** Vẽ giờ đã chọn ở cột trái (gọi lại mỗi khi đổi giờ trong popup). */
  function renderSelection() {
    $('bc-submit-text').textContent = isTrial
      ? (params.offSchedule ? 'Gửi đề xuất học thử' : 'Xác nhận gửi lịch học thử')
      : 'Gửi yêu cầu kết nối';
    if (isTrial) {
      const [date, time] = params.start.split('T');
      const [y, mo, d] = date.split('-').map(Number);
      const weekday = DAY_NAMES[new Date(Date.UTC(y, mo - 1, d)).getUTCDay()];
      // Chỉ hiện giờ bắt đầu (không hiện giờ kết thúc)
      $('bc-summary').innerHTML = `
        <div class="flex items-center gap-4">
          ${dateBadge(MONTHS[mo - 1], d)}
          <div>
            <div class="text-base font-bold">Bắt đầu lúc ${time}</div>
            <div class="text-xs text-slate-500">${weekday}, ${d}/${mo}/${y} · giờ Việt Nam</div>
          </div>
        </div>
        <div class="pt-2 space-y-1.5">
          ${params.offSchedule ? row('Loại yêu cầu:', '<span class="text-amber-700">Đề xuất ngoài lịch rảnh</span>') : ''}
          ${row('Hình thức:', 'Trực tuyến (Google Meet)')}
          ${row('Học phí:', '<span class="text-emerald-600">0 VNĐ</span>')}
        </div>`;
    } else {
      $('bc-summary').innerHTML = params.slots
        .map(s => `<div class="flex items-center gap-2 px-3 py-2 rounded-xl bg-slate-50 border border-slate-200">
          <span class="material-symbols-outlined text-[18px] text-[#006194]">event_repeat</span>
          <span class="font-bold text-sm">${dowName(s.dayOfWeek)} · ${s.startTime}</span>
        </div>`).join('')
        + '<div class="pt-1 text-[11px] text-slate-500">Giờ bắt đầu mong muốn. Gia sư sẽ quyết định độ dài mỗi ca khi đồng ý.</div>';
    }
  }

  // "Đổi giờ": mở popup chọn giờ ngay trên trang này, chọn xong cập nhật lại cột trái (giữ nguyên form đã nhập)
  const pickers = createBookingPickers(tutor, {
    continueLabel: 'Cập nhật giờ',
    onContinue: (sel) => {
      const next = parseBookingParams(`?${buildBookingQuery(sel)}`);
      if (!next) return;
      params = next;
      window.history.replaceState(null, '', `?${buildBookingQuery(sel)}`);
      renderSelection();
      showError('');
      closeModal(isTrial ? 'connect-modal' : 'direct-connect-modal');
    }
  });
  $('bc-change-time').addEventListener('click', () => {
    if (isTrial) {
      pickers.openTrialModal({ date: params.start.slice(0, 10), start: toMinutes(params.start.slice(11)), propose: params.offSchedule });
    } else {
      pickers.openDirectModal(null, { slots: params.slots });
    }
  });

  renderSelection();
  if (!isTrial) prepareMode(tutor);

  renderGoalPickers(tutor, null, '#bc-goals');
  renderLearnerLevel();
  $('bc-loading').classList.add('hidden');
  $('bc-content').classList.remove('hidden');

  // ---- Gửi
  const submitBtn = $('bc-submit');
  submitBtn.addEventListener('click', async () => {
    showError('');
    const notes = $('bc-notes').value.trim();
    const contact = readContact('bc');
    if (contact.error) { showError(contact.error); return; }
    const base = {
      tutorId: tutor.tutorId,
      contactPhone: contact.contactPhone,
      contactEmail: contact.contactEmail,
      shareContact: contact.shareContact
    };

    let payload;
    if (isTrial) {
      payload = {
        ...base,
        scheduledStartTime: params.scheduledStartTime,
        durationMinutes: TRIAL_DURATION_MINUTES,
        learningMode: 'ONLINE',
        locationDetails: null,
        // Trình độ: server lấy từ hồ sơ học viên
        learningGoals: selectedGoals('bc-goals'),
        initialMessage: notes || 'Đăng ký học thử trực tuyến 1–1',
        offSchedule: params.offSchedule
      };
    } else {
      const learningMode = modeOf();
      const locationDetails = learningMode === 'OFFLINE' ? $('bc-location').value.trim() : null;
      if (learningMode === 'OFFLINE' && !locationDetails) {
        showError('Vui lòng nhập khu vực / địa điểm học khi chọn học trực tiếp.');
        return;
      }
      payload = {
        ...base,
        slots: params.slots,
        learningMode,
        locationDetails,
        learningGoals: selectedGoals('bc-goals'),
        initialMessage: notes || null
      };
    }

    submitBtn.disabled = true;
    const original = submitBtn.innerHTML;
    submitBtn.innerHTML = '<span class="material-symbols-outlined animate-spin text-[18px]">progress_activity</span><span>Đang gửi...</span>';
    try {
      const created = isTrial
        ? await API.trialBookings.create(payload)
        : await API.connectionRequests.create(payload);
      showSuccess(tutor, isTrial, created);
    } catch (err) {
      console.error('Lỗi khi gửi yêu cầu:', err);
      submitBtn.disabled = false;
      submitBtn.innerHTML = original;
      if (err && err.status === 409) {
        // Giờ vừa có người nhận: tải lại lịch rồi mở popup để chọn giờ khác
        showError('Giờ bạn chọn vừa có người đặt hoặc không còn trống. Hãy chọn giờ khác.');
        pickers.reloadWeek().then(() => $('bc-change-time').click());
        return;
      }
      showError(friendlyError(err, 'Không thể gửi yêu cầu lúc này. Vui lòng thử lại sau.'));
    }
  });
});

// Hình thức học theo bài đăng của gia sư: BOTH → học viên chọn; ONLINE/OFFLINE (chưa khai báo = online) → cố định.
const modeOf = () => document.querySelector('input[name="bc-mode"]:checked')?.value || 'ONLINE';
function prepareMode(tutor) {
  const format = String(tutor.teachingFormatCode || 'ONLINE').toUpperCase();
  const fixed = format === 'BOTH' ? null : (format === 'OFFLINE' ? 'OFFLINE' : 'ONLINE');
  document.querySelectorAll('input[name="bc-mode"]').forEach((input) => {
    input.closest('label')?.classList.toggle('hidden', Boolean(fixed) && input.value !== fixed);
    input.addEventListener('change', syncLocation);
  });
  const radio = document.querySelector(`input[name="bc-mode"][value="${fixed || 'ONLINE'}"]`);
  if (radio) radio.checked = true;
  $('bc-mode-box').classList.remove('hidden');
  syncLocation();
}
function syncLocation() {
  $('bc-location-box').classList.toggle('hidden', modeOf() !== 'OFFLINE');
}

function showSuccess(tutor, isTrial, created) {
  const name = escapeHtml(tutor.fullName || '');
  const isProposal = isTrial && !!(created && created.offSchedule);
  $('bc-change-time').classList.add('hidden');
  $('bc-footer').classList.add('hidden');
  $('bc-body').innerHTML = `
    <div class="p-8 text-center space-y-4">
      <div class="w-16 h-16 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto border border-emerald-100">
        <span class="material-symbols-outlined text-[34px]">check_circle</span>
      </div>
      <h2 class="text-lg font-bold">${isTrial ? 'Đã gửi yêu cầu học thử thành công!' : 'Đã gửi yêu cầu kết nối'}</h2>
      <p class="text-xs text-slate-500 leading-relaxed">
        ${isTrial
          ? `Yêu cầu học thử ${isProposal ? '(đề xuất ngoài lịch rảnh) ' : ''}đã được gửi tới <strong>Sensei ${name}</strong>. Sensei sẽ xem thời gian bạn chọn và đồng ý hoặc từ chối.`
          : `Sensei <strong>${name}</strong> sẽ xem các ca bạn chọn và phản hồi sớm. Khi gia sư đồng ý, hai bạn được kết nối và có thể bắt đầu lớp học chính thức.`}
      </p>
      <div class="grid grid-cols-2 gap-2.5 pt-2">
        <a href="/pages/learner-requests/?tab=${isTrial ? 'trial' : 'direct'}" class="py-2.5 px-3 rounded-xl bg-[#006194] hover:bg-[#004b73] text-white text-xs font-bold flex items-center justify-center gap-1.5">
          <span class="material-symbols-outlined text-[16px]">list_alt</span><span>Xem yêu cầu của tôi</span>
        </a>
        <a href="/pages/tutor-profile/?id=${encodeURIComponent(tutor.tutorId)}" class="py-2.5 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold flex items-center justify-center">
          Về trang gia sư
        </a>
      </div>
    </div>`;
}
