/**
 * KizunaTutor - FE-07 Khối "Bài tập" trong trang chi tiết lớp của gia sư.
 * UC-63 View, UC-66 Create & Assign, UC-95 Update, UC-67 Grade. Dữ liệu từ /api/classrooms/{id}/assignments.
 */

import { API } from '../../js/api.js';
import { fileTypeLabel, friendlyErrorMessage, escapeHtml } from '../../js/classroom-helpers.js';
import {
  submissionStateLabel,
  submissionStateTone,
  formatDueDate,
  formatPoints,
  toDateTimeLocalValue,
  fromDateTimeLocalValue,
  validateAssignmentForm,
  validateGrade,
  parsePoints
} from '../../js/assignment-helpers.js';

const $ = (id) => document.getElementById(id);

const ctx = {
  space: null,
  assignments: [],
  editing: null,
  detail: null,
  toast: () => {}
};

function setFieldError(id, message) {
  const el = $(id);
  if (!el) return;
  el.textContent = message || '';
  el.classList.toggle('hidden', !message);
}

function open(id) { $(id)?.classList.remove('hidden'); }
function close(id) { $(id)?.classList.add('hidden'); }

function busy(button, isBusy, text) {
  if (!button) return;
  if (isBusy) {
    button.dataset.label = button.textContent;
    button.textContent = text;
    button.disabled = true;
  } else {
    button.textContent = button.dataset.label || button.textContent;
    button.disabled = false;
  }
}

function stateBox(message, retry) {
  const box = $('assignmentsState');
  box.innerHTML = `<p>${escapeHtml(message)}</p>${retry ? '<button type="button" class="retro-btn mt-4" data-retry>Thử lại</button>' : ''}`;
  box.classList.remove('hidden');
  if (retry) box.querySelector('[data-retry]').addEventListener('click', retry);
}

// ----------------------------------------------------------------------------
// Danh sách (UC-63)
// ----------------------------------------------------------------------------
export async function loadTutorAssignments(space) {
  ctx.space = space;
  const section = $('assignmentsSection');
  const list = $('assignmentsList');
  if (!section || !space) return;
  section.classList.remove('hidden');
  $('openAssignmentFormBtn').classList.toggle('hidden', !space.canManage);
  $('assignmentsState').classList.add('hidden');
  list.innerHTML = '<li class="text-[#444]">Đang tải bài tập...</li>';
  try {
    ctx.assignments = await API.assignments.list(space.spaceId) || [];
    render();
  } catch (err) {
    list.innerHTML = '';
    stateBox(friendlyErrorMessage(err), () => loadTutorAssignments(ctx.space));
  }
}

export function hideTutorAssignments() {
  $('assignmentsSection')?.classList.add('hidden');
}

function render() {
  const list = $('assignmentsList');
  if (!ctx.assignments.length) {
    list.innerHTML = '';
    stateBox('Hiện chưa có bài tập nào trong lớp học này.');
    return;
  }
  $('assignmentsState').classList.add('hidden');
  list.innerHTML = ctx.assignments.map((a) => `
    <li class="border border-black rounded-[5px] p-4 bg-white flex flex-col sm:flex-row sm:items-start gap-3">
      <div class="flex-1 min-w-0 space-y-1">
        <div class="flex flex-wrap items-center gap-2">
          <h3 class="font-semibold text-[16px] break-words">${escapeHtml(a.title)}</h3>
          <span class="retro-badge" data-tone="${submissionStateTone(a.submissionState)}">${escapeHtml(submissionStateLabel(a.submissionState))}</span>
        </div>
        ${a.description ? `<p class="text-[#333] whitespace-pre-line break-words">${escapeHtml(a.description)}</p>` : ''}
        <p class="font-meta text-[#555]">Hạn nộp: ${escapeHtml(formatDueDate(a.dueDate))} · Điểm tối đa: ${escapeHtml(formatPoints(a.totalPoints))}
          · Số lần nộp: ${Number(a.attemptCount) || 0}${a.grade !== null && a.grade !== undefined ? ` · Điểm: ${escapeHtml(formatPoints(a.grade))}` : ''}</p>
      </div>
      <div class="flex gap-2 shrink-0">
        <button type="button" class="retro-btn" data-view-assignment="${escapeHtml(a.assignmentId)}">${a.canGrade ? 'Xem và chấm' : 'Xem bài nộp'}</button>
        ${a.canEdit ? `<button type="button" class="retro-btn" data-edit-assignment="${escapeHtml(a.assignmentId)}">Chỉnh sửa</button>` : ''}
      </div>
    </li>
  `).join('');
  list.querySelectorAll('[data-view-assignment]').forEach((btn) => {
    btn.addEventListener('click', () => openSubmissions(btn.dataset.viewAssignment));
  });
  list.querySelectorAll('[data-edit-assignment]').forEach((btn) => {
    btn.addEventListener('click', () => openForm(ctx.assignments.find((a) => a.assignmentId === btn.dataset.editAssignment)));
  });
}

