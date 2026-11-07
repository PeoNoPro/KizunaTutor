/**
 * KizunaTutor - FE-06 Trang "Lớp học của tôi" của học viên.
 * UC-58 View Course (kèm lịch học hằng tuần), UC-61 View Learning Materials. Dữ liệu lấy từ API /api/classrooms.
 */

import { API } from '../../js/api.js';
import {
  classroomStatusLabel,
  classroomStatusTone,
  fileTypeLabel,
  formatDateVi,
  sortSchedules,
  summarizeSchedules,
  dayOfWeekLabel,
  shortTime,
  durationLabel,
  friendlyErrorMessage,
  escapeHtml,
  ROLE_LEARNER,
  getCurrentRole,
  roleDeniedHtml,
  clearSessionAndGoToLogin
} from '../../js/classroom-helpers.js';
import { loadLearnerAssignments, hideLearnerAssignments } from './my-assignments.js';

const $ = (id) => document.getElementById(id);
let currentSpace = null;

function renderStateBox(el, message, retry) {
  el.innerHTML = `
    <p class="text-[#333]">${escapeHtml(message)}</p>
    ${retry ? '<button type="button" class="mc-btn mt-4" data-retry>Thử lại</button>' : ''}
  `;
  el.classList.remove('hidden');
  if (retry) el.querySelector('[data-retry]').addEventListener('click', retry);
}

function showError(message) {
  const toast = $('myCoursesToast');
  if (!toast) return;
  toast.textContent = message;
  toast.classList.remove('hidden');
  clearTimeout(showError.timer);
  showError.timer = setTimeout(() => toast.classList.add('hidden'), 4000);
}

// ----------------------------------------------------------------------------
// Danh sách lớp
// ----------------------------------------------------------------------------
async function loadCourses() {
  const grid = $('myCoursesGrid');
  const stateBox = $('myCoursesState');
  stateBox.classList.add('hidden');
  grid.innerHTML = '<p class="text-[#444]">Đang tải danh sách lớp học...</p>';
  try {
    const courses = await API.classrooms.list() || [];
    if (!courses.length) {
      grid.innerHTML = '';
      renderStateBox(stateBox, 'Bạn chưa tham gia lớp học nào. Sau khi gia sư chấp nhận kết nối và mở lớp, lớp học sẽ hiển thị tại đây.');
      return;
    }
    grid.innerHTML = courses.map((c) => `
      <article class="mc-card p-4 flex flex-col gap-3">
        <div class="flex items-start justify-between gap-2">
          <h3 class="mc-heading text-[18px] leading-snug break-words">${escapeHtml(c.title)}</h3>
          <span class="mc-badge shrink-0" data-tone="${classroomStatusTone(c.status)}">${escapeHtml(classroomStatusLabel(c.status))}</span>
        </div>
        <div class="text-[#333] space-y-1">
          <div>Gia sư: <strong>${escapeHtml(c.tutorName || 'Gia sư')}</strong></div>
          <div>Ngày bắt đầu: ${escapeHtml(formatDateVi(c.createdAt))}</div>
          <div>Lịch học: ${c.schedules?.length ? escapeHtml(summarizeSchedules(c.schedules)) : 'Chưa có lịch học'}</div>
          <div>Tài liệu: ${Number(c.materialCount) || 0}</div>
        </div>
        <button type="button" class="mc-btn mt-auto" data-open-course="${escapeHtml(c.spaceId)}">Xem lớp học</button>
      </article>
    `).join('');
    grid.querySelectorAll('[data-open-course]').forEach((btn) => {
      btn.addEventListener('click', () => showCourse(btn.dataset.openCourse));
    });
  } catch (err) {
    grid.innerHTML = '';
    renderStateBox(stateBox, friendlyErrorMessage(err), loadCourses);
  }
}

// ----------------------------------------------------------------------------
// Chi tiết lớp + tài liệu
// ----------------------------------------------------------------------------
function setSpaceParam(spaceId) {
  // replaceState để không xung đột với bộ điều hướng sidebar (popstate) của khung học viên.
  const url = new URL(window.location.href);
  if (spaceId) url.searchParams.set('space', spaceId);
  else url.searchParams.delete('space');
  window.history.replaceState(window.history.state, '', url);
}

function showList() {
  setSpaceParam(null);
  currentSpace = null;
  $('myCourseDetailView').classList.add('hidden');
  $('myCoursesListView').classList.remove('hidden');
  loadCourses();
}

function showCourse(spaceId) {
  setSpaceParam(spaceId);
  $('myCoursesListView').classList.add('hidden');
  $('myCourseDetailView').classList.remove('hidden');
  loadCourse(spaceId);
}

