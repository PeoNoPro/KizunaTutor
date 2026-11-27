/**
 * KizunaTutor - Trang chung mọi lớp của học viên (mở từ thanh bên, cùng trang "Lớp học của tôi"):
 * - ?view=materials: "Tài liệu học tập" (FE-06) — tài liệu của mọi lớp, tìm / lọc theo lớp, trạng thái lớp, loại tệp, nhóm theo tuần.
 * - ?view=assignments: "Bài tập của tôi" (FE-07) — bài tập của mọi lớp: Cần làm (hạn gần nhất trước) / Chờ chấm / Đã có điểm.
 * Chỉ xem: mở tệp, làm bài; bài nộp tệp mở trong lớp tương ứng.
 */

import { API } from '../../js/api.js';
import { escapeHtml, formatDateVi, friendlyErrorMessage } from '../../js/classroom-helpers.js';
import { formatDateTime, formatScore } from '../../js/exercise-helpers.js';
import { submissionStateLabel, formatDueDate, formatPoints } from '../../js/assignment-helpers.js';
import {
  groupByWeek,
  matchesKeyword,
  FILE_KINDS,
  fileKindIcon,
  filterMaterials,
  countMaterialKinds,
  isRecent,
  learnerStatus,
  countStatuses,
  sortByDeadline,
  deadlineOf,
  timeLeftLabel,
  LEARNER_STATUSES
} from '../../js/class-tabs-helpers.js';
import { fileChipHtml, RETRY_BUTTON_HTML } from './my-courses-ui.js';

const $ = (id) => document.getElementById(id);
const PAGE = 10;

const CLASS_STATUSES = [
  { key: 'active', label: 'Đang hoạt động' },
  { key: 'ended', label: 'Đã hoàn thành' },
  { key: 'all', label: 'Tất cả' }
];
const matchesClassStatus = (c, s) => s === 'all' || (s === 'active' ? c.status === 'ACTIVE' : c.status !== 'ACTIVE');

const hub = {
  view: null,
  classes: [],
  items: [],
  filter: { keyword: '', spaceId: '', status: null, kind: '', chip: null },
  limit: PAGE,
  openGroups: new Set(),
  notify: () => {}
};

const classOf = (it) => hub.classes.find((c) => c.spaceId === it.spaceId) || null;
const classPage = (spaceId, extra = '') => `/pages/my-courses/?space=${encodeURIComponent(spaceId)}${extra}`;

/** Hiện trang chung; trả false nếu ?view= không phải trang chung. */
export function showLearnerHub(view, { notify } = {}) {
  if (view !== 'materials' && view !== 'assignments') return false;
  hub.view = view;
  if (notify) hub.notify = notify;
  $('myCoursesListView').classList.add('hidden');
  $('myCourseDetailView').classList.add('hidden');
  $('myHubView').classList.remove('hidden');
  const materials = view === 'materials';
  $('myHubTitle').textContent = materials ? 'Tài liệu học tập' : 'Bài tập của tôi';
  $('myHubIcon').innerHTML = `<span class="material-symbols-outlined text-[28px]">${materials ? 'menu_book' : 'assignment'}</span>`;
  $('myHubIcon').className = `mc-icon w-14 h-14 rounded-2xl ${materials ? 'bg-secondary-fixed text-on-secondary-container' : 'bg-tertiary-fixed text-on-tertiary-fixed'}`;
  $('myHubSearch').placeholder = materials ? 'Tìm theo tên tài liệu hoặc tên tệp' : 'Tìm theo tên bài tập';
  $('myHubKindBox').classList.toggle('hidden', materials);
  bind();
  load();
  return true;
}

