/**
 * KizunaTutor - Danh sách và chi tiết hội viên gia sư (FE-09: UC-44, UC-45). Chỉ đọc.
 */

import { API, ApiError } from '/js/api.js';
import { mountStaffLayout } from '/components/staff-layout/staff-layout.js';
import { escapeHtml, formatMoney, formatDate, formatDateTime, statusBadge } from '/js/membership-format.js';

const PAGE_SIZE = 20;
const $ = (id) => document.getElementById(id);
const state = { page: 0 };

function show(id) {
  ['loadingState', 'errorState', 'contentState'].forEach((s) => $(s).classList.toggle('hidden', s !== id));
}

function errorText(err) {
  return err instanceof ApiError ? err.message : 'Hệ thống đang bận, bạn vui lòng thử lại sau.';
}

function filters() {
  return { keyword: $('keyword').value.trim(), planId: $('planFilter').value, status: $('statusFilter').value };
}

function hasFilter() {
  const f = filters();
  return Boolean(f.keyword || f.planId || f.status);
}

async function loadPlans() {
  try {
    const plans = await API.managerMembership.getPlans();
    $('planFilter').innerHTML = '<option value="">Tất cả gói</option>'
      + plans.map((p) => `<option value="${p.planId}">${escapeHtml(p.planName)}</option>`).join('');
  } catch (e) {
    // Bộ lọc gói không bắt buộc; danh sách vẫn tải được
  }
}

async function load(page = 0) {
  show('loadingState');
  try {
    const data = await API.managerMembership.getTutorMemberships({ ...filters(), page, size: PAGE_SIZE });
    state.page = data.page;
    $('rowsBody').innerHTML = data.items.map((row) => `
      <tr class="border-t border-surface-container">
        <td class="px-4 py-3"><p class="font-semibold">${escapeHtml(row.tutorName)}</p><p class="text-on-surface-variant">${escapeHtml(row.email)}</p></td>
        <td class="px-4 py-3">${escapeHtml(row.planName)}</td>
        <td class="px-4 py-3">${formatDate(row.registeredAt)}</td>
        <td class="px-4 py-3">${formatDate(row.expiresOn)}</td>
        <td class="px-4 py-3">${statusBadge(row.status, row.statusLabel)}</td>
        <td class="px-4 py-3 text-right"><button type="button" class="tv-btn-small" data-detail="${row.tutorId}">Xem chi tiết</button></td>
      </tr>`).join('');
    const empty = data.items.length === 0;
    $('emptyState').classList.toggle('hidden', !empty);
    $('emptyState').textContent = hasFilter()
      ? 'Không tìm thấy kết quả phù hợp. Vui lòng điều chỉnh bộ lọc.'
      : 'Chưa có gia sư nào đăng ký gói hội viên.';
    renderPager(data);
    $('rowsBody').querySelectorAll('[data-detail]').forEach((b) => b.addEventListener('click', () => openDetail(b.dataset.detail)));
    show('contentState');
  } catch (err) {
    $('errorMessage').textContent = errorText(err);
    show('errorState');
  }
}

function renderPager(data) {
  if (data.totalPages <= 1) {
    $('pager').innerHTML = data.totalItems ? `<span class="text-on-surface-variant">Tổng ${data.totalItems} gia sư</span>` : '';
    return;
  }
  $('pager').innerHTML = `
    <span class="text-on-surface-variant">Tổng ${data.totalItems} gia sư</span>
    <div class="flex items-center gap-2">
      <button type="button" class="tv-btn-small" data-page="${data.page - 1}" ${data.page <= 0 ? 'disabled' : ''}>Trang trước</button>
      <span>Trang ${data.page + 1}/${data.totalPages}</span>
      <button type="button" class="tv-btn-small" data-page="${data.page + 1}" ${data.page + 1 >= data.totalPages ? 'disabled' : ''}>Trang sau</button>
    </div>`;
  $('pager').querySelectorAll('[data-page]').forEach((b) => b.addEventListener('click', () => load(Number(b.dataset.page))));
}

