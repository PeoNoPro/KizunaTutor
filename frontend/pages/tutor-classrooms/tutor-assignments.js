/**
 * KizunaTutor - FE-07 Khối "Bài tập" trong trang chi tiết lớp của gia sư.
 * UC-63 View, UC-66 Create & Assign, UC-95 Update, UC-67 Grade. Dữ liệu từ /api/classrooms/{id}/assignments.
 * Giao / sửa bài tập gửi MỘT lần (thông tin + tệp đề bài thêm/gỡ, tối đa 5 tệp); lỗi bất kỳ thì không lưu gì.
 */

import { API } from '../../js/api.js';
import { createFileList } from '../../js/file-picker.js';
import {
  fileTypeLabel,
  formatFileSize,
  friendlyErrorMessage,
  escapeHtml,
  validateMaterialFiles,
  MATERIAL_ACCEPT_ATTR,
  MAX_FILES_PER_UPLOAD,
  FILE_RULES_TEXT
} from '../../js/classroom-helpers.js';
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
import {
  exerciseStateLabel,
  exerciseStateTone,
  formatDateTime,
  formatScore
} from '../../js/exercise-helpers.js';

const $ = (id) => document.getElementById(id);

const ctx = {
  space: null,
  assignments: [],
  exercises: [],
  editing: null,
  detail: null,
  picker: null,
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
  box.innerHTML = `<p>${escapeHtml(message)}</p>${retry ? '<button type="button" class="kz-btn mt-4" data-retry>Thử lại</button>' : ''}`;
  box.classList.remove('hidden');
  if (retry) box.querySelector('[data-retry]').addEventListener('click', retry);
}

/** Danh sách nút mở tệp (đề bài hoặc bài làm). dataAttr: tên thuộc tính data-* chứa fileId. */
function fileLinksHtml(files, dataAttr) {
  if (!files || !files.length) return '';
  return `<div class="flex flex-wrap gap-1.5">${files.map((f) => `
    <button type="button" class="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-surface-container-lowest text-primary font-semibold hover:underline text-left max-w-full" ${dataAttr}="${escapeHtml(f.fileId)}" title="${escapeHtml(fileTypeLabel(f.fileType))} · ${escapeHtml(formatFileSize(f.fileSize))}">
      <span class="material-symbols-outlined text-[16px] shrink-0">attach_file</span><span class="min-w-0 [overflow-wrap:anywhere]">${escapeHtml(f.fileName)}</span>
    </button>`).join('')}</div>`;
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
  list.innerHTML = '<li class="text-on-surface-variant">Đang tải bài tập...</li>';
  try {
    const [assignments, exercises] = await Promise.all([
      API.assignments.list(space.spaceId),
      API.exercises.list(space.spaceId).catch(() => [])
    ]);
    ctx.assignments = assignments || [];
    ctx.exercises = exercises || [];
    render();
  } catch (err) {
    list.innerHTML = '';
    stateBox(friendlyErrorMessage(err), () => loadTutorAssignments(ctx.space));
  }
}

export function hideTutorAssignments() {
  $('assignmentsSection')?.classList.add('hidden');
}

/** Đường dẫn trang soạn / chấm bài làm trực tiếp. */
function exerciseEditorUrl(exerciseId = null, tab = null) {
  const q = new URLSearchParams({ space: ctx.space.spaceId });
  if (exerciseId) q.set('exercise', exerciseId);
  if (tab) q.set('tab', tab);
  return `/pages/exercise-editor/index.html?${q.toString()}`;
}

