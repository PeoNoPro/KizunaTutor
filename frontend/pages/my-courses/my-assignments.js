/**
 * KizunaTutor - FE-07 Khối "Bài tập" trong trang "Lớp học của tôi" (học viên).
 * UC-63 View Assignments, UC-64 Submit Assignment, UC-96 sửa bài đã nộp (thêm/xóa/thay tệp, sửa nội dung).
 */

import { API } from '../../js/api.js';
import { formatDateTime, formatScore } from '../../js/exercise-helpers.js';
import { createFileList } from '../../js/file-picker.js';
import {
  fileTypeLabel,
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

const $ = (id) => document.getElementById(id);

const ctx = { space: null, assignments: [], detail: null, notify: () => {} };

function stateBox(message, retry) {
  const box = $('myAssignmentsState');
  box.innerHTML = `<p>${escapeHtml(message)}</p>${retry ? '<button type="button" class="mc-btn mt-4" data-retry>Thử lại</button>' : ''}`;
  box.classList.remove('hidden');
  if (retry) box.querySelector('[data-retry]').addEventListener('click', retry);
}

export function hideLearnerAssignments() {
  $('myCourseAssignments')?.classList.add('hidden');
  $('myAssignmentDetail')?.classList.add('hidden');
}

export async function loadLearnerAssignments(space, { notify } = {}) {
  ctx.space = space;
  if (notify) ctx.notify = notify;
  const card = $('myCourseAssignments');
  const list = $('myAssignmentsList');
  if (!card || !space) return;
  card.classList.remove('hidden');
  $('myAssignmentsState').classList.add('hidden');
  list.innerHTML = '<li class="text-[#444]">Đang tải bài tập...</li>';
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
  const button = e.inProgressAttemptId ? 'Làm tiếp' : (e.canStart ? (e.attemptsUsed ? 'Làm lượt mới' : 'Làm bài') : 'Xem kết quả');
  const meta = [
    `${e.questionCount} câu · ${formatScore(e.maxScore)} điểm`,
    e.timeLimitMinutes ? `${e.timeLimitMinutes} phút` : 'Không giới hạn thời gian',
    `Đã làm ${e.attemptsUsed}/${e.maxAttempts} lượt`,
    e.closesAt ? `Đóng đề ${formatDateTime(e.closesAt)}` : ''
  ].filter(Boolean).join(' · ');
  return `
    <li class="border border-black rounded-[5px] p-4 bg-white flex flex-col sm:flex-row sm:items-start gap-3">
      <div class="flex-1 min-w-0 space-y-1">
        <div class="flex flex-wrap items-center gap-2">
          <span class="mc-badge" data-tone="active">Làm trực tiếp</span>
          <h3 class="font-semibold text-[16px] break-words">${escapeHtml(e.title)}</h3>
          <span class="mc-badge">${escapeHtml(status)}</span>
        </div>
        <p class="mc-meta text-[#555]">${escapeHtml(meta)}</p>
      </div>
      <a class="mc-btn ${e.canStart || e.inProgressAttemptId ? 'mc-btn-primary' : ''} shrink-0" href="${url}">${button}</a>
    </li>`;
}

function renderList() {
  const list = $('myAssignmentsList');
  const exercises = ctx.exercises || [];
  if (!ctx.assignments.length && !exercises.length) {
    list.innerHTML = '';
    stateBox('Hiện chưa có bài tập nào trong lớp học này.');
    return;
  }
  $('myAssignmentsState').classList.add('hidden');
  const items = [
    ...ctx.assignments.map((a) => ({ kind: 'file', at: a.createdAt, a })),
    ...exercises.map((e) => ({ kind: 'online', at: e.createdAt, e }))
  ].sort((x, y) => String(y.at || '').localeCompare(String(x.at || '')));
  list.innerHTML = items.map((it) => (it.kind === 'online' ? exerciseItemHtml(it.e) : fileItemHtml(it.a))).join('');
  list.querySelectorAll('[data-open-assignment]').forEach((btn) => {
    btn.addEventListener('click', () => openDetail(btn.dataset.openAssignment));
  });
}

function fileItemHtml(a) {
  return `
    <li class="border border-black rounded-[5px] p-4 bg-white flex flex-col sm:flex-row sm:items-start gap-3">
      <div class="flex-1 min-w-0 space-y-1">
        <div class="flex flex-wrap items-center gap-2">
          <span class="mc-badge">Nộp tệp</span>
          <h3 class="font-semibold text-[16px] break-words">${escapeHtml(a.title)}</h3>
          <span class="mc-badge" data-tone="${submissionStateTone(a.submissionState)}">${escapeHtml(submissionStateLabel(a.submissionState))}</span>
        </div>
        <p class="mc-meta text-[#555]">Hạn nộp: ${escapeHtml(formatDueDate(a.dueDate))} · Điểm tối đa: ${escapeHtml(formatPoints(a.totalPoints))}
          ${a.grade !== null && a.grade !== undefined ? ` · Điểm của bạn: ${escapeHtml(formatPoints(a.grade))}` : ''}</p>
      </div>
      <button type="button" class="mc-btn shrink-0" data-open-assignment="${escapeHtml(a.assignmentId)}">Xem bài tập</button>
    </li>`;
}

async function openDetail(assignmentId) {
  const panel = $('myAssignmentDetail');
  panel.innerHTML = '<p class="text-[#444]">Đang tải bài tập...</p>';
  panel.classList.remove('hidden');
  panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
  try {
    ctx.detail = await API.assignments.detail(ctx.space.spaceId, assignmentId);
    renderDetail();
  } catch (err) {
    panel.innerHTML = `<p class="text-[#BF360C]">${escapeHtml(friendlyErrorMessage(err))}</p>
      <button type="button" class="mc-btn" data-close-detail>Đóng</button>`;
    panel.querySelector('[data-close-detail]').addEventListener('click', () => panel.classList.add('hidden'));
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
  return `<div class="flex flex-wrap gap-2">${files.map((f) => `
    <button type="button" class="mc-btn max-w-full" ${dataAttr}="${escapeHtml(f.fileId)}" title="${escapeHtml(fileTypeLabel(f.fileType))} · ${escapeHtml(formatFileSize(f.fileSize))}">
      <span class="min-w-0 [overflow-wrap:anywhere]">📎 ${escapeHtml(f.fileName)}</span>
    </button>`).join('')}</div>`;
}

/** Form nộp bài (lần đầu) hoặc sửa bài nộp (gỡ tệp cũ / thêm tệp mới / sửa nội dung). */
function submissionFormHtml(a, submission) {
  const editing = !!submission;
  return `
    <form id="mySubmissionForm" class="border border-black rounded-[5px] p-4 space-y-3 bg-[#F8FAFC]" novalidate>
      <h3 class="font-semibold">${editing ? 'Chỉnh sửa bài nộp' : 'Nộp bài làm'}</h3>
      ${!editing && a.dueOver ? '<p class="text-[#BF360C]">Đã quá hạn nộp. Bạn chỉ được nộp một lần (nộp muộn) và không thể chỉnh sửa sau đó.</p>' : ''}
      ${editing && a.dueDate ? `<p class="mc-meta text-[#555]">Bạn có thể chỉnh sửa đến ${escapeHtml(formatDueDate(a.dueDate))}, khi gia sư chưa chấm điểm.</p>` : ''}
      <div>
        <label for="mySubmissionContent" class="block font-semibold mb-1">Nội dung trả lời <span class="font-normal text-[#555]">(không bắt buộc nếu có tệp)</span></label>
        <textarea id="mySubmissionContent" rows="4" maxlength="5000" class="w-full border border-black rounded-[5px] p-2 bg-white">${escapeHtml(submission?.content || '')}</textarea>
      </div>
      <div>
        <p class="block font-semibold mb-1">${editing ? 'Tệp bài làm' : 'Tệp bài làm'} <span class="font-normal text-[#555]">(không bắt buộc nếu có nội dung)</span></p>
        <div id="mySubmissionFileList"></div>
        <p class="mc-meta text-[#555] mt-1">${escapeHtml(FILE_RULES_TEXT)}${editing ? ' Bấm "Thay" để đổi đúng tệp đó, "Xóa" để bỏ tệp.' : ''}</p>
      </div>
      <p id="mySubmissionError" class="text-[#BF360C] text-[12px] whitespace-pre-line [overflow-wrap:anywhere] hidden"></p>
      <div class="flex justify-end gap-2">
        ${editing ? '<button type="button" class="mc-btn" data-cancel-edit>Hủy</button>' : ''}
        <button type="submit" id="mySubmissionSubmit" class="mc-btn mc-btn-primary">${editing ? 'Lưu bài nộp' : 'Nộp bài'}</button>
      </div>
    </form>`;
}

function renderDetail({ editMode = false, message = '' } = {}) {
  const a = ctx.detail.assignment;
  const subs = ctx.detail.submissions || [];
  const submission = subs[subs.length - 1] || null;
  const panel = $('myAssignmentDetail');
  const reason = lockedReason(a, submission);

  let actionHtml = '';
  if (!submission && a.canSubmit) actionHtml = submissionFormHtml(a, null);
  else if (submission && a.canEditSubmission && editMode) actionHtml = submissionFormHtml(a, submission);
  else if (reason && !(submission && a.canEditSubmission)) {
    actionHtml = `<p class="border border-black rounded-[5px] bg-[#FDEDE8] text-[#BF360C] px-3 py-2">${escapeHtml(reason)}</p>`;
  }

  panel.innerHTML = `
    ${message ? `<p class="border border-black rounded-[5px] bg-[#E5F4EA] text-[#1E8B3F] px-3 py-2 font-semibold">${escapeHtml(message)}</p>` : ''}
    <div class="flex items-start justify-between gap-3">
      <div class="min-w-0">
        <h2 class="mc-heading text-[20px] [overflow-wrap:anywhere]">${escapeHtml(a.title)}</h2>
        <p class="mc-meta text-[#555]">Hạn nộp: ${escapeHtml(formatDueDate(a.dueDate))} · Điểm tối đa: ${escapeHtml(formatPoints(a.totalPoints))} · Trạng thái: ${escapeHtml(submissionStateLabel(a.submissionState))}</p>
      </div>
      <button type="button" class="mc-btn shrink-0" data-close-detail>Đóng</button>
    </div>
    ${a.description ? `<p class="text-[#333] whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(a.description)}</p>` : ''}
    ${a.attachments?.length ? `<div class="space-y-1.5"><p class="font-semibold">Tệp đề bài (${a.attachments.length})</p>${fileButtonsHtml(a.attachments, 'data-open-attachment')}</div>` : ''}
    ${submission?.graded ? `<div class="border border-black rounded-[5px] bg-[#E5F4EA] px-3 py-2">
        <p><strong>Điểm: ${escapeHtml(formatPoints(submission.grade))}/${escapeHtml(formatPoints(a.totalPoints))}</strong></p>
        ${submission.tutorFeedback ? `<p class="whitespace-pre-line [overflow-wrap:anywhere]">Nhận xét của gia sư: ${escapeHtml(submission.tutorFeedback)}</p>` : ''}
      </div>` : ''}
    ${submission && !editMode ? `
      <div class="border border-black rounded-[5px] p-3 bg-white space-y-2">
        <div class="flex flex-wrap items-center gap-2">
          <h3 class="font-semibold">Bài nộp của bạn</h3>
          <span class="mc-badge" data-tone="${submission.onTime ? 'active' : 'paused'}">${submission.onTime ? 'Đúng hạn' : 'Nộp muộn'}</span>
          <span class="mc-meta text-[#555]">Cập nhật lần cuối: ${escapeHtml(formatDueDate(submission.submittedAt))}</span>
        </div>
        ${submission.content ? `<p class="text-[#333] whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(submission.content)}</p>` : ''}
        ${fileButtonsHtml(submission.files, 'data-open-submission-file')}
        ${a.canEditSubmission ? '<div class="flex justify-end"><button type="button" class="mc-btn mc-btn-primary" data-edit-submission>Chỉnh sửa bài nộp</button></div>' : ''}
      </div>` : (!submission ? '<p class="text-[#444]">Bạn chưa nộp bài cho bài tập này.</p>' : '')}
    ${actionHtml}
  `;

  panel.querySelector('[data-close-detail]').addEventListener('click', () => panel.classList.add('hidden'));
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
