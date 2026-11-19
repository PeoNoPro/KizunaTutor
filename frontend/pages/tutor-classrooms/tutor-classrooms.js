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
  formatFileSize,
  formatDateVi,
  validateMaterialFile,
  validateMaterialFiles,
  validateMaterialFields,
  FILE_RULES_TEXT,
  MAX_FILES_PER_UPLOAD,
  validateClassroomTitle,
  validateCourseFields,
  minSessionsHint,
  courseSummaryText,
  courseCardLines,
  localYmd,
  validateScheduleList,
  validateScheduleSlot,
  bindSessionTimeSelects,
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
import {
  initTutorAssignments,
  loadTutorAssignments,
  hideTutorAssignments,
  openNewFileAssignment,
  openAssignmentSubmissions
} from './tutor-assignments.js';
import { initAssignmentOverview, showAssignmentOverview, hideAssignmentOverview } from './assignment-overview.js';
import { createFileList } from '../../js/file-picker.js';
import { mountAddressPicker } from '../../js/address-picker.js';
import {
  normalizeTab,
  groupByWeek,
  FILE_KINDS,
  fileKindIcon,
  filterMaterials,
  countMaterialKinds,
  isRecent,
  nextSessionOf
} from '../../js/class-tabs-helpers.js';
import { ymdLocal } from '../../js/assignment-overview-helpers.js';
import { mountTutorLayout } from '/components/tutor-layout/tutor-layout.js';

let materialPicker = null;
/** Khối nhập địa chỉ học trực tiếp của form Tạo lớp và hộp Sửa hình thức học. */
let createAddress = null;
let deliveryAddress = null;
/** Học viên đủ điều kiện tạo lớp (kèm hình thức học trong yêu cầu kết nối) để điền sẵn. */
let eligibleLearners = [];

/** Thay một tài liệu trong danh sách đang hiển thị (không tải lại cả danh sách). */
function upsertMaterialLocally(material, { prepend = false } = {}) {
  const i = state.materials.findIndex((m) => m.materialId === material.materialId);
  if (i >= 0) state.materials[i] = material;
  else if (prepend) state.materials.unshift(material);
  else state.materials.push(material);
  renderMaterials();
}

const $ = (id) => document.getElementById(id);

const state = {
  classrooms: [],
  currentSpace: null,
  materials: [],
  editingMaterial: null,
  deletingMaterial: null,
  editingSchedule: null,
  deletingSchedule: null,
  tab: 'tong-quan',
  materialFilter: { kind: '', keyword: '' },
  collapsedGroups: new Set()
};

// ----------------------------------------------------------------------------
// Tab của lớp: Tổng quan · Bài tập · Tài liệu (?tab=tong-quan|bai-tap|tai-lieu)
// ----------------------------------------------------------------------------
const TAB_PANELS = { 'tong-quan': 'tabOverview', 'bai-tap': 'tabAssignments', 'tai-lieu': 'tabMaterials' };

/** Tab mở đầu: ?tab=, hoặc theo mục thanh bên (Tài liệu học tập / Quản lý bài tập), mặc định Tổng quan. */
function initialTab() {
  const params = new URLSearchParams(window.location.search);
  if (params.get('tab')) return normalizeTab(params.get('tab'));
  if (params.get('newFile') || params.get('grade')) return 'bai-tap';
  const muc = currentMuc();
  if (muc === 'tai-lieu' || currentView() === 'materials') return 'tai-lieu';
  if (muc === 'bai-tap' || currentView() === 'assignments') return 'bai-tap';
  return 'tong-quan';
}

function setTab(key, { updateUrl = true } = {}) {
  state.tab = normalizeTab(key);
  Object.entries(TAB_PANELS).forEach(([k, id]) => $(id)?.classList.toggle('hidden', k !== state.tab));
  document.querySelectorAll('#classTabs [data-tab]').forEach((b) => {
    b.setAttribute('aria-selected', String(b.dataset.tab === state.tab));
    b.tabIndex = b.dataset.tab === state.tab ? 0 : -1;
  });
  if (updateUrl) {
    const url = new URL(window.location.href);
    if (state.tab === 'tong-quan') url.searchParams.delete('tab');
    else url.searchParams.set('tab', state.tab);
    window.history.replaceState(window.history.state, '', url);
  }
}

// ----------------------------------------------------------------------------
// Sidebar: "Học viên của tôi" / "Tài liệu học tập" / "Quản lý bài tập" cùng mở trang lớp học,
// phân biệt bằng ?muc= để tô sáng đúng mục và cuộn tới đúng phần khi vào một lớp.
// ----------------------------------------------------------------------------
const SIDEBAR_ACTIVE = 'flex items-center gap-3 px-3 py-2.5 transition-colors bg-primary-container text-on-primary-container font-semibold rounded-lg shadow-sm';
const SIDEBAR_IDLE = 'flex items-center gap-3 px-3 py-2.5 rounded-lg text-on-surface-variant font-label-md text-label-md hover:bg-surface-container-high hover:text-on-surface transition-colors';
const SECTION_BY_MUC = {
  'tai-lieu': { path: 'tai-lieu-hoc-tap', sectionId: 'materialsSection' },
  'bai-tap': { path: 'quan-ly-bai-tap', sectionId: 'assignmentsSection' }
};

function currentMuc() {
  const muc = new URLSearchParams(window.location.search).get('muc');
  return SECTION_BY_MUC[muc] ? muc : null;
}

