/**
 * KizunaTutor - Tổng quan học vụ & hàng đợi hồ sơ xác minh (FE-03: UC-25, III.3.1.1)
 * Bố cục theo thiết kế Stitch "Cổng học vụ". Mọi số liệu lấy từ /api/academic/verifications.
 */

import { API, ApiError } from '/js/api.js';
import { debounce } from '/js/main.js';
import { mountAcademicLayout } from '/components/academic-layout/academic-layout.js';

const PAGE_SIZE = 10;
const MSG01 = 'Không tìm thấy kết quả phù hợp. Vui lòng thử lại với từ khóa hoặc bộ lọc khác.';

const STATUS_STYLE = {
  PENDING: { badge: 'bg-tertiary-fixed text-tertiary', dot: 'bg-tertiary' },
  ADDITIONAL_INFO_REQUIRED: { badge: 'bg-secondary-fixed text-on-secondary-fixed', dot: 'bg-secondary' },
  APPROVED: { badge: 'bg-surface-container-highest text-primary', dot: 'bg-primary' },
  REJECTED: { badge: 'bg-error-container text-error', dot: 'bg-error' }
};

const state = { page: 0 };
const $ = (id) => document.getElementById(id);

// =========================================================================
// 1. TIỆN ÍCH
// =========================================================================
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text == null ? '' : String(text);
  return div.innerHTML;
}

function formatNumber(value) {
  return new Intl.NumberFormat('vi-VN').format(value || 0);
}

function pad(n) {
  return String(n).padStart(2, '0');
}

