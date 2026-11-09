/**
 * KizunaTutor - Cán bộ học vụ duyệt bài đăng của gia sư.
 * API: GET /api/academic/posts(/summary), POST /api/academic/posts/{id}/approve|reject.
 */

import { API, ApiError } from '/js/api.js';
import { debounce, showToast } from '/js/main.js';
import { mountAcademicLayout } from '/components/academic-layout/academic-layout.js';
import { renderPagination, PAGE_SIZE_OPTIONS } from '/components/pagination/pagination.js';

const STATUS_STYLE = {
  PENDING: 'bg-tertiary-fixed text-tertiary',
  NEEDS_INFO: 'bg-secondary-fixed text-on-secondary-fixed',
  APPROVED: 'bg-[#e3f4e8] text-success',
  REJECTED: 'bg-error-container text-error'
};

const state = { status: 'PENDING', page: 0, size: PAGE_SIZE_OPTIONS[0], items: [], dialog: null };

// Hai thao tác cần nhập nội dung dùng chung một hộp thoại
const DIALOGS = {
  info: {
    title: 'Yêu cầu bổ sung',
    hint: (name) => `Gia sư ${name} sẽ sửa bài theo nội dung này và gửi lại để duyệt.`,
    placeholder: 'Ví dụ: Vui lòng ghi rõ lộ trình học cho từng trình độ và cập nhật học phí theo buổi 90 phút...',
    confirm: 'Gửi yêu cầu',
    confirmClass: 'bg-secondary',
    empty: 'Vui lòng nêu rõ thông tin gia sư cần bổ sung.',
    call: (id, text) => API.academicPosts.requestInfo(id, text)
  },
  reject: {
    title: 'Từ chối bài đăng',
    hint: (name) => `Đây là quyết định cuối cùng: gia sư ${name} sẽ không gửi lại được bài này.`,
    placeholder: 'Ví dụ: Bài đăng chứa thông tin liên hệ ngoài hệ thống, nội dung không phù hợp...',
    confirm: 'Từ chối',
    confirmClass: 'bg-error',
    empty: 'Vui lòng nhập lý do từ chối.',
    call: (id, text) => API.academicPosts.reject(id, text)
  }
};
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

const formatMoney = (v) => `${new Intl.NumberFormat('vi-VN').format(Number(v || 0))} đ`;

function renderTabs() {
  document.querySelectorAll('.status-tab').forEach((tab) => {
    const active = tab.dataset.status === state.status;
    tab.className = `status-tab px-3 py-1.5 rounded-full text-label-sm transition-colors ${active
      ? 'bg-primary text-on-primary shadow-sm'
      : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container'}`;
    tab.setAttribute('aria-selected', active ? 'true' : 'false');
  });
}

async function loadSummary() {
  try {
    const s = await API.academicPosts.getSummary();
    document.querySelectorAll('[data-count]').forEach((el) => {
      const n = s[el.dataset.count] || 0;
      el.textContent = n ? `(${n})` : '';
    });
  } catch (e) {
    // Số đếm chỉ để tham khảo
  }
}

function contentBlock(label, c, tone) {
  if (!c) return '';
  const tags = (c.specialties || '').split(',').map((s) => s.trim()).filter(Boolean);
  return `
    <div class="rounded-xl p-4 ${tone}">
      <p class="text-caption text-on-surface-variant font-semibold mb-2">${label}</p>
      <p class="text-label-md font-semibold text-on-surface">${escapeHtml(c.title)}</p>
      <div class="flex flex-wrap items-center gap-2 mt-1.5 text-body-sm">
        <span class="px-2 py-0.5 rounded-full bg-primary-fixed text-primary text-caption font-semibold">${escapeHtml(c.teachingFormatLabel || '')}</span>
        <span class="font-semibold text-primary">${formatMoney(c.hourlyRate)}/giờ</span>
      </div>
      ${tags.length ? `<div class="flex flex-wrap gap-1.5 mt-2">${tags.map((t) => `<span class="px-2 py-0.5 rounded-lg bg-surface-container text-caption">${escapeHtml(t)}</span>`).join('')}</div>` : ''}
      <p class="text-body-sm text-on-surface mt-2 whitespace-pre-line">${escapeHtml(c.description)}</p>
    </div>`;
}

