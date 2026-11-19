/**
 * KizunaTutor - FE-07 Gia sư soạn bài làm trực tiếp: /pages/exercise-editor/index.html?space={spaceId}[&exercise={exerciseId}][&tab=bai-lam]
 * - Cài đặt: tiêu đề, hướng dẫn, mở / đóng đề, thời gian làm bài (hoặc không giới hạn), số lượt làm, cách chấm, cho xem đáp án.
 * - Soạn câu hỏi: 8 dạng câu, đổi thứ tự, nhân bản, xóa; mỗi câu có đáp án và điểm.
 * - Lưu nháp (học viên không thấy), Xem thử (như học viên, không lưu), Giao bài (kiểm tra đủ đáp án).
 * - Đã có học viên làm bài: khóa câu hỏi, đáp án, thời gian làm bài, cách chấm; vẫn sửa được phần còn lại.
 * - Tab "Bài làm": xem từng lượt, sửa điểm từng câu, chấm câu tự luận, nhận xét.
 */

import { previousPage, pageLabel } from '../../js/back-nav.js';
import { API } from '../../js/api.js';
import {
  escapeHtml,
  friendlyErrorMessage,
  getCurrentRole,
  ROLE_TUTOR,
  roleDeniedHtml,
  clearSessionAndGoToLogin
} from '../../js/classroom-helpers.js';
import {
  QUESTION_TYPES,
  questionTypeLabel,
  questionTypeHint,
  exerciseStateLabel,
  exerciseStateTone,
  attemptStatusLabel,
  formatDateTime,
  formatScore,
  countBlanks,
  splitList,
  defaultQuestion,
  validateQuestion,
  isTutorGraded,
  upgradeWordBank,
  needsAudio,
  AUDIO_MISSING_MESSAGE,
  questionPayload,
  totalPoints,
  previewView,
  describeResponse,
  describeCorrect,
  toLocalInput,
  fromLocalInput,
  validateExerciseSettings,
  legacyWrittenAnswers
} from '../../js/exercise-helpers.js';
import { renderInteractive } from '../../js/exercise-player.js';
import { mountTutorLayout } from '/components/tutor-layout/tutor-layout.js';
import {
  AUDIO_ACCEPT,
  AUDIO_RULES_TEXT,
  MAX_PLAYS_OPTIONS,
  validateAudioFile,
  playLimitLabel,
  mountAudioPlayer
} from '../../js/exercise-audio.js';
import { IMAGE_ACCEPT, IMAGE_RULES_TEXT, validateImageFile, mountQuestionImage } from '../../js/exercise-image.js';

const $ = (id) => document.getElementById(id);
const params = new URLSearchParams(window.location.search);
const spaceId = params.get('space');

const state = {
  exerciseId: params.get('exercise'),
  detail: null,
  questions: [],
  locked: false,
  status: 'NEW',
  attempts: [],
  selectedAttempt: null,
  // Tab: questions (Câu hỏi) | settings (Cài đặt bài) | attempts (Bài làm của học viên)
  tab: ({ 'bai-lam': 'attempts', 'cai-dat': 'settings' })[params.get('tab')] || 'questions',
  selected: 0,
  gradeFilter: 'pending',
  saving: false,
  _dirty: false,
  // Có thay đổi chưa lưu: cập nhật luôn thanh dưới (bài đã giao chỉ bật nút "Cập nhật cho học viên" khi có thay đổi)
  get dirty() { return this._dirty; },
  set dirty(v) {
    this._dirty = !!v;
    updateSaveState();
  }
};

/** Thanh dưới của bài đã giao: trạng thái lưu + nút "Cập nhật cho học viên" (mờ khi chưa sửa gì). */
function updateSaveState() {
  const box = document.getElementById('saveState');
  const btn = document.getElementById('publishBtn');
  if (!box || !btn) return;
  const published = state.status === 'PUBLISHED';
  box.classList.toggle('hidden', !published);
  box.classList.toggle('flex', published);
  if (!published) {
    btn.disabled = state.saving;
    return;
  }
  box.innerHTML = state.dirty
    ? '<span class="w-2 h-2 rounded-full bg-tertiary" aria-hidden="true"></span><span class="text-on-surface">Có thay đổi chưa gửi cho học viên</span>'
    : '<span class="material-symbols-outlined text-[18px] text-primary" aria-hidden="true">check_circle</span><span class="text-on-surface-variant">Đang giao cho học viên · đã lưu hết</span>';
  btn.disabled = state.saving || !state.dirty;
  btn.title = state.dirty ? 'Gửi các thay đổi tới học viên' : 'Chưa có thay đổi nào để cập nhật';
}

/** Biểu tượng cho từng dạng câu (nút thêm câu, danh sách câu). */
const TYPE_ICONS = {
  SINGLE_CHOICE: 'radio_button_checked',
  MULTI_CHOICE: 'check_box',
  TRUE_FALSE: 'rule',
  FILL_BLANK: 'edit_note',
  WORD_BANK: 'dataset',
  MATCHING: 'compare_arrows',
  ORDERING: 'format_list_numbered',
  WRITTEN: 'notes'
};

const INPUT = 'kz-input';
const SMALL_BTN = 'kz-btn px-2.5 py-1.5';

/** Trang lớp, mở đúng tab Bài tập. */
function classLink() {
  return `/tutor/classrooms?space=${encodeURIComponent(spaceId || '')}&tab=bai-tap`;
}

/** Trang vừa đứng trước khi mở trang soạn bài (không có thì về tab Bài tập của lớp). */
const PREV_PAGE = previousPage(spaceId ? classLink() : '/tutor/classrooms?view=assignments');

/** Đường dẫn: tên lớp thật thay cho chữ "Lớp học". */
async function fillClassCrumb() {
  if (!spaceId) return;
  try {
    const c = await API.classrooms.detail(spaceId);
    if (c?.title) $('backLink').textContent = c.title;
  } catch (e) {
    // giữ chữ "Lớp học"
  }
}

function showToast(message, tone = 'success') {
  const t = $('editorToast');
  t.textContent = message;
  t.style.background = tone === 'success' ? '#cce5ff' : '#ffdad6';
  t.style.color = tone === 'success' ? '#004b73' : '#93000a';
  t.classList.remove('hidden');
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => t.classList.add('hidden'), 3500);
}

function setFieldError(id, message) {
  const el = $(id);
  if (!el) return;
  el.textContent = message || '';
  el.classList.toggle('hidden', !message);
}

function confirmBox(title, message, okText, danger = false) {
  return new Promise((resolve) => {
    $('confirmTitle').textContent = title;
    $('confirmText').textContent = message;
    $('confirmOk').textContent = okText;
    $('confirmOk').className = `kz-btn ${danger ? 'kz-btn-danger' : 'kz-btn-primary'}`;
    $('confirmModal').classList.remove('hidden');
    const done = (v) => {
      $('confirmModal').classList.add('hidden');
      resolve(v);
    };
    $('confirmOk').onclick = () => done(true);
    $('confirmCancel').onclick = () => done(false);
  });
}

// ============================================================ tải dữ liệu

async function load() {
  if (!state.exerciseId) {
    state.status = 'NEW';
    state.questions = [defaultQuestion('SINGLE_CHOICE')];
    fillSettings(null);
    await prefillNewExercise();
    $('loadingBox').classList.add('hidden');
    renderAll();
    // Mở từ hộp thoại "Giao bài tập" với nguồn "Nhập file Excel"
    if (params.get('import') === '1') {
      clearParams(['import']);
      openImport();
    }
    return;
  }
  try {
    // Mở thẳng tab "Bài làm": lấy thông tin bài và các lượt làm cùng lúc để không phải chờ hai lần
    const [detail, attempts] = await Promise.all([
      API.exercises.detail(spaceId, state.exerciseId),
      state.tab === 'attempts' ? API.exercises.attempts(spaceId, state.exerciseId).catch(() => null) : Promise.resolve(null)
    ]);
    state.detail = detail;
    if (attempts) state.attempts = attempts;
    state.attemptsPreloaded = !!attempts;
    const s = state.detail.summary;
    state.status = s.status;
    state.locked = s.locked;
    state.questions = (state.detail.questions || []).map((q) => ({
      type: q.type, prompt: q.prompt || '', points: Number(q.points), data: q.type === 'WORD_BANK' ? upgradeWordBank(q.data) : (q.data || {}), audio: q.audio || null,
      image: q.image || null,
      listening: q.listening ? { maxPlays: q.listening.maxPlays ?? null } : null
    }));
    fillSettings(state.detail);
    $('loadingBox').classList.add('hidden');
    renderAll();
    if (s.locked && !state.attemptsPreloaded) loadAttempts();
    state.attemptsPreloaded = false;
  } catch (err) {
    $('loadingBox').classList.add('hidden');
    $('editorMain').innerHTML = `<div class="kz-card p-6 text-center space-y-3"><p>${escapeHtml(friendlyErrorMessage(err))}</p>
      <a class="kz-btn" href="${classLink()}">Quay lại lớp học</a></div>`;
  }
}

/** Bỏ tham số một lần (import, copyFrom…) khỏi địa chỉ trang để tải lại không chạy lại. */
function clearParams(keys) {
  const url = new URL(window.location.href);
  keys.forEach((k) => url.searchParams.delete(k));
  window.history.replaceState(null, '', url);
}

/**
 * Bài mới mở từ hộp thoại "Giao bài tập" ở trang Quản lý bài tập:
 * - ?closes=ISO: điền sẵn giờ đóng đề (hạn nộp đã chọn);
 * - ?copyFrom=<exerciseId>&fromSpace=<spaceId>: sao chép câu hỏi + cài đặt của bài đã có thành bài nháp mới
 *   (không chép giờ mở / đóng đề và file nghe — file nghe thuộc bài cũ, cần tải lại).
 */
async function prefillNewExercise() {
  const copyFrom = params.get('copyFrom');
  if (copyFrom) {
    try {
      const src = await API.exercises.detail(params.get('fromSpace') || spaceId, copyFrom);
      const s = src.summary;
      const withAudio = (src.questions || []).filter((q) => q.audio).length;
      const withImage = (src.questions || []).filter((q) => q.image).length;
      state.questions = (src.questions || []).map((q) => ({
        type: q.type, prompt: q.prompt || '', points: Number(q.points), data: q.type === 'WORD_BANK' ? upgradeWordBank(q.data) : (q.data || {}), audio: null, image: null,
        listening: q.listening || q.audio ? { maxPlays: q.listening?.maxPlays ?? q.audio?.maxPlays ?? null } : null
      }));
      if (!state.questions.length) state.questions = [defaultQuestion('SINGLE_CHOICE')];
      fillSettings({ summary: { ...s, title: `${s.title} (bản sao)`.slice(0, 150), opensAt: null, closesAt: null }, instructions: src.instructions });
      state.dirty = true;
      const notes = [
        withAudio ? `${withAudio} câu có file nghe: đã đánh dấu "thiếu file nghe", hãy tải lại file nghe` : '',
        withImage ? `${withImage} câu có ảnh: ảnh không được sao chép, hãy thêm lại ảnh nếu cần` : ''
      ].filter(Boolean);
      showToast(notes.length
        ? `Đã sao chép ${state.questions.length} câu. ${notes.join('; ')}.`
        : `Đã sao chép ${state.questions.length} câu từ "${s.title}". Kiểm tra lại rồi lưu / giao bài.`);
    } catch (err) {
      showToast(`Không sao chép được bài: ${friendlyErrorMessage(err)}`, 'error');
    }
  }
  const closes = params.get('closes');
  if (closes) {
    $('exCloses').value = toLocalInput(closes);
    state.dirty = true;
  }
  if (copyFrom || closes) clearParams(['copyFrom', 'fromSpace', 'closes']);
}

