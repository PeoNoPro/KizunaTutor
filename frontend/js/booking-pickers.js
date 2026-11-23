/**
 * Popup chọn giờ dùng chung cho trang gia sư (tutor-profile) và trang xác nhận (booking-confirm):
 *  - Học thử 30 phút: chọn giờ trống trong lịch rảnh theo tuần, hoặc đề xuất giờ khác.
 *  - Kết nối: chọn các ca hằng tuần + môn học (môn quyết định thời lượng ca).
 * Popup chỉ chọn giờ; bấm "Tiếp tục" gọi onContinue(sel) với sel đúng dạng của buildBookingQuery.
 */
import { API } from './api.js';
import { openModal, closeModal } from '../components/modal/modal.js';
import { escapeHtml } from './profile-utils.js';

export const WEEKDAY_COLUMNS = [
  { dow: 2, shortName: 'Thứ 2', fullName: 'Thứ hai' },
  { dow: 3, shortName: 'Thứ 3', fullName: 'Thứ ba' },
  { dow: 4, shortName: 'Thứ 4', fullName: 'Thứ tư' },
  { dow: 5, shortName: 'Thứ 5', fullName: 'Thứ năm' },
  { dow: 6, shortName: 'Thứ 6', fullName: 'Thứ sáu' },
  { dow: 7, shortName: 'Thứ 7', fullName: 'Thứ bảy' },
  { dow: 1, shortName: 'Chủ nhật', fullName: 'Chủ nhật' }
];
export const TRIAL_DURATION_MINUTES = 30;
// Học viên chọn bao nhiêu ca mỗi tuần cũng được (chỉ chặn số quá lớn để tránh gửi nhầm)
const MAX_SELECTED_SLOTS = 50;
const DAY_NAMES = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'];

export const pad = (n) => String(n).padStart(2, '0');
export const toMinutes = (hhmm) => {
  const [h, m] = String(hhmm).split(':').map(Number);
  return h * 60 + m;
};
export const toHHMM = (minutes) => `${pad(Math.floor(minutes / 60))}:${pad(minutes % 60)}`;
export const fmtDM = (iso) => { const [, mo, d] = String(iso).split('-'); return `${d}/${mo}`; };
export const fmtDMY = (iso) => { const [y, mo, d] = String(iso).split('-'); return `${d}/${mo}/${y}`; };
export const todayIsoVN = () => new Date(Date.now() + 7 * 3600 * 1000).toISOString().slice(0, 10);
const addDaysIso = (iso, n) => {
  const [y, mo, d] = String(iso).split('-').map(Number);
  return new Date(Date.UTC(y, mo - 1, d + n)).toISOString().slice(0, 10);
};
const dayNameOf = (dow) => (WEEKDAY_COLUMNS.find(c => c.dow === dow) || {}).fullName || '';
// Chỉ hiện giờ bắt đầu học viên mong muốn (không hiện giờ kết thúc)
const slotLabel = (s) => `${dayNameOf(s.dow)} ${toHHMM(s.start)}`;
const overlaps = (aStart, aEnd, bStart, bEnd) => aStart < bEnd && aEnd > bStart;
const dowOrder = (d) => WEEKDAY_COLUMNS.findIndex(c => c.dow === d);

/** Các giờ bắt đầu (bước 30 phút) đủ chỗ cho một ca dài `duration` phút trong các khoảng trống cho trước. */
export function startTimesFrom(ranges, duration) {
  const starts = [];
  ranges.forEach(r => {
    for (let t = Math.ceil(r.start / 30) * 30; t + duration <= r.end && t + duration < 24 * 60; t += 30) {
      starts.push(t);
    }
  });
  return [...new Set(starts)].sort((a, b) => a - b);
}

const TIME_GROUPS = [
  { label: 'Buổi sáng', icon: 'wb_twilight', from: 0, to: 12 * 60 },
  { label: 'Buổi chiều', icon: 'light_mode', from: 12 * 60, to: 17 * 60 },
  { label: 'Buổi tối', icon: 'nights_stay', from: 17 * 60, to: 24 * 60 }
];
/** Danh sách giờ chia nhóm Buổi sáng / chiều / tối, mỗi nhóm một lưới 3 cột. */
function groupedTimesHtml(starts, renderItem) {
  return TIME_GROUPS.map(g => {
    const items = starts.filter(t => t >= g.from && t < g.to);
    if (!items.length) return '';
    return `
      <div class="space-y-1.5">
        <div class="flex items-center gap-1.5 text-xs font-bold text-[#0F2545]">
          <span class="material-symbols-outlined text-[16px] text-slate-600">${g.icon}</span>${g.label}
        </div>
        <div class="grid grid-cols-4 gap-1.5">${items.map(renderItem).join("")}</div>
      </div>`;
  }).join('');
}
const TIME_BTN = 'w-full py-1.5 px-1 rounded-lg text-xs text-center transition-all';
const TIME_BTN_IDLE = `${TIME_BTN} border border-slate-200 bg-white hover:border-[#006194] hover:bg-sky-50 text-slate-800 font-medium cursor-pointer`;
const TIME_BTN_ACTIVE = `${TIME_BTN} border-2 border-[#006194] bg-[#006194] text-white font-bold cursor-pointer`;

