/**
 * KizunaTutor - Xác minh hồ sơ gia sư (FE-03: UC-22, UC-23, UC-24, UC-82, UC-83)
 * Dữ liệu lấy từ API /api/tutor/verification; không dùng dữ liệu giả.
 */

import { API, ApiError } from '/js/api.js';
import { showToast } from '/js/main.js';
import { mountTutorLayout, setTutorIdentity } from '/components/tutor-layout/tutor-layout.js';

mountTutorLayout('verification');

const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ALLOWED_EXTENSIONS = ['pdf', 'png', 'jpg', 'jpeg'];
const MSG_INVALID_FILE = 'Tệp không hợp lệ. Vui lòng chọn tệp PDF, PNG hoặc JPG có dung lượng tối đa 10MB.';
const DOCUMENT_TYPES = [
  ['IDENTITY_DOCUMENT', 'Giấy tờ tùy thân'],
  ['JLPT_CERTIFICATE', 'Chứng chỉ JLPT'],
  ['OTHER_JAPANESE_CERTIFICATE', 'Chứng chỉ tiếng Nhật khác'],
  ['DEGREE', 'Bằng cấp'],
  ['TEACHING_CERTIFICATE', 'Chứng chỉ sư phạm'],
  ['OTHER', 'Khác']
];
const PROFILE_FIELDS = ['fullName', 'phone', 'dateOfBirth', 'address', 'experienceYears', 'highestJlptLevel', 'bio'];

const STATUS_STYLES = {
  UNVERIFIED: { css: 'tv-status-unverified', icon: 'radio_button_unchecked', hint: 'Hoàn thiện thông tin, tải lên giấy tờ và nộp hồ sơ để được thẩm định.' },
  PENDING: { css: 'tv-status-pending', icon: 'hourglass_top', hint: 'Cán bộ học vụ đang thẩm định hồ sơ. Kết quả sẽ được gửi qua email của bạn.' },
  ADDITIONAL_INFO_REQUIRED: { css: 'tv-status-needs-info', icon: 'edit_note', hint: 'Vui lòng xem phản hồi bên dưới, bổ sung thông tin và nộp lại hồ sơ.' },
  APPROVED: { css: 'tv-status-approved', icon: 'verified', hint: 'Hồ sơ đã được phê duyệt. Bạn đã có thể nhận học viên trên Kizuna.' },
  REJECTED: { css: 'tv-status-rejected', icon: 'block', hint: 'Hồ sơ không được chấp nhận. Đây là quyết định cuối cùng nên hồ sơ không thể chỉnh sửa hoặc nộp lại. Vui lòng liên hệ bộ phận hỗ trợ nếu bạn cần trao đổi thêm.' }
};

const state = {
  data: null,
  started: false,
  replaceTargetId: null,
  previewUrl: null
};

const $ = (id) => document.getElementById(id);

// =========================================================================
// 1. TIỆN ÍCH
// =========================================================================
function formatDate(value) {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  const dd = String(d.getDate()).padStart(2, '0');
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  return `${dd}/${mm}/${d.getFullYear()}`;
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text == null ? '' : String(text);
  return div.innerHTML;
}

function fileExtension(name) {
  const idx = (name || '').lastIndexOf('.');
  return idx < 0 ? '' : name.slice(idx + 1).toLowerCase();
}

function isAcceptableFile(file) {
  return file && file.size > 0 && file.size <= MAX_FILE_SIZE && ALLOWED_EXTENSIONS.includes(fileExtension(file.name));
}

function setVisible(el, visible) {
  if (el) el.classList.toggle('hidden', !visible);
}

function showFieldErrors(fields = {}) {
  document.querySelectorAll('[data-error-for]').forEach((el) => {
    const key = el.dataset.errorFor;
    el.textContent = fields[key] || '';
  });
  PROFILE_FIELDS.forEach((key) => {
    const input = document.querySelector(`#profileForm [name="${key}"]`);
    if (input) input.classList.toggle('has-error', Boolean(fields[key]));
  });
}

function messageOf(err) {
  return err instanceof ApiError ? err.message : 'Hệ thống đang bận, bạn vui lòng thử lại sau.';
}

function setBusy(button, busy) {
  if (!button) return;
  button.disabled = busy;
  button.setAttribute('aria-busy', busy ? 'true' : 'false');
}

