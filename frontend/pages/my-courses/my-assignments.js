/**
 * KizunaTutor - FE-07 Khối "Bài tập" trong trang "Lớp học của tôi" (học viên).
 * UC-63 View Assignments, UC-64 Submit Assignment, UC-96 Resubmit Assignments.
 */

import { API } from '../../js/api.js';
import { fileTypeLabel, friendlyErrorMessage, escapeHtml, MATERIAL_ACCEPT_ATTR } from '../../js/classroom-helpers.js';
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
    ctx.assignments = await API.assignments.list(space.spaceId) || [];
    renderList();
  } catch (err) {
    list.innerHTML = '';
    stateBox(friendlyErrorMessage(err), () => loadLearnerAssignments(ctx.space));
  }
}

function renderList() {
  const list = $('myAssignmentsList');
  if (!ctx.assignments.length) {
    list.innerHTML = '';
    stateBox('Hiện chưa có bài tập nào trong lớp học này.');
    return;
  }
  $('myAssignmentsState').classList.add('hidden');
  list.innerHTML = ctx.assignments.map((a) => `
    <li class="border border-black rounded-[5px] p-4 bg-white flex flex-col sm:flex-row sm:items-start gap-3">
      <div class="flex-1 min-w-0 space-y-1">
        <div class="flex flex-wrap items-center gap-2">
          <h3 class="font-semibold text-[16px] break-words">${escapeHtml(a.title)}</h3>
          <span class="mc-badge" data-tone="${submissionStateTone(a.submissionState)}">${escapeHtml(submissionStateLabel(a.submissionState))}</span>
        </div>
        <p class="mc-meta text-[#555]">Hạn nộp: ${escapeHtml(formatDueDate(a.dueDate))} · Điểm tối đa: ${escapeHtml(formatPoints(a.totalPoints))}
          ${a.grade !== null && a.grade !== undefined ? ` · Điểm của bạn: ${escapeHtml(formatPoints(a.grade))}` : ''}</p>
      </div>
      <button type="button" class="mc-btn shrink-0" data-open-assignment="${escapeHtml(a.assignmentId)}">Xem bài tập</button>
    </li>
  `).join('');
  list.querySelectorAll('[data-open-assignment]').forEach((btn) => {
    btn.addEventListener('click', () => openDetail(btn.dataset.openAssignment));
  });
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

function lockedReason(a, subs) {
  if (a.submissionState === 'GRADED') return 'Bài tập đã được chấm điểm.';
  if (a.dueOver && subs.length) return 'Đã quá hạn nộp bài nên không thể nộp lại hoặc chỉnh sửa bài làm.';
  if (!a.canSubmit) return 'Lớp học đang ở chế độ chỉ xem nên không thể nộp bài.';
  return '';
}

function renderDetail() {
  const a = ctx.detail.assignment;
  const subs = ctx.detail.submissions || [];
  const panel = $('myAssignmentDetail');
  const graded = subs.find((s) => s.graded);
  const reason = lockedReason(a, subs);

  panel.innerHTML = `
    <div class="flex items-start justify-between gap-3">
      <div class="min-w-0">
        <h2 class="mc-heading text-[20px] break-words">${escapeHtml(a.title)}</h2>
        <p class="mc-meta text-[#555]">Hạn nộp: ${escapeHtml(formatDueDate(a.dueDate))} · Điểm tối đa: ${escapeHtml(formatPoints(a.totalPoints))} · Trạng thái: ${escapeHtml(submissionStateLabel(a.submissionState))}</p>
      </div>
      <button type="button" class="mc-btn shrink-0" data-close-detail>Đóng</button>
    </div>
    ${a.description ? `<p class="text-[#333] whitespace-pre-line break-words">${escapeHtml(a.description)}</p>` : ''}
    ${a.attachmentName ? `<div class="flex flex-wrap items-center gap-2"><span class="font-semibold">Tệp đề bài:</span>
        <button type="button" class="mc-btn" data-open-attachment>Mở tệp: ${escapeHtml(a.attachmentName)} (${escapeHtml(fileTypeLabel(a.attachmentType))})</button></div>` : ''}
    ${graded ? `<div class="border border-black rounded-[5px] bg-[#E5F4EA] px-3 py-2">
        <p><strong>Điểm: ${escapeHtml(formatPoints(graded.grade))}/${escapeHtml(formatPoints(a.totalPoints))}</strong></p>
        ${graded.tutorFeedback ? `<p class="whitespace-pre-line break-words">Nhận xét của gia sư: ${escapeHtml(graded.tutorFeedback)}</p>` : ''}
      </div>` : ''}
    <div class="space-y-2">
      <h3 class="font-semibold">Các lần nộp của bạn</h3>
      ${subs.length ? `<ul class="space-y-2">${subs.slice().reverse().map((s) => `
        <li class="border border-black rounded-[5px] p-3 bg-white space-y-1">
          <div class="flex flex-wrap items-center gap-2">
            <strong>Lần nộp ${Number(s.attemptNumber)}</strong>
            <span class="mc-badge" data-tone="${s.onTime ? 'active' : 'paused'}">${s.onTime ? 'Đúng hạn' : 'Nộp muộn'}</span>
            <span class="mc-meta text-[#555]">${escapeHtml(formatDueDate(s.submittedAt))}</span>
          </div>
          ${s.content ? `<p class="text-[#333] whitespace-pre-line break-words">${escapeHtml(s.content)}</p>` : ''}
          ${s.fileName ? `<button type="button" class="mc-btn" data-open-submission="${escapeHtml(s.submissionId)}">Mở tệp: ${escapeHtml(s.fileName)} (${escapeHtml(fileTypeLabel(s.fileType))})</button>` : ''}
        </li>`).join('')}</ul>` : '<p class="text-[#444]">Bạn chưa nộp bài cho bài tập này.</p>'}
    </div>
    ${a.canSubmit ? `
      <form id="mySubmissionForm" class="border border-black rounded-[5px] p-4 space-y-3 bg-[#F8FAFC]" novalidate>
        <h3 class="font-semibold">${subs.length ? 'Nộp lại bài làm' : 'Nộp bài làm'}</h3>
        ${a.dueOver ? '<p class="text-[#BF360C]">Đã quá hạn nộp. Bạn chỉ được nộp một lần và không thể nộp lại.</p>' : ''}
        <div>
          <label for="mySubmissionContent" class="block font-semibold mb-1">Nội dung trả lời <span class="font-normal text-[#555]">(không bắt buộc nếu có tệp)</span></label>
          <textarea id="mySubmissionContent" rows="4" maxlength="5000" class="w-full border border-black rounded-[5px] p-2 bg-white"></textarea>
        </div>
        <div>
          <label for="mySubmissionFile" class="block font-semibold mb-1">Tệp bài làm <span class="font-normal text-[#555]">(không bắt buộc)</span></label>
          <input id="mySubmissionFile" type="file" accept="${MATERIAL_ACCEPT_ATTR}" class="w-full border border-black rounded-[5px] p-2 bg-white">
          <p class="mc-meta text-[#555] mt-1">Định dạng PDF, PNG, JPG, MP3. Dung lượng tối đa 10 MB.</p>
        </div>
        <p id="mySubmissionError" class="text-[#BF360C] text-[12px] hidden"></p>
        <div class="flex justify-end">
          <button type="submit" id="mySubmissionSubmit" class="mc-btn mc-btn-primary">${subs.length ? 'Nộp lại' : 'Nộp bài'}</button>
        </div>
      </form>` : (reason ? `<p class="border border-black rounded-[5px] bg-[#FDEDE8] text-[#BF360C] px-3 py-2">${escapeHtml(reason)}</p>` : '')}
  `;

  panel.querySelector('[data-close-detail]').addEventListener('click', () => panel.classList.add('hidden'));
  panel.querySelectorAll('[data-open-submission]').forEach((btn) => {
    btn.addEventListener('click', () => openFile(btn.dataset.openSubmission, btn));
  });
  const attachmentBtn = panel.querySelector('[data-open-attachment]');
  attachmentBtn?.addEventListener('click', () => openBlob(
    () => API.assignments.attachmentFile(ctx.space.spaceId, a.assignmentId), attachmentBtn));
  $('mySubmissionForm')?.addEventListener('submit', submitWork);
}

function openFile(submissionId, button) {
  return openBlob(() => API.assignments.submissionFile(
    ctx.space.spaceId, ctx.detail.assignment.assignmentId, submissionId), button);
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

async function submitWork(event) {
  event.preventDefault();
  const content = $('mySubmissionContent').value;
  const file = $('mySubmissionFile').files[0] || null;
  const errorEl = $('mySubmissionError');
  const error = validateSubmission({ content, file });
  errorEl.textContent = error || '';
  errorEl.classList.toggle('hidden', !error);
  if (error) return;

  const button = $('mySubmissionSubmit');
  button.disabled = true;
  const label = button.textContent;
  button.textContent = 'Đang nộp...';
  try {
    const assignmentId = ctx.detail.assignment.assignmentId;
    await API.assignments.submit(ctx.space.spaceId, assignmentId, { content: content.trim(), file });
    ctx.detail = await API.assignments.detail(ctx.space.spaceId, assignmentId);
    renderDetail();
    const panel = $('myAssignmentDetail');
    const ok = document.createElement('p');
    ok.className = 'border border-black rounded-[5px] bg-[#E5F4EA] text-[#1E8B3F] px-3 py-2 font-semibold';
    ok.textContent = ctx.detail.submissions.length > 1 ? 'Nộp lại bài tập thành công.' : 'Nộp bài tập thành công.';
    panel.prepend(ok);
    loadLearnerAssignments(ctx.space);
  } catch (err) {
    errorEl.textContent = (err.fieldErrors && (err.fieldErrors.content || err.fieldErrors.file)) || friendlyErrorMessage(err);
    errorEl.classList.remove('hidden');
    button.disabled = false;
    button.textContent = label;
  }
}
