/**
 * KizunaTutor - FE-07 Khối "Bài tập" trong trang "Lớp học của tôi" (học viên).
 * UC-63 View Assignments, UC-64 Submit Assignment, UC-96 sửa bài đã nộp (thêm/xóa/thay tệp, sửa nội dung).
 */

import { API } from '../../js/api.js';
import { formatDateTime, formatScore } from '../../js/exercise-helpers.js';
import { createFileList } from '../../js/file-picker.js';
import {
  formatFileSize,
  friendlyErrorMessage,
  escapeHtml,
  MATERIAL_ACCEPT_ATTR,
  MAX_FILES_PER_UPLOAD,
  FILE_RULES_TEXT
} from '../../js/classroom-helpers.js';
import {
  submissionStateLabel,
  submissionStateTone,
  formatDueDate,
  formatPoints,
  validateSubmission
} from '../../js/assignment-helpers.js';
import { fileChipHtml, RETRY_BUTTON_HTML } from './my-courses-ui.js';
import {
  groupByWeek,
  matchesKeyword,
  learnerStatus,
  countStatuses,
  sortByDeadline,
  deadlineOf,
  timeLeftLabel,
  LEARNER_STATUSES
} from '../../js/class-tabs-helpers.js';

const PAGE_SIZE = 10;

const $ = (id) => document.getElementById(id);

const ctx = {
  space: null,
  assignments: [],
  detail: null,
  notify: () => {},
  // Bộ lọc tab Bài tập: status null = mặc định ("Cần làm" nếu có, ngược lại "Tất cả")
  filter: { status: null, kind: '', keyword: '' },
  limit: PAGE_SIZE,
  openGroups: new Set(),
  bound: false
};

function stateBox(message, retry) {
  const box = $('myAssignmentsState');
  box.innerHTML = `<p>${escapeHtml(message)}</p>${retry ? RETRY_BUTTON_HTML : ''}`;
  box.classList.remove('hidden');
  if (retry) box.querySelector('[data-retry]').addEventListener('click', retry);
}

export function hideLearnerAssignments() {
  $('myCourseAssignments')?.classList.add('hidden');
  closeDetail();
}

/** Mở bài tập nộp tệp thành màn riêng trong lớp: ẩn nội dung lớp, thêm tên bài vào đường dẫn. */
function showDetailView(title) {
  $('myCourseMain')?.classList.add('hidden');
  $('myAssignmentDetail').classList.remove('hidden');
  const crumbClass = $('myCourseCrumbClass');
  if (crumbClass) {
    crumbClass.disabled = false;
    crumbClass.className = 'text-primary hover:underline font-medium [overflow-wrap:anywhere] text-left';
  }
  $('myCourseCrumbSep')?.classList.remove('hidden');
  const crumb = $('myCourseCrumbAssignment');
  if (crumb) {
    crumb.textContent = title || 'Bài tập';
    crumb.classList.remove('hidden');
  }
  document.querySelector('main')?.scrollTo({ top: 0, behavior: 'smooth' });
}

