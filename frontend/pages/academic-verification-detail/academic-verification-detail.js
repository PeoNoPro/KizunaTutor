/**
 * KizunaTutor - Thẩm định chi tiết một hồ sơ gia sư (FE-03: UC-26, UC-27, UC-28, UC-84)
 * Dữ liệu từ /api/academic/verifications/{tutorId}; không dùng dữ liệu giả.
 */

import { API, ApiError } from '/js/api.js';
import { showToast } from '/js/main.js';
import { mountAcademicLayout } from '/components/academic-layout/academic-layout.js';

const STATUS_STYLE = {
  PENDING: { badge: 'bg-tertiary-fixed text-tertiary', dot: 'bg-tertiary' },
  ADDITIONAL_INFO_REQUIRED: { badge: 'bg-secondary-fixed text-on-secondary-fixed', dot: 'bg-secondary' },
  APPROVED: { badge: 'bg-surface-container-highest text-primary', dot: 'bg-primary' },
  REJECTED: { badge: 'bg-error-container text-error', dot: 'bg-error' }
};
const HISTORY_DOT = { SUBMIT: 'bg-primary', REQUEST_INFO: 'bg-secondary', APPROVE: 'bg-success', REJECT: 'bg-error' };
const MSG_REJECT_REQUIRED = 'Vui lòng chọn lý do từ chối và nhập ghi chú trước khi xác nhận.';
const MSG_INFO_REQUIRED = 'Vui lòng nêu rõ thông tin cần bổ sung trước khi gửi yêu cầu.';

const tutorId = new URLSearchParams(window.location.search).get('tutorId');
const state = { data: null, previewUrl: null, busy: false };
const $ = (id) => document.getElementById(id);

// =========================================================================
// 1. TIỆN ÍCH
// =========================================================================
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text == null ? '' : String(text);
  return div.innerHTML;
}

function pad(n) {
  return String(n).padStart(2, '0');
}