async function load() {
  const list = $('myHubList');
  $('myHubState').classList.add('hidden');
  list.innerHTML = '<p class="text-on-surface-variant">Đang tải...</p>';
  try {
    const classes = await API.classrooms.list() || [];
    const perClass = await Promise.all(classes.map((c) => (hub.view === 'materials'
      ? API.classrooms.materials(c.spaceId).then((ms) => (ms || []).map((m) => ({ kind: 'material', spaceId: c.spaceId, at: m.createdAt, m })))
      : Promise.all([API.assignments.list(c.spaceId), API.exercises.list(c.spaceId).catch(() => [])]).then(([as, es]) => [
        ...(as || []).map((a) => ({ kind: 'file', spaceId: c.spaceId, at: a.createdAt, title: a.title, a })),
        ...(es || []).map((e) => ({ kind: 'online', spaceId: c.spaceId, at: e.createdAt, title: e.title, e }))
      ])).catch(() => null)));
    hub.classes = classes;
    hub.items = perClass.filter(Boolean).flat();
    const failed = perClass.filter((r) => r === null).length;
    if (failed) hub.notify(`Không tải được dữ liệu của ${failed} lớp. Vui lòng tải lại trang.`);
    render();
  } catch (err) {
    list.innerHTML = '';
    stateBox(friendlyErrorMessage(err), load);
  }
}

function stateBox(message, retry = null) {
  const box = $('myHubState');
  box.innerHTML = `<p>${escapeHtml(message)}</p>${retry ? RETRY_BUTTON_HTML : ''}`;
  box.classList.remove('hidden');
  if (retry) box.querySelector('[data-retry]').addEventListener('click', retry);
}

function render() {
  const f = hub.filter;
  if (!f.status) f.status = hub.classes.some((c) => c.status === 'ACTIVE') ? 'active' : 'all';
  const inStatus = (it) => { const c = classOf(it); return c && matchesClassStatus(c, f.status); };
  const classes = hub.classes.filter((c) => matchesClassStatus(c, f.status));
  if (f.spaceId && !classes.some((c) => c.spaceId === f.spaceId)) f.spaceId = '';
  $('myHubStatus').innerHTML = CLASS_STATUSES.map((st) => `<option value="${st.key}" ${f.status === st.key ? 'selected' : ''}>${st.label} (${hub.items.filter((it) => { const c = classOf(it); return c && matchesClassStatus(c, st.key); }).length})</option>`).join('');
  $('myHubClass').innerHTML = '<option value="">Tất cả lớp</option>' + classes.map((c) => `
    <option value="${escapeHtml(c.spaceId)}" ${f.spaceId === c.spaceId ? 'selected' : ''}>${escapeHtml(c.title)} (${hub.items.filter((it) => it.spaceId === c.spaceId).length})</option>`).join('');
  const scoped = hub.items.filter((it) => inStatus(it) && (!f.spaceId || it.spaceId === f.spaceId));
  if (hub.view === 'materials') renderMaterials(scoped);
  else renderAssignments(scoped);
}

// ---------------------------------------------------------------------------- tài liệu

