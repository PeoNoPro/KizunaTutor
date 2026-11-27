/**
 * KizunaTutor - Tổng quan học vụ (FE-03 + FE-09): gộp hồ sơ xác thực gia sư và bài đăng tuyển học viên.
 * - 4 thẻ chỉ số cộng cả hai loại đơn (bấm để lọc bảng).
 * - Bảng "Đơn cần xử lý" gộp hai danh sách (mỗi loại tối đa 50 đơn), phân trang trên trình duyệt.
 * - Cột thống kê: tình trạng xác thực, tình trạng bài đăng, hồ sơ trong tháng.
 * Trang "Hồ sơ xác thực gia sư" riêng nằm ở /pages/academic-tutor-verifications/.
 */

import { API, ApiError } from '/js/api.js';
import { debounce } from '/js/main.js';
import { mountAcademicLayout } from '/components/academic-layout/academic-layout.js';
import { renderPagination as renderPaginationBar, PAGE_SIZE_OPTIONS } from '/components/pagination/pagination.js';
import { STATUS_PARAMS, mergeRows, kpiCounts, breakdownText } from '/js/academic-dashboard-helpers.js';

// Đường dẫn cũ của trang hồ sơ (/academic/verifications?view=queue) chuyển sang trang riêng
if (new URLSearchParams(window.location.search).get('view') === 'queue') {
  window.location.replace('/academic/verifications');
}

const MSG01 = 'Không tìm thấy kết quả phù hợp. Vui lòng thử lại với từ khóa hoặc bộ lọc khác.';
const FETCH_LIMIT = 50;

const TONE_STYLE = {
  PENDING: { badge: 'bg-tertiary-fixed text-tertiary', dot: 'bg-tertiary' },
  NEEDS_INFO: { badge: 'bg-secondary-fixed text-on-secondary-fixed', dot: 'bg-secondary' },
  APPROVED: { badge: 'bg-surface-container-highest text-primary', dot: 'bg-primary' },
  REJECTED: { badge: 'bg-error-container text-error', dot: 'bg-error' }
};
const TYPE_STYLE = {
  VERIFICATION: { badge: 'bg-primary-fixed text-primary', icon: 'verified_user' },
  POST: { badge: 'bg-secondary-fixed text-on-secondary-fixed', icon: 'campaign' }
};

const state = { page: 0, size: PAGE_SIZE_OPTIONS[0], rows: [], token: 0, type: 'ALL' };
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

const pad = (n) => String(n).padStart(2, '0');
function formatDateTime(value) {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function relativeTime(value) {
  if (!value) return '';
  const minutes = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 60000));
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

// =========================================================================
// 2. SỐ LIỆU TỔNG QUAN (cả hai loại đơn)
// =========================================================================
async function loadSummary() {
  $('summaryError').classList.add('hidden');
  const [verification, posts] = await Promise.allSettled([
    API.academicVerification.getSummary(),
    API.academicPosts.getSummary()
  ]);
  const failed = [verification, posts].find((r) => r.status === 'rejected');
  if (failed && failed.reason instanceof ApiError && failed.reason.status === 401) return;
  const v = verification.status === 'fulfilled' ? verification.value : null;
  const p = posts.status === 'fulfilled' ? posts.value : null;
  if (!v && !p) {
    $('summaryErrorText').textContent = failed && failed.reason instanceof ApiError ? failed.reason.message : 'Không thể tải số liệu tổng quan.';
    $('summaryError').classList.remove('hidden');
    return;
  }
  renderKpis(v, p);
  if (v) renderVerificationStats(v);
  if (p) renderPostStats(p);
  if (failed) {
    $('summaryErrorText').textContent = v ? 'Chưa tải được số liệu bài đăng.' : 'Chưa tải được số liệu hồ sơ xác thực.';
    $('summaryError').classList.remove('hidden');
  }
}