// ----------------------------------------------------------------------------
// Giao / sửa bài tập (UC-66, UC-95)
// ----------------------------------------------------------------------------
function openForm(assignment = null) {
  ctx.editing = assignment;
  const isEdit = !!assignment;
  $('assignmentModalTitle').textContent = isEdit ? 'Chỉnh sửa bài tập' : 'Giao bài tập';
  $('assignmentSubmit').textContent = isEdit ? 'Lưu thay đổi' : 'Giao bài tập';
  $('assignmentTitleInput').value = assignment?.title || '';
  $('assignmentDescriptionInput').value = assignment?.description || '';
  $('assignmentDueInput').value = toDateTimeLocalValue(assignment?.dueDate);
  $('assignmentPointsInput').value = assignment ? formatPoints(assignment.totalPoints) : '';
  ['assignmentTitleError', 'assignmentDescriptionError', 'assignmentDueError', 'assignmentPointsError', 'assignmentFormError']
    .forEach((id) => setFieldError(id, ''));
  open('assignmentModal');
  $('assignmentTitleInput').focus();
}

async function submitForm(event) {
  event.preventDefault();
  const isEdit = !!ctx.editing;
  const form = {
    title: $('assignmentTitleInput').value,
    description: $('assignmentDescriptionInput').value,
    dueLocal: $('assignmentDueInput').value,
    totalPoints: $('assignmentPointsInput').value
  };
  const errors = validateAssignmentForm(form, { dueUnchangedIso: isEdit ? ctx.editing.dueDate : null });
  setFieldError('assignmentTitleError', errors.title);
  setFieldError('assignmentDescriptionError', errors.description);
  setFieldError('assignmentDueError', errors.dueDate);
  setFieldError('assignmentPointsError', errors.totalPoints);
  setFieldError('assignmentFormError', '');
  if (Object.keys(errors).length) return;

  const unchangedDue = isEdit && ctx.editing.dueDate && toDateTimeLocalValue(ctx.editing.dueDate) === form.dueLocal;
  const payload = {
    title: form.title.trim(),
    description: form.description.trim(),
    dueDate: unchangedDue ? ctx.editing.dueDate : fromDateTimeLocalValue(form.dueLocal),
    totalPoints: parsePoints(form.totalPoints)
  };
  const button = $('assignmentSubmit');
  busy(button, true, isEdit ? 'Đang lưu...' : 'Đang giao...');
  try {
    if (isEdit) await API.assignments.update(ctx.space.spaceId, ctx.editing.assignmentId, payload);
    else await API.assignments.create(ctx.space.spaceId, payload);
    close('assignmentModal');
    ctx.toast(isEdit ? 'Cập nhật bài tập thành công.' : 'Giao bài tập thành công.');
    await loadTutorAssignments(ctx.space);
  } catch (err) {
    const fe = err.fieldErrors || {};
    if (fe.title || fe.description || fe.dueDate || fe.totalPoints) {
      setFieldError('assignmentTitleError', fe.title);
      setFieldError('assignmentDescriptionError', fe.description);
      setFieldError('assignmentDueError', fe.dueDate);
      setFieldError('assignmentPointsError', fe.totalPoints);
    } else {
      setFieldError('assignmentFormError', friendlyErrorMessage(err));
    }
  } finally {
    busy(button, false);
  }
}

// ----------------------------------------------------------------------------
// Xem bài nộp & chấm điểm (UC-63, UC-67)
// ----------------------------------------------------------------------------
async function openSubmissions(assignmentId) {
  $('submissionsModalTitle').textContent = 'Đang tải...';
  $('submissionsMeta').textContent = '';
  $('submissionsDescription').classList.add('hidden');
  $('submissionsList').innerHTML = '';
  $('submissionsState').classList.add('hidden');
  $('gradeForm').classList.add('hidden');
  $('gradeLockedNote').classList.add('hidden');
  open('submissionsModal');
  try {
    ctx.detail = await API.assignments.detail(ctx.space.spaceId, assignmentId);
    renderSubmissions();
  } catch (err) {
    $('submissionsModalTitle').textContent = 'Bài tập';
    const box = $('submissionsState');
    box.textContent = friendlyErrorMessage(err);
    box.classList.remove('hidden');
  }
}

