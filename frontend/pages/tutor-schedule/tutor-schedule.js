/**
 * KizunaTutor - Quản lý Lịch giảng dạy & Thời khóa biểu của Gia sư (tutor-schedule.js)
 * Phân hệ Gia sư - Thiết kế theo mẫu Thời khóa biểu lưới tuần (Weekly Time Grid)
 * và phân tách 2 mục đích: (1) Lịch dạy thực tế & chốt lịch, (2) Cài đặt ca rảnh
 */

import { API, ApiError } from '/js/api.js';
import { mountTutorLayout, setTutorIdentity } from '/components/tutor-layout/tutor-layout.js';

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

let currentTab = 'LESSONS'; // 'LESSONS' hoặc 'AVAILABILITY'
let currentWeekMonday = getMondayOf(new Date());
let bookingsList = [];
let availabilitiesList = [];
let eligibleLearnersList = [];

function setVisible(el, visible) {
  if (!el) return;
  el.classList.toggle('hidden', !visible);
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text == null ? '' : String(text);
  return div.innerHTML;
}

function showToast(message, type = 'success') {
  const toast = $('toastNotification');
  const msgEl = $('toastMsg');
  const iconEl = $('toastIcon');
  if (!toast || !msgEl || !iconEl) return;

  msgEl.textContent = message;
  if (type === 'error') {
    toast.className = 'fixed bottom-6 right-6 z-50 max-w-sm px-4 py-3 rounded-xl shadow-lg text-body-sm font-semibold flex items-center gap-2 transition-all bg-error text-on-error';
    iconEl.textContent = 'error';
  } else {
    toast.className = 'fixed bottom-6 right-6 z-50 max-w-sm px-4 py-3 rounded-xl shadow-lg text-body-sm font-semibold flex items-center gap-2 transition-all bg-[#0F2545] text-white';
    iconEl.textContent = 'check_circle';
  }

  toast.classList.remove('hidden');
  clearTimeout(toast.__timer);
  toast.__timer = setTimeout(() => {
    toast.classList.add('hidden');
  }, 3500);
}

/**
 * Lấy ngày Thứ Hai của tuần chứa một ngày bất kỳ (00:00:00)
 */