// =========================================================================
// 2. TẢI DỮ LIỆU
// =========================================================================
async function loadVerification() {
  setVisible($('loadingState'), true);
  setVisible($('errorState'), false);
  setVisible($('contentState'), false);
  try {
    state.data = await API.tutorVerification.getMine();
    render();
    setVisible($('contentState'), true);
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return;
    $('errorMessage').textContent = err instanceof ApiError && err.status === 404
      ? 'Không tìm thấy hồ sơ gia sư của tài khoản này. Vui lòng liên hệ bộ phận hỗ trợ.'
      : messageOf(err);
    setVisible($('errorState'), true);
  } finally {
    setVisible($('loadingState'), false);
  }
}

// =========================================================================
// 3. HIỂN THỊ
// =========================================================================
function isUntouched(data) {
  const p = data.profile || {};
  return data.status === 'UNVERIFIED' && !data.submittedAt && (data.documents || []).length === 0
    && !p.phone && !p.dateOfBirth && !p.address && !p.highestJlptLevel;
}

function render() {
  const data = state.data;
  const profile = data.profile || {};
  setTutorIdentity({ fullName: profile.fullName, jlptLevel: profile.highestJlptLevel, statusLabel: data.statusLabel });
  renderStatus(data);
  renderFeedback(data);

  const showStart = isUntouched(data) && !state.started;
  setVisible($('startBlock'), showStart);
  setVisible($('workArea'), !showStart);

  renderProfile(data);
  renderDocuments(data);
  renderEditMode(data);
  renderMissing(data);
}

function renderStatus(data) {
  const style = STATUS_STYLES[data.status] || STATUS_STYLES.UNVERIFIED;
  const badge = $('statusBadge');
  badge.className = `tv-badge text-sm ${style.css}`;
  badge.innerHTML = `<span class="material-symbols-outlined text-base">${style.icon}</span>${escapeHtml(data.statusLabel)}`;
  $('statusHint').textContent = style.hint;
  $('submittedAtText').textContent = formatDate(data.submittedAt);
  $('verifiedAtText').textContent = formatDate(data.verifiedAt);
}

function renderFeedback(data) {
  const block = $('feedbackBlock');
  if (data.status === 'UNVERIFIED') {
    setVisible(block, false);
    return;
  }
  setVisible(block, true);
  const fb = data.latestFeedback;
  setVisible($('feedbackContent'), Boolean(fb));
  setVisible($('feedbackEmpty'), !fb);
  if (!fb) return;
  $('feedbackKind').textContent = fb.kindLabel || 'Phản hồi';
  $('feedbackDate').textContent = fb.createdAt ? ` · ${formatDate(fb.createdAt)}` : '';
  $('feedbackReason').textContent = fb.reasonLabel ? `Lý do: ${fb.reasonLabel}` : '';
  setVisible($('feedbackReason'), Boolean(fb.reasonLabel));
  $('feedbackText').textContent = fb.content || '';
  setVisible($('feedbackEditButton'), Boolean(data.editable));
}

function renderProfile(data) {
  const p = data.profile || {};
  $('fullNameInput').value = p.fullName || '';
  $('phoneInput').value = p.phone || '';
  $('dateOfBirthInput').value = p.dateOfBirth || '';
  $('addressInput').value = p.address || '';
  $('experienceYearsInput').value = p.experienceYears ?? 0;
  $('highestJlptLevelInput').value = p.highestJlptLevel || '';
  $('bioInput').value = p.bio || '';
  $('bioCounter').textContent = String(($('bioInput').value || '').length);
}

function renderDocuments(data) {
  const docs = data.documents || [];
  const list = $('documentList');
  setVisible($('documentEmpty'), docs.length === 0);
  list.innerHTML = docs.map((doc) => {
    const unavailable = !doc.fileAvailable
      ? '<p class="mt-1" style="color: var(--tv-warn);">Không thể hiển thị tệp này. Tệp có thể bị lỗi hoặc không được hỗ trợ.</p>'
      : '';
    const typeSelect = data.editable ? `
        <label class="sr-only" for="docType-${doc.id}">Loại giấy tờ</label>
        <select id="docType-${doc.id}" class="tv-input !w-auto !py-1 text-sm" data-change-type="${doc.id}" title="Đổi loại giấy tờ">
          ${DOCUMENT_TYPES.map(([value, label]) => `<option value="${value}" ${value === doc.type ? 'selected' : ''}>${label}</option>`).join('')}
        </select>` : '';
    const editButtons = data.editable ? `
        ${typeSelect}
        <button type="button" class="tv-btn tv-btn-small" data-action="replace" data-id="${doc.id}">
          <span class="material-symbols-outlined text-base">swap_horiz</span>Thay tệp
        </button>
        <button type="button" class="tv-btn tv-btn-small tv-btn-danger" data-action="remove" data-id="${doc.id}">
          <span class="material-symbols-outlined text-base">delete</span>Gỡ
        </button>` : '';
    return `
      <li class="border border-black rounded-[5px] p-3 flex flex-col md:flex-row md:items-center md:justify-between gap-3">
        <div class="min-w-0">
          <p class="tv-meta text-[var(--tv-muted)]">${escapeHtml(doc.typeLabel)} · Tải lên ${formatDate(doc.uploadedAt)}</p>
          <p class="font-semibold truncate">${escapeHtml(doc.title)}</p>
          <span class="tv-badge mt-1">${escapeHtml(doc.statusLabel)}</span>
          ${unavailable}
        </div>
        <div class="flex flex-wrap gap-2">
          <button type="button" class="tv-btn tv-btn-small" data-action="view" data-id="${doc.id}" ${doc.fileAvailable ? '' : 'disabled'}>
            <span class="material-symbols-outlined text-base">visibility</span>Xem
          </button>
          ${editButtons}
        </div>
      </li>`;
  }).join('');
}