async function openDetail(tutorId) {
  $('detailDrawer').classList.remove('hidden');
  const body = $('detailBody');
  body.innerHTML = '<div class="tv-skeleton h-48 rounded-xl"></div>';
  try {
    const d = await API.managerMembership.getTutorMembershipDetail(tutorId);
    const memberships = d.memberships.map((m) => `
      <tr class="border-t border-surface-container">
        <td class="py-2 pr-3">${escapeHtml(m.planName)}</td>
        <td class="py-2 pr-3">${formatDate(m.registeredAt)}</td>
        <td class="py-2 pr-3">${formatDate(m.startDate)} – ${formatDate(m.endDate)}</td>
        <td class="py-2 pr-3 text-right">${formatMoney(m.amount)}</td>
        <td class="py-2">${statusBadge(m.status, m.statusLabel)}</td>
      </tr>`).join('');
    let payments;
    if (d.paymentsUnavailable) {
      payments = '<p class="text-body-sm text-error">Lịch sử thanh toán tạm thời không khả dụng.</p>';
    } else if (!d.payments || d.payments.length === 0) {
      payments = '<p class="text-body-sm text-on-surface-variant">Chưa có giao dịch.</p>';
    } else {
      payments = `<table class="w-full text-left text-body-sm"><tbody>${d.payments.map((p) => `
        <tr class="border-t border-surface-container">
          <td class="py-2 pr-3">${formatDateTime(p.transactionTime)}</td>
          <td class="py-2 pr-3">${escapeHtml(p.planName)}</td>
          <td class="py-2 pr-3 text-right">${formatMoney(p.amount)}</td>
          <td class="py-2">${statusBadge(p.status, p.statusLabel)}</td>
        </tr>`).join('')}</tbody></table>`;
    }
    body.innerHTML = `
      <div class="rounded-xl bg-surface-container-low p-4">
        <p class="text-title-md font-semibold">${escapeHtml(d.tutor.fullName)}</p>
        <p class="text-body-sm text-on-surface-variant">${escapeHtml(d.tutor.email)}${d.tutor.phone ? ' · ' + escapeHtml(d.tutor.phone) : ''}</p>
        <p class="text-body-sm mt-2">Trạng thái gia sư: <strong>${escapeHtml(d.tutor.tutorStateLabel)}</strong></p>
        <p class="text-body-sm">Hiệu lực đến: <strong>${d.expiresOn ? formatDate(d.expiresOn) : 'Không còn gói hiệu lực'}</strong></p>
      </div>
      <h3 class="text-title-md mt-6 mb-2">Các lần đăng ký</h3>
      ${memberships ? `<div class="overflow-x-auto"><table class="w-full text-left text-body-sm min-w-[480px]"><tbody>${memberships}</tbody></table></div>`
        : '<p class="text-body-sm text-on-surface-variant">Chưa có lần đăng ký nào.</p>'}
      <h3 class="text-title-md mt-6 mb-2">Lịch sử thanh toán</h3>
      ${payments}`;
  } catch (err) {
    body.innerHTML = `<p class="text-body-md text-error">${escapeHtml(errorText(err))}</p>
      <button type="button" class="tv-btn mt-3" id="detailRetry">Thử lại</button>`;
    $('detailRetry').addEventListener('click', () => openDetail(tutorId));
  }
}

function closeDetail() {
  $('detailDrawer').classList.add('hidden');
}

if (mountStaffLayout('manager', 'tutorMemberships', 'Hội viên gia sư')) {
$('filterForm').addEventListener('submit', (event) => { event.preventDefault(); load(0); });
$('resetButton').addEventListener('click', () => {
  $('keyword').value = '';
  $('planFilter').value = '';
  $('statusFilter').value = '';
  load(0);
});
$('retryButton').addEventListener('click', () => load(state.page));
$('closeDetail').addEventListener('click', closeDetail);
$('detailDrawer').addEventListener('click', (event) => { if (event.target.id === 'detailDrawer') closeDetail(); });
document.addEventListener('keydown', (event) => { if (event.key === 'Escape') closeDetail(); });
loadPlans();
load(0);
}
