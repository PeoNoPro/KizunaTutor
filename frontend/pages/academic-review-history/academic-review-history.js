/**
 * KizunaTutor - Lịch sử xét duyệt của Cổng học vụ.
 * Dữ liệu thật từ GET /api/academic/reviews (các quyết định phê duyệt / từ chối / yêu cầu bổ sung).
 */

import { API, ApiError } from '/js/api.js';
import { debounce } from '/js/main.js';
import { mountAcademicLayout } from '/components/academic-layout/academic-layout.js';
import { renderPagination, PAGE_SIZE_OPTIONS } from '/components/pagination/pagination.js';

const ACTION_STYLE = {
  APPROVE: { icon: 'task_alt', box: 'bg-primary-fixed text-primary', badge: 'bg-surface-container-highest text-primary' },
  REQUEST_INFO: { icon: 'contact_support', box: 'bg-secondary-fixed text-secondary', badge: 'bg-secondary-fixed text-on-secondary-fixed' },
  REJECT: { icon: 'block', box: 'bg-error-container text-error', badge: 'bg-error-container text-error' }
};

const state = { action: 'ALL', page: 0, size: PAGE_SIZE_OPTIONS[0] };
const $ = (id) => document.getElementById(id);

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text == null ? '' : String(text);
  return div.innerHTML;
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

function formatNumber(value) {
  return new Intl.NumberFormat('vi-VN').format(value || 0);
}

function renderTabs() {
  document.querySelectorAll('.action-tab').forEach((tab) => {
    const active = tab.dataset.action === state.action;
    tab.className = `action-tab px-3 py-1.5 rounded-full text-label-sm transition-colors ${active
      ? 'bg-primary text-on-primary shadow-sm'
      : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container'}`;
    tab.setAttribute('aria-selected', active ? 'true' : 'false');
  });
}

function showState({ icon, title, retry = false }) {
  $('recordList').innerHTML = '';
  $('listStateIcon').textContent = icon;
  $('listStateTitle').textContent = title;
  $('retryButton').classList.toggle('hidden', !retry);
  $('listState').classList.remove('hidden');
}

function recordHtml(r) {
  const st = ACTION_STYLE[r.action] || ACTION_STYLE.APPROVE;
  const detail = [];
  if (r.reasonLabel) detail.push(`<p class="text-body-sm text-on-surface"><span class="text-on-surface-variant">Lý do:</span> ${escapeHtml(r.reasonLabel)}</p>`);
  if (r.feedback) detail.push(`<p class="text-body-sm text-on-surface whitespace-pre-line"><span class="text-on-surface-variant">Phản hồi gửi gia sư:</span> ${escapeHtml(r.feedback)}</p>`);
  if (r.internalNote) detail.push(`<p class="text-body-sm text-on-surface-variant whitespace-pre-line"><span class="material-symbols-outlined text-[14px] align-[-2px]">lock</span> Ghi chú nội bộ: ${escapeHtml(r.internalNote)}</p>`);
  return `
    <li class="px-6 py-4 flex gap-4 border-t border-surface-container first:border-t-0 hover:bg-surface-container-low/60 transition-colors">
      <div class="w-10 h-10 rounded-xl ${st.box} flex items-center justify-center shrink-0">
        <span class="material-symbols-outlined text-[20px]">${st.icon}</span>
      </div>
      <div class="flex-1 min-w-0">
        <div class="flex flex-wrap items-center gap-x-2 gap-y-1">
          <span class="px-2 py-0.5 rounded-full ${st.badge} text-caption font-semibold">${escapeHtml(r.actionLabel)}</span>
          <a href="/academic/verifications/detail?tutorId=${encodeURIComponent(r.tutorId)}" class="text-body-md font-bold text-on-surface hover:text-primary truncate">${escapeHtml(r.tutorName)}</a>
          ${r.tutorJlptLevel ? `<span class="text-caption text-outline">JLPT ${escapeHtml(r.tutorJlptLevel)}</span>` : ''}
        </div>
        <p class="text-caption text-outline mt-0.5">${escapeHtml(r.tutorEmail || '')}</p>
        ${detail.length ? `<div class="mt-2 flex flex-col gap-1">${detail.join('')}</div>` : ''}
      </div>
      <div class="text-right shrink-0">
        <p class="text-label-sm text-on-surface">${formatDateTime(r.createdAt)}</p>
        <p class="text-caption ${r.mine ? 'text-primary font-semibold' : 'text-outline'}">${r.mine ? 'Bạn' : escapeHtml(r.officerName || 'Không rõ')}</p>
      </div>
    </li>`;
}

function renderPager(page) {
  renderPagination($('pager'), page, {
    onPage: (p) => { state.page = p; load(); },
    onSize: (s) => { state.size = s; state.page = 0; load(); }
  });
}

async function load() {
  $('listState').classList.add('hidden');
  $('recordList').innerHTML = Array.from({ length: 3 }).map(() =>
    '<li class="px-6 py-3"><div class="tv-skeleton h-14"></div></li>').join('');
  const keyword = $('keywordInput').value.trim();
  try {
    const page = await API.academicOfficer.getReviews({
      action: state.action, keyword, page: state.page, size: state.size
    });
    $('totalText').textContent = `${formatNumber(page.totalElements)} quyết định`;
    if (!page.content || page.content.length === 0) {
      const filtered = keyword || state.action !== 'ALL';
      showState({
        icon: filtered ? 'search_off' : 'inbox',
        title: filtered ? 'Không tìm thấy quyết định phù hợp với bộ lọc.' : 'Chưa có quyết định xét duyệt nào.'
      });
    } else {
      $('recordList').innerHTML = page.content.map(recordHtml).join('');
    }
    renderPager(page);
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return;
    $('totalText').textContent = '';
    $('pager').innerHTML = '';
    showState({ icon: 'cloud_off', title: err instanceof ApiError ? err.message : 'Không thể tải lịch sử xét duyệt.', retry: true });
  }
}

function reload() {
  state.page = 0;
  load();
}

mountAcademicLayout('history', 'Lịch sử xét duyệt');

document.addEventListener('DOMContentLoaded', () => {
  renderTabs();
  $('actionTabs').addEventListener('click', (e) => {
    const tab = e.target.closest('.action-tab');
    if (!tab || tab.dataset.action === state.action) return;
    state.action = tab.dataset.action;
    renderTabs();
    reload();
  });
  $('keywordInput').addEventListener('input', debounce(reload, 300));
  $('filterForm').addEventListener('submit', (e) => { e.preventDefault(); reload(); });
  $('retryButton').addEventListener('click', load);
  load();
});