function getMondayOf(date) {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  const day = d.getDay(); // 0 is Sunday, 1 is Monday
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

// ---------------------------------------------------------------------------
// CHUYỂN TABS (LỊCH DẠY vs CÀI ĐẶT CA RẢNH)
// ---------------------------------------------------------------------------
function switchTab(tab) {
  currentTab = tab;
  const isLessons = tab === 'LESSONS';

  $('tabBtnLessons').className = isLessons
    ? 'px-4 py-3 text-label-md font-bold border-b-2 border-primary text-primary flex items-center gap-2 transition-all cursor-pointer'
    : 'px-4 py-3 text-label-md font-semibold border-b-2 border-transparent text-on-surface-variant hover:text-on-surface flex items-center gap-2 transition-all cursor-pointer';

  $('tabBtnAvailability').className = !isLessons
    ? 'px-4 py-3 text-label-md font-bold border-b-2 border-primary text-primary flex items-center gap-2 transition-all cursor-pointer'
    : 'px-4 py-3 text-label-md font-semibold border-b-2 border-transparent text-on-surface-variant hover:text-on-surface flex items-center gap-2 transition-all cursor-pointer';

  setVisible($('tabContentLessons'), isLessons);
  setVisible($('tabContentAvailability'), !isLessons);
}

// ---------------------------------------------------------------------------
// PHÂN HỆ 1: LỊCH DẠY THỰC TẾ (WEEKLY TIME GRID)
// ---------------------------------------------------------------------------
function renderWeeklyCalendar() {
  const sunday = new Date(currentWeekMonday.getTime() + 6 * 86400000);
  $('currentWeekRangeText').textContent = `Tuần: ${formatDateDMY(currentWeekMonday)} - ${formatDateDMY(sunday)}`;

  const todayStr = formatDateYMD(new Date());

  // 1. Render Header Row (Thứ 2 -> Chủ nhật)
  const headerHtml = `
    <div class="p-3 text-caption font-bold text-on-surface-variant text-center border-r border-outline-variant/30 flex items-center justify-center">
      Giờ
    </div>
    ${WEEKDAYS_DEF.map((day, idx) => {
      const dayDate = new Date(currentWeekMonday.getTime() + idx * 86400000);
      const isToday = formatDateYMD(dayDate) === todayStr;
      return `
        <div class="p-2.5 text-center border-r last:border-r-0 border-outline-variant/30 ${isToday ? 'bg-primary-fixed/20' : ''}">
          <p class="text-[11px] font-bold ${isToday ? 'text-primary' : 'text-on-surface-variant'} uppercase">${day.colName}</p>
          <div class="mt-0.5 inline-flex items-center justify-center ${isToday ? 'w-7 h-7 rounded-full bg-primary text-white font-bold' : 'text-title-sm font-bold text-on-surface'}">
            ${dayDate.getDate()}
          </div>
          <p class="text-[10px] text-on-surface-variant mt-0.5">${day.name}</p>
        </div>
      `;
    }).join('')}
  `;
  $('gridHeaderRow').innerHTML = headerHtml;

  // Lọc các buổi học trong tuần này
  const weekStartTs = currentWeekMonday.getTime();
  const weekEndTs = currentWeekMonday.getTime() + 7 * 86400000;

  const weekLessons = bookingsList.filter(b => {
    if (!b.scheduledStartTime) return false;
    const start = new Date(b.scheduledStartTime);
    const ts = start.getTime();
    return ts >= weekStartTs && ts < weekEndTs && !['CANCELLED', 'DECLINED'].includes(b.bookingStatus);
  });

  $('badgeLessonsCount').textContent = weekLessons.length;

  // 2. Render Time Rows (07:00 đến 21:00)
  const bodyHtml = HOURS_GRID.map(hour => {
    const timeLabel = formatHour(hour);

    const cellsHtml = WEEKDAYS_DEF.map((day, idx) => {
      const cellDate = new Date(currentWeekMonday.getTime() + idx * 86400000);
      const dateYmd = formatDateYMD(cellDate);

      // Tìm buổi học rơi vào ô ngày & giờ này
      const matchedLessons = weekLessons.filter(b => {
        const d = new Date(b.scheduledStartTime);
        return formatDateYMD(d) === dateYmd && d.getHours() === hour;
      });

      let contentHtml = '';
      if (matchedLessons.length > 0) {
        contentHtml = matchedLessons.map(lesson => {
          const isTrial = (lesson.durationMinutes || 50) <= 50 && (!lesson.locationDetails || !lesson.locationDetails.includes('Lớp'));
          const themeClass = isTrial
            ? 'bg-sky-50/90 border-sky-300 text-sky-950'
            : 'bg-emerald-50/90 border-emerald-300 text-emerald-950';
          const barClass = isTrial ? 'bg-[#006194]' : 'bg-emerald-600';
          const badgeClass = isTrial ? 'bg-sky-100 text-[#006194]' : 'bg-emerald-100 text-emerald-800';
          const badgeText = isTrial ? 'Học thử 0đ' : 'Lớp 1-1';

          const sTime = new Date(lesson.scheduledStartTime);
          const eTime = lesson.scheduledEndTime ? new Date(lesson.scheduledEndTime) : new Date(sTime.getTime() + (lesson.durationMinutes || 50) * 60000);
          const timeSpan = `${pad(sTime.getHours())}:${pad(sTime.getMinutes())} - ${pad(eTime.getHours())}:${pad(eTime.getMinutes())}`;
          const partner = escapeHtml(lesson.partnerName || 'Học viên');
          const title = escapeHtml(lesson.locationDetails || (isTrial ? 'Học thử 1-1' : 'Tiếng Nhật 1-1'));
          const meetLink = lesson.meetingLink;

          return `
            <div class="relative p-2 rounded-xl border ${themeClass} shadow-2xs space-y-1 hover:shadow-md transition-all text-left">
              <div class="absolute left-0 top-0 bottom-0 w-1 ${barClass} rounded-l-xl"></div>
              <div class="flex items-center justify-between pl-1 gap-1">
                <span class="font-bold text-[12px] truncate flex items-center gap-1">
                  ${partner}
                  <span class="material-symbols-outlined text-[13px] ${isTrial ? 'text-[#006194]' : 'text-emerald-600'}">check_circle</span>
                </span>
                <span class="text-[9px] font-bold px-1.5 py-0.5 rounded ${badgeClass} shrink-0">${badgeText}</span>
              </div>
              <div class="text-[10px] font-semibold pl-1 opacity-80">
                ${timeSpan}
              </div>
              <div class="pl-1 text-[11px] font-medium truncate text-on-surface">
                ${title}
              </div>
            </div>
          `;
        }).join('');
      } else {
        // Ô trống: click vào để lên lịch nhanh
        contentHtml = `
          <div class="w-full h-full min-h-[50px] flex items-center justify-center opacity-0 hover:opacity-100 transition-opacity">
            <span class="text-[11px] font-semibold text-primary inline-flex items-center gap-0.5 bg-primary/10 px-2 py-1 rounded-lg pointer-events-none">
              <span class="material-symbols-outlined text-[14px]">add</span> Xếp lịch
            </span>
          </div>
        `;
      }

      return `
        <div class="calendar-slot-cell p-1.5 border-r last:border-r-0 border-outline-variant/20 cursor-pointer overflow-hidden relative group" data-date="${dateYmd}" data-hour="${hour}" title="Bấm để lên lịch cho ngày ${dateYmd} lúc ${timeLabel}">
          ${contentHtml}
        </div>
      `;
    }).join('');

    return `
      <div class="grid grid-cols-8 items-stretch border-b border-outline-variant/20 hover:bg-surface-container-low/20 transition-colors">
        <div class="calendar-time-cell p-2 border-r border-outline-variant/30 text-[11px] font-bold text-on-surface-variant flex items-start justify-center pt-1.5 bg-surface-container-low/20">
          ${timeLabel}
        </div>
        ${cellsHtml}
      </div>
    `;
  }).join('');

  $('gridBodyRows').innerHTML = bodyHtml;

  // Gắn sự kiện click vào ô trống trên lưới để lên lịch
  document.querySelectorAll('.calendar-slot-cell').forEach(cell => {
    cell.addEventListener('click', (e) => {
      // Nếu click vào thẻ buổi học có link Meet thì không mở modal
      if (e.target.closest('a')) return;
      const dateStr = cell.dataset.date;
      const hourNum = Number(cell.dataset.hour);
      openScheduleModal(dateStr, `${pad(hourNum)}:00`);
    });
  });
}

// ---------------------------------------------------------------------------
// MODAL LÊN LỊCH & CHỐT LỊCH BUỔI HỌC CHO HỌC VIÊN
// ---------------------------------------------------------------------------
function openScheduleModal(preselectedDate = null, preselectedTime = null) {
  const modal = $('modalScheduleLesson');
  if (!modal) return;

  $('scheduleLessonError').classList.add('hidden');

  // Điền ngày & giờ
  const todayStr = formatDateYMD(new Date());
  $('scheduleDateInput').value = preselectedDate || todayStr;
  $('scheduleStartTimeInput').value = preselectedTime || '18:00';
  $('scheduleDurationSelect').value = '60';
  $('scheduleTitleInput').value = 'Tiếng Nhật giao tiếp 1-1';

  // Cập nhật danh sách học viên
  const select = $('scheduleLearnerSelect');
  select.innerHTML = '';

  const uniqueLearners = new Map();
  // 1. Từ eligibleLearnersList
  eligibleLearnersList.forEach(l => {
    uniqueLearners.set(String(l.learnerId), l.fullName || l.email || 'Học viên');
  });
  // 2. Từ bookingsList
  bookingsList.forEach(b => {
    if (b.partnerRole === 'Học viên') {
      const learnerKey = b.learnerId || b.partnerId;
      if (learnerKey) {
        uniqueLearners.set(String(learnerKey), b.partnerName || 'Học viên');
      }
    }
  });

  if (uniqueLearners.size === 0) {
    select.innerHTML = '<option value="">Chưa có học viên kết nối. Vui lòng kết nối với học viên trước.</option>';
  } else {
    select.innerHTML = '<option value="">-- Chọn học viên --</option>' +
      Array.from(uniqueLearners.entries()).map(([id, name]) => `
        <option value="${id}">${escapeHtml(name)}</option>
      `).join('');
  }

  modal.classList.remove('hidden');
}

function closeScheduleModal() {
  $('modalScheduleLesson')?.classList.add('hidden');
}

// ---------------------------------------------------------------------------
// PHÂN HỆ 2: CÀI ĐẶT CA RẢNH (AVAILABILITY CHO HỌC VIÊN TÌM KIẾM)
// ---------------------------------------------------------------------------
function computeDurationText(startStr, endStr) {
  if (!startStr || !endStr) return '';
  const [sh, sm] = startStr.split(':').map(Number);
  const [eh, em] = endStr.split(':').map(Number);
  if (isNaN(sh) || isNaN(sm) || isNaN(eh) || isNaN(em)) return '';

  const totalMin = (eh * 60 + em) - (sh * 60 + sm);
  if (totalMin <= 0) return 'Giờ kết thúc phải sau giờ bắt đầu';

  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  const hText = h > 0 ? `${h} giờ ` : '';
  const mText = m > 0 ? `${m} phút` : '';
  const approxSlots = Math.floor(totalMin / 50);

  return `Thời lượng: ${hText}${mText} (học viên có thể đặt tối đa ~${approxSlots} ca học thử 50 phút)`;
}

function updateDurationPreview() {
  const start = $('inputStartTime')?.value;
  const end = $('inputEndTime')?.value;
  const preview = $('slotDurationPreview');
  if (preview) {
    preview.textContent = computeDurationText(start, end);
  }
}

function renderAvailabilityGrid() {
  const grid = $('availabilityGrid');
  if (!grid) return;

  $('badgeAvailabilityCount').textContent = availabilitiesList.length;

  grid.innerHTML = WEEKDAYS_DEF.map(day => {
    const daySlots = availabilitiesList.filter(a => Number(a.dayOfWeek) === day.dow);
    const hasSlots = daySlots.length > 0;

    const slotsHtml = hasSlots
      ? daySlots.map(slot => {
          const isActive = slot.isActive !== false;
          return `
            <div class="p-3 rounded-xl bg-surface-container-low border ${isActive ? 'border-primary/30' : 'border-outline-variant/60 opacity-60'} space-y-2">
              <div class="flex items-center justify-between">
                <span class="text-caption font-bold ${isActive ? 'text-primary' : 'text-on-surface-variant'}">
                  ${escapeHtml(slot.startTime)} - ${escapeHtml(slot.endTime)}
                </span>
                <button type="button" class="btn-delete-slot text-on-surface-variant hover:text-error p-1 rounded-md transition-colors cursor-pointer" data-id="${slot.availabilityId}" title="Xóa ca rảnh này">
                  <span class="material-symbols-outlined text-[16px]">delete</span>
                </button>
              </div>

              <div class="flex items-center justify-between text-caption pt-1 border-t border-outline-variant/30">
                <span class="${isActive ? 'text-emerald-700 font-semibold' : 'text-slate-500'}">
                  ${isActive ? 'Đang mở' : 'Tạm ẩn'}
                </span>
                <button type="button" class="btn-toggle-slot text-[11px] font-semibold text-primary hover:underline cursor-pointer" data-id="${slot.availabilityId}" data-active="${isActive}">
                  ${isActive ? 'Tạm tắt' : 'Bật lại'}
                </button>
              </div>
            </div>
          `;
        }).join('')
      : `
        <div class="py-8 px-2 rounded-xl border border-dashed border-outline-variant/60 text-center space-y-2 bg-surface-container-low/30">
          <span class="material-symbols-outlined text-outline text-[24px]">event_busy</span>
          <p class="text-caption text-on-surface-variant">Chưa có ca rảnh</p>
          <button type="button" class="btn-quick-add text-caption font-bold text-primary hover:underline cursor-pointer" data-dow="${day.dow}">
            + Thêm ca
          </button>
        </div>
      `;

    return `
      <div class="bg-surface-container-lowest rounded-2xl p-4 shadow-sm border border-outline-variant/40 flex flex-col justify-between space-y-3">
        <!-- Đầu thẻ Thứ -->
        <div class="flex items-center justify-between pb-2 border-b border-outline-variant/30">
          <div>
            <h3 class="text-label-md font-bold text-on-surface">${day.name}</h3>
            <span class="text-caption text-on-surface-variant">${daySlots.length} ca rảnh</span>
          </div>
          <button type="button" class="btn-quick-add w-7 h-7 rounded-lg bg-surface-container hover:bg-primary hover:text-on-primary text-on-surface flex items-center justify-center transition-all cursor-pointer" data-dow="${day.dow}" title="Thêm ca rảnh cho ${day.name}">
            <span class="material-symbols-outlined text-[18px]">add</span>
          </button>
        </div>

        <!-- Danh sách ca rảnh -->
        <div class="space-y-2 flex-1">
          ${slotsHtml}
        </div>
      </div>
    `;
  }).join('');

  bindAvailabilityEvents();
}

function bindAvailabilityEvents() {
  document.querySelectorAll('.btn-quick-add').forEach(btn => {
    btn.addEventListener('click', () => {
      const dow = btn.dataset.dow;
      openAddSlotModal(dow);
    });
  });

  document.querySelectorAll('.btn-delete-slot').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.dataset.id;
      if (!id) return;
      if (!confirm('Bạn có chắc chắn muốn xóa ca rảnh này không? Ca rảnh này sẽ không còn hiển thị cho học viên đặt lịch.')) {
        return;
      }
      try {
        await API.tutorSchedule.removeSlot(id);
        showToast('Đã xóa ca rảnh thành công');
        await loadAllData();
      } catch (err) {
        showToast(err.message || 'Không thể xóa ca rảnh lúc này', 'error');
      }
    });
  });

  document.querySelectorAll('.btn-toggle-slot').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.dataset.id;
      const currentActive = btn.dataset.active === 'true';
      try {
        await API.tutorSchedule.toggleSlot(id, !currentActive);
        showToast(!currentActive ? 'Đã bật nhận lịch cho ca này' : 'Đã tạm ẩn ca rảnh này');
        await loadAllData();
      } catch (err) {
        showToast(err.message || 'Không thể cập nhật trạng thái ca rảnh', 'error');
      }
    });
  });
}