function renderMaterials(scoped) {
  const f = hub.filter;
  const list = $('myHubList');
  const materials = scoped.map((it) => ({ ...it.m, __spaceId: it.spaceId }));
  $('myHubSummary').textContent = hub.classes.length
    ? `${hub.items.length} tài liệu · ${hub.classes.length} lớp học` : 'Bạn chưa có lớp học nào.';
  const counts = countMaterialKinds(materials);
  const kinds = [{ key: '', label: 'Tất cả', icon: '' }, ...FILE_KINDS.filter((k) => counts[k.key])];
  $('myHubChips').innerHTML = materials.length ? kinds.map((k) => `
    <button type="button" class="kz-chip" data-chip="${k.key}" aria-pressed="${f.kind === k.key}">
      ${k.icon ? `<span class="material-symbols-outlined text-[16px]" aria-hidden="true">${k.icon}</span>` : ''}${k.label} · ${k.key ? counts[k.key] || 0 : counts.all}</button>`).join('') : '';
  if (!hub.items.length) {
    list.innerHTML = '';
    stateBox(hub.classes.length ? 'Gia sư chưa chia sẻ tài liệu nào.' : 'Bạn chưa có lớp học nào.');
    return;
  }
  const filtered = filterMaterials(materials, { kind: f.kind, keyword: f.keyword });
  if (!filtered.length) {
    list.innerHTML = '';
    stateBox(f.status === 'active' && !f.keyword && !f.kind ? 'Các lớp đang hoạt động chưa có tài liệu nào.' : 'Không tìm thấy tài liệu phù hợp.');
    return;
  }
  $('myHubState').classList.add('hidden');
  const visible = [...filtered].sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || ''))).slice(0, hub.limit);
  const item = (m) => {
    const c = hub.classes.find((x) => x.spaceId === m.__spaceId);
    return `
      <li class="flex flex-wrap gap-3.5 p-4 rounded-xl border border-surface-container-high" data-space-id="${escapeHtml(m.__spaceId)}">
        <div class="mc-icon bg-surface-container-low text-primary" aria-hidden="true"><span class="material-symbols-outlined">${fileKindIcon((m.files || [])[0])}</span></div>
        <div class="flex-1 min-w-[200px] space-y-2">
          <div class="flex flex-wrap items-center gap-2">
            <h3 class="font-title-sm text-title-sm text-on-surface [overflow-wrap:anywhere]">${escapeHtml(m.title)}</h3>
            ${isRecent(m.createdAt) ? '<span class="kz-badge" data-tone="active">Mới</span>' : ''}
            ${c ? `<a class="kz-badge hover:underline no-underline" href="${classPage(c.spaceId, '&tab=tai-lieu')}"><span class="material-symbols-outlined text-[14px]" aria-hidden="true">school</span>${escapeHtml(c.title)}</a>` : ''}
          </div>
          ${m.description ? `<p class="text-on-surface-variant whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(m.description)}</p>` : ''}
          <div class="flex flex-wrap gap-2">${(m.files || []).map((file) => fileChipHtml(file,
            `data-open-material="${escapeHtml(m.materialId)}" data-file="${escapeHtml(file.fileId)}"`)).join('')}</div>
          <p class="font-body-sm text-[12px] text-on-surface-variant">${(m.files || []).length} tệp · Tải lên ngày ${escapeHtml(formatDateVi(m.createdAt))}</p>
        </div>
      </li>`;
  };
  list.innerHTML = withMore(filtered.length, visible.length, f.keyword.trim() || f.kind
    ? `<p class="font-body-sm text-[12px] text-on-surface-variant">${filtered.length} tài liệu phù hợp</p><ul class="flex flex-col gap-3">${visible.map(item).join('')}</ul>`
    : grouped(visible, (m) => m.createdAt, item, 'tài liệu'));
}

// ---------------------------------------------------------------------------- bài tập

function renderAssignments(scoped) {
  const f = hub.filter;
  const list = $('myHubList');
  const counts = countStatuses(scoped, learnerStatus);
  const allCounts = countStatuses(hub.items, learnerStatus);
  $('myHubSummary').textContent = hub.classes.length
    ? `${allCounts.todo || 0} bài cần làm · ${hub.items.length} bài tập · ${hub.classes.length} lớp học` : 'Bạn chưa có lớp học nào.';
  if (f.chip === null || (f.chip !== 'all' && !counts[f.chip])) f.chip = counts.todo ? 'todo' : 'all';
  $('myHubChips').innerHTML = scoped.length ? LEARNER_STATUSES.filter((st) => st.key === 'all' || counts[st.key]).map((st) => `
    <button type="button" class="kz-chip" data-chip="${st.key}" aria-pressed="${f.chip === st.key}">${st.label} · ${counts[st.key] || 0}</button>`).join('') : '';
  if (!hub.items.length) {
    list.innerHTML = '';
    stateBox(hub.classes.length ? 'Gia sư chưa giao bài tập nào.' : 'Bạn chưa có lớp học nào.');
    return;
  }
  const shown = scoped.filter((it) => (f.chip === 'all' || learnerStatus(it) === f.chip)
    && (!f.kind || it.kind === f.kind) && matchesKeyword(it.title, f.keyword));
  if (!shown.length) {
    list.innerHTML = '';
    stateBox(f.status === 'active' && !f.keyword && !f.kind ? 'Các lớp đang hoạt động chưa có bài tập nào.' : 'Không có bài tập nào khớp bộ lọc.');
    return;
  }
  $('myHubState').classList.add('hidden');
  if (f.chip === 'todo') {
    list.innerHTML = `<p class="font-label-sm text-label-sm uppercase tracking-wide text-on-surface-variant">Hạn gần nhất trước</p>
      <ul class="flex flex-col gap-3">${sortByDeadline(shown).map(assignmentHtml).join('')}</ul>`;
    return;
  }
  const visible = shown.slice(0, hub.limit);
  list.innerHTML = withMore(shown.length, visible.length, grouped(visible, (it) => it.at, assignmentHtml, 'bài'));
}

function assignmentHtml(it) {
  const c = classOf(it);
  const todo = learnerStatus(it) === 'todo';
  const left = todo ? timeLeftLabel(deadlineOf(it)) : '';
  let status;
  let meta;
  let action;
  if (it.kind === 'online') {
    const e = it.e;
    status = e.inProgressAttemptId ? 'Đang làm dở' : e.pendingGrading ? 'Chờ gia sư chấm'
      : e.bestScore !== null && e.bestScore !== undefined ? `${formatScore(e.bestScore)} / ${formatScore(e.maxScore)} điểm`
        : e.state === 'CLOSED' ? 'Đã đóng đề' : 'Chưa làm';
    meta = [`${e.questionCount} câu`, e.timeLimitMinutes ? `${e.timeLimitMinutes} phút` : '', `${e.attemptsUsed}/${e.maxAttempts} lượt`,
      e.closesAt ? `đóng ${formatDateTime(e.closesAt)}` : ''].filter(Boolean).join(' · ');
    const label = e.inProgressAttemptId ? 'Làm tiếp' : e.canStart ? (e.attemptsUsed ? 'Làm lượt mới' : 'Làm bài') : 'Xem kết quả';
    action = `<a class="kz-btn ${todo ? 'kz-btn-primary' : ''} h-11 px-4 no-underline shrink-0" href="/pages/exercise-take/index.html?space=${encodeURIComponent(it.spaceId)}&exercise=${encodeURIComponent(e.exerciseId)}">${label}</a>`;
  } else {
    const a = it.a;
    status = submissionStateLabel(a.submissionState) + (a.grade !== null && a.grade !== undefined ? ` · ${formatPoints(a.grade)}/${formatPoints(a.totalPoints)}` : '');
    meta = `Hạn nộp ${formatDueDate(a.dueDate)} · tối đa ${formatPoints(a.totalPoints)} điểm`;
    action = `<a class="kz-btn ${todo ? 'kz-btn-primary' : ''} h-11 px-4 no-underline shrink-0" href="${classPage(it.spaceId, `&tab=bai-tap&assignment=${encodeURIComponent(a.assignmentId)}`)}">${todo ? 'Xem &amp; nộp bài' : 'Xem bài tập'}</a>`;
  }
  const tone = todo ? 'paused' : learnerStatus(it) === 'graded' ? 'active' : 'done';
  return `
    <li class="flex flex-wrap items-center gap-3.5 p-4 rounded-xl ${todo ? 'border border-surface-container-high' : 'bg-surface-container-low'}">
      <div class="mc-icon ${todo ? (it.kind === 'online' ? 'bg-primary-fixed text-on-primary-fixed-variant' : 'bg-secondary-fixed text-on-secondary-container') : 'bg-surface-container-lowest text-on-surface-variant'}" aria-hidden="true">
        <span class="material-symbols-outlined">${todo ? (it.kind === 'online' ? 'quiz' : 'upload_file') : 'task_alt'}</span></div>
      <div class="flex-1 min-w-[220px] space-y-1">
        <div class="flex flex-wrap items-center gap-2">
          <h3 class="font-title-sm text-title-sm text-on-surface break-words">${escapeHtml(it.title)}</h3>
          <span class="kz-badge ${todo ? '' : 'bg-surface-container-lowest'}">${it.kind === 'online' ? 'Làm trực tiếp' : 'Nộp tệp'}</span>
          <span class="kz-badge" data-tone="${tone}">${escapeHtml(status)}</span>
          ${c ? `<a class="kz-badge hover:underline no-underline" href="${classPage(c.spaceId, '&tab=bai-tap')}"><span class="material-symbols-outlined text-[14px]" aria-hidden="true">school</span>${escapeHtml(c.title)}</a>` : ''}
        </div>
        <p class="font-body-sm text-body-sm text-on-surface-variant">${escapeHtml(meta)}${left ? ` · <b class="text-tertiary">${escapeHtml(left)}</b>` : ''}</p>
      </div>
      ${action}
    </li>`;
}

// ---------------------------------------------------------------------------- dùng chung

function grouped(items, getDate, itemHtml, unit) {
  return groupByWeek(items, getDate).map((g, gi) => {
    const open = gi === 0 || hub.openGroups.has(g.key);
    return `
      <section>
        <button type="button" class="kz-group-head" data-group="${escapeHtml(g.key)}" aria-expanded="${open}">
          <span class="material-symbols-outlined text-[18px]" aria-hidden="true">${open ? 'expand_more' : 'chevron_right'}</span>${escapeHtml(g.label)}<span class="normal-case font-normal">· ${g.items.length} ${unit}</span></button>
        <ul class="flex flex-col gap-3 mt-2 ${open ? '' : 'hidden'}">${g.items.map(itemHtml).join('')}</ul>
      </section>`;
  }).join('');
}

function withMore(total, shown, html) {
  return html + (total > shown ? `
    <div class="flex justify-center"><button type="button" class="kz-btn" data-more>Xem thêm ${Math.min(PAGE, total - shown)} (còn ${total - shown})</button></div>` : '');
}

let bound = false;
function bind() {
  if (bound) return;
  bound = true;
  const reset = () => { hub.limit = PAGE; render(); };
  $('myHubSearch').addEventListener('input', (e) => { hub.filter.keyword = e.target.value; reset(); });
  $('myHubClass').addEventListener('change', (e) => { hub.filter.spaceId = e.target.value; hub.filter.kind = hub.view === 'materials' ? '' : hub.filter.kind; reset(); });
  $('myHubStatus').addEventListener('change', (e) => { hub.filter.status = e.target.value; hub.filter.spaceId = ''; reset(); });
  $('myHubKind').addEventListener('change', (e) => { hub.filter.kind = e.target.value; reset(); });
  $('myHubChips').addEventListener('click', (e) => {
    const chip = e.target.closest('[data-chip]');
    if (!chip) return;
    if (hub.view === 'materials') hub.filter.kind = chip.dataset.chip;
    else hub.filter.chip = chip.dataset.chip;
    reset();
  });
  $('myHubList').addEventListener('click', (e) => {
    if (e.target.closest('[data-more]')) { hub.limit += PAGE; render(); return; }
    const head = e.target.closest('[data-group]');
    if (head) {
      const open = head.getAttribute('aria-expanded') === 'true';
      if (open) hub.openGroups.delete(head.dataset.group); else hub.openGroups.add(head.dataset.group);
      head.setAttribute('aria-expanded', String(!open));
      head.querySelector('.material-symbols-outlined').textContent = open ? 'chevron_right' : 'expand_more';
      head.nextElementSibling.classList.toggle('hidden', open);
      return;
    }
    const fileBtn = e.target.closest('[data-open-material]');
    if (fileBtn) openFile(fileBtn.closest('[data-space-id]').dataset.spaceId, fileBtn.dataset.openMaterial, fileBtn.dataset.file, fileBtn);
  });
}

async function openFile(spaceId, materialId, fileId, button) {
  const tab = window.open('', '_blank');
  button.disabled = true;
  try {
    const blob = await API.classrooms.materialFile(spaceId, materialId, fileId);
    const url = URL.createObjectURL(blob);
    if (tab) tab.location.href = url;
    else window.location.href = url;
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  } catch (err) {
    if (tab) tab.close();
    hub.notify(friendlyErrorMessage(err));
  } finally {
    button.disabled = false;
  }
}