/** Đóng màn bài tập, trở về nội dung lớp (cuộn tới khối Bài tập khi người dùng bấm quay lại). */
function closeDetail({ scroll = false } = {}) {
  const panel = $('myAssignmentDetail');
  if (!panel) return;
  panel.classList.add('hidden');
  panel.innerHTML = '';
  $('myCourseMain')?.classList.remove('hidden');
  const crumbClass = $('myCourseCrumbClass');
  if (crumbClass) {
    crumbClass.disabled = true;
    crumbClass.className = 'text-on-surface-variant [overflow-wrap:anywhere] text-left';
  }
  $('myCourseCrumbSep')?.classList.add('hidden');
  $('myCourseCrumbAssignment')?.classList.add('hidden');
  if (scroll) $('myCourseAssignments')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function bindCrumb() {
  const crumbClass = $('myCourseCrumbClass');
  if (crumbClass && !crumbClass.dataset.bound) {
    crumbClass.dataset.bound = 'true';
    crumbClass.addEventListener('click', () => closeDetail({ scroll: true }));
  }
}

export async function loadLearnerAssignments(space, { notify } = {}) {
  if (ctx.space?.spaceId !== space?.spaceId) {
    ctx.filter = { status: null, kind: '', keyword: '' };
    ctx.limit = PAGE_SIZE;
    ctx.openGroups = new Set();
    if ($('myAssignmentSearch')) $('myAssignmentSearch').value = '';
    if ($('myAssignmentKind')) $('myAssignmentKind').value = '';
  }
  ctx.space = space;
  if (notify) ctx.notify = notify;
  bindFilters();
  const card = $('myCourseAssignments');
  const list = $('myAssignmentsList');
  if (!card || !space) return;
  bindCrumb();
  card.classList.remove('hidden');
  $('myAssignmentsTodo')?.classList.add('hidden');
  $('myAssignmentsState').classList.add('hidden');
  list.innerHTML = '<p class="text-on-surface-variant">Đang tải bài tập...</p>';
  const overview = $('myOverviewAssignments');
  if (overview) overview.innerHTML = '<li class="text-on-surface-variant">Đang tải bài tập...</li>';
  try {
    const [assignments, exercises] = await Promise.all([
      API.assignments.list(space.spaceId),
      API.exercises.list(space.spaceId).catch(() => [])
    ]);
    ctx.assignments = assignments || [];
    ctx.exercises = exercises || [];
    renderList();
  } catch (err) {
    list.innerHTML = '';
    stateBox(friendlyErrorMessage(err), () => loadLearnerAssignments(ctx.space));
  }
}

/** Bài làm trực tiếp (FE-07): mở trang làm bài riêng. */
function exerciseItemHtml(e) {
  const url = `/pages/exercise-take/index.html?space=${encodeURIComponent(ctx.space.spaceId)}&exercise=${encodeURIComponent(e.exerciseId)}`;
  const status = e.inProgressAttemptId ? 'Đang làm dở'
    : e.pendingGrading ? 'Chờ gia sư chấm'
      : e.bestScore !== null && e.bestScore !== undefined ? `Điểm: ${formatScore(e.bestScore)} / ${formatScore(e.maxScore)}`
        : e.state === 'CLOSED' ? 'Đã đóng đề' : 'Chưa làm';
  const tone = e.inProgressAttemptId || (e.canStart && !e.attemptsUsed) ? 'paused'
    : e.bestScore !== null && e.bestScore !== undefined && !e.pendingGrading ? 'active' : 'done';
  const button = e.inProgressAttemptId ? 'Làm tiếp' : (e.canStart ? (e.attemptsUsed ? 'Làm lượt mới' : 'Làm bài') : 'Xem kết quả');
  const primary = e.canStart || e.inProgressAttemptId;
  const meta = [
    `${e.questionCount} câu · ${formatScore(e.maxScore)} điểm`,
    e.timeLimitMinutes ? `${e.timeLimitMinutes} phút` : 'Không giới hạn thời gian',
    `Đã làm ${e.attemptsUsed}/${e.maxAttempts} lượt`,
    e.closesAt ? `Đóng đề ${formatDateTime(e.closesAt)}` : ''
  ].filter(Boolean).join(' · ');
  const left = primary ? timeLeftLabel(e.closesAt) : '';
  return `
    <li class="flex flex-wrap items-center gap-3.5 p-4 rounded-xl ${primary ? 'border border-surface-container-high' : 'bg-surface-container-low'}">
      <div class="mc-icon ${primary ? 'bg-primary-fixed text-on-primary-fixed-variant' : 'bg-surface-container-lowest text-on-surface-variant'}" aria-hidden="true">
        <span class="material-symbols-outlined">${primary ? 'quiz' : 'task_alt'}</span></div>
      <div class="flex-1 min-w-[220px] space-y-1">
        <div class="flex flex-wrap items-center gap-2">
          <h3 class="font-title-sm text-title-sm text-on-surface break-words">${escapeHtml(e.title)}</h3>
          <span class="kz-badge ${primary ? '' : 'bg-surface-container-lowest'}">Làm trực tiếp</span>
          <span class="kz-badge" data-tone="${tone}">${escapeHtml(status)}</span>
        </div>
        <p class="font-body-sm text-body-sm text-on-surface-variant">${escapeHtml(meta)}${left ? ` · <b class="text-tertiary">${escapeHtml(left)}</b>` : ''}</p>
      </div>
      <a class="kz-btn ${primary ? 'kz-btn-primary' : 'bg-surface-container-lowest shadow-sm'} h-11 px-4 no-underline shrink-0" href="${url}">${primary ? '<span class="material-symbols-outlined text-[18px]" aria-hidden="true">play_arrow</span>' : ''}${button}</a>
    </li>`;
}

const itemHtml = (it) => (it.kind === 'online' ? exerciseItemHtml(it.e) : fileItemHtml(it.a));

function bindOpenButtons(root) {
  root.querySelectorAll('[data-open-assignment]').forEach((btn) => {
    btn.addEventListener('click', () => openDetail(btn.dataset.openAssignment, btn.dataset.title));
  });
}

/** Thẻ "Bài tập cần làm" ở tab Tổng quan: bài cần làm hạn gần nhất trước, rồi bài mới có điểm. */
function renderOverview(items) {
  const box = $('myOverviewAssignments');
  if (!box) return;
  $('myOverviewAssignmentsMore').textContent = items.length ? `Xem tất cả ${items.length} bài tập` : 'Mở tab Bài tập';
  const todo = sortByDeadline(items.filter((it) => learnerStatus(it) === 'todo')).slice(0, 3);
  const graded = items.filter((it) => learnerStatus(it) === 'graded').slice(0, Math.max(0, 3 - todo.length) || 1);
  const rows = [...todo, ...(todo.length < 3 ? graded : [])];
  if (!rows.length) {
    box.innerHTML = `<li class="text-on-surface-variant">${items.length ? 'Bạn đã làm hết bài tập. ' : ''}Chưa có bài tập nào cần làm.</li>`;
    return;
  }
  box.innerHTML = rows.map((it) => {
    const isTodo = learnerStatus(it) === 'todo';
    const isOnline = it.kind === 'online';
    const left = timeLeftLabel(deadlineOf(it));
    const sub = isOnline
      ? (isTodo ? `Làm trực tiếp${it.e.closesAt ? ` · đóng ${escapeHtml(formatDateTime(it.e.closesAt))}` : ''}` : `Điểm ${formatScore(it.e.bestScore)} / ${formatScore(it.e.maxScore)}`)
      : (isTodo ? `Nộp tệp · hạn ${escapeHtml(formatDueDate(it.a.dueDate))}` : `Điểm ${escapeHtml(formatPoints(it.a.grade))} / ${escapeHtml(formatPoints(it.a.totalPoints))}`);
    const action = isOnline
      ? `<a class="kz-btn ${isTodo ? 'kz-btn-primary' : ''} shrink-0 no-underline" href="/pages/exercise-take/index.html?space=${encodeURIComponent(ctx.space.spaceId)}&exercise=${encodeURIComponent(it.e.exerciseId)}">${isTodo ? (it.e.inProgressAttemptId ? 'Làm tiếp' : 'Làm bài') : 'Xem kết quả'}</a>`
      : `<button type="button" class="kz-btn ${isTodo ? 'kz-btn-primary' : ''} shrink-0" data-open-assignment="${escapeHtml(it.a.assignmentId)}" data-title="${escapeHtml(it.a.title)}">${isTodo ? 'Nộp bài' : 'Xem'}</button>`;
    return `
      <li class="flex items-center gap-3 px-3.5 py-3 rounded-xl ${isTodo ? 'border border-surface-container-high' : 'bg-surface-container-low'}">
        <span class="material-symbols-outlined text-[22px] ${isTodo ? (isOnline ? 'text-primary' : 'text-secondary') : 'text-primary'} shrink-0" aria-hidden="true">${isTodo ? (isOnline ? 'quiz' : 'upload_file') : 'task_alt'}</span>
        <div class="flex-1 min-w-0">
          ${isTodo ? '' : '<p class="font-body-sm text-[12px] text-on-surface-variant">Mới có điểm</p>'}
          <p class="font-label-md text-label-md text-on-surface truncate">${escapeHtml(it.title)}</p>
          <p class="font-body-sm text-[12px] text-on-surface-variant truncate">${sub}${isTodo && left ? ` · <b class="text-tertiary">${escapeHtml(left)}</b>` : ''}</p>
        </div>
        ${action}
      </li>`;
  }).join('');
  bindOpenButtons(box);
}

function renderList() {
  const list = $('myAssignmentsList');
  const exercises = ctx.exercises || [];
  const items = [
    ...ctx.assignments.map((a) => ({ kind: 'file', at: a.createdAt, a, title: a.title })),
    ...exercises.map((e) => ({ kind: 'online', at: e.createdAt, e, title: e.title }))
  ].sort((x, y) => String(y.at || '').localeCompare(String(x.at || '')));
  const counts = countStatuses(items, learnerStatus);
  const todoBadge = $('myAssignmentsTodo');
  if (todoBadge) {
    todoBadge.textContent = `${counts.todo || 0} cần làm`;
    todoBadge.classList.toggle('hidden', !counts.todo);
  }
  renderOverview(items);
  if (ctx.filter.status === null || (ctx.filter.status !== 'all' && !counts[ctx.filter.status])) {
    ctx.filter.status = counts.todo ? 'todo' : 'all';
  }
  $('myAssignmentChips').innerHTML = items.length ? LEARNER_STATUSES.filter((st) => st.key === 'all' || counts[st.key]).map((st) => `
    <button type="button" class="kz-chip" data-status="${st.key}" aria-pressed="${ctx.filter.status === st.key}">${st.label} · ${counts[st.key] || 0}</button>`).join('') : '';
  if (!items.length) {
    list.innerHTML = '';
    stateBox('Hiện chưa có bài tập nào trong lớp học này.');
    return;
  }
  const shown = items.filter((it) => (ctx.filter.status === 'all' || learnerStatus(it) === ctx.filter.status)
    && (!ctx.filter.kind || it.kind === ctx.filter.kind)
    && matchesKeyword(it.title, ctx.filter.keyword));
  if (!shown.length) {
    list.innerHTML = '';
    stateBox('Không có bài tập nào khớp bộ lọc.');
    return;
  }
  $('myAssignmentsState').classList.add('hidden');
  if (ctx.filter.status === 'todo') {
    // Cần làm: hạn gần nhất trước, không nhóm
    list.innerHTML = `<p class="font-label-sm text-label-sm uppercase tracking-wide text-on-surface-variant">Hạn gần nhất trước</p>
      <ul class="flex flex-col gap-3">${sortByDeadline(shown).map(itemHtml).join('')}</ul>`;
  } else {
    const visible = shown.slice(0, ctx.limit);
    list.innerHTML = groupByWeek(visible, (it) => it.at).map((g, gi) => {
      const open = gi === 0 || ctx.openGroups.has(g.key) || ctx.filter.keyword.trim() !== '';
      return `
        <section>
          <button type="button" class="kz-group-head" data-agroup="${escapeHtml(g.key)}" aria-expanded="${open}">
            <span class="material-symbols-outlined text-[18px]" aria-hidden="true">${open ? 'expand_more' : 'chevron_right'}</span>${escapeHtml(g.label)}<span class="normal-case font-normal">· ${g.items.length} bài</span></button>
          <ul class="flex flex-col gap-3 mt-2 ${open ? '' : 'hidden'}">${g.items.map(itemHtml).join('')}</ul>
        </section>`;
    }).join('') + (shown.length > visible.length ? `
      <div class="flex justify-center"><button type="button" class="kz-btn" data-more-assignments>Xem thêm ${Math.min(PAGE_SIZE, shown.length - visible.length)} bài (còn ${shown.length - visible.length})</button></div>` : '');
  }
  bindOpenButtons(list);
}

/** Gắn sự kiện bộ lọc tab Bài tập (một lần). */
function bindFilters() {
  if (ctx.bound) return;
  ctx.bound = true;
  $('myAssignmentSearch')?.addEventListener('input', (e) => {
    ctx.filter.keyword = e.target.value;
    ctx.limit = PAGE_SIZE;
    renderList();
  });
  $('myAssignmentKind')?.addEventListener('change', (e) => {
    ctx.filter.kind = e.target.value;
    ctx.limit = PAGE_SIZE;
    renderList();
  });
  $('myAssignmentChips')?.addEventListener('click', (e) => {
    const chip = e.target.closest('[data-status]');
    if (!chip) return;
    ctx.filter.status = chip.dataset.status;
    ctx.limit = PAGE_SIZE;
    renderList();
  });
  $('myAssignmentsList')?.addEventListener('click', (e) => {
    if (e.target.closest('[data-more-assignments]')) {
      ctx.limit += PAGE_SIZE;
      renderList();
      return;
    }
    const head = e.target.closest('[data-agroup]');
    if (!head) return;
    const open = head.getAttribute('aria-expanded') === 'true';
    if (open) ctx.openGroups.delete(head.dataset.agroup); else ctx.openGroups.add(head.dataset.agroup);
    head.setAttribute('aria-expanded', String(!open));
    head.querySelector('.material-symbols-outlined').textContent = open ? 'chevron_right' : 'expand_more';
    head.nextElementSibling.classList.toggle('hidden', open);
  });
}

function fileItemHtml(a) {
  const todo = a.submissionState === 'NOT_SUBMITTED';
  return `
    <li class="flex flex-wrap items-center gap-3.5 p-4 rounded-xl ${todo ? 'border border-surface-container-high' : 'bg-surface-container-low'}">
      <div class="mc-icon ${todo ? 'bg-secondary-fixed text-on-secondary-container' : 'bg-surface-container-lowest text-on-surface-variant'}" aria-hidden="true">
        <span class="material-symbols-outlined">${todo ? 'upload_file' : 'task_alt'}</span></div>
      <div class="flex-1 min-w-[220px] space-y-1">
        <div class="flex flex-wrap items-center gap-2">
          <h3 class="font-title-sm text-title-sm text-on-surface break-words">${escapeHtml(a.title)}</h3>
          <span class="kz-badge ${todo ? '' : 'bg-surface-container-lowest'}">Nộp tệp</span>
          <span class="kz-badge" data-tone="${todo ? 'paused' : submissionStateTone(a.submissionState)}">${escapeHtml(submissionStateLabel(a.submissionState))}</span>
        </div>
        <p class="font-body-sm text-body-sm text-on-surface-variant">Hạn nộp: ${escapeHtml(formatDueDate(a.dueDate))}${todo && timeLeftLabel(a.dueDate) ? ` · <b class="text-tertiary">${escapeHtml(timeLeftLabel(a.dueDate))}</b>` : ''} · Điểm tối đa: ${escapeHtml(formatPoints(a.totalPoints))}
          ${a.grade !== null && a.grade !== undefined ? ` · Điểm của bạn: ${escapeHtml(formatPoints(a.grade))}` : ''}</p>
      </div>
      <button type="button" class="kz-btn ${todo ? 'kz-btn-primary' : 'bg-surface-container-lowest shadow-sm'} h-11 px-4 shrink-0" data-open-assignment="${escapeHtml(a.assignmentId)}" data-title="${escapeHtml(a.title)}">${todo ? 'Xem &amp; nộp bài' : 'Xem bài tập'}</button>
    </li>`;
}

async function openDetail(assignmentId, title) {
  const panel = $('myAssignmentDetail');
  panel.innerHTML = '<div class="kz-card p-6 text-on-surface-variant">Đang tải bài tập...</div>';
  showDetailView(title);
  try {
    ctx.detail = await API.assignments.detail(ctx.space.spaceId, assignmentId);
    renderDetail();
  } catch (err) {
    panel.innerHTML = `<div class="kz-card p-6 space-y-4 text-center">
      <p class="text-error">${escapeHtml(friendlyErrorMessage(err))}</p>
      <button type="button" class="kz-btn" data-close-detail><span class="material-symbols-outlined text-[18px]" aria-hidden="true">arrow_back</span>Về lớp học</button></div>`;
    panel.querySelector('[data-close-detail]').addEventListener('click', () => closeDetail({ scroll: true }));
  }
}

function lockedReason(a, submission) {
  if (a.submissionState === 'GRADED') return 'Bài tập đã được chấm điểm nên không thể chỉnh sửa bài nộp.';
  if (submission && a.dueOver) return 'Đã quá hạn nộp bài nên không thể chỉnh sửa bài nộp.';
  if (!a.canSubmit && !a.canEditSubmission) return 'Lớp học đang ở chế độ chỉ xem nên không thể nộp hoặc sửa bài.';
  return '';
}

/** Danh sách nút mở tệp. dataAttr: tên thuộc tính data-* chứa fileId. */
function fileButtonsHtml(files, dataAttr) {
  if (!files || !files.length) return '';
  return `<div class="flex flex-wrap gap-2">${files.map((f) => fileChipHtml(f,
    `${dataAttr}="${escapeHtml(f.fileId)}"`, formatFileSize(f.fileSize))).join('')}</div>`;
}

/** Form nộp bài (lần đầu) hoặc sửa bài nộp (gỡ tệp cũ / thêm tệp mới / sửa nội dung). */
function submissionFormHtml(a, submission) {
  const editing = !!submission;
  return `
    <form id="mySubmissionForm" class="space-y-4" novalidate>
      <h2 class="mc-section-title">${editing ? 'Chỉnh sửa bài nộp' : 'Nộp bài làm'}</h2>
      ${!editing && a.dueOver ? noticeHtml('schedule', 'Đã quá hạn nộp. Bạn chỉ được nộp một lần (nộp muộn) và không thể chỉnh sửa sau đó.', 'warn') : ''}
      ${editing && a.dueDate ? `<p class="font-body-sm text-body-sm text-on-surface-variant">Bạn có thể chỉnh sửa đến ${escapeHtml(formatDueDate(a.dueDate))}, khi gia sư chưa chấm điểm.</p>` : ''}
      <div>
        <label for="mySubmissionContent" class="kz-label">Nội dung trả lời <span class="font-normal text-on-surface-variant">(không bắt buộc nếu có tệp)</span></label>
        <textarea id="mySubmissionContent" rows="5" maxlength="5000" class="kz-input resize-y">${escapeHtml(submission?.content || '')}</textarea>
      </div>
      <div>
        <p class="kz-label">Tệp bài làm <span class="font-normal text-on-surface-variant">(không bắt buộc nếu có nội dung)</span></p>
        <div id="mySubmissionFileList"></div>
        <p class="font-body-sm text-[12px] text-on-surface-variant mt-1.5">${escapeHtml(FILE_RULES_TEXT)}${editing ? ' Bấm "Thay" để đổi đúng tệp đó, "Xóa" để bỏ tệp.' : ''}</p>
      </div>
      <p id="mySubmissionError" class="field-error hidden"></p>
      <div class="flex flex-wrap justify-end gap-2">
        ${editing ? '<button type="button" class="kz-btn h-11 px-4" data-cancel-edit>Hủy</button>' : ''}
        <button type="submit" id="mySubmissionSubmit" class="kz-btn kz-btn-primary h-11 px-5 text-[14px]">${editing ? 'Lưu bài nộp' : 'Nộp bài'}</button>
      </div>
    </form>`;
}

/** Dải thông báo: warn = cam nhạt (khóa, quá hạn), ok = xanh nhạt (thành công). */
function noticeHtml(icon, text, kind) {
  const cls = kind === 'ok' ? 'bg-primary-fixed text-on-primary-fixed-variant' : 'bg-tertiary-fixed text-on-tertiary-fixed-variant';
  return `<p class="flex items-start gap-2.5 rounded-xl px-4 py-3 ${cls}" ${kind === 'ok' ? 'role="status"' : ''}>
    <span class="material-symbols-outlined text-[20px] shrink-0" aria-hidden="true">${icon}</span><span class="min-w-0 [overflow-wrap:anywhere]">${escapeHtml(text)}</span></p>`;
}

function infoTileHtml(icon, label, value) {
  return `<div class="p-3 rounded-xl bg-surface-container-low">
    <p class="flex items-center gap-1.5 text-on-surface-variant"><span class="material-symbols-outlined text-[18px]" aria-hidden="true">${icon}</span>${label}</p>
    <p class="font-title-sm text-title-sm text-on-surface mt-0.5">${escapeHtml(value)}</p></div>`;
}

function renderDetail({ editMode = false, message = '' } = {}) {
  const a = ctx.detail.assignment;
  const subs = ctx.detail.submissions || [];
  const submission = subs[subs.length - 1] || null;
  const panel = $('myAssignmentDetail');
  const reason = lockedReason(a, submission);
  const stateTone = a.submissionState === 'NOT_SUBMITTED' ? 'paused' : submissionStateTone(a.submissionState);

  let actionHtml = '';
  if (!submission && a.canSubmit) actionHtml = submissionFormHtml(a, null);
  else if (submission && a.canEditSubmission && editMode) actionHtml = submissionFormHtml(a, submission);
  else if (reason && !(submission && a.canEditSubmission)) actionHtml = noticeHtml('lock', reason, 'warn');

  const gradedHtml = submission?.graded ? `
    <div class="space-y-3">
      <div class="flex items-center gap-3">
        <div class="w-14 h-14 rounded-full bg-primary-fixed text-on-primary-fixed-variant flex items-center justify-center font-title-md text-title-md shrink-0">${escapeHtml(formatPoints(submission.grade))}</div>
        <div><p class="font-title-sm text-title-sm text-on-surface">Đã chấm điểm</p>
          <p class="font-body-sm text-body-sm text-on-surface-variant">${escapeHtml(formatPoints(submission.grade))} / ${escapeHtml(formatPoints(a.totalPoints))} điểm</p></div>
      </div>
      ${submission.tutorFeedback ? `<p class="rounded-xl bg-surface-container-low px-4 py-3 whitespace-pre-line [overflow-wrap:anywhere]"><strong class="font-semibold">Nhận xét của gia sư:</strong> ${escapeHtml(submission.tutorFeedback)}</p>` : ''}
    </div>` : '';

  const submissionHtml = submission && !editMode ? `
    <div class="space-y-3">
      <div class="flex flex-wrap items-center gap-2">
        <h2 class="mc-section-title">Bài nộp của bạn</h2>
        <span class="kz-badge" data-tone="${submission.onTime ? 'active' : 'paused'}">${submission.onTime ? 'Đúng hạn' : 'Nộp muộn'}</span>
      </div>
      <p class="font-body-sm text-body-sm text-on-surface-variant">Cập nhật lần cuối: ${escapeHtml(formatDueDate(submission.submittedAt))}</p>
      ${submission.content ? `<p class="whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(submission.content)}</p>` : ''}
      ${fileButtonsHtml(submission.files, 'data-open-submission-file')}
      ${a.canEditSubmission ? '<div class="flex justify-end"><button type="button" class="kz-btn h-11 px-4" data-edit-submission><span class="material-symbols-outlined text-[18px]" aria-hidden="true">edit</span>Chỉnh sửa bài nộp</button></div>' : ''}
    </div>` : (!submission && !actionHtml ? '<p class="text-on-surface-variant">Bạn chưa nộp bài cho bài tập này.</p>' : '');

  panel.innerHTML = `
    <section class="kz-card p-5 sm:p-6 flex flex-wrap items-center gap-4">
      <div class="mc-icon w-12 h-12 bg-secondary-fixed text-on-secondary-container" aria-hidden="true"><span class="material-symbols-outlined text-[26px]">upload_file</span></div>
      <div class="flex-1 min-w-[220px]">
        <h1 class="font-headline-md text-headline-md text-on-surface [overflow-wrap:anywhere]">${escapeHtml(a.title)}</h1>
        <p class="text-on-surface-variant">Bài nộp tệp${ctx.space?.tutorName ? ` · Gia sư ${escapeHtml(ctx.space.tutorName)}` : ''}</p>
      </div>
      <span class="kz-badge" data-tone="${stateTone}">${escapeHtml(submissionStateLabel(a.submissionState))}</span>
      <button type="button" class="kz-btn h-10" data-close-detail><span class="material-symbols-outlined text-[18px]" aria-hidden="true">arrow_back</span>Về lớp học</button>
    </section>
    ${message ? noticeHtml('check_circle', message, 'ok') : ''}
    <div class="grid gap-5 lg:grid-cols-2 items-start">
      <section class="kz-card p-5 sm:p-6 space-y-4 min-w-0">
        <h2 class="mc-section-title">Đề bài</h2>
        <div class="grid grid-cols-2 gap-3">
          ${infoTileHtml('event', 'Hạn nộp', formatDueDate(a.dueDate))}
          ${infoTileHtml('grade', 'Điểm tối đa', `${formatPoints(a.totalPoints)} điểm`)}
        </div>
        ${a.description ? `<p class="whitespace-pre-line leading-relaxed [overflow-wrap:anywhere]">${escapeHtml(a.description)}</p>` : ''}
        ${a.attachments?.length ? `<div class="space-y-2"><p class="font-label-md text-label-md text-on-surface">Tệp đề bài (${a.attachments.length})</p>${fileButtonsHtml(a.attachments, 'data-open-attachment')}</div>` : ''}
      </section>
      <section class="kz-card p-5 sm:p-6 space-y-5 min-w-0">
        ${gradedHtml}
        ${submissionHtml}
        ${actionHtml}
      </section>
    </div>
  `;

  panel.querySelector('[data-close-detail]').addEventListener('click', () => closeDetail({ scroll: true }));
  panel.querySelectorAll('[data-open-attachment]').forEach((btn) => {
    btn.addEventListener('click', () => openBlob(
      () => API.assignments.attachmentFile(ctx.space.spaceId, a.assignmentId, btn.dataset.openAttachment), btn));
  });
  panel.querySelectorAll('[data-open-submission-file]').forEach((btn) => {
    btn.addEventListener('click', () => openBlob(() => API.assignments.submissionFile(
      ctx.space.spaceId, a.assignmentId, submission.submissionId, btn.dataset.openSubmissionFile), btn));
  });
  panel.querySelector('[data-edit-submission]')?.addEventListener('click', () => renderDetail({ editMode: true }));
  panel.querySelector('[data-cancel-edit]')?.addEventListener('click', () => renderDetail());
  ctx.picker = null;
  if ($('mySubmissionFileList')) {
    ctx.picker = createFileList($('mySubmissionFileList'), { accept: MATERIAL_ACCEPT_ATTR, addLabel: 'Thêm tệp bài làm' });
    ctx.picker.reset(editMode && submission ? submission.files : [], { max: MAX_FILES_PER_UPLOAD });
  }
  $('mySubmissionForm')?.addEventListener('submit', (e) => submitWork(e, submission));
}

/** Mở tệp (Blob có kiểm quyền bằng token) trong tab mới. */
async function openBlob(load, button) {
  const tab = window.open('', '_blank');
  button.disabled = true;
  try {
    const blob = await load();
    const url = URL.createObjectURL(blob);
    if (tab) tab.location.href = url;
    else window.location.href = url;
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  } catch (err) {
    if (tab) tab.close();
    ctx.notify(friendlyErrorMessage(err));
  } finally {
    button.disabled = false;
  }
}

async function submitWork(event, submission) {
  event.preventDefault();
  const content = $('mySubmissionContent').value;
  const changes = ctx.picker ? ctx.picker.getChanges() : { newFiles: [], removeIds: [], keptCount: 0 };
  const files = changes.newFiles;
  const removeFileIds = changes.removeIds;
  const keptFiles = changes.keptCount;
  const errorEl = $('mySubmissionError');
  const error = validateSubmission({ content, files, keptFiles });
  errorEl.textContent = error || '';
  errorEl.classList.toggle('hidden', !error);
  if (error) return;

  const button = $('mySubmissionSubmit');
  button.disabled = true;
  const label = button.textContent;
  button.textContent = files.length ? `Đang tải ${files.length} tệp...` : 'Đang lưu...';
  try {
    const assignmentId = ctx.detail.assignment.assignmentId;
    if (submission) {
      await API.assignments.updateSubmission(ctx.space.spaceId, assignmentId, submission.submissionId,
        { content: content.trim(), files, removeFileIds });
    } else {
      await API.assignments.submit(ctx.space.spaceId, assignmentId, { content: content.trim(), files });
    }
    ctx.detail = await API.assignments.detail(ctx.space.spaceId, assignmentId);
    renderDetail({ message: submission ? 'Cập nhật bài nộp thành công.' : 'Nộp bài tập thành công.' });
    loadLearnerAssignments(ctx.space);
  } catch (err) {
    errorEl.textContent = (err.fieldErrors && (err.fieldErrors.content || err.fieldErrors.file)) || friendlyErrorMessage(err);
    errorEl.classList.remove('hidden');
    button.disabled = false;
    button.textContent = label;
  }
}
