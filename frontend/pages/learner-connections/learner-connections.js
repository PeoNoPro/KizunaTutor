/**
 * KizunaTutor - Lịch học của tôi (learner-connections.js)
 * Thời khóa biểu tuần của học viên: buổi học thử đã được gia sư xác nhận + lớp học chính thức hằng tuần.
 * Các yêu cầu đã gửi (chờ phản hồi, bị từ chối, hết hạn...) nằm ở trang Yêu cầu của tôi (learner-requests).
 */

import { initLearnerShell } from '/components/learner-layout/learner-layout.js';
import { API, ApiError } from '/js/api.js';
import { renderWeekTimeGrid, trialEvent, officialEvent } from '/js/week-time-grid.js';

// 1. Khởi tạo khung giao diện học viên
initLearnerShell({
  activeMenu: 'requests',
  pageTitle: 'Lịch học của tôi',
  badge: 'Gia sư & lớp học'
});

const $ = (id) => document.getElementById(id);

const WEEKDAYS_DEF = [
  { dayIndex: 0, dow: 2, name: 'Thứ hai', short: 'Thứ 2', colName: 'MON' },
  { dayIndex: 1, dow: 3, name: 'Thứ ba', short: 'Thứ 3', colName: 'TUE' },
  { dayIndex: 2, dow: 4, name: 'Thứ tư', short: 'Thứ 4', colName: 'WED' },
  { dayIndex: 3, dow: 5, name: 'Thứ năm', short: 'Thứ 5', colName: 'THU' },
  { dayIndex: 4, dow: 6, name: 'Thứ sáu', short: 'Thứ 6', colName: 'FRI' },
  { dayIndex: 5, dow: 7, name: 'Thứ bảy', short: 'Thứ 7', colName: 'SAT' },
  { dayIndex: 6, dow: 1, name: 'Chủ nhật', short: 'CN', colName: 'SUN' }
];

// Chỉ buổi học thử đã được gia sư xác nhận mới xuất hiện trên lịch học
const SCHEDULED_TRIAL_STATUSES = ['CONFIRMED', 'RESCHEDULED', 'COMPLETED'];


let bookingsList = [];
let classTimetable = []; // Lịch học chính thức hằng tuần của các lớp (FE-06: GET /api/classrooms/schedules/me)
let pendingDirect = 0; // số yêu cầu kết nối trực tiếp đang chờ (chỉ để hiện trên thẻ thống kê)
let learnerCurrentWeekMonday = getMondayOf(new Date());

function getMondayOf(date) {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  const day = d.getDay();
  const diff = (day === 0 ? -6 : 1) - day;
  d.setDate(d.getDate() + diff);
  return d;
}

function pad(num) {
  return String(num).padStart(2, '0');
}

