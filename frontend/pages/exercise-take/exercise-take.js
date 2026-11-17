/**
 * KizunaTutor - FE-07 Học viên làm bài trực tiếp: /pages/exercise-take/index.html?space={spaceId}&exercise={exerciseId}[&attempt={attemptId}]
 * - Màn giới thiệu: thông tin bài, các lượt đã làm, nút Bắt đầu / Làm tiếp.
 * - Màn làm bài: bảng số câu, đồng hồ đếm ngược (theo giờ máy chủ), tự lưu từng câu, nộp có xác nhận, hết giờ tự nộp.
 *   Tắt trang giữa chừng thì vào lại làm tiếp đúng lượt đó (đồng hồ vẫn chạy).
 * - Màn kết quả: điểm, câu đúng / sai, đáp án đúng (nếu gia sư cho xem), nhận xét của gia sư.
 */

import { API } from '../../js/api.js';
import {
  escapeHtml,
  friendlyErrorMessage,
  getCurrentRole,
  ROLE_LEARNER,
  roleDeniedHtml,
  clearSessionAndGoToLogin
} from '../../js/classroom-helpers.js';
import {
  questionTypeLabel,
  attemptStatusLabel,
  gradingModeLabel,
  formatDateTime,
  formatScore,
  formatCountdown,
  isAnswered,
  describeResponse,
  describeCorrect
} from '../../js/exercise-helpers.js';
import { renderInteractive } from '../../js/exercise-player.js';

const $ = (id) => document.getElementById(id);
const params = new URLSearchParams(window.location.search);
const spaceId = params.get('space');
const exerciseId = params.get('exercise');

const state = {
  summary: null,
  instructions: '',
  attempt: null,
  current: 0,
  responses: {},
  timers: {},
  saving: new Set(),
  failed: new Set(),
  deadlineMs: null,
  tick: null,
  submitting: false
};

function classLink() {
  return `/learner/my-courses?space=${encodeURIComponent(spaceId || '')}`;
}

function showToast(message, tone = 'success') {
  const t = $('takeToast');
  t.textContent = message;
  t.style.background = tone === 'success' ? '#cce5ff' : '#ffdad6';
  t.style.color = tone === 'success' ? '#004b73' : '#93000a';
  t.classList.remove('hidden');
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => t.classList.add('hidden'), 3500);
}

function setHeader({ sub = '', taking = false } = {}) {
  $('takeTitle').textContent = state.summary?.title || state.attempt?.exerciseTitle || 'Bài làm trực tiếp';
  $('takeSub').textContent = sub;
  $('submitBtn').classList.toggle('hidden', !taking);
  $('timerBox').classList.toggle('hidden', !taking || state.deadlineMs === null);
  $('saveState').classList.toggle('hidden', !taking);
}

/** Hộp xác nhận dùng chung: trả Promise<boolean>. */
function confirmBox(title, message, okText) {
  return new Promise((resolve) => {
    $('confirmTitle').textContent = title;
    $('confirmText').textContent = message;
    $('confirmOk').textContent = okText;
    $('confirmModal').classList.remove('hidden');
    const done = (v) => {
      $('confirmModal').classList.add('hidden');
      $('confirmOk').onclick = null;
      $('confirmCancel').onclick = null;
      resolve(v);
    };
    $('confirmOk').onclick = () => done(true);
    $('confirmCancel').onclick = () => done(false);
  });
}

// ============================================================ màn giới thiệu

async function showIntro() {
  stopTimer();
  state.attempt = null;
  setHeader();
  const main = $('takeMain');
  main.innerHTML = '<p class="text-on-surface-variant">Đang tải bài tập...</p>';
  try {
    const [detail, attempts] = await Promise.all([
      API.exercises.detail(spaceId, exerciseId),
      API.exercises.attempts(spaceId, exerciseId).catch(() => [])
    ]);
    state.summary = detail.summary;
    state.instructions = detail.instructions || '';
    renderIntro(attempts || []);
  } catch (err) {
    main.innerHTML = `<div class="kz-card p-6 text-center space-y-3"><p>${escapeHtml(friendlyErrorMessage(err))}</p>
      <a href="${classLink()}" class="kz-btn">Quay lại lớp học</a></div>`;
  }
}