function highlightSidebar() {
  const activePath = SECTION_BY_MUC[currentMuc()]?.path || 'hoc-vien-cua-toi';
  document.querySelectorAll('aside nav a[data-path]').forEach((a) => {
    const active = a.dataset.path === activePath;
    a.className = active ? SIDEBAR_ACTIVE : SIDEBAR_IDLE;
    if (active) a.setAttribute('aria-current', 'page');
    else a.removeAttribute('aria-current');
  });
}

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
  grid.innerHTML = state.classrooms.map((c) => {
    const lines = courseCardLines(c);
    return `
    <article class="kz-item flex flex-col gap-3">
      <div class="flex items-start justify-between gap-2">
        <h3 class="font-title-sm text-title-sm text-on-surface min-w-0 [overflow-wrap:anywhere]">${escapeHtml(c.title)}</h3>
        <span class="kz-badge shrink-0" data-tone="${classroomStatusTone(c.status)}">${escapeHtml(classroomStatusLabel(c.status))}</span>
      </div>
      <dl class="text-on-surface-variant space-y-1">
        <div>Học viên: <strong>${escapeHtml(c.learnerName || 'Học viên')}</strong></div>
        <div>Thời gian học: ${escapeHtml(lines.period)}</div>
        ${lines.progress ? `<div>Tiến độ: ${escapeHtml(lines.progress)}</div>` : ''}
        <div>Lịch học: ${c.schedules?.length ? escapeHtml(summarizeSchedules(c.schedules)) : '<span class="text-tertiary">Chưa có lịch học</span>'}</div>
        <div>Tài liệu: ${Number(c.materialCount) || 0}</div>
      </dl>
      ${c.readOnly ? '<p class="font-caption text-caption text-tertiary">Chế độ chỉ xem</p>' : ''}
      <button type="button" class="kz-btn kz-btn-primary mt-auto" data-open-space="${escapeHtml(c.spaceId)}">Vào lớp học</button>
    </article>`;
  }).join('');
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
      <select class="kz-input bg-surface-container-lowest" data-field="startTime" aria-label="Giờ bắt đầu" title="Giờ bắt đầu"></select>
      <select class="kz-input bg-surface-container-lowest" data-field="endTime" aria-label="Giờ kết thúc" title="Giờ kết thúc"></select>
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
  bindSessionTimeSelects(row.querySelector('[data-field="startTime"]'), row.querySelector('[data-field="endTime"]'));
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

// ----------------------------------------------------------------------------
// Hình thức học khi tạo lớp: online (link, không bắt buộc) hoặc trực tiếp (tỉnh → phường → địa chỉ cụ thể)
// ----------------------------------------------------------------------------
function checkedMode(name) {
  return document.querySelector(`input[name="${name}"]:checked`)?.value || 'ONLINE';
}

function setMode(name, mode) {
  const radio = document.querySelector(`input[name="${name}"][value="${mode === 'OFFLINE' ? 'OFFLINE' : 'ONLINE'}"]`);
  if (radio) radio.checked = true;
}

function syncCreateMode() {
  const offline = checkedMode('createMode') === 'OFFLINE';
  $('createLinkBox').classList.toggle('hidden', offline);
  $('createAddressBox').classList.toggle('hidden', !offline);
}

/** Chọn học viên → điền sẵn hình thức học + địa điểm theo yêu cầu kết nối học viên đã gửi. */
function prefillCreateMode() {
  const learner = eligibleLearners.find((l) => String(l.learnerId) === $('learnerSelect').value);
  const note = $('createModeNote');
  const mode = learner?.learningMode === 'OFFLINE' ? 'OFFLINE' : 'ONLINE';
  setMode('createMode', mode);
  $('createLinkInput').value = '';
  setFieldError('createLinkError', '');
  createAddress.setValue(mode === 'OFFLINE' ? (learner?.locationDetails || '') : '',
    { sourceNote: 'Học viên ghi khi gửi yêu cầu kết nối' });
  if (learner?.learningMode) {
    note.querySelector('[data-text]').textContent = `Điền sẵn theo yêu cầu kết nối: học viên chọn ${mode === 'OFFLINE' ? 'học trực tiếp' : 'học online'}. Bạn vẫn đổi được.`;
    note.classList.remove('hidden');
  } else {
    note.classList.add('hidden');
  }
  syncCreateMode();
}

/** @returns {{payload?: object, ok: boolean}} hình thức học đã kiểm tra của form tạo lớp */
function readCreateDelivery() {
  const learningMode = checkedMode('createMode');
  if (learningMode === 'OFFLINE') {
    const { value, errors } = createAddress.read();
    createAddress.showErrors(errors);
    setFieldError('createLinkError', '');
    return Object.keys(errors).length ? { ok: false } : { ok: true, payload: { learningMode, locationDetails: value } };
  }
  const link = $('createLinkInput').value.trim();
  const linkError = link && !/^https?:\/\/\S+$/i.test(link) ? 'Link phòng học phải bắt đầu bằng http:// hoặc https://' : '';
  setFieldError('createLinkError', linkError);
  createAddress.showErrors({});
  return linkError ? { ok: false } : { ok: true, payload: { learningMode, meetingLink: link || null } };
}

/** @param {string|null} preselectLearnerId học viên chọn sẵn (khi mở từ trang Yêu cầu kết nối / Lịch giảng dạy) */
async function openAddLearner(preselectLearnerId = null) {
  const select = $('learnerSelect');
  const fields = $('addLearnerFields');
  const eligibleState = $('eligibleState');
  const submit = $('addLearnerSubmit');
  setFieldError('learnerSelectError', '');
  setFieldError('classroomTitleError', '');
  setFieldError('addLearnerFormError', '');
  setFieldError('scheduleRowsError', '');
  $('classroomTitleInput').value = '';
  $('courseStartInput').value = localYmd();
  $('courseStartInput').min = localYmd();
  $('courseTotalInput').value = '';
  setFieldError('courseStartError', '');
  setFieldError('courseTotalError', '');
  $('scheduleRows').innerHTML = '';
  addScheduleRow();
  eligibleLearners = [];
  setMode('createMode', 'ONLINE');
  $('createLinkInput').value = '';
  setFieldError('createLinkError', '');
  $('createModeNote').classList.add('hidden');
  createAddress.setValue('');
  syncCreateMode();
  select.innerHTML = '';
  eligibleState.textContent = 'Đang tải danh sách học viên...';
  eligibleState.classList.remove('hidden');
  fields.classList.add('hidden');
  submit.disabled = true;
  openModal('addLearnerModal');

  try {
    const learners = await API.classrooms.eligibleLearners() || [];
    eligibleLearners = learners;
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
    if (preselectLearnerId) {
      if (learners.some((l) => String(l.learnerId) === String(preselectLearnerId))) {
        select.value = preselectLearnerId;
        prefillCreateMode();
        $('classroomTitleInput').focus();
      } else {
        setFieldError('learnerSelectError', 'Học viên bạn chọn chưa đủ điều kiện tạo lớp (chưa đồng ý kết nối hoặc đã có lớp đang hoạt động với bạn).');
      }
    }
  } catch (err) {
    eligibleState.textContent = friendlyErrorMessage(err);
  }
}

