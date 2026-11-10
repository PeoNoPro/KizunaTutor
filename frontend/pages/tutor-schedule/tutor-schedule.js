/**
 * KizunaTutor - Quản lý Lịch giảng dạy & Thời khóa biểu của Gia sư (tutor-schedule.js)
 * Phân hệ Gia sư - Thiết kế theo mẫu Thời khóa biểu lưới tuần (Weekly Time Grid)
 * và phân tách 2 mục đích: (1) Lịch dạy thực tế & chốt lịch, (2) Cài đặt ca rảnh
 */

import { API, ApiError } from '/js/api.js';
import { renderWeekTimeGrid, trialEvent, officialEvent } from '/js/week-time-grid.js';
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


let currentTab = 'LESSONS'; // 'LESSONS' hoặc 'AVAILABILITY'
let currentWeekMonday = getMondayOf(new Date());
let bookingsList = [];
let availabilitiesList = [];
let classTimetable = []; // Lịch học chính thức hằng tuần từ FE-06 (GET /api/classrooms/schedules/me)

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
  // Vẽ lại khi quay về tab lịch dạy để lưới tự cuộn tới buổi học (khung bị ẩn thì không cuộn được)
  if (isLessons) renderWeeklyCalendar();
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
    <div class="p-3 text-caption font-bold text-on-surface-variant text-center border-r border-outline-variant/30 flex items-center justify-center sticky left-0 z-10 bg-surface-container-lowest">
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

  // Buổi học chính thức: lặp hằng tuần theo lịch lớp (FE-06), chỉ vẽ từ tuần mở lớp trở đi
  const weekOfficial = classTimetable.map(entry => {
    const dayIdx = Number(entry.dayOfWeek) - 1; // 1 = Thứ Hai
    const date = new Date(currentWeekMonday.getTime() + dayIdx * 86400000);
    const [sh, sm] = String(entry.startTime || '').split(':').map(Number);
    const [eh, em] = String(entry.endTime || '').split(':').map(Number);
    const started = entry.classStartedAt ? new Date(entry.classStartedAt) : null;
    const endOfDay = new Date(date.getTime() + 86400000);
    if (dayIdx < 0 || dayIdx > 6 || Number.isNaN(sh) || (started && started >= endOfDay)) return null;
    return { ...entry, dateYmd: formatDateYMD(date), startHour: sh, timeSpan: `${pad(sh)}:${pad(sm || 0)} - ${pad(eh)}:${pad(em || 0)}` };
  }).filter(Boolean);

  $('badgeLessonsCount').textContent = weekLessons.length + weekOfficial.length;

  // 2. Thân lưới 24 giờ
  // Thân lưới: lịch theo phút (thẻ đặt đúng giờ bắt đầu, cao theo thời lượng) — js/week-time-grid.js
  renderWeekTimeGrid({
    bodyEl: $('gridBodyRows'),
    scrollEl: $('gridScroll'),
    monday: currentWeekMonday,
    events: [
      ...weekOfficial.map(o => officialEvent(o, `/tutor/classrooms?space=${encodeURIComponent(o.spaceId)}`)),
      ...weekLessons.map(b => trialEvent(b, '/tutor/connections'))
    ]
  });
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
  const approxSlots = Math.floor(totalMin / 30);

  return `Thời lượng: ${hText}${mText} (học viên có thể đặt tối đa ~${approxSlots} ca học thử 30 phút)`;
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
    const [availData, bookingsData, timetableData] = await Promise.allSettled([
      API.tutorSchedule.getMySchedule(),
      API.trialBookings.getMyRequests('ALL', 0, 100),
      API.classrooms.timetable().catch(() => [])
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

    // 3. Lịch học chính thức hằng tuần của các lớp (FE-06)
    if (timetableData.status === 'fulfilled' && Array.isArray(timetableData.value)) {
      classTimetable = timetableData.value;
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
