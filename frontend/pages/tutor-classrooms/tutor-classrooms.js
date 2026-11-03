/**
 * KizunaTutor - FE-06 Trang quản lý lớp học của gia sư.
 * UC-93 Access Classroom, UC-61 View / UC-65 Upload / UC-94 Update Learning Materials.
 * Dữ liệu lấy hoàn toàn từ API /api/classrooms; không có dữ liệu giả.
 */

import { API } from '../../js/api.js';
import {
  classroomStatusLabel,
  classroomStatusTone,
  fileTypeLabel,
  formatDateVi,
  validateMaterialFile,
  validateMaterialFields,
  validateClassroomTitle,
  friendlyErrorMessage,
  escapeHtml,
  MATERIAL_ACCEPT_ATTR
} from '../../js/classroom-helpers.js';

const $ = (id) => document.getElementById(id);

const state = {
  classrooms: [],
  currentSpace: null,
  materials: [],
  editingMaterial: null
};

// ----------------------------------------------------------------------------
// Tiện ích giao diện
// ----------------------------------------------------------------------------
function showToast(message, tone = 'success') {
  const toast = $('classroomToast');
  if (!toast) return;
  toast.textContent = message;
  toast.style.background = tone === 'success' ? '#E5F4EA' : '#FDEDE8';
  toast.style.color = tone === 'success' ? '#1E8B3F' : '#BF360C';
  toast.classList.remove('hidden');
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => toast.classList.add('hidden'), 3500);
}

function setFieldError(id, message) {
  const el = $(id);
  if (!el) return;
  el.textContent = message || '';
  el.classList.toggle('hidden', !message);
}

function openModal(id) {
  $(id)?.classList.remove('hidden');
}

function closeModal(id) {
  $(id)?.classList.add('hidden');
}

function renderStateBox(el, message, { retry } = {}) {
  el.innerHTML = `
    <p class="text-[#333]">${escapeHtml(message)}</p>
    ${retry ? '<button type="button" class="retro-btn mt-4" data-retry>Thử lại</button>' : ''}
  `;
  el.classList.remove('hidden');
  if (retry) el.querySelector('[data-retry]').addEventListener('click', retry);
}

function setBusy(button, busy, busyText) {
  if (!button) return;
  if (busy) {
    button.dataset.label = button.textContent;
    button.textContent = busyText;
    button.disabled = true;
  } else {
    button.textContent = button.dataset.label || button.textContent;
    button.disabled = false;
  }
}

// ----------------------------------------------------------------------------
// Danh sách lớp (UC-93 bước 1–2)
// ----------------------------------------------------------------------------
async function loadClassrooms() {
  const grid = $('classroomGrid');
  const stateBox = $('classroomListState');
  stateBox.classList.add('hidden');
  grid.innerHTML = '<p class="text-[#444]">Đang tải danh sách lớp học...</p>';
  try {
    state.classrooms = await API.classrooms.list() || [];
    renderClassrooms();
  } catch (err) {
    grid.innerHTML = '';
    renderStateBox(stateBox, friendlyErrorMessage(err), { retry: loadClassrooms });
  }
}

function renderClassrooms() {
  const grid = $('classroomGrid');
  const stateBox = $('classroomListState');
  if (!state.classrooms.length) {
    grid.innerHTML = '';
    renderStateBox(stateBox, 'Bạn chưa có lớp học nào. Hãy thêm học viên đã kết nối để mở lớp đầu tiên.');
    return;
  }
  stateBox.classList.add('hidden');
  grid.innerHTML = state.classrooms.map((c) => `
    <article class="retro-card p-4 flex flex-col gap-3">
      <div class="flex items-start justify-between gap-2">
        <h3 class="font-heading text-[18px] leading-snug break-words">${escapeHtml(c.title)}</h3>
        <span class="retro-badge shrink-0" data-tone="${classroomStatusTone(c.status)}">${escapeHtml(classroomStatusLabel(c.status))}</span>
      </div>
      <dl class="text-[#333] space-y-1">
        <div>Học viên: <strong>${escapeHtml(c.learnerName || 'Học viên')}</strong></div>
        <div>Ngày mở lớp: ${escapeHtml(formatDateVi(c.createdAt))}</div>
        <div>Tài liệu: ${Number(c.materialCount) || 0}</div>
      </dl>
      ${c.readOnly ? '<p class="font-meta text-[#BF360C]">Chế độ chỉ xem</p>' : ''}
      <button type="button" class="retro-btn mt-auto" data-open-space="${escapeHtml(c.spaceId)}">Vào lớp học</button>
    </article>
  `).join('');
  grid.querySelectorAll('[data-open-space]').forEach((btn) => {
    btn.addEventListener('click', () => navigateToSpace(btn.dataset.openSpace));
  });
}

