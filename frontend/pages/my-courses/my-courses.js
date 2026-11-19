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
import { mapLink } from '../../js/address-helpers.js';
import { ymdLocal } from '../../js/assignment-overview-helpers.js';
import { loadLearnerAssignments, hideLearnerAssignments } from './my-assignments.js';

const $ = (id) => document.getElementById(id);
let currentSpace = null;
let materials = [];
const materialFilter = { kind: '', keyword: '' };
const openMaterialGroups = new Set();
let currentTab = 'tong-quan';

// ----------------------------------------------------------------------------
// Tab của lớp: Tổng quan · Bài tập · Tài liệu (?tab=)
// ----------------------------------------------------------------------------
const TAB_PANELS = { 'tong-quan': 'myTabOverview', 'bai-tap': 'myTabAssignments', 'tai-lieu': 'myTabMaterials' };

function setTab(key, { updateUrl = true } = {}) {
  currentTab = normalizeTab(key);
  Object.entries(TAB_PANELS).forEach(([k, id]) => $(id)?.classList.toggle('hidden', k !== currentTab));
  document.querySelectorAll('#myCourseTabs [data-tab]').forEach((b) => {
    b.setAttribute('aria-selected', String(b.dataset.tab === currentTab));
    b.tabIndex = b.dataset.tab === currentTab ? 0 : -1;
  });
  if (updateUrl) {
    const url = new URL(window.location.href);
    if (currentTab === 'tong-quan') url.searchParams.delete('tab');
    else url.searchParams.set('tab', currentTab);
    window.history.replaceState(window.history.state, '', url);
  }
}

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
  const url = new URL(window.location.href);
  url.searchParams.delete('tab');
  window.history.replaceState(window.history.state, '', url);
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
  Object.values(TAB_PANELS).forEach((id) => $(id)?.classList.add('hidden'));
  $('myCourseCrumbClass').textContent = '';
  materials = [];
  materialFilter.kind = '';
  materialFilter.keyword = '';
  openMaterialGroups.clear();
  $('myMaterialSearch').value = '';
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
    const progress = courseCardLines(currentSpace).progress;
    $('myCourseProgress').innerHTML = progress
      ? `<span class="material-symbols-outlined text-[18px]" aria-hidden="true">trending_up</span>${escapeHtml(progress)}` : '';
    $('myCourseProgress').classList.toggle('hidden', !progress);
    card.classList.remove('hidden');
    setTab(new URLSearchParams(window.location.search).get('tab'));
    renderSchedule(currentSpace.schedules);
    loadNextSession(currentSpace);
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
    box.innerHTML = `
      <p class="flex items-center gap-2 text-on-surface-variant"><span class="material-symbols-outlined text-[18px]" aria-hidden="true">home_pin</span>Học trực tiếp</p>
      <div class="flex items-start gap-2.5 rounded-xl bg-surface-container-low px-3.5 py-3">
        <span class="material-symbols-outlined text-[20px] text-primary" aria-hidden="true">location_on</span>
        <div class="min-w-0"><p class="font-semibold text-on-surface">${escapeHtml(space.locationDetails || '')}</p>
          ${space.locationDetails ? `<a href="${escapeHtml(mapLink(space.locationDetails))}" target="_blank" rel="noopener noreferrer" class="inline-flex items-center gap-1 font-label-md text-label-md text-primary hover:underline mt-1">Mở bản đồ<span class="material-symbols-outlined text-[16px]" aria-hidden="true">open_in_new</span></a>` : ''}</div>
      </div>`;
  } else if (space.meetingLink && /^https?:\/\//i.test(space.meetingLink)) {
    box.innerHTML = '<p class="flex items-center gap-2 text-on-surface-variant"><span class="material-symbols-outlined text-[18px]" aria-hidden="true">videocam</span>Học online · bấm “Vào phòng học” ở đầu trang</p>';
    join.innerHTML = `<a href="${escapeHtml(space.meetingLink)}" target="_blank" rel="noopener noreferrer" class="kz-btn kz-btn-primary h-12 px-5 text-[14px] no-underline">
      <span class="material-symbols-outlined text-[20px]" aria-hidden="true">video_call</span>Vào phòng học<span class="material-symbols-outlined text-[16px]" aria-hidden="true">open_in_new</span></a>`;
  } else {
    box.innerHTML = '<p class="flex items-center gap-2 text-on-surface-variant"><span class="material-symbols-outlined text-[18px]" aria-hidden="true">videocam</span>Học online · Gia sư chưa gửi link phòng học.</p>';
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
      <li class="flex items-center gap-3 px-3.5 py-3 rounded-xl bg-surface-container-low">
        <span class="material-symbols-outlined text-[20px] text-primary" aria-hidden="true">calendar_today</span>
        <p class="flex-1 min-w-0"><b class="font-semibold">${escapeHtml(dayOfWeekLabel(s.dayOfWeek))}</b> · ${escapeHtml(shortTime(s.startTime))} – ${escapeHtml(shortTime(s.endTime))}</p>
        <span class="font-body-sm text-[12px] text-on-surface-variant shrink-0">${escapeHtml(durationLabel(s.startTime, s.endTime))}</span>
      </li>
    `).join('');
  }
  $('myCourseSchedule').classList.remove('hidden');
}

/** "Buổi tới: Thứ Năm 20/11 · 19:00" từ các buổi cụ thể của lớp (đã tính buổi dời / buổi thêm). */
async function loadNextSession(space) {
  const line = $('myNextSession');
  line.classList.add('hidden');
  if (!space || space.readOnly) return;
  try {
    const from = new Date();
    const to = new Date(from.getTime() + 45 * 24 * 60 * 60 * 1000);
    const next = nextSessionOf(await API.classrooms.sessions(ymdLocal(from), ymdLocal(to)), space.spaceId);
    if (!next || currentSpace?.spaceId !== space.spaceId) return;
    const d = new Date(`${next.date}T00:00:00`);
    const day = ['Chủ nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'][d.getDay()];
    line.innerHTML = `<span class="font-semibold">Buổi tới:</span> ${day} ${escapeHtml(formatDateVi(next.date).slice(0, 5))} · ${escapeHtml(String(next.startTime).slice(0, 5))}`
      + `${next.number ? ` <span class="text-on-surface-variant">(buổi ${next.number}${next.totalSessions ? `/${next.totalSessions}` : ''})</span>` : ''}`;
    line.classList.remove('hidden');
  } catch (err) {
    // Không có lịch / lỗi mạng: chỉ bỏ dòng "Buổi tới"
  }
}

function materialItemHtml(m) {
  return `
    <li class="flex flex-wrap gap-3.5 p-4 rounded-xl border border-surface-container-high">
      <div class="mc-icon bg-surface-container-low text-primary" aria-hidden="true"><span class="material-symbols-outlined">folder_open</span></div>
      <div class="flex-1 min-w-[200px] space-y-2">
        <div class="flex flex-wrap items-center gap-2">
          <h3 class="font-title-sm text-title-sm text-on-surface [overflow-wrap:anywhere]">${escapeHtml(m.title)}</h3>
          ${isRecent(m.createdAt) ? '<span class="kz-badge" data-tone="active">Mới</span>' : ''}
        </div>
        ${m.description ? `<p class="text-on-surface-variant whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(m.description)}</p>` : ''}
        <div class="flex flex-wrap gap-2">${(m.files || []).map((f) => fileChipHtml(f,
          `data-open-material="${escapeHtml(m.materialId)}" data-file="${escapeHtml(f.fileId)}"`)).join('')}</div>
        <p class="font-body-sm text-[12px] text-on-surface-variant">${(m.files || []).length} tệp · Tải lên ngày ${escapeHtml(formatDateVi(m.createdAt))}</p>
      </div>
    </li>`;
}

function renderMaterialsOverview() {
  const n = materials.length;
  $('myMaterialsBadge').textContent = String(n);
  $('myMaterialsBadge').classList.toggle('hidden', !n);
  $('myOverviewMaterialsMore').textContent = n ? `Xem tất cả ${n} tài liệu` : 'Mở tab Tài liệu';
  const recent = [...materials].sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || ''))).slice(0, 3);
  $('myOverviewMaterials').innerHTML = recent.length ? recent.map((m) => `
    <li class="flex items-center gap-3 px-3.5 py-3 rounded-xl bg-surface-container-low">
      <span class="material-symbols-outlined text-[22px] text-primary shrink-0" aria-hidden="true">${fileKindIcon((m.files || [])[0])}</span>
      <div class="flex-1 min-w-0">
        <p class="font-label-md text-label-md text-on-surface truncate">${escapeHtml(m.title)}</p>
        <p class="font-body-sm text-[12px] text-on-surface-variant">${(m.files || []).length} tệp · ${escapeHtml(formatDateVi(m.createdAt))}</p>
      </div>
      ${isRecent(m.createdAt) ? '<span class="kz-badge" data-tone="active">Mới</span>' : ''}
    </li>`).join('') : '<li class="text-on-surface-variant">Chưa có tài liệu nào.</li>';
}

function renderMaterials() {
  const list = $('myCourseMaterialsList');
  const stateBox = $('myCourseMaterialsState');
  renderMaterialsOverview();
  const counts = countMaterialKinds(materials);
  const kinds = [{ key: '', label: 'Tất cả', icon: '' }, ...FILE_KINDS.filter((k) => counts[k.key])];
  $('myMaterialChips').innerHTML = materials.length ? kinds.map((k) => `
    <button type="button" class="kz-chip" data-kind="${k.key}" aria-pressed="${materialFilter.kind === k.key}">
      ${k.icon ? `<span class="material-symbols-outlined text-[16px]" aria-hidden="true">${k.icon}</span>` : ''}${k.label} · ${k.key ? counts[k.key] : counts.all}</button>`).join('') : '';
  if (!materials.length) {
    list.innerHTML = '';
    renderStateBox(stateBox, 'Hiện chưa có tài liệu nào trong lớp học này.');
    return;
  }
  const filtered = filterMaterials(materials, materialFilter);
  if (!filtered.length) {
    list.innerHTML = '';
    renderStateBox(stateBox, 'Không tìm thấy tài liệu phù hợp.');
    return;
  }
  stateBox.classList.add('hidden');
  if (materialFilter.keyword.trim() || materialFilter.kind) {
    const sorted = [...filtered].sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')));
    list.innerHTML = `<p class="font-body-sm text-[12px] text-on-surface-variant">${sorted.length} tài liệu phù hợp</p>
      <ul class="flex flex-col gap-3">${sorted.map(materialItemHtml).join('')}</ul>`;
  } else {
    list.innerHTML = groupByWeek(filtered, (m) => m.createdAt).map((g, gi) => {
      const open = gi === 0 || openMaterialGroups.has(g.key);
      return `
        <section>
          <button type="button" class="kz-group-head" data-mgroup="${escapeHtml(g.key)}" aria-expanded="${open}">
            <span class="material-symbols-outlined text-[18px]" aria-hidden="true">${open ? 'expand_more' : 'chevron_right'}</span>${escapeHtml(g.label)}<span class="normal-case font-normal">· ${g.items.length} tài liệu</span></button>
          <ul class="flex flex-col gap-3 mt-2 ${open ? '' : 'hidden'}">${g.items.map(materialItemHtml).join('')}</ul>
        </section>`;
    }).join('');
  }
  list.querySelectorAll('[data-open-material]').forEach((btn) => {
    btn.addEventListener('click', () => openMaterial(btn.dataset.openMaterial, btn.dataset.file, btn));
  });
}

async function loadMaterials() {
  const list = $('myCourseMaterialsList');
  const stateBox = $('myCourseMaterialsState');
  stateBox.classList.add('hidden');
  list.innerHTML = '<p class="text-on-surface-variant">Đang tải tài liệu...</p>';
  try {
    materials = await API.classrooms.materials(currentSpace.spaceId) || [];
    renderMaterials();
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
  document.querySelectorAll('#myCourseTabs [data-tab]').forEach((b) => b.addEventListener('click', () => setTab(b.dataset.tab)));
  $('myCourseTabs').addEventListener('keydown', (e) => {
    if (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft') return;
    const tabs = [...document.querySelectorAll('#myCourseTabs [data-tab]')];
    const i = tabs.findIndex((t) => t.dataset.tab === currentTab);
    const next = tabs[(i + (e.key === 'ArrowRight' ? 1 : tabs.length - 1)) % tabs.length];
    setTab(next.dataset.tab);
    next.focus();
  });
  document.querySelectorAll('[data-goto-tab]').forEach((b) => b.addEventListener('click', () => setTab(b.dataset.gotoTab)));
  $('myMaterialSearch').addEventListener('input', (e) => {
    materialFilter.keyword = e.target.value;
    renderMaterials();
  });
  $('myMaterialChips').addEventListener('click', (e) => {
    const chip = e.target.closest('[data-kind]');
    if (!chip) return;
    materialFilter.kind = chip.dataset.kind;
    renderMaterials();
  });
  $('myCourseMaterialsList').addEventListener('click', (e) => {
    const head = e.target.closest('[data-mgroup]');
    if (!head) return;
    const open = head.getAttribute('aria-expanded') === 'true';
    if (open) openMaterialGroups.delete(head.dataset.mgroup); else openMaterialGroups.add(head.dataset.mgroup);
    head.setAttribute('aria-expanded', String(!open));
    head.querySelector('.material-symbols-outlined').textContent = open ? 'chevron_right' : 'expand_more';
    head.nextElementSibling.classList.toggle('hidden', open);
  });
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
