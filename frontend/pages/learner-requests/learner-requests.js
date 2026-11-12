/**
 * KizunaTutor - Yêu cầu của tôi (learner-requests.js)
 * Học viên theo dõi và xử lý các yêu cầu đã gửi tới gia sư:
 *  - Học thử: chờ phản hồi, đã chấp nhận, lịch sử; hủy / đổi lịch / xác nhận hoàn thành.
 *  - Kết nối trực tiếp (không học thử): xem trạng thái, lý do từ chối, hủy khi đang chờ.
 * Thời khóa biểu (buổi học đã xếp) nằm ở trang Lịch học của tôi (learner-connections).
 */

import { initLearnerShell } from '/components/learner-layout/learner-layout.js';
import { API, ApiError } from '/js/api.js';
// Sắp xếp theo từng nhóm: chờ phản hồi (buổi học gần nhất) → đã chấp nhận (buổi gần nhất) → lịch sử (mới nhất)
import { sortTrials, sortDirect } from '/js/request-sorting.js';

initLearnerShell({
  activeMenu: 'my-requests',
  pageTitle: 'Yêu cầu của tôi',
  badge: 'Gia sư & lớp học'
});

const $ = (id) => document.getElementById(id);

let bookingsList = [];
let currentFilter = 'ALL';
let activeBooking = null;
let activeCompleteBooking = null;

// Trạng thái học thử theo nhóm tab
const ACCEPTED_STATUSES = ['CONFIRMED', 'RESCHEDULED'];
const HISTORY_STATUSES = ['COMPLETED', 'REJECTED', 'DECLINED', 'CANCELLED', 'EXPIRED'];

function setVisible(el, visible) {
  if (!el) return;
  el.classList.toggle('hidden', !visible);
}

// ---------------------------------------------------------------- Phân trang (7 mục mỗi trang)
const PAGE_SIZE = 7;
let bookingsPage = 0;
let directPage = 0;

/** Cắt danh sách theo trang; trả về { items, page } với page đã được giới hạn trong khoảng hợp lệ. */
function pageSlice(list, page) {
  const totalPages = Math.max(1, Math.ceil(list.length / PAGE_SIZE));
  const p = Math.min(Math.max(0, page), totalPages - 1);
  return { items: list.slice(p * PAGE_SIZE, (p + 1) * PAGE_SIZE), page: p };
}