// ----------------------------------------------------------------------------
// Thêm học viên vào lớp (UC-93 bước 4–10)
// ----------------------------------------------------------------------------
async function openAddLearner() {
  const select = $('learnerSelect');
  const fields = $('addLearnerFields');
  const eligibleState = $('eligibleState');
  const submit = $('addLearnerSubmit');
  setFieldError('learnerSelectError', '');
  setFieldError('classroomTitleError', '');
  setFieldError('addLearnerFormError', '');
  $('classroomTitleInput').value = '';
  select.innerHTML = '';
  eligibleState.textContent = 'Đang tải danh sách học viên...';
  eligibleState.classList.remove('hidden');
  fields.classList.add('hidden');
  submit.disabled = true;
  openModal('addLearnerModal');

  try {
    const learners = await API.classrooms.eligibleLearners() || [];
    if (!learners.length) {
      eligibleState.textContent = 'Hiện chưa có học viên nào đủ điều kiện. Học viên cần được bạn chấp nhận kết nối và chưa có lớp đang hoạt động với bạn.';
      return;
    }
    select.innerHTML = '<option value="">Chọn học viên</option>' + learners.map((l) =>
      `<option value="${escapeHtml(l.learnerId)}">${escapeHtml(l.fullName || 'Học viên')}${l.email ? ' · ' + escapeHtml(l.email) : ''}</option>`
    ).join('');
    eligibleState.classList.add('hidden');
    fields.classList.remove('hidden');
    submit.disabled = false;
  } catch (err) {
    eligibleState.textContent = friendlyErrorMessage(err);
  }
}

async function submitAddLearner(event) {
  event.preventDefault();
  const learnerId = $('learnerSelect').value;
  const title = $('classroomTitleInput').value;
  const titleError = validateClassroomTitle(title);
  setFieldError('learnerSelectError', learnerId ? '' : 'Vui lòng chọn học viên.');
  setFieldError('classroomTitleError', titleError);
  setFieldError('addLearnerFormError', '');
  if (!learnerId || titleError) return;

  const submit = $('addLearnerSubmit');
  setBusy(submit, true, 'Đang thêm...');
  try {
    await API.classrooms.create({ learnerId, title: title.trim() });
    closeModal('addLearnerModal');
    showToast('Đã thêm học viên vào lớp học và kích hoạt quyền truy cập.');
    await loadClassrooms();
  } catch (err) {
    if (err.fieldErrors?.title) setFieldError('classroomTitleError', err.fieldErrors.title);
    else setFieldError('addLearnerFormError', friendlyErrorMessage(err));
  } finally {
    setBusy(submit, false);
  }
}

// ----------------------------------------------------------------------------
// Chi tiết lớp + tài liệu (UC-61)
// ----------------------------------------------------------------------------
function navigateToSpace(spaceId) {
  const url = new URL(window.location.href);
  url.searchParams.set('space', spaceId);
  window.history.pushState({}, '', url);
  renderRoute();
}

function navigateToList() {
  const url = new URL(window.location.href);
  url.searchParams.delete('space');
  window.history.pushState({}, '', url);
  renderRoute();
}

async function loadSpace(spaceId) {
  const stateBox = $('classroomDetailState');
  const card = $('classroomDetailCard');
  const section = $('materialsSection');
  stateBox.classList.add('hidden');
  card.classList.add('hidden');
  section.classList.add('hidden');
  try {
    state.currentSpace = await API.classrooms.detail(spaceId);
    renderSpace();
    section.classList.remove('hidden');
    await loadMaterials();
  } catch (err) {
    renderStateBox(stateBox, friendlyErrorMessage(err), { retry: () => loadSpace(spaceId) });
  }
}