function openAddSlotModal(preselectedDow = null, start = null, end = null) {
  if (preselectedDow) $('selectDayOfWeek').value = String(preselectedDow);
  if (start) $('inputStartTime').value = start;
  if (end) $('inputEndTime').value = end;

  $('formAddSlotError').classList.add('hidden');
  updateDurationPreview();
  $('modalAddSlot').classList.remove('hidden');
}

function closeAddSlotModal() {
  $('modalAddSlot').classList.add('hidden');
}

// ---------------------------------------------------------------------------
// TẢI DỮ LIỆU TỔNG THỂ TỪ MÁY CHỦ
// ---------------------------------------------------------------------------
async function loadAllData() {
  try {
    const [availData, bookingsData, eligibleData] = await Promise.allSettled([
      API.tutorSchedule.getMySchedule(),
      API.trialBookings.getMyRequests('ALL', 0, 100),
      API.classrooms.eligibleLearners().catch(() => [])
    ]);

    // 1. Ca rảnh
    if (availData.status === 'fulfilled' && availData.value) {
      availabilitiesList = Array.isArray(availData.value) ? availData.value : (availData.value.items || availData.value.data || []);
    }

    // 2. Buổi học thực tế
    if (bookingsData.status === 'fulfilled' && bookingsData.value) {
      const bRes = bookingsData.value;
      bookingsList = Array.isArray(bRes) ? bRes : (bRes.content || bRes.items || bRes.data || []);
    }

    // 3. Học viên đủ điều kiện
    if (eligibleData.status === 'fulfilled' && eligibleData.value) {
      eligibleLearnersList = Array.isArray(eligibleData.value) ? eligibleData.value : (eligibleData.value.data || []);
    }

    renderWeeklyCalendar();
    renderAvailabilityGrid();
  } catch (err) {
    console.error('Lỗi tải dữ liệu lịch:', err);
    showToast(err.message || 'Không thể tải toàn bộ dữ liệu lịch', 'error');
  }
}

