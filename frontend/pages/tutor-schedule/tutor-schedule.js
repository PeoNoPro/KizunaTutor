/**
 * KizunaTutor - Quản lý Lịch giảng dạy & Thời khóa biểu của Gia sư (tutor-schedule.js)
 * Phân hệ Gia sư - Thiết kế theo mẫu Thời khóa biểu lưới tuần (Weekly Time Grid)
 * và phân tách 2 mục đích: (1) Lịch dạy thực tế & chốt lịch, (2) Cài đặt ca rảnh
 */

import { API, ApiError } from '/js/api.js';
import { renderWeekTimeGrid, trialEvent, officialEvent, dayDateLabel, ymd, weekRange } from '/js/week-time-grid.js';
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
let weekSessions = []; // Buổi học chính thức của tuần đang xem (FE-06: GET /api/classrooms/sessions/me)
let weekSessionsToken = 0; // Bỏ kết quả cũ khi chuyển tuần nhanh
let weekSessionsKey = ''; // Tuần của weekSessions đang vẽ ("yyyy-MM-dd" Thứ Hai)
let manageableClasses = []; // Lớp gia sư đang quản lý được (để thêm buổi học)
let currentSession = null; // Buổi đang mở trong hộp thoại
let sessionModalMode = 'edit'; // 'edit' (dời / hủy buổi) hoặc 'add' (thêm buổi)

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
      Ca
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

  // Buổi học chính thức của tuần: máy chủ đã sinh lịch cho đủ số buổi của khóa (áp dụng dời / hủy / thêm buổi riêng)
  const weekOfficial = weekSessions;

  $('badgeLessonsCount').textContent = weekLessons.length + weekOfficial.length;

  // 2. Thân lưới theo ca (Sáng / Tối) — js/week-time-grid.js. Bấm buổi học chính thức để dời riêng buổi của tuần này.
  renderWeekTimeGrid({
    bodyEl: $('gridBodyRows'),
    scrollEl: $('gridScroll'),
    monday: currentWeekMonday,
    events: [
      ...weekOfficial.map(o => officialEvent(o, null, { onClick: true })),
      ...weekLessons.map(b => trialEvent(b, '/tutor/connections'))
    ],
    onEventClick: openSessionModal
  });
}

// ---------------------------------------------------------------------------
// BUỔI HỌC CỤ THỂ CỦA KHÓA (FE-06): dời / hủy / thêm riêng trong MỘT tuần — các tuần khác giữ nguyên,
// lịch tự sinh cho đủ tổng số buổi của khóa (máy chủ tính sẵn: GET /api/classrooms/sessions/me)
// ---------------------------------------------------------------------------
async function loadWeekSessions() {
  const token = ++weekSessionsToken;
  const { from, to } = weekRange(currentWeekMonday);
  // Sang tuần khác: xóa buổi của tuần cũ. Tải lại cùng tuần (sau khi dời / hủy / thêm): giữ lịch đang hiện tới khi có dữ liệu mới.
  if (weekSessionsKey !== from) {
    weekSessions = [];
    weekSessionsKey = from;
  }
  renderWeeklyCalendar();
  setWeekLoading(true);
  try {
    const list = await API.classrooms.sessions(from, to);
    if (token !== weekSessionsToken) return;
    weekSessions = Array.isArray(list) ? list : [];
  } catch (err) {
    if (token !== weekSessionsToken) return;
    console.error('Lỗi tải lịch học của tuần:', err);
    showToast(err.userMessage || 'Không thể tải lịch học của tuần này.', 'error');
  }
  setWeekLoading(false);
  renderWeeklyCalendar();
}

function setWeekLoading(loading) {
  $('weekLoadingNote')?.classList.toggle('hidden', !loading);
}

function setMoveError(message) {
  const el = $('moveSessionError');
  if (!el) return;
  el.textContent = message || '';
  el.classList.toggle('hidden', !message);
}

function ymdToDmy(value) {
  const [y, m, d] = String(value || '').split('-');
  return y && m && d ? `${d}/${m}/${y}` : '';
}