async function loadCourse(spaceId) {
  const stateBox = $('myCourseDetailState');
  const card = $('myCourseDetailCard');
  const materialsCard = $('myCourseMaterials');
  stateBox.classList.add('hidden');
  card.classList.add('hidden');
  materialsCard.classList.add('hidden');
  $('myCourseSchedule').classList.add('hidden');
  hideLearnerAssignments();
  try {
    currentSpace = await API.classrooms.detail(spaceId);
    $('myCourseTitle').textContent = currentSpace.title;
    const badge = $('myCourseStatus');
    badge.textContent = classroomStatusLabel(currentSpace.status);
    badge.dataset.tone = classroomStatusTone(currentSpace.status);
    $('myCourseTutor').textContent = currentSpace.tutorName || 'Gia sư';
    $('myCourseCreated').textContent = formatDateVi(currentSpace.createdAt);
    $('myCourseReadOnlyNote').classList.toggle('hidden', !currentSpace.readOnly);
    card.classList.remove('hidden');
    renderSchedule(currentSpace.schedules);
    materialsCard.classList.remove('hidden');
    loadLearnerAssignments(currentSpace, { notify: showError });
    await loadMaterials();
  } catch (err) {
    renderStateBox(stateBox, friendlyErrorMessage(err), () => loadCourse(spaceId));
  }
}

function renderSchedule(list) {
  const schedules = sortSchedules(list);
  const ul = $('myCourseScheduleList');
  const stateBox = $('myCourseScheduleState');
  if (!schedules.length) {
    ul.innerHTML = '';
    renderStateBox(stateBox, 'Gia sư chưa xếp lịch học cho lớp này.');
  } else {
    stateBox.classList.add('hidden');
    ul.innerHTML = schedules.map((s) => `
      <li class="border border-black rounded-[5px] p-3 bg-[#E5F4EA] flex flex-col gap-0.5">
        <span class="font-semibold text-[16px]">${escapeHtml(dayOfWeekLabel(s.dayOfWeek))}</span>
        <span>${escapeHtml(shortTime(s.startTime))} – ${escapeHtml(shortTime(s.endTime))}</span>
        <span class="mc-meta text-[#555]">${escapeHtml(durationLabel(s.startTime, s.endTime))}</span>
      </li>
    `).join('');
  }
  $('myCourseSchedule').classList.remove('hidden');
}

async function loadMaterials() {
  const list = $('myCourseMaterialsList');
  const stateBox = $('myCourseMaterialsState');
  stateBox.classList.add('hidden');
  list.innerHTML = '<li class="text-[#444]">Đang tải tài liệu...</li>';
  try {
    const materials = await API.classrooms.materials(currentSpace.spaceId) || [];
    if (!materials.length) {
      list.innerHTML = '';
      renderStateBox(stateBox, 'Hiện chưa có tài liệu nào trong lớp học này.');
      return;
    }
    list.innerHTML = materials.map((m) => `
      <li class="border border-black rounded-[5px] p-4 bg-white flex flex-col sm:flex-row sm:items-start gap-3">
        <div class="flex-1 min-w-0 space-y-1">
          <h3 class="font-semibold text-[16px] break-words">${escapeHtml(m.title)}</h3>
          ${m.description ? `<p class="text-[#333] whitespace-pre-line break-words">${escapeHtml(m.description)}</p>` : ''}
          <p class="mc-meta text-[#555]">${escapeHtml(fileTypeLabel(m.fileType))} · Tải lên ngày ${escapeHtml(formatDateVi(m.createdAt))}</p>
        </div>
        <button type="button" class="mc-btn mc-btn-primary shrink-0" data-open-material="${escapeHtml(m.materialId)}">Mở tài liệu</button>
      </li>
    `).join('');
    list.querySelectorAll('[data-open-material]').forEach((btn) => {
      btn.addEventListener('click', () => openMaterial(btn.dataset.openMaterial, btn));
    });
  } catch (err) {
    list.innerHTML = '';
    renderStateBox(stateBox, friendlyErrorMessage(err), loadMaterials);
  }
}

async function openMaterial(materialId, button) {
  const tab = window.open('', '_blank');
  button.disabled = true;
  try {
    const blob = await API.classrooms.materialFile(currentSpace.spaceId, materialId);
    const url = URL.createObjectURL(blob);
    if (tab) tab.location.href = url;
    else window.location.href = url;
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  } catch (err) {
    if (tab) tab.close();
    showError(friendlyErrorMessage(err));
  } finally {
    button.disabled = false;
  }
}

// ----------------------------------------------------------------------------
// Khởi tạo (chạy được cả khi tải trực tiếp lẫn khi điều hướng bằng sidebar)
// ----------------------------------------------------------------------------
function init() {
  const root = $('myCoursesRoot');
  if (!root || root.dataset.initialized === 'true') return;
  root.dataset.initialized = 'true';

  let hasToken = false;
  try {
    hasToken = !!localStorage.getItem('kizuna_token');
  } catch (e) {
    hasToken = false;
  }
  if (!hasToken) {
    window.location.href = '/login';
    return;
  }

  // Chỉ tài khoản học viên được dùng trang Lớp học của tôi
  const role = getCurrentRole();
  if (role !== ROLE_LEARNER) {
    root.innerHTML = roleDeniedHtml('Lớp học của tôi', 'học viên', role);
    root.querySelector('[data-switch-account]')?.addEventListener('click', clearSessionAndGoToLogin);
    return;
  }

  $('myCoursesBackBtn').addEventListener('click', showList);
  const spaceId = new URLSearchParams(window.location.search).get('space');
  if (spaceId) showCourse(spaceId);
  else showList();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
document.addEventListener('DOMContentLoaded', init);