function fillSettings(detail) {
  const s = detail?.summary;
  $('exTitle').value = s?.title || '';
  $('exInstructions').value = detail?.instructions || '';
  $('exOpens').value = toLocalInput(s?.opensAt);
  $('exCloses').value = toLocalInput(s?.closesAt);
  $('exUnlimited').checked = s ? !s.timeLimitMinutes : false;
  $('exTimeLimit').value = s?.timeLimitMinutes || (s ? '' : 30);
  $('exAttempts').value = s?.maxAttempts || 1;
  $('exShowAnswers').checked = s ? s.showAnswers : true;
}

/** Cách chấm của bài suy ra từ câu hỏi: có câu tự luận / điền khuyết → "AI gợi ý, gia sư duyệt" (TUTOR), còn lại máy chấm (AUTO). */
function autoMode() {
  return !state.questions.some(isTutorGraded);
}

// ============================================================ vẽ trang

function renderAll() {
  const s = state.detail?.summary;
  const title = s?.title || 'Soạn bài làm trực tiếp';
  $('pageTitle').textContent = title;
  $('crumbTitle').textContent = s?.title || 'Bài mới';
  document.title = `${title} - KizunaTutor`;
  const badge = $('statusBadge');
  if (s) {
    badge.textContent = exerciseStateLabel(s.state);
    badge.dataset.tone = exerciseStateTone(s.state);
    badge.classList.remove('hidden');
  } else badge.classList.add('hidden');
  const published = state.status === 'PUBLISHED';
  $('saveDraftBtn').classList.toggle('hidden', published);
  $('publishBtn').textContent = published ? 'Cập nhật cho học viên' : 'Giao bài';
  updateSaveState();
  $('deleteBtn').classList.toggle('hidden', !state.exerciseId || state.locked);
  $('lockedNote').classList.toggle('hidden', !state.locked);
  $('exUnlimited').disabled = state.locked;
  $('exTimeLimit').disabled = state.locked || $('exUnlimited').checked;
  $('addQuestionBar').classList.toggle('hidden', state.locked);
  $('tabAttempts').classList.toggle('hidden', !state.exerciseId);
  renderTabLabels();
  switchTab(state.tab);
  renderQuestions();
}

function renderTabLabels() {
  $('tabQuestions').textContent = `Câu hỏi (${state.questions.length})`;
  $('tabAttempts').textContent = `Bài làm của học viên${state.attempts.length ? ` (${state.attempts.length})` : ''}`;
}

/** Đổi tab và ghi lại trên địa chỉ trang (?tab=cai-dat | bai-lam) để tải lại vẫn đúng tab. */
function switchTab(tab) {
  state.tab = tab;
  const editing = tab !== 'attempts';
  $('editPanel').classList.toggle('hidden', !editing);
  $('questionsPanel').classList.toggle('hidden', tab !== 'questions');
  $('settingsPanel').classList.toggle('hidden', tab !== 'settings');
  $('attemptsPanel').classList.toggle('hidden', editing);
  $('tabQuestions').dataset.active = String(tab === 'questions');
  $('tabSettings').dataset.active = String(tab === 'settings');
  $('tabAttempts').dataset.active = String(tab === 'attempts');
  $('editorActions').classList.toggle('hidden', !editing);
  const url = new URL(window.location.href);
  const param = { attempts: 'bai-lam', settings: 'cai-dat' }[tab];
  if (param) url.searchParams.set('tab', param);
  else url.searchParams.delete('tab');
  window.history.replaceState(null, '', url);
  if (!editing) {
    // Đã có sẵn dữ liệu bài làm (tải cùng lúc khi mở trang) thì vẽ ngay, không gọi lại máy chủ
    if (state.attemptsPreloaded) renderAttempts();
    else {
      $('attemptsPanel').innerHTML = '<div class="kz-card p-6 text-center text-on-surface-variant">Đang tải bài làm…</div>';
      loadAttempts();
    }
  }
}

/** Danh sách câu bên trái: số câu, dạng câu, điểm, câu nghe, câu chưa hoàn chỉnh. */
function renderOutline() {
  const auto = autoMode();
  $('questionOutline').innerHTML = state.questions.map((q, i) => {
    const incomplete = validateQuestion(q, { autoMode: auto });
    const manual = isTutorGraded(q);
    return `
      <li>
        <button type="button" class="ex-outline-item" data-select="${i}" data-active="${i === state.selected}" aria-current="${i === state.selected ? 'true' : 'false'}">
          <b class="w-6 shrink-0 text-center">${i + 1}</b>
          <span class="material-symbols-outlined text-[18px] text-primary shrink-0" aria-hidden="true">${TYPE_ICONS[q.type] || 'help'}</span>
          <span class="flex-1 min-w-0 truncate">${escapeHtml(questionTypeLabel(q.type))}</span>
          ${q.image ? '<span class="material-symbols-outlined text-[17px] text-secondary" title="Có ảnh" aria-label="Có ảnh">image</span>' : ''}
          ${q.audio ? '<span class="material-symbols-outlined text-[17px] text-secondary" title="Có file nghe" aria-label="Có file nghe">headphones</span>' : ''}
          ${needsAudio(q) ? '<span class="material-symbols-outlined text-[17px] text-error" title="Câu nghe chưa có file nghe" aria-label="Thiếu file nghe">headset_off</span>' : ''}
          ${incomplete && incomplete !== AUDIO_MISSING_MESSAGE ? '<span class="material-symbols-outlined text-[17px] text-error" title="Chưa hoàn chỉnh" aria-label="Chưa hoàn chỉnh">error</span>' : ''}
          ${manual && !incomplete ? '<span class="kz-badge" data-tone="paused" title="AI gợi ý điểm, gia sư duyệt">AI</span>' : ''}
          <span class="font-caption text-caption text-on-surface-variant shrink-0">${formatScore(q.points)}đ</span>
        </button>
      </li>`;
  }).join('') || '<li class="px-2 py-3 text-on-surface-variant font-body-sm text-body-sm">Chưa có câu hỏi. Chọn dạng câu bên dưới để thêm.</li>';
}

/** Vẽ danh sách câu (trái) và câu đang chọn (phải). */
function renderQuestions() {
  const n = state.questions.length;
  if (state.selected >= n) state.selected = Math.max(0, n - 1);
  renderOutline();
  const list = $('questionList');
  const q = state.questions[state.selected];
  list.innerHTML = q ? cardHtml(q, state.selected)
    : '<li class="kz-card p-8 text-center text-on-surface-variant">Chưa có câu hỏi nào. Chọn một dạng câu ở cột bên trái để bắt đầu soạn.</li>';
  const card = list.querySelector('[data-q]');
  if (card && q?.audio) mountAudioPlayer(card.querySelector('[data-audio-player]'), spaceId, q.audio, { showName: true });
  if (card && q?.image) mountQuestionImage(card.querySelector('[data-image-view]'), spaceId, q.image, { maxHeight: 280 });
  renderTabLabels();
  updateTotals();
}