function exerciseItemHtml(e) {
  const meta = [
    `${e.questionCount} câu · ${formatScore(e.maxScore)} điểm`,
    e.timeLimitMinutes ? `${e.timeLimitMinutes} phút` : 'Không giới hạn thời gian',
    `${e.maxAttempts} lượt`,
    e.opensAt ? `Mở ${formatDateTime(e.opensAt)}` : '',
    e.closesAt ? `Đóng ${formatDateTime(e.closesAt)}` : ''
  ].filter(Boolean).join(' · ');
  const progress = e.attemptsUsed
    ? `Học viên đã làm ${e.attemptsUsed} lượt${e.bestScore !== null && e.bestScore !== undefined ? ` · Điểm cao nhất: ${formatScore(e.bestScore)}` : ''}${e.pendingGrading ? ' · Có bài chờ chấm' : ''}`
    : 'Học viên chưa làm';
  return `
    <li class="kz-item flex flex-col sm:flex-row sm:items-start gap-3" data-exercise="${escapeHtml(e.exerciseId)}">
      <div class="flex-1 min-w-0 space-y-1.5">
        <div class="flex flex-wrap items-center gap-2">
          <span class="kz-badge" data-tone="active"><span class="material-symbols-outlined text-[14px]">edit_note</span>Làm trực tiếp</span>
          <h3 class="font-title-sm text-title-sm text-on-surface min-w-0 [overflow-wrap:anywhere]">${escapeHtml(e.title)}</h3>
          <span class="kz-badge" data-tone="${exerciseStateTone(e.state)}">${escapeHtml(exerciseStateLabel(e.state))}</span>
        </div>
        <p class="font-caption text-caption text-on-surface-variant">${escapeHtml(meta)}</p>
        <p class="font-caption text-caption ${e.pendingGrading ? 'text-tertiary font-semibold' : 'text-on-surface-variant'}">${escapeHtml(progress)}</p>
      </div>
      <div class="flex gap-2 shrink-0">
        ${e.attemptsUsed ? `<a class="kz-btn" href="${exerciseEditorUrl(e.exerciseId, 'bai-lam')}">${e.pendingGrading ? 'Chấm bài' : 'Xem bài làm'}</a>` : ''}
        <a class="kz-btn" href="${exerciseEditorUrl(e.exerciseId)}">${e.state === 'DRAFT' ? 'Soạn tiếp' : 'Mở bài'}</a>
      </div>
    </li>`;
}

function render() {
  const list = $('assignmentsList');
  if (!ctx.assignments.length && !ctx.exercises.length) {
    list.innerHTML = '';
    stateBox('Hiện chưa có bài tập nào trong lớp học này.');
    return;
  }
  $('assignmentsState').classList.add('hidden');
  // Gộp bài nộp tệp và bài làm trực tiếp, bài mới nhất lên trên
  const items = [
    ...ctx.assignments.map((a) => ({ kind: 'file', at: a.createdAt, a })),
    ...ctx.exercises.map((e) => ({ kind: 'online', at: e.createdAt, e }))
  ].sort((x, y) => String(y.at || '').localeCompare(String(x.at || '')));
  list.innerHTML = items.map((it) => (it.kind === 'online' ? exerciseItemHtml(it.e) : fileItemHtml(it.a))).join('');
  bindFileItems(list);
}