/** Vẽ thanh phân trang: « Trước · 1 2 3 … · Sau » kèm "Hiển thị 1–8 / 20". Ẩn nếu chỉ có 1 trang. */
function renderPager(containerId, total, page, onChange) {
  const el = $(containerId);
  if (!el) return;
  const totalPages = Math.ceil(total / PAGE_SIZE);
  if (totalPages <= 1) {
    el.classList.add('hidden');
    el.innerHTML = '';
    return;
  }
  el.classList.remove('hidden');

  // Số trang hiển thị: trang đầu, trang cuối và 1 trang mỗi bên trang hiện tại
  const pages = [];
  for (let i = 0; i < totalPages; i++) {
    if (i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 1) pages.push(i);
    else if (pages[pages.length - 1] !== '…') pages.push('…');
  }
  const btnBase = 'min-w-9 h-9 px-2.5 rounded-xl text-xs font-semibold inline-flex items-center justify-center transition-all';
  const from = page * PAGE_SIZE + 1;
  const to = Math.min(total, (page + 1) * PAGE_SIZE);

  el.innerHTML = `
    <div class="flex flex-col sm:flex-row items-center justify-between gap-3 pt-2">
      <p class="text-xs text-[#64748B]">Hiển thị <strong class="text-[#0F172A]">${from}–${to}</strong> / ${total} yêu cầu</p>
      <nav class="flex items-center gap-1.5" aria-label="Phân trang">
        <button type="button" data-page="${page - 1}" ${page === 0 ? 'disabled' : ''}
          class="${btnBase} border border-[#E2E8F0] bg-white text-[#0F172A] hover:bg-[#F1F5F9] disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer" aria-label="Trang trước">
          <span class="material-symbols-outlined text-[18px]">chevron_left</span>
        </button>
        ${pages.map(p => p === '…'
          ? `<span class="px-1 text-xs text-[#94A3B8]">…</span>`
          : `<button type="button" data-page="${p}" ${p === page ? 'aria-current="page"' : ''}
              class="${btnBase} cursor-pointer ${p === page
                ? 'bg-[#006194] text-white shadow-xs'
                : 'border border-[#E2E8F0] bg-white text-[#0F172A] hover:bg-[#F1F5F9]'}">${p + 1}</button>`).join('')}
        <button type="button" data-page="${page + 1}" ${page >= totalPages - 1 ? 'disabled' : ''}
          class="${btnBase} border border-[#E2E8F0] bg-white text-[#0F172A] hover:bg-[#F1F5F9] disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer" aria-label="Trang sau">
          <span class="material-symbols-outlined text-[18px]">chevron_right</span>
        </button>
      </nav>
    </div>`;
  el.querySelectorAll('button[data-page]').forEach(btn => btn.addEventListener('click', () => {
    if (btn.disabled) return;
    onChange(Number(btn.dataset.page));
  }));
}

/** Chuyển trang rồi cuộn lên đầu danh sách. */
function scrollToList(listId) {
  const el = $(listId);
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

/** " • 8 buổi (17/11 – 11/12/2026)" cho yêu cầu kết nối có buổi cụ thể; rỗng với yêu cầu cũ. */
function courseRangeText(r) {
  if (!r || !r.totalSessions || !r.startDate || !r.endDate) return '';
  const [, sm, sd] = String(r.startDate).split('-');
  const [ey, em, ed] = String(r.endDate).split('-');
  return ` • ${r.totalSessions} buổi (${sd}/${sm} – ${ed}/${em}/${ey})`;
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
  const accepted = bookingsList.filter(b => ACCEPTED_STATUSES.includes(b.bookingStatus)).length;
  const history = bookingsList.filter(b => HISTORY_STATUSES.includes(b.bookingStatus)).length;

  if ($('tabCountAll')) $('tabCountAll').textContent = total;
  if ($('tabCountPending')) $('tabCountPending').textContent = pending;
  if ($('tabCountConfirmed')) $('tabCountConfirmed').textContent = accepted;
  if ($('tabCountHistory')) $('tabCountHistory').textContent = history;
  if ($('typeCountTrial')) {
    $('typeCountTrial').textContent = pending;
    $('typeCountTrial').classList.toggle('hidden', pending === 0);
  }
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
    filtered = bookingsList.filter(b => ACCEPTED_STATUSES.includes(b.bookingStatus));
  } else if (currentFilter === 'HISTORY') {
    filtered = bookingsList.filter(b => HISTORY_STATUSES.includes(b.bookingStatus));
  }

  if (filtered.length === 0) {
    setVisible(listEl, false);
    setVisible(emptyEl, true);
    renderPager('bookingsPager', 0, 0, () => {});
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

  const paged = pageSlice(filtered, bookingsPage);
  bookingsPage = paged.page;
  renderPager('bookingsPager', filtered.length, bookingsPage, (p) => {
    bookingsPage = p;
    renderBookings();
    scrollToList('bookingsList');
  });

  listEl.innerHTML = paged.items.map(item => {
    const isPending = item.bookingStatus === 'PENDING';
    const isConfirmed = item.bookingStatus === 'CONFIRMED';
    const isRescheduled = item.bookingStatus === 'RESCHEDULED';
    const isRejected = item.bookingStatus === 'REJECTED' || item.bookingStatus === 'DECLINED';
    const isCancelled = item.bookingStatus === 'CANCELLED';
    const isCompleted = item.bookingStatus === 'COMPLETED';
    const isExpired = item.bookingStatus === 'EXPIRED';

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
    } else if (isExpired) {
      badgeHtml = `<span class="px-2.5 py-1 rounded-full text-[11px] font-bold bg-slate-100 text-slate-600 flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">timer_off</span>Đã hết hạn</span>`;
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
    } else if (isExpired) {
      actionsHtml = `
        <div class="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-slate-100 bg-slate-50 p-3 rounded-xl border border-slate-200">
          <div class="flex items-center gap-2 text-xs text-slate-600 font-medium">
            <span class="material-symbols-outlined text-[18px]">info</span>
            <span>Gia sư chưa phản hồi kịp nên yêu cầu đã hết hạn. Bạn có thể gửi lại yêu cầu mới hoặc chọn gia sư khác.</span>
          </div>
          <a href="/pages/tutor-discovery/" class="px-3.5 py-1.5 rounded-lg bg-white border border-slate-300 hover:bg-slate-100 text-slate-700 text-xs font-semibold inline-flex items-center gap-1 transition-all no-underline">
            <span class="material-symbols-outlined text-[15px]">search</span>
            <span>Tìm gia sư</span>
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
    const res = await API.trialBookings.getMyRequests('ALL', 0, 100);
    bookingsList = sortTrials(res?.items || res?.content || res?.data?.items || res?.data?.content || (Array.isArray(res) ? res : []), 'learner');
    updateCounts();
    renderBookings();
  } catch (err) {
    console.error('Lỗi tải danh sách yêu cầu học thử:', err);
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
  // Tabs lọc trạng thái
  const tabBtns = document.querySelectorAll('#statusTabs .tab-btn');
  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      tabBtns.forEach(b => {
        b.className = 'tab-btn px-4 py-2 rounded-xl text-[#64748B] hover:bg-[#F1F5F9] hover:text-[#0F172A] transition-all flex items-center gap-1.5 cursor-pointer';
      });
      btn.className = 'tab-btn px-4 py-2 rounded-xl bg-[#006194] text-white shadow-xs transition-all flex items-center gap-1.5 cursor-pointer';

      currentFilter = btn.dataset.status;
      bookingsPage = 0;
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
      showToast('Đã đổi lịch học thử. Yêu cầu đang chờ gia sư xác nhận lại giờ mới.');
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


// ====================================================================
// Kết nối trực tiếp (không học thử): xem trạng thái, lý do từ chối, hủy khi đang chờ
// ====================================================================
let directRequests = [];
let directFilter = 'ALL';
const DIRECT_HISTORY = ['DECLINED', 'CANCELLED', 'EXPIRED'];

function directMatches(r) {
  if (directFilter === 'ALL') return true;
  if (directFilter === 'HISTORY') return DIRECT_HISTORY.includes(r.status);
  return r.status === directFilter;
}

function updateDirectCounts() {
  const count = (fn) => directRequests.filter(fn).length;
  const pending = count(r => r.status === 'PENDING');
  if ($('directCountAll')) $('directCountAll').textContent = directRequests.length;
  if ($('directCountPending')) $('directCountPending').textContent = pending;
  if ($('directCountAccepted')) $('directCountAccepted').textContent = count(r => r.status === 'ACCEPTED');
  if ($('directCountHistory')) $('directCountHistory').textContent = count(r => DIRECT_HISTORY.includes(r.status));
  if ($('typeCountDirect')) {
    $('typeCountDirect').textContent = pending;
    $('typeCountDirect').classList.toggle('hidden', pending === 0);
  }
}

function renderDirect() {
  const listEl = $('directList');
  if (!listEl) return;
  const filtered = directRequests.filter(directMatches);
  const paged = pageSlice(filtered, directPage);
  directPage = paged.page;
  renderPager('directPager', filtered.length, directPage, (p) => {
    directPage = p;
    renderDirect();
    scrollToList('directList');
  });

  if (!filtered.length) {
    const msg = directFilter === 'PENDING' ? 'Bạn không có yêu cầu kết nối nào đang chờ gia sư phản hồi.'
      : directFilter === 'ACCEPTED' ? 'Chưa có gia sư nào đồng ý kết nối trực tiếp với bạn.'
      : 'Chưa có yêu cầu kết nối trực tiếp nào trong mục này.';
    listEl.innerHTML = `
      <div class="bg-white border border-[#E2E8F0] rounded-2xl p-10 text-center shadow-xs space-y-3">
        <div class="w-14 h-14 rounded-2xl bg-[#F8FAFC] text-[#006194] mx-auto flex items-center justify-center">
          <span class="material-symbols-outlined text-[32px]">handshake</span>
        </div>
        <p class="text-xs text-[#64748B] max-w-md mx-auto">${msg}</p>
        <a href="/pages/tutor-discovery/" class="px-5 py-2.5 rounded-xl bg-[#006194] hover:bg-[#004b73] text-white text-xs font-bold inline-flex items-center gap-2 shadow-xs transition-all no-underline">
          <span class="material-symbols-outlined text-[16px]">search</span><span>Tìm gia sư</span>
        </a>
      </div>`;
    return;
  }

  listEl.innerHTML = paged.items.map(r => {
    const tutorName = escapeHtml(r.partnerName || 'Gia sư');
    const avatarSrc = r.partnerAvatarUrl || 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=120&q=80';
    const badgeCls = r.status === 'PENDING' ? 'bg-amber-100 text-amber-800'
      : r.status === 'ACCEPTED' ? 'bg-emerald-100 text-emerald-800'
      : r.status === 'DECLINED' ? 'bg-rose-100 text-rose-800' : 'bg-slate-100 text-slate-600';
    const slots = (r.slots || []).map(s => `
      <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-sky-50 border border-sky-100 text-[11px] font-semibold text-[#006194]">
        <span class="material-symbols-outlined text-[14px]">event_repeat</span>${escapeHtml(`${s.dayName} ${s.startTime}–${s.endTime}`)}
      </span>`).join('');

    let footer = '';
    if (r.status === 'PENDING') {
      footer = `
        <div class="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-slate-100">
          <div class="text-xs text-slate-500 italic flex items-center gap-1">
            <span class="material-symbols-outlined text-[16px] text-amber-600">info</span>
            <span>Yêu cầu đang chờ gia sư phản hồi. Bạn có thể hủy nếu đổi ý.</span>
          </div>
          <button type="button" class="btn-cancel-direct px-3.5 py-1.5 rounded-xl bg-slate-100 hover:bg-rose-50 hover:text-rose-700 text-slate-700 text-xs font-semibold inline-flex items-center gap-1.5 transition-all cursor-pointer" data-id="${r.requestId}">
            <span class="material-symbols-outlined text-[16px]">close</span><span>Hủy yêu cầu</span>
          </button>
        </div>`;
    } else if (r.status === 'ACCEPTED') {
      footer = `
        <div class="flex flex-wrap items-center justify-between gap-3 bg-emerald-50/60 p-3 rounded-xl border border-emerald-200">
          <div class="text-xs text-emerald-800 font-medium">
            Gia sư đã đồng ý kết nối. Lớp học chính thức sẽ xuất hiện trong Lịch học khi gia sư tạo lớp.
            ${r.responseMessage ? `<span class="block italic mt-1">"${escapeHtml(r.responseMessage)}"</span>` : ''}
          </div>
          <a href="/learner/connections" class="px-4 py-1.5 rounded-lg bg-[#006194] hover:bg-[#004b73] text-white text-xs font-semibold inline-flex items-center gap-1.5 shadow-xs no-underline">
            <span class="material-symbols-outlined text-[15px]">calendar_month</span><span>Xem lịch học</span>
          </a>
        </div>`;
    } else if (r.responseMessage) {
      const declined = r.status === 'DECLINED';
      footer = `
        <div class="text-xs ${declined ? 'text-rose-700 bg-rose-50/60 border-rose-200' : 'text-slate-600 bg-slate-50 border-slate-200'} p-3 rounded-xl border">
          ${declined ? 'Lý do từ chối: ' : ''}${escapeHtml(r.responseMessage)}
        </div>`;
    }

    return `
      <div class="bg-white rounded-2xl p-5 sm:p-6 shadow-xs border border-[#E2E8F0] hover:border-[#006194]/40 transition-all space-y-4">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div class="flex items-center gap-3">
            <img src="${avatarSrc}" alt="${tutorName}" class="w-12 h-12 rounded-2xl object-cover ring-2 ring-slate-100 shrink-0">
            <div>
              <div class="flex items-center gap-2">
                <h3 class="text-base font-bold text-[#0F172A]">${tutorName}</h3>
                <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-sky-50 text-[#006194]">Kết nối trực tiếp</span>
              </div>
              <p class="text-xs text-[#64748B] mt-0.5">${(r.slots || []).length} ca mỗi tuần${courseRangeText(r)} • Không qua học thử</p>
            </div>
          </div>
          <span class="self-start sm:self-center px-2.5 py-1 rounded-full text-[11px] font-bold ${badgeCls}">${escapeHtml(r.statusLabel || '')}</span>
        </div>
        <div class="flex flex-wrap gap-2">${slots}</div>
        ${r.initialMessage ? `<div class="text-xs text-[#64748B] bg-[#F8FAFC] p-3 rounded-xl border border-[#E2E8F0] italic">"Lời nhắn: ${escapeHtml(r.initialMessage)}"</div>` : ''}
        ${footer}
      </div>`;
  }).join('');

  listEl.querySelectorAll('.btn-cancel-direct').forEach(btn => {
    btn.addEventListener('click', async () => {
      if (!window.confirm('Bạn chắc chắn muốn hủy yêu cầu kết nối này?')) return;
      btn.disabled = true;
      try {
        await API.connectionRequests.cancel(btn.dataset.id);
        showToast('Đã hủy yêu cầu kết nối.');
        await loadDirect();
      } catch (err) {
        showToast(err.message || 'Không thể hủy yêu cầu lúc này. Vui lòng thử lại.', 'error');
        btn.disabled = false;
      }
    });
  });
}

async function loadDirect() {
  try {
    const res = await API.connectionRequests.getMyRequests('ALL', 0, 100);
    directRequests = sortDirect(res?.items || [], 'learner');
  } catch (err) {
    console.error('Lỗi tải yêu cầu kết nối trực tiếp:', err);
    directRequests = [];
  }
  updateDirectCounts();
  renderDirect();
}

// ====================================================================
// Chuyển giữa 2 loại yêu cầu (Học thử / Kết nối trực tiếp)
// ====================================================================
const TAB_ACTIVE = 'tab-btn px-4 py-2 rounded-xl bg-[#006194] text-white shadow-xs transition-all flex items-center gap-1.5 cursor-pointer';
const TAB_IDLE = 'tab-btn px-4 py-2 rounded-xl text-[#64748B] hover:bg-[#F1F5F9] hover:text-[#0F172A] transition-all flex items-center gap-1.5 cursor-pointer';
const TYPE_ACTIVE = 'type-btn px-4 py-2.5 rounded-xl bg-white text-[#0F172A] shadow-xs font-bold text-sm flex items-center gap-2 cursor-pointer';
const TYPE_IDLE = 'type-btn px-4 py-2.5 rounded-xl text-[#64748B] hover:text-[#0F172A] font-semibold text-sm flex items-center gap-2 cursor-pointer';

function switchType(type) {
  const isDirect = type === 'direct';
  document.querySelectorAll('#typeTabs .type-btn').forEach(b => {
    b.className = b.dataset.type === type ? TYPE_ACTIVE : TYPE_IDLE;
  });
  setVisible($('trialPanel'), !isDirect);
  setVisible($('directPanel'), isDirect);
  const url = new URL(window.location.href);
  url.searchParams.set('tab', isDirect ? 'direct' : 'trial');
  window.history.replaceState(null, '', url);
}

function initDirectEvents() {
  document.querySelectorAll('#typeTabs .type-btn').forEach(btn =>
    btn.addEventListener('click', () => switchType(btn.dataset.type)));

  const tabBtns = document.querySelectorAll('#directStatusTabs .tab-btn');
  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      tabBtns.forEach(b => { b.className = TAB_IDLE; });
      btn.className = TAB_ACTIVE;
      directFilter = btn.dataset.status;
      directPage = 0;
      renderDirect();
    });
  });
}

document.addEventListener('DOMContentLoaded', () => {
  initEvents();
  initDirectEvents();
  $('btnRefresh')?.addEventListener('click', loadDirect);
  const tab = new URLSearchParams(window.location.search).get('tab');
  switchType(tab === 'direct' ? 'direct' : 'trial');
  loadDirect();
  loadBookings();
});
