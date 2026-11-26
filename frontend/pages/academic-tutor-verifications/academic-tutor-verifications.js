/**
 * KizunaTutor - Hồ sơ xác thực gia sư (FE-03: UC-25) cho cán bộ học vụ.
 * Bố cục giống trang "Duyệt bài đăng": tab trạng thái có số đếm, tìm kiếm / lọc, danh sách, phân trang.
 * Bấm một hồ sơ để mở trang thẩm định chi tiết (/academic/verifications/detail).
 * Dữ liệu lấy từ /api/academic/verifications (danh sách) và /summary (số đếm của tab).
 */

import { API, ApiError } from '/js/api.js';
import { debounce } from '/js/main.js';
import { mountAcademicLayout, refreshAcademicNavCounts } from '/components/academic-layout/academic-layout.js';
import { renderPagination, PAGE_SIZE_OPTIONS } from '/components/pagination/pagination.js';

const MSG_NO_MATCH = 'Không tìm thấy hồ sơ phù hợp. Vui lòng thử lại với từ khóa hoặc bộ lọc khác.';

const STATUS_STYLE = {
  PENDING: 'bg-tertiary-fixed text-tertiary',
  PROFILE_UPDATE: 'bg-tertiary-fixed text-tertiary',
  ADDITIONAL_INFO_REQUIRED: 'bg-secondary-fixed text-on-secondary-fixed',
  APPROVED: 'bg-surface-container-highest text-primary',
  REJECTED: 'bg-error-container text-error'
};

const EMPTY_TEXT = {
  PENDING: { icon: 'task_alt', title: 'Hiện không còn hồ sơ nào chờ xét duyệt.' },
  ADDITIONAL_INFO_REQUIRED: { icon: 'inbox', title: 'Không có hồ sơ nào đang chờ gia sư bổ sung.' },
  APPROVED: { icon: 'inbox', title: 'Chưa có hồ sơ nào được phê duyệt.' },
  REJECTED: { icon: 'inbox', title: 'Chưa có hồ sơ nào bị từ chối.' },
  ALL: { icon: 'inbox', title: 'Chưa có hồ sơ xác thực nào.' }
};

const state = { status: 'PENDING', page: 0, size: PAGE_SIZE_OPTIONS[0] };
const $ = (id) => document.getElementById(id);

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text == null ? '' : String(text);
  return div.innerHTML;
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

function renderTabs() {
  document.querySelectorAll('.status-tab').forEach((tab) => {
    const active = tab.dataset.status === state.status;
    tab.className = `status-tab px-3 py-1.5 rounded-full text-label-sm transition-colors ${active
      ? 'bg-primary text-on-primary shadow-sm'
      : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container'}`;
    tab.setAttribute('aria-selected', active ? 'true' : 'false');
  });
}

/** Số đếm trên tab; "Chờ xét duyệt" gồm cả yêu cầu cập nhật hồ sơ của gia sư đã phê duyệt. */
async function loadSummary() {
  refreshAcademicNavCounts();
  try {
    const s = await API.academicVerification.getSummary();
    const counts = {
      pending: (s.pendingCount || 0) + (s.profileUpdatePendingCount || 0),
      needsInfo: s.needsInfoCount || 0,
      approved: s.approvedCount || 0,
      rejected: s.rejectedCount || 0
    };
    document.querySelectorAll('[data-count]').forEach((el) => {
      const n = counts[el.dataset.count] || 0;
      el.textContent = n ? `(${n})` : '';
    });
  } catch (e) {
    // Số đếm chỉ để tham khảo
  }
}

function currentFilters() {
  return {
    status: state.status,
    keyword: $('keywordInput').value.trim(),
    jlptLevel: $('levelSelect').value,
    submittedFrom: $('fromInput').value,
    submittedTo: $('toInput').value,
    sort: $('sortSelect').value,
    page: state.page,
    size: state.size
  };
}