function renderEditMode(data) {
  const editable = Boolean(data.editable);
  document.querySelectorAll('#profileForm .tv-input').forEach((input) => { input.disabled = !editable; });
  document.querySelectorAll('[data-edit-only]').forEach((el) => setVisible(el, editable));
  setVisible($('readOnlyNote'), !editable);
  $('readOnlyNote').textContent = data.status === 'REJECTED'
    ? 'Chỉ xem — hồ sơ đã bị từ chối và không thể nộp lại'
    : 'Chỉ xem — hồ sơ đang được thẩm định hoặc đã được phê duyệt';
  const resubmit = data.status === 'ADDITIONAL_INFO_REQUIRED';
  $('submitButtonText').textContent = resubmit ? 'Nộp lại hồ sơ' : 'Nộp hồ sơ';
  $('confirmSubmitButton').textContent = resubmit ? 'Nộp lại hồ sơ' : 'Nộp hồ sơ';
}

// Nhãn nhóm giấy tờ bắt buộc do backend trả về (VerificationLabels) → loại giấy tờ chọn sẵn khi tải lên
const MISSING_DOCUMENT_TYPES = {
  'Giấy tờ tùy thân': 'IDENTITY_DOCUMENT',
  'Chứng chỉ năng lực tiếng Nhật (JLPT hoặc chứng chỉ tương đương)': 'JLPT_CERTIFICATE'
};

function renderMissing(data, extraDocuments) {
  const items = extraDocuments || data.missingRequirements || [];
  setVisible($('missingBox'), data.editable && items.length > 0);
  $('missingList').innerHTML = items.map((item) => {
    const type = MISSING_DOCUMENT_TYPES[item];
    const action = type
      ? ` <button type="button" class="tv-btn tv-btn-small ml-2" data-upload-type="${type}">
            <span class="material-symbols-outlined text-base">upload</span>Tải lên ngay</button>`
      : '';
    return `<li class="py-0.5">${escapeHtml(item)}${action}</li>`;
  }).join('');
}

/** Chọn sẵn loại giấy tờ trong biểu mẫu tải lên và đưa người dùng tới đó. */
function prepareUpload(type) {
  $('documentTypeInput').value = type;
  const label = DOCUMENT_TYPES.find(([value]) => value === type);
  if (label && !$('documentTitleInput').value.trim()) $('documentTitleInput').value = label[1];
  $('uploadForm').scrollIntoView({ behavior: 'smooth', block: 'center' });
  $('documentTypeInput').classList.add('has-error');
  setTimeout(() => $('documentTypeInput').classList.remove('has-error'), 1500);
  $('documentFileInput').focus();
}

// =========================================================================
// 4. THAO TÁC
// =========================================================================
function readProfileForm() {
  const value = (id) => $(id).value.trim();
  const years = $('experienceYearsInput').value;
  return {
    fullName: value('fullNameInput'),
    phone: value('phoneInput'),
    dateOfBirth: $('dateOfBirthInput').value || null,
    address: value('addressInput'),
    experienceYears: years === '' ? null : Number(years),
    highestJlptLevel: $('highestJlptLevelInput').value,
    bio: value('bioInput')
  };
}

async function saveProfile({ silent = false } = {}) {
  const button = $('saveProfileButton');
  setBusy(button, true);
  showFieldErrors({});
  try {
    state.data = await API.tutorVerification.saveProfile(readProfileForm());
    render();
    if (!silent) showToast(state.data.message || 'Đã cập nhật thông tin xác minh. Vui lòng nộp hồ sơ khi đã sẵn sàng.', 'success');
    return true;
  } catch (err) {
    showFieldErrors(err.fields || {});
    showToast(messageOf(err), 'error');
    if (err.code === 'VERIFICATION_LOCKED' || err.code === 'VERIFICATION_REJECTED') await loadVerification();
    return false;
  } finally {
    setBusy(button, false);
  }
}

