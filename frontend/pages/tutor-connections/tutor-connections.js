/**
 * KizunaTutor - Quản lý Yêu cầu kết nối dành cho Gia sư (tutor-connections.js)
 * Phân hệ Gia sư - Sử dụng layout Stitch Tutor Dashboard
 */

import { API, ApiError } from '/js/api.js';
import { mountTutorLayout, setTutorIdentity } from '/components/tutor-layout/tutor-layout.js';

const $ = (id) => document.getElementById(id);

let bookingsList = [];
let currentFilter = 'ALL';
let activeBooking = null;

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
 * Cập nhật số liệu thống kê trên các thẻ KPI và nhãn của từng Tab
 */
function updateCounts() {
  const total = bookingsList.length;
  const pending = bookingsList.filter(b => b.bookingStatus === 'PENDING').length;
  const confirmed = bookingsList.filter(b => b.bookingStatus === 'CONFIRMED').length;
  const history = bookingsList.filter(b => ['COMPLETED', 'REJECTED', 'CANCELLED'].includes(b.bookingStatus)).length;

  if ($('statPending')) $('statPending').textContent = pending;
  if ($('statConfirmed')) $('statConfirmed').textContent = confirmed;
  if ($('statTotal')) $('statTotal').textContent = total;

  if ($('tabCountAll')) $('tabCountAll').textContent = total;
  if ($('tabCountPending')) $('tabCountPending').textContent = pending;
  if ($('tabCountConfirmed')) $('tabCountConfirmed').textContent = confirmed;
  if ($('tabCountHistory')) $('tabCountHistory').textContent = history;
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
    filtered = bookingsList.filter(b => b.bookingStatus === 'CONFIRMED');
  } else if (currentFilter === 'HISTORY') {
    filtered = bookingsList.filter(b => ['COMPLETED', 'REJECTED', 'CANCELLED'].includes(b.bookingStatus));
  }

  if (filtered.length === 0) {
    setVisible(listEl, false);
    setVisible(emptyEl, true);
    const msgEl = $('emptyMessage');
    if (msgEl) {
      if (currentFilter === 'PENDING') {
        msgEl.textContent = 'Bạn không có yêu cầu học thử nào đang chờ phản hồi.';
      } else if (currentFilter === 'CONFIRMED') {
        msgEl.textContent = 'Chưa có buổi học thử nào được xác nhận.';
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
    const isRejected = item.bookingStatus === 'REJECTED';
    const isCancelled = item.bookingStatus === 'CANCELLED';
    const isCompleted = item.bookingStatus === 'COMPLETED';

    const avatarSrc = item.partnerAvatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=120&q=80';
    const partnerName = escapeHtml(item.partnerName || 'Học viên');
    const timeFormatted = formatBookingTime(item.scheduledStartTime, item.durationMinutes);
    const isOnline = item.learningMode === 'ONLINE';

    // Badge trạng thái
    let badgeHtml = '';
    if (isPending) {
      badgeHtml = `<span class="px-2.5 py-1 rounded-full text-caption font-bold bg-amber-100 text-amber-800 flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">hourglass_top</span>Chờ phản hồi</span>`;
    } else if (isConfirmed) {
      badgeHtml = `<span class="px-2.5 py-1 rounded-full text-caption font-bold bg-emerald-100 text-emerald-800 flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">check_circle</span>Đã xác nhận</span>`;
    } else if (isRejected) {
      badgeHtml = `<span class="px-2.5 py-1 rounded-full text-caption font-bold bg-error-container text-error flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">cancel</span>Đã từ chối</span>`;
    } else if (isCancelled) {
      badgeHtml = `<span class="px-2.5 py-1 rounded-full text-caption font-bold bg-surface-container text-on-surface-variant flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">close</span>Đã hủy</span>`;
    } else if (isCompleted) {
      badgeHtml = `<span class="px-2.5 py-1 rounded-full text-caption font-bold bg-primary-fixed text-primary flex items-center gap-1"><span class="material-symbols-outlined text-[14px]">task_alt</span>Đã hoàn thành</span>`;
    }

    // Các nút hành động
    let actionsHtml = '';
    if (isPending) {
      actionsHtml = `
        <div class="flex flex-wrap items-center gap-2 pt-3 border-t border-outline-variant/40">
          <button type="button" class="btn-open-accept px-4 py-2 rounded-xl bg-primary hover:bg-primary-container text-on-primary text-label-md font-semibold inline-flex items-center gap-1.5 shadow-sm transition-all" data-id="${item.bookingId}">
            <span class="material-symbols-outlined text-[18px]">check_circle</span>
            <span>Chấp nhận</span>
          </button>
          <button type="button" class="btn-open-decline px-4 py-2 rounded-xl bg-surface-container hover:bg-error-container hover:text-error text-on-surface text-label-md font-semibold inline-flex items-center gap-1.5 transition-all" data-id="${item.bookingId}">
            <span class="material-symbols-outlined text-[18px]">cancel</span>
            <span>Từ chối</span>
          </button>
        </div>
      `;
    } else if (isConfirmed) {
      actionsHtml = `
        <div class="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-outline-variant/40 bg-emerald-50/50 p-3 rounded-xl border border-emerald-200/60">
          ${item.meetingLink ? `
            <div class="flex items-center gap-2 min-w-0">
              <span class="material-symbols-outlined text-emerald-700 text-[20px] shrink-0">video_camera_front</span>
              <div class="min-w-0">
                <p class="text-caption text-emerald-800 font-bold">Phòng học Google Meet đã sẵn sàng:</p>
                <a href="${item.meetingLink}" target="_blank" rel="noopener noreferrer" class="text-body-sm text-emerald-700 hover:text-emerald-900 font-medium underline truncate block">
                  ${escapeHtml(item.meetingLink)}
                </a>
              </div>
            </div>
          ` : `
            <div class="flex items-center gap-2 text-body-sm text-emerald-800 font-medium">
              <span class="material-symbols-outlined text-emerald-700 text-[20px] shrink-0">verified</span>
              <span>Buổi học thử đã được xác nhận.</span>
            </div>
          `}
          <div class="flex items-center gap-2 shrink-0">
            ${item.meetingLink ? `
              <button type="button" class="btn-copy-link px-3 py-1.5 rounded-lg bg-white border border-emerald-300 text-emerald-800 hover:bg-emerald-100 text-label-sm font-semibold inline-flex items-center gap-1 transition-all" data-link="${escapeHtml(item.meetingLink)}">
                <span class="material-symbols-outlined text-[16px]">content_copy</span>
                <span>Sao chép</span>
              </button>
              <a href="${item.meetingLink}" target="_blank" rel="noopener noreferrer" class="px-4 py-1.5 rounded-lg bg-emerald-700 hover:bg-emerald-800 text-white text-label-sm font-semibold inline-flex items-center gap-1 shadow-xs transition-all no-underline">
                <span class="material-symbols-outlined text-[16px]">open_in_new</span>
                <span>Vào lớp</span>
              </a>
            ` : ''}
            <button type="button" class="btn-complete-booking px-3.5 py-1.5 rounded-lg bg-primary hover:bg-primary-container text-on-primary text-label-sm font-semibold inline-flex items-center gap-1 transition-all cursor-pointer" data-id="${item.bookingId}" title="Xác nhận hoàn thành buổi học thử">
              <span class="material-symbols-outlined text-[16px]">task_alt</span>
              <span>Hoàn thành</span>
            </button>
          </div>
        </div>
      `;
    } else if (isCompleted) {
      actionsHtml = `
        <div class="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-outline-variant/40 bg-sky-50/50 p-3 rounded-xl border border-sky-200/60">
          <div class="flex items-center gap-2 text-body-sm text-sky-900 font-medium">
            <span class="material-symbols-outlined text-sky-700 text-[20px] shrink-0">task_alt</span>
            <span>Buổi học thử đã hoàn thành! Bạn có thể trao đổi và tạo lớp học 1-1 chính thức cho học viên này.</span>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <a href="/pages/tutor-classrooms/" class="px-4 py-2 rounded-xl bg-primary hover:bg-primary-container text-on-primary text-label-md font-semibold inline-flex items-center gap-1.5 shadow-sm transition-all no-underline">
              <span class="material-symbols-outlined text-[18px]">add_circle</span>
              <span>Mở lớp học 1-1</span>
            </a>
          </div>
        </div>
      `;
    } else if (isRejected) {
      actionsHtml = `
        <div class="pt-2 text-caption text-error flex items-center gap-1">
          <span class="material-symbols-outlined text-[16px]">info</span>
          <span>Yêu cầu học thử này đã được từ chối.</span>
        </div>
      `;
    }

    return `
      <div class="bg-surface-container-lowest rounded-2xl p-5 sm:p-6 shadow-sm border border-outline-variant/40 hover:border-primary/40 transition-all space-y-4">
        <!-- Hàng đầu: Thông tin học viên + Trạng thái -->
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div class="flex items-center gap-3">
            <img src="${avatarSrc}" alt="${partnerName}" class="w-12 h-12 rounded-2xl object-cover ring-2 ring-surface-container shrink-0">
            <div>
              <div class="flex items-center gap-2">
                <h3 class="text-title-md font-bold text-on-surface">${partnerName}</h3>
                <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-primary-fixed text-primary">Học viên</span>
              </div>
              <p class="text-caption text-on-surface-variant mt-0.5">${item.durationMinutes || 50} phút • Học thử miễn phí 0đ</p>
            </div>
          </div>
          <div class="self-start sm:self-center">
            ${badgeHtml}
          </div>
        </div>

        <!-- Khối chi tiết lịch hẹn -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-3 p-3.5 rounded-xl bg-surface-container-low border border-outline-variant/30 text-body-sm">
          <div class="flex items-center gap-2 text-on-surface font-medium">
            <span class="material-symbols-outlined text-[20px] text-primary shrink-0">calendar_today</span>
            <span>${timeFormatted}</span>
          </div>
          <div class="flex items-center gap-2 text-on-surface-variant">
            <span class="material-symbols-outlined text-[20px] text-primary shrink-0">${isOnline ? 'laptop_chromebook' : 'pin_drop'}</span>
            <span>${isOnline ? 'Trực tuyến (Google Meet)' : `Trực tiếp tại ${escapeHtml(item.locationDetails || 'địa chỉ hẹn')}`}</span>
          </div>

          ${item.currentLevel ? `
            <div class="flex items-center gap-2 text-on-surface-variant">
              <span class="material-symbols-outlined text-[20px] text-amber-700 shrink-0">school</span>
              <span>Trình độ hiện tại: <strong class="text-on-surface font-semibold">${escapeHtml(item.currentLevel)}</strong></span>
            </div>
          ` : ''}

          ${item.learningGoals ? `
            <div class="flex items-center gap-2 text-on-surface-variant">
              <span class="material-symbols-outlined text-[20px] text-primary shrink-0">flag</span>
              <span>Mục tiêu: <span class="text-on-surface font-medium">${escapeHtml(item.learningGoals)}</span></span>
            </div>
          ` : ''}
        </div>

        <!-- Lời nhắn ban đầu của học viên -->
        ${(item.initialMessage && item.initialMessage.trim()) ? `
          <div class="text-body-sm text-on-surface-variant bg-surface-container-low/60 p-3 rounded-xl border border-outline-variant/30 italic">
            "${escapeHtml(item.initialMessage)}"
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
  // Nút mở modal Chấp nhận
  document.querySelectorAll('.btn-open-accept').forEach(btn => {
    btn.addEventListener('click', () => {
      const id = btn.dataset.id;
      const booking = bookingsList.find(b => String(b.bookingId) === String(id));
      if (!booking) return;
      openAcceptModal(booking);
    });
  });

  // Nút mở modal Từ chối
  document.querySelectorAll('.btn-open-decline').forEach(btn => {
    btn.addEventListener('click', () => {
      const id = btn.dataset.id;
      const booking = bookingsList.find(b => String(b.bookingId) === String(id));
      if (!booking) return;
      openDeclineModal(booking);
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

  // Nút xác nhận hoàn thành buổi học
  document.querySelectorAll('.btn-complete-booking').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.dataset.id;
      if (!id) return;
      if (!confirm('Xác nhận buổi học thử này đã hoàn thành?')) return;
      btn.disabled = true;
      try {
        await API.trialBookings.complete(id);
        showToast('Đã xác nhận hoàn thành buổi học thử!');
        await loadBookings();
      } catch (err) {
        showToast(err.message || 'Không thể hoàn thành buổi học', 'error');
        btn.disabled = false;
      }
    });
  });
}

/**
 * Mở modal Chấp nhận
 */
function openAcceptModal(booking) {
  activeBooking = booking;
  $('acceptStudentName').textContent = booking.partnerName || 'Học viên';
  $('acceptTimeStr').textContent = formatBookingTime(booking.scheduledStartTime, booking.durationMinutes);

  const meetInput = $('acceptMeetingLink');
  meetInput.value = booking.meetingLink || '';
  meetInput.classList.remove('has-error');
  $('acceptMeetingLinkError').classList.add('hidden');
  $('acceptNote').value = '';

  $('modalAcceptBooking').classList.remove('hidden');
  meetInput.focus();
}

/**
 * Mở modal Từ chối
 */
function openDeclineModal(booking) {
  activeBooking = booking;
  $('declineStudentName').textContent = booking.partnerName || 'Học viên';
  $('declineTimeStr').textContent = formatBookingTime(booking.scheduledStartTime, booking.durationMinutes);

  $('declineReasonSelect').value = 'Trùng lịch với ca dạy hoặc công việc khác';
  $('declineCustomReason').value = '';
  $('declineCustomReason').classList.add('hidden');

  $('modalDeclineBooking').classList.remove('hidden');
}

/**
 * Đóng tất cả modal
 */
function closeModals() {
  $('modalAcceptBooking').classList.add('hidden');
  $('modalDeclineBooking').classList.add('hidden');
  activeBooking = null;
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
    renderBookings();
  } catch (err) {
    console.error('Lỗi tải danh sách yêu cầu kết nối:', err);
    $('errorMessage').textContent = err instanceof ApiError ? err.message : 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.';
    setVisible($('errorState'), true);
  } finally {
    setVisible($('loadingState'), false);
  }
}

/**
 * Khởi tạo sự kiện chung trên trang
 */
function initEvents() {
  // Tabs lọc trạng thái
  const tabBtns = document.querySelectorAll('#statusTabs .tab-btn');
  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      tabBtns.forEach(b => {
        b.className = 'tab-btn px-4 py-2 rounded-xl text-on-surface-variant hover:bg-surface-container hover:text-on-surface transition-all flex items-center gap-1.5';
      });
      btn.className = 'tab-btn px-4 py-2 rounded-xl bg-primary text-on-primary shadow-xs transition-all flex items-center gap-1.5';

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

  // Tạo link Meet mẫu
  $('btnQuickSampleMeet')?.addEventListener('click', () => {
    const randPart = () => Math.random().toString(36).substring(2, 6);
    const sampleLink = `https://meet.google.com/kzt-${randPart()}-${randPart()}`;
    $('acceptMeetingLink').value = sampleLink;
    $('acceptMeetingLink').classList.remove('has-error');
    $('acceptMeetingLinkError').classList.add('hidden');
  });

  // Đổi lựa chọn lý do từ chối
  $('declineReasonSelect')?.addEventListener('change', (e) => {
    const isCustom = e.target.value === 'Khác';
    $('declineCustomReason').classList.toggle('hidden', !isCustom);
    if (isCustom) $('declineCustomReason').focus();
  });

  // Gửi xác nhận Chấp nhận (ACCEPT)
  $('btnSubmitAccept')?.addEventListener('click', async () => {
    if (!activeBooking) return;

    const meetInput = $('acceptMeetingLink');
    const meetingLink = meetInput.value.trim();
    const note = $('acceptNote').value.trim();

    if (!meetingLink) {
      meetInput.classList.add('has-error');
      const errEl = $('acceptMeetingLinkError');
      errEl.textContent = 'Vui lòng nhập đường link Google Meet để hai bên gặp gỡ.';
      errEl.classList.remove('hidden');
      meetInput.focus();
      return;
    }

    const btn = $('btnSubmitAccept');
    const btnText = $('btnSubmitAcceptText');
    btn.disabled = true;
    btnText.textContent = 'Đang xử lý...';

    try {
      await API.trialBookings.respond(activeBooking.bookingId, {
        action: 'ACCEPT',
        meetingLink: meetingLink,
        note: note || null
      });

      closeModals();
      showToast('Đã chấp nhận lịch học thử thành công! Link Google Meet đã được gửi cho học viên.');
      await loadBookings();
    } catch (err) {
      showToast(err.message || 'Không thể chấp nhận yêu cầu lúc này', 'error');
    } finally {
      btn.disabled = false;
      btnText.textContent = 'Xác nhận chấp nhận';
    }
  });

  // Gửi xác nhận Từ chối (DECLINE)
  $('btnSubmitDecline')?.addEventListener('click', async () => {
    if (!activeBooking) return;

    const select = $('declineReasonSelect');
    let reason = select.value;
    if (reason === 'Khác') {
      const customVal = $('declineCustomReason').value.trim();
      reason = customVal || 'Bận việc đột xuất không thể sắp xếp ca học';
    }

    const btn = $('btnSubmitDecline');
    const btnText = $('btnSubmitDeclineText');
    btn.disabled = true;
    btnText.textContent = 'Đang xử lý...';

    try {
      await API.trialBookings.respond(activeBooking.bookingId, {
        action: 'DECLINE',
        declineReason: reason
      });

      closeModals();
      showToast('Đã từ chối yêu cầu học thử.');
      await loadBookings();
    } catch (err) {
      showToast(err.message || 'Không thể từ chối yêu cầu lúc này', 'error');
    } finally {
      btn.disabled = false;
      btnText.textContent = 'Xác nhận từ chối';
    }
  });
}

// Khởi chạy khi tài liệu sẵn sàng
mountTutorLayout('connections');

// Tải thông tin hồ sơ gia sư để đồng bộ Sidebar & Topbar
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
  loadBookings();
});