function selectQuestion(i) {
  if (i < 0 || i >= state.questions.length) return;
  state.selected = i;
  renderQuestions();
  if (window.innerWidth < 1024) $('questionList').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

/** Khối file nghe của một câu: thêm / đổi / bỏ file MP3 và số lần nghe tối đa. */
/** Tab "Cài đặt bài" → Chấm bài: cách chấm tự suy ra từ các câu tự luận (không chọn chung cho cả bài nữa). */
function renderGradingInfo(manualCount) {
  const box = $('gradingInfo');
  if (!box) return;
  box.innerHTML = manualCount
    ? `<span class="material-symbols-outlined text-[20px] text-tertiary" aria-hidden="true">auto_awesome</span>
       <span><b>AI gợi ý, gia sư duyệt</b> — ${manualCount} câu tự luận / điền khuyết: khi học viên nộp, AI gợi ý điểm và nhận xét, bạn duyệt lại; các câu còn lại máy chấm ngay.
       <span class="block font-caption text-caption text-on-surface-variant mt-0.5">Câu điền khuyết có nhập đáp án: học viên điền khớp là có điểm ngay, chỉ câu điền khác mới cần duyệt.</span></span>`
    : `<span class="material-symbols-outlined text-[20px] text-primary" aria-hidden="true">bolt</span>
       <span><b>Hệ thống chấm tự động</b> — bài không có câu tự luận / điền khuyết, học viên nộp xong có điểm ngay.</span>`;
}

/** Ô chọn số lần nghe tối đa (dùng chung cho câu nghe có / chưa có file). */
function playsSelectHtml(plays) {
  const v = plays || '';
  return `
    <label class="flex items-center gap-1.5 font-caption text-caption text-on-surface-variant">Số lần nghe
      <select data-field="maxPlays" class="${INPUT} w-auto py-1.5" ${dis()}>
        <option value="" ${v === '' ? 'selected' : ''}>Không giới hạn</option>
        ${MAX_PLAYS_OPTIONS.map((n) => `<option value="${n}" ${Number(v) === n ? 'selected' : ''}>Tối đa ${n} lần</option>`).join('')}
      </select>
    </label>`;
}

/**
 * Khối file nghe của một câu, 3 trạng thái:
 * - câu thường: nút "Thêm file nghe" + "Đánh dấu câu nghe";
 * - câu nghe chưa có file (nhập từ Excel / đánh dấu trước): cảnh báo nổi bật, chưa giao bài được;
 * - đã có file: trình phát, số lần nghe, đổi / bỏ file.
 */
function audioEditorHtml(q) {
  if (!q.audio && q.listening) {
    if (state.locked) return '';
    return `
      <div class="rounded-xl border-2 border-dashed border-[#e8a35c] bg-tertiary-fixed/40 p-4 space-y-3">
        <p class="flex items-start gap-2 text-on-tertiary-fixed"><span class="material-symbols-outlined text-[20px]" aria-hidden="true">headphones</span>
          <span><b>Câu nghe – chưa có file nghe.</b> Thêm file trước khi giao bài (vẫn lưu nháp được).</span></p>
        <div class="flex flex-wrap items-center gap-2">
          <label class="kz-btn kz-btn-primary cursor-pointer">
            <span class="material-symbols-outlined text-[18px]" aria-hidden="true">upload</span>Thêm file nghe
            <input type="file" data-field="audioFile" accept="${AUDIO_ACCEPT}" class="sr-only">
          </label>
          ${playsSelectHtml(q.listening.maxPlays)}
          <button type="button" data-act="listening-off" class="${SMALL_BTN}">Không phải câu nghe</button>
          <span data-audio-busy class="font-caption text-caption text-primary hidden">Đang tải file lên…</span>
        </div>
        <p class="font-caption text-caption text-on-surface-variant">${AUDIO_RULES_TEXT}.</p>
      </div>`;
  }
  if (!q.audio) {
    if (state.locked) return '';
    return `
      <div class="flex flex-wrap items-center gap-2">
        <label class="${SMALL_BTN} cursor-pointer">
          <span class="material-symbols-outlined text-[18px]" aria-hidden="true">headphones</span>Thêm file nghe
          <input type="file" data-field="audioFile" accept="${AUDIO_ACCEPT}" class="sr-only">
        </label>
        <button type="button" data-act="listening-on" class="${SMALL_BTN}" title="Đánh dấu là câu nghe, thêm file sau">Đánh dấu câu nghe</button>
        <span class="font-caption text-caption text-on-surface-variant">Câu nghe hiểu: ${AUDIO_RULES_TEXT}.</span>
        <span data-audio-busy class="font-caption text-caption text-primary hidden">Đang tải file lên…</span>
      </div>`;
  }
  const plays = q.audio.maxPlays || '';
  return `
    <div class="space-y-2">
      <div data-audio-player></div>
      <div class="flex flex-wrap items-center gap-2">
        <label class="flex items-center gap-1.5 font-caption text-caption text-on-surface-variant">Số lần nghe
          <select data-field="maxPlays" class="${INPUT} w-auto py-1.5" ${dis()}>
            <option value="" ${plays === '' ? 'selected' : ''}>Không giới hạn</option>
            ${MAX_PLAYS_OPTIONS.map((n) => `<option value="${n}" ${Number(plays) === n ? 'selected' : ''}>Tối đa ${n} lần</option>`).join('')}
          </select>
        </label>
        ${state.locked ? `<span class="font-caption text-caption text-on-surface-variant">${escapeHtml(playLimitLabel(q.audio.maxPlays))}</span>` : `
          <label class="${SMALL_BTN} cursor-pointer">Đổi file
            <input type="file" data-field="audioFile" accept="${AUDIO_ACCEPT}" class="sr-only">
          </label>
          <button type="button" data-act="audio-remove" class="${SMALL_BTN} text-error">Bỏ file nghe</button>
          <span data-audio-busy class="font-caption text-caption text-primary hidden">Đang tải file lên…</span>`}
      </div>
    </div>`;
}

/** Khối ảnh minh họa của câu: thêm / đổi / bỏ một ảnh. Bài đã có người làm thì chỉ xem. */
function imageEditorHtml(q) {
  if (!q.image) {
    if (state.locked) return '';
    return `
      <div class="flex flex-wrap items-center gap-2">
        <label class="${SMALL_BTN} cursor-pointer">
          <span class="material-symbols-outlined text-[18px]" aria-hidden="true">add_photo_alternate</span>Thêm ảnh
          <input type="file" data-field="imageFile" accept="${IMAGE_ACCEPT}" class="sr-only">
        </label>
        <span class="font-caption text-caption text-on-surface-variant">Ảnh minh họa cho đề (tranh, bảng, chữ viết tay…): ${IMAGE_RULES_TEXT}</span>
        <span data-image-busy class="font-caption text-caption text-primary hidden">Đang tải ảnh lên…</span>
      </div>`;
  }
  return `
    <div class="space-y-2">
      <div data-image-view></div>
      ${state.locked ? '' : `
        <div class="flex flex-wrap items-center gap-2">
          <span class="font-caption text-caption text-on-surface-variant min-w-0 truncate max-w-[260px]" title="${escapeHtml(q.image.fileName || '')}">${escapeHtml(q.image.fileName || 'Ảnh')}</span>
          <label class="${SMALL_BTN} cursor-pointer">Đổi ảnh
            <input type="file" data-field="imageFile" accept="${IMAGE_ACCEPT}" class="sr-only">
          </label>
          <button type="button" data-act="image-remove" class="${SMALL_BTN} text-error">Bỏ ảnh</button>
          <span data-image-busy class="font-caption text-caption text-primary hidden">Đang tải ảnh lên…</span>
        </div>`}
    </div>`;
}

/** Gia sư chọn ảnh: kiểm tra định dạng / dung lượng, tải lên máy chủ (R2 hoặc DB), gắn vào câu (lưu khi lưu bài). */
async function uploadQuestionImage(card, q, input) {
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  const errEl = card.querySelector('[data-error]');
  const showErr = (msg) => {
    errEl.textContent = msg;
    errEl.classList.toggle('hidden', !msg);
  };
  const invalid = validateImageFile(file);
  if (invalid) {
    showErr(invalid);
    return;
  }
  showErr('');
  card.querySelector('[data-image-busy]')?.classList.remove('hidden');
  card.querySelectorAll('input[data-field="imageFile"]').forEach((el) => { el.disabled = true; });
  try {
    q.image = await API.exercises.uploadImage(spaceId, file);
    state.dirty = true;
    renderQuestions();
    showToast('Đã tải ảnh lên. Nhớ lưu bài để giữ thay đổi.');
  } catch (err) {
    card.querySelector('[data-image-busy]')?.classList.add('hidden');
    card.querySelectorAll('input[data-field="imageFile"]').forEach((el) => { el.disabled = false; });
    showErr(err.fieldErrors?.file || friendlyErrorMessage(err));
  }
}

/** Gia sư chọn file nghe: kiểm tra định dạng / dung lượng, tải lên Cloudflare R2, gắn vào câu (lưu khi lưu bài). */
async function uploadQuestionAudio(card, q, input) {
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  const errEl = card.querySelector('[data-error]');
  const showErr = (msg) => {
    errEl.textContent = msg;
    errEl.classList.toggle('hidden', !msg);
  };
  const invalid = validateAudioFile(file);
  if (invalid) {
    showErr(invalid);
    return;
  }
  showErr('');
  card.querySelector('[data-audio-busy]')?.classList.remove('hidden');
  card.querySelectorAll('input[data-field="audioFile"]').forEach((el) => { el.disabled = true; });
  try {
    const info = await API.exercises.uploadAudio(spaceId, file);
    const plays = q.audio?.maxPlays ?? q.listening?.maxPlays ?? null;
    q.audio = { ...info, maxPlays: plays };
    q.listening = { maxPlays: plays };
    state.dirty = true;
    renderQuestions();
    showToast('Đã tải file nghe lên. Nhớ lưu bài để giữ thay đổi.');
  } catch (err) {
    card.querySelector('[data-audio-busy]')?.classList.add('hidden');
    card.querySelectorAll('input[data-field="audioFile"]').forEach((el) => { el.disabled = false; });
    showErr(err.fieldErrors?.file || friendlyErrorMessage(err));
  }
}

function updateTotals() {
  const qs = state.questions;
  const total = formatScore(totalPoints(qs));
  $('totalsText').textContent = `${qs.length} câu · ${total} điểm`;
  const auto = autoMode();
  const manual = qs.filter(isTutorGraded).length;
  renderGradingInfo(manual);
  const listening = qs.filter((q) => q.audio || q.listening).length;
  const missing = qs.filter((q) => needsAudio(q)).length;
  const incomplete = qs.filter((q) => { const e = validateQuestion(q, { autoMode: auto }); return e && e !== AUDIO_MISSING_MESSAGE; }).length;
  $('footerStats').innerHTML = `
    <span><b class="text-on-surface">${qs.length}</b> câu</span>
    <span><b class="text-on-surface">${total}</b> điểm</span>
    ${manual ? `<span><b class="text-on-surface">${manual}</b> câu AI gợi ý · gia sư duyệt</span>` : ''}
    ${listening ? `<span class="inline-flex items-center gap-1"><span class="material-symbols-outlined text-[18px] text-primary" aria-hidden="true">headphones</span><b class="text-on-surface">${listening}</b> câu nghe</span>` : ''}
    ${missing && !state.locked ? `<button type="button" data-goto="missing" class="inline-flex items-center gap-1 text-error font-semibold hover:underline"><span class="material-symbols-outlined text-[18px]" aria-hidden="true">headset_off</span>${missing} câu chưa có file nghe</button>` : ''}
    ${incomplete && !state.locked ? `<button type="button" data-goto="incomplete" class="inline-flex items-center gap-1 text-error hover:underline"><span class="material-symbols-outlined text-[18px]" aria-hidden="true">error</span>${incomplete} câu chưa hoàn chỉnh</button>` : ''}`;
}

function dis() {
  return state.locked ? 'disabled' : '';
}

function cardHtml(q, i) {
  const n = state.questions.length;
  return `
    <li class="kz-card p-5 md:p-6 space-y-4" data-q="${i}">
      <div class="flex flex-wrap items-center gap-2">
        <span class="font-title-md text-title-md">Câu ${i + 1}</span>
        ${q.audio ? '<span class="kz-badge" style="background:#dce1ff;color:#1d3989"><span class="material-symbols-outlined text-[15px]" aria-hidden="true">headphones</span>Câu nghe</span>' : ''}
        ${needsAudio(q) ? '<span class="kz-badge" style="background:#ffdad6;color:#93000a"><span class="material-symbols-outlined text-[15px]" aria-hidden="true">headset_off</span>Câu nghe · thiếu file</span>' : ''}
        ${q.image ? '<span class="kz-badge"><span class="material-symbols-outlined text-[15px]" aria-hidden="true">image</span>Có ảnh</span>' : ''}
        <select data-field="type" class="${INPUT} w-auto py-1.5" ${dis()} aria-label="Dạng câu">
          ${QUESTION_TYPES.map((t) => `<option value="${t.value}" ${t.value === q.type ? 'selected' : ''}>${t.label}</option>`).join('')}
        </select>
        <label class="flex items-center gap-1.5 font-caption text-caption text-on-surface-variant">Điểm
          <input data-field="points" type="number" min="0.25" max="100" step="0.25" value="${escapeHtml(q.points)}" class="${INPUT} w-20 py-1.5" ${dis()}>
        </label>
        <span class="flex-1"></span>
        ${state.locked ? '' : `
          <button type="button" data-act="up" class="${SMALL_BTN}" ${i === 0 ? 'disabled' : ''} aria-label="Lên">▲</button>
          <button type="button" data-act="down" class="${SMALL_BTN}" ${i === n - 1 ? 'disabled' : ''} aria-label="Xuống">▼</button>
          <button type="button" data-act="duplicate" class="${SMALL_BTN}">Nhân bản</button>
          <button type="button" data-act="remove" class="${SMALL_BTN} text-error">Xóa</button>`}
      </div>
      <p class="font-caption text-caption text-on-surface-variant">${escapeHtml(questionTypeHint(q.type))}</p>
      ${['FILL_BLANK', 'WORD_BANK'].includes(q.type) ? '' : `
        <div>
          <label class="kz-label">Đề bài</label>
          <textarea data-field="prompt" rows="2" maxlength="2000" class="${INPUT}" placeholder="Nhập câu hỏi..." ${dis()}>${escapeHtml(q.prompt)}</textarea>
        </div>`}
      ${imageEditorHtml(q)}
      ${audioEditorHtml(q)}
      <div data-body>${bodyHtml(q, i)}</div>
      <p data-error class="field-error hidden"></p>
      <div class="flex justify-between gap-3 pt-3 border-t border-surface-container-high">
        <button type="button" data-act="prev" class="kz-btn" ${i === 0 ? 'disabled' : ''}><span class="material-symbols-outlined text-[18px]" aria-hidden="true">arrow_back</span>Câu ${i}</button>
        <button type="button" data-act="next" class="kz-btn" ${i === n - 1 ? 'disabled' : ''}>Câu ${i + 2}<span class="material-symbols-outlined text-[18px]" aria-hidden="true">arrow_forward</span></button>
      </div>
    </li>`;
}

function bodyHtml(q) {
  const d = q.data || {};
  switch (q.type) {
    case 'SINGLE_CHOICE':
    case 'MULTI_CHOICE': {
      const multi = q.type === 'MULTI_CHOICE';
      const correct = multi ? new Set(d.correct || []) : d.correct;
      const group = `c-${Math.random().toString(36).slice(2)}`;
      return `
        <p class="kz-label">Các lựa chọn <span class="font-normal text-on-surface-variant">(${multi ? 'tích tất cả đáp án đúng' : 'chọn một đáp án đúng'})</span></p>
        <div class="flex flex-col gap-2">
          ${(d.options || []).map((o, j) => `
            <div class="flex items-center gap-2">
              <input type="${multi ? 'checkbox' : 'radio'}" name="${group}" data-correct="${j}" ${(multi ? correct.has(j) : correct === j) ? 'checked' : ''} ${dis()} aria-label="Đáp án đúng ${String.fromCharCode(65 + j)}" class="w-5 h-5 accent-[#006194]">
              <span class="w-6 text-center font-bold text-on-surface-variant">${String.fromCharCode(65 + j)}</span>
              <input data-opt="${j}" value="${escapeHtml(o)}" class="${INPUT}" placeholder="Lựa chọn ${String.fromCharCode(65 + j)}" ${dis()}>
              ${state.locked ? '' : `<button type="button" data-act="opt-remove" data-j="${j}" class="${SMALL_BTN}" ${(d.options || []).length <= 2 ? 'disabled' : ''} aria-label="Xóa lựa chọn">✕</button>`}
            </div>`).join('')}
        </div>
        ${state.locked ? '' : `<button type="button" data-act="opt-add" class="${SMALL_BTN} mt-2" ${(d.options || []).length >= 10 ? 'disabled' : ''}>＋ Thêm lựa chọn</button>`}`;
    }
    case 'TRUE_FALSE': {
      const group = `tf-${Math.random().toString(36).slice(2)}`;
      return `
        <p class="kz-label">Đáp án đúng</p>
        <div class="flex gap-4">
          ${[[true, 'Đúng'], [false, 'Sai']].map(([v, l]) => `
            <label class="flex items-center gap-2"><input type="radio" name="${group}" data-tf="${v}" ${d.correct === v ? 'checked' : ''} ${dis()} class="w-5 h-5 accent-[#006194]">${l}</label>`).join('')}
        </div>`;
    }
    case 'WORD_BANK': {
      const n = countBlanks(d.text);
      const group = `wb-${Math.random().toString(36).slice(2)}`;
      return `
        <div>
          <label class="kz-label">Câu có chỗ trống <span class="font-normal text-on-surface-variant">(gõ ___ ở mỗi chỗ trống)</span></label>
          <textarea data-field="text" rows="3" maxlength="2000" class="${INPUT}" placeholder="Ví dụ: 私は ___ 学校 ___ 日本語 ___ 勉強します。" ${dis()}>${escapeHtml(d.text || '')}</textarea>
        </div>
        <div>
          <p class="kz-label">Các phương án <span class="font-normal text-on-surface-variant">(chọn một phương án đúng)</span></p>
          <p data-blanks class="font-caption text-caption text-on-surface-variant mb-2">${wordBankHint(n)}</p>
          <div class="flex flex-col gap-2">
            ${(d.options || []).map((o, j) => `
              <div class="flex items-center gap-2">
                <input type="radio" name="${group}" data-correct="${j}" ${d.correct === j ? 'checked' : ''} ${dis()} aria-label="Phương án đúng ${String.fromCharCode(65 + j)}" class="w-5 h-5 accent-[#006194]">
                <span class="w-6 text-center font-bold text-on-surface-variant">${String.fromCharCode(65 + j)}</span>
                <input data-opt="${j}" value="${escapeHtml(o)}" class="${INPUT}" placeholder="${n > 1 ? Array.from({ length: n }, (_, i) => `từ ${i + 1}`).join(' / ') : 'Từ cần điền'}" ${dis()}>
                ${state.locked ? '' : `<button type="button" data-act="opt-remove" data-j="${j}" class="${SMALL_BTN}" ${(d.options || []).length <= 2 ? 'disabled' : ''} aria-label="Xóa phương án">✕</button>`}
              </div>`).join('')}
          </div>
          ${state.locked ? '' : `<button type="button" data-act="opt-add" class="${SMALL_BTN} mt-2" ${(d.options || []).length >= 10 ? 'disabled' : ''}>＋ Thêm phương án</button>`}
        </div>`;
    }
    case 'FILL_BLANK':
      return `
        <div>
          <label class="kz-label">Câu có chỗ trống <span class="font-normal text-on-surface-variant">(gõ ___ ở mỗi chỗ trống)</span></label>
          <textarea data-field="text" rows="3" maxlength="2000" class="${INPUT}" placeholder="Ví dụ: 私は 毎日 ___ を 食べます。" ${dis()}>${escapeHtml(d.text || '')}</textarea>
        </div>
        <div data-blanks>${blanksHtml(q)}</div>`;
    case 'MATCHING':
      return `
        <p class="kz-label">Các cặp nối đúng <span class="font-normal text-on-surface-variant">(học viên sẽ thấy cột phải bị xáo trộn)</span></p>
        <div class="flex flex-col gap-2">
          ${(d.pairs || []).map((p, j) => `
            <div class="grid grid-cols-[1fr_auto_1fr_auto] items-center gap-2">
              <input data-pair="${j}" data-side="left" value="${escapeHtml(p.left)}" class="${INPUT}" placeholder="Vế trái" ${dis()}>
              <span class="text-on-surface-variant">→</span>
              <input data-pair="${j}" data-side="right" value="${escapeHtml(p.right)}" class="${INPUT}" placeholder="Vế phải" ${dis()}>
              ${state.locked ? '<span></span>' : `<button type="button" data-act="pair-remove" data-j="${j}" class="${SMALL_BTN}" ${(d.pairs || []).length <= 2 ? 'disabled' : ''} aria-label="Xóa cặp">✕</button>`}
            </div>`).join('')}
        </div>
        ${state.locked ? '' : `<button type="button" data-act="pair-add" class="${SMALL_BTN} mt-2" ${(d.pairs || []).length >= 20 ? 'disabled' : ''}>＋ Thêm cặp</button>`}`;
    case 'ORDERING':
      return `
        <p class="kz-label">Các mục theo thứ tự đúng <span class="font-normal text-on-surface-variant">(học viên sẽ thấy bị xáo trộn)</span></p>
        <div class="flex flex-col gap-2">
          ${(d.items || []).map((it, j) => `
            <div class="flex items-center gap-2">
              <span class="w-6 text-center font-bold text-on-surface-variant">${j + 1}</span>
              <input data-item="${j}" value="${escapeHtml(it)}" class="${INPUT}" placeholder="Mục ${j + 1}" ${dis()}>
              ${state.locked ? '' : `
                <button type="button" data-act="item-up" data-j="${j}" class="${SMALL_BTN}" ${j === 0 ? 'disabled' : ''} aria-label="Lên">▲</button>
                <button type="button" data-act="item-down" data-j="${j}" class="${SMALL_BTN}" ${j === (d.items || []).length - 1 ? 'disabled' : ''} aria-label="Xuống">▼</button>
                <button type="button" data-act="item-remove" data-j="${j}" class="${SMALL_BTN}" ${(d.items || []).length <= 2 ? 'disabled' : ''} aria-label="Xóa mục">✕</button>`}
            </div>`).join('')}
        </div>
        ${state.locked ? '' : `<button type="button" data-act="item-add" class="${SMALL_BTN} mt-2" ${(d.items || []).length >= 20 ? 'disabled' : ''}>＋ Thêm mục</button>`}`;
    case 'WRITTEN': {
      const old = state.locked ? [] : legacyWrittenAnswers(q);
      const oldNote = old.length ? `
        <p class="mb-2 flex items-start gap-2.5 rounded-xl border border-amber-300 bg-amber-50 px-4 py-3 text-amber-900">
          <span class="material-symbols-outlined text-[20px]" aria-hidden="true">warning</span>
          <span>Câu này có đáp án cũ: <b>${escapeHtml(old.join(' / '))}</b>. Khi lưu, đáp án cũ sẽ bị bỏ và câu chuyển sang <b>AI gợi ý, gia sư duyệt</b>.</span>
        </p>` : '';
      return `${oldNote}
        <p class="flex items-start gap-2.5 rounded-xl bg-surface-container-low px-4 py-3 text-on-surface-variant">
          <span class="material-symbols-outlined text-[20px] text-primary" aria-hidden="true">auto_awesome</span>
          <span>Không cần đáp án. Khi học viên nộp bài, <b class="text-on-surface">AI gợi ý điểm và nhận xét</b>, bạn duyệt lại ở tab "Bài làm của học viên".</span>
        </p>`;
    }
    default:
      return '';
  }
}

/** Hướng dẫn nhập phương án theo số chỗ trống của câu "Điền từ có sẵn". */
function wordBankHint(n) {
  if (!n) return 'Gõ ___ vào câu để tạo chỗ trống trước.';
  const sample = Array.from({ length: n }, (_, i) => ['は', 'で', 'を', 'に', 'が'][i % 5]).join('/');
  return `Câu có <b>${n}</b> chỗ trống: mỗi phương án ghi đủ ${n} từ${n > 1 ? ` theo thứ tự, cách nhau bằng dấu / (ví dụ: ${sample})` : ''}.`;
}

function blanksHtml(q) {
  const d = q.data || {};
  const n = countBlanks(d.text);
  if (!n) return '<p class="font-caption text-caption text-on-surface-variant">Chưa có chỗ trống. Gõ ___ (3 dấu gạch dưới) vào câu để tạo chỗ trống.</p>';
  if (q.type === 'FILL_BLANK') {
    return `
      <p class="kz-label">Đáp án từng chỗ trống <span class="font-normal text-on-surface-variant">(không bắt buộc; nhiều cách viết đúng thì phân cách bằng ;)</span></p>
      <p class="font-caption text-caption text-on-surface-variant mb-2">Ô có đáp án: học viên điền khớp là máy chấm đúng ngay. Ô để trống hoặc học viên điền khác: AI gợi ý điểm, bạn duyệt lại.</p>
      <div class="flex flex-col gap-2">
        ${Array.from({ length: n }, (_, i) => `
          <label class="flex items-center gap-2"><span class="w-10 font-bold text-on-surface-variant">(${i + 1})</span>
            <input data-blank="${i}" value="${escapeHtml(((d.answers || [])[i] || []).join('; '))}" class="${INPUT}" placeholder="Để trống = AI chấm · ví dụ: たべます; 食べます" ${dis()}></label>`).join('')}
      </div>`;
  }
  return '';
}

// ============================================================ sửa câu hỏi

function onListInput(e) {
  const card = e.target.closest('[data-q]');
  if (!card) return;
  const i = Number(card.dataset.q);
  const q = state.questions[i];
  const d = q.data;
  const t = e.target;
  state.dirty = true;
  if (t.dataset.field === 'prompt') q.prompt = t.value;
  else if (t.dataset.field === 'points') { q.points = t.value; updateTotals(); }
  else if (t.dataset.field === 'text') {
    const before = countBlanks(d.text);
    d.text = t.value;
    const after = countBlanks(d.text);
    if (after !== before) {
      card.querySelector('[data-blanks]').innerHTML = q.type === 'WORD_BANK' ? wordBankHint(after) : blanksHtml(q);
    }
  } else if (t.dataset.field === 'distractors') d.distractors = splitList(t.value);
  else if (t.dataset.opt !== undefined) d.options[Number(t.dataset.opt)] = t.value;
  else if (t.dataset.blank !== undefined) {
    d.answers = d.answers || [];
    d.answers[Number(t.dataset.blank)] = splitList(t.value);
  } else if (t.dataset.bankword !== undefined) {
    d.answers = d.answers || [];
    d.answers[Number(t.dataset.bankword)] = t.value;
  } else if (t.dataset.pair !== undefined) d.pairs[Number(t.dataset.pair)][t.dataset.side] = t.value;
  else if (t.dataset.item !== undefined) d.items[Number(t.dataset.item)] = t.value;
  // Cập nhật dấu "chưa hoàn chỉnh" / điểm ở danh sách câu và thanh dưới
  renderOutline();
  updateTotals();
}

async function onListChange(e) {
  const card = e.target.closest('[data-q]');
  if (!card) return;
  const i = Number(card.dataset.q);
  const q = state.questions[i];
  const t = e.target;
  if (t.dataset.field === 'audioFile') {
    await uploadQuestionAudio(card, q, t);
    return;
  }
  if (t.dataset.field === 'imageFile') {
    await uploadQuestionImage(card, q, t);
    return;
  }
  state.dirty = true;
  if (t.dataset.field === 'maxPlays') {
    const plays = t.value ? Number(t.value) : null;
    if (q.audio) q.audio.maxPlays = plays;
    if (q.listening) q.listening.maxPlays = plays;
  } else if (t.dataset.field === 'type') {
    const hasContent = JSON.stringify(q.data) !== JSON.stringify(defaultQuestion(q.type).data) || q.prompt;
    if (hasContent && !(await confirmBox('Đổi dạng câu?', 'Đáp án đã nhập của câu này sẽ bị xóa.', 'Đổi dạng câu'))) {
      t.value = q.type;
      return;
    }
    // Giữ file nghe và ảnh khi đổi dạng câu
    state.questions[i] = { ...defaultQuestion(t.value), prompt: q.prompt, points: q.points, audio: q.audio || null,
      image: q.image || null, listening: q.listening || null };
    renderQuestions();
  } else if (t.dataset.correct !== undefined) {
    const j = Number(t.dataset.correct);
    if (q.type === 'MULTI_CHOICE') {
      const set = new Set(q.data.correct || []);
      if (t.checked) set.add(j); else set.delete(j);
      q.data.correct = [...set].sort((a, b) => a - b);
    } else q.data.correct = j;
  } else if (t.dataset.tf !== undefined) q.data.correct = t.dataset.tf === 'true';
  renderOutline();
  updateTotals();
}

function onListClick(e) {
  const btn = e.target.closest('[data-act]');
  if (!btn) return;
  const card = btn.closest('[data-q]');
  const i = Number(card.dataset.q);
  const q = state.questions[i];
  const d = q.data;
  const j = Number(btn.dataset.j);
  const swap = (arr, a, b) => { [arr[a], arr[b]] = [arr[b], arr[a]]; };
  state.dirty = true;
  switch (btn.dataset.act) {
    case 'prev': selectQuestion(i - 1); return;
    case 'next': selectQuestion(i + 1); return;
    case 'up': if (i > 0) { swap(state.questions, i, i - 1); state.selected = i - 1; } break;
    case 'down': if (i < state.questions.length - 1) { swap(state.questions, i, i + 1); state.selected = i + 1; } break;
    case 'duplicate': state.questions.splice(i + 1, 0, JSON.parse(JSON.stringify(q))); state.selected = i + 1; break;
    case 'remove': state.questions.splice(i, 1); state.selected = Math.min(i, state.questions.length - 1); break;
    case 'opt-add': d.options.push(''); break;
    case 'opt-remove':
      d.options.splice(j, 1);
      if (q.type !== 'MULTI_CHOICE') d.correct = d.correct === j ? null : (d.correct > j ? d.correct - 1 : d.correct);
      else d.correct = (d.correct || []).filter((c) => c !== j).map((c) => (c > j ? c - 1 : c));
      break;
    case 'pair-add': d.pairs.push({ left: '', right: '' }); break;
    case 'pair-remove': d.pairs.splice(j, 1); break;
    case 'item-add': d.items.push(''); break;
    case 'item-remove': d.items.splice(j, 1); break;
    case 'item-up': if (j > 0) swap(d.items, j, j - 1); break;
    case 'item-down': if (j < d.items.length - 1) swap(d.items, j, j + 1); break;
    case 'audio-remove': q.audio = null; break; // vẫn là câu nghe → hiện cảnh báo thiếu file
    case 'image-remove': q.image = null; break;
    case 'listening-on': q.listening = { maxPlays: null }; break;
    case 'listening-off': q.listening = null; break;
    default: return;
  }
  renderQuestions();
}

function addQuestion(type) {
  state.questions.push(defaultQuestion(type));
  state.dirty = true;
  selectQuestion(state.questions.length - 1);
  const outline = $('questionOutline');
  outline.scrollTop = outline.scrollHeight;
}

// ============================================================ lưu / giao bài

function collectSettings() {
  return {
    title: $('exTitle').value,
    instructions: $('exInstructions').value,
    opensLocal: $('exOpens').value,
    closesLocal: $('exCloses').value,
    unlimited: $('exUnlimited').checked,
    timeLimit: $('exTimeLimit').value,
    maxAttempts: $('exAttempts').value
  };
}

function showSettingErrors(errors) {
  setFieldError('exTitleError', errors.title);
  setFieldError('exInstructionsError', errors.instructions);
  setFieldError('exClosesError', errors.closesAt);
  setFieldError('exTimeLimitError', errors.timeLimitMinutes);
  setFieldError('exAttemptsError', errors.maxAttempts);
}

function clearQuestionErrors() {
  $('questionList').querySelectorAll('[data-error]').forEach((el) => { el.textContent = ''; el.classList.add('hidden'); });
}

/** Chuyển tới câu bị lỗi rồi hiện lỗi dưới câu đó. */
function showQuestionError(index, message) {
  if (index >= 0 && index < state.questions.length && index !== state.selected) selectQuestion(index);
  const el = $('questionList').querySelector(`[data-q="${index}"]`)?.querySelector('[data-error]');
  if (el) {
    el.textContent = message;
    el.classList.remove('hidden');
  }
}

async function save(publish) {
  const f = collectSettings();
  const closesChanged = (state.detail?.summary?.closesAt ? toLocalInput(state.detail.summary.closesAt) : '') !== f.closesLocal;
  const strict = publish || state.status === 'PUBLISHED';
  const errors = validateExerciseSettings(f, { checkCloseFuture: strict && closesChanged });
  showSettingErrors(errors);
  setFieldError('formError', '');
  clearQuestionErrors();
  if (Object.keys(errors).length) {
    $('formError').textContent = 'Vui lòng kiểm tra lại phần cài đặt bài.';
    $('formError').classList.remove('hidden');
    if (state.tab !== 'settings') switchTab('settings');
    window.scrollTo({ top: 0, behavior: 'smooth' });
    return;
  }
  if (strict && !state.locked) {
    if (!state.questions.length) {
      setFieldError('formError', 'Bài tập cần có ít nhất một câu hỏi.');
      return;
    }
    const errors = state.questions.map((q) => validateQuestion(q, { autoMode: autoMode() }));
    const first = errors.findIndex(Boolean);
    if (first >= 0) {
      const count = errors.filter(Boolean).length;
      setFieldError('formError', `Có ${count} câu hỏi chưa hoàn chỉnh (đánh dấu đỏ ở danh sách câu). Đang mở câu ${first + 1}.`);
      if (state.tab !== 'questions') switchTab('questions');
      showQuestionError(first, `Câu ${first + 1}: ${errors[first]}`);
      $('questionList').scrollIntoView({ behavior: 'smooth', block: 'start' });
      return;
    }
  }
  // Câu tự luận soạn từ trước còn đáp án cũ: báo trước khi lưu làm mất đáp án
  const legacy = state.locked ? [] : state.questions.map((q, i) => (legacyWrittenAnswers(q).length ? i + 1 : null)).filter(Boolean);
  if (legacy.length) {
    const ok = await confirmBox('Bỏ đáp án cũ của câu tự luận?',
      `Câu ${legacy.join(', ')} có đáp án cũ. Tự luận không dùng đáp án nữa: khi lưu, đáp án cũ bị bỏ và các câu này do AI gợi ý, gia sư duyệt.`,
      'Vẫn lưu');
    if (!ok) return;
  }
  if (publish && state.status !== 'PUBLISHED') {
    const ok = await confirmBox('Giao bài cho học viên?',
      $('exOpens').value ? `Học viên sẽ thấy bài từ ${formatDateTime(fromLocalInput($('exOpens').value))}.` : 'Học viên sẽ thấy bài ngay.', 'Giao bài');
    if (!ok) return;
  } else if (state.status === 'PUBLISHED') {
    const ok = await confirmBox('Cập nhật bài đã giao?', state.locked
      ? 'Học viên đã làm bài nên chỉ cập nhật tiêu đề, hướng dẫn, giờ mở / đóng đề và số lượt. Câu hỏi giữ nguyên.'
      : 'Học viên sẽ thấy nội dung mới ngay.', 'Cập nhật');
    if (!ok) return;
  }
  const body = {
    title: f.title.trim(),
    instructions: f.instructions.trim(),
    opensAt: fromLocalInput(f.opensLocal),
    closesAt: fromLocalInput(f.closesLocal),
    timeLimitMinutes: f.unlimited ? null : Number(f.timeLimit),
    maxAttempts: Number(f.maxAttempts),
    showAnswers: $('exShowAnswers').checked,
    publish,
    questions: state.questions.map(questionPayload)
  };
  const buttons = ['saveDraftBtn', 'publishBtn'];
  buttons.forEach((id) => { $(id).disabled = true; });
  state.saving = true;
  try {
    const saved = state.exerciseId
      ? await API.exercises.update(spaceId, state.exerciseId, body)
      : await API.exercises.create(spaceId, body);
    state.exerciseId = saved.summary.exerciseId;
    state.dirty = false;
    const url = new URL(window.location.href);
    url.searchParams.set('exercise', state.exerciseId);
    window.history.replaceState(null, '', url);
    showToast(saved.summary.status === 'PUBLISHED'
      ? (publish && state.status !== 'PUBLISHED' ? 'Đã giao bài cho học viên.' : 'Đã cập nhật bài cho học viên.')
      : 'Đã lưu nháp. Học viên chưa thấy bài này.');
    state.saving = false;
    await load();
  } catch (err) {
    const fe = err.fieldErrors || {};
    showSettingErrors({ title: fe.title, instructions: fe.instructions, closesAt: fe.closesAt,
      timeLimitMinutes: fe.timeLimitMinutes, maxAttempts: fe.maxAttempts });
    const qErr = fe.questions;
    const m = /^Câu (\d+):/.exec(qErr || '');
    if (m) {
      if (state.tab !== 'questions') switchTab('questions');
      showQuestionError(Number(m[1]) - 1, qErr);
    } else if (fe.title || fe.instructions || fe.closesAt || fe.timeLimitMinutes || fe.maxAttempts) {
      if (state.tab !== 'settings') switchTab('settings');
    }
    setFieldError('formError', qErr || friendlyErrorMessage(err));
  } finally {
    state.saving = false;
    buttons.forEach((id) => { $(id).disabled = false; });
    updateSaveState();
  }
}

async function removeExercise() {
  const ok = await confirmBox('Xóa bài làm trực tiếp?', 'Bài và toàn bộ câu hỏi sẽ bị xóa, không khôi phục được.', 'Xóa bài', true);
  if (!ok) return;
  try {
    await API.exercises.remove(spaceId, state.exerciseId);
    state.dirty = false;
    window.location.href = classLink();
  } catch (err) {
    showToast(friendlyErrorMessage(err), 'error');
  }
}

// ============================================================ nhập câu hỏi từ Excel

const IMPORT_MAX_BYTES = 2 * 1024 * 1024;
const importState = { preview: null, filter: 'all', busy: false };

function openImport() {
  if (state.locked) {
    showToast('Học viên đã làm bài nên không thêm câu hỏi được nữa.', 'error');
    return;
  }
  importState.preview = null;
  importState.filter = 'all';
  $('importFile').value = '';
  $('importFileName').textContent = 'Chưa chọn file';
  $('importFileMeta').textContent = 'File .xlsx theo mẫu, tối đa 2 MB và 200 câu.';
  $('importPickLabel').textContent = 'Chọn file Excel';
  setImportError('');
  renderImport();
  $('importModal').classList.remove('hidden');
}

function closeImport() {
  $('importModal').classList.add('hidden');
}

function setImportError(message) {
  $('importError').textContent = message || '';
  $('importError').classList.toggle('hidden', !message);
}

function renderImportSteps() {
  const p = importState.preview;
  const steps = [
    ['Tải file mẫu', true],
    ['Chọn file', !!p || importState.busy],
    ['Kiểm tra & xem trước', !!p],
    ['Thêm vào bài', false]
  ];
  const current = p ? 3 : (importState.busy ? 2 : 1);
  $('importSteps').innerHTML = steps.map(([label], i) => {
    const done = i < current;
    const now = i === current;
    const badge = done
      ? '<span class="material-symbols-outlined text-[22px] text-[#1b6b3a]" aria-hidden="true">check_circle</span>'
      : `<span class="w-6 h-6 rounded-full ${now ? 'bg-primary text-on-primary' : 'bg-surface-container text-on-surface-variant'} text-[12px] flex items-center justify-center">${i + 1}</span>`;
    return `${i ? '<li aria-hidden="true" class="w-8 h-0.5 bg-outline-variant"></li>' : ''}
      <li class="flex items-center gap-2 ${done ? 'text-[#1b6b3a] font-semibold' : now ? 'text-primary font-semibold' : 'text-on-surface-variant'}" ${now ? 'aria-current="step"' : ''}>${badge}${label}</li>`;
  }).join('');
}

/** Tóm tắt lựa chọn và đáp án của một dòng (để gia sư soát trước khi thêm). */
function importSummary(r) {
  const d = r.data || {};
  const opts = d.options || [];
  switch (r.type) {
    case 'SINGLE_CHOICE': return [opts.join(' | '), opts[d.correct] ?? ''];
    case 'MULTI_CHOICE': return [opts.join(' | '), (d.correct || []).map((i) => opts[i]).join(' | ')];
    case 'TRUE_FALSE': return ['', d.correct ? 'Đúng' : 'Sai'];
    case 'FILL_BLANK': return ['', (d.answers || []).map((a) => a.join(' / ')).join(' | ')];
    case 'WORD_BANK': return [opts.map((o) => o.split('/').map((w) => w.trim()).join(' / ')).join(' | '), (opts[d.correct] ?? '').split('/').map((w) => w.trim()).join(' / ')];
    case 'MATCHING': return [(d.pairs || []).map((p) => `${p.left} = ${p.right}`).join(' | '), 'Theo các cặp'];
    case 'ORDERING': return [(d.items || []).join(' → '), 'Theo thứ tự trên'];
    case 'WRITTEN': return ['', 'Không cần đáp án · AI gợi ý, gia sư duyệt'];
    default: return ['', ''];
  }
}

function renderImport() {
  renderImportSteps();
  const p = importState.preview;
  $('importResult').classList.toggle('hidden', !p);
  $('importApply').disabled = !p || !p.valid || importState.busy;
  $('importApply').textContent = p && p.valid ? `Thêm ${p.valid} câu hợp lệ` : 'Thêm câu hợp lệ';
  if (!p) return;
  $('importValid').textContent = p.valid;
  $('importInvalid').textContent = p.invalid;
  $('importPoints').textContent = `${formatScore(p.validPoints)} điểm`;
  document.querySelectorAll('[data-import-filter]').forEach((b) => { b.dataset.active = String(b.dataset.importFilter === importState.filter); });
  const rows = p.rows.filter((r) => importState.filter === 'all' || !r.ok);
  $('importTable').innerHTML = `
    <table class="w-full min-w-[860px] text-left">
      <thead class="bg-surface-container-low font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wide">
        <tr><th class="px-3 py-2.5 w-16">Dòng</th><th class="px-3 py-2.5">Loại câu</th><th class="px-3 py-2.5">Đề bài</th><th class="px-3 py-2.5">Lựa chọn / mục</th><th class="px-3 py-2.5">Đáp án đúng</th><th class="px-3 py-2.5 w-16">Điểm</th><th class="px-3 py-2.5">Nghe</th></tr>
      </thead>
      <tbody>
        ${rows.map((r) => {
          if (!r.ok) {
            return `<tr class="bg-[#fff8f7] border-t border-surface-container-high">
              <td class="px-3 py-3 font-semibold text-error">${r.row}</td>
              <td class="px-3 py-3">${escapeHtml(r.type ? questionTypeLabel(r.type) : '—')}</td>
              <td class="px-3 py-3 text-error" colspan="5"><span class="inline-flex items-start gap-1.5"><span class="material-symbols-outlined text-[18px]" aria-hidden="true">error</span>${escapeHtml(r.error || 'Dòng không hợp lệ.')}</span></td>
            </tr>`;
          }
          const [choices, answer] = importSummary(r);
          const prompt = r.prompt || r.data?.text || '';
          return `<tr class="border-t border-surface-container-high align-top">
            <td class="px-3 py-3 text-on-surface-variant">${r.row}</td>
            <td class="px-3 py-3">${escapeHtml(questionTypeLabel(r.type))}</td>
            <td class="px-3 py-3 [overflow-wrap:anywhere]">${escapeHtml(prompt)}</td>
            <td class="px-3 py-3 [overflow-wrap:anywhere] text-on-surface-variant">${escapeHtml(choices || '—')}</td>
            <td class="px-3 py-3 [overflow-wrap:anywhere]">${escapeHtml(answer || '—')}</td>
            <td class="px-3 py-3">${formatScore(r.points)}</td>
            <td class="px-3 py-3 whitespace-nowrap">${r.listening ? `<span class="inline-flex items-center gap-1 text-secondary"><span class="material-symbols-outlined text-[17px]" aria-hidden="true">headphones</span>${r.maxPlays ? `tối đa ${r.maxPlays} lần` : 'không giới hạn'}</span>` : '—'}</td>
          </tr>`;
        }).join('') || '<tr><td colspan="7" class="px-3 py-6 text-center text-on-surface-variant">Không có dòng lỗi.</td></tr>'}
      </tbody>
    </table>`;
}

async function onImportFile(e) {
  const file = e.target.files?.[0];
  if (!file) return;
  setImportError('');
  importState.preview = null;
  $('importFileName').textContent = file.name;
  $('importFileMeta').textContent = 'Đang kiểm tra file…';
  if (!file.name.toLowerCase().endsWith('.xlsx')) {
    setImportError('Chỉ nhận file Excel .xlsx. Hãy tải file mẫu, điền rồi lưu dạng .xlsx.');
    $('importFileMeta').textContent = '';
    renderImport();
    return;
  }
  if (file.size > IMPORT_MAX_BYTES) {
    setImportError('File Excel tối đa 2 MB.');
    $('importFileMeta').textContent = '';
    renderImport();
    return;
  }
  importState.busy = true;
  renderImport();
  try {
    importState.preview = await API.exercises.importPreview(spaceId, file);
    const p = importState.preview;
    $('importFileMeta').textContent = `${p.total} dòng dữ liệu · ${p.valid} hợp lệ · ${p.invalid} lỗi`;
    $('importPickLabel').textContent = 'Chọn file khác';
    if (p.invalid && !p.valid) importState.filter = 'error';
  } catch (err) {
    setImportError(err.fieldErrors?.file || friendlyErrorMessage(err));
    $('importFileMeta').textContent = '';
  } finally {
    importState.busy = false;
    e.target.value = '';
    renderImport();
  }
}

async function downloadImportTemplate() {
  const btn = $('importTemplateBtn');
  btn.disabled = true;
  try {
    const blob = await API.exercises.importTemplate(spaceId);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'mau-nhap-cau-hoi-kizuna.xlsx';
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 10_000);
  } catch (err) {
    setImportError(friendlyErrorMessage(err));
  } finally {
    btn.disabled = false;
  }
}

