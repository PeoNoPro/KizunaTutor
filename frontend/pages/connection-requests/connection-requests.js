/**
 * KizunaTutor - Connection Requests & Trial Bookings Page Logic (FE-05)
 * Hỗ trợ hiển thị danh sách, chuyển tab, duyệt lịch và hủy lịch cho cả Học viên và Gia sư.
 */

import { API } from '../../js/api.js';

let currentStatusFilter = 'ALL';
let activeBookingId = null;
let bookingsList = [];

// Khởi tạo các hàm đóng mở Modal toàn cục cho HTML gọi
window.closeAcceptModal = () => {
  document.getElementById('modal-accept-booking')?.classList.add('hidden');
  activeBookingId = null;
};

window.closeDeclineModal = () => {
  document.getElementById('modal-decline-booking')?.classList.add('hidden');
  activeBookingId = null;
};

window.closeCancelModal = () => {
  document.getElementById('modal-cancel-booking')?.classList.add('hidden');
  activeBookingId = null;
};

function showToast(message, isError = false) {
  const alertEl = document.getElementById('action-toast-alert');
  const msgEl = document.getElementById('action-toast-msg');
  const iconEl = document.getElementById('action-toast-icon');
  if (!alertEl || !msgEl) return;

  msgEl.innerText = message;
  if (isError) {
    alertEl.className = 'p-3.5 rounded-2xl text-xs font-medium flex items-center justify-between shadow-sm bg-rose-50 border border-rose-200 text-rose-800 transition-all';
    if (iconEl) iconEl.innerText = 'error';
  } else {
    alertEl.className = 'p-3.5 rounded-2xl text-xs font-medium flex items-center justify-between shadow-sm bg-emerald-50 border border-emerald-200 text-emerald-800 transition-all';
    if (iconEl) iconEl.innerText = 'check_circle';
  }
  alertEl.classList.remove('hidden');
  alertEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  setTimeout(() => {
    alertEl.classList.add('hidden');
  }, 5000);
}

function formatBookingTime(startTimeStr, endTimeStr, durationMinutes) {
  if (!startTimeStr) return 'Thời gian linh hoạt';
  const start = new Date(startTimeStr);
  const end = endTimeStr ? new Date(endTimeStr) : new Date(start.getTime() + (durationMinutes || 50) * 60000);

  const pad = (n) => String(n).padStart(2, '0');
  const startH = pad(start.getHours());
  const startM = pad(start.getMinutes());
  const endH = pad(end.getHours());
  const endM = pad(end.getMinutes());

  const DAY_NAMES = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'];
  const dayName = DAY_NAMES[start.getDay()];
  const dateStr = `${pad(start.getDate())}/${pad(start.getMonth() + 1)}/${start.getFullYear()}`;

  return `${startH}:${startM} – ${endH}:${endM} • ${dayName}, ${dateStr}`;
}