function formatDate(value, withTime = false) {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  const date = `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
  return withTime ? `${date} ${pad(d.getHours())}:${pad(d.getMinutes())}` : date;
}

function initialsOf(name) {
  const parts = String(name || '').trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return 'GS';
  return ((parts[0][0] || '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
}

function orDash(value) {
  return value == null || value === '' ? '—' : value;
}

function showFieldErrors(fields = {}) {
  document.querySelectorAll('[data-error-for]').forEach((el) => {
    el.textContent = fields[el.dataset.errorFor] || '';
  });
}

function openModal(id) {
  showFieldErrors({});
  $(id).classList.remove('hidden');
  $(id).classList.add('flex');
}

function closeModal(id) {
  $(id).classList.add('hidden');
  $(id).classList.remove('flex');
}

// =========================================================================
// 2. TẢI DỮ LIỆU
// =========================================================================
async function load() {
  $('loadingState').classList.remove('hidden');
  $('errorState').classList.add('hidden');
  $('contentState').classList.add('hidden');
  if (!tutorId) {
    showError('Không tìm thấy hồ sơ hoặc giấy tờ được yêu cầu.');
    return;
  }
  try {
    state.data = await API.academicVerification.getDetail(tutorId);
    render(state.data);
    $('contentState').classList.remove('hidden');
    loadHistory();
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return;
    showError(err instanceof ApiError ? err.message : 'Hệ thống đang bận, bạn vui lòng thử lại sau.');
  } finally {
    $('loadingState').classList.add('hidden');
  }
}

function showError(message) {
  $('errorMessage').textContent = message;
  $('errorState').classList.remove('hidden');
  $('loadingState').classList.add('hidden');
}

async function loadHistory() {
  $('historyError').classList.add('hidden');
  try {
    const items = await API.academicVerification.getHistory(tutorId);
    $('historyEmpty').classList.toggle('hidden', items.length > 0);
    $('historyList').classList.toggle('hidden', items.length === 0);
    $('historyList').innerHTML = items.length === 0 ? '' : `
      <div class="absolute left-2.5 top-2 bottom-2 w-0.5 bg-surface-container-high"></div>
      ${items.map(historyItemHtml).join('')}`;
    if (window.location.hash === '#history') $('history').scrollIntoView({ behavior: 'smooth' });
  } catch (err) {
    $('historyList').innerHTML = '';
    $('historyError').classList.remove('hidden');
  }
}

function historyItemHtml(h) {
  const actor = h.officerName ? `Cán bộ: ${escapeHtml(h.officerName)}` : 'Gia sư';
  const reason = h.reasonLabel ? `<p class="text-body-sm text-error mt-1">Lý do: ${escapeHtml(h.reasonLabel)}</p>` : '';
  const feedback = h.feedback ? `<p class="text-body-sm text-on-surface mt-1 whitespace-pre-line">Phản hồi: ${escapeHtml(h.feedback)}</p>` : '';
  const note = h.internalNote
    ? `<p class="text-body-sm text-on-surface-variant mt-1 p-2 rounded-lg bg-surface-container-low whitespace-pre-line"><span class="text-caption text-outline block">Ghi chú nội bộ</span>${escapeHtml(h.internalNote)}</p>`
    : '';
  return `
    <div class="relative">
      <span class="absolute -left-6 top-1 w-3 h-3 rounded-full ${HISTORY_DOT[h.action] || 'bg-outline'} ring-4 ring-surface-container-lowest"></span>
      <p class="text-label-md text-on-surface">${escapeHtml(h.actionLabel)}</p>
      <p class="text-caption text-outline">${formatDate(h.createdAt, true)} • ${actor}</p>
      ${reason}${feedback}${note}
    </div>`;
}

// =========================================================================
// 3. HIỂN THỊ
// =========================================================================
function render(data) {
  const p = data.profile || {};
  const style = STATUS_STYLE[data.status] || STATUS_STYLE.PENDING;
  document.title = `${p.fullName || 'Hồ sơ gia sư'} - Thẩm định - KizunaTutor`;

  $('avatar').textContent = initialsOf(p.fullName);
  $('tutorName').textContent = p.fullName || 'Gia sư';
  $('statusBadge').className = `inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-caption font-semibold ${style.badge}`;
  $('statusBadge').innerHTML = `<span class="w-1.5 h-1.5 rounded-full ${style.dot}"></span>${escapeHtml(data.statusLabel)}`;
  $('contactText').textContent = [data.email, p.phone].filter(Boolean).join(' • ');
  $('submittedText').textContent = formatDate(data.submittedAt, true);

  const pending = data.status === 'PENDING';
  $('actionBar').classList.toggle('hidden', !pending);
  $('processedNote').classList.toggle('hidden', pending);
  $('processedNote').textContent = pending ? '' : 'Hồ sơ không ở trạng thái chờ thẩm định nên không thể ra quyết định. Xem lịch sử thẩm định để biết chi tiết.';
  renderProfileUpdate(data);

  $('fFullName').textContent = orDash(p.fullName);
  $('fPhone').textContent = orDash(p.phone);
  $('fDob').textContent = formatDate(p.dateOfBirth);
  $('fAddress').textContent = orDash(p.address);
  $('fJlpt').textContent = p.highestJlptLevel ? `JLPT ${p.highestJlptLevel}` : '—';
  $('fExperience').textContent = `${p.experienceYears ?? 0} năm`;
  $('fBio').textContent = orDash(p.bio);

  const docs = data.documents || [];
  $('documentCount').textContent = `${docs.length} giấy tờ`;
  $('documentEmpty').classList.toggle('hidden', docs.length > 0);
  $('documentList').innerHTML = docs.map((doc) => `
    <li class="p-4 rounded-xl bg-surface-container-low flex flex-col sm:flex-row sm:items-center justify-between gap-3">
      <div class="flex items-start gap-3 min-w-0">
        <div class="w-10 h-10 rounded-lg bg-surface-container-lowest text-primary flex items-center justify-center shrink-0">
          <span class="material-symbols-outlined text-[20px]">${(doc.contentType || '').startsWith('image/') ? 'image' : 'description'}</span>
        </div>
        <div class="min-w-0">
          <p class="text-label-md text-on-surface font-semibold truncate">${escapeHtml(doc.title)}</p>
          <p class="text-caption text-outline">${escapeHtml(doc.typeLabel)} • Tải lên ${formatDate(doc.uploadedAt)} • ${escapeHtml(doc.statusLabel)}</p>
          ${doc.fileAvailable ? '' : '<p class="text-caption text-error mt-0.5">Không thể hiển thị tệp này. Tệp có thể bị lỗi hoặc không được hỗ trợ.</p>'}
        </div>
      </div>
      <button type="button" data-view-doc="${doc.id}" ${doc.fileAvailable ? '' : 'disabled'}
        class="shrink-0 px-3 py-1.5 rounded-lg bg-surface-container-lowest text-primary hover:bg-primary hover:text-on-primary text-label-sm shadow-sm transition-all disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-surface-container-lowest disabled:hover:text-primary">
        Xem giấy tờ
      </button>
    </li>`).join('');
}

/** Yêu cầu cập nhật gần nhất của hồ sơ đã phê duyệt; chỉ ra quyết định được khi đang chờ. */
function renderProfileUpdate(data) {
  const update = data.status === 'APPROVED' ? data.profileUpdate : null;
  $('updatePanel').classList.toggle('hidden', !update);
  if (!update) return;
  const waiting = update.status === 'PENDING';
  $('updateBadge').textContent = update.statusLabel;
  const newDocs = (data.documents || []).filter((d) => d.status === 'PENDING');
  const items = [`Gửi lúc ${formatDate(update.submittedAt, true)}`];
  if (update.requestedJlptLevel) {
    items.push(`Trình độ JLPT: <strong>${escapeHtml(update.currentJlptLevel || '—')}</strong> → <strong>${escapeHtml(update.requestedJlptLevel)}</strong>`);
  }
  if (waiting) {
    items.push(newDocs.length
      ? `Giấy tờ mới chờ thẩm định: ${newDocs.map((d) => escapeHtml(d.title)).join(', ')} (xem ở mục giấy tờ bên dưới)`
      : 'Không có giấy tờ mới');
  }
  if (update.note) items.push(`Ghi chú của gia sư: ${escapeHtml(update.note)}`);
  if (!waiting && update.reviewedAt) {
    items.push(`Xử lý lúc ${formatDate(update.reviewedAt, true)}${update.reviewedByName ? ` bởi ${escapeHtml(update.reviewedByName)}` : ''}`);
  }
  if (update.status === 'REJECTED' && update.feedback) items.push(`Lý do từ chối: ${escapeHtml(update.feedback)}`);
  $('updateDetails').innerHTML = items.map((t) => `<li>${t}</li>`).join('');
  $('updateActions').classList.toggle('hidden', !waiting);
  $('updateFeedbackInput').value = '';
}

async function decideProfileUpdate(approve) {
  const button = approve ? $('approveUpdateButton') : $('rejectUpdateButton');
  showFieldErrors({});
  if (approve) {
    if (!window.confirm('Chấp nhận yêu cầu cập nhật hồ sơ này?')) return;
  } else {
    const feedback = $('updateFeedbackInput').value.trim();
    if (!feedback) {
      showFieldErrors({ updateFeedback: 'Vui lòng nhập lý do từ chối yêu cầu cập nhật.' });
      $('updateFeedbackInput').focus();
      return;
    }
  }
  if (state.busy) return;
  state.busy = true;
  button.disabled = true;
  try {
    const result = approve
      ? await API.academicVerification.approveProfileUpdate(tutorId)
      : await API.academicVerification.rejectProfileUpdate(tutorId, $('updateFeedbackInput').value.trim());
    showToast(result.message, 'success');
    await load();
  } catch (err) {
    if (err instanceof ApiError && err.status === 400) showFieldErrors({ updateFeedback: (err.fields || {}).feedback });
    showToast(err instanceof ApiError ? err.message : 'Hệ thống đang bận, bạn vui lòng thử lại sau.', 'error');
    if (err instanceof ApiError && err.code === 'ALREADY_PROCESSED') await load();
  } finally {
    state.busy = false;
    button.disabled = false;
  }
}

async function viewDocument(docId) {
  const doc = (state.data.documents || []).find((d) => d.id === docId);
  const body = $('previewBody');
  $('previewTitle').textContent = doc ? `${doc.typeLabel} — ${doc.title}` : 'Xem giấy tờ';
  $('previewPanel').classList.remove('hidden');
  body.innerHTML = '<div class="tv-skeleton w-full h-[320px]"></div>';
  $('previewPanel').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  try {
    const blob = await API.academicVerification.getDocumentFile(tutorId, docId);
    releasePreview();
    state.previewUrl = URL.createObjectURL(blob);
    body.innerHTML = (blob.type || '').startsWith('image/')
      ? `<img src="${state.previewUrl}" alt="${escapeHtml(doc ? doc.title : 'Giấy tờ')}" class="max-w-full max-h-[70vh] object-contain">`
      : `<iframe src="${state.previewUrl}" title="${escapeHtml(doc ? doc.title : 'Giấy tờ')}" class="w-full h-[70vh] rounded-lg"></iframe>`;
  } catch (err) {
    body.innerHTML = '<p class="p-6 text-center text-error">Không thể hiển thị tệp này. Tệp có thể bị lỗi hoặc không được hỗ trợ.</p>';
  }
}

function releasePreview() {
  if (state.previewUrl) {
    URL.revokeObjectURL(state.previewUrl);
    state.previewUrl = null;
  }
}

// =========================================================================
// 4. QUYẾT ĐỊNH
// =========================================================================
async function runDecision(modalId, button, action) {
  if (state.busy) return;
  state.busy = true;
  button.disabled = true;
  try {
    const result = await action();
    closeModal(modalId);
    showToast(result.message, 'success');
    await load();
  } catch (err) {
    if (err instanceof ApiError && err.code === 'ALREADY_PROCESSED') {
      closeModal(modalId);
      showToast(err.message, 'error');
      await load();
    } else if (err instanceof ApiError && err.status === 400) {
      showFieldErrors(err.fields);
      showToast(err.message, 'error');
    } else {
      showToast(err instanceof ApiError ? err.message : 'Hệ thống đang bận, bạn vui lòng thử lại sau.', 'error');
    }
  } finally {
    state.busy = false;
    button.disabled = false;
  }
}

function submitReject(event) {
  event.preventDefault();
  const reasonCode = $('reasonSelect').value;
  const internalNote = $('internalNoteInput').value.trim();
  const feedback = $('feedbackInput').value.trim();
  const errors = {};
  if (!reasonCode) errors.reasonCode = 'Vui lòng chọn lý do từ chối.';
  if (!internalNote) errors.internalNote = 'Vui lòng nhập ghi chú nội bộ.';
  showFieldErrors(errors);
  if (Object.keys(errors).length) {
    $('rejectFormError').textContent = MSG_REJECT_REQUIRED;
    $('rejectFormError').classList.remove('hidden');
    return;
  }
  $('rejectFormError').classList.add('hidden');
  runDecision('rejectModal', $('rejectConfirmButton'),
    () => API.academicVerification.reject(tutorId, { reasonCode, feedback, internalNote }));
}

function submitRequestInfo(event) {
  event.preventDefault();
  const message = $('requestInfoInput').value.trim();
  if (!message) {
    showFieldErrors({ message: MSG_INFO_REQUIRED });
    return;
  }
  runDecision('requestInfoModal', $('requestInfoConfirmButton'),
    () => API.academicVerification.requestInfo(tutorId, message));
}

// =========================================================================
// 5. KHỞI TẠO
// =========================================================================
function bindEvents() {
  $('retryButton').addEventListener('click', load);
  $('approveUpdateButton').addEventListener('click', () => decideProfileUpdate(true));
  $('rejectUpdateButton').addEventListener('click', () => decideProfileUpdate(false));
  $('approveButton').addEventListener('click', () => openModal('approveModal'));
  $('rejectButton').addEventListener('click', () => {
    $('rejectForm').reset();
    $('rejectFormError').classList.add('hidden');
    openModal('rejectModal');
  });
  $('requestInfoButton').addEventListener('click', () => {
    $('requestInfoForm').reset();
    $('requestInfoCounter').textContent = '0';
    openModal('requestInfoModal');
  });
  $('approveConfirmButton').addEventListener('click', () =>
    runDecision('approveModal', $('approveConfirmButton'), () => API.academicVerification.approve(tutorId)));
  $('rejectForm').addEventListener('submit', submitReject);
  $('requestInfoForm').addEventListener('submit', submitRequestInfo);
  $('requestInfoInput').addEventListener('input', () => {
    $('requestInfoCounter').textContent = String($('requestInfoInput').value.length);
  });
  $('documentList').addEventListener('click', (e) => {
    const button = e.target.closest('button[data-view-doc]');
    if (button && !button.disabled) viewDocument(button.dataset.viewDoc);
  });
  $('previewCloseButton').addEventListener('click', () => {
    $('previewPanel').classList.add('hidden');
    releasePreview();
  });
  document.querySelectorAll('[data-close-modal]').forEach((b) =>
    b.addEventListener('click', () => closeModal(b.closest('[role="dialog"]').id)));
  ['approveModal', 'rejectModal', 'requestInfoModal'].forEach((id) =>
    $(id).addEventListener('click', (e) => { if (e.target.id === id) closeModal(id); }));
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') ['approveModal', 'rejectModal', 'requestInfoModal'].forEach(closeModal);
  });
}

mountAcademicLayout('verifications', 'Thẩm định hồ sơ gia sư');

document.addEventListener('DOMContentLoaded', () => {
  bindEvents();
  load();
});
