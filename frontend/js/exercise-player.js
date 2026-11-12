/**
 * KizunaTutor - FE-07 Bài làm trực tiếp: vẽ một câu hỏi để học viên làm (dùng cho trang làm bài và "Xem thử" của gia sư).
 * Mỗi dạng câu trả về câu trả lời dạng JSON giống máy chủ:
 *   SINGLE_CHOICE {choice}, MULTI_CHOICE {choices}, TRUE_FALSE {value}, FILL_BLANK / WORD_BANK {blanks},
 *   MATCHING {matches}, ORDERING {order}, WRITTEN {text}.
 * Điền từ có sẵn: kéo thả hoặc bấm chọn (bấm từ → điền vào chỗ trống đang chọn; bấm chỗ đã điền → trả từ về).
 * Sắp xếp thứ tự: kéo thả hoặc dùng nút lên / xuống (dễ dùng trên điện thoại).
 */

import { escapeHtml } from './classroom-helpers.js';

const CHOICE = 'flex items-start gap-3 w-full text-left rounded-xl border px-4 py-3 transition-colors cursor-pointer';
const CHOICE_OFF = 'border-outline-variant/60 bg-surface-container-lowest hover:bg-surface-container-low';
const CHOICE_ON = 'border-primary bg-primary-fixed/40';
const INPUT = 'rounded-lg bg-surface-container-low px-3 py-2 border border-outline-variant/60 focus:outline-none focus:border-primary focus:bg-surface-container-lowest';

/**
 * @param {HTMLElement} container
 * @param {{type:string, view:object}} item câu hỏi (view = đề cho học viên, không có đáp án)
 * @param {{response?:object, onChange?:Function, disabled?:boolean}} opts
 */
export function renderInteractive(container, item, { response = null, onChange = () => {}, disabled = false } = {}) {
  const view = item.view || {};
  const emit = (r) => onChange(r);
  switch (item.type) {
    case 'SINGLE_CHOICE': return renderChoices(container, view, response, emit, disabled, false);
    case 'MULTI_CHOICE': return renderChoices(container, view, response, emit, disabled, true);
    case 'TRUE_FALSE': return renderTrueFalse(container, response, emit, disabled);
    case 'FILL_BLANK': return renderFillBlank(container, view, response, emit, disabled);
    case 'WORD_BANK': return renderWordBank(container, view, response, emit, disabled);
    case 'MATCHING': return renderMatching(container, view, response, emit, disabled);
    case 'ORDERING': return renderOrdering(container, view, response, emit, disabled);
    case 'WRITTEN': return renderWritten(container, response, emit, disabled);
    default: container.innerHTML = '<p class="text-on-surface-variant">Dạng câu hỏi không được hỗ trợ.</p>';
  }
  return undefined;
}

function letter(i) {
  return String.fromCharCode(65 + i);
}

function renderChoices(container, view, response, emit, disabled, multi) {
  const options = view.options || [];
  let chosen = multi ? new Set(response?.choices || []) : (Number.isInteger(response?.choice) ? response.choice : null);
  const draw = () => {
    container.innerHTML = `
      ${multi ? '<p class="font-caption text-caption text-on-surface-variant mb-2">Chọn tất cả đáp án đúng.</p>' : ''}
      <div class="flex flex-col gap-2">
        ${options.map((o, i) => {
          const on = multi ? chosen.has(i) : chosen === i;
          return `<button type="button" data-i="${i}" ${disabled ? 'disabled' : ''} class="${CHOICE} ${on ? CHOICE_ON : CHOICE_OFF}" aria-pressed="${on}">
            <span class="shrink-0 w-7 h-7 ${multi ? 'rounded-md' : 'rounded-full'} border ${on ? 'bg-primary text-on-primary border-primary' : 'border-outline-variant text-on-surface-variant'} flex items-center justify-center text-[13px] font-bold">${on && multi ? '✓' : letter(i)}</span>
            <span class="pt-0.5 [overflow-wrap:anywhere]">${escapeHtml(o)}</span>
          </button>`;
        }).join('')}
      </div>`;
    container.querySelectorAll('[data-i]').forEach((b) => b.addEventListener('click', () => {
      const i = Number(b.dataset.i);
      if (multi) {
        if (chosen.has(i)) chosen.delete(i); else chosen.add(i);
        emit({ choices: [...chosen].sort((a, c) => a - c) });
      } else {
        chosen = i;
        emit({ choice: i });
      }
      draw();
    }));
  };
  draw();
}

function renderTrueFalse(container, response, emit, disabled) {
  let value = typeof response?.value === 'boolean' ? response.value : null;
  const draw = () => {
    container.innerHTML = `
      <div class="grid grid-cols-2 gap-3 max-w-md">
        ${[[true, 'Đúng'], [false, 'Sai']].map(([v, label]) => `
          <button type="button" data-v="${v}" ${disabled ? 'disabled' : ''} class="${CHOICE} justify-center font-semibold ${value === v ? CHOICE_ON : CHOICE_OFF}" aria-pressed="${value === v}">${label}</button>`).join('')}
      </div>`;
    container.querySelectorAll('[data-v]').forEach((b) => b.addEventListener('click', () => {
      value = b.dataset.v === 'true';
      emit({ value });
      draw();
    }));
  };
  draw();
}

