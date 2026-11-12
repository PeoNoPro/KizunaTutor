/**
 * KizunaTutor - FE-07 Gia sư soạn bài làm trực tiếp: /pages/exercise-editor/index.html?space={spaceId}[&exercise={exerciseId}][&tab=bai-lam]
 * - Cài đặt: tiêu đề, hướng dẫn, mở / đóng đề, thời gian làm bài (hoặc không giới hạn), số lượt làm, cách chấm, cho xem đáp án.
 * - Soạn câu hỏi: 8 dạng câu, đổi thứ tự, nhân bản, xóa; mỗi câu có đáp án và điểm.
 * - Lưu nháp (học viên không thấy), Xem thử (như học viên, không lưu), Giao bài (kiểm tra đủ đáp án).
 * - Đã có học viên làm bài: khóa câu hỏi, đáp án, thời gian làm bài, cách chấm; vẫn sửa được phần còn lại.
 * - Tab "Bài làm": xem từng lượt, sửa điểm từng câu, chấm câu tự luận, nhận xét.
 */

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
  questionPayload,
  totalPoints,
  previewView,
  describeResponse,
  describeCorrect,
  toLocalInput,
  fromLocalInput,
  validateExerciseSettings
} from '../../js/exercise-helpers.js';
import { renderInteractive } from '../../js/exercise-player.js';

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
  tab: params.get('tab') === 'bai-lam' ? 'attempts' : 'edit',
  dirty: false
};

const INPUT = 'kz-input';
const SMALL_BTN = 'kz-btn px-2.5 py-1.5';