function renderIntro(attempts) {
  const s = state.summary;
  setHeader({ sub: `${s.questionCount} câu · ${formatScore(s.maxScore)} điểm` });
  const chips = [
    ['quiz', `${s.questionCount} câu hỏi · ${formatScore(s.maxScore)} điểm`],
    ['timer', s.timeLimitMinutes ? `Thời gian làm bài: ${s.timeLimitMinutes} phút` : 'Không giới hạn thời gian'],
    ['replay', `Đã làm ${s.attemptsUsed}/${s.maxAttempts} lượt`],
    ['fact_check', gradingModeLabel(s.gradingMode)],
    ['event', s.closesAt ? `Đóng đề: ${formatDateTime(s.closesAt)}` : 'Không đóng đề']
  ];
  const inProgress = s.inProgressAttemptId;
  let action = '';
  if (inProgress) action = '<button type="button" id="startBtn" class="kz-btn kz-btn-primary">Làm tiếp</button>';
  else if (s.canStart) action = `<button type="button" id="startBtn" class="kz-btn kz-btn-primary">${s.attemptsUsed ? 'Làm lượt mới' : 'Bắt đầu làm bài'}</button>`;
  else if (s.state === 'CLOSED') action = '<p class="font-semibold text-on-surface-variant">Đề đã đóng.</p>';
  else if (s.attemptsUsed >= s.maxAttempts) action = '<p class="font-semibold text-on-surface-variant">Bạn đã dùng hết lượt làm bài.</p>';
  else action = '<p class="font-semibold text-on-surface-variant">Hiện chưa thể làm bài này.</p>';

  $('takeMain').innerHTML = `
    <div class="max-w-3xl mx-auto space-y-5">
      <section class="kz-card p-6 space-y-4">
        <h1 class="font-headline-sm text-headline-sm text-on-surface [overflow-wrap:anywhere]">${escapeHtml(s.title)}</h1>
        ${state.instructions ? `<p class="whitespace-pre-line text-on-surface [overflow-wrap:anywhere]">${escapeHtml(state.instructions)}</p>` : ''}
        <ul class="grid gap-2 sm:grid-cols-2">
          ${chips.map(([icon, text]) => `<li class="flex items-center gap-2 text-on-surface-variant"><span class="material-symbols-outlined text-[18px] text-primary">${icon}</span>${escapeHtml(text)}</li>`).join('')}
        </ul>
        ${s.bestScore !== null && s.bestScore !== undefined ? `<p class="font-semibold">Điểm cao nhất: ${formatScore(s.bestScore)} / ${formatScore(s.maxScore)}</p>` : ''}
        ${s.pendingGrading ? '<p class="text-on-surface-variant">Có lượt đang chờ gia sư chấm.</p>' : ''}
        ${s.timeLimitMinutes && !inProgress && s.canStart ? '<p class="font-caption text-caption text-on-surface-variant">Đồng hồ bắt đầu chạy khi bạn bấm bắt đầu và vẫn chạy nếu bạn rời trang. Hết giờ bài sẽ tự nộp.</p>' : ''}
        <div class="flex flex-wrap items-center gap-3 pt-1">${action}</div>
      </section>
      ${attempts.length ? `
        <section class="kz-card p-6 space-y-3">
          <h2 class="font-title-md text-title-md">Các lượt đã làm</h2>
          <ul class="flex flex-col gap-2">
            ${attempts.map((a) => `
              <li class="kz-item flex flex-wrap items-center gap-3">
                <span class="font-semibold">Lượt ${a.attemptNumber}</span>
                <span class="kz-badge">${escapeHtml(attemptStatusLabel(a.status))}</span>
                ${a.status !== 'IN_PROGRESS' ? `<span class="text-on-surface-variant">Nộp lúc ${escapeHtml(formatDateTime(a.submittedAt))}${a.late ? ' · nộp muộn' : ''}</span>` : ''}
                <span class="flex-1"></span>
                ${a.finalScore !== null && a.finalScore !== undefined ? `<span class="font-semibold">${formatScore(a.finalScore)} / ${formatScore(a.maxScore)} điểm</span>` : ''}
                ${a.status !== 'IN_PROGRESS' ? `<button type="button" class="kz-btn" data-result="${escapeHtml(a.attemptId)}">Xem kết quả</button>` : ''}
              </li>`).join('')}
          </ul>
        </section>` : ''}
    </div>`;
  $('startBtn')?.addEventListener('click', startAttempt);
  $('takeMain').querySelectorAll('[data-result]').forEach((b) => b.addEventListener('click', () => openAttempt(b.dataset.result)));
}