function renderFillBlank(container, view, response, emit, disabled) {
  const segments = view.segments || [''];
  const n = segments.length - 1;
  const blanks = Array.from({ length: n }, (_, i) => (response?.blanks || [])[i] || '');
  container.innerHTML = `
    <p class="leading-[2.6] text-[16px] [overflow-wrap:anywhere]">
      ${segments.map((s, i) => `${escapeHtml(s)}${i < n
        ? `<input type="text" data-b="${i}" ${disabled ? 'disabled' : ''} value="${escapeHtml(blanks[i])}" aria-label="Chỗ trống ${i + 1}"
            class="${INPUT} inline-block w-40 mx-1 py-1 text-center align-baseline" placeholder="(${i + 1})">` : ''}`).join('')}
    </p>`;
  container.querySelectorAll('[data-b]').forEach((input) => input.addEventListener('input', () => {
    blanks[Number(input.dataset.b)] = input.value;
    emit({ blanks: [...blanks] });
  }));
}

function renderWordBank(container, view, response, emit, disabled) {
  const segments = view.segments || [''];
  const n = segments.length - 1;
  const bank = view.bank || [];
  // slot[i] = vị trí từ trong ngân hàng đang nằm ở chỗ trống i (để từ trùng nhau vẫn phân biệt được)
  const slot = Array(n).fill(null);
  (response?.blanks || []).forEach((word, i) => {
    if (i >= n || !word) return;
    const idx = bank.findIndex((w, j) => w === word && !slot.includes(j));
    if (idx >= 0) slot[i] = idx;
  });
  let selected = slot.findIndex((s) => s === null);
  if (selected < 0) selected = 0;

  const emitNow = () => emit({ blanks: slot.map((j) => (j === null ? '' : bank[j])) });
  const place = (bankIndex, blankIndex) => {
    const target = blankIndex ?? (slot[selected] === null ? selected : slot.findIndex((s) => s === null));
    if (target < 0 || target >= n) return;
    const prev = slot.indexOf(bankIndex);
    if (prev >= 0) slot[prev] = null;
    slot[target] = bankIndex;
    const next = slot.findIndex((s) => s === null);
    selected = next >= 0 ? next : target;
    emitNow();
    draw();
  };

  const draw = () => {
    container.innerHTML = `
      <p class="leading-[2.8] text-[16px] [overflow-wrap:anywhere]">
        ${segments.map((s, i) => `${escapeHtml(s)}${i < n ? `<button type="button" data-blank="${i}" ${disabled ? 'disabled' : ''}
            class="inline-flex items-center justify-center min-w-[96px] mx-1 px-3 py-1 rounded-lg border-2 border-dashed align-baseline
              ${slot[i] !== null ? 'border-primary bg-primary-fixed/40 font-semibold' : (i === selected && !disabled ? 'border-primary bg-surface-container-lowest' : 'border-outline-variant bg-surface-container-lowest text-on-surface-variant')}"
            aria-label="Chỗ trống ${i + 1}">${slot[i] !== null ? escapeHtml(bank[slot[i]]) : `(${i + 1})`}</button>` : ''}`).join('')}
      </p>
      <div class="mt-4 rounded-xl bg-surface-container-low p-3">
        <p class="font-caption text-caption text-on-surface-variant mb-2">Kéo từ vào chỗ trống, hoặc bấm chọn chỗ trống rồi bấm từ. Bấm vào chỗ đã điền để trả từ về.</p>
        <div class="flex flex-wrap gap-2">
          ${bank.map((w, j) => {
            const used = slot.includes(j);
            return `<button type="button" data-word="${j}" draggable="${!disabled && !used}" ${disabled || used ? 'disabled' : ''}
              class="px-3 py-1.5 rounded-lg border font-semibold ${used ? 'border-outline-variant/40 text-on-surface-variant/40 bg-surface-container' : 'border-outline-variant bg-surface-container-lowest hover:border-primary cursor-grab'}">${escapeHtml(w)}</button>`;
          }).join('')}
        </div>
      </div>`;
    if (disabled) return;
    container.querySelectorAll('[data-blank]').forEach((b) => {
      const i = Number(b.dataset.blank);
      b.addEventListener('click', () => {
        if (slot[i] !== null) {
          slot[i] = null;
          emitNow();
        }
        selected = i;
        draw();
      });
      b.addEventListener('dragover', (e) => e.preventDefault());
      b.addEventListener('drop', (e) => {
        e.preventDefault();
        const j = Number(e.dataTransfer.getData('text/plain'));
        if (Number.isInteger(j)) place(j, i);
      });
    });
    container.querySelectorAll('[data-word]').forEach((w) => {
      w.addEventListener('click', () => place(Number(w.dataset.word)));
      w.addEventListener('dragstart', (e) => e.dataTransfer.setData('text/plain', w.dataset.word));
    });
  };
  draw();
}