function classLink() {
  return `/tutor/classrooms?space=${encodeURIComponent(spaceId || '')}`;
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
    $('loadingBox').classList.add('hidden');
    renderAll();
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
      type: q.type, prompt: q.prompt || '', points: Number(q.points), data: q.data || {}
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

function fillSettings(detail) {
  const s = detail?.summary;
  $('exTitle').value = s?.title || '';
  $('exInstructions').value = detail?.instructions || '';
  $('exOpens').value = toLocalInput(s?.opensAt);
  $('exCloses').value = toLocalInput(s?.closesAt);
  $('exUnlimited').checked = s ? !s.timeLimitMinutes : false;
  $('exTimeLimit').value = s?.timeLimitMinutes || (s ? '' : 30);
  $('exAttempts').value = s?.maxAttempts || 1;
  document.querySelector(`input[name="exMode"][value="${s?.gradingMode || 'AUTO'}"]`).checked = true;
  $('exShowAnswers').checked = s ? s.showAnswers : true;
}

function autoMode() {
  return document.querySelector('input[name="exMode"]:checked')?.value !== 'TUTOR';
}

// ============================================================ vẽ trang

function renderAll() {
  const s = state.detail?.summary;
  $('pageTitle').textContent = state.exerciseId ? 'Bài làm trực tiếp' : 'Soạn bài làm trực tiếp';
  const badge = $('statusBadge');
  if (s) {
    badge.textContent = exerciseStateLabel(s.state);
    badge.dataset.tone = exerciseStateTone(s.state);
    badge.classList.remove('hidden');
  } else badge.classList.add('hidden');
  const published = state.status === 'PUBLISHED';
  $('saveDraftBtn').classList.toggle('hidden', published);
  $('publishBtn').textContent = published ? 'Lưu thay đổi' : 'Giao bài';
  $('deleteBtn').classList.toggle('hidden', !state.exerciseId || state.locked);
  $('lockedNote').classList.toggle('hidden', !state.locked);
  $('exUnlimited').disabled = state.locked;
  $('exTimeLimit').disabled = state.locked || $('exUnlimited').checked;
  document.querySelectorAll('input[name="exMode"]').forEach((r) => { r.disabled = state.locked; });
  $('addQuestionBar').classList.toggle('hidden', state.locked);
  $('tabAttempts').classList.toggle('hidden', !state.exerciseId);
  $('tabAttempts').textContent = `Bài làm của học viên${state.attempts.length ? ` (${state.attempts.length})` : ''}`;
  switchTab(state.tab);
  renderQuestions();
}

function switchTab(tab) {
  state.tab = tab;
  const edit = tab === 'edit';
  $('editPanel').classList.toggle('hidden', !edit);
  $('attemptsPanel').classList.toggle('hidden', edit);
  $('tabEdit').dataset.active = String(edit);
  $('tabAttempts').dataset.active = String(!edit);
  $('editorActions').classList.toggle('hidden', !edit);
  if (!edit) {
    // Đã có sẵn dữ liệu bài làm (tải cùng lúc khi mở trang) thì vẽ ngay, không gọi lại máy chủ
    if (state.attemptsPreloaded) renderAttempts();
    else {
      $('attemptsPanel').innerHTML = '<div class="kz-card p-6 text-center text-on-surface-variant">Đang tải bài làm…</div>';
      loadAttempts();
    }
  }
}

function renderQuestions() {
  const list = $('questionList');
  list.innerHTML = state.questions.map((q, i) => cardHtml(q, i)).join('')
    || '<li class="kz-item text-center text-on-surface-variant py-6">Chưa có câu hỏi nào. Chọn dạng câu rồi bấm "Thêm câu hỏi".</li>';
  updateTotals();
}

function updateTotals() {
  $('totalsText').textContent = `${state.questions.length} câu · ${formatScore(totalPoints(state.questions))} điểm`;
}

function dis() {
  return state.locked ? 'disabled' : '';
}

function cardHtml(q, i) {
  const n = state.questions.length;
  return `
    <li class="kz-card p-5 space-y-4" data-q="${i}">
      <div class="flex flex-wrap items-center gap-2">
        <span class="font-title-sm text-title-sm">Câu ${i + 1}</span>
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
      <div data-body>${bodyHtml(q, i)}</div>
      <p data-error class="field-error hidden"></p>
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
    case 'FILL_BLANK':
    case 'WORD_BANK':
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
    case 'WRITTEN':
      return `
        <div>
          <label class="kz-label">Đáp án được chấp nhận <span class="font-normal text-on-surface-variant">(phân cách bằng dấu ; — để trống nếu gia sư tự chấm)</span></label>
          <input data-field="accepted" value="${escapeHtml((d.accepted || []).join('; '))}" class="${INPUT}" placeholder="Ví dụ: ありがとう; ありがとうございます" ${dis()}>
          <p class="font-caption text-caption text-on-surface-variant mt-1">Có đáp án: hệ thống tự chấm (bỏ qua khoảng trắng, hoa/thường, chữ toàn/nửa độ rộng). Không có đáp án: chờ gia sư chấm (cần chọn cách chấm "Gia sư chấm").</p>
        </div>
        <div>
          <label class="kz-label">Đáp án mẫu <span class="font-normal text-on-surface-variant">(không bắt buộc, học viên xem sau khi nộp nếu được cho xem đáp án)</span></label>
          <textarea data-field="sample" rows="2" maxlength="2000" class="${INPUT}" ${dis()}>${escapeHtml(d.sample || '')}</textarea>
        </div>`;
    default:
      return '';
  }
}

function blanksHtml(q) {
  const d = q.data || {};
  const n = countBlanks(d.text);
  if (!n) return '<p class="font-caption text-caption text-on-surface-variant">Chưa có chỗ trống. Gõ ___ (3 dấu gạch dưới) vào câu để tạo chỗ trống.</p>';
  if (q.type === 'FILL_BLANK') {
    return `
      <p class="kz-label">Đáp án từng chỗ trống <span class="font-normal text-on-surface-variant">(nhiều đáp án được chấp nhận thì phân cách bằng ;)</span></p>
      <div class="flex flex-col gap-2">
        ${Array.from({ length: n }, (_, i) => `
          <label class="flex items-center gap-2"><span class="w-10 font-bold text-on-surface-variant">(${i + 1})</span>
            <input data-blank="${i}" value="${escapeHtml(((d.answers || [])[i] || []).join('; '))}" class="${INPUT}" placeholder="Ví dụ: たべます; 食べます" ${dis()}></label>`).join('')}
      </div>`;
  }
  return `
    <p class="kz-label">Từ đúng của từng chỗ trống</p>
    <div class="flex flex-col gap-2">
      ${Array.from({ length: n }, (_, i) => `
        <label class="flex items-center gap-2"><span class="w-10 font-bold text-on-surface-variant">(${i + 1})</span>
          <input data-bankword="${i}" value="${escapeHtml((d.answers || [])[i] || '')}" class="${INPUT}" ${dis()}></label>`).join('')}
    </div>
    <label class="kz-label mt-3">Từ gây nhiễu <span class="font-normal text-on-surface-variant">(không bắt buộc, phân cách bằng ;)</span></label>
    <input data-field="distractors" value="${escapeHtml((d.distractors || []).join('; '))}" class="${INPUT}" placeholder="Ví dụ: あれ; それ" ${dis()}>`;
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
    if (countBlanks(d.text) !== before) card.querySelector('[data-blanks]').innerHTML = blanksHtml(q);
  } else if (t.dataset.field === 'accepted') d.accepted = splitList(t.value);
  else if (t.dataset.field === 'sample') d.sample = t.value;
  else if (t.dataset.field === 'distractors') d.distractors = splitList(t.value);
  else if (t.dataset.opt !== undefined) d.options[Number(t.dataset.opt)] = t.value;
  else if (t.dataset.blank !== undefined) {
    d.answers = d.answers || [];
    d.answers[Number(t.dataset.blank)] = splitList(t.value);
  } else if (t.dataset.bankword !== undefined) {
    d.answers = d.answers || [];
    d.answers[Number(t.dataset.bankword)] = t.value;
  } else if (t.dataset.pair !== undefined) d.pairs[Number(t.dataset.pair)][t.dataset.side] = t.value;
  else if (t.dataset.item !== undefined) d.items[Number(t.dataset.item)] = t.value;
}

async function onListChange(e) {
  const card = e.target.closest('[data-q]');
  if (!card) return;
  const i = Number(card.dataset.q);
  const q = state.questions[i];
  const t = e.target;
  state.dirty = true;
  if (t.dataset.field === 'type') {
    const hasContent = JSON.stringify(q.data) !== JSON.stringify(defaultQuestion(q.type).data) || q.prompt;
    if (hasContent && !(await confirmBox('Đổi dạng câu?', 'Đáp án đã nhập của câu này sẽ bị xóa.', 'Đổi dạng câu'))) {
      t.value = q.type;
      return;
    }
    state.questions[i] = { ...defaultQuestion(t.value), prompt: q.prompt, points: q.points };
    renderQuestions();
  } else if (t.dataset.correct !== undefined) {
    const j = Number(t.dataset.correct);
    if (q.type === 'MULTI_CHOICE') {
      const set = new Set(q.data.correct || []);
      if (t.checked) set.add(j); else set.delete(j);
      q.data.correct = [...set].sort((a, b) => a - b);
    } else q.data.correct = j;
  } else if (t.dataset.tf !== undefined) q.data.correct = t.dataset.tf === 'true';
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
    case 'up': if (i > 0) swap(state.questions, i, i - 1); break;
    case 'down': if (i < state.questions.length - 1) swap(state.questions, i, i + 1); break;
    case 'duplicate': state.questions.splice(i + 1, 0, JSON.parse(JSON.stringify(q))); break;
    case 'remove': state.questions.splice(i, 1); break;
    case 'opt-add': d.options.push(''); break;
    case 'opt-remove':
      d.options.splice(j, 1);
      if (q.type === 'SINGLE_CHOICE') d.correct = d.correct === j ? null : (d.correct > j ? d.correct - 1 : d.correct);
      else d.correct = (d.correct || []).filter((c) => c !== j).map((c) => (c > j ? c - 1 : c));
      break;
    case 'pair-add': d.pairs.push({ left: '', right: '' }); break;
    case 'pair-remove': d.pairs.splice(j, 1); break;
    case 'item-add': d.items.push(''); break;
    case 'item-remove': d.items.splice(j, 1); break;
    case 'item-up': if (j > 0) swap(d.items, j, j - 1); break;
    case 'item-down': if (j < d.items.length - 1) swap(d.items, j, j + 1); break;
    default: return;
  }
  renderQuestions();
}

function addQuestion() {
  state.questions.push(defaultQuestion($('newQuestionType').value));
  state.dirty = true;
  renderQuestions();
  const cards = $('questionList').querySelectorAll('[data-q]');
  cards[cards.length - 1]?.scrollIntoView({ behavior: 'smooth', block: 'center' });
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

function showQuestionError(index, message) {
  const el = $('questionList').querySelectorAll('[data-q]')[index]?.querySelector('[data-error]');
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
    window.scrollTo({ top: 0, behavior: 'smooth' });
    return;
  }
  if (strict && !state.locked) {
    if (!state.questions.length) {
      setFieldError('formError', 'Bài tập cần có ít nhất một câu hỏi.');
      return;
    }
    let first = -1;
    state.questions.forEach((q, i) => {
      const err = validateQuestion(q, { autoMode: autoMode() });
      if (err) {
        showQuestionError(i, `Câu ${i + 1}: ${err}`);
        if (first < 0) first = i;
      }
    });
    if (first >= 0) {
      setFieldError('formError', 'Có câu hỏi chưa hoàn chỉnh. Hãy sửa các câu được đánh dấu đỏ.');
      $('questionList').querySelectorAll('[data-q]')[first]?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }
  }
  if (publish && state.status !== 'PUBLISHED') {
    const ok = await confirmBox('Giao bài cho học viên?',
      $('exOpens').value ? `Học viên sẽ thấy bài từ ${formatDateTime(fromLocalInput($('exOpens').value))}.` : 'Học viên sẽ thấy bài ngay.', 'Giao bài');
    if (!ok) return;
  }
  const body = {
    title: f.title.trim(),
    instructions: f.instructions.trim(),
    opensAt: fromLocalInput(f.opensLocal),
    closesAt: fromLocalInput(f.closesLocal),
    timeLimitMinutes: f.unlimited ? null : Number(f.timeLimit),
    maxAttempts: Number(f.maxAttempts),
    gradingMode: autoMode() ? 'AUTO' : 'TUTOR',
    showAnswers: $('exShowAnswers').checked,
    publish,
    questions: state.questions.map(questionPayload)
  };
  const buttons = ['saveDraftBtn', 'publishBtn'];
  buttons.forEach((id) => { $(id).disabled = true; });
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
      ? (publish && state.status !== 'PUBLISHED' ? 'Đã giao bài cho học viên.' : 'Đã lưu thay đổi.')
      : 'Đã lưu nháp. Học viên chưa thấy bài này.');
    await load();
  } catch (err) {
    const fe = err.fieldErrors || {};
    showSettingErrors({ title: fe.title, instructions: fe.instructions, closesAt: fe.closesAt,
      timeLimitMinutes: fe.timeLimitMinutes, maxAttempts: fe.maxAttempts });
    const qErr = fe.questions;
    const m = /^Câu (\d+):/.exec(qErr || '');
    if (m) showQuestionError(Number(m[1]) - 1, qErr);
    setFieldError('formError', qErr || friendlyErrorMessage(err));
  } finally {
    buttons.forEach((id) => { $(id).disabled = false; });
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
      <div data-preview="${i}"></div>
    </li>`).join('') || '<li class="kz-item text-center">Chưa có câu hỏi.</li>';
  state.questions.forEach((q, i) => {
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
  const sel = state.attempts.find((a) => a.attemptId === state.selectedAttempt) || state.attempts[state.attempts.length - 1];
  state.selectedAttempt = sel.attemptId;
  panel.innerHTML = `
    <div class="grid gap-5 lg:grid-cols-[280px_1fr]">
      <aside class="kz-card p-4 space-y-2 h-fit">
        <p class="font-label-md text-label-md">${escapeHtml(sel.learnerName || 'Học viên')}</p>
        ${state.attempts.map((a) => `
          <button type="button" data-attempt="${escapeHtml(a.attemptId)}" class="w-full text-left kz-item ${a.attemptId === sel.attemptId ? 'ring-2 ring-primary' : ''}">
            <p class="font-semibold">Lượt ${a.attemptNumber} · ${escapeHtml(attemptStatusLabel(a.status))}</p>
            <p class="font-caption text-caption text-on-surface-variant">${a.status === 'IN_PROGRESS' ? `Bắt đầu ${escapeHtml(formatDateTime(a.startedAt))}` : `Nộp ${escapeHtml(formatDateTime(a.submittedAt))}${a.late ? ' · muộn' : ''}`}</p>
            ${a.finalScore !== null && a.finalScore !== undefined ? `<p class="font-semibold">${formatScore(a.finalScore)} / ${formatScore(a.maxScore)} điểm</p>` : ''}
          </button>`).join('')}
      </aside>
      <section id="attemptDetail" class="space-y-3 min-w-0"></section>
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
  box.innerHTML = `
    <div class="kz-card p-5 flex flex-wrap items-center gap-3">
      <div class="flex-1 min-w-0">
        <p class="font-title-sm text-title-sm">Lượt ${a.attemptNumber} · ${escapeHtml(attemptStatusLabel(a.status))}</p>
        <p class="font-caption text-caption text-on-surface-variant">Máy chấm: ${formatScore(a.autoScore)} điểm${a.finalScore !== null && a.finalScore !== undefined ? ` · Điểm cuối: ${formatScore(a.finalScore)} / ${formatScore(a.maxScore)}` : ' · còn câu chờ chấm'}${a.submitReason === 'TIMEOUT' ? ' · tự nộp khi hết giờ' : ''}${a.submitReason === 'CLOSED' ? ' · tự nộp khi đóng đề' : ''}</p>
      </div>
      <button type="button" id="saveGradeBtn" class="kz-btn kz-btn-primary">Lưu điểm</button>
    </div>
    <p id="gradeError" class="field-error hidden"></p>
    <ol class="flex flex-col gap-3">
      ${a.questions.map((q, i) => {
        const status = q.needsManualGrading && (q.score === null || q.score === undefined) ? 'Chờ chấm'
          : (q.correct === true ? 'Máy chấm: đúng' : q.correct === false ? 'Máy chấm: sai' : 'Tự luận');
        // Ô điểm chỉ hiện điểm gia sư đã sửa; để trống = giữ điểm máy (hiện mờ trong ô).
        const tutorEdited = !q.needsManualGrading && q.score !== null && q.score !== undefined
          && Number(q.score) !== Number(q.autoScore);
        const scoreValue = q.needsManualGrading ? q.score : (tutorEdited ? q.score : null);
        return `
        <li class="kz-card p-5 space-y-2" data-grade="${escapeHtml(q.questionId)}">
          <div class="flex flex-wrap items-center gap-2">
            <span class="font-title-sm text-title-sm">Câu ${i + 1}</span>
            <span class="font-caption text-caption text-on-surface-variant">${escapeHtml(questionTypeLabel(q.type))} · ${formatScore(q.points)} điểm</span>
            <span class="kz-badge" data-tone="${q.correct === true ? 'active' : q.correct === false ? 'paused' : 'done'}">${escapeHtml(status)}</span>
          </div>
          ${q.prompt ? `<p class="whitespace-pre-line [overflow-wrap:anywhere]">${escapeHtml(q.prompt)}</p>` : ''}
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
    </ol>`;
  $('saveGradeBtn').addEventListener('click', () => saveGrades(a));
}

async function saveGrades(a) {
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
  $('backLink').href = classLink();
  $('newQuestionType').innerHTML = QUESTION_TYPES.map((t) => `<option value="${t.value}">${t.label}</option>`).join('');
  $('questionList').addEventListener('input', onListInput);
  $('questionList').addEventListener('change', onListChange);
  $('questionList').addEventListener('click', onListClick);
  $('addQuestionBtn').addEventListener('click', addQuestion);
  $('exUnlimited').addEventListener('change', () => { $('exTimeLimit').disabled = state.locked || $('exUnlimited').checked; });
  $('saveDraftBtn').addEventListener('click', () => save(false));
  $('publishBtn').addEventListener('click', () => save(true));
  $('previewBtn').addEventListener('click', openPreview);
  $('previewClose').addEventListener('click', () => $('previewModal').classList.add('hidden'));
  $('deleteBtn').addEventListener('click', removeExercise);
  $('tabEdit').addEventListener('click', () => switchTab('edit'));
  $('tabAttempts').addEventListener('click', () => switchTab('attempts'));
  ['exTitle', 'exInstructions', 'exOpens', 'exCloses', 'exTimeLimit', 'exAttempts'].forEach((id) => $(id).addEventListener('input', () => { state.dirty = true; }));
  $('backLink').addEventListener('click', async (e) => {
    if (!state.dirty) return;
    e.preventDefault();
    if (await confirmBox('Rời trang?', 'Các thay đổi chưa lưu sẽ bị mất. Bấm "Lưu nháp" để giữ lại.', 'Rời trang')) {
      state.dirty = false;
      window.location.href = classLink();
    }
  });
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