/** Ô chọn ngày chỉ cho chọn trong khoảng [min, max] ("yyyy-MM-dd"), kèm dòng ghi khoảng ngày được chọn. */
function setDateRange(min, max, selected) {
  const input = $('moveSessionDate');
  input.min = min || '';
  input.max = max || '';
  input.value = selected;
  $('moveSessionDateRange').textContent = max
    ? `Chọn từ ${ymdToDmy(min)} đến ${ymdToDmy(max)}.`
    : `Chọn từ ${ymdToDmy(min)} trở đi.`;
}

/** Dời buổi được chọn từ hôm nay (và không trước ngày bắt đầu khóa) tới buổi cuối của khóa; lớp cũ: tối đa 1 năm tới. */
function courseDateRange(session) {
  const today = formatDateYMD(new Date());
  const min = session.courseStartDate && session.courseStartDate > today ? session.courseStartDate : today;
  const oneYear = new Date();
  oneYear.setFullYear(oneYear.getFullYear() + 1);
  const max = session.courseEndDate || formatDateYMD(oneYear);
  return { min, max };
}

/** Bấm vào một buổi trên lưới: dời sang ngày/giờ khác trong thời gian khóa học, hủy buổi, hoặc đưa về lịch gốc. */
function openSessionModal(session) {
  if (!session) return;
  sessionModalMode = 'edit';
  currentSession = session;
  const number = session.number ? `Buổi ${session.number}${session.totalSessions ? `/${session.totalSessions}` : ''} · ` : '';
  $('modalMoveTitle').textContent = session.extra ? 'Buổi học thêm' : 'Buổi học';
  $('moveSessionClass').textContent = `${session.classTitle || 'Lớp học'} · ${session.partnerName || 'Học viên'}`;
  $('moveSessionClassField').classList.add('hidden');
  $('moveSessionOriginal').textContent = `${number}${dayDateLabel(session.date)} · ${session.startTime} – ${session.endTime}`;
  $('moveSessionCurrent').textContent = session.moved ? `Đã dời từ lịch gốc ${dayDateLabel(session.originalDate)}.` : '';
  $('moveSessionCurrent').classList.toggle('hidden', !session.moved);
  $('moveSessionHint').textContent = 'Chỉ thay đổi riêng buổi này, các buổi khác giữ nguyên. Có thể dời sang bất kỳ ngày nào trong thời gian khóa học. Hủy buổi thì khóa tự bù thêm một buổi ở cuối.';
  $('moveSessionDateLabel').textContent = 'Dời sang ngày (trong thời gian khóa học)';
  $('moveSessionOpenClass').href = `/tutor/classrooms?space=${encodeURIComponent(session.spaceId)}`;
  $('moveSessionOpenClass').classList.remove('hidden');
  const range = courseDateRange(session);
  setDateRange(range.min, range.max, session.date);
  $('moveSessionStart').value = session.startTime;
  $('moveSessionEnd').value = session.endTime;
  $('btnCancelSession').classList.remove('hidden');
  $('btnRestoreSession').classList.toggle('hidden', !session.moved);
  $('btnSubmitMoveSessionText').textContent = 'Lưu thay đổi';
  setMoveError('');
  $('modalMoveSession').classList.remove('hidden');
}

