/**
 * KizunaTutor - Quản lý người dùng cho Quản lý vận hành & Quản trị viên (FE-02, US3 & US4).
 * Đặc tả: specs/002-profile-management/. Dữ liệu từ GET/PUT /api/profile/users.
 */

import { API, ApiError } from '/js/api.js';
import { API_CONFIG } from '/js/config/api.config.js';
import { mountStaffLayout } from '/components/staff-layout/staff-layout.js';
import { renderPagination } from '/components/pagination/pagination.js';
import {
  DEFAULT_PROFILE_ERROR,
  VERIFICATION_LABEL_MAP,
  avatarSrc,
  escapeHtml,
  formatCurrencyVnd,
  formatDateTime,
  roleLabel,
  statusBadgeHtml,
  statusLabel,
  validateStatusUpdate
} from '/js/profile-utils.js';

const API_ORIGIN = new URL(API_CONFIG.BASE_URL).origin;
const $ = (id) => document.getElementById(id);

const state = {
  role: 'ALL',
  status: 'ALL',
  keyword: '',
  page: 0,
  size: 10,
  items: [],
  detailUser: null,
  statusTarget: null
};

let debounceTimer = null;

function errorText(err) {
  return err instanceof ApiError ? err.message : DEFAULT_PROFILE_ERROR;
}

function showState(id) {
  ['usersLoading', 'usersError', 'usersContent'].forEach((s) => {
    $(s)?.classList.toggle('hidden', s !== id);
  });
}

function showFeedback(message, type = 'success') {
  const box = $('usersFeedback');
  if (!box) return;
  box.textContent = message || '';
  box.className = `rounded-xl px-4 py-3 text-sm ${type === 'success'
    ? 'bg-[#059669]/10 border border-[#059669]/20 text-[#047857]'
    : 'bg-[#BA1A1A]/5 border border-[#BA1A1A]/20 text-[#BA1A1A]'}`;
  box.classList.toggle('hidden', !message);
}

function hasActiveFilters() {
  return state.role !== 'ALL' || state.status !== 'ALL' || Boolean(state.keyword.trim());
}

function avatarCellHtml(url, fullName) {
  const initial = escapeHtml((fullName || '?').trim().charAt(0).toUpperCase() || '?');
  if (!url) {
    return `<div class="w-9 h-9 rounded-full bg-surface-container text-primary font-semibold text-sm flex items-center justify-center shrink-0">${initial}</div>`;
  }
  const src = escapeHtml(avatarSrc(url, API_ORIGIN));
  return `<div class="w-9 h-9 rounded-full bg-surface-container overflow-hidden shrink-0 flex items-center justify-center text-primary font-semibold text-sm">
    <img src="${src}" alt="Ảnh đại diện" class="w-full h-full object-cover">
  </div>`;
}

function renderRoleTabs() {
  document.querySelectorAll('[data-role-tab]').forEach((tab) => {
    const active = tab.dataset.roleTab === state.role;
    tab.setAttribute('aria-selected', String(active));
    tab.className = active
      ? 'px-4 py-2 rounded-xl text-label-md font-semibold transition-colors bg-primary text-on-primary shadow-sm'
      : 'px-4 py-2 rounded-xl text-label-md font-medium transition-colors bg-white text-on-surface-variant border border-outline-variant hover:bg-surface-container-low';
  });
}