function renderMatching(container, view, response, emit, disabled) {
  const lefts = view.lefts || [];
  const rights = view.rights || [];
  const matches = lefts.map((_, i) => (response?.matches || [])[i] || '');
  container.innerHTML = `
    <p class="font-caption text-caption text-on-surface-variant mb-2">Chọn vế phải tương ứng với từng vế trái.</p>
    <div class="flex flex-col gap-2">
      ${lefts.map((l, i) => `
        <div class="grid grid-cols-1 sm:grid-cols-[1fr_auto_1fr] items-center gap-2 rounded-xl border border-outline-variant/60 bg-surface-container-lowest px-4 py-3">
          <span class="font-semibold [overflow-wrap:anywhere]">${escapeHtml(l)}</span>
          <span class="hidden sm:inline text-on-surface-variant">→</span>
          <select data-m="${i}" ${disabled ? 'disabled' : ''} class="${INPUT} w-full" aria-label="Vế phải của ${escapeHtml(l)}">
            <option value="">— Chọn —</option>
            ${rights.map((r) => `<option value="${escapeHtml(r)}" ${matches[i] === r ? 'selected' : ''}>${escapeHtml(r)}</option>`).join('')}
          </select>
        </div>`).join('')}
    </div>`;
  container.querySelectorAll('[data-m]').forEach((s) => s.addEventListener('change', () => {
    matches[Number(s.dataset.m)] = s.value;
    emit({ matches: [...matches] });
  }));
}

function renderOrdering(container, view, response, emit, disabled) {
  let order = Array.isArray(response?.order) && response.order.length ? [...response.order] : [...(view.items || [])];
  let touched = Array.isArray(response?.order) && response.order.length > 0;
  let dragFrom = null;
  const move = (from, to) => {
    if (to < 0 || to >= order.length || from === to) return;
    const [x] = order.splice(from, 1);
    order.splice(to, 0, x);
    touched = true;
    emit({ order: [...order] });
    draw();
  };
  const draw = () => {
    container.innerHTML = `
      <p class="font-caption text-caption text-on-surface-variant mb-2">${touched ? 'Kéo hoặc dùng nút mũi tên để đổi thứ tự.' : 'Sắp xếp các mục theo thứ tự đúng: kéo thả hoặc dùng nút mũi tên.'}</p>
      <ol class="flex flex-col gap-2">
        ${order.map((it, i) => `
          <li data-row="${i}" draggable="${!disabled}" class="flex items-center gap-3 rounded-xl border border-outline-variant/60 bg-surface-container-lowest px-3 py-2.5 ${disabled ? '' : 'cursor-grab'}">
            <span class="shrink-0 w-7 h-7 rounded-full bg-surface-container text-[13px] font-bold flex items-center justify-center">${i + 1}</span>
            <span class="flex-1 min-w-0 [overflow-wrap:anywhere]">${escapeHtml(it)}</span>
            ${disabled ? '' : `
              <button type="button" data-up="${i}" class="px-2 py-1 rounded-md hover:bg-surface-container" aria-label="Lên" ${i === 0 ? 'disabled' : ''}>▲</button>
              <button type="button" data-down="${i}" class="px-2 py-1 rounded-md hover:bg-surface-container" aria-label="Xuống" ${i === order.length - 1 ? 'disabled' : ''}>▼</button>`}
          </li>`).join('')}
      </ol>`;
    if (disabled) return;
    container.querySelectorAll('[data-up]').forEach((b) => b.addEventListener('click', () => move(Number(b.dataset.up), Number(b.dataset.up) - 1)));
    container.querySelectorAll('[data-down]').forEach((b) => b.addEventListener('click', () => move(Number(b.dataset.down), Number(b.dataset.down) + 1)));
    container.querySelectorAll('[data-row]').forEach((row) => {
      row.addEventListener('dragstart', () => { dragFrom = Number(row.dataset.row); });
      row.addEventListener('dragover', (e) => e.preventDefault());
      row.addEventListener('drop', (e) => {
        e.preventDefault();
        if (dragFrom !== null) move(dragFrom, Number(row.dataset.row));
        dragFrom = null;
      });
    });
  };
  draw();
}

function renderWritten(container, response, emit, disabled) {
  container.innerHTML = `
    <textarea rows="6" maxlength="5000" ${disabled ? 'disabled' : ''} class="${INPUT} w-full" placeholder="Nhập câu trả lời của bạn...">${escapeHtml(response?.text || '')}</textarea>`;
  container.querySelector('textarea').addEventListener('input', (e) => emit({ text: e.target.value }));
}