function itemHtml(p) {
  const pending = p.reviewStatus === 'PENDING';
  // Bản đang hiển thị chỉ đáng so sánh khi khác bản chờ duyệt
  const showPublished = p.published && pending;
  return `
    <li class="px-6 py-5 border-t border-surface-container first:border-t-0">
      <div class="flex flex-col md:flex-row md:items-start justify-between gap-3">
        <div class="min-w-0">
          <div class="flex flex-wrap items-center gap-2">
            <a href="/academic/verifications/detail?tutorId=${encodeURIComponent(p.tutorId)}" class="text-body-md font-bold text-on-surface hover:text-primary">${escapeHtml(p.tutorName)}</a>
            ${p.highestJlptLevel ? `<span class="text-caption text-outline">JLPT ${escapeHtml(p.highestJlptLevel)}</span>` : ''}
            <span class="px-2 py-0.5 rounded-full text-caption font-semibold ${STATUS_STYLE[p.reviewStatus] || ''}">${escapeHtml(p.reviewStatusLabel)}</span>
            ${p.membershipActive ? '' : '<span class="px-2 py-0.5 rounded-full bg-surface-container text-on-surface-variant text-caption font-semibold">Gói đã hết hạn</span>'}
          </div>
          <p class="text-caption text-outline mt-0.5">${escapeHtml(p.tutorEmail)} · Gửi lúc ${formatDateTime(p.submittedAt)}
            ${p.reviewedAt ? ` · ${escapeHtml(p.reviewStatusLabel)} bởi ${escapeHtml(p.reviewerName || '—')} lúc ${formatDateTime(p.reviewedAt)}` : ''}</p>
          ${p.reviewStatus === 'REJECTED' && p.reviewFeedback ? `<p class="text-body-sm text-error mt-1">Lý do từ chối: ${escapeHtml(p.reviewFeedback)}</p>` : ''}
          ${p.reviewStatus === 'NEEDS_INFO' && p.reviewFeedback ? `<p class="text-body-sm text-secondary mt-1">Đã yêu cầu bổ sung: ${escapeHtml(p.reviewFeedback)}</p>` : ''}
        </div>
        ${pending ? `
          <div class="flex flex-wrap gap-2 shrink-0">
            <button type="button" data-dialog="reject" data-id="${p.postId}" class="px-3 py-1.5 rounded-lg bg-surface-container text-error text-label-sm hover:bg-error-container">Từ chối</button>
            <button type="button" data-dialog="info" data-id="${p.postId}" class="px-3 py-1.5 rounded-lg bg-secondary-fixed text-on-secondary-fixed text-label-sm hover:opacity-90">Yêu cầu bổ sung</button>
            <button type="button" data-approve="${p.postId}" class="px-3 py-1.5 rounded-lg bg-primary text-on-primary text-label-sm hover:bg-primary-container shadow-sm">Duyệt và hiển thị</button>
          </div>` : ''}
      </div>
      <div class="mt-4 grid grid-cols-1 ${showPublished ? 'xl:grid-cols-2' : ''} gap-4">
        ${contentBlock(pending ? (p.published ? 'Bản mới chờ duyệt' : 'Nội dung chờ duyệt') : 'Nội dung', p.draft, 'bg-surface-container-low')}
        ${showPublished ? contentBlock('Bản đang hiển thị', p.published, 'bg-surface-container-lowest ring-1 ring-surface-container') : ''}
      </div>
    </li>`;
}

function showState({ icon, title, retry = false }) {
  $('postList').innerHTML = '';
  $('listStateIcon').textContent = icon;
  $('listStateTitle').textContent = title;
  $('retryButton').classList.toggle('hidden', !retry);
  $('listState').classList.remove('hidden');
}