/** Thêm một buổi riêng vào tuần đang xem (tính vào tổng số buổi của khóa). */
function openAddSessionModal() {
  if (!manageableClasses.length) {
    showToast('Bạn chưa có lớp học nào đang hoạt động để thêm buổi học.', 'error');
    return;
  }
  sessionModalMode = 'add';
  currentSession = null;
  $('modalMoveTitle').textContent = 'Thêm buổi học';
  $('moveSessionClass').textContent = 'Buổi thêm chỉ thuộc tuần này';
  $('moveSessionClassField').classList.remove('hidden');
  $('moveSessionClassSelect').innerHTML = manageableClasses.map(c =>
    `<option value="${escapeHtml(c.spaceId)}">${escapeHtml(c.title)} · ${escapeHtml(c.learnerName || 'Học viên')}</option>`).join('');
  $('moveSessionOriginal').textContent = `Tuần ${formatDateDMY(currentWeekMonday)} – ${formatDateDMY(new Date(currentWeekMonday.getTime() + 6 * 86400000))}`;
  $('moveSessionCurrent').classList.add('hidden');
  $('moveSessionHint').textContent = 'Buổi thêm được tính vào tổng số buổi của khóa, nên khóa sẽ kết thúc sớm hơn một buổi. Các tuần khác giữ nguyên.';
  $('moveSessionDateLabel').textContent = 'Ngày học (trong tuần này)';
  $('moveSessionOpenClass').classList.add('hidden');
  const today = formatDateYMD(new Date());
  const { from, to } = weekRange(currentWeekMonday);
  const min = today > from ? today : from;
  setDateRange(min, to, today >= from && today <= to ? today : from);
  $('moveSessionStart').value = '18:00';
  $('moveSessionEnd').value = '19:30';
  $('btnCancelSession').classList.add('hidden');
  $('btnRestoreSession').classList.add('hidden');
  $('btnSubmitMoveSessionText').textContent = 'Thêm buổi học';
  setMoveError('');
  $('modalMoveSession').classList.remove('hidden');
}

function closeMoveSessionModal() {
  $('modalMoveSession')?.classList.add('hidden');
  closeCancelConfirm();
  currentSession = null;
}

function setMoveBusy(busy) {
  ['btnSubmitMoveSession', 'btnRestoreSession', 'btnCancelSession'].forEach(id => { if ($(id)) $(id).disabled = busy; });
  if (busy) $('btnSubmitMoveSessionText').textContent = 'Đang lưu...';
  else $('btnSubmitMoveSessionText').textContent = sessionModalMode === 'add' ? 'Thêm buổi học' : 'Lưu thay đổi';
}

function sessionErrorMessage(err, fallback) {
  return err.fieldErrors?.session || err.userMessage || fallback;
}

async function submitMoveSession(event) {
  event.preventDefault();
  const newDate = $('moveSessionDate').value;
  const startTime = $('moveSessionStart').value;
  const endTime = $('moveSessionEnd').value;
  if (!newDate || !startTime || !endTime) {
    setMoveError('Vui lòng chọn ngày, giờ bắt đầu và giờ kết thúc cho buổi học.');
    return;
  }
  if (endTime <= startTime) {
    setMoveError('Giờ kết thúc phải sau giờ bắt đầu.');
    return;
  }
  const dateInput = $('moveSessionDate');
  if ((dateInput.min && newDate < dateInput.min) || (dateInput.max && newDate > dateInput.max)) {
    setMoveError($('moveSessionDateRange').textContent || 'Ngày đã chọn nằm ngoài khoảng cho phép.');
    return;
  }
  const body = { newDate, startTime, endTime };
  setMoveError('');
  setMoveBusy(true);
  try {
    let message;
    if (sessionModalMode === 'add') {
      const spaceId = $('moveSessionClassSelect').value;
      if (!spaceId) {
        setMoveError('Vui lòng chọn lớp học.');
        return;
      }
      await API.classrooms.addExtraSession(spaceId, body);
      message = 'Đã thêm buổi học vào tuần này.';
    } else if (currentSession?.extra) {
      await API.classrooms.moveExtraSession(currentSession.spaceId, currentSession.extraId, body);
      message = 'Đã dời buổi học thêm.';
    } else if (currentSession) {
      const s = currentSession;
      await API.classrooms.moveSession(s.spaceId, s.scheduleId, s.originalDate, body);
      message = 'Đã dời buổi học. Các tuần khác vẫn giữ lịch cũ.';
    } else {
      return;
    }
    closeMoveSessionModal();
    showToast(message);
    await loadWeekSessions();
  } catch (err) {
    setMoveError(sessionErrorMessage(err, 'Không thể lưu buổi học lúc này. Vui lòng thử lại.'));
  } finally {
    setMoveBusy(false);
  }
}