function formatDateYMD(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function formatDateDMY(d) {
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}

function formatHour(h) {
  return `${pad(h)}:00`;
}

function setVisible(el, visible) {
  if (!el) return;
  el.classList.toggle('hidden', !visible);
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text == null ? '' : String(text);
  return div.innerHTML;
}

function formatBookingTime(startTimeStr, durationMinutes = 50) {
  if (!startTimeStr) return 'Chưa định dạng';
  const start = new Date(startTimeStr);
  if (Number.isNaN(start.getTime())) return 'Chưa định dạng';

  const end = new Date(start.getTime() + durationMinutes * 60 * 1000);
  const pad = (n) => String(n).padStart(2, '0');
  const DAYS = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'];

  const startTime = `${pad(start.getHours())}:${pad(start.getMinutes())}`;
  const endTime = `${pad(end.getHours())}:${pad(end.getMinutes())}`;
  const dayName = DAYS[start.getDay()];
  const dateStr = `${pad(start.getDate())}/${pad(start.getMonth() + 1)}/${start.getFullYear()}`;

  return `${startTime} - ${endTime} • ${dayName}, ${dateStr}`;
}

function showToast(message, type = 'success') {
  const toast = $('toastNotification');
  const msgEl = $('toastMsg');
  const iconEl = $('toastIcon');
  if (!toast || !msgEl || !iconEl) return;

  msgEl.textContent = message;
  if (type === 'error') {
    toast.className = 'fixed bottom-6 right-6 z-50 max-w-sm px-4 py-3 rounded-xl shadow-lg text-xs font-semibold flex items-center gap-2 transition-all bg-rose-600 text-white';
    iconEl.textContent = 'error';
  } else {
    toast.className = 'fixed bottom-6 right-6 z-50 max-w-sm px-4 py-3 rounded-xl shadow-lg text-xs font-semibold flex items-center gap-2 transition-all bg-[#0F2545] text-white';
    iconEl.textContent = 'check_circle';
  }

  toast.classList.remove('hidden');
  clearTimeout(toast.__timer);
  toast.__timer = setTimeout(() => {
    toast.classList.add('hidden');
  }, 3500);
}

/**
 * Hiển thị thời khóa biểu tuần của Học viên (Weekly Time Grid)
 */
function renderLearnerWeeklyCalendar() {
  const headerRow = $('learnerGridHeaderRow');
  const bodyRows = $('learnerGridBodyRows');
  if (!headerRow || !bodyRows) return;

  const sunday = new Date(learnerCurrentWeekMonday.getTime() + 6 * 86400000);
  if ($('learnerCurrentWeekText')) {
    $('learnerCurrentWeekText').textContent = `Tuần: ${formatDateDMY(learnerCurrentWeekMonday)} - ${formatDateDMY(sunday)}`;
  }

  const todayStr = formatDateYMD(new Date());

  // 1. Header Row
  headerRow.innerHTML = `
    <div class="p-3 text-[11px] font-bold text-[#64748B] text-center border-r border-[#E2E8F0] flex items-center justify-center sticky left-0 z-10 bg-white">
      Giờ
    </div>
    ${WEEKDAYS_DEF.map((day, idx) => {
      const dayDate = new Date(learnerCurrentWeekMonday.getTime() + idx * 86400000);
      const isToday = formatDateYMD(dayDate) === todayStr;
      return `
        <div class="p-2.5 text-center border-r last:border-r-0 border-[#E2E8F0] ${isToday ? 'bg-[#006194]/10' : ''}">
          <p class="text-[11px] font-bold ${isToday ? 'text-[#006194]' : 'text-[#64748B]'} uppercase">${day.colName}</p>
          <div class="mt-0.5 inline-flex items-center justify-center ${isToday ? 'w-7 h-7 rounded-full bg-[#006194] text-white font-bold' : 'text-sm font-bold text-[#0F172A]'}">
            ${dayDate.getDate()}
          </div>
          <p class="text-[10px] text-[#64748B] mt-0.5">${day.name}</p>
        </div>
      `;
    }).join('')}
  `;

  // Lọc các buổi học trong tuần này của học viên
  const weekStartTs = learnerCurrentWeekMonday.getTime();
  const weekEndTs = learnerCurrentWeekMonday.getTime() + 7 * 86400000;

  const weekLessons = bookingsList.filter(b => {
    if (!b.scheduledStartTime) return false;
    const start = new Date(b.scheduledStartTime);
    const ts = start.getTime();
    return ts >= weekStartTs && ts < weekEndTs && SCHEDULED_TRIAL_STATUSES.includes(b.bookingStatus);
  });

  // Buổi học chính thức: lặp hằng tuần theo lịch lớp (FE-06), chỉ vẽ từ tuần mở lớp trở đi
  const weekOfficial = classTimetable.map(entry => {
    const dayIdx = Number(entry.dayOfWeek) - 1; // 1 = Thứ Hai
    const date = new Date(learnerCurrentWeekMonday.getTime() + dayIdx * 86400000);
    const [sh, sm] = String(entry.startTime || '').split(':').map(Number);
    const [eh, em] = String(entry.endTime || '').split(':').map(Number);
    const started = entry.classStartedAt ? new Date(entry.classStartedAt) : null;
    const endOfDay = new Date(date.getTime() + 86400000);
    if (dayIdx < 0 || dayIdx > 6 || Number.isNaN(sh) || (started && started >= endOfDay)) return null;
    return { ...entry, dateYmd: formatDateYMD(date), startHour: sh, timeSpan: `${pad(sh)}:${pad(sm || 0)} - ${pad(eh)}:${pad(em || 0)}` };
  }).filter(Boolean);

  // 2. Thân lưới 24 giờ
  // Thân lưới: lịch theo phút (thẻ đặt đúng giờ bắt đầu, cao theo thời lượng) — js/week-time-grid.js
  renderWeekTimeGrid({
    bodyEl: bodyRows,
    scrollEl: $('learnerGridScroll'),
    monday: learnerCurrentWeekMonday,
    events: [
      ...weekOfficial.map(o => officialEvent(o, `/my-courses?space=${encodeURIComponent(o.spaceId)}`)),
      ...weekLessons.map(b => trialEvent(b, null))
    ]
  });
}

/**
 * Thẻ thống kê: học thử sắp diễn ra, buổi học chính thức mỗi tuần, số yêu cầu đang chờ (dẫn sang trang Yêu cầu của tôi)
 */
function updateStats() {
  const upcoming = upcomingTrials().length;
  const pendingTrials = bookingsList.filter(b => b.bookingStatus === 'PENDING').length;
  if ($('statUpcoming')) $('statUpcoming').textContent = upcoming;
  if ($('statOfficial')) $('statOfficial').textContent = classTimetable.length;
  if ($('statPendingRequests')) $('statPendingRequests').textContent = pendingTrials + pendingDirect;
}

/** Buổi học thử đã được gia sư xác nhận và chưa kết thúc, sắp xếp theo giờ bắt đầu. */
function upcomingTrials() {
  const now = Date.now();
  return bookingsList
    .filter(b => SCHEDULED_TRIAL_STATUSES.includes(b.bookingStatus) && b.bookingStatus !== 'COMPLETED')
    .filter(b => {
      const end = b.scheduledEndTime ? new Date(b.scheduledEndTime).getTime()
        : new Date(b.scheduledStartTime).getTime() + (b.durationMinutes || 30) * 60000;
      return end > now;
    })
    .sort((a, b) => new Date(a.scheduledStartTime) - new Date(b.scheduledStartTime));
}

/** Danh sách gọn các buổi học thử sắp diễn ra: thời gian, gia sư, link vào lớp. */
function renderUpcoming() {
  const listEl = $('upcomingList');
  if (!listEl) return;
  const items = upcomingTrials();
  if (!items.length) {
    listEl.innerHTML = `
      <div class="bg-white border border-[#E2E8F0] rounded-2xl p-6 text-center text-xs text-[#64748B] shadow-xs">
        Chưa có buổi học thử nào sắp diễn ra. Yêu cầu đang chờ gia sư xác nhận nằm ở
        <a href="/pages/learner-requests/" class="text-[#006194] font-semibold underline">Yêu cầu của tôi</a>.
      </div>`;
    return;
  }
  listEl.innerHTML = items.map(item => `
    <div class="bg-white rounded-2xl p-4 shadow-xs border border-[#E2E8F0] flex flex-col sm:flex-row sm:items-center justify-between gap-3">
      <div class="flex items-center gap-3 min-w-0">
        <div class="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-700 flex items-center justify-center shrink-0">
          <span class="material-symbols-outlined text-[22px]">event_available</span>
        </div>
        <div class="min-w-0">
          <p class="text-sm font-bold text-[#0F172A]">${escapeHtml(formatBookingTime(item.scheduledStartTime, item.durationMinutes))}</p>
          <p class="text-xs text-[#64748B] truncate">Học thử với ${escapeHtml(item.partnerName || 'gia sư')} • Trực tuyến</p>
        </div>
      </div>
      <div class="flex items-center gap-2 shrink-0">
        ${item.meetingLink ? `
          <a href="${escapeHtml(item.meetingLink)}" target="_blank" rel="noopener noreferrer" class="px-4 py-1.5 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-semibold inline-flex items-center gap-1 shadow-xs no-underline">
            <span class="material-symbols-outlined text-[15px]">open_in_new</span><span>Vào lớp học</span>
          </a>` : ''}
        <a href="/pages/learner-requests/?tab=trial" class="px-3.5 py-1.5 rounded-lg bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 text-xs font-semibold inline-flex items-center gap-1 no-underline">
          <span class="material-symbols-outlined text-[15px]">tune</span><span>Đổi lịch / hủy</span>
        </a>
      </div>
    </div>`).join('');
}

/**
 * Tải buổi học thử, lịch lớp chính thức và số yêu cầu kết nối đang chờ
 */
async function loadSchedule() {
  // Vẽ khung lịch ngay, rồi điền dữ liệu khi từng API trả về (không chờ API chậm nhất)
  setVisible($('errorState'), false);
  renderLearnerWeeklyCalendar();

  const timetableTask = API.classrooms.timetable()
    .then(timetable => {
      classTimetable = Array.isArray(timetable) ? timetable : [];
      updateStats();
      renderLearnerWeeklyCalendar();
    })
    .catch(() => {});

  const directTask = API.connectionRequests.getMyRequests('PENDING', 0, 1)
    .then(direct => {
      pendingDirect = Number(direct?.totalItems ?? (direct?.items || []).length) || 0;
      updateStats();
    })
    .catch(() => {});

  setVisible($('loadingState'), true);
  try {
    const res = await API.trialBookings.getMyRequests('ALL', 0, 50);
    bookingsList = res?.items || res?.content || res?.data?.items || res?.data?.content || (Array.isArray(res) ? res : []);
    updateStats();
    renderLearnerWeeklyCalendar();
    renderUpcoming();
  } catch (err) {
    console.error('Lỗi tải lịch học của học viên:', err);
    $('errorMessage').textContent = err instanceof ApiError ? err.message : 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.';
    setVisible($('errorState'), true);
  } finally {
    setVisible($('loadingState'), false);
  }
  await Promise.all([timetableTask, directTask]);
}

function initEvents() {
  $('btnLearnerPrevWeek')?.addEventListener('click', () => {
    learnerCurrentWeekMonday = new Date(learnerCurrentWeekMonday.getTime() - 7 * 86400000);
    renderLearnerWeeklyCalendar();
  });
  $('btnLearnerNextWeek')?.addEventListener('click', () => {
    learnerCurrentWeekMonday = new Date(learnerCurrentWeekMonday.getTime() + 7 * 86400000);
    renderLearnerWeeklyCalendar();
  });
  $('btnLearnerToday')?.addEventListener('click', () => {
    learnerCurrentWeekMonday = getMondayOf(new Date());
    renderLearnerWeeklyCalendar();
  });
  $('btnRefresh')?.addEventListener('click', loadSchedule);
  $('btnRetry')?.addEventListener('click', loadSchedule);
}

document.addEventListener('DOMContentLoaded', () => {
  initEvents();
  loadSchedule();
});
