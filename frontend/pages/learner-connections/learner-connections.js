/**
 * KizunaTutor - Quản lý Yêu cầu kết nối dành cho Học viên (learner-connections.js)
 * Phân hệ Học viên - Sử dụng layout Learner Shared Layout
 */

import { initLearnerShell } from '/components/learner-layout/learner-layout.js';
import { API, ApiError } from '/js/api.js';

// 1. Khởi tạo khung giao diện học viên
initLearnerShell({
  activeMenu: 'requests',
  pageTitle: 'Thời khóa biểu & Lịch học',
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

const HOURS_GRID = [7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21];

let bookingsList = [];
let currentFilter = 'ALL';
let activeBooking = null;
let activeCompleteBooking = null;
let currentViewMode = 'CALENDAR'; // 'CALENDAR' hoặc 'LIST'
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

function switchViewMode(mode) {
  currentViewMode = mode;
  const isCal = mode === 'CALENDAR';

  if ($('btnViewCalendar')) {
    $('btnViewCalendar').className = isCal
      ? 'px-4 py-2 rounded-xl bg-[#006194] text-white text-xs font-bold flex items-center gap-1.5 shadow-xs transition-all cursor-pointer'
      : 'px-4 py-2 rounded-xl text-[#64748B] hover:bg-[#F1F5F9] hover:text-[#0F172A] text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer';
  }
  if ($('btnViewList')) {
    $('btnViewList').className = !isCal
      ? 'px-4 py-2 rounded-xl bg-[#006194] text-white text-xs font-bold flex items-center gap-1.5 shadow-xs transition-all cursor-pointer'
      : 'px-4 py-2 rounded-xl text-[#64748B] hover:bg-[#F1F5F9] hover:text-[#0F172A] text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer';
  }

  setVisible($('viewCalendarContainer'), isCal);
  setVisible($('viewListContainer'), !isCal);

  if (isCal) {
    renderLearnerWeeklyCalendar();
  } else {
    renderBookings();
  }
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
 * Cập nhật số liệu thống kê trên các thẻ KPI và nhãn của từng Tab
 */
function updateCounts() {
  const total = bookingsList.length;
  const pending = bookingsList.filter(b => b.bookingStatus === 'PENDING').length;
  const confirmed = bookingsList.filter(b => b.bookingStatus === 'CONFIRMED' || b.bookingStatus === 'RESCHEDULED').length;
  const history = bookingsList.filter(b => ['COMPLETED', 'REJECTED', 'CANCELLED', 'DECLINED'].includes(b.bookingStatus)).length;

  if ($('statPending')) $('statPending').textContent = pending;
  if ($('statConfirmed')) $('statConfirmed').textContent = confirmed;
  if ($('statTotal')) $('statTotal').textContent = total;

  if ($('tabCountAll')) $('tabCountAll').textContent = total;
  if ($('tabCountPending')) $('tabCountPending').textContent = pending;
  if ($('tabCountConfirmed')) $('tabCountConfirmed').textContent = confirmed;
  if ($('tabCountHistory')) $('tabCountHistory').textContent = history;
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
    <div class="p-3 text-[11px] font-bold text-[#64748B] text-center border-r border-[#E2E8F0] flex items-center justify-center">
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
    return ts >= weekStartTs && ts < weekEndTs && !['CANCELLED', 'DECLINED'].includes(b.bookingStatus);
  });

  // 2. Body Rows (07:00 đến 21:00)
  bodyRows.innerHTML = HOURS_GRID.map(hour => {
    const timeLabel = formatHour(hour);

    const cellsHtml = WEEKDAYS_DEF.map((day, idx) => {
      const cellDate = new Date(learnerCurrentWeekMonday.getTime() + idx * 86400000);
      const dateYmd = formatDateYMD(cellDate);

      // Tìm buổi học rơi vào ô ngày & giờ này
      const matchedLessons = weekLessons.filter(b => {
        const d = new Date(b.scheduledStartTime);
        return formatDateYMD(d) === dateYmd && d.getHours() === hour;
      });

      let contentHtml = '';
      if (matchedLessons.length > 0) {
        contentHtml = matchedLessons.map(lesson => {
          const isCompleted = lesson.bookingStatus === 'COMPLETED';
          const themeClass = isCompleted
            ? 'bg-blue-50 border-blue-200 text-blue-950'
            : 'bg-emerald-50 border-emerald-200 text-emerald-950';
          const barClass = isCompleted ? 'bg-[#006194]' : 'bg-emerald-600';
          const badgeClass = isCompleted ? 'bg-blue-100 text-[#006194]' : 'bg-emerald-100 text-emerald-800';
          const badgeText = isCompleted ? 'Đã hoàn thành' : 'Đã xếp lịch';

          const sTime = new Date(lesson.scheduledStartTime);
          const eTime = lesson.scheduledEndTime ? new Date(lesson.scheduledEndTime) : new Date(sTime.getTime() + (lesson.durationMinutes || 50) * 60000);
          const timeSpan = `${pad(sTime.getHours())}:${pad(sTime.getMinutes())} - ${pad(eTime.getHours())}:${pad(eTime.getMinutes())}`;
          const tutorName = escapeHtml(lesson.partnerName || 'Gia sư Kizuna');
          const title = escapeHtml(lesson.locationDetails || 'Tiếng Nhật 1-1');

          return `
            <div class="relative p-2 rounded-xl border ${themeClass} shadow-2xs space-y-1 hover:shadow-md transition-all text-left">
              <div class="absolute left-0 top-0 bottom-0 w-1 ${barClass} rounded-l-xl"></div>
              <div class="flex items-center justify-between pl-1 gap-1">
                <span class="font-bold text-[12px] truncate flex items-center gap-1">
                  ${tutorName}
                  <span class="material-symbols-outlined text-[13px] ${isCompleted ? 'text-[#006194]' : 'text-emerald-600'}">check_circle</span>
                </span>
                <span class="text-[9px] font-bold px-1.5 py-0.5 rounded ${badgeClass} shrink-0">${badgeText}</span>
              </div>
              <div class="text-[10px] font-semibold pl-1 opacity-80">
                ${timeSpan}
              </div>
              <div class="pl-1 text-[11px] font-medium truncate text-[#0F172A]">
                ${title}
              </div>
            </div>
          `;
        }).join('');
      } else {
        contentHtml = '';
      }

      return `
        <div class="p-1.5 min-h-[64px] border-r last:border-r-0 border-[#E2E8F0]/60 overflow-hidden relative" data-date="${dateYmd}" data-hour="${hour}">
          ${contentHtml}
        </div>
      `;
    }).join('');

    return `
      <div class="grid grid-cols-8 items-stretch border-b border-[#E2E8F0]/40 hover:bg-[#F8FAFC]/60 transition-colors">
        <div class="p-2 border-r border-[#E2E8F0] text-[11px] font-bold text-[#64748B] flex items-start justify-center pt-1.5 bg-[#F8FAFC]/40 min-h-[64px]">
          ${timeLabel}
        </div>
        ${cellsHtml}
      </div>
    `;
  }).join('');
}

/**
 * Hiển thị danh sách thẻ yêu cầu kết nối
 */
function renderBookings() {
  const listEl = $('bookingsList');
  const emptyEl = $('emptyState');
  if (!listEl || !emptyEl) return;

  let filtered = bookingsList;
  if (currentFilter === 'PENDING') {
    filtered = bookingsList.filter(b => b.bookingStatus === 'PENDING');
  } else if (currentFilter === 'CONFIRMED') {
    filtered = bookingsList.filter(b => b.bookingStatus === 'CONFIRMED' || b.bookingStatus === 'RESCHEDULED');
  } else if (currentFilter === 'HISTORY') {
    filtered = bookingsList.filter(b => ['COMPLETED', 'REJECTED', 'CANCELLED', 'DECLINED'].includes(b.bookingStatus));
  }

  if (filtered.length === 0) {
    setVisible(listEl, false);
    setVisible(emptyEl, true);
    const msgEl = $('emptyMessage');
    if (msgEl) {
      if (currentFilter === 'PENDING') {
        msgEl.textContent = 'Bạn không có yêu cầu học thử nào đang chờ gia sư phản hồi.';
      } else if (currentFilter === 'CONFIRMED') {
        msgEl.textContent = 'Chưa có buổi học thử nào được gia sư xác nhận.';
      } else {
        msgEl.textContent = 'Chưa có yêu cầu nào trong danh mục này.';
      }
    }
    return;
  }

  setVisible(emptyEl, false);
  setVisible(listEl, true);

  listEl.innerHTML = filtered.map(item => {
    const isPending = item.bookingStatus === 'PENDING';
    const isConfirmed = item.bookingStatus === 'CONFIRMED';
    const isRescheduled = item.bookingStatus === 'RESCHEDULED';
    const isRejected = item.bookingStatus === 'REJECTED' || item.bookingStatus === 'DECLINED';
    const isCancelled = item.bookingStatus === 'CANCELLED';
    const isCompleted = item.bookingStatus === 'COMPLETED';

    const avatarSrc = item.partnerAvatarUrl || 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=120&q=80';
    const partnerName = escapeHtml(item.partnerName || 'Gia sư Kizuna');
    const timeFormatted = formatBookingTime(item.scheduledStartTime, item.durationMinutes);
    const isOnline = item.learningMode === 'ONLINE';

    // Badge trạng thái
    let badgeHtml = '';
    if (isPending) {
      badgeHtml = `<span class="px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-100 text-amber-800 flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">hourglass_top</span>Chờ gia sư xác nhận</span>`;
    } else if (isRescheduled) {
      badgeHtml = `<span class="px-2.5 py-1 rounded-full text-[11px] font-bold bg-indigo-100 text-indigo-800 flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">update</span>Đã dời lịch</span>`;
    } else if (isConfirmed) {
      badgeHtml = `<span class="px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-100 text-emerald-800 flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">check_circle</span>Đã xác nhận</span>`;
    } else if (isRejected) {
      badgeHtml = `<span class="px-2.5 py-1 rounded-full text-[11px] font-bold bg-rose-100 text-rose-800 flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">cancel</span>Gia sư từ chối</span>`;
    } else if (isCancelled) {
      badgeHtml = `<span class="px-2.5 py-1 rounded-full text-[11px] font-bold bg-slate-100 text-slate-600 flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">close</span>Đã hủy</span>`;
    } else if (isCompleted) {
      badgeHtml = `<span class="px-2.5 py-1 rounded-full text-[11px] font-bold bg-blue-100 text-blue-800 flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">task_alt</span>Đã hoàn thành</span>`;
    }

    // Các nút hành động
    let actionsHtml = '';
    if (isPending) {
      actionsHtml = `
        <div class="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-slate-100">
          <div class="text-xs text-slate-500 italic flex items-center gap-1">
            <span class="material-symbols-outlined text-[16px] text-amber-600">info</span>
            <span>Yêu cầu đang chờ gia sư phản hồi. Bạn có thể đổi lịch hoặc hủy nếu đổi ý.</span>
          </div>
          <div class="flex items-center gap-2">
            <button type="button" class="btn-open-reschedule px-3.5 py-1.5 rounded-xl bg-white border border-[#006194] text-[#006194] hover:bg-[#006194]/5 text-xs font-bold inline-flex items-center gap-1.5 transition-all cursor-pointer" data-id="${item.bookingId}">
              <span class="material-symbols-outlined text-[16px]">edit_calendar</span>
              <span>Đổi lịch</span>
            </button>
            <button type="button" class="btn-open-cancel px-3.5 py-1.5 rounded-xl bg-slate-100 hover:bg-rose-50 hover:text-rose-700 text-slate-700 text-xs font-semibold inline-flex items-center gap-1.5 transition-all cursor-pointer" data-id="${item.bookingId}">
              <span class="material-symbols-outlined text-[16px]">close</span>
              <span>Hủy yêu cầu</span>
            </button>
          </div>
        </div>
      `;
    } else if (isConfirmed || isRescheduled) {
      const sTime = item.scheduledStartTime ? new Date(item.scheduledStartTime) : null;
      const eTime = item.scheduledEndTime
        ? new Date(item.scheduledEndTime)
        : (sTime ? new Date(sTime.getTime() + (item.durationMinutes || 50) * 60000) : null);
      const isEnded = eTime ? (Date.now() >= eTime.getTime()) : true;
      const padNum = (n) => String(n).padStart(2, '0');
      const endTimeStr = eTime ? `${padNum(eTime.getHours())}:${padNum(eTime.getMinutes())}` : '';

      actionsHtml = `
        <div class="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-slate-100 bg-emerald-50/60 p-3 rounded-xl border border-emerald-200">
          ${item.meetingLink ? `
            <div class="flex items-center gap-2 min-w-0">
              <span class="material-symbols-outlined text-emerald-700 text-[20px] shrink-0">video_camera_front</span>
              <div class="min-w-0">
                <p class="text-[11px] text-emerald-800 font-bold">Phòng học Google Meet đã sẵn sàng:</p>
                <a href="${item.meetingLink}" target="_blank" rel="noopener noreferrer" class="text-xs text-emerald-700 hover:text-emerald-900 font-medium underline truncate block">
                  ${escapeHtml(item.meetingLink)}
                </a>
              </div>
            </div>
          ` : `
            <div class="flex items-center gap-2 text-xs text-emerald-800 font-medium">
              <span class="material-symbols-outlined text-[18px] text-emerald-600">verified</span>
              <span>Buổi học đã được gia sư xác nhận. Vui lòng tham gia đúng giờ hẹn.</span>
            </div>
          `}
          <div class="flex items-center gap-2 shrink-0">
            ${item.meetingLink ? `
              <button type="button" class="btn-copy-link px-3 py-1.5 rounded-lg bg-white border border-emerald-300 text-emerald-800 hover:bg-emerald-100 text-xs font-semibold inline-flex items-center gap-1 transition-all cursor-pointer" data-link="${escapeHtml(item.meetingLink)}">
                <span class="material-symbols-outlined text-[15px]">content_copy</span>
                <span>Sao chép</span>
              </button>
              <a href="${item.meetingLink}" target="_blank" rel="noopener noreferrer" class="px-4 py-1.5 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-semibold inline-flex items-center gap-1 shadow-xs transition-all no-underline">
                <span class="material-symbols-outlined text-[15px]">open_in_new</span>
                <span>Vào lớp học ngay</span>
              </a>
            ` : ''}
            ${isEnded ? `
              <button type="button" class="btn-complete-booking px-3 py-1.5 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-semibold inline-flex items-center gap-1 shadow-xs transition-all cursor-pointer" data-id="${item.bookingId}" data-ended="true" title="Xác nhận buổi học đã hoàn thành">
                <span class="material-symbols-outlined text-[15px]">task_alt</span>
                <span>Hoàn thành</span>
              </button>
            ` : `
              <button type="button" class="btn-complete-booking px-3 py-1.5 rounded-lg bg-slate-100 border border-slate-300 text-slate-400 text-xs font-semibold inline-flex items-center gap-1 transition-all cursor-not-allowed opacity-80" data-id="${item.bookingId}" data-ended="false" data-endtime="${endTimeStr}" title="Chỉ có thể xác nhận sau khi kết thúc buổi học lúc ${endTimeStr}">
                <span class="material-symbols-outlined text-[15px]">schedule</span>
                <span>Chưa kết thúc (${endTimeStr})</span>
              </button>
            `}
            <button type="button" class="btn-open-reschedule px-3 py-1.5 rounded-lg bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 text-xs font-semibold inline-flex items-center gap-1 transition-all cursor-pointer" data-id="${item.bookingId}" title="Đổi sang lịch khác trước ít nhất 24 giờ">
              <span class="material-symbols-outlined text-[15px]">edit_calendar</span>
              <span>Đổi lịch</span>
            </button>
            <button type="button" class="btn-open-cancel px-3 py-1.5 rounded-lg bg-white border border-slate-300 hover:bg-rose-50 hover:text-rose-700 text-slate-700 text-xs font-semibold inline-flex items-center gap-1 transition-all cursor-pointer" data-id="${item.bookingId}">
              <span class="material-symbols-outlined text-[15px]">close</span>
              <span>Hủy</span>
            </button>
          </div>
        </div>
      `;
    } else if (isCompleted) {
      actionsHtml = `
        <div class="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-slate-100 bg-sky-50/60 p-3.5 rounded-xl border border-sky-200">
          <div class="flex items-center gap-2 text-xs text-sky-800 font-medium">
            <span class="material-symbols-outlined text-[18px] text-sky-600">verified</span>
            <span>Buổi học thử 1-1 đã hoàn tất! Hai bên có thể tiếp tục trao đổi để sắp xếp lịch học chính thức.</span>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <a href="/pages/my-courses/" class="px-4 py-1.5 rounded-lg bg-[#006194] hover:bg-[#004b73] text-white text-xs font-semibold inline-flex items-center gap-1.5 shadow-xs transition-all no-underline">
              <span class="material-symbols-outlined text-[15px]">school</span>
              <span>Vào lớp học của tôi</span>
            </a>
            <a href="/pages/tutor-discovery/" class="px-3.5 py-1.5 rounded-lg bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 text-xs font-semibold inline-flex items-center gap-1.5 transition-all no-underline">
              <span class="material-symbols-outlined text-[15px]">person_search</span>
              <span>Tìm gia sư khác</span>
            </a>
          </div>
        </div>
      `;
    } else if (isRejected) {
      actionsHtml = `
        <div class="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-slate-100 bg-rose-50/60 p-3 rounded-xl border border-rose-200">
          <div class="flex items-center gap-2 text-xs text-rose-700 font-medium">
            <span class="material-symbols-outlined text-[18px]">info</span>
            <span>Gia sư không thể nhận lịch này. Bạn có thể tìm kiếm gia sư khác.</span>
          </div>
          <a href="/pages/tutor-discovery/" class="px-3.5 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold inline-flex items-center gap-1 shadow-xs transition-all no-underline">
            <span class="material-symbols-outlined text-[15px]">search</span>
            <span>Tìm gia sư khác</span>
          </a>
        </div>
      `;
    }

    return `
      <div class="bg-white rounded-2xl p-5 sm:p-6 shadow-xs border border-[#E2E8F0] hover:border-[#006194]/40 transition-all space-y-4">
        <!-- Hàng đầu: Thông tin gia sư + Trạng thái -->
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div class="flex items-center gap-3">
            <img src="${avatarSrc}" alt="${partnerName}" class="w-12 h-12 rounded-2xl object-cover ring-2 ring-slate-100 shrink-0">
            <div>
              <div class="flex items-center gap-2">
                <h3 class="text-base font-bold text-[#0F172A]">${partnerName}</h3>
                <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-[#FFE5EC] text-[#831843]">Gia sư Kizuna</span>
              </div>
              <p class="text-xs text-[#64748B] mt-0.5">${item.durationMinutes || 50} phút • Học thử miễn phí 0đ</p>
            </div>
          </div>
          <div class="self-start sm:self-center">
            ${badgeHtml}
          </div>
        </div>

        <!-- Khối chi tiết lịch hẹn -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-3 p-3.5 rounded-xl bg-[#F8FAFC] border border-[#E2E8F0] text-xs">
          <div class="flex items-center gap-2 text-[#0F172A] font-medium">
            <span class="material-symbols-outlined text-[18px] text-[#006194] shrink-0">calendar_today</span>
            <span>${timeFormatted}</span>
          </div>
          <div class="flex items-center gap-2 text-[#64748B]">
            <span class="material-symbols-outlined text-[18px] text-[#006194] shrink-0">${isOnline ? 'laptop_chromebook' : 'pin_drop'}</span>
            <span>${isOnline ? 'Trực tuyến (Google Meet)' : `Trực tiếp tại ${escapeHtml(item.locationDetails || 'địa chỉ hẹn')}`}</span>
          </div>

          ${item.currentLevel ? `
            <div class="flex items-center gap-2 text-[#64748B]">
              <span class="material-symbols-outlined text-[18px] text-amber-600 shrink-0">school</span>
              <span>Trình độ đã chọn: <strong class="text-[#0F172A] font-semibold">${escapeHtml(item.currentLevel)}</strong></span>
            </div>
          ` : ''}

          ${item.learningGoals ? `
            <div class="flex items-center gap-2 text-[#64748B]">
              <span class="material-symbols-outlined text-[18px] text-[#006194] shrink-0">flag</span>
              <span>Mục tiêu: <span class="text-[#0F172A] font-medium">${escapeHtml(item.learningGoals)}</span></span>
            </div>
          ` : ''}
        </div>

        <!-- Lời nhắn ban đầu gửi cho gia sư -->
        ${(item.initialMessage && item.initialMessage.trim()) ? `
          <div class="text-xs text-[#64748B] bg-[#F8FAFC] p-3 rounded-xl border border-[#E2E8F0] italic">
            "Lời nhắn: ${escapeHtml(item.initialMessage)}"
          </div>
        ` : ''}

        <!-- Hàng nút hành động -->
        ${actionsHtml}
      </div>
    `;
  }).join('');

  bindCardActionEvents();
}

/**
 * Gắn sự kiện cho các nút trên từng thẻ yêu cầu
 */
function bindCardActionEvents() {
  // Nút mở modal Đổi lịch học thử (SRS 5.3)
  document.querySelectorAll('.btn-open-reschedule').forEach(btn => {
    btn.addEventListener('click', () => {
      const id = btn.dataset.id;
      const booking = bookingsList.find(b => String(b.bookingId) === String(id));
      if (!booking) return;
      openRescheduleModal(booking);
    });
  });

  // Nút mở modal Hủy yêu cầu
  document.querySelectorAll('.btn-open-cancel').forEach(btn => {
    btn.addEventListener('click', () => {
      const id = btn.dataset.id;
      const booking = bookingsList.find(b => String(b.bookingId) === String(id));
      if (!booking) return;
      openCancelModal(booking);
    });
  });

  // Nút sao chép link Google Meet
  document.querySelectorAll('.btn-copy-link').forEach(btn => {
    btn.addEventListener('click', async () => {
      const link = btn.dataset.link;
      if (!link) return;
      try {
        await navigator.clipboard.writeText(link);
        showToast('Đã sao chép link Google Meet vào bộ nhớ tạm!');
      } catch (e) {
        showToast('Không thể sao chép link', 'error');
      }
    });
  });

  // Nút xác nhận hoàn thành buổi học thử
  document.querySelectorAll('.btn-complete-booking').forEach(btn => {
    btn.addEventListener('click', () => {
      const id = btn.dataset.id;
      if (!id) return;
      if (btn.dataset.ended === 'false') {
        const endTimeStr = btn.dataset.endtime || 'kết thúc';
        showToast(`Buổi học chưa kết thúc. Bạn chỉ có thể xác nhận hoàn thành sau ${endTimeStr}.`, 'error');
        return;
      }
      const booking = bookingsList.find(b => String(b.bookingId) === String(id));
      if (!booking) return;
      openCompleteModal(booking);
    });
  });
}

/**
 * Mở modal Đổi lịch học thử
 */
function openRescheduleModal(booking) {
  activeBooking = booking;
  if ($('rescheduleTutorName')) $('rescheduleTutorName').textContent = booking.partnerName || 'Gia sư';
  if ($('rescheduleCurrentTimeStr')) $('rescheduleCurrentTimeStr').textContent = formatBookingTime(booking.scheduledStartTime, booking.durationMinutes);

  const alertBox = $('rescheduleAlertBox');
  if (alertBox) {
    alertBox.classList.add('hidden');
    alertBox.textContent = '';
  }

  // Gợi ý ngày mặc định: ngày mai
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const pad = (n) => String(n).padStart(2, '0');
  const defaultDate = `${tomorrow.getFullYear()}-${pad(tomorrow.getMonth() + 1)}-${pad(tomorrow.getDate())}`;

  if ($('rescheduleDateInput')) $('rescheduleDateInput').value = defaultDate;
  if ($('rescheduleTimeInput')) $('rescheduleTimeInput').value = '09:00';
  if ($('rescheduleReasonInput')) $('rescheduleReasonInput').value = '';

  $('modalRescheduleBooking')?.classList.remove('hidden');
}

/**
 * Mở modal Hủy yêu cầu
 */
function openCancelModal(booking) {
  activeBooking = booking;
  $('cancelTutorName').textContent = booking.partnerName || 'Gia sư';
  $('cancelTimeStr').textContent = formatBookingTime(booking.scheduledStartTime, booking.durationMinutes);

  $('cancelReasonSelect').value = 'Bận việc đột xuất không thể tham gia đúng giờ';
  $('cancelCustomReason').value = '';
  $('cancelCustomReason').classList.add('hidden');

  $('modalCancelBooking').classList.remove('hidden');
}

/**
 * Mở modal Xác nhận hoàn thành buổi học
 */
function openCompleteModal(booking) {
  activeCompleteBooking = booking;
  if ($('completeTutorName')) {
    $('completeTutorName').textContent = booking.partnerName || 'Gia sư';
  }
  if ($('completeTimeStr')) {
    $('completeTimeStr').textContent = formatBookingTime(booking.scheduledStartTime, booking.durationMinutes);
  }
  $('modalConfirmComplete')?.classList.remove('hidden');
}

/**
 * Đóng modal
 */
function closeModals() {
  $('modalCancelBooking')?.classList.add('hidden');
  $('modalRescheduleBooking')?.classList.add('hidden');
  $('modalConfirmComplete')?.classList.add('hidden');
  activeBooking = null;
  activeCompleteBooking = null;
}

/**
 * Tải danh sách yêu cầu học thử từ máy chủ
 */
async function loadBookings() {
  setVisible($('loadingState'), true);
  setVisible($('errorState'), false);
  setVisible($('emptyState'), false);
  setVisible($('bookingsList'), false);

  try {
    const res = await API.trialBookings.getMyRequests('ALL', 0, 50);
    bookingsList = res?.items || res?.content || res?.data?.items || res?.data?.content || (Array.isArray(res) ? res : []);

    updateCounts();
    switchViewMode(currentViewMode);
  } catch (err) {
    console.error('Lỗi tải danh sách yêu cầu học viên:', err);
    $('errorMessage').textContent = err instanceof ApiError ? err.message : 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.';
    setVisible($('errorState'), true);
  } finally {
    setVisible($('loadingState'), false);
  }
}

/**
 * Khởi tạo sự kiện trên trang
 */
function initEvents() {
  // Chuyển đổi chế độ xem: Thời khóa biểu tuần vs Danh sách chi tiết
  $('btnViewCalendar')?.addEventListener('click', () => switchViewMode('CALENDAR'));
  $('btnViewList')?.addEventListener('click', () => switchViewMode('LIST'));

  // Điều hướng tuần trên Thời khóa biểu học viên
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

  // Tabs lọc trạng thái
  const tabBtns = document.querySelectorAll('#statusTabs .tab-btn');
  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      tabBtns.forEach(b => {
        b.className = 'tab-btn px-4 py-2 rounded-xl text-[#64748B] hover:bg-[#F1F5F9] hover:text-[#0F172A] transition-all flex items-center gap-1.5 cursor-pointer';
      });
      btn.className = 'tab-btn px-4 py-2 rounded-xl bg-[#006194] text-white shadow-xs transition-all flex items-center gap-1.5 cursor-pointer';

      currentFilter = btn.dataset.status;
      renderBookings();
    });
  });

  // Nút làm mới & thử lại
  $('btnRefresh')?.addEventListener('click', loadBookings);
  $('btnRetry')?.addEventListener('click', loadBookings);

  // Đóng modal
  document.querySelectorAll('.modal-close').forEach(btn => {
    btn.addEventListener('click', closeModals);
  });

  // Đổi lựa chọn lý do hủy
  $('cancelReasonSelect')?.addEventListener('change', (e) => {
    const isCustom = e.target.value === 'Khác';
    $('cancelCustomReason').classList.toggle('hidden', !isCustom);
    if (isCustom) $('cancelCustomReason').focus();
  });

  // Gửi xác nhận Hủy yêu cầu (CANCEL)
  $('btnSubmitCancel')?.addEventListener('click', async () => {
    if (!activeBooking) return;

    const select = $('cancelReasonSelect');
    let reason = select.value;
    if (reason === 'Khác') {
      const customVal = $('cancelCustomReason').value.trim();
      reason = customVal || 'Học viên thay đổi kế hoạch';
    }

    const btn = $('btnSubmitCancel');
    const btnText = $('btnSubmitCancelText');
    btn.disabled = true;
    btnText.textContent = 'Đang xử lý...';

    try {
      await API.trialBookings.cancel(activeBooking.bookingId, {
        cancelReason: reason
      });

      closeModals();
      showToast('Đã hủy yêu cầu học thử thành công.');
      await loadBookings();
    } catch (err) {
      showToast(err.message || 'Không thể hủy yêu cầu lúc này', 'error');
    } finally {
      btn.disabled = false;
      btnText.textContent = 'Xác nhận hủy';
    }
  });

  // Gửi xác nhận Đổi lịch học thử (RESCHEDULE - SRS 5.3)
  $('btnSubmitReschedule')?.addEventListener('click', async () => {
    if (!activeBooking) return;

    const dateVal = $('rescheduleDateInput')?.value.trim();
    const timeVal = $('rescheduleTimeInput')?.value.trim();
    const reasonVal = $('rescheduleReasonInput')?.value.trim();
    const alertBox = $('rescheduleAlertBox');

    if (!dateVal || !timeVal) {
      if (alertBox) {
        alertBox.classList.remove('hidden');
        alertBox.textContent = 'Vui lòng chọn ngày và giờ học mới.';
      }
      return;
    }

    const isoStartTime = `${dateVal}T${timeVal}:00+07:00`;

    const btn = $('btnSubmitReschedule');
    const btnText = $('btnSubmitRescheduleText');
    btn.disabled = true;
    if (btnText) btnText.textContent = 'Đang xử lý...';

    try {
      await API.trialBookings.reschedule(activeBooking.bookingId, {
        newScheduledStartTime: isoStartTime,
        rescheduleReason: reasonVal || 'Học viên yêu cầu dời lịch học thử'
      });

      closeModals();
      showToast('Đã dời lịch học thử thành công!');
      await loadBookings();
    } catch (err) {
      if (alertBox) {
        alertBox.classList.remove('hidden');
        alertBox.textContent = err.message || 'Không thể dời lịch lúc này. Vui lòng thử lại sau.';
      } else {
        showToast(err.message || 'Không thể dời lịch lúc này', 'error');
      }
    } finally {
      btn.disabled = false;
      if (btnText) btnText.textContent = 'Xác nhận đổi lịch';
    }
  });

  // Gửi xác nhận Hoàn thành buổi học (COMPLETE)
  $('btnSubmitComplete')?.addEventListener('click', async () => {
    if (!activeCompleteBooking) return;
    const btn = $('btnSubmitComplete');
    const btnText = $('btnSubmitCompleteText');
    btn.disabled = true;
    if (btnText) btnText.textContent = 'Đang xử lý...';

    try {
      await API.trialBookings.complete(activeCompleteBooking.bookingId);
      closeModals();
      showToast('Đã xác nhận hoàn thành buổi học thành công!');
      await loadBookings();
    } catch (err) {
      showToast(err.message || 'Không thể hoàn thành buổi học lúc này', 'error');
    } finally {
      btn.disabled = false;
      if (btnText) btnText.textContent = 'Xác nhận hoàn thành';
    }
  });
}

document.addEventListener('DOMContentLoaded', () => {
  initEvents();
  loadBookings();
});

