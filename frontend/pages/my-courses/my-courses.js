/**
 * KizunaTutor - FE-06 Trang "Lớp học của tôi" của học viên.
 * UC-58 View Course (kèm lịch học hằng tuần), UC-61 View Learning Materials. Dữ liệu lấy từ API /api/classrooms.
 */

import { API } from '../../js/api.js';
import {
  classroomStatusLabel,
  classroomStatusTone,
  formatDateVi,
  sortSchedules,
  summarizeSchedules,
  courseCardLines,
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
import { fileChipHtml, RETRY_BUTTON_HTML } from './my-courses-ui.js';
import { loadLearnerAssignments, hideLearnerAssignments } from './my-assignments.js';

const $ = (id) => document.getElementById(id);
let currentSpace = null;

function renderStateBox(el, message, retry) {
  el.innerHTML = `
    <p>${escapeHtml(message)}</p>
    ${retry ? RETRY_BUTTON_HTML : ''}
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
  grid.innerHTML = '<p class="text-on-surface-variant">Đang tải danh sách lớp học...</p>';
  try {
    const courses = await API.classrooms.list() || [];
    if (!courses.length) {
      grid.innerHTML = '';
      renderStateBox(stateBox, 'Bạn chưa tham gia lớp học nào. Sau khi gia sư chấp nhận kết nối và mở lớp, lớp học sẽ hiển thị tại đây.');
      return;
    }
    grid.innerHTML = courses.map((c) => {
      const lines = courseCardLines(c);
      const active = c.status === 'ACTIVE';
      return `
      <article class="kz-card p-5 flex flex-col gap-4">
        <div class="flex items-start gap-3">
          <div class="mc-icon ${active ? 'bg-primary-fixed text-on-primary-fixed-variant' : 'bg-surface-container-high text-on-surface-variant'}" aria-hidden="true">
            <span class="material-symbols-outlined text-[24px]">${active ? 'school' : 'pause_circle'}</span></div>
          <div class="flex-1 min-w-0">
            <h3 class="font-title-sm text-title-sm text-on-surface [overflow-wrap:anywhere]">${escapeHtml(c.title)}</h3>
            <p class="font-body-sm text-body-sm text-on-surface-variant">Gia sư ${escapeHtml(c.tutorName || '')}</p>
          </div>
          <span class="kz-badge shrink-0" data-tone="${classroomStatusTone(c.status)}">${escapeHtml(classroomStatusLabel(c.status))}</span>
        </div>
        <ul class="space-y-1.5 text-on-surface-variant">
          <li class="flex items-start gap-2"><span class="material-symbols-outlined text-[18px]" aria-hidden="true">date_range</span><span class="min-w-0">${escapeHtml(lines.period)}</span></li>
          ${lines.progress ? `<li class="flex items-start gap-2"><span class="material-symbols-outlined text-[18px]" aria-hidden="true">trending_up</span><span class="min-w-0">Tiến độ: ${escapeHtml(lines.progress)}</span></li>` : ''}
          <li class="flex items-start gap-2"><span class="material-symbols-outlined text-[18px]" aria-hidden="true">calendar_month</span><span class="min-w-0">${c.schedules?.length ? escapeHtml(summarizeSchedules(c.schedules)) : 'Chưa có lịch học'}</span></li>
          <li class="flex items-start gap-2"><span class="material-symbols-outlined text-[18px]" aria-hidden="true">folder_open</span><span class="min-w-0">${Number(c.materialCount) || 0} tài liệu</span></li>
        </ul>
        <button type="button" class="kz-btn ${active ? 'kz-btn-primary' : ''} mt-auto h-11" data-open-course="${escapeHtml(c.spaceId)}">${active ? 'Vào lớp học<span class="material-symbols-outlined text-[18px]" aria-hidden="true">arrow_forward</span>' : 'Xem lớp học'}</button>
      </article>`;
    }).join('');
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
  $('myCourseCrumbClass').textContent = '';
  hideLearnerAssignments();
  try {
    currentSpace = await API.classrooms.detail(spaceId);
    $('myCourseTitle').textContent = currentSpace.title;
    $('myCourseCrumbClass').textContent = currentSpace.title;
    const badge = $('myCourseStatus');
    badge.textContent = classroomStatusLabel(currentSpace.status);
    badge.dataset.tone = classroomStatusTone(currentSpace.status);
    $('myCourseTutor').textContent = currentSpace.tutorName || 'Gia sư';
    $('myCourseCreated').textContent = formatDateVi(currentSpace.courseStartDate || currentSpace.createdAt);
    renderDelivery(currentSpace);
    $('myCourseReadOnlyNote').classList.toggle('hidden', !currentSpace.readOnly);
    $('myCourseReadOnlyNote').textContent = currentSpace.status === 'COMPLETED'
      ? 'Khóa học đã kết thúc. Bạn vẫn xem lại được tài liệu, bài tập và điểm của lớp này.'
      : 'Lớp học đang ở chế độ chỉ xem. Bạn vẫn có thể xem lại các tài liệu đã được chia sẻ.';
    card.classList.remove('hidden');
    renderSchedule(currentSpace.schedules);
    materialsCard.classList.remove('hidden');
    loadLearnerAssignments(currentSpace, { notify: showError });
    await loadMaterials();
  } catch (err) {
    renderStateBox(stateBox, friendlyErrorMessage(err), () => loadCourse(spaceId));
  }
}

/** Hình thức học: lớp online có nút vào phòng học (khi gia sư đã gửi link), lớp trực tiếp hiện địa điểm. */
function renderDelivery(space) {
  const box = $('myCourseDelivery');
  const join = $('myCourseJoin');
  join.innerHTML = '';
  if (space.learningMode === 'OFFLINE') {
    box.innerHTML = `<span class="material-symbols-outlined text-[18px]" aria-hidden="true">home_pin</span>Học trực tiếp: <strong class="font-semibold text-on-surface">${escapeHtml(space.locationDetails || '')}</strong>`;
  } else if (space.meetingLink && /^https?:\/\//i.test(space.meetingLink)) {
    box.innerHTML = '<span class="material-symbols-outlined text-[18px]" aria-hidden="true">videocam</span>Học online';
    join.innerHTML = `<a href="${escapeHtml(space.meetingLink)}" target="_blank" rel="noopener noreferrer" class="kz-btn kz-btn-primary h-12 px-5 text-[14px] no-underline">
      <span class="material-symbols-outlined text-[20px]" aria-hidden="true">video_call</span>Vào phòng học<span class="material-symbols-outlined text-[16px]" aria-hidden="true">open_in_new</span></a>`;
  } else {
    box.innerHTML = '<span class="material-symbols-outlined text-[18px]" aria-hidden="true">videocam</span>Học online · Gia sư chưa gửi link phòng học.';
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
      <li class="flex items-center gap-3 p-3.5 rounded-xl bg-surface-container-low">
        <div class="mc-icon bg-surface-container-lowest text-primary" aria-hidden="true"><span class="material-symbols-outlined">calendar_today</span></div>
        <div class="min-w-0">
          <p class="font-title-sm text-title-sm text-on-surface">${escapeHtml(dayOfWeekLabel(s.dayOfWeek))}</p>
          <p>${escapeHtml(shortTime(s.startTime))} – ${escapeHtml(shortTime(s.endTime))}</p>
          <p class="font-body-sm text-[12px] text-on-surface-variant">${escapeHtml(durationLabel(s.startTime, s.endTime))}</p>
        </div>
      </li>
    `).join('');
  }
  $('myCourseSchedule').classList.remove('hidden');
}

