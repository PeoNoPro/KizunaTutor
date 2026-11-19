/**
 * KizunaTutor - FE-07 Học viên làm bài trực tiếp: /pages/exercise-take/index.html?space={spaceId}&exercise={exerciseId}[&attempt={attemptId}]
 * - Màn giới thiệu: thông tin bài, các lượt đã làm, nút Bắt đầu / Làm tiếp.
 * - Màn làm bài: bảng số câu, đồng hồ đếm ngược (theo giờ máy chủ), tự lưu từng câu, nộp có xác nhận, hết giờ tự nộp.
 *   Tắt trang giữa chừng thì vào lại làm tiếp đúng lượt đó (đồng hồ vẫn chạy).
 * - Màn kết quả: điểm, câu đúng / sai, đáp án đúng (nếu gia sư cho xem), nhận xét của gia sư.
 */

import { previousPage, pageLabel } from '../../js/back-nav.js';
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
import { mountAudioPlayer, playsKey } from '../../js/exercise-audio.js';
import { mountQuestionImage } from '../../js/exercise-image.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

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

/** Trang lớp, mở đúng tab Bài tập. */
function classLink() {
  return `/learner/my-courses?space=${encodeURIComponent(spaceId || '')}&tab=bai-tap`;
}

/** Trang vừa đứng trước khi vào làm bài (Bài tập của tôi, tab Bài tập của lớp…); không có thì về tab Bài tập của lớp. */
const PREV_PAGE = previousPage(classLink());

function showToast(message, tone = 'success') {
  const t = $('takeToast');
  t.textContent = message;
  t.style.background = tone === 'success' ? '#cce5ff' : '#ffdad6';
  t.style.color = tone === 'success' ? '#004b73' : '#93000a';
  t.classList.remove('hidden');
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => t.classList.add('hidden'), 3500);
}

/**
 * Thanh trên chỉ hiện khi đang làm bài (tên bài, tự lưu, đồng hồ, nộp bài) và lúc đó ẩn sidebar học viên để tập trung.
 * Màn giới thiệu / kết quả dùng sidebar + đường dẫn như các trang học viên khác.
 */
function setHeader({ sub = '', taking = false } = {}) {
  const title = state.summary?.title || state.attempt?.exerciseTitle || 'Bài làm trực tiếp';
  $('takeTitle').textContent = title;
  document.title = `${title} - KizunaTutor`;
  $('takeSub').textContent = [className(), sub].filter(Boolean).join(' · ');
  $('takeBar').classList.toggle('hidden', !taking);
  document.body.classList.toggle('ex-taking', taking);
  $('submitBtn').classList.toggle('hidden', !taking);
  $('timerBox').classList.toggle('hidden', !taking || state.deadlineMs === null);
  $('saveState').classList.toggle('hidden', !taking);
}

/** "Lớp N4 · Yamada Kenji" (tải kèm khi mở bài; thiếu thì để trống). */
function className() {
  const c = state.classInfo;
  if (!c) return '';
  return [c.title, c.tutorName ? `Gia sư ${c.tutorName.replace(/\s*\(Gia Sư\)\s*$/i, '')}` : ''].filter(Boolean).join(' · ');
}