function renderSubmissions() {
  const a = ctx.detail.assignment;
  const subs = ctx.detail.submissions || [];
  $('submissionsModalTitle').textContent = a.title;
  $('submissionsMeta').textContent = `Hạn nộp: ${formatDueDate(a.dueDate)} · Điểm tối đa: ${formatPoints(a.totalPoints)} · Trạng thái: ${submissionStateLabel(a.submissionState)}`;
  const desc = $('submissionsDescription');
  desc.textContent = a.description || '';
  desc.classList.toggle('hidden', !a.description);

  const box = $('submissionsState');
  if (!subs.length) {
    box.textContent = 'Học viên chưa nộp bài cho bài tập này.';
    box.classList.remove('hidden');
  } else {
    box.classList.add('hidden');
  }
  $('submissionsList').innerHTML = subs.slice().reverse().map((s) => `
    <li class="border border-black rounded-[5px] p-3 bg-white space-y-1">
      <div class="flex flex-wrap items-center gap-2">
        <strong>Lần nộp ${Number(s.attemptNumber)}</strong>
        <span class="retro-badge" data-tone="${s.onTime ? 'active' : 'paused'}">${s.onTime ? 'Đúng hạn' : 'Nộp muộn'}</span>
        ${s.graded ? '<span class="retro-badge" data-tone="active">Đã chấm</span>' : ''}
        <span class="font-meta text-[#555]">${escapeHtml(formatDueDate(s.submittedAt))}</span>
      </div>
      ${s.content ? `<p class="text-[#333] whitespace-pre-line break-words">${escapeHtml(s.content)}</p>` : ''}
      ${s.fileName ? `<button type="button" class="retro-btn" data-open-submission="${escapeHtml(s.submissionId)}">Mở tệp: ${escapeHtml(s.fileName)} (${escapeHtml(fileTypeLabel(s.fileType))})</button>` : ''}
      ${s.graded ? `<p class="text-[#333]"><strong>Điểm: ${escapeHtml(formatPoints(s.grade))}/${escapeHtml(formatPoints(a.totalPoints))}</strong>${s.tutorFeedback ? ` · Nhận xét: ${escapeHtml(s.tutorFeedback)}` : ''}</p>` : ''}
    </li>
  `).join('');
  $('submissionsList').querySelectorAll('[data-open-submission]').forEach((btn) => {
    btn.addEventListener('click', () => openFile(btn.dataset.openSubmission, btn));
  });

  const note = $('gradeLockedNote');
  const graded = a.submissionState === 'GRADED';
  if (a.canGrade) {
    $('gradeMaxLabel').textContent = `(từ 0 đến ${formatPoints(a.totalPoints)})`;
    $('gradeInput').value = '';
    $('feedbackInput').value = '';
    ['gradeError', 'feedbackError', 'gradeFormError'].forEach((id) => setFieldError(id, ''));
    $('gradeForm').classList.remove('hidden');
    note.classList.add('hidden');
  } else {
    $('gradeForm').classList.add('hidden');
    let message = '';
    if (graded) message = 'Bài tập đã được chấm điểm.';
    else if (subs.length && a.dueDate && !a.dueOver) message = 'Chưa hết hạn nộp bài nên chưa thể chấm điểm.';
    note.textContent = message;
    note.classList.toggle('hidden', !message);
  }
}

async function openFile(submissionId, button) {
  const tab = window.open('', '_blank');
  button.disabled = true;
  try {
    const blob = await API.assignments.submissionFile(ctx.space.spaceId, ctx.detail.assignment.assignmentId, submissionId);
    const url = URL.createObjectURL(blob);
    if (tab) tab.location.href = url;
    else window.location.href = url;
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  } catch (err) {
    if (tab) tab.close();
    ctx.toast(friendlyErrorMessage(err), 'error');
  } finally {
    button.disabled = false;
  }
}

async function submitGrade(event) {
  event.preventDefault();
  const a = ctx.detail.assignment;
  const subs = ctx.detail.submissions || [];
  const latest = subs[subs.length - 1];
  const gradeValue = $('gradeInput').value;
  const feedback = $('feedbackInput').value;
  const gradeError = validateGrade(gradeValue, a.totalPoints);
  setFieldError('gradeError', gradeError);
  setFieldError('feedbackError', feedback.trim().length > 5000 ? 'Nhận xét tối đa 5000 ký tự.' : '');
  setFieldError('gradeFormError', '');
  if (gradeError || feedback.trim().length > 5000 || !latest) return;

  const button = $('gradeSubmit');
  busy(button, true, 'Đang gửi...');
  try {
    await API.assignments.grade(ctx.space.spaceId, a.assignmentId, latest.submissionId, {
      grade: parsePoints(gradeValue),
      feedback: feedback.trim()
    });
    ctx.toast('Đã gửi điểm và nhận xét.');
    ctx.detail = await API.assignments.detail(ctx.space.spaceId, a.assignmentId);
    renderSubmissions();
    await loadTutorAssignments(ctx.space);
  } catch (err) {
    const fe = err.fieldErrors || {};
    if (fe.grade || fe.feedback) {
      setFieldError('gradeError', fe.grade);
      setFieldError('feedbackError', fe.feedback);
    } else {
      setFieldError('gradeFormError', friendlyErrorMessage(err));
    }
  } finally {
    busy(button, false);
  }
}

// ----------------------------------------------------------------------------
export function initTutorAssignments({ showToast }) {
  ctx.toast = showToast || (() => {});
  $('openAssignmentFormBtn')?.addEventListener('click', () => openForm(null));
  $('assignmentForm')?.addEventListener('submit', submitForm);
  $('gradeForm')?.addEventListener('submit', submitGrade);
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      close('assignmentModal');
      close('submissionsModal');
    }
  });
}