function renderSpace() {
  const c = state.currentSpace;
  $('detailTitle').textContent = c.title;
  const badge = $('detailStatus');
  badge.textContent = classroomStatusLabel(c.status);
  badge.dataset.tone = classroomStatusTone(c.status);
  $('detailLearner').textContent = c.learnerName || 'Học viên';
  $('detailCreated').textContent = formatDateVi(c.createdAt);

  const note = $('detailReadOnlyNote');
  if (c.readOnly) {
    note.textContent = c.status === 'ACTIVE'
      ? 'Gói hội viên của bạn đã hết hạn nên lớp học đang ở chế độ chỉ xem. Vui lòng gia hạn để tiếp tục tải lên hoặc cập nhật tài liệu.'
      : 'Lớp học không còn hoạt động nên đang ở chế độ chỉ xem. Bạn vẫn có thể xem lại tài liệu đã chia sẻ.';
    note.classList.remove('hidden');
  } else {
    note.classList.add('hidden');
  }
  $('openUploadBtn').classList.toggle('hidden', !c.canManage);
  $('classroomDetailCard').classList.remove('hidden');
}

async function loadMaterials() {
  const list = $('materialsList');
  const stateBox = $('materialsState');
  stateBox.classList.add('hidden');
  list.innerHTML = '<li class="text-[#444]">Đang tải tài liệu...</li>';
  try {
    state.materials = await API.classrooms.materials(state.currentSpace.spaceId) || [];
    renderMaterials();
  } catch (err) {
    list.innerHTML = '';
    renderStateBox(stateBox, friendlyErrorMessage(err), { retry: loadMaterials });
  }
}

function renderMaterials() {
  const list = $('materialsList');
  const stateBox = $('materialsState');
  if (!state.materials.length) {
    list.innerHTML = '';
    renderStateBox(stateBox, 'Hiện chưa có tài liệu nào trong lớp học này.');
    return;
  }
  stateBox.classList.add('hidden');
  const canManage = !!state.currentSpace?.canManage;
  list.innerHTML = state.materials.map((m) => `
    <li class="border border-black rounded-[5px] p-4 bg-white flex flex-col sm:flex-row sm:items-start gap-3">
      <div class="flex-1 min-w-0 space-y-1">
        <h3 class="font-semibold text-[16px] break-words">${escapeHtml(m.title)}</h3>
        ${m.description ? `<p class="text-[#333] whitespace-pre-line break-words">${escapeHtml(m.description)}</p>` : ''}
        <p class="font-meta text-[#555]">${escapeHtml(fileTypeLabel(m.fileType))} · Tải lên ngày ${escapeHtml(formatDateVi(m.createdAt))}</p>
      </div>
      <div class="flex gap-2 shrink-0">
        <button type="button" class="retro-btn" data-open-material="${escapeHtml(m.materialId)}">Mở tài liệu</button>
        ${canManage ? `<button type="button" class="retro-btn" data-edit-material="${escapeHtml(m.materialId)}">Chỉnh sửa</button>` : ''}
      </div>
    </li>
  `).join('');
  list.querySelectorAll('[data-open-material]').forEach((btn) => {
    btn.addEventListener('click', () => openMaterial(btn.dataset.openMaterial, btn));
  });
  list.querySelectorAll('[data-edit-material]').forEach((btn) => {
    btn.addEventListener('click', () => openMaterialForm(state.materials.find((m) => m.materialId === btn.dataset.editMaterial)));
  });
}

async function openMaterial(materialId, button) {
  // Mở tab trước (đồng bộ với thao tác bấm) để trình duyệt không chặn cửa sổ bật lên.
  const tab = window.open('', '_blank');
  setBusy(button, true, 'Đang mở...');
  try {
    const link = await API.classrooms.materialFileLink(state.currentSpace.spaceId, materialId);
    if (tab) tab.location.href = link.url;
    else window.location.href = link.url;
  } catch (err) {
    if (tab) tab.close();
    showToast(friendlyErrorMessage(err), 'error');
  } finally {
    setBusy(button, false);
  }
}

// ----------------------------------------------------------------------------
// Tải lên (UC-65) và cập nhật (UC-94) tài liệu
// ----------------------------------------------------------------------------
function openMaterialForm(material = null) {
  state.editingMaterial = material;
  const isEdit = !!material;
  $('materialModalTitle').textContent = isEdit ? 'Chỉnh sửa tài liệu' : 'Tải lên tài liệu';
  $('materialSubmit').textContent = isEdit ? 'Lưu thay đổi' : 'Tải lên';
  $('materialTitleInput').value = material?.title || '';
  $('materialDescriptionInput').value = material?.description || '';
  const fileInput = $('materialFileInput');
  fileInput.value = '';
  fileInput.accept = MATERIAL_ACCEPT_ATTR;
  $('materialFileHint').textContent = isEdit
    ? 'Chọn tệp mới nếu muốn thay thế (để trống sẽ giữ tệp hiện tại). Định dạng PDF, PNG, JPG, MP3, tối đa 10 MB.'
    : 'Định dạng PDF, PNG, JPG, MP3. Dung lượng tối đa 10 MB.';
  ['materialTitleError', 'materialDescriptionError', 'materialFileError', 'materialFormError'].forEach((id) => setFieldError(id, ''));
  openModal('materialModal');
  $('materialTitleInput').focus();
}