function hasPendingUpload() {
  return Boolean($('documentFileInput').files[0]);
}

async function uploadDocument(event) {
  if (event) event.preventDefault();
  return uploadSelectedDocument();
}

/**
 * Tải lên giấy tờ đang chọn trong biểu mẫu. Trả về true nếu tải thành công.
 */
async function uploadSelectedDocument() {
  const type = $('documentTypeInput').value;
  const title = $('documentTitleInput').value.trim();
  const file = $('documentFileInput').files[0];
  const errors = {};
  if (!title) errors.title = 'Vui lòng nhập tên giấy tờ.';
  if (!file) errors.file = 'Vui lòng chọn tệp.';
  else if (!isAcceptableFile(file)) errors.file = MSG_INVALID_FILE;
  showFieldErrors(errors);
  if (Object.keys(errors).length) {
    if (errors.file === MSG_INVALID_FILE) showToast(MSG_INVALID_FILE, 'error');
    $('uploadForm').scrollIntoView({ behavior: 'smooth', block: 'center' });
    return false;
  }

  const button = $('uploadButton');
  setBusy(button, true);
  try {
    await API.tutorVerification.addDocument({ type, title, file });
    $('uploadForm').reset();
    showToast('Đã tải lên giấy tờ.', 'success');
    await refreshQuietly();
    return true;
  } catch (err) {
    showFieldErrors(err.code === 'INVALID_FILE' ? { file: err.message } : (err.fields || {}));
    showToast(messageOf(err), 'error');
    return false;
  } finally {
    setBusy(button, false);
  }
}

async function replaceDocument(file) {
  const id = state.replaceTargetId;
  state.replaceTargetId = null;
  if (!id || !file) return;
  if (!isAcceptableFile(file)) {
    showToast(MSG_INVALID_FILE, 'error');
    return;
  }
  try {
    await API.tutorVerification.updateDocument(id, { file });
    showToast('Đã thay tệp giấy tờ.', 'success');
    await refreshQuietly();
  } catch (err) {
    showToast(messageOf(err), 'error');
  }
}

async function removeDocument(id) {
  const doc = (state.data.documents || []).find((d) => d.id === id);
  if (!window.confirm(`Gỡ giấy tờ "${doc ? doc.title : ''}" khỏi hồ sơ?`)) return;
  try {
    await API.tutorVerification.removeDocument(id);
    showToast('Đã gỡ giấy tờ.', 'success');
    await refreshQuietly();
  } catch (err) {
    showToast(messageOf(err), 'error');
  }
}

async function viewDocument(id) {
  const doc = (state.data.documents || []).find((d) => d.id === id);
  const body = $('previewBody');
  $('previewTitle').textContent = doc ? doc.title : 'Xem giấy tờ';
  body.innerHTML = '<div class="tv-skeleton w-full h-[300px]"></div>';
  openModal('previewModal');
  try {
    const blob = await API.tutorVerification.getDocumentFile(id);
    releasePreview();
    state.previewUrl = URL.createObjectURL(blob);
    if ((blob.type || '').startsWith('image/')) {
      body.innerHTML = `<img src="${state.previewUrl}" alt="${escapeHtml(doc ? doc.title : 'Giấy tờ')}" class="max-w-full max-h-[70vh] object-contain">`;
    } else {
      body.innerHTML = `<iframe src="${state.previewUrl}" title="${escapeHtml(doc ? doc.title : 'Giấy tờ')}" class="w-full h-[70vh]"></iframe>`;
    }
  } catch (err) {
    body.innerHTML = '<p class="p-6 text-center" style="color: var(--tv-warn);">Không thể hiển thị tệp này. Tệp có thể bị lỗi hoặc không được hỗ trợ.</p>';
  }
}

function requestSubmit() {
  showFieldErrors({});
  if (!$('commitmentInput').checked) {
    showFieldErrors({ commitment: 'Vui lòng xác nhận cam kết trước khi nộp hồ sơ.' });
    return;
  }
  openModal('confirmModal');
}