async function startAttempt() {
  const s = state.summary;
  if (!s.inProgressAttemptId && s.timeLimitMinutes) {
    const ok = await confirmBox('Bắt đầu làm bài?',
      `Bài có thời gian ${s.timeLimitMinutes} phút. Đồng hồ bắt đầu chạy ngay khi bạn bấm và vẫn chạy nếu bạn rời trang.`, 'Bắt đầu');
    if (!ok) return;
  }
  try {
    const attempt = await API.exercises.start(spaceId, exerciseId);
    enterAttempt(attempt);
  } catch (err) {
    showToast(friendlyErrorMessage(err), 'error');
  }
}

async function openAttempt(attemptId) {
  try {
    enterAttempt(await API.exercises.attempt(spaceId, exerciseId, attemptId));
  } catch (err) {
    showToast(friendlyErrorMessage(err), 'error');
    if (!state.summary) showIntro();
  }
}

function enterAttempt(attempt) {
  state.attempt = attempt;
  const url = new URL(window.location.href);
  url.searchParams.set('attempt', attempt.attemptId);
  window.history.replaceState(null, '', url);
  if (attempt.status === 'IN_PROGRESS') startTaking();
  else renderResult();
}

// ============================================================ màn làm bài

function startTaking() {
  const a = state.attempt;
  state.responses = {};
  a.questions.forEach((q) => { if (q.response) state.responses[q.questionId] = q.response; });
  state.current = Math.max(0, a.questions.findIndex((q) => !isAnswered(q.type, state.responses[q.questionId])));
  if (state.current < 0) state.current = 0;
  state.deadlineMs = a.remainingSeconds === null || a.remainingSeconds === undefined ? null : Date.now() + a.remainingSeconds * 1000;
  setHeader({ sub: `Lượt ${a.attemptNumber}/${a.maxAttempts}`, taking: true });
  setSaveState('Đã lưu tự động');
  startTimer();
  drawTaking();
}

function answeredCount() {
  return state.attempt.questions.filter((q) => isAnswered(q.type, state.responses[q.questionId])).length;
}