function applyImport() {
  const p = importState.preview;
  if (!p || !p.valid) return;
  const added = p.rows.filter((r) => r.ok).map((r) => ({
    type: r.type, prompt: r.prompt || '', points: Number(r.points), data: r.data || {}, audio: null, image: null,
    listening: r.listening ? { maxPlays: r.maxPlays ?? null } : null
  }));
  const replace = document.querySelector('input[name="importMode"]:checked')?.value === 'replace';
  // Bài mới chỉ có 1 câu trống mặc định → coi như thay thế
  const first = state.questions[0];
  const onlyBlank = state.questions.length === 1 && !first.prompt && !first.audio && !first.image
    && JSON.stringify(first.data) === JSON.stringify(defaultQuestion(first.type).data);
  const start = replace || onlyBlank ? 0 : state.questions.length;
  state.questions = replace || onlyBlank ? added : [...state.questions, ...added];
  if (state.questions.length > 200) {
    state.questions = state.questions.slice(0, 200);
    showToast('Mỗi bài tối đa 200 câu: đã bỏ bớt các câu vượt quá.', 'error');
  }
  state.dirty = true;
  closeImport();
  if (state.tab !== 'questions') switchTab('questions');
  selectQuestion(Math.min(start, state.questions.length - 1));
  const needFiles = added.filter((q) => q.listening).length;
  showToast(needFiles
    ? `Đã thêm ${added.length} câu từ Excel. ${needFiles} câu nghe cần thêm file nghe (đánh dấu đỏ ở danh sách câu).`
    : `Đã thêm ${added.length} câu từ Excel. Kiểm tra lại rồi bấm Lưu nháp / Giao bài.`);
}