/** Đường dẫn trên đầu màn giới thiệu / kết quả. */
function breadcrumbHtml(last, { linkToIntro = false } = {}) {
  const title = state.summary?.title || state.attempt?.exerciseTitle || 'Bài làm trực tiếp';
  const sep = '<span class="material-symbols-outlined text-[16px]" aria-hidden="true">chevron_right</span>';
  return `
    <a href="${escapeHtml(PREV_PAGE)}" class="inline-flex items-center gap-1 mb-1.5 font-label-md text-label-md text-primary hover:underline">
      <span class="material-symbols-outlined text-[18px]" aria-hidden="true">arrow_back</span>Quay lại ${escapeHtml(pageLabel(PREV_PAGE))}</a>
    <nav aria-label="Đường dẫn" class="flex flex-wrap items-center gap-1 font-body-sm text-body-sm text-on-surface-variant">
      <a href="/learner/my-courses" class="text-primary hover:underline">Lớp học của tôi</a>${sep}
      <a href="${classLink()}" class="text-primary hover:underline">${escapeHtml(state.classInfo?.title || 'Lớp học')}</a>${sep}
      ${linkToIntro ? `<button type="button" data-crumb-intro class="text-primary hover:underline">${escapeHtml(title)}</button>${sep}<span>${escapeHtml(last)}</span>`
        : `<span class="[overflow-wrap:anywhere]">${escapeHtml(last || title)}</span>`}
    </nav>`;
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
      API.exercises.attempts(spaceId, exerciseId).catch(() => []),
      loadClassInfo()
    ]);
    state.summary = detail.summary;
    state.instructions = detail.instructions || '';
    renderIntro(attempts || []);
  } catch (err) {
    main.innerHTML = `<div class="kz-card p-6 text-center space-y-3"><p>${escapeHtml(friendlyErrorMessage(err))}</p>
      <a href="${classLink()}" class="kz-btn">Quay lại lớp học</a></div>`;
  }
}

/** Tên lớp + gia sư cho đường dẫn và thanh trên (lỗi thì bỏ qua). */
async function loadClassInfo() {
  if (state.classInfo) return state.classInfo;
  try {
    state.classInfo = await API.classrooms.detail(spaceId);
  } catch (err) {
    state.classInfo = null;
  }
  return state.classInfo;
}

const STATE_TEXT = { OPEN: 'Đang mở', NOT_OPEN: 'Chưa mở', CLOSED: 'Đã đóng', DRAFT: 'Nháp' };