async function submitAddLearner(event) {
  event.preventDefault();
  const learnerId = $('learnerSelect').value;
  const title = $('classroomTitleInput').value;
  const schedules = collectScheduleRows();
  const startDate = $('courseStartInput').value;
  const totalSessions = $('courseTotalInput').value;
  const titleError = validateClassroomTitle(title);
  const scheduleError = validateScheduleList(schedules);
  const courseErrors = validateCourseFields({ startDate, totalSessions, weeklySlots: schedules.length });
  setFieldError('learnerSelectError', learnerId ? '' : 'Vui lòng chọn học viên.');
  setFieldError('classroomTitleError', titleError);
  setFieldError('courseStartError', courseErrors.startDate);
  setFieldError('courseTotalError', courseErrors.totalSessions);
  setFieldError('scheduleRowsError', scheduleError);
  setFieldError('addLearnerFormError', '');
  const delivery = readCreateDelivery();
  if (!learnerId || titleError || scheduleError || courseErrors.startDate || courseErrors.totalSessions || !delivery.ok) return;

  const submit = $('addLearnerSubmit');
  setBusy(submit, true, 'Đang tạo lớp...');
  try {
    await API.classrooms.create({
      learnerId, title: title.trim(), schedules, startDate, totalSessions: Number(totalSessions), ...delivery.payload
    });
    closeModal('addLearnerModal');
    showToast('Tạo lớp học thành công.');
    await loadClassrooms();
  } catch (err) {
    const fe = err.fieldErrors || {};
    if (fe.title) setFieldError('classroomTitleError', fe.title);
    else if (fe.startDate) setFieldError('courseStartError', fe.startDate);
    else if (fe.totalSessions) setFieldError('courseTotalError', fe.totalSessions);
    else if (fe.schedules) setFieldError('scheduleRowsError', fe.schedules);
    else if (fe.meetingLink) setFieldError('createLinkError', fe.meetingLink);
    else if (fe.locationDetails) setFieldError('addLearnerFormError', fe.locationDetails);
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
  Object.values(TAB_PANELS).forEach((id) => $(id)?.classList.add('hidden'));
  state.materialFilter = { kind: '', keyword: '' };
  state.collapsedGroups = new Set();
  if ($('materialSearch')) $('materialSearch').value = '';
  try {
    state.currentSpace = await API.classrooms.detail(spaceId);
    renderSpace();
    section.classList.remove('hidden');
    setTab(initialTab());
    loadNextSession();
    const assignmentsLoaded = loadTutorAssignments(state.currentSpace);
    await loadMaterials();
    // Mở từ trang Quản lý bài tập: ?newFile=1[&due=...] giao bài nộp tệp, ?grade=<id> chấm bài nộp tệp
    const params = new URLSearchParams(window.location.search);
    if (params.get('newFile') || params.get('grade')) {
      await assignmentsLoaded;
      setTab('bai-tap');
      if (params.get('newFile')) openNewFileAssignment({ due: params.get('due') });
      else openAssignmentSubmissions(params.get('grade'));
      const url = new URL(window.location.href);
      ['newFile', 'due', 'grade'].forEach((k) => url.searchParams.delete(k));
      window.history.replaceState(null, '', url);
    }
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
      ? 'Bạn chưa có gói hội viên hoặc gói đã hết hạn nên lớp học đang ở chế độ chỉ xem. Vui lòng đăng ký hoặc gia hạn gói để tiếp tục quản lý lớp học.'
      : c.status === 'COMPLETED'
        ? 'Khóa học đã học xong buổi cuối nên lớp chuyển sang chỉ xem. Bạn vẫn chấm được các bài tập còn chờ chấm và có thể mở lớp mới với học viên này.'
        : 'Lớp học không còn hoạt động nên đang ở chế độ chỉ xem. Bạn vẫn có thể xem lại tài liệu đã chia sẻ.';
    note.classList.remove('hidden');
  } else {
    note.classList.add('hidden');
  }
  $('openUploadBtn').classList.toggle('hidden', !c.canManage);
  $('overviewUploadBtn').classList.toggle('hidden', !c.canManage);
  $('overviewAssignBtn').classList.toggle('hidden', !c.canManage);
  $('openRenameBtn').classList.toggle('hidden', !c.canManage);
  const progress = courseCardLines(c).progress;
  $('detailProgress').innerHTML = progress
    ? `<span class="material-symbols-outlined text-[16px] text-primary" aria-hidden="true">trending_up</span>${escapeHtml(progress)}` : '';
  $('detailProgress').classList.toggle('hidden', !progress);
  $('detailProgressDot').classList.toggle('hidden', !progress);
  $('classroomDetailCard').classList.remove('hidden');
  renderCourseInfo();
  renderSchedules();
}

/** "Buổi tới: Thứ Năm 20/11 · 19:00" — lấy từ các buổi cụ thể của lớp (đã tính buổi dời / buổi thêm). */
async function loadNextSession() {
  const line = $('nextSessionLine');
  line.classList.add('hidden');
  const space = state.currentSpace;
  if (!space || space.readOnly) return;
  try {
    const from = new Date();
    const to = new Date(from.getTime() + 45 * 24 * 60 * 60 * 1000);
    const next = nextSessionOf(await API.classrooms.sessions(ymdLocal(from), ymdLocal(to)), space.spaceId);
    if (!next || state.currentSpace?.spaceId !== space.spaceId) return;
    const d = new Date(`${next.date}T00:00:00`);
    const day = ['Chủ nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'][d.getDay()];
    line.innerHTML = `<span class="font-semibold">Buổi tới:</span> ${day} ${escapeHtml(formatDateVi(next.date).slice(0, 5))} · ${escapeHtml(String(next.startTime).slice(0, 5))}`
      + `${next.number ? ` <span class="text-on-surface-variant">(buổi ${next.number}${next.totalSessions ? `/${next.totalSessions}` : ''})</span>` : ''}`;
    line.classList.remove('hidden');
  } catch (err) {
    // Không có lịch / lỗi mạng: chỉ bỏ dòng "Buổi tới", phần còn lại vẫn dùng được
  }
}

// ----------------------------------------------------------------------------
// Khóa học: tổng số buổi + ngày bắt đầu (lịch tự sinh cho đủ số buổi rồi dừng)
// ----------------------------------------------------------------------------
function renderCourseInfo() {
  const c = state.currentSpace;
  $('courseSummary').textContent = courseSummaryText(c);
  $('courseMissingNote').classList.toggle('hidden', !!c.totalSessions || !c.canManage);
  $('openCourseBtn').classList.toggle('hidden', !c.canManage);
  renderDeliveryInfo();
}

// ----------------------------------------------------------------------------
// Hình thức học: online (link phòng học) hoặc trực tiếp (địa điểm)
// ----------------------------------------------------------------------------
function renderDeliveryInfo() {
  const c = state.currentSpace;
  const offline = c.learningMode === 'OFFLINE';
  $('deliverySummary').textContent = offline
    ? `Học trực tiếp · ${c.locationDetails || ''}`
    : `Học online${c.meetingLink ? ` · ${c.meetingLink}` : ''}`;
  $('deliveryMissingNote').classList.toggle('hidden', offline || !!c.meetingLink || !c.canManage);
  $('openDeliveryBtn').classList.toggle('hidden', !c.canManage);
}

function syncDeliveryFields() {
  const offline = checkedMode('deliveryMode') === 'OFFLINE';
  $('deliveryLinkBox').classList.toggle('hidden', offline);
  $('deliveryLocationBox').classList.toggle('hidden', !offline);
}

function openDeliveryModal() {
  const c = state.currentSpace;
  const mode = c.learningMode === 'OFFLINE' ? 'OFFLINE' : 'ONLINE';
  document.querySelector(`input[name="deliveryMode"][value="${mode}"]`).checked = true;
  $('deliveryLinkInput').value = c.meetingLink || '';
  deliveryAddress.setValue(c.learningMode === 'OFFLINE' ? (c.locationDetails || '') : '', { sourceNote: 'Địa điểm đang lưu' });
  ['deliveryLinkError', 'deliveryLocationError', 'deliveryFormError'].forEach((id) => setFieldError(id, ''));
  syncDeliveryFields();
  openModal('deliveryModal');
}

async function submitDelivery(event) {
  event.preventDefault();
  const learningMode = document.querySelector('input[name="deliveryMode"]:checked')?.value || 'ONLINE';
  const meetingLink = $('deliveryLinkInput').value.trim();
  const address = learningMode === 'OFFLINE' ? deliveryAddress.read() : { value: '', errors: {} };
  deliveryAddress.showErrors(address.errors);
  const locationDetails = address.value;
  const linkError = learningMode === 'ONLINE' && meetingLink && !/^https?:\/\/\S+$/i.test(meetingLink)
    ? 'Link phòng học phải bắt đầu bằng http:// hoặc https://' : '';
  const locationError = Object.keys(address.errors).length > 0;
  setFieldError('deliveryLinkError', linkError);
  setFieldError('deliveryLocationError', '');
  setFieldError('deliveryFormError', '');
  if (linkError || locationError) return;

  const submit = $('deliverySubmit');
  setBusy(submit, true, 'Đang lưu...');
  try {
    state.currentSpace = await API.classrooms.updateDelivery(state.currentSpace.spaceId, {
      learningMode,
      meetingLink: learningMode === 'ONLINE' ? (meetingLink || null) : null,
      locationDetails: learningMode === 'OFFLINE' ? locationDetails : null
    });
    closeModal('deliveryModal');
    showToast('Cập nhật hình thức học thành công.');
    renderSpace();
  } catch (err) {
    const fe = err.fieldErrors || {};
    if (fe.meetingLink || fe.locationDetails) {
      setFieldError('deliveryLinkError', fe.meetingLink);
      setFieldError('deliveryLocationError', fe.locationDetails);
    } else {
      setFieldError('deliveryFormError', friendlyErrorMessage(err));
    }
  } finally {
    setBusy(submit, false);
  }
}

function openCourseModal() {
  const c = state.currentSpace;
  const started = c.startedSessions || 0;
  $('courseEditStartInput').value = c.courseStartDate || localYmd();
  $('courseEditStartInput').min = started > 0 ? '' : localYmd();
  $('courseEditStartInput').disabled = started > 0;
  $('courseEditStartHint').classList.toggle('hidden', started === 0);
  $('courseEditTotalInput').value = c.totalSessions || '';
  const weeklySlots = (c.schedules || []).length;
  $('courseEditTotalInput').min = String(Math.max(1, started, weeklySlots));
  $('courseEditTotalHint').textContent = `${minSessionsHint({ started, weeklySlots })} Lịch học tự sinh cho đủ số buổi này rồi dừng.`;
  ['courseEditStartError', 'courseEditTotalError', 'courseFormError'].forEach((id) => setFieldError(id, ''));
  openModal('courseModal');
}

async function submitCourse(event) {
  event.preventDefault();
  const c = state.currentSpace;
  const started = c.startedSessions || 0;
  const startDate = $('courseEditStartInput').value;
  const totalSessions = $('courseEditTotalInput').value;
  const startChanged = started === 0 && startDate !== c.courseStartDate;
  const errors = validateCourseFields({
    startDate, totalSessions, minTotal: started, weeklySlots: (c.schedules || []).length, checkStartDate: startChanged
  });
  setFieldError('courseEditStartError', errors.startDate);
  setFieldError('courseEditTotalError', errors.totalSessions);
  setFieldError('courseFormError', '');
  if (errors.startDate || errors.totalSessions) return;

  const submit = $('courseSubmit');
  setBusy(submit, true, 'Đang lưu...');
  try {
    state.currentSpace = await API.classrooms.updateCourse(c.spaceId, {
      startDate: startChanged ? startDate : null,
      totalSessions: Number(totalSessions)
    });
    closeModal('courseModal');
    showToast('Cập nhật khóa học thành công.');
    renderSpace();
  } catch (err) {
    const fe = err.fieldErrors || {};
    if (fe.startDate || fe.totalSessions) {
      setFieldError('courseEditStartError', fe.startDate);
      setFieldError('courseEditTotalError', fe.totalSessions);
    } else {
      setFieldError('courseFormError', friendlyErrorMessage(err));
    }
  } finally {
    setBusy(submit, false);
  }
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
    ? 'Các buổi học cố định mỗi tuần. Thêm, sửa hoặc xóa buổi áp dụng cho mọi tuần từ bây giờ (các buổi đã học vẫn được giữ); lớp luôn cần ít nhất một buổi. Muốn đổi riêng một tuần, hãy dùng Lịch giảng dạy.'
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
          <p class="text-on-surface whitespace-nowrap">${escapeHtml(shortTime(s.startTime))} – ${escapeHtml(shortTime(s.endTime))}</p>
          <p class="font-caption text-caption text-on-surface-variant">${escapeHtml(durationLabel(s.startTime, s.endTime))}</p>
        </div>
        ${c.canManage ? `
          <div class="flex flex-col gap-1.5 shrink-0">
            <button type="button" class="kz-btn" data-edit-schedule="${escapeHtml(s.scheduleId)}"><span class="material-symbols-outlined text-[16px]">edit</span>Sửa</button>
            ${schedules.length > 1 ? `<button type="button" class="kz-btn text-error" data-delete-schedule="${escapeHtml(s.scheduleId)}"><span class="material-symbols-outlined text-[16px]">delete</span>Xóa</button>` : ''}
          </div>` : ''}
      </li>
    `).join('');
    list.querySelectorAll('[data-edit-schedule]').forEach((btn) => {
      btn.addEventListener('click', () => openScheduleForm(schedules.find((s) => s.scheduleId === btn.dataset.editSchedule)));
    });
    list.querySelectorAll('[data-delete-schedule]').forEach((btn) => {
      btn.addEventListener('click', () => openDeleteSchedule(schedules.find((s) => s.scheduleId === btn.dataset.deleteSchedule)));
    });
  }
  $('scheduleSection').classList.remove('hidden');
}

function openRename() {
  $('renameInput').value = state.currentSpace?.title || '';
  setFieldError('renameError', '');
  openModal('renameModal');
  $('renameInput').focus();
}

async function submitRename(event) {
  event.preventDefault();
  const title = $('renameInput').value;
  const error = validateClassroomTitle(title);
  setFieldError('renameError', error);
  if (error) return;
  const submit = $('renameSubmit');
  setBusy(submit, true, 'Đang lưu...');
  try {
    state.currentSpace = await API.classrooms.rename(state.currentSpace.spaceId, title.trim());
    closeModal('renameModal');
    showToast('Đổi tên lớp học thành công.');
    renderSpace();
  } catch (err) {
    setFieldError('renameError', err.fieldErrors?.title || friendlyErrorMessage(err));
  } finally {
    setBusy(submit, false);
  }
}

function openDeleteSchedule(schedule) {
  if (!schedule) return;
  state.deletingSchedule = schedule;
  $('deleteScheduleName').textContent = `${dayOfWeekLabel(schedule.dayOfWeek)} ${shortTime(schedule.startTime)} – ${shortTime(schedule.endTime)}`;
  setFieldError('deleteScheduleError', '');
  openModal('deleteScheduleModal');
}

async function confirmDeleteSchedule() {
  const schedule = state.deletingSchedule;
  if (!schedule) return;
  const button = $('deleteScheduleConfirm');
  setBusy(button, true, 'Đang xóa...');
  try {
    const spaceId = state.currentSpace.spaceId;
    await API.classrooms.deleteSchedule(spaceId, schedule.scheduleId);
    closeModal('deleteScheduleModal');
    state.deletingSchedule = null;
    showToast('Xóa buổi học thành công.');
    state.currentSpace = await API.classrooms.detail(spaceId);
    renderSchedules();
  } catch (err) {
    setFieldError('deleteScheduleError', err.fieldErrors?.schedules || friendlyErrorMessage(err));
  } finally {
    setBusy(button, false);
  }
}

function openScheduleForm(schedule = null) {
  state.editingSchedule = schedule;
  const isEdit = !!schedule;
  $('scheduleModalTitle').textContent = isEdit ? 'Sửa buổi học' : 'Thêm buổi học';
  $('scheduleSubmit').textContent = isEdit ? 'Lưu thay đổi' : 'Thêm buổi học';
  $('scheduleDayInput').innerHTML = dayOptionsHtml(schedule?.dayOfWeek, { placeholder: !isEdit });
  bindSessionTimeSelects($('scheduleStartInput'), $('scheduleEndInput'),
    isEdit ? { start: schedule.startTime, end: schedule.endTime } : {});
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

/** Nút mở một tệp: biểu tượng theo loại, tên tệp. */
function materialFileButton(m, f) {
  return `<button type="button" class="inline-flex items-center gap-1.5 h-9 px-3 rounded-lg bg-surface-container-lowest hover:bg-surface-container text-on-surface font-label-md text-[13px] text-left max-w-full"
    data-open-material="${escapeHtml(m.materialId)}" data-file="${escapeHtml(f.fileId)}" title="${escapeHtml(fileTypeLabel(f.fileType))} · ${escapeHtml(formatFileSize(f.fileSize))}">
    <span class="material-symbols-outlined text-[18px] text-primary shrink-0" aria-hidden="true">${fileKindIcon(f)}</span><span class="min-w-0 truncate">${escapeHtml(f.fileName)}</span></button>`;
}

function materialItemHtml(m, canManage) {
  return `
    <li class="kz-item flex flex-wrap items-start gap-3">
      <div class="w-10 h-10 rounded-xl bg-surface-container-lowest text-primary flex items-center justify-center shrink-0" aria-hidden="true">
        <span class="material-symbols-outlined text-[22px]">folder_open</span></div>
      <div class="flex-1 min-w-[200px] space-y-1.5">
        <div class="flex flex-wrap items-center gap-2">
          <h3 class="font-title-sm text-title-sm text-on-surface [overflow-wrap:anywhere]">${escapeHtml(m.title)}</h3>
          ${isRecent(m.createdAt) ? '<span class="kz-badge" data-tone="active">Mới</span>' : ''}
        </div>
        ${m.description ? `<p class="text-on-surface-variant whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(m.description)}</p>` : ''}
        <div class="flex flex-wrap gap-1.5">${(m.files || []).map((f) => materialFileButton(m, f)).join('')}</div>
        <p class="font-caption text-caption text-on-surface-variant">${(m.files || []).length} tệp · Tải lên ngày ${escapeHtml(formatDateVi(m.createdAt))}</p>
      </div>
      ${canManage ? `<div class="flex gap-2 shrink-0">
        <button type="button" class="kz-btn" data-edit-material="${escapeHtml(m.materialId)}">Chỉnh sửa</button>
        <button type="button" class="kz-btn text-error" data-delete-material="${escapeHtml(m.materialId)}">Xóa</button>
      </div>` : ''}
    </li>`;
}

/** Chip lọc loại tệp (kèm số lượng). */
function renderMaterialChips() {
  const counts = countMaterialKinds(state.materials);
  const kinds = [{ key: '', label: 'Tất cả', icon: '' }, ...FILE_KINDS.filter((k) => counts[k.key])];
  $('materialKindChips').innerHTML = kinds.map((k) => `
    <button type="button" class="kz-chip" data-kind="${k.key}" aria-pressed="${state.materialFilter.kind === k.key}">
      ${k.icon ? `<span class="material-symbols-outlined text-[16px]" aria-hidden="true">${k.icon}</span>` : ''}${k.label} · ${k.key ? counts[k.key] : counts.all}</button>`).join('');
}

/** Thẻ "Tài liệu mới nhất" ở tab Tổng quan + số trên tab. */
function renderMaterialsOverview() {
  const n = state.materials.length;
  const badge = $('tabBadgeMaterials');
  badge.textContent = String(n);
  badge.classList.toggle('hidden', !n);
  $('overviewMaterialsMore').textContent = n ? `Xem tất cả ${n} tài liệu` : 'Mở tab Tài liệu';
  const recent = [...state.materials].sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || ''))).slice(0, 3);
  $('overviewMaterials').innerHTML = recent.length ? recent.map((m) => `
    <li class="kz-item flex items-center gap-3 py-3">
      <span class="material-symbols-outlined text-[22px] text-primary shrink-0" aria-hidden="true">${fileKindIcon((m.files || [])[0])}</span>
      <div class="flex-1 min-w-0">
        <p class="font-label-md text-label-md text-on-surface truncate">${escapeHtml(m.title)}</p>
        <p class="font-caption text-caption text-on-surface-variant">${(m.files || []).length} tệp · ${escapeHtml(formatDateVi(m.createdAt))}</p>
      </div>
      ${isRecent(m.createdAt) ? '<span class="kz-badge" data-tone="active">Mới</span>' : ''}
    </li>`).join('')
    : '<li class="kz-item text-center text-on-surface-variant">Chưa có tài liệu nào.</li>';
}

function renderMaterials() {
  const list = $('materialsList');
  const stateBox = $('materialsState');
  renderMaterialsOverview();
  renderMaterialChips();
  if (!state.materials.length) {
    list.innerHTML = '';
    renderStateBox(stateBox, 'Hiện chưa có tài liệu nào trong lớp học này.');
    return;
  }
  const canManage = !!state.currentSpace?.canManage;
  const filtered = filterMaterials(state.materials, state.materialFilter);
  if (!filtered.length) {
    list.innerHTML = '';
    stateBox.innerHTML = '<p class="text-on-surface-variant">Không tìm thấy tài liệu phù hợp.</p><button type="button" class="kz-btn mt-4" data-clear-material-filter>Xóa bộ lọc</button>';
    stateBox.classList.remove('hidden');
    stateBox.querySelector('[data-clear-material-filter]').addEventListener('click', () => {
      state.materialFilter = { kind: '', keyword: '' };
      $('materialSearch').value = '';
      renderMaterials();
    });
    return;
  }
  stateBox.classList.add('hidden');
  const searching = !!(state.materialFilter.keyword.trim() || state.materialFilter.kind);
  if (searching) {
    // Đang tìm / lọc: bỏ nhóm tuần, hiện thẳng kết quả (mới nhất trước)
    const sorted = [...filtered].sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')));
    list.innerHTML = `<p class="font-caption text-caption text-on-surface-variant">${sorted.length} tài liệu phù hợp</p>
      <ul class="flex flex-col gap-3">${sorted.map((m) => materialItemHtml(m, canManage)).join('')}</ul>`;
  } else {
    const groups = groupByWeek(filtered, (m) => m.createdAt);
    list.innerHTML = groups.map((g, gi) => {
      const collapsed = state.collapsedGroups.has(`m:${g.key}`) || (gi > 0 && !state.collapsedGroups.has(`m:${g.key}:open`));
      return `
        <section>
          <button type="button" class="kz-group-head" data-group="m:${g.key}" aria-expanded="${!collapsed}">
            <span class="material-symbols-outlined text-[18px]" aria-hidden="true">${collapsed ? 'chevron_right' : 'expand_more'}</span>${escapeHtml(g.label)}<span class="normal-case font-normal">· ${g.items.length} tài liệu</span></button>
          <ul class="flex flex-col gap-3 mt-2 ${collapsed ? 'hidden' : ''}">${g.items.map((m) => materialItemHtml(m, canManage)).join('')}</ul>
        </section>`;
    }).join('');
  }
  list.querySelectorAll('[data-open-material]').forEach((btn) => {
    btn.addEventListener('click', () => openMaterial(btn.dataset.openMaterial, btn.dataset.file, btn));
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
    state.materials = state.materials.filter((m) => m.materialId !== material.materialId);
    renderMaterials();
  } catch (err) {
    setFieldError('deleteMaterialError', friendlyErrorMessage(err));
  } finally {
    setBusy(button, false);
  }
}

async function openMaterial(materialId, fileId, button) {
  // Mở tab trước (đồng bộ với thao tác bấm) để trình duyệt không chặn cửa sổ bật lên.
  const tab = window.open('', '_blank');
  button.disabled = true;
  try {
    const blob = await API.classrooms.materialFile(state.currentSpace.spaceId, materialId, fileId);
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
  // Một tài liệu = tiêu đề + mô tả + 1–5 tệp. Sửa: "Thay" / "Xóa" đúng tệp muốn đổi, "＋ Thêm tệp" để bổ sung.
  materialPicker.reset(isEdit ? (material.files || []) : [], { max: MAX_FILES_PER_UPLOAD, addLabel: 'Thêm tệp' });
  $('materialFileHint').textContent = isEdit
    ? `Bấm "Thay" để đổi đúng tệp đó, "Xóa" để bỏ tệp, "＋ Thêm tệp" để bổ sung. ${FILE_RULES_TEXT}`
    : FILE_RULES_TEXT;
  ['materialTitleError', 'materialDescriptionError', 'materialFileError', 'materialFormError'].forEach((id) => setFieldError(id, ''));
  openModal('materialModal');
  $('materialTitleInput').focus();
}

async function submitMaterial(event) {
  event.preventDefault();
  const isEdit = !!state.editingMaterial;
  const title = $('materialTitleInput').value;
  const description = $('materialDescriptionInput').value;
  const { newFiles, removeIds, keptCount } = materialPicker.getChanges();

  const fieldErrors = validateMaterialFields({ title, description });
  let fileError = validateMaterialFiles(newFiles, { required: false, existing: keptCount });
  if (!fileError && keptCount + newFiles.length === 0) fileError = 'Tài liệu cần có ít nhất một tệp.';
  setFieldError('materialTitleError', fieldErrors.title);
  setFieldError('materialDescriptionError', fieldErrors.description);
  setFieldError('materialFileError', fileError);
  setFieldError('materialFormError', '');
  if (fieldErrors.title || fieldErrors.description || fileError) return;

  const submit = $('materialSubmit');
  setBusy(submit, true, isEdit ? 'Đang lưu...' : 'Đang tải lên...');
  const payload = { title: title.trim(), description: description.trim(), files: newFiles, removeFileIds: removeIds };
  try {
    const spaceId = state.currentSpace.spaceId;
    const saved = isEdit
      ? await API.classrooms.updateMaterial(spaceId, state.editingMaterial.materialId, payload)
      : await API.classrooms.uploadMaterial(spaceId, payload);
    closeModal('materialModal');
    showToast(isEdit ? 'Cập nhật tài liệu thành công.' : 'Tải lên tài liệu thành công.');
    // Chỉ cập nhật đúng tài liệu vừa lưu, không tải lại cả danh sách
    if (saved && saved.materialId) upsertMaterialLocally(saved, { prepend: !isEdit });
    else await loadMaterials();
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
// Mục thanh bên đang mở: ?view=students | materials | assignments (mặc định: học viên / danh sách lớp)
const VIEW_HINTS = {
  materials: { icon: 'menu_book', text: 'Chọn một lớp học bên dưới để xem và quản lý tài liệu học tập của lớp đó.' },
  assignments: { icon: 'assignment', text: 'Chọn một lớp học bên dưới để giao bài và chấm bài tập của lớp đó.' }
};

function currentView() {
  const view = new URLSearchParams(window.location.search).get('view');
  return ['materials', 'assignments'].includes(view) ? view : 'students';
}

function renderViewHint() {
  // Khối "Quản lý lớp học" (kèm nút "Thêm học viên vào lớp") chỉ có ở mục "Học viên của tôi"
  $('classroomHeaderCard').classList.toggle('hidden', currentView() !== 'students');
  const hint = VIEW_HINTS[currentView()];
  $('viewHint').classList.toggle('hidden', !hint);
  if (hint) {
    $('viewHintIcon').textContent = hint.icon;
    $('viewHintText').textContent = hint.text;
  }
}

function renderRoute() {
  highlightSidebar();
  const spaceId = new URLSearchParams(window.location.search).get('space');
  renderViewHint();
  // Mục "Quản lý bài tập" trên thanh bên: trang bài tập chung của mọi lớp (thay cho danh sách lớp)
  const overview = !spaceId && currentView() === 'assignments';
  $('classroomListView').classList.toggle('hidden', !!spaceId || overview);
  $('classroomDetailView').classList.toggle('hidden', !spaceId);
  if (overview) {
    state.currentSpace = null;
    showAssignmentOverview();
    return;
  }
  hideAssignmentOverview();
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

function init() {
  const root = $('tutorClassroomsRoot');
  if (!root || root.dataset.initialized === 'true') return;
  root.dataset.initialized = 'true';

  if (!hasToken()) {
    window.location.href = '/login';
    return;
  }
  // Sidebar + menu tài khoản dùng chung với các trang gia sư khác (tên, đăng xuất ở góc dưới trái)
  mountTutorLayout(currentView());

  // Chỉ tài khoản gia sư được dùng trang Quản lý lớp học
  const role = getCurrentRole();
  if (role !== ROLE_TUTOR) {
    root.innerHTML = roleDeniedHtml('Quản lý lớp học', 'gia sư', role);
    root.querySelector('[data-switch-account]')?.addEventListener('click', clearSessionAndGoToLogin);
    return;
  }

  createAddress = mountAddressPicker($('createAddressBox'), { idPrefix: 'createAddr' });
  deliveryAddress = mountAddressPicker($('deliveryLocationBox'), { idPrefix: 'deliveryAddr' });
  $('learnerSelect').addEventListener('change', prefillCreateMode);
  document.querySelectorAll('input[name="createMode"]').forEach((r) => r.addEventListener('change', syncCreateMode));
  $('openAddLearnerBtn').addEventListener('click', () => openAddLearner());
  $('addLearnerForm').addEventListener('submit', submitAddLearner);
  $('addScheduleRowBtn').addEventListener('click', addScheduleRow);
  $('openAddScheduleBtn').addEventListener('click', () => openScheduleForm(null));
  $('scheduleForm').addEventListener('submit', submitSchedule);
  $('openRenameBtn').addEventListener('click', openRename);
  $('openCourseBtn').addEventListener('click', openCourseModal);
  $('courseForm').addEventListener('submit', submitCourse);
  $('openDeliveryBtn').addEventListener('click', openDeliveryModal);
  $('deliveryForm').addEventListener('submit', submitDelivery);
  document.querySelectorAll('input[name="deliveryMode"]').forEach((r) => r.addEventListener('change', syncDeliveryFields));
  $('deleteScheduleConfirm').addEventListener('click', confirmDeleteSchedule);
  $('renameForm').addEventListener('submit', submitRename);
  $('backToListBtn').addEventListener('click', navigateToList);
  $('openUploadBtn').addEventListener('click', () => openMaterialForm(null));
  $('overviewUploadBtn').addEventListener('click', () => openMaterialForm(null));
  $('overviewAssignBtn').addEventListener('click', () => $('openAssignmentFormBtn').click());
  document.querySelectorAll('#classTabs [data-tab]').forEach((b) => b.addEventListener('click', () => setTab(b.dataset.tab)));
  $('classTabs').addEventListener('keydown', (e) => {
    if (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft') return;
    const tabs = [...document.querySelectorAll('#classTabs [data-tab]')];
    const i = tabs.findIndex((t) => t.dataset.tab === state.tab);
    const next = tabs[(i + (e.key === 'ArrowRight' ? 1 : tabs.length - 1)) % tabs.length];
    setTab(next.dataset.tab);
    next.focus();
  });
  document.querySelectorAll('[data-goto-tab]').forEach((b) => b.addEventListener('click', () => {
    setTab(b.dataset.gotoTab);
    $('classroomDetailCard').scrollIntoView({ behavior: 'smooth', block: 'start' });
  }));
  $('materialSearch').addEventListener('input', (e) => {
    state.materialFilter.keyword = e.target.value;
    renderMaterials();
  });
  $('materialKindChips').addEventListener('click', (e) => {
    const chip = e.target.closest('[data-kind]');
    if (!chip) return;
    state.materialFilter.kind = chip.dataset.kind;
    renderMaterials();
  });
  $('materialsList').addEventListener('click', (e) => {
    const head = e.target.closest('[data-group]');
    if (!head) return;
    const key = head.dataset.group;
    const open = head.getAttribute('aria-expanded') === 'true';
    state.collapsedGroups.delete(key);
    state.collapsedGroups.delete(`${key}:open`);
    state.collapsedGroups.add(open ? key : `${key}:open`);
    head.setAttribute('aria-expanded', String(!open));
    head.querySelector('.material-symbols-outlined').textContent = open ? 'chevron_right' : 'expand_more';
    head.nextElementSibling.classList.toggle('hidden', open);
  });
  $('materialForm').addEventListener('submit', submitMaterial);
  materialPicker = createFileList($('materialFileList'), { accept: MATERIAL_ACCEPT_ATTR });
  $('deleteMaterialConfirm').addEventListener('click', confirmDeleteMaterial);
  initTutorAssignments({ showToast });
  initAssignmentOverview({ showToast });
  document.querySelectorAll('[data-close-modal]').forEach((btn) => {
    btn.addEventListener('click', () => closeModal(btn.dataset.closeModal));
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      closeModal('addLearnerModal');
      closeModal('deliveryModal');
      closeModal('materialModal');
      closeModal('scheduleModal');
      closeModal('renameModal');
      closeModal('deleteScheduleModal');
    }
  });
  window.addEventListener('popstate', renderRoute);
  renderRoute();
  openCreateFromLink();
}

/**
 * Mở sẵn hộp thoại Tạo lớp khi trang được mở bằng /tutor/classrooms?taoLop=1[&learnerId=...]
 * (nút "Tạo lớp học" của trang Yêu cầu kết nối / Lịch giảng dạy). Xóa tham số khỏi địa chỉ để tải lại không mở lần nữa.
 */
function openCreateFromLink() {
  const url = new URL(window.location.href);
  if (url.searchParams.get('taoLop') !== '1') return;
  const learnerId = url.searchParams.get('learnerId');
  url.searchParams.delete('taoLop');
  url.searchParams.delete('learnerId');
  window.history.replaceState(window.history.state, '', url);
  openAddLearner(learnerId);
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