function renderRows(items) {
  const tbody = $('usersTableBody');
  if (!tbody) return;
  tbody.innerHTML = items.map((u) => {
    const roleText = escapeHtml(roleLabel(u.role, u.roleBadge));
    const createdText = escapeHtml(formatDateTime(u.createdAt) || 'Chưa ghi nhận');
    return `
      <tr class="hover:bg-surface-container-low/50 transition-colors">
        <td class="px-4 py-3">
          <div class="flex items-center gap-3">
            ${avatarCellHtml(u.avatarUrl, u.fullName)}
            <div class="min-w-0">
              <p class="font-semibold text-on-surface truncate">${escapeHtml(u.fullName || 'Chưa cập nhật')}</p>
              <p class="text-xs text-on-surface-variant truncate">${escapeHtml(u.email)}</p>
            </div>
          </div>
        </td>
        <td class="px-4 py-3 text-on-surface">${escapeHtml(u.phone || 'Chưa cập nhật')}</td>
        <td class="px-4 py-3">
          <span class="inline-flex items-center rounded-lg bg-surface-container px-2.5 py-1 text-xs font-medium text-on-surface">${roleText}</span>
        </td>
        <td class="px-4 py-3">${statusBadgeHtml(u.accountStatus)}</td>
        <td class="px-4 py-3 text-on-surface-variant">${createdText}</td>
        <td class="px-4 py-3 text-right">
          <div class="inline-flex items-center justify-end gap-1.5">
            <button type="button" class="tv-btn tv-btn-small" data-view-user="${escapeHtml(u.userId)}">Xem chi tiết</button>
            <button type="button" class="tv-btn tv-btn-small" data-status-user="${escapeHtml(u.userId)}">Trạng thái</button>
          </div>
        </td>
      </tr>`;
  }).join('');

  tbody.querySelectorAll('[data-view-user]').forEach((btn) => {
    btn.addEventListener('click', () => openDetailModal(btn.dataset.viewUser));
  });
  tbody.querySelectorAll('[data-status-user]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const found = state.items.find((item) => item.userId === btn.dataset.statusUser);
      if (found) openStatusModal(found);
    });
  });
}

function renderEmpty(isEmpty) {
  const emptyEl = $('usersEmpty');
  const resetBtn = $('emptyResetBtn');
  if (!emptyEl) return;
  emptyEl.classList.toggle('hidden', !isEmpty);
  if (isEmpty) {
    const filtered = hasActiveFilters();
    $('usersEmptyText').textContent = filtered
      ? 'Không tìm thấy người dùng phù hợp với tiêu chí lọc hiện tại.'
      : 'Chưa có người dùng học viên hoặc gia sư nào trong hệ thống.';
    resetBtn?.classList.toggle('hidden', !filtered);
  }
}

async function loadUsers(page = state.page) {
  state.page = page;
  showState('usersLoading');
  try {
    const res = await API.profile.getUsers({
      role: state.role,
      status: state.status,
      keyword: state.keyword.trim(),
      page: state.page,
      size: state.size
    });
    state.items = Array.isArray(res?.content) ? res.content : [];
    state.page = Number(res?.page ?? state.page);
    renderRows(state.items);
    renderEmpty(state.items.length === 0);
    renderPagination($('usersPager'), res || { page: 0, size: state.size, totalElements: 0, totalPages: 1 }, {
      onPage: (nextPage) => loadUsers(nextPage),
      onSize: (nextSize) => {
        state.size = nextSize;
        loadUsers(0);
      }
    });
    showState('usersContent');
  } catch (err) {
    $('usersErrorMessage').textContent = errorText(err);
    showState('usersError');
  }
}

function resetFilters() {
  state.role = 'ALL';
  state.status = 'ALL';
  state.keyword = '';
  $('keywordInput').value = '';
  $('statusFilter').value = 'ALL';
  renderRoleTabs();
  loadUsers(0);
}