function renderIntro(attempts) {
  const s = state.summary;
  setHeader();
  // Có câu nghe: biết được qua các lượt đã làm (đề chỉ gửi cho học viên khi bắt đầu làm)
  const listening = (attempts.find((a) => a.questions?.length)?.questions || []).filter((q) => q.audio);
  const tiles = [
    ['help', 'Câu hỏi', `${s.questionCount} câu · ${formatScore(s.maxScore)} điểm`],
    ['timer', 'Thời gian làm bài', s.timeLimitMinutes ? `${s.timeLimitMinutes} phút` : 'Không giới hạn'],
    ['replay', 'Lượt làm', `Đã làm ${s.attemptsUsed}/${s.maxAttempts} lượt`],
    ['event', 'Đóng đề', s.closesAt ? formatDateTime(s.closesAt) : 'Không đóng đề'],
    ['fact_check', 'Cách chấm', gradingModeLabel(s.gradingMode)]
  ];
  if (listening.length) {
    const plays = listening.map((q) => q.audio.maxPlays).filter(Boolean);
    tiles.push(['headphones', 'Có câu nghe', `${listening.length} câu${plays.length ? ` · nghe tối đa ${Math.min(...plays)} lần` : ''}`, true]);
  }
  const inProgress = s.inProgressAttemptId;
  const btn = (label) => `<button type="button" id="startBtn" class="kz-btn kz-btn-primary h-12 px-6 text-[15px]"><span class="material-symbols-outlined text-[20px]" aria-hidden="true">play_arrow</span>${label}</button>`;
  let action = '';
  if (inProgress) action = btn('Làm tiếp');
  else if (s.canStart) action = btn(s.attemptsUsed ? `Bắt đầu lượt ${s.attemptsUsed + 1}` : 'Bắt đầu làm bài');
  else if (s.state === 'CLOSED') action = '<p class="font-semibold text-on-surface-variant">Đề đã đóng.</p>';
  else if (s.attemptsUsed >= s.maxAttempts) action = '<p class="font-semibold text-on-surface-variant">Bạn đã dùng hết lượt làm bài.</p>';
  else action = '<p class="font-semibold text-on-surface-variant">Hiện chưa thể làm bài này.</p>';
  const stateTone = s.state === 'OPEN' ? 'active' : (s.state === 'CLOSED' ? 'done' : 'paused');

  $('takeMain').innerHTML = `
    <div class="max-w-4xl mx-auto space-y-5">
      ${breadcrumbHtml(s.title)}
      <section class="kz-card p-6 md:p-7 space-y-5">
        <div class="flex flex-wrap items-center gap-3">
          <div class="w-12 h-12 rounded-xl bg-primary-fixed text-on-primary-fixed-variant flex items-center justify-center"><span class="material-symbols-outlined text-[26px]" aria-hidden="true">quiz</span></div>
          <div class="flex-1 min-w-[220px]">
            <h1 class="font-headline-sm text-headline-sm text-on-surface [overflow-wrap:anywhere]">${escapeHtml(s.title)}</h1>
            <p class="text-on-surface-variant">Bài làm trực tiếp${state.classInfo?.tutorName ? ` · ${escapeHtml(className().split(' · ').slice(1).join(' · '))}` : ''}</p>
          </div>
          <span class="kz-badge" data-tone="${stateTone}">${escapeHtml(STATE_TEXT[s.state] || s.state)}</span>
        </div>
        <ul class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          ${tiles.map(([icon, label, value, accent]) => `
            <li class="ex-tile ${accent ? '!bg-secondary-fixed text-on-secondary-container' : ''}">
              <p class="flex items-center gap-2 font-body-sm text-body-sm ${accent ? '' : 'text-on-surface-variant'}"><span class="material-symbols-outlined text-[18px]" aria-hidden="true">${icon}</span>${escapeHtml(label)}</p>
              <p class="font-title-md text-title-md mt-1 [overflow-wrap:anywhere]">${escapeHtml(value)}</p>
            </li>`).join('')}
        </ul>
        ${state.instructions ? `<div class="rounded-xl border border-surface-container-high px-4 py-3"><p class="font-semibold mb-1">Hướng dẫn của gia sư</p><p class="whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(state.instructions)}</p></div>` : ''}
        ${s.pendingGrading ? '<p class="rounded-xl bg-tertiary-fixed text-on-tertiary-fixed px-4 py-2.5">Có lượt đang chờ gia sư chấm câu tự luận.</p>' : ''}
        <div class="flex flex-wrap items-center gap-3 pt-1">
          <p class="flex-1 min-w-[200px] text-on-surface-variant">${s.bestScore !== null && s.bestScore !== undefined ? `Điểm cao nhất của bạn: <b class="text-on-surface text-[16px]">${formatScore(s.bestScore)} / ${formatScore(s.maxScore)}</b>` : (s.timeLimitMinutes && !inProgress && s.canStart ? 'Đồng hồ bắt đầu chạy khi bạn bấm bắt đầu và vẫn chạy nếu bạn rời trang. Hết giờ bài sẽ tự nộp.' : '')}</p>
          ${action}
        </div>
      </section>
      ${attempts.length ? `
        <section class="kz-card p-6 space-y-3">
          <h2 class="font-title-md text-title-md">Các lượt đã làm</h2>
          <ul class="flex flex-col gap-2">
            ${attempts.map((a) => `
              <li class="kz-item flex flex-wrap items-center gap-3">
                <span class="font-semibold">Lượt ${a.attemptNumber}</span>
                <span class="kz-badge" data-tone="${a.status === 'GRADED' ? 'active' : a.status === 'SUBMITTED' ? 'paused' : 'done'}">${escapeHtml(attemptStatusLabel(a.status))}</span>
                ${a.status !== 'IN_PROGRESS' ? `<span class="text-on-surface-variant">Nộp lúc ${escapeHtml(formatDateTime(a.submittedAt))}${a.late ? ' · nộp muộn' : ''}</span>` : ''}
                <span class="flex-1"></span>
                ${a.finalScore !== null && a.finalScore !== undefined ? `<span class="font-semibold text-[16px]">${formatScore(a.finalScore)} / ${formatScore(a.maxScore)} điểm</span>` : ''}
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
    const [attempt] = await Promise.all([API.exercises.attempt(spaceId, exerciseId, attemptId), loadClassInfo()]);
    enterAttempt(attempt);
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
      <aside class="kz-card p-4 h-fit lg:sticky lg:top-24 space-y-3">
        <p class="font-label-md text-label-md">Câu hỏi · đã làm <span id="answeredCount">${answeredCount()}</span>/${total}</p>
        <div class="grid grid-cols-5 gap-2" id="navGrid"></div>
        <div class="flex flex-wrap gap-3 font-caption text-caption text-on-surface-variant">
          <span class="flex items-center gap-1"><span class="w-3 h-3 rounded bg-primary inline-block"></span>Đã làm</span>
          <span class="flex items-center gap-1"><span class="w-3 h-3 rounded border border-outline-variant inline-block"></span>Chưa làm</span>
          ${a.questions.some((x) => x.audio) ? '<span class="flex items-center gap-1"><span class="material-symbols-outlined text-[14px] text-secondary" aria-hidden="true">headphones</span>Câu nghe</span>' : ''}
        </div>
      </aside>
      <section class="kz-card p-6 md:p-7 space-y-4 min-w-0">
        <div class="flex flex-wrap items-center gap-2">
          <span class="font-title-sm text-title-sm">Câu ${state.current + 1}/${total}</span>
          <span class="kz-badge">${escapeHtml(questionTypeLabel(q.type))}</span>
          ${q.audio ? '<span class="kz-badge" style="background:#dce1ff;color:#1d3989"><span class="material-symbols-outlined text-[15px]" aria-hidden="true">headphones</span>Câu nghe</span>' : ''}
          <span class="font-caption text-caption text-on-surface-variant">${formatScore(q.points)} điểm</span>
        </div>
        ${q.audio ? '<div id="questionAudio"></div>' : ''}
        ${q.prompt ? `<p class="text-[16px] whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(q.prompt)}</p>` : ''}
        ${q.image ? '<div id="questionImage"></div>' : ''}
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
  if (q.audio) mountAudioPlayer($('questionAudio'), spaceId, q.audio, { limitKey: playsKey(a.attemptId, q.questionId) });
  if (q.image) mountQuestionImage($('questionImage'), spaceId, q.image, { maxHeight: 420 });
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
    return `<button type="button" data-go="${i}" aria-label="Câu ${i + 1}${q.audio ? ' (câu nghe)' : ''}"
      class="relative h-9 rounded-lg text-[13px] font-bold border ${done ? 'bg-primary text-on-primary border-primary' : 'border-outline-variant text-on-surface'} ${cur ? 'ring-2 ring-offset-2 ring-primary' : ''}">${i + 1}${q.audio ? '<span class="material-symbols-outlined absolute -top-2 -right-2 text-[13px] leading-none p-0.5 rounded-full bg-secondary-fixed text-on-secondary-container" aria-hidden="true">headphones</span>' : ''}</button>`;
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
  const graded = a.status === 'GRADED';
  const percent = graded && Number(a.maxScore) > 0 ? Math.round((Number(a.finalScore) / Number(a.maxScore)) * 100) : null;
  // Trạng thái từng câu: chờ gia sư chấm / gia sư đã chấm (câu tự luận) / đúng / sai (máy chấm)
  const kinds = a.questions.map((q) => {
    const manualPending = q.needsManualGrading && (q.score === null || q.score === undefined);
    if (manualPending) return 'pending';
    if (q.correct === null || q.correct === undefined) return 'tutor';
    return q.correct ? 'right' : 'wrong';
  });
  const n = (k) => kinds.filter((x) => x === k).length;
  const counts = [
    n('right') ? `${n('right')} câu đúng` : '', n('wrong') ? `${n('wrong')} câu sai` : '',
    n('tutor') ? `${n('tutor')} câu gia sư đã chấm` : '', n('pending') ? `${n('pending')} câu chờ chấm` : ''
  ].filter(Boolean).join(' · ');
  const reason = a.submitReason === 'TIMEOUT' ? 'tự nộp khi hết giờ'
    : a.submitReason === 'CLOSED' ? 'tự nộp khi đóng đề' : '';
  const commented = a.questions.filter((q) => q.tutorComment).length;
  const ring = percent === null
    ? '<div class="w-24 h-24 rounded-full border-8 border-tertiary-fixed flex items-center justify-center shrink-0"><span class="material-symbols-outlined text-[30px] text-tertiary" aria-hidden="true">hourglass_top</span></div>'
    : `<div class="w-24 h-24 rounded-full flex items-center justify-center shrink-0" style="background: conic-gradient(#006194 ${percent * 3.6}deg, #dae2fd 0deg)"><div class="w-[72px] h-[72px] rounded-full bg-surface-container-lowest flex items-center justify-center font-bold text-[20px]">${percent}%</div></div>`;
  const scoreLine = graded
    ? `<p class="text-[30px] font-bold leading-tight">${formatScore(a.finalScore)} <span class="text-[18px] text-on-surface-variant font-semibold">/ ${formatScore(a.maxScore)} điểm</span></p>`
    : `<p class="text-[20px] font-semibold">Chờ gia sư chấm câu tự luận</p>
       <p class="text-on-surface-variant">Phần hệ thống đã chấm: ${formatScore(a.autoScore)} điểm</p>`;
  $('takeMain').innerHTML = `
    <div class="max-w-4xl mx-auto space-y-5">
      ${breadcrumbHtml(`Kết quả lượt ${a.attemptNumber}`, { linkToIntro: true })}
      <section class="kz-card p-6 flex flex-wrap items-center gap-5">
        ${ring}
        <div class="flex-1 min-w-[240px] space-y-1">
          <p class="font-label-md text-label-md text-on-surface-variant">Kết quả lượt ${a.attemptNumber} · ${escapeHtml(summary?.title || a.exerciseTitle || '')}</p>
          ${scoreLine}
          <p class="font-body-sm text-body-sm text-on-surface-variant">Nộp ${escapeHtml(formatDateTime(a.submittedAt))}${a.late ? ' · nộp muộn' : ''}${reason ? ` · ${reason}` : ''}${counts ? ` · ${counts}` : ''}</p>
          ${a.answersVisible ? '' : '<p class="font-body-sm text-body-sm text-on-surface-variant">Gia sư không cho xem đáp án đúng của bài này. Bạn vẫn xem được câu nào đúng, câu nào sai.</p>'}
        </div>
        <div class="flex flex-wrap gap-2">
          <button type="button" id="introBtn" class="kz-btn h-11 px-4">Về trang bài tập</button>
          ${summary?.canStart && !summary.inProgressAttemptId ? `<button type="button" id="againBtn" class="kz-btn kz-btn-primary h-11 px-5">Làm lượt ${(summary.attemptsUsed || 0) + 1}</button>` : ''}
        </div>
      </section>
      <div class="flex flex-wrap gap-2" role="group" aria-label="Lọc câu">
        <button type="button" class="ex-chip" data-filter="all" data-active="true">Tất cả (${a.questions.length})</button>
        ${n('wrong') ? `<button type="button" class="ex-chip" data-filter="wrong" data-active="false">Câu sai (${n('wrong')})</button>` : ''}
        ${commented ? `<button type="button" class="ex-chip" data-filter="comment" data-active="false">Có nhận xét (${commented})</button>` : ''}
      </div>
      <ol class="flex flex-col gap-3">
        ${a.questions.map((q, i) => {
          const tone = { pending: 'border-l-[#e8a35c]', tutor: 'border-l-[#006194]', right: 'border-l-[#4b9d6c]', wrong: 'border-l-[#e37b7b]' }[kinds[i]];
          const badge = {
            pending: '<span class="kz-badge" data-tone="paused">Chờ gia sư chấm</span>',
            tutor: '<span class="kz-badge" data-tone="active"><span class="material-symbols-outlined text-[15px]" aria-hidden="true">fact_check</span>Gia sư đã chấm</span>',
            right: '<span class="kz-badge" style="background:#d4f0dd;color:#1b6b3a">Đúng</span>',
            wrong: '<span class="kz-badge" style="background:#ffdad6;color:#93000a">Sai</span>'
          }[kinds[i]];
          const correctText = a.answersVisible ? describeCorrect(q.type, q.view, q.correctAnswer) : '';
          return `
          <li class="kz-card p-5 border-l-4 ${tone} space-y-2" data-kind="${kinds[i]}" data-comment="${q.tutorComment ? 'true' : 'false'}">
            <div class="flex flex-wrap items-center gap-2">
              <span class="font-title-sm text-title-sm">Câu ${i + 1}</span>
              <span class="font-caption text-caption text-on-surface-variant">${escapeHtml(questionTypeLabel(q.type))}</span>
              ${q.audio ? '<span class="kz-badge" style="background:#dce1ff;color:#1d3989"><span class="material-symbols-outlined text-[15px]" aria-hidden="true">headphones</span>Câu nghe</span>' : ''}
              ${badge}
              <span class="flex-1"></span>
              <span class="font-semibold">${q.score === null || q.score === undefined ? '…' : formatScore(q.score)} / ${formatScore(q.points)} điểm</span>
            </div>
            ${q.prompt ? `<p class="whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(q.prompt)}</p>` : ''}
            ${q.image ? `<div data-result-image="${i}"></div>` : ''}
            ${q.audio ? `<div data-result-audio="${i}"></div>` : ''}
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
  // Xem lại kết quả: nghe lại không giới hạn
  a.questions.forEach((q, i) => {
    if (q.audio) mountAudioPlayer($('takeMain').querySelector(`[data-result-audio="${i}"]`), spaceId, q.audio);
    if (q.image) mountQuestionImage($('takeMain').querySelector(`[data-result-image="${i}"]`), spaceId, q.image, { maxHeight: 240 });
  });
  const main = $('takeMain');
  main.querySelectorAll('[data-filter]').forEach((b) => b.addEventListener('click', () => {
    const f = b.dataset.filter;
    main.querySelectorAll('[data-filter]').forEach((x) => { x.dataset.active = String(x === b); });
    main.querySelectorAll('[data-kind]').forEach((li) => {
      const show = f === 'all' || (f === 'wrong' && li.dataset.kind === 'wrong') || (f === 'comment' && li.dataset.comment === 'true');
      li.classList.toggle('hidden', !show);
    });
  }));
  const backToIntro = () => {
    const url = new URL(window.location.href);
    url.searchParams.delete('attempt');
    window.history.replaceState(null, '', url);
    showIntro();
  };
  $('introBtn').addEventListener('click', backToIntro);
  main.querySelector('[data-crumb-intro]')?.addEventListener('click', backToIntro);
  $('againBtn')?.addEventListener('click', startAttempt);
}

// ============================================================ khởi tạo

/**
 * Sidebar dùng chung của học viên với mục "Lớp học của tôi" đang mở.
 * Chỗ đặt sidebar mang tên riêng để learner-layout không tự gắn theo địa chỉ trang (sẽ sáng nhầm "Tổng quan");
 * đổi sang tên chuẩn rồi mới gọi initLearnerShell với mục cần sáng.
 */
function mountLearnerSidebar() {
  const sidebarSlot = $('exLearnerSidebarSlot');
  const headerSlot = $('exLearnerHeaderSlot');
  if (!sidebarSlot) return;
  sidebarSlot.id = 'learnerSidebarContainer';
  if (headerSlot) headerSlot.id = 'learnerHeaderContainer';
  try {
    initLearnerShell({ activeMenu: 'my-courses', pageTitle: 'Lớp học của tôi', badge: 'Gia sư & lớp học' });
  } catch (err) {
    console.debug('Không gắn được sidebar học viên:', err);
  }
}

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
  mountLearnerSidebar();
  if (!spaceId || !exerciseId) {
    $('takeMain').innerHTML = '<div class="kz-card p-6 text-center">Không tìm thấy bài tập.</div>';
    return;
  }
  $('backLink').href = PREV_PAGE;
  $('backLink').title = `Về ${pageLabel(PREV_PAGE)} (bài vẫn được lưu, đồng hồ vẫn chạy)`;
  $('backLink').addEventListener('click', async (e) => {
    if (state.attempt?.status !== 'IN_PROGRESS') return;
    e.preventDefault();
    const ok = await confirmBox('Rời khỏi bài làm?',
      'Câu trả lời đã được tự lưu. Đồng hồ vẫn chạy khi bạn rời trang; bạn có thể vào lại để làm tiếp.', 'Rời trang');
    if (ok) {
      await flushSaves();
      window.location.href = PREV_PAGE;
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