function itemHtml(item) {
  const overdue = (item.status === 'PENDING' || item.status === 'PROFILE_UPDATE') && item.submittedAt
    && (Date.now() - new Date(item.submittedAt).getTime()) > 24 * 3600 * 1000;
  const primary = item.status === 'PENDING' || item.status === 'PROFILE_UPDATE';
  const contact = [item.email, item.phone].filter(Boolean).map(escapeHtml).join(' · ');
  const submitted = item.submittedAt
    ? `Gửi lúc ${formatDateTime(item.submittedAt)} <span class="${overdue ? 'text-error font-medium' : ''}">(${relativeTime(item.submittedAt)}${overdue ? ', quá 24 giờ' : ''})</span>`
    : 'Chưa có ngày nộp';
  return `
    <li class="px-6 py-4 border-t border-surface-container first:border-t-0 flex flex-col md:flex-row md:items-center justify-between gap-3 hover:bg-surface-container-low/60 transition-colors">
      <div class="flex items-start gap-3 min-w-0">
        <div class="w-10 h-10 rounded-full bg-primary-fixed text-on-primary-fixed-variant flex items-center justify-center text-label-sm shadow-sm shrink-0">${escapeHtml(initialsOf(item.fullName))}</div>
        <div class="min-w-0">
          <div class="flex flex-wrap items-center gap-2">
            <span class="text-body-md font-bold text-on-surface">${escapeHtml(item.fullName)}</span>
            <span class="text-caption text-outline">${item.highestJlptLevel ? `JLPT ${escapeHtml(item.highestJlptLevel)}` : 'Chưa khai báo JLPT'}</span>
            <span class="px-2 py-0.5 rounded-full text-caption font-semibold ${STATUS_STYLE[item.status] || STATUS_STYLE.PENDING}">${escapeHtml(item.statusLabel)}</span>
          </div>
          ${contact ? `<p class="text-caption text-outline mt-0.5 truncate">${contact}</p>` : ''}
          <p class="text-caption text-outline mt-0.5">Kinh nghiệm ${item.experienceYears ?? 0} năm · ${submitted}</p>
        </div>
      </div>
      <a href="/academic/verifications/detail?tutorId=${encodeURIComponent(item.tutorId)}"
         class="shrink-0 self-start md:self-auto inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg ${primary ? 'bg-primary text-on-primary hover:bg-primary-container shadow-sm' : 'bg-surface-container text-on-surface hover:bg-surface-container-high'} text-label-sm">
        <span class="material-symbols-outlined text-[18px]">visibility</span>${primary ? 'Thẩm định hồ sơ' : 'Xem chi tiết'}
      </a>
    </li>`;
}

function showState({ icon, title, retry = false }) {
  $('tutorList').innerHTML = '';
  $('listStateIcon').textContent = icon;
  $('listStateTitle').textContent = title;
  $('retryButton').classList.toggle('hidden', !retry);
  $('listState').classList.remove('hidden');
}

async function load() {
  const filters = currentFilters();
  $('filterError').classList.add('hidden');
  if (filters.submittedFrom && filters.submittedTo && filters.submittedFrom > filters.submittedTo) {
    $('filterError').textContent = 'Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.';
    $('filterError').classList.remove('hidden');
    return;
  }
  $('listState').classList.add('hidden');
  $('tutorList').innerHTML = Array.from({ length: 3 }).map(() =>
    '<li class="px-6 py-3"><div class="tv-skeleton h-16"></div></li>').join('');
  try {
    const page = await API.academicVerification.getQueue(filters);
    const items = page.content || [];
    if (items.length === 0) {
      const narrowed = filters.keyword || filters.jlptLevel || filters.submittedFrom || filters.submittedTo;
      showState(narrowed ? { icon: 'search_off', title: MSG_NO_MATCH } : (EMPTY_TEXT[state.status] || EMPTY_TEXT.ALL));
    } else {
      $('tutorList').innerHTML = items.map(itemHtml).join('');
    }
    renderPagination($('pager'), page, {
      onPage: (p) => { state.page = p; load(); },
      onSize: (s) => { state.size = s; state.page = 0; load(); }
    });
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return;
    $('pager').innerHTML = '';
    showState({ icon: 'cloud_off', title: err instanceof ApiError ? err.message : 'Không thể tải danh sách hồ sơ.', retry: true });
  }
}

function reload() {
  state.page = 0;
  load();
}

mountAcademicLayout('verifications', 'Hồ sơ xác thực gia sư');

document.addEventListener('DOMContentLoaded', () => {
  // Mở từ thẻ số liệu ở trang Tổng quan: ?status=ADDITIONAL_INFO_REQUIRED ...
  const fromUrl = new URLSearchParams(window.location.search).get('status');
  if (fromUrl && document.querySelector(`.status-tab[data-status="${CSS.escape(fromUrl)}"]`)) state.status = fromUrl;
  renderTabs();
  $('statusTabs').addEventListener('click', (e) => {
    const tab = e.target.closest('.status-tab');
    if (!tab || tab.dataset.status === state.status) return;
    state.status = tab.dataset.status;
    renderTabs();
    reload();
  });
  $('keywordInput').addEventListener('input', debounce(reload, 300));
  ['levelSelect', 'sortSelect', 'fromInput', 'toInput'].forEach((id) => $(id).addEventListener('change', reload));
  $('toggleDateButton').addEventListener('click', () => $('dateRow').classList.toggle('hidden'));
  $('clearFiltersButton').addEventListener('click', () => {
    $('keywordInput').value = '';
    $('levelSelect').value = '';
    $('fromInput').value = '';
    $('toInput').value = '';
    reload();
  });
  $('filterForm').addEventListener('submit', (e) => { e.preventDefault(); reload(); });
  $('retryButton').addEventListener('click', load);
  loadSummary();
  load();
});
