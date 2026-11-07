/**
 * KizunaTutor - Quản lý Lịch giảng dạy & Ca rảnh của Gia sư (tutor-schedule.js)
 * Phân hệ Gia sư - Sử dụng layout Stitch Tutor Dashboard
 */

import { API, ApiError } from '/js/api.js';
import { mountTutorLayout, setTutorIdentity } from '/components/tutor-layout/tutor-layout.js';

const $ = (id) => document.getElementById(id);

const WEEKDAYS = [
  { dow: 2, name: 'Thứ hai', short: 'Thứ 2' },
  { dow: 3, name: 'Thứ ba', short: 'Thứ 3' },
  { dow: 4, name: 'Thứ tư', short: 'Thứ 4' },
  { dow: 5, name: 'Thứ năm', short: 'Thứ 5' },
  { dow: 6, name: 'Thứ sáu', short: 'Thứ 6' },
  { dow: 7, name: 'Thứ bảy', short: 'Thứ 7' },
  { dow: 1, name: 'Chủ nhật', short: 'Chủ nhật' }
];

let availabilitiesList = [];

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

/**
 * Hiển thị lưới lịch rảnh 7 ngày trong tuần
 */
function renderScheduleGrid() {
  const grid = $('scheduleGrid');
  if (!grid) return;

  grid.innerHTML = WEEKDAYS.map(day => {
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

  bindGridEvents();
}

/**
 * Gắn sự kiện xóa và bật/tắt ca rảnh
 */
function bindGridEvents() {
  // Nút thêm ca nhanh từ ngày
  document.querySelectorAll('.btn-quick-add').forEach(btn => {
    btn.addEventListener('click', () => {
      const dow = btn.dataset.dow;
      openAddModal(dow);
    });
  });

  // Nút xóa ca rảnh
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
        await loadSchedule();
      } catch (err) {
        showToast(err.message || 'Không thể xóa ca rảnh lúc này', 'error');
      }
    });
  });

  // Nút bật/tắt ca rảnh
  document.querySelectorAll('.btn-toggle-slot').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.dataset.id;
      const currentActive = btn.dataset.active === 'true';
      try {
        await API.tutorSchedule.toggleSlot(id, !currentActive);
        showToast(!currentActive ? 'Đã bật nhận lịch cho ca này' : 'Đã tạm ẩn ca rảnh này');
        await loadSchedule();
      } catch (err) {
        showToast(err.message || 'Không thể cập nhật trạng thái ca rảnh', 'error');
      }
    });
  });
}

function openAddModal(preselectedDow = null, start = null, end = null) {
  if (preselectedDow) {
    $('selectDayOfWeek').value = String(preselectedDow);
  }
  if (start) {
    $('inputStartTime').value = start;
  }
  if (end) {
    $('inputEndTime').value = end;
  }

  $('formAddSlotError').classList.add('hidden');
  updateDurationPreview();
  $('modalAddSlot').classList.remove('hidden');
}

function closeAddModal() {
  $('modalAddSlot').classList.add('hidden');
}

/**
 * Tải danh sách lịch rảnh từ máy chủ
 */
async function loadSchedule() {
  setVisible($('loadingState'), true);
  setVisible($('scheduleGrid'), false);

  try {
    const data = await API.tutorSchedule.getMySchedule();
    availabilitiesList = Array.isArray(data) ? data : (data?.items || data?.data || []);
    renderScheduleGrid();
  } catch (err) {
    console.error('Lỗi tải lịch giảng dạy:', err);
    showToast(err.message || 'Không thể tải lịch rảnh từ máy chủ', 'error');
  } finally {
    setVisible($('loadingState'), false);
    setVisible($('scheduleGrid'), true);
  }
}

function initEvents() {
  // Nút mở modal thêm ca rảnh
  $('btnOpenAddModal')?.addEventListener('click', () => {
    openAddModal();
  });

  // Đóng modal
  document.querySelectorAll('.modal-close').forEach(btn => {
    btn.addEventListener('click', closeAddModal);
  });

  // Cập nhật xem trước thời lượng khi đổi giờ
  $('inputStartTime')?.addEventListener('input', updateDurationPreview);
  $('inputEndTime')?.addEventListener('input', updateDurationPreview);

  // Mẫu ca rảnh phổ biến
  document.querySelectorAll('.btn-preset').forEach(btn => {
    btn.addEventListener('click', () => {
      const start = btn.dataset.start;
      const end = btn.dataset.end;
      const dow = btn.dataset.dow || '2';
      openAddModal(dow, start, end);
    });
  });

  // Gửi form thêm ca rảnh
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

      closeAddModal();
      showToast('Đã lưu ca rảnh thành công! Ca này đã hiển thị trên trang tìm kiếm gia sư.');
      await loadSchedule();
    } catch (err) {
      errEl.textContent = err instanceof ApiError ? err.message : (err.message || 'Lỗi khi lưu ca rảnh');
      errEl.classList.remove('hidden');
    } finally {
      submitBtn.disabled = false;
      submitText.textContent = 'Lưu ca rảnh';
    }
  });
}

// Khởi tạo layout Gia sư với mục 'schedule'
mountTutorLayout('schedule');

// Tải danh tính gia sư cho Sidebar & Topbar
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
  loadSchedule();
});