function drawTaking() {
  const a = state.attempt;
  const q = a.questions[state.current];
  const total = a.questions.length;
  $('takeMain').innerHTML = `
    <div class="grid gap-5 lg:grid-cols-[260px_1fr]">
      <aside class="kz-card p-4 h-fit lg:sticky lg:top-20 space-y-3">
        <p class="font-label-md text-label-md">Câu hỏi · đã làm <span id="answeredCount">${answeredCount()}</span>/${total}</p>
        <div class="grid grid-cols-5 gap-2" id="navGrid"></div>
        <div class="flex flex-wrap gap-3 font-caption text-caption text-on-surface-variant">
          <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-primary inline-block"></span>Đã làm</span>
          <span class="flex items-center gap-1"><span class="w-3 h-3 rounded border border-outline-variant inline-block"></span>Chưa làm</span>
        </div>
      </aside>
      <section class="kz-card p-6 space-y-4 min-w-0">
        <div class="flex flex-wrap items-center gap-2">
          <span class="font-title-sm text-title-sm">Câu ${state.current + 1}/${total}</span>
          <span class="kz-badge">${escapeHtml(questionTypeLabel(q.type))}</span>
          <span class="font-caption text-caption text-on-surface-variant">${formatScore(q.points)} điểm</span>
        </div>
        ${q.prompt ? `<p class="text-[16px] whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(q.prompt)}</p>` : ''}
        <div id="questionBody"></div>
        <div class="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-outline-variant/30">
          <button type="button" id="prevBtn" class="kz-btn" ${state.current === 0 ? 'disabled' : ''}>← Câu trước</button>
          ${state.current === total - 1
            ? '<button type="button" id="lastSubmitBtn" class="kz-btn kz-btn-primary">Nộp bài</button>'
            : '<button type="button" id="nextBtn" class="kz-btn kz-btn-primary">Câu sau →</button>'}
        </div>
      </section>
    </div>`;
  drawNav();
  renderInteractive($('questionBody'), q, {
    response: state.responses[q.questionId] || null,
    onChange: (r) => onAnswer(q, r)
  });
  $('prevBtn').addEventListener('click', () => go(state.current - 1));
  $('nextBtn')?.addEventListener('click', () => go(state.current + 1));
  $('lastSubmitBtn')?.addEventListener('click', confirmSubmit);
}

function drawNav() {
  const a = state.attempt;
  $('navGrid').innerHTML = a.questions.map((q, i) => {
    const done = isAnswered(q.type, state.responses[q.questionId]);
    const cur = i === state.current;
    return `<button type="button" data-go="${i}" aria-label="Câu ${i + 1}"
      class="h-9 rounded-lg text-[13px] font-bold border ${done ? 'bg-primary text-on-primary border-primary' : 'border-outline-variant text-on-surface'} ${cur ? 'ring-2 ring-offset-2 ring-primary' : ''}">${i + 1}</button>`;
  }).join('');
  $('navGrid').querySelectorAll('[data-go]').forEach((b) => b.addEventListener('click', () => go(Number(b.dataset.go))));
  if ($('answeredCount')) $('answeredCount').textContent = answeredCount();
}

function go(index) {
  if (index < 0 || index >= state.attempt.questions.length) return;
  state.current = index;
  drawTaking();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function setSaveState(text, tone = 'muted') {
  const el = $('saveState');
  el.textContent = text;
  el.className = `font-caption text-caption ${tone === 'error' ? 'text-error font-semibold' : 'text-on-surface-variant'}`;
}

function onAnswer(q, response) {
  state.responses[q.questionId] = response;
  drawNav();
  clearTimeout(state.timers[q.questionId]);
  setSaveState('Đang lưu...');
  state.timers[q.questionId] = setTimeout(() => saveNow(q.questionId), 500);
}

async function saveNow(questionId) {
  clearTimeout(state.timers[questionId]);
  delete state.timers[questionId];
  state.saving.add(questionId);
  try {
    await API.exercises.saveAnswer(spaceId, exerciseId, state.attempt.attemptId, questionId, state.responses[questionId] || null);
    state.failed.delete(questionId);
  } catch (err) {
    state.failed.add(questionId);
    if (err.status === 409) {
      // Hết giờ / đã nộp ở nơi khác → tải lại để xem kết quả
      await reloadAttempt(friendlyErrorMessage(err));
      return;
    }
  } finally {
    state.saving.delete(questionId);
  }
  if (state.failed.size) setSaveState('Chưa lưu được một số câu, đang thử lại...', 'error');
  else if (!state.saving.size && !Object.keys(state.timers).length) setSaveState('Đã lưu tự động');
  if (state.failed.size && !state.retryTimer) {
    state.retryTimer = setTimeout(() => {
      state.retryTimer = null;
      [...state.failed].forEach((id) => saveNow(id));
    }, 3000);
  }
}

async function flushSaves() {
  const pending = [...new Set([...Object.keys(state.timers), ...state.failed])];
  await Promise.all(pending.map((id) => saveNow(id)));
}

async function confirmSubmit() {
  const left = state.attempt.questions.length - answeredCount();
  const ok = await confirmBox('Nộp bài?',
    left > 0 ? `Bạn còn ${left} câu chưa làm. Nộp rồi sẽ không sửa được đáp án nữa.` : 'Nộp rồi sẽ không sửa được đáp án nữa.',
    'Nộp bài');
  if (ok) submitNow();
}

async function submitNow(autoMessage = '') {
  if (state.submitting || !state.attempt) return;
  state.submitting = true;
  $('submitBtn').disabled = true;
  setSaveState('Đang nộp bài...');
  try {
    await flushSaves();
    state.attempt = await API.exercises.submit(spaceId, exerciseId, state.attempt.attemptId);
    stopTimer();
    renderResult();
    showToast(autoMessage || 'Nộp bài thành công.');
  } catch (err) {
    showToast(friendlyErrorMessage(err), 'error');
    setSaveState('Chưa nộp được, vui lòng thử lại.', 'error');
  } finally {
    state.submitting = false;
    $('submitBtn').disabled = false;
  }
}

async function reloadAttempt(message) {
  stopTimer();
  try {
    state.attempt = await API.exercises.attempt(spaceId, exerciseId, state.attempt.attemptId);
  } catch (err) {
    /* giữ dữ liệu cũ */
  }
  if (state.attempt.status === 'IN_PROGRESS') startTaking();
  else renderResult();
  if (message) showToast(message, 'error');
}

function startTimer() {
  stopTimer();
  if (state.deadlineMs === null) return;
  const update = () => {
    const left = Math.round((state.deadlineMs - Date.now()) / 1000);
    $('timerText').textContent = formatCountdown(left);
    $('timerBox').classList.toggle('text-error', left <= 60);
    if (left <= 0) {
      stopTimer();
      submitNow('Đã hết giờ làm bài, bài của bạn đã được tự nộp.');
    }
  };
  update();
  state.tick = setInterval(update, 1000);
}

function stopTimer() {
  if (state.tick) clearInterval(state.tick);
  state.tick = null;
}

// ============================================================ màn kết quả

function segmentsText(view) {
  const segs = view?.segments || [];
  return segs.map((s, i) => `${s}${i < segs.length - 1 ? `(${i + 1})` : ''}`).join('');
}

async function renderResult() {
  stopTimer();
  const a = state.attempt;
  setHeader({ sub: `Lượt ${a.attemptNumber}/${a.maxAttempts} · ${attemptStatusLabel(a.status)}` });
  let summary = state.summary;
  try {
    summary = (await API.exercises.detail(spaceId, exerciseId)).summary;
    state.summary = summary;
  } catch (err) {
    /* vẫn hiện kết quả */
  }
  const scoreLine = a.status === 'GRADED'
    ? `<p class="text-[28px] font-bold">${formatScore(a.finalScore)} <span class="text-[18px] text-on-surface-variant font-semibold">/ ${formatScore(a.maxScore)} điểm</span></p>`
    : `<p class="text-[18px] font-semibold">Chờ gia sư chấm câu tự luận</p>
       <p class="text-on-surface-variant">Phần hệ thống đã chấm: ${formatScore(a.autoScore)} điểm</p>`;
  const reason = a.submitReason === 'TIMEOUT' ? 'Bài được tự nộp khi hết giờ.'
    : a.submitReason === 'CLOSED' ? 'Bài được tự nộp khi đóng đề.' : '';
  $('takeMain').innerHTML = `
    <div class="max-w-3xl mx-auto space-y-5">
      <section class="kz-card p-6 space-y-2">
        <p class="font-label-md text-label-md text-on-surface-variant">Kết quả lượt ${a.attemptNumber}</p>
        ${scoreLine}
        <p class="font-caption text-caption text-on-surface-variant">Nộp lúc ${escapeHtml(formatDateTime(a.submittedAt))}${a.late ? ' · nộp muộn' : ''}${reason ? ` · ${reason}` : ''}</p>
        ${a.answersVisible ? '' : '<p class="text-on-surface-variant">Gia sư không cho xem đáp án đúng của bài này. Bạn vẫn xem được câu nào đúng, câu nào sai.</p>'}
        <div class="flex flex-wrap gap-3 pt-2">
          <button type="button" id="introBtn" class="kz-btn">Thông tin bài</button>
          ${summary?.canStart && !summary.inProgressAttemptId ? '<button type="button" id="againBtn" class="kz-btn kz-btn-primary">Làm lượt mới</button>' : ''}
          <a href="${classLink()}" class="kz-btn">Quay lại lớp học</a>
        </div>
      </section>
      <ol class="flex flex-col gap-3">
        ${a.questions.map((q, i) => {
          const pending = q.correct === null || q.correct === undefined;
          const tone = pending ? 'border-outline-variant' : (q.correct ? 'border-emerald-400' : 'border-red-300');
          const badge = pending && q.score === null ? '<span class="kz-badge">Chờ chấm</span>'
            : (q.correct ? '<span class="kz-badge" data-tone="active">Đúng</span>' : '<span class="kz-badge" data-tone="paused">Sai</span>');
          const correctText = a.answersVisible ? describeCorrect(q.type, q.view, q.correctAnswer) : '';
          return `
          <li class="kz-card p-5 border-l-4 ${tone} space-y-2">
            <div class="flex flex-wrap items-center gap-2">
              <span class="font-title-sm text-title-sm">Câu ${i + 1}</span>
              <span class="font-caption text-caption text-on-surface-variant">${escapeHtml(questionTypeLabel(q.type))}</span>
              ${badge}
              <span class="flex-1"></span>
              <span class="font-semibold">${q.score === null || q.score === undefined ? '…' : formatScore(q.score)} / ${formatScore(q.points)} điểm</span>
            </div>
            ${q.prompt ? `<p class="whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(q.prompt)}</p>` : ''}
            ${q.view?.segments ? `<p class="text-on-surface-variant [overflow-wrap:anywhere]">${escapeHtml(segmentsText(q.view))}</p>` : ''}
            <p class="[overflow-wrap:anywhere]"><span class="font-semibold">Câu trả lời của bạn:</span> ${escapeHtml(describeResponse(q.type, q.view, q.response))}</p>
            ${correctText ? `<p class="text-emerald-800 [overflow-wrap:anywhere]"><span class="font-semibold">Đáp án đúng:</span> ${escapeHtml(correctText)}</p>` : ''}
            ${!q.needsManualGrading && q.score !== null && q.score !== undefined && Number(q.score) !== Number(q.autoScore)
              ? `<p class="font-caption text-caption text-on-surface-variant">Gia sư đã sửa điểm câu này (hệ thống chấm ${formatScore(q.autoScore)} điểm).</p>` : ''}
            ${q.tutorComment ? `<p class="rounded-lg bg-surface-container-low px-3 py-2 [overflow-wrap:anywhere]"><span class="font-semibold">Nhận xét của gia sư:</span> ${escapeHtml(q.tutorComment)}</p>` : ''}
          </li>`;
        }).join('')}
      </ol>
    </div>`;
  $('introBtn').addEventListener('click', () => {
    const url = new URL(window.location.href);
    url.searchParams.delete('attempt');
    window.history.replaceState(null, '', url);
    showIntro();
  });
  $('againBtn')?.addEventListener('click', startAttempt);
}

// ============================================================ khởi tạo

function init() {
  const root = $('takeRoot');
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
  const role = getCurrentRole();
  if (role !== ROLE_LEARNER) {
    root.innerHTML = roleDeniedHtml('Làm bài trực tiếp', 'học viên', role);
    root.querySelector('[data-switch-account]')?.addEventListener('click', clearSessionAndGoToLogin);
    return;
  }
  if (!spaceId || !exerciseId) {
    $('takeMain').innerHTML = '<div class="kz-card p-6 text-center">Không tìm thấy bài tập.</div>';
    return;
  }
  $('backLink').href = classLink();
  $('backLink').addEventListener('click', async (e) => {
    if (state.attempt?.status !== 'IN_PROGRESS') return;
    e.preventDefault();
    const ok = await confirmBox('Rời khỏi bài làm?',
      'Câu trả lời đã được tự lưu. Đồng hồ vẫn chạy khi bạn rời trang; bạn có thể vào lại để làm tiếp.', 'Rời trang');
    if (ok) {
      await flushSaves();
      window.location.href = classLink();
    }
  });
  $('submitBtn').addEventListener('click', confirmSubmit);
  window.addEventListener('beforeunload', (e) => {
    if (state.attempt?.status === 'IN_PROGRESS' && (Object.keys(state.timers).length || state.failed.size)) {
      e.preventDefault();
      e.returnValue = '';
    }
  });
  const attemptId = params.get('attempt');
  if (attemptId) openAttempt(attemptId);
  else showIntro();
}

if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
else init();