function formatDateTime(value) {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function relativeTime(value) {
  if (!value) return '';
  const diffMs = Date.now() - new Date(value).getTime();
  const minutes = Math.max(0, Math.floor(diffMs / 60000));
  if (minutes < 60) return `${minutes || 1} phút trước`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} giờ trước`;
  return `${Math.floor(hours / 24)} ngày trước`;
}

function initialsOf(name) {
  const parts = String(name || '').trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return 'GS';
  return ((parts[0][0] || '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
}

function nowText() {
  const d = new Date();
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

// =========================================================================
// 2. SỐ LIỆU TỔNG QUAN
// =========================================================================
async function loadSummary() {
  $('summaryError').classList.add('hidden');
  try {
    const s = await API.academicVerification.getSummary();
    renderSummary(s);
    $('updatedAtText').textContent = nowText();
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return;
    $('summaryErrorText').textContent = err instanceof ApiError ? err.message : 'Không thể tải số liệu tổng quan.';
    $('summaryError').classList.remove('hidden');
  }
}

function renderSummary(s) {
  $('kpiPending').textContent = formatNumber(s.pendingCount);
  $('kpiPendingBadge').textContent = s.pendingOver24h > 0
    ? `${formatNumber(s.pendingOver24h)} hồ sơ chờ quá 24 giờ`
    : `${formatNumber(s.pendingNewToday)} hồ sơ mới hôm nay`;
  $('kpiNeedsInfo').textContent = formatNumber(s.needsInfoCount);
  $('kpiApproved').textContent = formatNumber(s.approvedCount);
  $('kpiApprovedBadge').textContent = `${formatNumber(s.approvedThisMonth)} trong tháng này`;
  $('kpiRejected').textContent = formatNumber(s.rejectedCount);
  $('kpiRejectedBadge').textContent = `${formatNumber(s.rejectedThisMonth)} trong tháng này`;

  $('approvedMonthText').textContent = formatNumber(s.approvedThisMonth);
  $('rejectedMonthText').textContent = formatNumber(s.rejectedThisMonth);
  $('totalTutorsText').textContent = formatNumber(s.totalTutors);
  $('notSubmittedText').textContent = `${formatNumber(s.notSubmittedCount)} gia sư chưa nộp hồ sơ xác minh (không tính trong biểu đồ).`;

  renderDistribution(s);
}

function renderDistribution(s) {
  const parts = [
    { label: 'Đã duyệt', value: s.approvedCount, color: 'bg-primary', note: 'tổng cộng' },
    { label: 'Chờ xét duyệt', value: s.pendingCount, color: 'bg-tertiary', note: 'đang chờ' },
    { label: 'Cần bổ sung', value: s.needsInfoCount, color: 'bg-secondary', note: 'chờ gia sư' },
    { label: 'Đã từ chối', value: s.rejectedCount, color: 'bg-error', note: 'không đạt' }
  ];
  const total = parts.reduce((sum, p) => sum + (p.value || 0), 0);
  const percent = (v) => (total === 0 ? 0 : Math.round((v / total) * 1000) / 10);

  $('distributionBar').innerHTML = total === 0
    ? '<div class="h-full w-full rounded-full bg-surface-container"></div>'
    : parts.filter((p) => p.value > 0).map((p) =>
        `<div class="${p.color} h-full rounded-full transition-all duration-500" style="width:${percent(p.value)}%" title="${p.label}: ${percent(p.value)}%"></div>`
      ).join('');

  $('distributionList').innerHTML = parts.map((p) => `
    <li class="flex items-center justify-between gap-3 px-3 py-2 rounded-lg bg-surface-container-low">
      <span class="flex items-center gap-2 min-w-0">
        <span class="w-2.5 h-2.5 rounded-full ${p.color} inline-block shrink-0"></span>
        <span class="text-body-sm text-on-surface truncate">${p.label}</span>
      </span>
      <span class="flex items-baseline gap-2 shrink-0">
        <span class="text-title-sm font-bold text-on-surface">${formatNumber(p.value)}</span>
        <span class="text-caption text-outline w-12 text-right">${percent(p.value)}%</span>
      </span>
    </li>`).join('');
}

/** Làm nổi thẻ chỉ số ứng với trạng thái đang lọc. */
function highlightKpi() {
  const status = $('statusSelect').value;
  document.querySelectorAll('[data-kpi-filter]').forEach((card) => {
    const active = card.dataset.kpiFilter === status;
    card.classList.toggle('ring-2', active);
    card.classList.toggle('ring-primary', active);
    card.setAttribute('aria-pressed', active ? 'true' : 'false');
  });
}

// =========================================================================
// 3. HÀNG ĐỢI HỒ SƠ
// =========================================================================
function currentFilters() {
  return {
    status: $('statusSelect').value,
    keyword: $('keywordInput').value.trim(),
    jlptLevel: $('levelSelect').value,
    submittedFrom: $('fromInput').value,
    submittedTo: $('toInput').value,
    sort: $('sortSelect').value,
    page: state.page,
    size: PAGE_SIZE
  };
}

function hasNarrowingFilters(f) {
  return Boolean(f.keyword || f.jlptLevel || f.submittedFrom || f.submittedTo);
}

function showQueueState({ icon, title, text, retry = false }) {
  $('queueBody').innerHTML = '';
  $('queueStateIcon').textContent = icon;
  $('queueStateTitle').textContent = title;
  $('queueStateText').textContent = text || '';
  $('queueRetryButton').classList.toggle('hidden', !retry);
  $('queueState').classList.remove('hidden');
}

async function loadQueue() {
  const filters = currentFilters();
  $('queueState').classList.add('hidden');
  $('queueBody').innerHTML = Array.from({ length: 3 }).map(() =>
    '<tr><td colspan="5" class="px-6 py-3"><div class="tv-skeleton h-10"></div></td></tr>').join('');
  try {
    const page = await API.academicVerification.getQueue(filters);
    $('filteredTotal').textContent = formatNumber(page.totalElements);
    if (!page.content || page.content.length === 0) {
      if (hasNarrowingFilters(filters)) {
        showQueueState({ icon: 'search_off', title: MSG01 });
      } else if (filters.status === 'PENDING') {
        showQueueState({ icon: 'task_alt', title: 'Hiện không còn hồ sơ nào chờ thẩm định.' });
      } else {
        showQueueState({ icon: 'inbox', title: 'Chưa có hồ sơ nào ở trạng thái này.' });
      }
      renderPagination(page);
      return;
    }
    $('queueBody').innerHTML = page.content.map(rowHtml).join('');
    renderPagination(page);
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return;
    $('filteredTotal').textContent = '0';
    showQueueState({ icon: 'cloud_off', title: 'Không thể tải danh sách hồ sơ.', text: err instanceof ApiError ? err.message : '', retry: true });
    $('pageInfo').textContent = '';
    $('pagination').innerHTML = '';
  }
}

function rowHtml(item) {
  const style = STATUS_STYLE[item.status] || STATUS_STYLE.PENDING;
  const overdue = item.status === 'PENDING' && item.submittedAt && (Date.now() - new Date(item.submittedAt).getTime()) > 24 * 3600 * 1000;
  const waitText = item.submittedAt
    ? `<span class="text-caption ${overdue ? 'text-error font-medium' : 'text-outline'}">${relativeTime(item.submittedAt)}${overdue ? ' (quá 24 giờ)' : ''}</span>`
    : '<span class="text-caption text-outline">Chưa có ngày nộp</span>';
  const primary = item.status === 'PENDING';
  const contact = [item.email, item.phone].filter(Boolean).map(escapeHtml).join(' • ');
  return `
    <tr class="hover:bg-surface-container-low/60 transition-colors">
      <td class="py-4 px-6">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-primary-fixed text-on-primary-fixed-variant flex items-center justify-center text-label-sm shadow-sm flex-shrink-0">${escapeHtml(initialsOf(item.fullName))}</div>
          <div class="flex flex-col min-w-0">
            <span class="text-body-md font-bold text-on-surface truncate">${escapeHtml(item.fullName)}</span>
            <span class="text-caption text-outline truncate">${contact}</span>
          </div>
        </div>
      </td>
      <td class="py-4 px-6">
        <div class="flex flex-col gap-1">
          <span class="font-medium text-on-surface">${item.highestJlptLevel ? `JLPT ${escapeHtml(item.highestJlptLevel)}` : 'Chưa khai báo JLPT'}</span>
          <span class="text-caption text-outline">Kinh nghiệm ${item.experienceYears ?? 0} năm</span>
        </div>
      </td>
      <td class="py-4 px-6">
        <div class="flex flex-col">
          <span class="font-medium text-on-surface">${formatDateTime(item.submittedAt)}</span>
          ${waitText}
        </div>
      </td>
      <td class="py-4 px-6">
        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full ${style.badge} text-caption font-semibold">
          <span class="w-1.5 h-1.5 rounded-full ${style.dot}"></span>${escapeHtml(item.statusLabel)}
        </span>
      </td>
      <td class="py-4 px-6 text-right">
        <a href="/academic/verifications/detail?tutorId=${encodeURIComponent(item.tutorId)}"
           class="inline-block px-3 py-1.5 rounded-lg ${primary ? 'bg-primary text-on-primary hover:bg-primary-container shadow-sm' : 'bg-surface-container text-on-surface hover:bg-surface-container-high'} text-label-sm transition-colors">
          ${primary ? 'Xem hồ sơ' : 'Chi tiết'}
        </a>
      </td>
    </tr>`;
}

function renderPagination(page) {
  const total = page.totalElements || 0;
  const from = total === 0 ? 0 : page.page * page.size + 1;
  const to = Math.min(total, (page.page + 1) * page.size);
  $('pageInfo').innerHTML = `Hiển thị <strong class="text-on-surface font-semibold">${from} - ${to}</strong> trong tổng số <strong class="text-on-surface font-semibold">${formatNumber(total)}</strong> hồ sơ`;

  const totalPages = page.totalPages || 0;
  if (totalPages <= 1) {
    $('pagination').innerHTML = '';
    return;
  }
  const current = page.page;
  const pages = new Set([0, totalPages - 1, current - 1, current, current + 1]);
  const sorted = [...pages].filter((p) => p >= 0 && p < totalPages).sort((a, b) => a - b);
  const btn = (label, target, disabled, active = false) => `
    <button type="button" data-page="${target}" ${disabled ? 'disabled' : ''}
      class="h-8 min-w-[2rem] px-2.5 rounded-lg ${active ? 'bg-primary text-on-primary shadow-sm' : 'bg-surface-container-lowest hover:bg-surface-container text-on-surface'} ${disabled ? 'text-outline-variant cursor-not-allowed hover:bg-surface-container-lowest' : ''} text-label-sm flex items-center justify-center">${label}</button>`;
  let html = btn('<span class="material-symbols-outlined text-[16px]">chevron_left</span>Trước', current - 1, current === 0);
  let last = -1;
  sorted.forEach((p) => {
    if (last >= 0 && p - last > 1) html += '<span class="px-1 text-outline text-label-sm">...</span>';
    html += btn(String(p + 1), p, false, p === current);
    last = p;
  });
  html += btn('Tiếp<span class="material-symbols-outlined text-[16px]">chevron_right</span>', current + 1, current >= totalPages - 1);
  $('pagination').innerHTML = html;
}

function applyFilters() {
  state.page = 0;
  highlightKpi();
  loadQueue();
}

function focusQueue(status) {
  if (status) $('statusSelect').value = status;
  applyFilters();
  // Bảng nằm ngay dưới thẻ chỉ số trên màn hình rộng; chỉ cuộn khi bảng đang khuất
  const top = $('queue').getBoundingClientRect().top;
  if (top > window.innerHeight * 0.6 || top < 0) {
    $('queue').scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

// =========================================================================
// 4. KHỞI TẠO
// =========================================================================
function bindEvents() {
  $('keywordInput').addEventListener('input', debounce(applyFilters, 300));
  ['statusSelect', 'levelSelect', 'sortSelect', 'fromInput', 'toInput'].forEach((id) => $(id).addEventListener('change', applyFilters));
  $('filterForm').addEventListener('submit', (e) => { e.preventDefault(); applyFilters(); });
  $('toggleDateButton').addEventListener('click', () => $('dateRow').classList.toggle('hidden'));
  $('clearFiltersButton').addEventListener('click', () => {
    $('keywordInput').value = '';
    $('levelSelect').value = '';
    $('fromInput').value = '';
    $('toInput').value = '';
    $('sortSelect').value = 'oldest';
    applyFilters();
  });
  $('pagination').addEventListener('click', (e) => {
    const button = e.target.closest('button[data-page]');
    if (!button || button.disabled) return;
    state.page = Number(button.dataset.page);
    loadQueue();
  });
  $('queueRetryButton').addEventListener('click', loadQueue);
  document.querySelectorAll('[data-kpi-filter]').forEach((card) => card.addEventListener('click', () => focusQueue(card.dataset.kpiFilter)));
  $('refreshButton').addEventListener('click', () => { loadSummary(); loadQueue(); });
  $('summaryRetryButton').addEventListener('click', loadSummary);
}

const onQueuePage = window.location.pathname.startsWith('/academic/verifications');
mountAcademicLayout(onQueuePage ? 'verifications' : 'dashboard', onQueuePage ? 'Hồ sơ xác thực gia sư' : 'Tổng quan học vụ');

document.addEventListener('DOMContentLoaded', () => {
  bindEvents();
  highlightKpi();
  loadSummary();
  loadQueue();
  if (window.location.hash === '#queue') {
    setTimeout(() => $('queue').scrollIntoView({ behavior: 'smooth', block: 'start' }), 300);
  }
});