function renderKpis(v, p) {
  const k = kpiCounts(v, p);
  [['kpiPending', k.pending], ['kpiNeedsInfo', k.needsInfo], ['kpiApproved', k.approved], ['kpiRejected', k.rejected]]
    .forEach(([id, count]) => {
      $(id).textContent = formatNumber(count.total);
      $(`${id}Breakdown`).textContent = breakdownText(count) || 'Không có đơn';
    });
  const overdue = v && v.pendingOver24h > 0 ? `${formatNumber(v.pendingOver24h)} hồ sơ chờ quá 24 giờ` : '';
  $('kpiPendingNote').textContent = overdue;
  $('kpiPendingNote').classList.toggle('hidden', !overdue);
}

function renderVerificationStats(s) {
  $('approvedMonthText').textContent = formatNumber(s.approvedThisMonth);
  $('rejectedMonthText').textContent = formatNumber(s.rejectedThisMonth);
  $('totalTutorsText').textContent = formatNumber(s.totalTutors);
  renderDistribution('distributionBar', 'distributionList', [
    { label: 'Đã duyệt', value: s.approvedCount, color: 'bg-primary' },
    { label: 'Chờ xét duyệt', value: (s.pendingCount || 0) + (s.profileUpdatePendingCount || 0), color: 'bg-tertiary' },
    { label: 'Cần bổ sung', value: s.needsInfoCount, color: 'bg-secondary' },
    { label: 'Đã từ chối', value: s.rejectedCount, color: 'bg-error' }
  ]);
}

function renderPostStats(p) {
  const parts = [
    { label: 'Đang hiển thị', value: p.approvedCount, color: 'bg-primary' },
    { label: 'Chờ duyệt', value: p.pendingCount, color: 'bg-tertiary' },
    { label: 'Cần bổ sung', value: p.needsInfoCount, color: 'bg-secondary' },
    { label: 'Bị từ chối', value: p.rejectedCount, color: 'bg-error' }
  ];
  $('totalPostsText').textContent = formatNumber(parts.reduce((sum, x) => sum + (x.value || 0), 0));
  renderDistribution('postDistributionBar', 'postDistributionList', parts);
}