const MODALS_HTML = `
  <!-- Popup chọn giờ học thử -->
  <div id="connect-modal" class="modal-overlay fixed inset-0 z-50 hidden overflow-y-auto bg-black/60 backdrop-blur-sm flex items-center justify-center p-3 md:p-4">
    <div class="relative w-full max-w-[500px] bg-white rounded-3xl border border-[#E5DECB] shadow-2xl overflow-hidden flex flex-col h-[min(760px,92vh)]">
      <div class="flex items-center justify-between px-5 py-3.5 border-b border-[#ECE8DE] bg-white flex-shrink-0">
        <div class="flex items-center gap-3">
          <div id="booking-modal-avatar-container" class="relative shrink-0"></div>
          <div>
            <h3 class="text-base md:text-lg font-bold text-[#0F2545] leading-tight">Đặt lịch học thử 1–1</h3>
            <p id="booking-modal-subtitle" class="text-xs text-slate-500 font-medium mt-0.5">Chọn thời gian bạn muốn học thử</p>
          </div>
        </div>
        <button type="button" data-close-modal="connect-modal" class="w-8 h-8 rounded-full hover:bg-slate-100 flex items-center justify-center text-slate-400 hover:text-slate-600 transition-colors cursor-pointer" aria-label="Đóng">
          <span class="material-symbols-outlined text-[20px]">close</span>
        </button>
      </div>

      <div class="overflow-y-auto flex-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        <div class="px-5 pt-3 pb-2 space-y-2.5">
          <div class="flex items-center justify-between px-3 py-2 rounded-xl bg-slate-50 border border-slate-200/80 text-xs text-[#0F2545]">
            <div class="flex items-center gap-2 font-semibold text-slate-700">
              <span class="material-symbols-outlined text-[#006194] text-[18px]">schedule</span>
              <span>Thời lượng buổi học thử</span>
            </div>
            <span class="px-3 py-1 rounded-xl bg-white border border-slate-200 font-bold text-[#006194]">30 phút</span>
          </div>

          <!-- Cách chính: chọn giờ trống trong lịch rảnh của gia sư -->
          <div id="trial-pick-calendar" class="space-y-3">
            <div class="flex items-center justify-between gap-2 px-2 py-1 rounded-xl bg-sky-50 border border-sky-100">
              <button type="button" id="trial-week-prev" class="w-7 h-7 rounded-lg flex items-center justify-center text-[#006194] hover:bg-white disabled:text-slate-300 disabled:hover:bg-transparent disabled:cursor-not-allowed cursor-pointer" aria-label="Tuần trước">
                <span class="material-symbols-outlined text-[18px]">chevron_left</span>
              </button>
              <div id="trial-week-label" class="text-xs font-bold text-[#0F2545] text-center">Tuần này</div>
              <button type="button" id="trial-week-next" class="w-7 h-7 rounded-lg flex items-center justify-center text-[#006194] hover:bg-white disabled:text-slate-300 disabled:hover:bg-transparent disabled:cursor-not-allowed cursor-pointer" aria-label="Tuần sau">
                <span class="material-symbols-outlined text-[18px]">chevron_right</span>
              </button>
            </div>
            <div id="trial-days" class="grid grid-cols-7 gap-1"></div>
            <div id="trial-times" class="space-y-2.5"></div>
            <button type="button" id="trial-propose-toggle" class="inline-flex items-center gap-1 text-xs font-semibold text-[#006194] hover:underline cursor-pointer">
              <span class="material-symbols-outlined text-[16px]">edit_calendar</span>
              Không có giờ phù hợp? Đề xuất giờ khác
            </button>
          </div>

          <!-- Cách phụ: đề xuất giờ ngoài lịch rảnh (gia sư đồng ý hoặc từ chối) -->
          <div id="trial-pick-propose" class="hidden space-y-3">
            <div class="p-3 rounded-xl bg-amber-50 border border-amber-200 text-[11px] text-amber-800 leading-relaxed flex gap-2">
              <span class="material-symbols-outlined text-[18px] flex-shrink-0">info</span>
              <span>Giờ bạn đề xuất <strong>nằm ngoài lịch rảnh</strong> của gia sư nên gia sư có thể từ chối. Bạn được đề xuất tối đa <strong>2 lần mỗi tuần</strong> với mỗi gia sư. Giờ gia sư đã có lịch thì không đề xuất được.</span>
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div class="space-y-1">
                <label for="trial-date" class="block text-xs font-bold text-[#0F2545]">Ngày học thử (*)</label>
                <input type="date" id="trial-date" class="w-full px-3 py-2 text-xs font-medium rounded-xl border border-slate-300 bg-white text-slate-800 focus:outline-none focus:border-primary">
              </div>
              <div class="space-y-1">
                <label for="trial-time" class="block text-xs font-bold text-[#0F2545]">Giờ bắt đầu (*)</label>
                <input type="time" id="trial-time" step="1800" class="w-full px-3 py-2 text-xs font-medium rounded-xl border border-slate-300 bg-white text-slate-800 focus:outline-none focus:border-primary">
              </div>
            </div>
            <button type="button" id="trial-back-to-calendar" class="inline-flex items-center gap-1 text-xs font-semibold text-[#006194] hover:underline cursor-pointer">
              <span class="material-symbols-outlined text-[16px]">arrow_back</span>
              Quay lại chọn giờ trống trên lịch
            </button>
          </div>

          <p id="trial-time-summary" class="text-[11px] text-slate-500 leading-relaxed">Chọn một giờ trống trong lịch rảnh của gia sư.</p>
        </div>

        <div class="px-5 pb-1">
          <div id="booking-error-alert" class="hidden p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2" role="alert">
            <span class="material-symbols-outlined text-base flex-shrink-0">error</span>
            <span id="booking-error-message"></span>
          </div>
        </div>
      </div>

      <div class="px-5 py-3 border-t border-[#ECE8DE] bg-white flex-shrink-0">
        <button type="button" id="btn-booking-continue" class="w-full py-3 px-4 rounded-2xl text-sm font-bold transition-all flex items-center justify-center gap-2 bg-[#006194] text-white shadow-md hover:bg-[#004b73] active:scale-[0.99] cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed">
          <span data-continue-label>Tiếp tục</span>
          <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
        </button>
      </div>
    </div>
  </div>

  <!-- Popup chọn ca kết nối hằng tuần + môn học -->
  <div id="direct-connect-modal" class="modal-overlay fixed inset-0 z-50 hidden overflow-y-auto bg-black/60 backdrop-blur-sm flex items-center justify-center p-3 md:p-4">
    <div class="relative w-full max-w-[560px] bg-white rounded-3xl border border-[#E5DECB] shadow-2xl overflow-hidden flex flex-col h-[min(760px,92vh)]">
      <div class="flex items-center justify-between px-5 py-3.5 border-b border-[#ECE8DE] bg-white flex-shrink-0">
        <div class="flex items-center gap-3">
          <div id="direct-modal-avatar-container" class="relative shrink-0"></div>
          <div>
            <h3 class="text-base md:text-lg font-bold text-[#0F2545] leading-tight">Kết nối với gia sư</h3>
            <p id="direct-modal-subtitle" class="text-xs text-slate-500 font-medium mt-0.5">Chọn ca học cố định mỗi tuần, không cần học thử trước</p>
          </div>
        </div>
        <button type="button" data-close-modal="direct-connect-modal" class="w-8 h-8 rounded-full hover:bg-slate-100 flex items-center justify-center text-slate-400 hover:text-slate-600 transition-colors cursor-pointer" aria-label="Đóng">
          <span class="material-symbols-outlined text-[20px]">close</span>
        </button>
      </div>

      <div class="overflow-y-auto flex-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        <div class="px-5 py-3 space-y-3">
          <div class="space-y-1.5">
            <div class="text-xs font-semibold text-slate-600">Thứ trong tuần</div>
            <div id="dc-days" class="grid grid-cols-7 gap-1"></div>
          </div>
          <div class="space-y-1.5">
            <div id="dc-times-label" class="text-xs font-semibold text-slate-600">Giờ bắt đầu</div>
            <div id="dc-times" class="space-y-2.5"></div>
          </div>
          <div class="space-y-1.5 pt-1">
            <div class="text-xs font-bold text-[#0F2545]">Ca đã chọn</div>
            <div id="dc-selected" class="flex flex-wrap gap-2"></div>          </div>
        </div>

        <div class="px-5 pb-1">
          <div id="direct-error-alert" class="hidden p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2" role="alert">
            <span class="material-symbols-outlined text-base flex-shrink-0">error</span>
            <span id="direct-error-message"></span>
          </div>
        </div>
      </div>

      <div class="px-5 py-3 border-t border-[#ECE8DE] bg-white flex-shrink-0">
        <button type="button" id="btn-direct-submit" class="w-full py-3 px-4 rounded-2xl text-sm font-bold transition-all flex items-center justify-center gap-2 bg-[#006194] text-white shadow-md hover:bg-[#004b73] active:scale-[0.99] cursor-pointer disabled:bg-slate-200 disabled:text-slate-400 disabled:shadow-none disabled:cursor-not-allowed">
          <span data-continue-label>Tiếp tục</span>
          <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
        </button>
      </div>
    </div>
  </div>`;