async function loadMaterials() {
  const list = $('myCourseMaterialsList');
  const stateBox = $('myCourseMaterialsState');
  stateBox.classList.add('hidden');
  $('myCourseMaterialsCount').textContent = '';
  list.innerHTML = '<li class="text-on-surface-variant">Đang tải tài liệu...</li>';
  try {
    const materials = await API.classrooms.materials(currentSpace.spaceId) || [];
    if (!materials.length) {
      list.innerHTML = '';
      renderStateBox(stateBox, 'Hiện chưa có tài liệu nào trong lớp học này.');
      return;
    }
    $('myCourseMaterialsCount').textContent = `· ${materials.length} tài liệu`;
    list.innerHTML = materials.map((m) => `
      <li class="flex gap-3.5 p-4 rounded-xl border border-surface-container-high">
        <div class="mc-icon bg-surface-container-low text-primary" aria-hidden="true"><span class="material-symbols-outlined">folder_open</span></div>
        <div class="flex-1 min-w-0 space-y-2">
          <h3 class="font-title-sm text-title-sm text-on-surface [overflow-wrap:anywhere]">${escapeHtml(m.title)}</h3>
          ${m.description ? `<p class="text-on-surface-variant whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(m.description)}</p>` : ''}
          <div class="flex flex-wrap gap-2">${(m.files || []).map((f) => fileChipHtml(f,
            `data-open-material="${escapeHtml(m.materialId)}" data-file="${escapeHtml(f.fileId)}"`)).join('')}</div>
          <p class="font-body-sm text-[12px] text-on-surface-variant">${(m.files || []).length} tệp · Tải lên ngày ${escapeHtml(formatDateVi(m.createdAt))}</p>
        </div>
      </li>
    `).join('');
    list.querySelectorAll('[data-open-material]').forEach((btn) => {
      btn.addEventListener('click', () => openMaterial(btn.dataset.openMaterial, btn.dataset.file, btn));
    });
  } catch (err) {
    list.innerHTML = '';
    renderStateBox(stateBox, friendlyErrorMessage(err), loadMaterials);
  }
}

async function openMaterial(materialId, fileId, button) {
  const tab = window.open('', '_blank');
  button.disabled = true;
  try {
    const blob = await API.classrooms.materialFile(currentSpace.spaceId, materialId, fileId);
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