function renderDistribution(barId, listId, parts) {
  const total = parts.reduce((sum, p) => sum + (p.value || 0), 0);
  const percent = (v) => (total === 0 ? 0 : Math.round(((v || 0) / total) * 1000) / 10);
  $(barId).innerHTML = total === 0
    ? '<div class="h-full w-full rounded-full bg-surface-container"></div>'
    : parts.filter((p) => p.value > 0).map((p) =>
        `<div class="${p.color} h-full rounded-full transition-all duration-500" style="width:${percent(p.value)}%" title="${p.label}: ${percent(p.value)}%"></div>`
      ).join('');
  $(listId).innerHTML = parts.map((p) => `
    <li class="flex items-center justify-between gap-2 text-body-sm" title="${percent(p.value)}%">
      <span class="flex items-center gap-2 min-w-0 text-on-surface-variant">
        <span class="w-2 h-2 rounded-full ${p.color} inline-block shrink-0"></span>
        <span class="truncate">${p.label}</span>
      </span>
      <span class="font-semibold text-on-surface">${formatNumber(p.value)}</span>
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
// 3. BẢNG ĐƠN (gộp hai loại)
// =========================================================================
function showQueueState({ icon, title, text, retry = false }) {
  $('queueBody').innerHTML = '';
  $('queueStateIcon').textContent = icon;
  $('queueStateTitle').textContent = title;
  $('queueStateText').textContent = text || '';
  $('queueRetryButton').classList.toggle('hidden', !retry);
  $('queueState').classList.remove('hidden');
}

/** Tải hai danh sách theo bộ lọc rồi gộp; phân trang trên trình duyệt. */
async function loadQueue() {
  const token = ++state.token;
  const status = $('statusSelect').value;
  const type = state.type;
  const keyword = $('keywordInput').value.trim();
  const sort = $('sortSelect').value;
  const params = STATUS_PARAMS[status] || STATUS_PARAMS.ALL;
  $('queueState').classList.add('hidden');
  $('queueLimitNote').classList.add('hidden');
  $('queueBody').innerHTML = Array.from({ length: 3 }).map(() =>
    '<tr><td colspan="5" class="px-6 py-3"><div class="tv-skeleton h-10"></div></td></tr>').join('');

  const wantVerification = type !== 'POST';
  const wantPost = type !== 'VERIFICATION';
  const [vr, pr] = await Promise.allSettled([
    wantVerification ? API.academicVerification.getQueue({ status: params.verification, keyword, sort, page: 0, size: FETCH_LIMIT }) : Promise.resolve(null),
    wantPost ? API.academicPosts.list({ status: params.post, keyword, page: 0, size: FETCH_LIMIT }) : Promise.resolve(null)
  ]);
  if (token !== state.token) return;
  const errors = [vr, pr].filter((r) => r.status === 'rejected').map((r) => r.reason);
  if (errors.some((e) => e instanceof ApiError && e.status === 401)) return;
  const vPage = vr.status === 'fulfilled' ? vr.value : null;
  const pPage = pr.status === 'fulfilled' ? pr.value : null;
  if (!vPage && !pPage) {
    $('filteredTotal').textContent = '0';
    $('pager').innerHTML = '';
    showQueueState({ icon: 'cloud_off', title: 'Không thể tải danh sách đơn.', text: errors[0] instanceof ApiError ? errors[0].message : '', retry: true });
    return;
  }

  state.rows = mergeRows(vPage?.content, pPage?.content, sort);
  state.page = 0;
  const total = (vPage?.totalElements || 0) + (pPage?.totalElements || 0);
  $('filteredTotal').textContent = formatNumber(total);

  const notes = [];
  if (vPage && vPage.totalElements > (vPage.content || []).length) notes.push('Hồ sơ xác thực gia sư');
  if (pPage && pPage.totalElements > (pPage.content || []).length) notes.push('Duyệt bài đăng');
  if (errors.length) notes.push(vPage ? 'chưa tải được bài đăng' : 'chưa tải được hồ sơ xác thực');
  if (notes.length) {
    $('queueLimitNote').textContent = errors.length
      ? `Một phần dữ liệu ${notes[notes.length - 1]}. Vui lòng tải lại trang.`
      : `Bảng hiển thị tối đa ${FETCH_LIMIT} đơn mỗi loại. Xem đầy đủ ở trang ${notes.join(' và ')}.`;
    $('queueLimitNote').classList.remove('hidden');
  }
  renderRows(status, keyword);
}

function renderRows(status, keyword) {
  if (state.rows.length === 0) {
    $('pager').innerHTML = '';
    if (keyword) showQueueState({ icon: 'search_off', title: MSG01 });
    else if (status === 'PENDING') showQueueState({ icon: 'task_alt', title: 'Hiện không còn đơn nào chờ xét duyệt.' });
    else showQueueState({ icon: 'inbox', title: 'Chưa có đơn nào ở trạng thái này.' });
    return;
  }
  $('queueState').classList.add('hidden');
  const start = state.page * state.size;
  $('queueBody').innerHTML = state.rows.slice(start, start + state.size).map(rowHtml).join('');
  renderPaginationBar($('pager'), { totalElements: state.rows.length, page: state.page, size: state.size }, {
    onPage: (p) => { state.page = p; renderRows(status, keyword); },
    onSize: (s) => { state.size = s; state.page = 0; renderRows(status, keyword); }
  });
}

function rowHtml(row) {
  const tone = TONE_STYLE[row.tone] || TONE_STYLE.PENDING;
  const type = TYPE_STYLE[row.type];
  const waiting = row.tone === 'PENDING';
  const overdue = waiting && row.submittedAt && (Date.now() - new Date(row.submittedAt).getTime()) > 24 * 3600 * 1000;
  return `
    <tr class="border-t border-surface-container hover:bg-surface-container-low/60 transition-colors">
      <td class="py-3.5 px-6">
        <div class="flex items-center gap-3 min-w-0">
          <span class="w-9 h-9 rounded-lg ${type.badge} flex items-center justify-center shrink-0" title="${escapeHtml(row.typeLabel)}">
            <span class="material-symbols-outlined text-[20px]">${type.icon}</span>
          </span>
          <div class="flex flex-col min-w-0 max-w-[480px]">
            <span class="font-semibold text-on-surface truncate">${escapeHtml(row.title)}</span>
            <span class="text-caption text-outline truncate">${escapeHtml(row.typeLabel)}${row.subtitle ? ` · ${escapeHtml(row.subtitle)}` : ''}</span>
          </div>
        </div>
      </td>
      <td class="py-3.5 px-4">
        <div class="flex flex-col min-w-0 max-w-[360px]">
          <span class="font-medium text-on-surface truncate">${escapeHtml(row.tutorName)}</span>
          <span class="text-caption text-outline truncate">${escapeHtml(row.contact)}</span>
        </div>
      </td>
      <td class="py-3.5 px-4 whitespace-nowrap" title="${formatDateTime(row.submittedAt)}">
        ${row.submittedAt
          ? `<span class="${overdue ? 'text-error font-medium' : 'text-on-surface-variant'}">${relativeTime(row.submittedAt)}</span>`
          : '<span class="text-outline">—</span>'}
      </td>
      <td class="py-3.5 px-4">
        <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full ${tone.badge} text-caption font-semibold whitespace-nowrap">
          <span class="w-1.5 h-1.5 rounded-full ${tone.dot}"></span>${escapeHtml(row.statusLabel)}
        </span>
      </td>
      <td class="py-3.5 px-6 text-right">
        <a href="${row.href}"
           class="inline-block px-3 py-1.5 rounded-lg ${waiting ? 'bg-primary text-on-primary hover:bg-primary-container shadow-sm' : 'text-primary hover:bg-surface-container'} text-label-sm transition-colors whitespace-nowrap">
          ${escapeHtml(row.actionLabel)}
        </a>
      </td>
    </tr>`;
}

/** Tab loại đơn (Tất cả / Hồ sơ gia sư / Bài đăng). */
function renderTypeTabs() {
  document.querySelectorAll('.type-tab').forEach((tab) => {
    const active = tab.dataset.type === state.type;
    tab.className = `type-tab px-3 py-1.5 rounded-md text-label-sm transition-colors ${active
      ? 'bg-surface-container-lowest text-primary shadow-sm font-semibold'
      : 'text-on-surface-variant hover:text-on-surface'}`;
    tab.setAttribute('aria-selected', active ? 'true' : 'false');
  });
}

function applyFilters() {
  highlightKpi();
  loadQueue();
}

function focusQueue(status) {
  if (status) $('statusSelect').value = status;
  applyFilters();
  $('queue').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function bindEvents() {
  $('filterForm').addEventListener('submit', (event) => { event.preventDefault(); applyFilters(); });
  $('keywordInput').addEventListener('input', debounce(applyFilters, 300));
  ['statusSelect', 'sortSelect'].forEach((id) => $(id).addEventListener('change', applyFilters));
  $('typeTabs').addEventListener('click', (e) => {
    const tab = e.target.closest('.type-tab');
    if (!tab || tab.dataset.type === state.type) return;
    state.type = tab.dataset.type;
    renderTypeTabs();
    loadQueue();
  });
  $('summaryRetryButton').addEventListener('click', loadSummary);
  $('queueRetryButton').addEventListener('click', loadQueue);
  document.querySelectorAll('[data-kpi-filter]').forEach((card) => card.addEventListener('click', () => focusQueue(card.dataset.kpiFilter)));
}

mountAcademicLayout('dashboard', 'Tổng quan học vụ');

document.addEventListener('DOMContentLoaded', () => {
  bindEvents();
  renderTypeTabs();
  highlightKpi();
  loadSummary();
  loadQueue();
  if (window.location.hash === '#queue') {
    setTimeout(() => $('queue').scrollIntoView({ behavior: 'smooth', block: 'start' }), 300);
  }
});