function initImport() {
  $('importExcelBtn').addEventListener('click', openImport);
  $('importFile').addEventListener('change', onImportFile);
  $('importTemplateBtn').addEventListener('click', downloadImportTemplate);
  $('importApply').addEventListener('click', applyImport);
  document.querySelectorAll('[data-import-close]').forEach((b) => b.addEventListener('click', closeImport));
  document.querySelectorAll('[data-import-filter]').forEach((b) => b.addEventListener('click', () => {
    importState.filter = b.dataset.importFilter;
    renderImport();
  }));
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !$('importModal').classList.contains('hidden')) closeImport();
  });
}

// ============================================================ xem thử

function openPreview() {
  const list = $('previewList');
  list.innerHTML = state.questions.map((q, i) => `
    <li class="kz-card p-5 space-y-3">
      <div class="flex flex-wrap items-center gap-2">
        <span class="font-title-sm text-title-sm">Câu ${i + 1}</span>
        <span class="kz-badge">${escapeHtml(questionTypeLabel(q.type))}</span>
        <span class="font-caption text-caption text-on-surface-variant">${formatScore(q.points)} điểm</span>
      </div>
      ${q.prompt && !['FILL_BLANK', 'WORD_BANK'].includes(q.type) ? `<p class="text-[16px] whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(q.prompt)}</p>` : ''}
      ${q.image ? `<div data-preview-image="${i}"></div>` : ''}
      ${q.audio ? `<div data-preview-audio="${i}"></div><p class="font-caption text-caption text-on-surface-variant">${escapeHtml(playLimitLabel(q.audio.maxPlays))} (khi học viên làm bài)</p>` : ''}
      ${needsAudio(q) ? '<p class="rounded-lg bg-error-container text-on-error-container px-3 py-2 font-body-sm text-body-sm">Câu nghe chưa có file nghe — cần thêm trước khi giao bài.</p>' : ''}
      <div data-preview="${i}"></div>
    </li>`).join('') || '<li class="kz-item text-center">Chưa có câu hỏi.</li>';
  state.questions.forEach((q, i) => {
    if (q.audio) mountAudioPlayer(list.querySelector(`[data-preview-audio="${i}"]`), spaceId, q.audio);
    if (q.image) mountQuestionImage(list.querySelector(`[data-preview-image="${i}"]`), spaceId, q.image);
    renderInteractive(list.querySelector(`[data-preview="${i}"]`), { type: q.type, view: previewView(q) }, { onChange: () => {} });
  });
  $('previewTitle').textContent = $('exTitle').value.trim() || 'Bài làm trực tiếp';
  const limit = $('exUnlimited').checked ? 'Không giới hạn thời gian' : `${$('exTimeLimit').value || '?'} phút`;
  $('previewMeta').textContent = `${state.questions.length} câu · ${formatScore(totalPoints(state.questions))} điểm · ${limit}`;
  $('previewModal').classList.remove('hidden');
}

// ============================================================ bài làm của học viên (chấm)

async function loadAttempts() {
  if (!state.exerciseId) return;
  const panel = $('attemptsPanel');
  try {
    state.attempts = await API.exercises.attempts(spaceId, state.exerciseId) || [];
  } catch (err) {
    if (state.tab === 'attempts') panel.innerHTML = `<div class="kz-item text-center">${escapeHtml(friendlyErrorMessage(err))}</div>`;
    return;
  }
  $('tabAttempts').textContent = `Bài làm của học viên${state.attempts.length ? ` (${state.attempts.length})` : ''}`;
  if (state.tab === 'attempts') renderAttempts();
}

function renderAttempts() {
  const panel = $('attemptsPanel');
  if (!state.attempts.length) {
    panel.innerHTML = '<div class="kz-card p-6 text-center text-on-surface-variant">Học viên chưa làm bài này.</div>';
    return;
  }
  // Mặc định mở lượt đang chờ chấm (nếu có), không thì lượt mới nhất
  const sel = state.attempts.find((a) => a.attemptId === state.selectedAttempt)
    || state.attempts.find((a) => a.status === 'SUBMITTED')
    || state.attempts[state.attempts.length - 1];
  state.selectedAttempt = sel.attemptId;
  const name = sel.learnerName || 'Học viên';
  const initials = name.split(/\s+/).slice(-2).map((w) => w.charAt(0)).join('').toUpperCase();
  const pending = state.attempts.filter((a) => a.status === 'SUBMITTED').length;
  panel.innerHTML = `
    <div class="flex flex-col lg:flex-row gap-5 items-start">
      <aside class="kz-card p-3.5 w-full lg:w-72 shrink-0 space-y-2 lg:sticky lg:top-4">
        <div class="flex items-center gap-2.5 px-1 pb-2">
          <div class="w-9 h-9 rounded-full bg-primary-fixed text-on-primary-fixed-variant flex items-center justify-center font-semibold">${escapeHtml(initials || 'HV')}</div>
          <div class="min-w-0"><p class="font-semibold truncate">${escapeHtml(name)}</p>
            <p class="font-caption text-caption text-on-surface-variant">${state.attempts.length} lượt${pending ? ` · ${pending} chờ chấm` : ''}</p></div>
        </div>
        ${state.attempts.map((a) => {
          const chosen = a.attemptId === sel.attemptId;
          const score = a.finalScore !== null && a.finalScore !== undefined
            ? `${formatScore(a.finalScore)} / ${formatScore(a.maxScore)}`
            : (a.status === 'SUBMITTED' ? '<span class="kz-badge" data-tone="paused">Chờ chấm</span>' : '');
          return `
          <button type="button" data-attempt="${escapeHtml(a.attemptId)}" class="w-full text-left p-3 rounded-xl border ${chosen ? 'border-2 border-primary bg-surface-container-low' : 'border-surface-container-high bg-surface-container-lowest hover:bg-surface-container-low'}">
            <span class="flex items-center justify-between gap-2 font-semibold"><span>Lượt ${a.attemptNumber}</span><span>${score}</span></span>
            <span class="block font-caption text-caption text-on-surface-variant mt-0.5">${escapeHtml(attemptStatusLabel(a.status))} · ${a.status === 'IN_PROGRESS' ? `bắt đầu ${escapeHtml(formatDateTime(a.startedAt))}` : `nộp ${escapeHtml(formatDateTime(a.submittedAt))}${a.late ? ' · muộn' : ''}`}</span>
          </button>`;
        }).join('')}
      </aside>
      <section id="attemptDetail" class="flex-1 min-w-0 w-full space-y-3"></section>
    </div>`;
  panel.querySelectorAll('[data-attempt]').forEach((b) => b.addEventListener('click', () => {
    state.selectedAttempt = b.dataset.attempt;
    renderAttempts();
  }));
  renderAttemptDetail(sel);
}

function renderAttemptDetail(a) {
  const box = $('attemptDetail');
  if (a.status === 'IN_PROGRESS') {
    box.innerHTML = `<div class="kz-card p-6 text-on-surface-variant">Học viên đang làm lượt này${a.deadlineAt ? `, hạn chót ${escapeHtml(formatDateTime(a.deadlineAt))}` : ''}. Khi học viên nộp bài (hoặc hết giờ) bạn mới chấm được.</div>`;
    return;
  }
  // Phân loại từng câu để lọc: cần chấm (tự luận chưa có điểm), sai (máy chấm sai), còn lại
  const kinds = a.questions.map((q) => {
    if (q.needsManualGrading && (q.score === null || q.score === undefined)) return 'pending';
    if (q.correct === false) return 'wrong';
    return 'ok';
  });
  const count = (k) => kinds.filter((x) => x === k).length;
  if (state.gradeFilter === 'pending' && !count('pending')) state.gradeFilter = count('wrong') ? 'wrong' : 'all';
  if (state.gradeFilter === 'wrong' && !count('wrong')) state.gradeFilter = 'all';
  const nextPending = state.attempts.find((x) => x.status === 'SUBMITTED' && x.attemptId !== a.attemptId);
  const chip = (key, label, n) => `<button type="button" class="ex-chip" data-filter="${key}" data-active="${state.gradeFilter === key}">${label} (${n})</button>`;
  box.innerHTML = `
    <div class="kz-card p-5 flex flex-wrap items-center gap-3">
      <div class="flex-1 min-w-[240px]">
        <p class="font-title-md text-title-md">Lượt ${a.attemptNumber} · ${escapeHtml(attemptStatusLabel(a.status))}</p>
        <p class="font-body-sm text-body-sm text-on-surface-variant">Máy chấm: ${formatScore(a.autoScore)} điểm${a.finalScore !== null && a.finalScore !== undefined ? ` · Điểm cuối: ${formatScore(a.finalScore)} / ${formatScore(a.maxScore)}` : ` · còn ${count('pending')} câu chờ chấm`}${a.submitReason === 'TIMEOUT' ? ' · tự nộp khi hết giờ' : ''}${a.submitReason === 'CLOSED' ? ' · tự nộp khi đóng đề' : ''}</p>
      </div>
      <button type="button" id="saveGradeBtn" class="kz-btn ${nextPending ? '' : 'kz-btn-primary'}">Lưu điểm</button>
      ${nextPending ? `<button type="button" id="saveNextBtn" class="kz-btn kz-btn-primary">Lưu &amp; chấm lượt ${nextPending.attemptNumber}<span class="material-symbols-outlined text-[18px]" aria-hidden="true">arrow_forward</span></button>` : ''}
    </div>
    <p id="gradeError" class="field-error hidden"></p>
    <div class="flex flex-wrap gap-2" role="group" aria-label="Lọc câu">
      ${chip('pending', 'Cần chấm', count('pending'))}${chip('wrong', 'Câu sai', count('wrong'))}${chip('all', 'Tất cả', a.questions.length)}
    </div>
    <ol class="flex flex-col gap-3">
      ${a.questions.map((q, i) => {
        const status = kinds[i] === 'pending' ? 'Chờ chấm'
          : (q.correct === true ? 'Máy chấm: đúng' : q.correct === false ? 'Máy chấm: sai' : 'Gia sư đã chấm');
        const border = { pending: 'border-l-[#e8a35c]', wrong: 'border-l-[#e37b7b]', ok: 'border-l-[#4b9d6c]' }[kinds[i]];
        const shown = state.gradeFilter === 'all' || state.gradeFilter === kinds[i];
        // Ô điểm chỉ hiện điểm gia sư đã sửa; để trống = giữ điểm máy (hiện mờ trong ô).
        const tutorEdited = !q.needsManualGrading && q.score !== null && q.score !== undefined
          && Number(q.score) !== Number(q.autoScore);
        const scoreValue = q.needsManualGrading ? q.score : (tutorEdited ? q.score : null);
        return `
        <li class="kz-card p-5 space-y-2 border-l-4 ${border} ${shown ? '' : 'hidden'}" data-grade="${escapeHtml(q.questionId)}">
          <div class="flex flex-wrap items-center gap-2">
            <span class="font-title-sm text-title-sm">Câu ${i + 1}</span>
            <span class="font-caption text-caption text-on-surface-variant">${escapeHtml(questionTypeLabel(q.type))} · ${formatScore(q.points)} điểm</span>
            ${q.audio ? '<span class="kz-badge" style="background:#dce1ff;color:#1d3989"><span class="material-symbols-outlined text-[15px]" aria-hidden="true">headphones</span>Câu nghe</span>' : ''}
            <span class="kz-badge" data-tone="${kinds[i] === 'pending' ? 'paused' : q.correct === false ? 'paused' : 'active'}">${escapeHtml(status)}</span>
          </div>
          ${q.prompt ? `<p class="whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(q.prompt)}</p>` : ''}
          ${q.image ? '<div data-grade-image></div>' : ''}
          ${q.audio ? '<div data-grade-audio></div>' : ''}
          <p class="[overflow-wrap:anywhere]"><span class="font-semibold">Học viên trả lời:</span> ${escapeHtml(describeResponse(q.type, q.view, q.response))}</p>
          ${describeCorrect(q.type, q.view, q.correctAnswer) ? `<p class="text-emerald-800 [overflow-wrap:anywhere]"><span class="font-semibold">Đáp án:</span> ${escapeHtml(describeCorrect(q.type, q.view, q.correctAnswer))}</p>` : ''}
          <div class="grid gap-2 sm:grid-cols-[160px_1fr]">
            <label class="font-caption text-caption">Điểm (0–${formatScore(q.points)})
              <input type="number" min="0" max="${escapeHtml(q.points)}" step="0.25" data-score class="kz-input" value="${scoreValue === null || scoreValue === undefined ? '' : escapeHtml(scoreValue)}" placeholder="${q.needsManualGrading ? 'Chưa chấm' : `Máy chấm ${formatScore(q.autoScore)}`}">
            </label>
            <label class="font-caption text-caption">Nhận xét
              <input type="text" maxlength="5000" data-comment class="kz-input" value="${escapeHtml(q.tutorComment || '')}">
            </label>
          </div>
        </li>`;
      }).join('')}
    </ol>
    ${kinds.some((k, i) => state.gradeFilter === 'all' || state.gradeFilter === k) ? ''
      : '<p class="kz-card p-6 text-center text-on-surface-variant">Không có câu nào trong mục này.</p>'}`;
  box.querySelectorAll('[data-grade]').forEach((li, i) => {
    const q = a.questions[i];
    if (q?.audio) mountAudioPlayer(li.querySelector('[data-grade-audio]'), spaceId, q.audio);
    if (q?.image) mountQuestionImage(li.querySelector('[data-grade-image]'), spaceId, q.image, { maxHeight: 240 });
  });
  // Lọc chỉ ẩn / hiện thẻ câu (mọi câu vẫn được gửi khi lưu điểm, giữ nguyên điểm đã nhập)
  box.querySelectorAll('[data-filter]').forEach((b) => b.addEventListener('click', () => {
    state.gradeFilter = b.dataset.filter;
    box.querySelectorAll('[data-filter]').forEach((x) => { x.dataset.active = String(x === b); });
    box.querySelectorAll('[data-grade]').forEach((li, i) => {
      li.classList.toggle('hidden', !(state.gradeFilter === 'all' || state.gradeFilter === kinds[i]));
    });
  }));
  $('saveGradeBtn').addEventListener('click', () => saveGrades(a));
  $('saveNextBtn')?.addEventListener('click', () => saveGrades(a, nextPending?.attemptId));
}

/** Lưu điểm lượt a; nextAttemptId: chuyển sang chấm lượt đó sau khi lưu xong. */
async function saveGrades(a, nextAttemptId = null) {
  const items = [...$('attemptDetail').querySelectorAll('[data-grade]')].map((li) => {
    const raw = li.querySelector('[data-score]').value.trim();
    return {
      questionId: li.dataset.grade,
      score: raw === '' ? null : Number(raw.replace(',', '.')),
      comment: li.querySelector('[data-comment]').value
    };
  });
  const bad = items.find((it, i) => it.score !== null && (!Number.isFinite(it.score) || it.score < 0 || it.score > Number(a.questions[i].points)));
  if (bad) {
    setFieldError('gradeError', 'Có câu có điểm không hợp lệ (phải từ 0 đến điểm tối đa của câu).');
    return;
  }
  $('saveGradeBtn').disabled = true;
  try {
    const updated = await API.exercises.grade(spaceId, state.exerciseId, a.attemptId, items);
    state.attempts = state.attempts.map((x) => (x.attemptId === updated.attemptId ? updated : x));
    showToast(updated.status === 'GRADED' ? 'Đã lưu điểm. Học viên đã thấy điểm.' : 'Đã lưu. Còn câu tự luận chưa chấm.');
    if (nextAttemptId) {
      state.selectedAttempt = nextAttemptId;
      state.gradeFilter = 'pending';
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
    renderAttempts();
  } catch (err) {
    setFieldError('gradeError', err.fieldErrors?.grade || friendlyErrorMessage(err));
    $('saveGradeBtn').disabled = false;
  }
}

// ============================================================ khởi tạo

function init() {
  const root = $('editorRoot');
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
  if (role !== ROLE_TUTOR) {
    root.innerHTML = roleDeniedHtml('Soạn bài làm trực tiếp', 'gia sư', role);
    root.querySelector('[data-switch-account]')?.addEventListener('click', clearSessionAndGoToLogin);
    return;
  }
  if (!spaceId) {
    $('editorMain').innerHTML = '<div class="kz-card p-6 text-center">Không tìm thấy lớp học.</div>';
    return;
  }
  // Sidebar dùng chung của gia sư, mục "Quản lý bài tập" đang mở
  mountTutorLayout('assignments');
  initImport();
  $('backLink').href = classLink();
  $('prevLink').href = PREV_PAGE;
  $('prevLabel').textContent = `Quay lại ${pageLabel(PREV_PAGE)}`;
  fillClassCrumb();
  $('addTypeButtons').innerHTML = QUESTION_TYPES.map((t) => `
    <button type="button" class="ex-type-btn" data-add-type="${t.value}" title="${escapeHtml(t.hint)}">
      <span class="material-symbols-outlined text-[18px] text-primary" aria-hidden="true">${TYPE_ICONS[t.value] || 'help'}</span>
      <span class="truncate">${escapeHtml(t.label)}</span>
    </button>`).join('');
  $('addTypeButtons').addEventListener('click', (e) => {
    const b = e.target.closest('[data-add-type]');
    if (b) addQuestion(b.dataset.addType);
  });
  $('questionOutline').addEventListener('click', (e) => {
    const b = e.target.closest('[data-select]');
    if (b) selectQuestion(Number(b.dataset.select));
  });
  // Thanh dưới: bấm "n câu chưa có file nghe" / "n câu chưa hoàn chỉnh" → mở câu đầu tiên bị thiếu
  $('footerStats').addEventListener('click', (e) => {
    const b = e.target.closest('[data-goto]');
    if (!b) return;
    const auto = autoMode();
    const idx = state.questions.findIndex((q) => (b.dataset.goto === 'missing'
      ? needsAudio(q)
      : (() => { const err = validateQuestion(q, { autoMode: auto }); return err && err !== AUDIO_MISSING_MESSAGE; })()));
    if (idx < 0) return;
    if (state.tab !== 'questions') switchTab('questions');
    selectQuestion(idx);
    $('questionList').scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
  $('questionList').addEventListener('input', onListInput);
  $('questionList').addEventListener('change', onListChange);
  $('questionList').addEventListener('click', onListClick);
  $('exUnlimited').addEventListener('change', () => { $('exTimeLimit').disabled = state.locked || $('exUnlimited').checked; });
  // Mọi chỉnh sửa ở tab Câu hỏi / Cài đặt bài (gõ, chọn, tải tệp) đều tính là có thay đổi chưa lưu
  ['questionsPanel', 'settingsPanel'].forEach((id) => {
    ['input', 'change'].forEach((type) => $(id).addEventListener(type, () => { if (!state.locked || id === 'settingsPanel') state.dirty = true; }, true));
  });
  $('saveDraftBtn').addEventListener('click', () => save(false));
  $('publishBtn').addEventListener('click', () => save(true));
  $('previewBtn').addEventListener('click', openPreview);
  $('previewClose').addEventListener('click', () => $('previewModal').classList.add('hidden'));
  $('deleteBtn').addEventListener('click', removeExercise);
  $('tabQuestions').addEventListener('click', () => switchTab('questions'));
  $('tabSettings').addEventListener('click', () => switchTab('settings'));
  $('tabAttempts').addEventListener('click', () => switchTab('attempts'));
  ['exTitle', 'exInstructions', 'exOpens', 'exCloses', 'exTimeLimit', 'exAttempts'].forEach((id) => $(id).addEventListener('input', () => { state.dirty = true; }));
  // Rời trang khi còn thay đổi chưa lưu (nút Quay lại, đường dẫn): hỏi lại trước
  document.querySelectorAll('#prevLink, #editorRoot header nav a').forEach((link) => link.addEventListener('click', async (e) => {
    if (!state.dirty) return;
    e.preventDefault();
    if (await confirmBox('Rời trang?', state.status === 'PUBLISHED'
      ? 'Các thay đổi chưa gửi cho học viên sẽ bị mất. Bấm "Cập nhật cho học viên" để giữ lại.'
      : 'Các thay đổi chưa lưu sẽ bị mất. Bấm "Lưu nháp" để giữ lại.', 'Rời trang')) {
      state.dirty = false;
      window.location.href = link.getAttribute('href');
    }
  }));
  window.addEventListener('beforeunload', (e) => {
    if (state.dirty) {
      e.preventDefault();
      e.returnValue = '';
    }
  });
  load();
}

if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
else init();