async function submitMaterial(event) {
  event.preventDefault();
  const isEdit = !!state.editingMaterial;
  const title = $('materialTitleInput').value;
  const description = $('materialDescriptionInput').value;
  const file = $('materialFileInput').files[0] || null;

  const fieldErrors = validateMaterialFields({ title, description });
  const fileError = validateMaterialFile(file, { required: !isEdit });
  setFieldError('materialTitleError', fieldErrors.title);
  setFieldError('materialDescriptionError', fieldErrors.description);
  setFieldError('materialFileError', fileError);
  setFieldError('materialFormError', '');
  if (fieldErrors.title || fieldErrors.description || fileError) return;

  const submit = $('materialSubmit');
  setBusy(submit, true, isEdit ? 'Đang lưu...' : 'Đang tải lên...');
  const payload = { title: title.trim(), description: description.trim(), file };
  try {
    const spaceId = state.currentSpace.spaceId;
    if (isEdit) {
      await API.classrooms.updateMaterial(spaceId, state.editingMaterial.materialId, payload);
    } else {
      await API.classrooms.uploadMaterial(spaceId, payload);
    }
    closeModal('materialModal');
    showToast(isEdit ? 'Cập nhật tài liệu thành công.' : 'Tải lên tài liệu thành công.');
    await loadMaterials();
  } catch (err) {
    const fe = err.fieldErrors || {};
    if (fe.title || fe.description || fe.file) {
      setFieldError('materialTitleError', fe.title);
      setFieldError('materialDescriptionError', fe.description);
      setFieldError('materialFileError', fe.file);
    } else {
      setFieldError('materialFormError', friendlyErrorMessage(err));
    }
  } finally {
    setBusy(submit, false);
  }
}

// ----------------------------------------------------------------------------
// Điều hướng & khởi tạo
// ----------------------------------------------------------------------------
function renderRoute() {
  const spaceId = new URLSearchParams(window.location.search).get('space');
  $('classroomListView').classList.toggle('hidden', !!spaceId);
  $('classroomDetailView').classList.toggle('hidden', !spaceId);
  if (spaceId) {
    loadSpace(spaceId);
  } else {
    state.currentSpace = null;
    loadClassrooms();
  }
}

function hasToken() {
  try {
    return !!localStorage.getItem('kizuna_token');
  } catch (e) {
    return false;
  }
}

function renderTutorName() {
  try {
    const user = JSON.parse(localStorage.getItem('kizuna_user') || 'null');
    if (user?.fullName) $('tutorNameLabel').textContent = user.fullName;
  } catch (e) {
    /* bỏ qua thông tin hiển thị không đọc được */
  }
}

function logout() {
  try {
    localStorage.removeItem('kizuna_token');
    localStorage.removeItem('kizuna_user');
  } catch (e) {
    /* không có quyền truy cập bộ nhớ trình duyệt */
  }
  document.cookie = 'access_token=; Max-Age=0; path=/';
  window.location.href = '/login';
}

function init() {
  const root = $('tutorClassroomsRoot');
  if (!root || root.dataset.initialized === 'true') return;
  root.dataset.initialized = 'true';

  if (!hasToken()) {
    window.location.href = '/login';
    return;
  }
  renderTutorName();

  $('logoutBtn').addEventListener('click', logout);
  $('openAddLearnerBtn').addEventListener('click', openAddLearner);
  $('addLearnerForm').addEventListener('submit', submitAddLearner);
  $('backToListBtn').addEventListener('click', navigateToList);
  $('openUploadBtn').addEventListener('click', () => openMaterialForm(null));
  $('materialForm').addEventListener('submit', submitMaterial);
  document.querySelectorAll('[data-close-modal]').forEach((btn) => {
    btn.addEventListener('click', () => closeModal(btn.dataset.closeModal));
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      closeModal('addLearnerModal');
      closeModal('materialModal');
    }
  });
  window.addEventListener('popstate', renderRoute);
  renderRoute();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