// ---------------------------------------------------------------------------
// SỰ KIỆN TƯƠNG TÁC
// ---------------------------------------------------------------------------
function initEvents() {
  // Tabs switching
  $('tabBtnLessons')?.addEventListener('click', () => switchTab('LESSONS'));
  $('tabBtnAvailability')?.addEventListener('click', () => switchTab('AVAILABILITY'));

  // Nút mở modal Lên lịch buổi học (+ Schedule a lesson dạng viên thuốc)
  $('btnOpenScheduleLesson')?.addEventListener('click', () => {
    openScheduleModal();
  });

  // Nút mở modal Thêm ca rảnh
  $('btnOpenAddModal')?.addEventListener('click', () => {
    openAddSlotModal();
  });

  // Điều hướng tuần
  $('btnPrevWeek')?.addEventListener('click', () => {
    currentWeekMonday = new Date(currentWeekMonday.getTime() - 7 * 86400000);
    renderWeeklyCalendar();
  });

  $('btnNextWeek')?.addEventListener('click', () => {
    currentWeekMonday = new Date(currentWeekMonday.getTime() + 7 * 86400000);
    renderWeeklyCalendar();
  });

  $('btnToday')?.addEventListener('click', () => {
    currentWeekMonday = getMondayOf(new Date());
    renderWeeklyCalendar();
  });

  // Đóng modals
  document.querySelectorAll('.modal-schedule-close').forEach(btn => {
    btn.addEventListener('click', closeScheduleModal);
  });
  document.querySelectorAll('.modal-add-close').forEach(btn => {
    btn.addEventListener('click', closeAddSlotModal);
  });

  // Presets ca rảnh
  document.querySelectorAll('.btn-preset').forEach(btn => {
    btn.addEventListener('click', () => {
      const start = btn.dataset.start;
      const end = btn.dataset.end;
      const dow = btn.dataset.dow || '2';
      openAddSlotModal(dow, start, end);
    });
  });

  $('inputStartTime')?.addEventListener('input', updateDurationPreview);
  $('inputEndTime')?.addEventListener('input', updateDurationPreview);

  // Gửi form Lên lịch buổi học cho học viên
  $('formScheduleLesson')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const learnerId = $('scheduleLearnerSelect').value;
    const dateStr = $('scheduleDateInput').value;
    const startTimeStr = $('scheduleStartTimeInput').value;
    const duration = Number($('scheduleDurationSelect').value) || 60;
    const title = $('scheduleTitleInput').value.trim();
    const note = $('scheduleNoteInput').value.trim();
    const errEl = $('scheduleLessonError');
    const submitBtn = $('btnSubmitScheduleLesson');

    errEl.classList.add('hidden');

    if (!learnerId) {
      errEl.textContent = 'Vui lòng chọn học viên để xếp lịch.';
      errEl.classList.remove('hidden');
      return;
    }
    if (!dateStr || !startTimeStr) {
      errEl.textContent = 'Vui lòng chọn đầy đủ ngày và giờ bắt đầu.';
      errEl.classList.remove('hidden');
      return;
    }

    const scheduledStartTime = `${dateStr}T${startTimeStr}:00+07:00`;

    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="material-symbols-outlined text-[18px] animate-spin">refresh</span><span>Đang xử lý...</span>';

    try {
      await API.trialBookings.scheduleLesson({
        learnerId: learnerId,
        scheduledStartTime: scheduledStartTime,
        durationMinutes: duration,
        lessonTitle: title,
        meetingLink: null,
        note: note,
        learningMode: 'ONLINE'
      });

      closeScheduleModal();
      showToast('Đã lên lịch buổi học thành công và gửi thông báo tới học sinh!');
      await loadAllData();
    } catch (err) {
      errEl.textContent = err instanceof ApiError ? err.message : (err.message || 'Lỗi khi lên lịch buổi học');
      errEl.classList.remove('hidden');
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerHTML = '<span class="material-symbols-outlined text-[18px]">send</span><span>Xác nhận & Gửi lịch cho học viên</span>';
    }
  });

  // Gửi form Thêm ca rảnh
  $('formAddSlot')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const dow = Number($('selectDayOfWeek').value);
    const startTime = $('inputStartTime').value.trim();
    const endTime = $('inputEndTime').value.trim();
    const errEl = $('formAddSlotError');
    const submitBtn = $('btnSubmitSlot');
    const submitText = $('btnSubmitSlotText');

    errEl.classList.add('hidden');

    if (!startTime || !endTime) {
      errEl.textContent = 'Vui lòng điền đầy đủ giờ bắt đầu và giờ kết thúc.';
      errEl.classList.remove('hidden');
      return;
    }

    submitBtn.disabled = true;
    submitText.textContent = 'Đang lưu...';

    try {
      await API.tutorSchedule.addSlot({
        dayOfWeek: dow,
        startTime: startTime,
        endTime: endTime,
        isActive: true
      });

      closeAddSlotModal();
      showToast('Đã lưu ca rảnh thành công! Ca này đã hiển thị trên hồ sơ tìm kiếm gia sư.');
      await loadAllData();
    } catch (err) {
      errEl.textContent = err instanceof ApiError ? err.message : (err.message || 'Lỗi khi lưu ca rảnh');
      errEl.classList.remove('hidden');
    } finally {
      submitBtn.disabled = false;
      submitText.textContent = 'Lưu ca rảnh';
    }
  });
}

// Khởi tạo layout Gia sư
mountTutorLayout('schedule');

// Tải danh tính gia sư
API.tutorVerification.getMine()
  .then(data => {
    if (data?.profile) {
      setTutorIdentity({
        fullName: data.profile.fullName,
        jlptLevel: data.profile.highestJlptLevel,
        statusLabel: data.statusLabel
      });
    }
  })
  .catch(() => {});

document.addEventListener('DOMContentLoaded', () => {
  initEvents();
  loadAllData();
});