async function load() {
  $('listState').classList.add('hidden');
  $('postList').innerHTML = Array.from({ length: 2 }).map(() =>
    '<li class="px-6 py-3"><div class="tv-skeleton h-32"></div></li>').join('');
  const keyword = $('keywordInput').value.trim();
  try {
    const page = await API.academicPosts.list({ status: state.status, keyword, page: state.page, size: state.size });
    state.items = page.content || [];
    if (state.items.length === 0) {
      showState(keyword
        ? { icon: 'search_off', title: 'Không tìm thấy bài đăng phù hợp.' }
        : { icon: state.status === 'PENDING' ? 'task_alt' : 'inbox', title: state.status === 'PENDING' ? 'Không còn bài đăng nào chờ duyệt.' : 'Chưa có bài đăng nào.' });
    } else {
      $('postList').innerHTML = state.items.map(itemHtml).join('');
    }
    renderPagination($('pager'), page, {
      onPage: (p) => { state.page = p; load(); },
      onSize: (s) => { state.size = s; state.page = 0; load(); }
    });
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return;
    $('pager').innerHTML = '';
    showState({ icon: 'cloud_off', title: err instanceof ApiError ? err.message : 'Không thể tải danh sách bài đăng.', retry: true });
  }
}

function reload() {
  state.page = 0;
  load();
}

async function approve(postId, button) {
  button.disabled = true;
  try {
    const res = await API.academicPosts.approve(postId);
    showToast(res.message, 'success');
    loadSummary();
    load();
  } catch (err) {
    button.disabled = false;
    showToast(err instanceof ApiError ? err.message : 'Không thể duyệt bài đăng.', 'error');
    if (err instanceof ApiError && err.status === 409) { loadSummary(); load(); }
  }
}

function openDialog(mode, postId) {
  const d = DIALOGS[mode];
  const item = state.items.find((p) => p.postId === postId);
  state.dialog = { mode, postId };
  $('rejectTitle').textContent = d.title;
  $('rejectHint').textContent = d.hint(item ? item.tutorName : '');
  $('rejectReason').placeholder = d.placeholder;
  $('rejectReason').value = '';
  $('rejectConfirm').textContent = d.confirm;
  $('rejectConfirm').className = `px-4 py-2.5 rounded-lg text-white text-label-md hover:opacity-90 disabled:opacity-60 ${d.confirmClass}`;
  $('rejectError').classList.add('hidden');
  $('rejectModal').classList.remove('hidden');
  $('rejectReason').focus();
}

function closeDialog() {
  state.dialog = null;
  $('rejectModal').classList.add('hidden');
}

async function confirmDialog() {
  if (!state.dialog) return;
  const d = DIALOGS[state.dialog.mode];
  const text = $('rejectReason').value.trim();
  if (!text) {
    $('rejectError').textContent = d.empty;
    $('rejectError').classList.remove('hidden');
    return;
  }
  $('rejectConfirm').disabled = true;
  try {
    const res = await d.call(state.dialog.postId, text);
    showToast(res.message, 'success');
    closeDialog();
    loadSummary();
    load();
  } catch (err) {
    $('rejectError').textContent = err instanceof ApiError ? err.message : 'Không thể thực hiện thao tác.';
    $('rejectError').classList.remove('hidden');
  } finally {
    $('rejectConfirm').disabled = false;
  }
}

mountAcademicLayout('posts', 'Duyệt bài đăng');

document.addEventListener('DOMContentLoaded', () => {
  renderTabs();
  $('statusTabs').addEventListener('click', (e) => {
    const tab = e.target.closest('.status-tab');
    if (!tab || tab.dataset.status === state.status) return;
    state.status = tab.dataset.status;
    renderTabs();
    reload();
  });
  $('keywordInput').addEventListener('input', debounce(reload, 300));
  $('filterForm').addEventListener('submit', (e) => { e.preventDefault(); reload(); });
  $('retryButton').addEventListener('click', load);
  $('postList').addEventListener('click', (e) => {
    const approveBtn = e.target.closest('[data-approve]');
    if (approveBtn) return approve(approveBtn.dataset.approve, approveBtn);
    const dialogBtn = e.target.closest('[data-dialog]');
    if (dialogBtn) openDialog(dialogBtn.dataset.dialog, dialogBtn.dataset.id);
  });
  $('rejectCancel').addEventListener('click', closeDialog);
  $('rejectConfirm').addEventListener('click', confirmDialog);
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeDialog(); });
  loadSummary();
  load();
});
