/**
 * KizunaTutor - FE-06 Trang quản lý lớp học của gia sư.
 * UC-93 Access Classroom (tạo lớp + lịch học hằng tuần), UC-61 View / UC-65 Upload / UC-94 Update / Delete Learning Materials.
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
  validateScheduleList,
  validateScheduleSlot,
  sortSchedules,
  summarizeSchedules,
  dayOfWeekLabel,
  shortTime,
  durationLabel,
  DAYS_OF_WEEK,
  MAX_SCHEDULE_SLOTS,
  friendlyErrorMessage,
  escapeHtml,
  MATERIAL_ACCEPT_ATTR,
  ROLE_TUTOR,
  getCurrentRole,
  roleDeniedHtml,
  clearSessionAndGoToLogin
} from '../../js/classroom-helpers.js';
import { initTutorAssignments, loadTutorAssignments, hideTutorAssignments } from './tutor-assignments.js';

const $ = (id) => document.getElementById(id);

const state = {
  classrooms: [],
  currentSpace: null,
  materials: [],
  editingMaterial: null,
  deletingMaterial: null,
  editingSchedule: null
};

function dayOptionsHtml(selected, { placeholder = false } = {}) {
  return (placeholder ? '<option value="">Chọn thứ</option>' : '') + DAYS_OF_WEEK.map((d) =>
    `<option value="${d.value}" ${Number(selected) === d.value ? 'selected' : ''}>${d.label}</option>`
  ).join('');
}

// ----------------------------------------------------------------------------
// Tiện ích giao diện
// ----------------------------------------------------------------------------
function showToast(message, tone = 'success') {
  const toast = $('classroomToast');
  if (!toast) return;
  toast.textContent = message;
  toast.style.background = tone === 'success' ? '#cce5ff' : '#ffdad6';
  toast.style.color = tone === 'success' ? '#004b73' : '#93000a';
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
    <p class="text-on-surface-variant">${escapeHtml(message)}</p>
    ${retry ? '<button type="button" class="kz-btn mt-4" data-retry>Thử lại</button>' : ''}
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
  grid.innerHTML = '<p class="text-on-surface-variant">Đang tải danh sách lớp học...</p>';
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
    renderStateBox(stateBox, 'Bạn chưa có lớp học nào. Hãy bấm "Tạo lớp học" để mở lớp và xếp lịch cho học viên đã kết nối.');
    return;
  }
  stateBox.classList.add('hidden');
  grid.innerHTML = state.classrooms.map((c) => `
    <article class="kz-item flex flex-col gap-3">
      <div class="flex items-start justify-between gap-2">
        <h3 class="font-title-sm text-title-sm text-on-surface break-words">${escapeHtml(c.title)}</h3>
        <span class="kz-badge shrink-0" data-tone="${classroomStatusTone(c.status)}">${escapeHtml(classroomStatusLabel(c.status))}</span>
      </div>
      <dl class="text-on-surface-variant space-y-1">
        <div>Học viên: <strong>${escapeHtml(c.learnerName || 'Học viên')}</strong></div>
        <div>Ngày mở lớp: ${escapeHtml(formatDateVi(c.createdAt))}</div>
        <div>Lịch học: ${c.schedules?.length ? escapeHtml(summarizeSchedules(c.schedules)) : '<span class="text-tertiary">Chưa có lịch học</span>'}</div>
        <div>Tài liệu: ${Number(c.materialCount) || 0}</div>
      </dl>
      ${c.readOnly ? '<p class="font-caption text-caption text-tertiary">Chế độ chỉ xem</p>' : ''}
      <button type="button" class="kz-btn kz-btn-primary mt-auto" data-open-space="${escapeHtml(c.spaceId)}">Vào lớp học</button>
    </article>
  `).join('');
  grid.querySelectorAll('[data-open-space]').forEach((btn) => {
    btn.addEventListener('click', () => navigateToSpace(btn.dataset.openSpace));
  });
}

// ----------------------------------------------------------------------------
// Tạo lớp: chọn học viên đã kết nối + tên lớp + lịch học hằng tuần (UC-93 bước 4–10)
// ----------------------------------------------------------------------------
function scheduleRowHtml() {
  return `
    <div class="kz-item p-3 grid grid-cols-[1fr_1fr_auto] sm:grid-cols-[1.3fr_1fr_1fr_auto] gap-2 items-center" data-schedule-row>
      <select class="kz-input bg-surface-container-lowest col-span-3 sm:col-span-1" data-field="dayOfWeek" aria-label="Thứ trong tuần">${dayOptionsHtml(null, { placeholder: true })}</select>
      <input type="time" step="300" class="kz-input bg-surface-container-lowest" data-field="startTime" aria-label="Giờ bắt đầu" title="Giờ bắt đầu">
      <input type="time" step="300" class="kz-input bg-surface-container-lowest" data-field="endTime" aria-label="Giờ kết thúc" title="Giờ kết thúc">
      <button type="button" class="kz-btn px-2" data-remove-row title="Bỏ buổi này" aria-label="Bỏ buổi này"><span class="material-symbols-outlined text-[18px]">close</span></button>
    </div>`;
}

function refreshScheduleRows() {
  const rows = $('scheduleRows').querySelectorAll('[data-schedule-row]');
  rows.forEach((row) => {
    row.querySelector('[data-remove-row]').classList.toggle('invisible', rows.length <= 1);
  });
  $('addScheduleRowBtn').disabled = rows.length >= MAX_SCHEDULE_SLOTS;
}

function addScheduleRow() {
  const container = $('scheduleRows');
  if (container.querySelectorAll('[data-schedule-row]').length >= MAX_SCHEDULE_SLOTS) return;
  container.insertAdjacentHTML('beforeend', scheduleRowHtml());
  const row = container.lastElementChild;
  row.querySelector('[data-remove-row]').addEventListener('click', () => {
    row.remove();
    refreshScheduleRows();
  });
  refreshScheduleRows();
}

function collectScheduleRows() {
  return [...$('scheduleRows').querySelectorAll('[data-schedule-row]')].map((row) => ({
    dayOfWeek: Number(row.querySelector('[data-field="dayOfWeek"]').value) || null,
    startTime: row.querySelector('[data-field="startTime"]').value,
    endTime: row.querySelector('[data-field="endTime"]').value
  }));
}

async function openAddLearner() {
  const select = $('learnerSelect');
  const fields = $('addLearnerFields');
  const eligibleState = $('eligibleState');
  const submit = $('addLearnerSubmit');
  setFieldError('learnerSelectError', '');
  setFieldError('classroomTitleError', '');
  setFieldError('addLearnerFormError', '');
  setFieldError('scheduleRowsError', '');
  $('classroomTitleInput').value = '';
  $('scheduleRows').innerHTML = '';
  addScheduleRow();
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
  const schedules = collectScheduleRows();
  const titleError = validateClassroomTitle(title);
  const scheduleError = validateScheduleList(schedules);
  setFieldError('learnerSelectError', learnerId ? '' : 'Vui lòng chọn học viên.');
  setFieldError('classroomTitleError', titleError);
  setFieldError('scheduleRowsError', scheduleError);
  setFieldError('addLearnerFormError', '');
  if (!learnerId || titleError || scheduleError) return;

  const submit = $('addLearnerSubmit');
  setBusy(submit, true, 'Đang tạo lớp...');
  try {
    await API.classrooms.create({ learnerId, title: title.trim(), schedules });
    closeModal('addLearnerModal');
    showToast('Tạo lớp học thành công.');
    await loadClassrooms();
  } catch (err) {
    if (err.fieldErrors?.title) setFieldError('classroomTitleError', err.fieldErrors.title);
    else if (err.fieldErrors?.schedules) setFieldError('scheduleRowsError', err.fieldErrors.schedules);
    else if (err.status === 409 && /giờ|lịch/i.test(err.userMessage || '')) setFieldError('scheduleRowsError', friendlyErrorMessage(err));
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
  $('scheduleSection').classList.add('hidden');
  hideTutorAssignments();
  try {
    state.currentSpace = await API.classrooms.detail(spaceId);
    renderSpace();
    section.classList.remove('hidden');
    loadTutorAssignments(state.currentSpace);
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
  renderSchedules();
}

// ----------------------------------------------------------------------------
// Lịch học hằng tuần: xem, sửa, thêm buổi (không có xóa theo nghiệp vụ)
// ----------------------------------------------------------------------------
function renderSchedules() {
  const c = state.currentSpace;
  const list = $('scheduleList');
  const stateBox = $('scheduleState');
  const schedules = sortSchedules(c.schedules);
  $('openAddScheduleBtn').classList.toggle('hidden', !c.canManage);
  $('scheduleSubtitle').textContent = c.canManage
    ? 'Các buổi học cố định mỗi tuần. Bạn có thể sửa giờ hoặc thêm buổi, lịch học không thể xóa.'
    : 'Các buổi học cố định mỗi tuần của lớp.';
  if (!schedules.length) {
    list.innerHTML = '';
    renderStateBox(stateBox, c.canManage
      ? 'Lớp học chưa có lịch học. Bấm "Thêm buổi học" để xếp lịch cho học viên.'
      : 'Lớp học chưa có lịch học.');
  } else {
    stateBox.classList.add('hidden');
    list.innerHTML = schedules.map((s) => `
      <li class="kz-item flex items-center gap-3">
        <div class="w-11 h-11 rounded-xl bg-primary-fixed text-on-primary-fixed-variant flex items-center justify-center shrink-0">
          <span class="material-symbols-outlined text-[22px]">event_repeat</span>
        </div>
        <div class="flex-1 min-w-0">
          <p class="font-title-sm text-title-sm text-on-surface">${escapeHtml(dayOfWeekLabel(s.dayOfWeek))}</p>
          <p class="text-on-surface-variant">${escapeHtml(shortTime(s.startTime))} – ${escapeHtml(shortTime(s.endTime))} <span class="font-caption text-caption">· ${escapeHtml(durationLabel(s.startTime, s.endTime))}</span></p>
        </div>
        ${c.canManage ? `<button type="button" class="kz-btn shrink-0" data-edit-schedule="${escapeHtml(s.scheduleId)}"><span class="material-symbols-outlined text-[16px]">edit</span>Sửa</button>` : ''}
      </li>
    `).join('');
    list.querySelectorAll('[data-edit-schedule]').forEach((btn) => {
      btn.addEventListener('click', () => openScheduleForm(schedules.find((s) => s.scheduleId === btn.dataset.editSchedule)));
    });
  }
  $('scheduleSection').classList.remove('hidden');
}

function openScheduleForm(schedule = null) {
  state.editingSchedule = schedule;
  const isEdit = !!schedule;
  $('scheduleModalTitle').textContent = isEdit ? 'Sửa buổi học' : 'Thêm buổi học';
  $('scheduleSubmit').textContent = isEdit ? 'Lưu thay đổi' : 'Thêm buổi học';
  $('scheduleDayInput').innerHTML = dayOptionsHtml(schedule?.dayOfWeek, { placeholder: !isEdit });
  $('scheduleStartInput').value = isEdit ? shortTime(schedule.startTime) : '';
  $('scheduleEndInput').value = isEdit ? shortTime(schedule.endTime) : '';
  setFieldError('scheduleFormError', '');
  openModal('scheduleModal');
  $('scheduleDayInput').focus();
}

async function submitSchedule(event) {
  event.preventDefault();
  const slot = {
    dayOfWeek: Number($('scheduleDayInput').value) || null,
    startTime: $('scheduleStartInput').value,
    endTime: $('scheduleEndInput').value
  };
  const error = validateScheduleSlot(slot);
  setFieldError('scheduleFormError', error);
  if (error) return;

  const isEdit = !!state.editingSchedule;
  const submit = $('scheduleSubmit');
  setBusy(submit, true, 'Đang lưu...');
  try {
    const spaceId = state.currentSpace.spaceId;
    if (isEdit) await API.classrooms.updateSchedule(spaceId, state.editingSchedule.scheduleId, slot);
    else await API.classrooms.addSchedule(spaceId, slot);
    closeModal('scheduleModal');
    state.editingSchedule = null;
    showToast(isEdit ? 'Cập nhật lịch học thành công.' : 'Thêm buổi học thành công.');
    state.currentSpace = await API.classrooms.detail(spaceId);
    renderSchedules();
  } catch (err) {
    setFieldError('scheduleFormError', err.fieldErrors?.schedules || friendlyErrorMessage(err));
  } finally {
    setBusy(submit, false);
  }
}

async function loadMaterials() {
  const list = $('materialsList');
  const stateBox = $('materialsState');
  stateBox.classList.add('hidden');
  list.innerHTML = '<li class="text-on-surface-variant">Đang tải tài liệu...</li>';
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
    <li class="kz-item flex flex-col sm:flex-row sm:items-start gap-3">
      <div class="flex-1 min-w-0 space-y-1">
        <h3 class="font-title-sm text-title-sm text-on-surface break-words">${escapeHtml(m.title)}</h3>
        ${m.description ? `<p class="text-on-surface-variant whitespace-pre-line break-words">${escapeHtml(m.description)}</p>` : ''}
        <p class="font-caption text-caption text-on-surface-variant">${escapeHtml(fileTypeLabel(m.fileType))} · Tải lên ngày ${escapeHtml(formatDateVi(m.createdAt))}</p>
      </div>
      <div class="flex gap-2 shrink-0">
        <button type="button" class="kz-btn" data-open-material="${escapeHtml(m.materialId)}">Mở tài liệu</button>
        ${canManage ? `<button type="button" class="kz-btn" data-edit-material="${escapeHtml(m.materialId)}">Chỉnh sửa</button>` : ''}
        ${canManage ? `<button type="button" class="kz-btn text-error" data-delete-material="${escapeHtml(m.materialId)}">Xóa</button>` : ''}
      </div>
    </li>
  `).join('');
  list.querySelectorAll('[data-open-material]').forEach((btn) => {
    btn.addEventListener('click', () => openMaterial(btn.dataset.openMaterial, btn));
  });
  list.querySelectorAll('[data-edit-material]').forEach((btn) => {
    btn.addEventListener('click', () => openMaterialForm(state.materials.find((m) => m.materialId === btn.dataset.editMaterial)));
  });
  list.querySelectorAll('[data-delete-material]').forEach((btn) => {
    btn.addEventListener('click', () => openDeleteMaterial(state.materials.find((m) => m.materialId === btn.dataset.deleteMaterial)));
  });
}

function openDeleteMaterial(material) {
  if (!material) return;
  state.deletingMaterial = material;
  $('deleteMaterialName').textContent = material.title;
  setFieldError('deleteMaterialError', '');
  openModal('deleteMaterialModal');
}

async function confirmDeleteMaterial() {
  const material = state.deletingMaterial;
  if (!material) return;
  const button = $('deleteMaterialConfirm');
  setBusy(button, true, 'Đang xóa...');
  try {
    await API.classrooms.deleteMaterial(state.currentSpace.spaceId, material.materialId);
    closeModal('deleteMaterialModal');
    state.deletingMaterial = null;
    showToast('Xóa tài liệu thành công.');
    await loadMaterials();
  } catch (err) {
    setFieldError('deleteMaterialError', friendlyErrorMessage(err));
  } finally {
    setBusy(button, false);
  }
}

async function openMaterial(materialId, button) {
  // Mở tab trước (đồng bộ với thao tác bấm) để trình duyệt không chặn cửa sổ bật lên.
  const tab = window.open('', '_blank');
  setBusy(button, true, 'Đang mở...');
  try {
    const blob = await API.classrooms.materialFile(state.currentSpace.spaceId, materialId);
    const url = URL.createObjectURL(blob);
    if (tab) tab.location.href = url;
    else window.location.href = url;
    setTimeout(() => URL.revokeObjectURL(url), 60000);
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
    const name = (user?.fullName || '').trim();
    if (!name) return;
    $('tutorNameLabel').textContent = name;
    $('tutorNameSidebar').textContent = name;
    // Ảnh đại diện dạng chữ cái đầu (không dùng ảnh mẫu)
    const initials = name.split(/\s+/).slice(-2).map((w) => w.charAt(0)).join('').toUpperCase();
    if (initials) $('tutorAvatar').textContent = initials;
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
  $('logoutBtn').addEventListener('click', logout);

  // Chỉ tài khoản gia sư được dùng trang Quản lý lớp học
  const role = getCurrentRole();
  if (role !== ROLE_TUTOR) {
    root.innerHTML = roleDeniedHtml('Quản lý lớp học', 'gia sư', role);
    root.querySelector('[data-switch-account]')?.addEventListener('click', clearSessionAndGoToLogin);
    return;
  }
  renderTutorName();

  $('openAddLearnerBtn').addEventListener('click', openAddLearner);
  $('addLearnerForm').addEventListener('submit', submitAddLearner);
  $('addScheduleRowBtn').addEventListener('click', addScheduleRow);
  $('openAddScheduleBtn').addEventListener('click', () => openScheduleForm(null));
  $('scheduleForm').addEventListener('submit', submitSchedule);
  $('backToListBtn').addEventListener('click', navigateToList);
  $('openUploadBtn').addEventListener('click', () => openMaterialForm(null));
  $('materialForm').addEventListener('submit', submitMaterial);
  $('deleteMaterialConfirm').addEventListener('click', confirmDeleteMaterial);
  initTutorAssignments({ showToast });
  document.querySelectorAll('[data-close-modal]').forEach((btn) => {
    btn.addEventListener('click', () => closeModal(btn.dataset.closeModal));
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      closeModal('addLearnerModal');
      closeModal('materialModal');
      closeModal('scheduleModal');
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