function fileItemHtml(a) {
  return `
    <li class="kz-item flex flex-col sm:flex-row sm:items-start gap-3" data-assignment="${escapeHtml(a.assignmentId)}">
      <div class="flex-1 min-w-0 space-y-1.5">
        <div class="flex flex-wrap items-center gap-2">
          <span class="kz-badge"><span class="material-symbols-outlined text-[14px]">attach_file</span>Nộp tệp</span>
          <h3 class="font-title-sm text-title-sm text-on-surface min-w-0 [overflow-wrap:anywhere]">${escapeHtml(a.title)}</h3>
          ${a.opened === false
            ? `<span class="kz-badge" data-tone="paused">Chưa mở · mở lúc ${escapeHtml(formatDateTime(a.opensAt))}</span>`
            : `<span class="kz-badge" data-tone="${submissionStateTone(a.submissionState)}">${escapeHtml(submissionStateLabel(a.submissionState))}</span>`}
        </div>
        ${a.description ? `<p class="text-on-surface-variant whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(a.description)}</p>` : ''}
        ${fileLinksHtml(a.attachments, 'data-open-attachment')}
        <p class="font-caption text-caption text-on-surface-variant">Hạn nộp: ${escapeHtml(formatDueDate(a.dueDate))} · Điểm tối đa: ${escapeHtml(formatPoints(a.totalPoints))}
          · ${Number(a.attemptCount) > 0 ? 'Học viên đã nộp' : 'Học viên chưa nộp'}${a.grade !== null && a.grade !== undefined ? ` · Điểm: ${escapeHtml(formatPoints(a.grade))}` : ''}</p>
      </div>
      <div class="flex gap-2 shrink-0">
        <button type="button" class="kz-btn" data-view-assignment="${escapeHtml(a.assignmentId)}">${a.canGrade ? 'Xem và chấm' : 'Xem bài nộp'}</button>
        ${a.canEdit ? `<button type="button" class="kz-btn" data-edit-assignment="${escapeHtml(a.assignmentId)}">Chỉnh sửa</button>` : ''}
      </div>
    </li>`;
}

function bindFileItems(list) {
  list.querySelectorAll('[data-view-assignment]').forEach((btn) => {
    btn.addEventListener('click', () => openSubmissions(btn.dataset.viewAssignment));
  });
  list.querySelectorAll('[data-edit-assignment]').forEach((btn) => {
    btn.addEventListener('click', () => openForm(ctx.assignments.find((a) => a.assignmentId === btn.dataset.editAssignment)));
  });
  list.querySelectorAll('[data-open-attachment]').forEach((btn) => {
    const assignmentId = btn.closest('[data-assignment]').dataset.assignment;
    btn.addEventListener('click', () => openBlob(
      () => API.assignments.attachmentFile(ctx.space.spaceId, assignmentId, btn.dataset.openAttachment), btn));
  });
}

// ----------------------------------------------------------------------------
// Giao / sửa bài tập (UC-66, UC-95) — nhiều tệp đề bài
// ----------------------------------------------------------------------------
function openForm(assignment = null) {
  ctx.editing = assignment;
  const isEdit = !!assignment;
  $('assignmentModalTitle').textContent = isEdit ? 'Chỉnh sửa bài tập' : 'Giao bài tập';
  $('assignmentSubmit').textContent = isEdit ? 'Lưu thay đổi' : 'Giao bài tập';
  $('assignmentTitleInput').value = assignment?.title || '';
  $('assignmentDescriptionInput').value = assignment?.description || '';
  $('assignmentDueInput').value = toDateTimeLocalValue(assignment?.dueDate);
  $('assignmentOpensInput').value = toDateTimeLocalValue(assignment?.opensAt);
  $('assignmentPointsInput').value = assignment ? formatPoints(assignment.totalPoints) : '';
  ctx.picker.reset(assignment?.attachments || [], { max: MAX_FILES_PER_UPLOAD });
  $('assignmentFileHint').textContent = isEdit && assignment.attachments?.length
    ? `Bấm "Thay" để đổi đúng tệp đó, "Xóa" để bỏ tệp, "＋ Thêm tệp đề bài" để bổ sung. ${FILE_RULES_TEXT}`
    : FILE_RULES_TEXT;
  ['assignmentTitleError', 'assignmentDescriptionError', 'assignmentDueError', 'assignmentPointsError', 'assignmentOpensError',
    'assignmentFileError', 'assignmentFormError'].forEach((id) => setFieldError(id, ''));
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
  const opensLocal = $('assignmentOpensInput').value;
  if (opensLocal && form.dueLocal && !(new Date(opensLocal) < new Date(form.dueLocal))) {
    errors.opensAt = 'Thời gian mở bài phải trước hạn nộp.';
  }
  setFieldError('assignmentOpensError', errors.opensAt);
  setFieldError('assignmentPointsError', errors.totalPoints);
  setFieldError('assignmentFormError', '');

  const { newFiles, removeIds, keptCount } = ctx.picker.getChanges();
  const fileError = validateMaterialFiles(newFiles, { required: false, existing: keptCount });
  setFieldError('assignmentFileError', fileError);
  if (!errors.description && !form.description.trim() && keptCount + newFiles.length === 0) {
    errors.description = 'Vui lòng nhập nội dung bài tập hoặc tải lên tệp đề bài.';
    setFieldError('assignmentDescriptionError', errors.description);
  }
  if (Object.keys(errors).length || fileError) return;

  const unchangedDue = isEdit && ctx.editing.dueDate && toDateTimeLocalValue(ctx.editing.dueDate) === form.dueLocal;
  const payload = {
    title: form.title.trim(),
    description: form.description.trim(),
    dueDate: unchangedDue ? ctx.editing.dueDate : fromDateTimeLocalValue(form.dueLocal),
    totalPoints: parsePoints(form.totalPoints),
    opensAt: fromDateTimeLocalValue($('assignmentOpensInput').value)
  };
  const button = $('assignmentSubmit');
  busy(button, true, newFiles.length
    ? `Đang ${isEdit ? 'lưu' : 'giao'} bài tập và tải ${newFiles.length} tệp...`
    : (isEdit ? 'Đang lưu...' : 'Đang giao...'));
  try {
    // MỘT lần gửi: thông tin bài tập + tệp thêm + tệp gỡ. Máy chủ kiểm tra hết; lỗi bất kỳ → không lưu gì.
    const body = { ...payload, files: newFiles, removeFileIds: removeIds };
    if (isEdit) await API.assignments.update(ctx.space.spaceId, ctx.editing.assignmentId, body);
    else await API.assignments.create(ctx.space.spaceId, body);
  } catch (err) {
    const fe = err.fieldErrors || {};
    if (fe.title || fe.description || fe.dueDate || fe.totalPoints || fe.file || fe.opensAt) {
      setFieldError('assignmentOpensError', fe.opensAt);
      setFieldError('assignmentTitleError', fe.title);
      setFieldError('assignmentDescriptionError', fe.description);
      setFieldError('assignmentDueError', fe.dueDate);
      setFieldError('assignmentPointsError', fe.totalPoints);
      setFieldError('assignmentFileError', fe.file);
    } else {
      setFieldError('assignmentFormError', friendlyErrorMessage(err));
    }
    busy(button, false);
    return;
  }
  busy(button, false);
  close('assignmentModal');
  ctx.toast(isEdit ? 'Cập nhật bài tập thành công.' : 'Giao bài tập thành công.');
  await loadTutorAssignments(ctx.space);
}

// ----------------------------------------------------------------------------
// Xem bài nộp & chấm điểm (UC-63, UC-67) — mỗi học viên một bài nộp
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

function currentSubmission() {
  const subs = ctx.detail?.submissions || [];
  return subs[subs.length - 1] || null;
}

function renderSubmissions() {
  const a = ctx.detail.assignment;
  const s = currentSubmission();
  $('submissionsModalTitle').textContent = a.title;
  $('submissionsMeta').textContent = `Hạn nộp: ${formatDueDate(a.dueDate)} · Điểm tối đa: ${formatPoints(a.totalPoints)} · Trạng thái: ${submissionStateLabel(a.submissionState)}`;
  const desc = $('submissionsDescription');
  desc.textContent = a.description || '';
  desc.classList.toggle('hidden', !a.description);

  const box = $('submissionsState');
  if (!s) {
    box.textContent = 'Học viên chưa nộp bài cho bài tập này.';
    box.classList.remove('hidden');
    $('submissionsList').innerHTML = '';
  } else {
    box.classList.add('hidden');
    $('submissionsList').innerHTML = `
      <li class="kz-item p-3 space-y-2">
        <div class="flex flex-wrap items-center gap-2">
          <strong>Bài nộp của học viên</strong>
          <span class="kz-badge" data-tone="${s.onTime ? 'active' : 'paused'}">${s.onTime ? 'Đúng hạn' : 'Nộp muộn'}</span>
          ${s.graded ? '<span class="kz-badge" data-tone="active">Đã chấm</span>' : ''}
          <span class="font-caption text-caption text-on-surface-variant">Cập nhật lần cuối: ${escapeHtml(formatDueDate(s.submittedAt))}</span>
        </div>
        ${s.content ? `<p class="text-on-surface-variant whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(s.content)}</p>` : ''}
        ${s.files?.length ? `<div class="space-y-1"><p class="font-caption text-caption text-on-surface-variant">Tệp bài làm (${s.files.length}):</p>${fileLinksHtml(s.files, 'data-open-submission-file')}</div>` : ''}
        ${s.graded ? `<p class="text-on-surface-variant"><strong>Điểm: ${escapeHtml(formatPoints(s.grade))}/${escapeHtml(formatPoints(a.totalPoints))}</strong>${s.tutorFeedback ? ` · Nhận xét: ${escapeHtml(s.tutorFeedback)}` : ''}</p>` : ''}
      </li>`;
    $('submissionsList').querySelectorAll('[data-open-submission-file]').forEach((btn) => {
      btn.addEventListener('click', () => openBlob(() => API.assignments.submissionFile(
        ctx.space.spaceId, a.assignmentId, s.submissionId, btn.dataset.openSubmissionFile), btn));
    });
  }

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
    else if (s && a.dueDate && !a.dueOver) message = 'Chưa hết hạn nộp bài nên chưa thể chấm điểm (học viên vẫn có thể sửa bài nộp).';
    note.textContent = message;
    note.classList.toggle('hidden', !message);
  }
}

/** Mở tệp (Blob có kiểm quyền bằng token) trong tab mới; tệp Word trình duyệt sẽ tải về máy. */
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
    ctx.toast(friendlyErrorMessage(err), 'error');
  } finally {
    button.disabled = false;
  }
}

async function submitGrade(event) {
  event.preventDefault();
  const a = ctx.detail.assignment;
  const s = currentSubmission();
  const gradeValue = $('gradeInput').value;
  const feedback = $('feedbackInput').value;
  const gradeError = validateGrade(gradeValue, a.totalPoints);
  setFieldError('gradeError', gradeError);
  setFieldError('feedbackError', feedback.trim().length > 5000 ? 'Nhận xét tối đa 5000 ký tự.' : '');
  setFieldError('gradeFormError', '');
  if (gradeError || feedback.trim().length > 5000 || !s) return;

  const button = $('gradeSubmit');
  busy(button, true, 'Đang gửi...');
  try {
    await API.assignments.grade(ctx.space.spaceId, a.assignmentId, s.submissionId, {
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
  ctx.picker = createFileList($('assignmentFileList'), { accept: MATERIAL_ACCEPT_ATTR, addLabel: 'Thêm tệp đề bài' });
  // Giao bài tập: chọn loại bài (nộp tệp → hộp thoại hiện có; làm trực tiếp → trang soạn đề)
  $('openAssignmentFormBtn')?.addEventListener('click', () => open('assignmentTypeModal'));
  $('chooseFileAssignment')?.addEventListener('click', () => {
    close('assignmentTypeModal');
    openForm(null);
  });
  $('chooseOnlineExercise')?.addEventListener('click', () => {
    window.location.href = exerciseEditorUrl();
  });
  $('assignmentForm')?.addEventListener('submit', submitForm);
  $('gradeForm')?.addEventListener('submit', submitGrade);
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      close('assignmentModal');
      close('submissionsModal');
      close('assignmentTypeModal');
    }
  });
}