function getStatusBadge(status, label) {
  switch (status) {
    case 'PENDING':
      return `<span class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
        <span class="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse"></span>
        ${label || 'Chờ phản hồi'}
      </span>`;
    case 'CONFIRMED':
      return `<span class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
        <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
        ${label || 'Đã xác nhận'}
      </span>`;
    case 'CANCELLED':
      return `<span class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-600 border border-slate-200">
        <span class="w-1.5 h-1.5 rounded-full bg-slate-400"></span>
        ${label || 'Đã hủy'}
      </span>`;
    case 'COMPLETED':
      return `<span class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-sky-50 text-sky-700 border border-sky-200">
        <span class="w-1.5 h-1.5 rounded-full bg-sky-500"></span>
        ${label || 'Đã hoàn thành'}
      </span>`;
    default:
      return `<span class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-600 border border-slate-200">
        ${label || status}
      </span>`;
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  const container = document.getElementById('requests-list');
  const emptyState = document.getElementById('requests-empty-state');
  const emptyDesc = document.getElementById('empty-state-desc');

  // Kiểm tra đăng nhập
  const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
  if (!token) {
    if (container) {
      container.innerHTML = `
        <div class="p-12 text-center bg-white rounded-3xl border border-[#E5DECB] space-y-4">
          <div class="w-16 h-16 rounded-2xl bg-sky-50 text-sky-600 flex items-center justify-center mx-auto">
            <span class="material-symbols-outlined text-[32px]">lock</span>
          </div>
          <h3 class="text-lg font-bold text-[#0F2545]">Vui lòng đăng nhập để xem lịch hẹn</h3>
          <p class="text-xs text-slate-500 max-w-sm mx-auto">
            Bạn cần đăng nhập tài khoản Học viên hoặc Gia sư để quản lý toàn bộ các yêu cầu học thử kết nối.
          </p>
          <a href="/login?redirect=${encodeURIComponent('/pages/connection-requests/')}" class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-bold transition-all shadow-md">
            Đăng nhập ngay
          </a>
        </div>
      `;
    }
    return;
  }

  // Khởi tạo toggle custom decline reason
  const declineSelect = document.getElementById('decline-reason-select');
  const declineCustom = document.getElementById('decline-reason-custom');
  if (declineSelect && declineCustom) {
    declineSelect.addEventListener('change', () => {
      if (declineSelect.value === 'Khác') {
        declineCustom.classList.remove('hidden');
        declineCustom.focus();
      } else {
        declineCustom.classList.add('hidden');
      }
    });
  }

  // Tải danh sách
  async function loadBookings() {
    if (!container) return;
    try {
      const response = await API.trialBookings.getMyRequests('ALL', 0, 50);
      bookingsList = response?.items || response?.content || response?.data?.items || response?.data?.content || (Array.isArray(response) ? response : []);

      // Cập nhật số lượng trên các tab
      updateTabCounts();
      renderFilteredBookings();

    } catch (err) {
      console.error('Lỗi tải danh sách yêu cầu:', err);
      container.innerHTML = `
        <div class="p-8 text-center bg-white rounded-3xl border border-[#E5DECB] space-y-3">
          <p class="text-xs text-rose-600 font-semibold">${err.message || 'Không thể tải danh sách lúc này'}</p>
          <button type="button" onclick="location.reload()" class="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-xs font-bold text-slate-700">Thử lại</button>
        </div>
      `;
    }
  }

  function updateTabCounts() {
    const allCount = bookingsList.length;
    const pendingCount = bookingsList.filter(b => b.bookingStatus === 'PENDING').length;
    const confirmedCount = bookingsList.filter(b => b.bookingStatus === 'CONFIRMED').length;
    const historyCount = bookingsList.filter(b => b.bookingStatus === 'CANCELLED' || b.bookingStatus === 'COMPLETED').length;

    const elAll = document.getElementById('count-all');
    const elPending = document.getElementById('count-pending');
    const elConfirmed = document.getElementById('count-confirmed');
    const elHistory = document.getElementById('count-history');

    if (elAll) elAll.innerText = allCount;
    if (elPending) elPending.innerText = pendingCount;
    if (elConfirmed) elConfirmed.innerText = confirmedCount;
    if (elHistory) elHistory.innerText = historyCount;
  }

  function renderFilteredBookings() {
    let filtered = [];
    if (currentStatusFilter === 'ALL') {
      filtered = bookingsList;
    } else if (currentStatusFilter === 'PENDING') {
      filtered = bookingsList.filter(b => b.bookingStatus === 'PENDING');
    } else if (currentStatusFilter === 'CONFIRMED') {
      filtered = bookingsList.filter(b => b.bookingStatus === 'CONFIRMED');
    } else if (currentStatusFilter === 'HISTORY') {
      filtered = bookingsList.filter(b => b.bookingStatus === 'CANCELLED' || b.bookingStatus === 'COMPLETED');
    }

    if (filtered.length === 0) {
      if (emptyState) emptyState.classList.remove('hidden');
      if (emptyDesc) {
        if (currentStatusFilter === 'PENDING') {
          emptyDesc.innerText = 'Bạn không có buổi học nào đang chờ phản hồi.';
        } else if (currentStatusFilter === 'CONFIRMED') {
          emptyDesc.innerText = 'Chưa có lịch hẹn nào được xác nhận.';
        } else {
          emptyDesc.innerText = 'Bạn chưa có buổi học thử nào ở danh mục này.';
        }
      }
      container.innerHTML = '';
      return;
    }

    if (emptyState) emptyState.classList.add('hidden');

    container.innerHTML = filtered.map(item => {
      const isOnline = item.learningMode === 'ONLINE';
      const isTutorViewing = (item.partnerRole === 'LEARNER');
      const partnerDisplayRole = isTutorViewing ? 'Học viên' : 'Gia sư';
      const timeFormatted = formatBookingTime(item.scheduledStartTime, item.scheduledEndTime, item.durationMinutes);
      const avatarSrc = item.partnerAvatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=120&q=80';

      // Nút hành động
      let actionButtonsHtml = '';

      if (item.bookingStatus === 'PENDING') {
        if (isTutorViewing) {
          // Gia sư duyệt
          actionButtonsHtml = `
            <div class="flex items-center gap-2 pt-2 border-t border-slate-100">
              <button type="button" class="btn-open-accept px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition-all shadow-xs flex items-center gap-1.5" data-booking-id="${item.bookingId}" data-mode="${item.learningMode}">
                <span class="material-symbols-outlined text-[16px]">check</span>
                <span>Chấp nhận</span>
              </button>
              <button type="button" class="btn-open-decline px-4 py-2 rounded-xl bg-white border border-rose-300 hover:bg-rose-50 text-rose-600 text-xs font-bold transition-all flex items-center gap-1.5" data-booking-id="${item.bookingId}">
                <span class="material-symbols-outlined text-[16px]">close</span>
                <span>Từ chối</span>
              </button>
            </div>
          `;
        } else {
          // Học viên có thể hủy khi đang chờ
          actionButtonsHtml = `
            <div class="flex items-center gap-2 pt-2 border-t border-slate-100">
              <button type="button" class="btn-open-cancel px-4 py-2 rounded-xl bg-white border border-slate-300 hover:bg-slate-50 text-slate-600 text-xs font-semibold transition-all flex items-center gap-1.5" data-booking-id="${item.bookingId}">
                <span class="material-symbols-outlined text-[16px]">close</span>
                <span>Hủy yêu cầu</span>
              </button>
            </div>
          `;
        }
      } else if (item.bookingStatus === 'CONFIRMED') {
        const meetButtonHtml = (isOnline && item.meetingLink) ? `
          <a href="${item.meetingLink}" target="_blank" rel="noopener noreferrer" class="px-4 py-2 rounded-xl bg-[#006194] hover:bg-[#004b73] text-white text-xs font-bold transition-all shadow-xs flex items-center gap-1.5">
            <span class="material-symbols-outlined text-[16px]">video_call</span>
            <span>Vào phòng học</span>
          </a>
        ` : '';

        actionButtonsHtml = `
          <div class="flex items-center gap-2 pt-2 border-t border-slate-100 flex-wrap">
            ${meetButtonHtml}
            <button type="button" class="btn-open-cancel px-4 py-2 rounded-xl bg-white border border-slate-300 hover:bg-slate-50 text-slate-600 text-xs font-semibold transition-all flex items-center gap-1.5" data-booking-id="${item.bookingId}">
              <span class="material-symbols-outlined text-[16px]">event_busy</span>
              <span>Hủy lịch</span>
            </button>
          </div>
        `;
      }

      return `
        <div class="p-5 md:p-6 bg-white rounded-3xl border border-[#E5DECB] shadow-xs space-y-4 transition-all hover:border-[#006194]/40">
          <!-- Hàng trên: Đối tác + Trạng thái -->
          <div class="flex items-start justify-between gap-4">
            <div class="flex items-center gap-3">
              <img src="${avatarSrc}" alt="Avatar ${partnerDisplayRole}" class="w-12 h-12 rounded-2xl object-cover border border-[#E5DECB] shadow-2xs">
              <div>
                <div class="flex items-center gap-2">
                  <h3 class="text-sm md:text-base font-bold text-[#0F2545]">${item.partnerName || 'Chưa cập nhật'}</h3>
                  <span class="px-2 py-0.5 rounded-md text-[10px] font-bold ${isTutorViewing ? 'bg-sky-50 text-sky-700' : 'bg-emerald-50 text-emerald-700'}">
                    ${partnerDisplayRole}
                  </span>
                </div>
                <p class="text-xs text-slate-500 mt-0.5">${item.durationMinutes || 50} phút • Học thử miễn phí 0đ</p>
              </div>
            </div>
            <div>
              ${getStatusBadge(item.bookingStatus, item.statusLabel)}
            </div>
          </div>

          <!-- Thông tin chi tiết lịch hẹn -->
          <div class="p-4 rounded-2xl bg-[#FAF8F2] border border-[#ECE8DE] space-y-2 text-xs">
            <div class="flex items-center gap-2 text-[#0F2545] font-semibold">
              <span class="material-symbols-outlined text-[18px] text-[#006194]">schedule</span>
              <span>${timeFormatted}</span>
            </div>

            <div class="flex items-center gap-2 text-slate-700">
              <span class="material-symbols-outlined text-[18px] text-slate-500">${isOnline ? 'laptop_chromebook' : 'pin_drop'}</span>
              <span>${isOnline ? 'Hình thức: Trực tuyến (Google Meet)' : `Hình thức: Trực tiếp tại ${item.locationDetails || 'địa chỉ hẹn'}`}</span>
            </div>

            ${(item.currentLevel || item.learningGoals) ? `
              <div class="flex items-center gap-2 text-slate-600 pt-1 border-t border-slate-200/60">
                <span class="material-symbols-outlined text-[18px] text-amber-600">psychology</span>
                <span>Trình độ: <strong>${item.currentLevel || 'Cơ bản'}</strong> ${item.learningGoals ? `• Mục tiêu: ${item.learningGoals}` : ''}</span>
              </div>
            ` : ''}

            ${(item.initialMessage && item.initialMessage.trim()) ? `
              <div class="text-slate-600 italic bg-white/70 p-2.5 rounded-xl border border-slate-200/60 mt-1">
                "${item.initialMessage.replace(/\n/g, '<br>')}"
              </div>
            ` : ''}

            ${(isOnline && item.meetingLink && item.bookingStatus === 'CONFIRMED') ? `
              <div class="flex items-center gap-2 text-emerald-700 font-semibold pt-1">
                <span class="material-symbols-outlined text-[18px]">link</span>
                <a href="${item.meetingLink}" target="_blank" class="underline hover:text-emerald-800 break-all">${item.meetingLink}</a>
              </div>
            ` : ''}
          </div>

          <!-- Các nút hành động -->
          ${actionButtonsHtml}
        </div>
      `;
    }).join('');

    bindActionEvents();
  }

  function bindActionEvents() {
    // Nút mở modal Chấp nhận
    document.querySelectorAll('.btn-open-accept').forEach(btn => {
      btn.addEventListener('click', () => {
        activeBookingId = btn.dataset.bookingId;
        const mode = btn.dataset.mode;
        const meetContainer = document.getElementById('accept-meet-container');
        if (meetContainer) {
          if (mode === 'ONLINE') {
            meetContainer.classList.remove('hidden');
          } else {
            meetContainer.classList.add('hidden');
          }
        }
        document.getElementById('modal-accept-booking')?.classList.remove('hidden');
      });
    });

    // Nút mở modal Từ chối
    document.querySelectorAll('.btn-open-decline').forEach(btn => {
      btn.addEventListener('click', () => {
        activeBookingId = btn.dataset.bookingId;
        document.getElementById('modal-decline-booking')?.classList.remove('hidden');
      });
    });

    // Nút mở modal Hủy lịch
    document.querySelectorAll('.btn-open-cancel').forEach(btn => {
      btn.addEventListener('click', () => {
        activeBookingId = btn.dataset.bookingId;
        document.getElementById('modal-cancel-booking')?.classList.remove('hidden');
      });
    });
  }

  // Chuyển tab
  const tabButtons = document.querySelectorAll('.tab-btn');
  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      tabButtons.forEach(b => {
        b.className = 'tab-btn px-4 py-2.5 rounded-xl border-b-2 border-transparent text-slate-600 hover:text-[#0F2545] hover:bg-slate-100/60 cursor-pointer transition-all';
      });
      btn.className = 'tab-btn px-4 py-2.5 rounded-xl border-b-2 border-[#006194] text-[#006194] bg-sky-50/60 font-bold cursor-pointer transition-all';
      currentStatusFilter = btn.dataset.status;
      renderFilteredBookings();
    });
  });

  // Xử lý gửi Duyệt Chấp nhận (Accept)
  const submitAcceptBtn = document.getElementById('btn-submit-accept');
  if (submitAcceptBtn) {
    submitAcceptBtn.addEventListener('click', async () => {
      if (!activeBookingId) return;

      const linkInput = document.getElementById('accept-meeting-link');
      const noteInput = document.getElementById('accept-note');
      const meetingLink = linkInput ? linkInput.value.trim() : '';
      const note = noteInput ? noteInput.value.trim() : '';

      submitAcceptBtn.disabled = true;
      submitAcceptBtn.innerText = 'Đang xử lý...';

      try {
        await API.trialBookings.respond(activeBookingId, {
          action: 'ACCEPT',
          meetingLink: meetingLink || null,
          note: note || null
        });

        window.closeAcceptModal();
        showToast('Đã xác nhận đồng ý lịch học thử thành công!');
        await loadBookings();
      } catch (err) {
        alert(err.message || 'Không thể duyệt yêu cầu lúc này');
      } finally {
        submitAcceptBtn.disabled = false;
        submitAcceptBtn.innerText = 'Xác nhận duyệt';
      }
    });
  }

  // Xử lý gửi Từ chối (Decline)
  const submitDeclineBtn = document.getElementById('btn-submit-decline');
  if (submitDeclineBtn) {
    submitDeclineBtn.addEventListener('click', async () => {
      if (!activeBookingId) return;

      const selectEl = document.getElementById('decline-reason-select');
      const customEl = document.getElementById('decline-reason-custom');
      let reason = selectEl ? selectEl.value : 'Bận việc đột xuất';
      if (reason === 'Khác' && customEl && customEl.value.trim()) {
        reason = customEl.value.trim();
      }

      submitDeclineBtn.disabled = true;
      submitDeclineBtn.innerText = 'Đang xử lý...';

      try {
        await API.trialBookings.respond(activeBookingId, {
          action: 'DECLINE',
          declineReason: reason
        });

        window.closeDeclineModal();
        showToast('Đã từ chối lịch học thử.');
        await loadBookings();
      } catch (err) {
        alert(err.message || 'Không thể từ chối yêu cầu lúc này');
      } finally {
        submitDeclineBtn.disabled = false;
        submitDeclineBtn.innerText = 'Xác nhận từ chối';
      }
    });
  }

  // Xử lý Hủy lịch (Cancel)
  const submitCancelBtn = document.getElementById('btn-submit-cancel');
  if (submitCancelBtn) {
    submitCancelBtn.addEventListener('click', async () => {
      if (!activeBookingId) return;

      const reasonInput = document.getElementById('cancel-reason-input');
      const cancelReason = reasonInput ? reasonInput.value.trim() : '';

      submitCancelBtn.disabled = true;
      submitCancelBtn.innerText = 'Đang xử lý...';

      try {
        await API.trialBookings.cancel(activeBookingId, {
          cancelReason: cancelReason || 'Học viên/Gia sư thay đổi kế hoạch'
        });

        window.closeCancelModal();
        showToast('Đã hủy lịch hẹn học thử thành công.');
        await loadBookings();
      } catch (err) {
        alert(err.message || 'Không thể hủy lịch lúc này');
      } finally {
        submitCancelBtn.disabled = false;
        submitCancelBtn.innerText = 'Xác nhận hủy lịch';
      }
    });
  }

  // Tải danh sách lần đầu
  await loadBookings();
});