async function confirmSubmit() {
  const confirmButton = $('confirmSubmitButton');
  const submitButton = $('submitButton');
  setBusy(confirmButton, true);
  setBusy(submitButton, true);
  try {
    // Giấy tờ đã chọn nhưng chưa bấm "Tải lên" thì tải lên trước khi nộp
    if (hasPendingUpload()) {
      const uploaded = await uploadSelectedDocument();
      if (!uploaded) {
        closeModal('confirmModal');
        return;
      }
    }
    // Lưu các thay đổi chưa lưu trên biểu mẫu trước khi nộp
    const saved = await saveProfile({ silent: true });
    if (!saved) {
      closeModal('confirmModal');
      return;
    }
    state.data = await API.tutorVerification.submit(true);
    closeModal('confirmModal');
    $('commitmentInput').checked = false;
    render();
    showToast(state.data.message, 'success');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } catch (err) {
    closeModal('confirmModal');
    showFieldErrors(err.fields || {});
    if (err.code === 'SUBMISSION_INCOMPLETE') {
      const labels = [...Object.values(err.fields || {}), ...(err.missingDocuments || [])];
      renderMissing(state.data, labels);
      $('missingBox').scrollIntoView({ behavior: 'smooth', block: 'center' });
      const missingDocs = err.missingDocuments || [];
      showToast(missingDocs.length
        ? `Không thể nộp hồ sơ. Còn thiếu: ${missingDocs.join(', ')}.`
        : messageOf(err), 'error');
    } else {
      showToast(messageOf(err), 'error');
    }
    if (err.code === 'VERIFICATION_LOCKED' || err.code === 'VERIFICATION_REJECTED') await loadVerification();
  } finally {
    setBusy(confirmButton, false);
    setBusy(submitButton, false);
  }
}

async function refreshQuietly() {
  try {
    state.data = await API.tutorVerification.getMine();
    render();
  } catch (err) {
    showToast(messageOf(err), 'error');
  }
}

// =========================================================================
// 5. HỘP THOẠI
// =========================================================================
function openModal(id) {
  const modal = $(id);
  modal.classList.remove('hidden');
  modal.classList.add('flex');
}

function closeModal(id) {
  const modal = $(id);
  modal.classList.add('hidden');
  modal.classList.remove('flex');
  if (id === 'previewModal') releasePreview();
}

function releasePreview() {
  if (state.previewUrl) {
    URL.revokeObjectURL(state.previewUrl);
    state.previewUrl = null;
  }
}

// =========================================================================
// 6. KHỞI TẠO
// =========================================================================
function bindEvents() {
  $('retryButton').addEventListener('click', loadVerification);
  $('startButton').addEventListener('click', () => {
    state.started = true;
    render();
    $('fullNameInput').focus();
  });
  $('feedbackEditButton').addEventListener('click', () => {
    $('profileSection').scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
  $('profileForm').addEventListener('submit', (event) => {
    event.preventDefault();
    saveProfile();
  });
  $('bioInput').addEventListener('input', () => {
    $('bioCounter').textContent = String($('bioInput').value.length);
  });
  $('uploadForm').addEventListener('submit', uploadDocument);
  $('documentList').addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action]');
    if (!button || button.disabled) return;
    const { action, id } = button.dataset;
    if (action === 'view') viewDocument(id);
    if (action === 'remove') removeDocument(id);
    if (action === 'replace') {
      state.replaceTargetId = id;
      $('replaceFileInput').value = '';
      $('replaceFileInput').click();
    }
  });
  $('replaceFileInput').addEventListener('change', (event) => replaceDocument(event.target.files[0]));
  $('documentList').addEventListener('change', async (event) => {
    const select = event.target.closest('select[data-change-type]');
    if (!select) return;
    select.disabled = true;
    try {
      await API.tutorVerification.updateDocument(select.dataset.changeType, { type: select.value });
      showToast('Đã đổi loại giấy tờ.', 'success');
      await refreshQuietly();
    } catch (err) {
      showToast(messageOf(err), 'error');
      await refreshQuietly();
    }
  });
  $('submitButton').addEventListener('click', requestSubmit);
  $('missingList').addEventListener('click', (event) => {
    const button = event.target.closest('button[data-upload-type]');
    if (button) prepareUpload(button.dataset.uploadType);
  });
  $('confirmCancelButton').addEventListener('click', () => closeModal('confirmModal'));
  $('confirmSubmitButton').addEventListener('click', confirmSubmit);
  $('previewCloseButton').addEventListener('click', () => closeModal('previewModal'));
  ['confirmModal', 'previewModal'].forEach((id) => {
    $(id).addEventListener('click', (event) => {
      if (event.target.id === id) closeModal(id);
    });
  });
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      closeModal('confirmModal');
      closeModal('previewModal');
    }
  });
}

document.addEventListener('DOMContentLoaded', () => {
  bindEvents();
  loadVerification();
});