function renderDetailContent(detail) {
  const roleText = escapeHtml(roleLabel(detail.role, detail.roleBadge));
  const learnerBlock = detail.learnerDetails ? `
    <div class="rounded-xl border border-surface-container bg-surface-container-low/60 p-4 space-y-2.5">
      <h3 class="text-sm font-semibold text-on-surface">Thông tin học tập</h3>
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
        <div>
          <span class="text-xs text-on-surface-variant block">Trình độ hiện tại</span>
          <span class="font-medium text-on-surface">${escapeHtml(detail.learnerDetails.currentLevel || 'Chưa cập nhật')}</span>
        </div>
        <div>
          <span class="text-xs text-on-surface-variant block">Trình độ mục tiêu</span>
          <span class="font-medium text-on-surface">${escapeHtml(detail.learnerDetails.targetLevel || 'Chưa cập nhật')}</span>
        </div>
        <div class="sm:col-span-2">
          <span class="text-xs text-on-surface-variant block">Mục tiêu học tập</span>
          <span class="text-on-surface whitespace-pre-line">${escapeHtml(detail.learnerDetails.learningGoals || 'Chưa cập nhật')}</span>
        </div>
      </div>
    </div>` : '';

  const tutorBlock = detail.tutorDetails ? `
    <div class="rounded-xl border border-surface-container bg-surface-container-low/60 p-4 space-y-2.5">
      <h3 class="text-sm font-semibold text-on-surface">Thông tin giảng dạy gia sư</h3>
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
        <div>
          <span class="text-xs text-on-surface-variant block">Mã gia sư</span>
          <span class="font-medium text-on-surface">${detail.tutorDetails.tutorCode ? `#${escapeHtml(detail.tutorDetails.tutorCode)}` : 'Chưa cấp'}</span>
        </div>
        <div>
          <span class="text-xs text-on-surface-variant block">Xác minh hồ sơ</span>
          <span class="font-medium text-on-surface">${escapeHtml(VERIFICATION_LABEL_MAP[detail.tutorDetails.verificationStatus] || 'Chưa xác minh')}</span>
        </div>
        <div>
          <span class="text-xs text-on-surface-variant block">Trình độ JLPT</span>
          <span class="font-medium text-on-surface">${escapeHtml(detail.tutorDetails.highestJlptLevel ? `JLPT ${detail.tutorDetails.highestJlptLevel}` : 'Chưa cập nhật')}</span>
        </div>
        <div>
          <span class="text-xs text-on-surface-variant block">Học phí dự kiến</span>
          <span class="font-medium text-on-surface">${escapeHtml(formatCurrencyVnd(detail.tutorDetails.hourlyRate))}</span>
        </div>
        <div>
          <span class="text-xs text-on-surface-variant block">Kinh nghiệm</span>
          <span class="font-medium text-on-surface">${detail.tutorDetails.experienceYears ?? 0} năm</span>
        </div>
        <div>
          <span class="text-xs text-on-surface-variant block">Khu vực công khai</span>
          <span class="font-medium text-on-surface">${escapeHtml(detail.tutorDetails.fullAddress || 'Chưa cập nhật')}</span>
        </div>
        <div class="sm:col-span-2">
          <span class="text-xs text-on-surface-variant block">Tiêu đề giảng dạy</span>
          <span class="font-medium text-on-surface">${escapeHtml(detail.tutorDetails.headline || 'Chưa cập nhật')}</span>
        </div>
        <div class="sm:col-span-2">
          <span class="text-xs text-on-surface-variant block">Giới thiệu</span>
          <span class="text-on-surface whitespace-pre-line">${escapeHtml(detail.tutorDetails.bio || 'Chưa cập nhật')}</span>
        </div>
      </div>
    </div>` : '';

  const historyItems = Array.isArray(detail.statusHistory) ? detail.statusHistory : [];
  const historyHtml = historyItems.length
    ? `<ul class="space-y-2">
        ${historyItems.map((h) => `
          <li class="rounded-xl border border-surface-container p-3 text-xs space-y-1">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <span class="font-semibold text-on-surface">
                ${escapeHtml(statusLabel(h.previousStatus))} → ${escapeHtml(statusLabel(h.newStatus))}
              </span>
              <span class="text-on-surface-variant">${escapeHtml(formatDateTime(h.createdAt))}</span>
            </div>
            <p class="text-on-surface">${escapeHtml(h.reason || 'Không ghi nhận lý do.')}</p>
            <p class="text-on-surface-variant">Người thực hiện: ${escapeHtml(h.actorName || 'Quản lý hệ thống')}</p>
          </li>`).join('')}
      </ul>`
    : '<p class="text-xs text-on-surface-variant">Chưa có lịch sử điều chỉnh trạng thái tài khoản.</p>';

  return `
    <div class="flex items-start gap-4">
      ${avatarCellHtml(detail.avatarUrl, detail.fullName)}
      <div class="flex-1 min-w-0 space-y-1">
        <div class="flex flex-wrap items-center gap-2">
          <h3 class="text-base font-semibold text-on-surface">${escapeHtml(detail.fullName || 'Chưa cập nhật')}</h3>
          <span class="inline-flex items-center rounded-lg bg-surface-container px-2.5 py-0.5 text-xs font-medium text-on-surface">${roleText}</span>
          ${statusBadgeHtml(detail.accountStatus)}
        </div>
        <p class="text-sm text-on-surface-variant">${escapeHtml(detail.email)}</p>
        <p class="text-xs text-on-surface-variant">Số điện thoại: ${escapeHtml(detail.phone || 'Chưa cập nhật')} • Ngày tạo: ${escapeHtml(formatDateTime(detail.createdAt) || 'Chưa ghi nhận')}</p>
      </div>
    </div>
    ${learnerBlock}
    ${tutorBlock}
    <div class="space-y-2">
      <h3 class="text-sm font-semibold text-on-surface">Lịch sử trạng thái tài khoản</h3>
      ${historyHtml}
    </div>`;
}

async function openDetailModal(userId) {
  const modal = $('userDetailModal');
  const body = $('detailModalBody');
  const openStatusBtn = $('detailOpenStatusBtn');
  if (!modal || !body) return;

  state.detailUser = null;
  openStatusBtn?.classList.add('hidden');
  body.innerHTML = '<div class="tv-skeleton h-48"></div>';
  modal.classList.remove('hidden');

  try {
    const detail = await API.profile.getUserDetail(userId);
    state.detailUser = detail;
    body.innerHTML = renderDetailContent(detail);
    openStatusBtn?.classList.remove('hidden');
  } catch (err) {
    body.innerHTML = `<div class="rounded-xl bg-[#BA1A1A]/5 border border-[#BA1A1A]/20 p-4 text-sm text-[#BA1A1A]">${escapeHtml(errorText(err))}</div>`;
  }
}

function closeDetailModal() {
  $('userDetailModal')?.classList.add('hidden');
}

function openStatusModal(user) {
  state.statusTarget = user;
  const modal = $('statusChangeModal');
  if (!modal) return;

  $('statusTargetSummary').innerHTML = `
    <div class="flex flex-wrap items-center justify-between gap-2">
      <div>
        <p class="font-semibold text-on-surface">${escapeHtml(user.fullName || 'Chưa cập nhật')}</p>
        <p class="text-xs text-on-surface-variant">${escapeHtml(user.email)}</p>
      </div>
      ${statusBadgeHtml(user.accountStatus)}
    </div>`;

  const currentOption = user.accountStatus && ['ACTIVE', 'SUSPENDED', 'BANNED'].includes(user.accountStatus)
    ? user.accountStatus
    : 'ACTIVE';
  document.querySelectorAll('input[name="accountStatusOption"]').forEach((radio) => {
    radio.checked = radio.value === currentOption;
  });

  $('statusReasonInput').value = '';
  $('reasonCharCount').textContent = '0 / 500';
  $('statusReasonError').textContent = '';
  $('statusReasonError').classList.add('hidden');
  $('statusFormError').textContent = '';
  $('statusFormError').classList.add('hidden');
  modal.classList.remove('hidden');
  $('statusReasonInput').focus();
}

function closeStatusModal() {
  $('statusChangeModal')?.classList.add('hidden');
  state.statusTarget = null;
}

async function handleStatusSubmit(event) {
  event.preventDefault();
  if (!state.statusTarget) return;

  const selectedRadio = document.querySelector('input[name="accountStatusOption"]:checked');
  const status = selectedRadio ? selectedRadio.value : '';
  const reason = $('statusReasonInput').value.trim();

  $('statusReasonError').classList.add('hidden');
  $('statusFormError').classList.add('hidden');

  const check = validateStatusUpdate({ status, reason });
  if (!check.valid) {
    $('statusReasonError').textContent = check.error;
    $('statusReasonError').classList.remove('hidden');
    return;
  }

  const submitBtn = $('submitStatusBtn');
  submitBtn.disabled = true;
  $('submitStatusLabel').textContent = 'Đang cập nhật...';

  const targetId = state.statusTarget.userId;
  try {
    await API.profile.updateUserStatus(targetId, { status, reason });
    closeStatusModal();
    showFeedback('Cập nhật trạng thái tài khoản thành công.', 'success');
    await loadUsers(state.page);
    if (!$('userDetailModal')?.classList.contains('hidden') && state.detailUser?.userId === targetId) {
      await openDetailModal(targetId);
    }
  } catch (err) {
    $('statusFormError').textContent = errorText(err);
    $('statusFormError').classList.remove('hidden');
  } finally {
    submitBtn.disabled = false;
    $('submitStatusLabel').textContent = 'Xác nhận cập nhật';
  }
}

function bindEvents() {
  document.querySelectorAll('[data-role-tab]').forEach((tab) => {
    tab.addEventListener('click', () => {
      if (state.role === tab.dataset.roleTab) return;
      state.role = tab.dataset.roleTab;
      renderRoleTabs();
      loadUsers(0);
    });
  });

  $('filterForm')?.addEventListener('submit', (event) => {
    event.preventDefault();
    clearTimeout(debounceTimer);
    state.keyword = $('keywordInput').value;
    state.status = $('statusFilter').value;
    loadUsers(0);
  });

  $('keywordInput')?.addEventListener('input', () => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      state.keyword = $('keywordInput').value;
      loadUsers(0);
    }, 300);
  });

  $('statusFilter')?.addEventListener('change', () => {
    state.status = $('statusFilter').value;
    loadUsers(0);
  });

  $('resetFilterBtn')?.addEventListener('click', resetFilters);
  $('emptyResetBtn')?.addEventListener('click', resetFilters);
  $('retryUsersBtn')?.addEventListener('click', () => loadUsers(state.page));

  $('closeDetailModal')?.addEventListener('click', closeDetailModal);
  $('detailCloseFooterBtn')?.addEventListener('click', closeDetailModal);
  $('detailOpenStatusBtn')?.addEventListener('click', () => {
    if (state.detailUser) openStatusModal(state.detailUser);
  });
  $('userDetailModal')?.addEventListener('click', (e) => {
    if (e.target === $('userDetailModal')) closeDetailModal();
  });

  $('closeStatusModal')?.addEventListener('click', closeStatusModal);
  $('cancelStatusModalBtn')?.addEventListener('click', closeStatusModal);
  $('statusChangeModal')?.addEventListener('click', (e) => {
    if (e.target === $('statusChangeModal')) closeStatusModal();
  });
  $('statusReasonInput')?.addEventListener('input', (e) => {
    $('reasonCharCount').textContent = `${e.target.value.length} / 500`;
  });
  $('statusChangeForm')?.addEventListener('submit', handleStatusSubmit);

  document.addEventListener('keydown', (e) => {
    if (e.key !== 'Escape') return;
    if (!$('statusChangeModal')?.classList.contains('hidden')) {
      closeStatusModal();
    } else if (!$('userDetailModal')?.classList.contains('hidden')) {
      closeDetailModal();
    }
  });
}

if (mountStaffLayout('manager', 'users', 'Quản lý người dùng', { allowedRoles: ['ROLE_MANAGER', 'ROLE_ADMIN'] })) {
  renderRoleTabs();
  bindEvents();
  loadUsers(0);
}