/** Bấm "Hủy buổi này": mở hộp xác nhận riêng, nêu rõ buổi nào và hệ quả với khóa học. */
function openCancelConfirm() {
  const s = currentSession;
  if (!s) return;
  const when = `${dayDateLabel(s.date)}, ${s.startTime}–${s.endTime}`;
  $('cancelSessionText').textContent = s.extra
    ? `Bạn chắc chắn hủy buổi học thêm ${when}? Buổi này sẽ bị xóa khỏi lịch, khóa học sẽ kết thúc muộn hơn một buổi.`
    : `Bạn chắc chắn hủy buổi ${when}? Chỉ buổi của tuần này bị hủy, các tuần khác giữ nguyên. Khóa học sẽ bù thêm một buổi ở cuối.`;
  $('cancelSessionError').textContent = '';
  $('cancelSessionError').classList.add('hidden');
  $('modalCancelSession').classList.remove('hidden');
}

function closeCancelConfirm() {
  $('modalCancelSession')?.classList.add('hidden');
}

async function cancelCurrentSession() {
  if (!currentSession) return;
  const button = $('btnConfirmCancelSession');
  button.disabled = true;
  button.textContent = 'Đang hủy...';
  try {
    const s = currentSession;
    if (s.extra) await API.classrooms.cancelExtraSession(s.spaceId, s.extraId);
    else await API.classrooms.cancelSession(s.spaceId, s.scheduleId, s.originalDate);
    closeMoveSessionModal();
    showToast(s.extra ? 'Đã hủy buổi học thêm.' : 'Đã hủy buổi học của tuần này. Khóa học tự bù thêm một buổi ở cuối.');
    await loadWeekSessions();
  } catch (err) {
    $('cancelSessionError').textContent = sessionErrorMessage(err, 'Không thể hủy buổi học lúc này.');
    $('cancelSessionError').classList.remove('hidden');
  } finally {
    button.disabled = false;
    button.textContent = 'Xác nhận hủy';
  }
}

async function restoreMovedSession() {
  if (!currentSession) return;
  setMoveError('');
  setMoveBusy(true);
  try {
    const s = currentSession;
    await API.classrooms.restoreSession(s.spaceId, s.scheduleId, s.originalDate);
    closeMoveSessionModal();
    showToast('Buổi học đã về lại lịch gốc.');
    await loadWeekSessions();
  } catch (err) {
    setMoveError(sessionErrorMessage(err, 'Không thể đưa buổi học về lịch gốc lúc này.'));
  } finally {
    setMoveBusy(false);
  }
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
    const [availData, bookingsData, classesData] = await Promise.allSettled([
      API.tutorSchedule.getMySchedule(),
      API.trialBookings.getMyRequests('ALL', 0, 100),
      API.classrooms.list().catch(() => [])
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

    // 3. Lớp gia sư đang quản lý được (FE-06) — để thêm buổi học riêng trong tuần
    if (classesData.status === 'fulfilled' && Array.isArray(classesData.value)) {
      manageableClasses = classesData.value.filter(c => c.canManage);
    }

    renderAvailabilityGrid();
    // 4. Buổi học chính thức của tuần đang xem (FE-06)
    await loadWeekSessions();
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
    loadWeekSessions();
  });

  $('btnNextWeek')?.addEventListener('click', () => {
    currentWeekMonday = new Date(currentWeekMonday.getTime() + 7 * 86400000);
    loadWeekSessions();
  });

  $('btnToday')?.addEventListener('click', () => {
    currentWeekMonday = getMondayOf(new Date());
    loadWeekSessions();
  });

  // Đóng modals
  document.querySelectorAll('.modal-add-close').forEach(btn => {
    btn.addEventListener('click', closeAddSlotModal);
  });

  // Hộp thoại dời một buổi học chính thức
  $('formMoveSession')?.addEventListener('submit', submitMoveSession);
  $('btnRestoreSession')?.addEventListener('click', restoreMovedSession);
  $('btnCancelSession')?.addEventListener('click', openCancelConfirm);
  $('btnConfirmCancelSession')?.addEventListener('click', cancelCurrentSession);
  document.querySelectorAll('.modal-cancel-session-close').forEach(btn => {
    btn.addEventListener('click', closeCancelConfirm);
  });
  $('btnOpenAddSession')?.addEventListener('click', openAddSessionModal);
  document.querySelectorAll('.modal-move-close').forEach(btn => {
    btn.addEventListener('click', closeMoveSessionModal);
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