/**
 * options:
 *  onContinue(sel)          — bấm "Tiếp tục" với giờ hợp lệ (sel: dạng của buildBookingQuery)
 *  continueLabel            — chữ trên nút tiếp tục (mặc định "Tiếp tục")
 *  onWeekLoading/onWeekLoaded/onWeekError(err, retry) — để trang gia sư vẽ bảng lịch rảnh của nó
 */
export function createBookingPickers(tutor, options = {}) {
  const { onContinue, continueLabel, onWeekLoading, onWeekLoaded, onWeekError } = options;
  document.body.insertAdjacentHTML('beforeend', MODALS_HTML);
  const $ = (id) => document.getElementById(id);
  document.querySelectorAll('[data-close-modal]').forEach(btn =>
    btn.addEventListener('click', () => closeModal(btn.dataset.closeModal)));
  if (continueLabel) document.querySelectorAll('[data-continue-label]').forEach(el => { el.textContent = continueLabel; });

  // ---- Đầu popup: ảnh + tên gia sư
  const name = String(tutor.fullName || '').trim();
  const initials = name ? name.split(/\s+/).map(p => p[0]).filter(Boolean).slice(-2).join('').toUpperCase() : 'GS';
  const initialsHtml = `<div class="w-12 h-12 rounded-2xl bg-sky-100 text-sky-800 font-bold flex items-center justify-center border border-slate-200 text-sm">${escapeHtml(initials)}</div>`;
  // Cùng một ảnh ở đầu cả popup học thử lẫn popup kết nối
  ['booking-modal-avatar-container', 'direct-modal-avatar-container'].forEach((id) => {
    const avatarBox = $(id);
    if (!tutor.avatarUrl) { avatarBox.innerHTML = initialsHtml; return; }
    const img = document.createElement('img');
    img.src = tutor.avatarUrl;
    img.alt = 'Avatar gia sư';
    img.className = 'w-12 h-12 rounded-2xl object-cover border border-[#E5DECB]';
    img.onerror = () => { avatarBox.innerHTML = initialsHtml; };
    avatarBox.replaceChildren(img);
  });
  if (name) {
    $('booking-modal-subtitle').textContent = `Chọn thời gian bạn muốn học thử cùng Sensei ${name}`;
    $('direct-modal-subtitle').textContent = `Chọn ca học cố định mỗi tuần cùng Sensei ${name}, không cần học thử trước`;
  }

  // ---------------------------------------------------------------- Lịch rảnh theo tuần
  // Server chỉ trả về khung giờ còn trống: đã trừ lịch lớp, ca đã có học viên, buổi học thử và giờ đã qua.
  const weekCache = new Map(); // weekStart "YYYY-MM-DD" -> dữ liệu tuần
  let weekData = null;         // tuần đang xem
  let weekLoadSeq = 0;
  const hasAvailability = () => (tutor.availabilities || []).length > 0;

  function dayOf(dow) {
    return weekData ? (weekData.days || []).find(d => Number(d.dayOfWeek) === dow) : null;
  }

  /** Khung giờ còn trống của một thứ trong tuần đang xem (phút từ 00:00). */
  function freeRangesOf(dow) {
    const day = dayOf(dow);
    return (day ? day.freeRanges || [] : [])
      .map(r => ({ start: toMinutes(r.startTime), end: toMinutes(r.endTime) }))
      .sort((x, y) => x.start - y.start);
  }

  async function loadWeek(weekStart = null) {
    const key = weekStart || '__current__';
    if (weekCache.has(key)) return weekCache.get(key);
    // Khoảng trống từ 30 phút (đủ cho một buổi học thử)
    const data = await API.tutors.getWeeklySchedule(tutor.tutorId, weekStart, TRIAL_DURATION_MINUTES);
    weekCache.set(data.weekStart, data);
    if (!weekStart) weekCache.set(key, data);
    return data;
  }

  /** Chuyển sang tuần khác: tải dữ liệu rồi vẽ lại popup và (qua onWeekLoaded) bảng lịch của trang. */
  async function showWeek(weekStart = null) {
    const seq = ++weekLoadSeq;
    if (onWeekLoading) onWeekLoading();
    setWeekNavBusy(true);
    try {
      const data = await loadWeek(weekStart);
      if (seq !== weekLoadSeq) return;
      weekData = data;
      renderConnectForm();
      renderTrialPicker();
      if (onWeekLoaded) onWeekLoaded();
    } catch (err) {
      if (seq !== weekLoadSeq) return;
      console.error('Lỗi khi tải lịch rảnh theo tuần:', err);
      if (onWeekError) onWeekError(err, () => showWeek(weekStart));
    } finally {
      if (seq === weekLoadSeq) setWeekNavBusy(false);
    }
  }

  function reloadWeek() {
    weekCache.clear();
    return showWeek(weekData ? weekData.weekStart : null);
  }

  function setWeekNavBusy(busy) {
    const canPrev = !!weekData && weekData.weekStart > weekData.minWeekStart;
    const canNext = !!weekData && weekData.weekStart < weekData.maxWeekStart;
    ['week-prev-btn', 'trial-week-prev'].forEach(id => { const el = $(id); if (el) el.disabled = busy || !canPrev; });
    ['week-next-btn', 'trial-week-next'].forEach(id => { const el = $(id); if (el) el.disabled = busy || !canNext; });
  }

  function shiftWeek(delta) {
    if (!weekData) return;
    const target = addDaysIso(weekData.weekStart, delta * 7);
    if (target < weekData.minWeekStart || target > weekData.maxWeekStart) return;
    showWeek(target);
  }

  function renderWeekHeader() {
    const rangeEl = $('week-range-label');
    if (weekData && rangeEl) rangeEl.textContent = `${fmtDM(weekData.weekStart)} – ${fmtDMY(weekData.weekEnd)}`;
    setWeekNavBusy(false);
  }

  // ---------------------------------------------------------------- Popup kết nối
  // Yêu cầu kết nối chỉ là giờ BẮT ĐẦU mong muốn: không giữ chỗ; gia sư quyết định độ dài mỗi ca khi đồng ý.
  let connectDay = null;    // dow đang xem trong form
  const selectedSlots = []; // { dow, start (phút từ 00:00) }

  /** MẪU lịch rảnh hằng tuần: lịch rảnh trừ lịch lớp chính thức (lặp mọi tuần). */
  function patternRangesOf(dow) {
    const busy = [...(tutor.classSchedules || []), ...(tutor.lockedSlots || [])]
      .filter(b => Number(b.dayOfWeek) === dow && b.startTime && b.endTime)
      .map(b => ({ start: toMinutes(b.startTime), end: toMinutes(b.endTime) }));
    const result = [];
    (tutor.availabilities || [])
      .filter(a => Number(a.dayOfWeek) === dow && a.startTime && a.endTime)
      .forEach(a => {
        let pieces = [{ start: toMinutes(a.startTime), end: toMinutes(a.endTime) || 24 * 60 }];
        busy.forEach(b => {
          pieces = pieces.flatMap(p => {
            if (!overlaps(p.start, p.end, b.start, b.end)) return [p];
            const parts = [];
            if (b.start > p.start) parts.push({ start: p.start, end: b.start });
            if (b.end < p.end) parts.push({ start: b.end, end: p.end });
            return parts;
          });
        });
        result.push(...pieces);
      });
    return result.sort((x, y) => x.start - y.start);
  }

  /** Giờ bắt đầu mong muốn: mọi mốc 30 phút trong giờ rảnh (gia sư nhận yêu cầu rồi tự sắp thời lượng). */
  function startTimesOf(dow) {
    return startTimesFrom(patternRangesOf(dow), 30);
  }

  const directSubmitBtn = $('btn-direct-submit');
  function showDirectError(msg) {
    $('direct-error-message').textContent = msg;
    $('direct-error-alert').classList.toggle('hidden', !msg);
    if (msg) $('direct-error-alert').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  function renderConnectForm() {
    if (weekData) renderWeekHeader();
    const daysEl = $('dc-days');
    daysEl.innerHTML = WEEKDAY_COLUMNS.map(col => {
      const available = startTimesOf(col.dow).length > 0;
      const active = col.dow === connectDay;
      const count = selectedSlots.filter(s => s.dow === col.dow).length;
      const cls = active
        ? 'border-2 border-[#006194] bg-sky-50 text-[#006194] font-bold'
        : available ? 'border border-slate-200 hover:bg-slate-50 text-slate-800 font-semibold' : 'border border-transparent text-slate-300';
      return `
        <button type="button" data-day="${col.dow}" ${available || count ? '' : 'disabled'}
          class="relative py-2 rounded-xl text-[11px] sm:text-xs transition-all ${available || count ? 'cursor-pointer' : 'cursor-not-allowed'} ${cls}">
          ${col.shortName === 'Chủ nhật' ? 'CN' : col.shortName.replace('Thứ ', 'T')}
          ${count ? `<span class="absolute -top-1.5 -right-1 min-w-4 h-4 px-1 rounded-full bg-[#006194] text-white text-[10px] leading-4">${count}</span>` : ''}
        </button>`;
    }).join('');
    daysEl.querySelectorAll('[data-day]').forEach(btn => btn.addEventListener('click', () => {
      connectDay = Number(btn.dataset.day);
      renderConnectForm();
    }));

    // Giờ bắt đầu của ngày đang chọn
    const timesEl = $('dc-times');
    $('dc-times-label').textContent = connectDay ? `Giờ bắt đầu - ${dayNameOf(connectDay)}` : 'Giờ bắt đầu';
    const starts = connectDay ? startTimesOf(connectDay) : [];
    const daySelected = selectedSlots.filter(s => s.dow === connectDay);
    const allStarts = [...new Set([...starts, ...daySelected.map(s => s.start)])].sort((a, b) => a - b);
    if (!allStarts.length) {
      timesEl.innerHTML = `<div class="py-4 text-center text-xs text-slate-500">${connectDay
        ? `Không còn giờ trống vào ${dayNameOf(connectDay)}. Hãy chọn thứ khác.`
        : 'Gia sư chưa có khung giờ trống phù hợp.'}</div>`;
    } else {
      timesEl.innerHTML = groupedTimesHtml(allStarts, t => {
        // Chỉ chọn giờ bắt đầu nên không khóa giờ gần nhau; gia sư chọn độ dài và kiểm tra chồng ca khi đồng ý
        if (daySelected.some(s => s.start === t)) return `<button type="button" data-start="${t}" class="${TIME_BTN_ACTIVE}" title="Bỏ chọn">${toHHMM(t)}</button>`;
        return `<button type="button" data-start="${t}" class="${TIME_BTN_IDLE}" title="${dayNameOf(connectDay)} ${toHHMM(t)}">${toHHMM(t)}</button>`;
      });
      timesEl.querySelectorAll('[data-start]').forEach(btn => btn.addEventListener('click', () => toggleSlot(connectDay, Number(btn.dataset.start))));
    }

    // Ca đã chọn
    const selEl = $('dc-selected');
    selEl.innerHTML = selectedSlots.length
      ? selectedSlots.map((s, i) => `
          <span class="inline-flex items-center gap-1.5 pl-3 pr-1.5 py-1 rounded-full bg-sky-50 border border-sky-200 text-xs font-semibold text-[#006194]">
            ${slotLabel(s)}
            <button type="button" data-remove-index="${i}" class="w-5 h-5 rounded-full hover:bg-sky-100 flex items-center justify-center cursor-pointer" aria-label="Bỏ chọn ${slotLabel(s)}">
              <span class="material-symbols-outlined text-[14px]">close</span>
            </button>
          </span>`).join('')
      : '<span class="text-xs text-slate-500">Chưa chọn ca nào. Hãy chọn thứ rồi bấm vào giờ bắt đầu.</span>';
    selEl.querySelectorAll('[data-remove-index]').forEach(btn => btn.addEventListener('click', () => {
      selectedSlots.splice(Number(btn.dataset.removeIndex), 1);
      renderConnectForm();
    }));

    // Nút tiếp tục chỉ bật khi đã chọn ít nhất một ca
    directSubmitBtn.disabled = selectedSlots.length === 0;
  }

  function addSlot(dow, start) {
    selectedSlots.push({ dow, start });
    selectedSlots.sort((a, b) => dowOrder(a.dow) - dowOrder(b.dow) || a.start - b.start);
  }

  function toggleSlot(dow, start) {
    showDirectError('');
    const idx = selectedSlots.findIndex(s => s.dow === dow && s.start === start);
    if (idx >= 0) {
      selectedSlots.splice(idx, 1);
    } else if (selectedSlots.length >= MAX_SELECTED_SLOTS) {
      showDirectError('Bạn đã chọn quá nhiều ca. Hãy bỏ bớt một số ca.');
      return;
    } else {
      addSlot(dow, start);
    }
    renderConnectForm();
  }

  /**
   * preselect: { dow, start } khi học viên bấm một giờ trên bảng lịch rảnh.
   * initial:   { slots: [{ dayOfWeek, startTime }] } khi mở lại từ trang xác nhận.
   */
  function openDirectModal(preselect = null, initial = null) {
    if (!weekData && hasAvailability()) {
      // Lịch tuần chưa tải xong (hoặc tải lỗi): tải rồi mở lại form
      showWeek().then(() => { if (weekData) openDirectModal(preselect, initial); });
      return;
    }
    if (initial) {
      // Nạp lại đúng các ca đã chọn trước đó
      selectedSlots.length = 0;
      (initial.slots || []).forEach(s => addSlot(s.dayOfWeek, toMinutes(s.startTime)));
      if (selectedSlots.length) connectDay = selectedSlots[0].dow;
    } else if (preselect) {
      connectDay = preselect.dow;
      const already = selectedSlots.some(s => s.dow === preselect.dow && s.start === preselect.start);
      if (!already && selectedSlots.length < MAX_SELECTED_SLOTS && startTimesOf(preselect.dow).includes(preselect.start)) {
        addSlot(preselect.dow, preselect.start);
      }
    }
    if (!connectDay || (!startTimesOf(connectDay).length && !selectedSlots.some(s => s.dow === connectDay))) {
      const firstDay = WEEKDAY_COLUMNS.find(c => startTimesOf(c.dow).length > 0);
      connectDay = firstDay ? firstDay.dow : null;
    }
    showDirectError('');
    renderConnectForm();
    openModal('direct-connect-modal');
  }

  directSubmitBtn.addEventListener('click', () => {
    showDirectError('');
    if (!selectedSlots.length) {
      showDirectError('Hãy chọn ít nhất một ca học hằng tuần.');
      return;
    }
    onContinue && onContinue({
      type: 'connect',
      tutorId: tutor.tutorId,
      slots: selectedSlots.map(s => ({ dayOfWeek: s.dow, startTime: toHHMM(s.start) }))
    });
  });

  // ---------------------------------------------------------------- Popup học thử
  // Hai cách chọn giờ: 'calendar' (giờ trống trong lịch rảnh) hoặc 'propose' (đề xuất ngoài lịch rảnh).
  const continueBtn = $('btn-booking-continue');
  const trialDateInput = $('trial-date');
  const trialTimeInput = $('trial-time');
  const trialSummary = $('trial-time-summary');
  let trialMode = 'calendar';
  let trialDay = null;   // ngày đang xem trong lịch ("YYYY-MM-DD")
  let trialPick = null;  // { date: "YYYY-MM-DD", start: phút từ 00:00 }

  function showError(msg) {
    $('booking-error-message').textContent = msg;
    $('booking-error-alert').classList.remove('hidden');
    $('booking-error-alert').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }
  const clearError = () => $('booking-error-alert').classList.add('hidden');

  function trialStartsOfDate(date) {
    const day = weekData ? (weekData.days || []).find(d => d.date === date) : null;
    if (!day) return [];
    const ranges = (day.freeRanges || []).map(r => ({ start: toMinutes(r.startTime), end: toMinutes(r.endTime) }));
    return startTimesFrom(ranges, TRIAL_DURATION_MINUTES);
  }

  function renderTrialPicker() {
    $('trial-pick-calendar').classList.toggle('hidden', trialMode !== 'calendar');
    $('trial-pick-propose').classList.toggle('hidden', trialMode !== 'propose');
    if (trialMode !== 'calendar' || !weekData) {
      updateTrialSummary();
      return;
    }
    $('trial-week-label').textContent = `${fmtDM(weekData.weekStart)} – ${fmtDMY(weekData.weekEnd)}`;

    const days = weekData.days || [];
    if (!trialDay || !days.some(d => d.date === trialDay)) {
      const firstFree = days.find(d => trialStartsOfDate(d.date).length > 0);
      trialDay = firstFree ? firstFree.date : (days[0] ? days[0].date : null);
    }
    // Giờ đã chọn ở tuần khác vẫn giữ; chỉ bỏ nếu thuộc tuần đang xem mà không còn trống
    if (trialPick && days.some(d => d.date === trialPick.date) && !trialStartsOfDate(trialPick.date).includes(trialPick.start)) trialPick = null;

    const daysEl = $('trial-days');
    daysEl.innerHTML = days.map(d => {
      const count = trialStartsOfDate(d.date).length;
      const active = d.date === trialDay;
      const col = WEEKDAY_COLUMNS.find(c => c.dow === Number(d.dayOfWeek));
      const short = col ? (col.shortName === 'Chủ nhật' ? 'CN' : col.shortName.replace('Thứ ', 'T')) : '';
      const cls = active
        ? 'border-2 border-[#006194] bg-sky-50 text-[#006194] font-bold'
        : count ? 'border border-slate-200 hover:bg-slate-50 text-slate-800 font-semibold' : 'border border-transparent text-slate-300';
      return `
        <button type="button" data-trial-day="${d.date}" ${count ? '' : 'disabled'}
          class="py-1.5 rounded-xl text-[11px] leading-tight transition-all ${count ? 'cursor-pointer' : 'cursor-not-allowed'} ${cls}">
          <span class="block">${short}</span><span class="block text-[10px] font-medium">${fmtDM(d.date)}</span>
        </button>`;
    }).join('');
    daysEl.querySelectorAll('[data-trial-day]').forEach(btn => btn.addEventListener('click', () => {
      trialDay = btn.dataset.trialDay;
      clearError();
      renderTrialPicker();
    }));

    const timesEl = $('trial-times');
    const starts = trialDay ? trialStartsOfDate(trialDay) : [];
    timesEl.innerHTML = starts.length ? groupedTimesHtml(starts, t => {
      const chosen = trialPick && trialPick.date === trialDay && trialPick.start === t;
      return `<button type="button" data-trial-start="${t}" class="${chosen ? TIME_BTN_ACTIVE : TIME_BTN_IDLE}" title="${toHHMM(t)}">${toHHMM(t)}</button>`;
    }) : '<div class="py-3 text-center text-xs text-slate-500">Tuần này gia sư không còn giờ trống. Hãy xem tuần sau hoặc đề xuất giờ khác.</div>';
    timesEl.querySelectorAll('[data-trial-start]').forEach(btn => btn.addEventListener('click', () => {
      trialPick = { date: trialDay, start: Number(btn.dataset.trialStart) };
      clearError();
      renderTrialPicker();
    }));
    updateTrialSummary();
  }

  function setTrialMode(mode) {
    trialMode = mode;
    clearError();
    renderTrialPicker();
  }

  /** Ngày giờ học thử đã chọn dạng "YYYY-MM-DDTHH:MM" (giờ Việt Nam), hoặc null nếu chưa đủ. */
  function getTrialStart() {
    if (trialMode === 'calendar') return trialPick ? `${trialPick.date}T${toHHMM(trialPick.start)}` : null;
    if (!trialDateInput.value || !trialTimeInput.value) return null;
    return `${trialDateInput.value}T${trialTimeInput.value.slice(0, 5)}`;
  }

  function updateTrialSummary() {
    const start = getTrialStart();
    continueBtn.disabled = !start;
    if (!start) {
      trialSummary.textContent = trialMode === 'propose'
        ? 'Chọn ngày và giờ bạn muốn đề xuất. Gia sư sẽ xem và đồng ý hoặc từ chối.'
        : 'Chọn một giờ trống trong lịch rảnh của gia sư.';
      return;
    }
    // Chỉ hiện giờ bắt đầu (không hiện giờ kết thúc)
    const [date, time] = start.split('T');
    const [y, mo, d] = date.split('-').map(Number);
    const when = `${DAY_NAMES[new Date(Date.UTC(y, mo - 1, d)).getUTCDay()]}, ${pad(d)}/${pad(mo)}/${y} · ${time}`;
    trialSummary.innerHTML = trialMode === 'propose'
      ? `Bạn đề xuất học thử vào <strong class="text-[#0F2545]">${when}</strong> <span class="text-amber-700 font-semibold">(ngoài lịch rảnh)</span>. Gia sư sẽ đồng ý hoặc từ chối.`
      : `Bạn muốn học thử vào <strong class="text-[#0F2545]">${when}</strong>. Gia sư sẽ xác nhận lại với bạn.`;
  }

  $('trial-propose-toggle').addEventListener('click', () => setTrialMode('propose'));
  $('trial-back-to-calendar').addEventListener('click', () => setTrialMode('calendar'));
  [['trial-week-prev', -1], ['trial-week-next', 1]].forEach(([id, delta]) => $(id).addEventListener('click', () => shiftWeek(delta)));
  trialDateInput.min = todayIsoVN();
  trialDateInput.addEventListener('change', () => { clearError(); updateTrialSummary(); });
  trialTimeInput.addEventListener('change', () => { clearError(); updateTrialSummary(); });

  /** preselect: { date: "YYYY-MM-DD", start: phút, propose?: true } */
  function openTrialModal(preselect = null) {
    // Mở đúng tuần chứa giờ đã chọn (server tự lùi ngày bất kỳ về thứ hai của tuần đó)
    const wantWeek = preselect && !preselect.propose && !preselect.loaded && !(weekData && (weekData.days || []).some(d => d.date === preselect.date))
      ? preselect.date : null;
    if (hasAvailability() && (!weekData || wantWeek)) {
      showWeek(wantWeek).then(() => { if (weekData) openTrialModal(wantWeek ? { ...preselect, loaded: true } : preselect); });
      return;
    }
    clearError();
    // Gia sư chưa khai báo lịch rảnh thì chỉ còn cách đề xuất giờ
    trialMode = (preselect && preselect.propose) || !hasAvailability() ? 'propose' : 'calendar';
    if (preselect && preselect.propose) {
      trialDateInput.value = preselect.date;
      trialTimeInput.value = toHHMM(preselect.start);
    } else if (preselect) {
      trialDay = preselect.date;
      trialPick = { date: preselect.date, start: preselect.start };
    }
    renderTrialPicker();
    openModal('connect-modal');
  }

  continueBtn.addEventListener('click', () => {
    clearError();
    const start = getTrialStart();
    if (!start) {
      showError('Vui lòng chọn ngày và giờ bạn muốn học thử.');
      return;
    }
    if (new Date(`${start}:00+07:00`) <= new Date()) {
      showError('Thời gian học thử phải ở tương lai. Vui lòng chọn thời gian khác.');
      return;
    }
    onContinue && onContinue({ type: 'trial', tutorId: tutor.tutorId, start, offSchedule: trialMode === 'propose' });
  });
  updateTrialSummary();

  return {
    openTrialModal,
    openDirectModal,
    showWeek,
    shiftWeek,
    reloadWeek,
    renderWeekHeader,
    get weekData() { return weekData; },
    dayOf,
    /** Giờ trống của đúng tuần đang xem đủ cho một buổi dài `duration` phút. */
    gridStartTimesOf: (dow, duration) => startTimesFrom(freeRangesOf(dow), duration)
  };
}
