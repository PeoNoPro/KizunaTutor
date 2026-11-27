/**
 * KizunaTutor - Danh sách tệp có nút "＋ Thêm tệp", "Thay", "Xóa" cho từng tệp (FE-06/FE-07).
 * Dùng cho: tệp đề bài (giao/sửa bài tập), tài liệu học tập, bài nộp của học viên.
 *
 * - Tệp đã có trên máy chủ (existing): Thay → chọn tệp khác thay đúng tệp đó; Xóa → đánh dấu sẽ xóa (có Hoàn tác).
 * - Tệp mới chọn: Thay / Xóa ngay trong danh sách.
 * - Ô <input type="file"> ẩn; chỉ thao tác qua các nút. Chưa gửi gì lên máy chủ cho tới khi bấm Lưu của form.
 * getChanges() trả về: newFiles (tệp cần tải lên, gồm cả tệp thay thế), removeIds (id tệp cũ cần xóa, gồm cả tệp bị thay),
 * keptCount (số tệp cũ giữ nguyên), total (tổng số tệp sau khi lưu).
 */

import { escapeHtml, formatFileSize, fileTypeLabel, validateMaterialFile, MAX_FILES_PER_UPLOAD } from './classroom-helpers.js';

const BTN = 'inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-[12px] font-semibold border transition-colors';
const BTN_NEUTRAL = `${BTN} border-slate-300 bg-white text-slate-700 hover:bg-slate-100`;
const BTN_DANGER = `${BTN} border-red-200 bg-white text-red-600 hover:bg-red-50`;

/**
 * @param {HTMLElement} container nơi vẽ danh sách + nút thêm
 * @param {{accept?: string, max?: number, addLabel?: string, onChange?: Function}} opts
 */
export function createFileList(container, { accept = '', max = MAX_FILES_PER_UPLOAD, addLabel = 'Thêm tệp', onChange } = {}) {
  /** @type {{kind:'existing'|'new', fileId?:string, name:string, size:number, type?:string, file?:File, removed?:boolean, replacement?:File}[]} */
  let items = [];
  let limit = max;
  let canRemoveExisting = true;
  let disabled = false;

  // Ô chọn tệp ẩn: một cho "Thêm", một cho "Thay"
  const addInput = Object.assign(document.createElement('input'), { type: 'file', multiple: true, accept, hidden: true });
  const replaceInput = Object.assign(document.createElement('input'), { type: 'file', accept, hidden: true });
  let replaceIndex = -1;

  const total = () => items.filter((it) => !it.removed).length;

  addInput.addEventListener('change', () => {
    Array.from(addInput.files || []).forEach((f) => {
      const dup = items.some((it) => it.kind === 'new' && it.file.name === f.name && it.file.size === f.size);
      if (!dup && total() < limit) items.push({ kind: 'new', name: f.name, size: f.size, file: f });
    });
    addInput.value = '';
    render();
  });

  replaceInput.addEventListener('change', () => {
    const f = replaceInput.files?.[0];
    const it = items[replaceIndex];
    if (f && it) {
      if (it.kind === 'existing') {
        it.replacement = f;
        it.removed = false;
      } else {
        Object.assign(it, { name: f.name, size: f.size, file: f });
      }
    }
    replaceInput.value = '';
    replaceIndex = -1;
    render();
  });

  function rowHtml(it, i) {
    const size = escapeHtml(formatFileSize(it.replacement ? it.replacement.size : it.size));
    // Tệp mới chọn (hoặc tệp thay thế) sai định dạng / quá dung lượng: tô đỏ kèm lý do ngay trong danh sách
    const picked = it.kind === 'new' ? it.file : it.replacement;
    const problem = picked && !it.removed ? validateMaterialFile(picked, { required: true }) : null;
    let label;
    let actions;
    if (it.kind === 'existing' && it.removed) {
      label = `<span class="line-through text-slate-400">📎 ${escapeHtml(it.name)}</span> <span class="text-[11px] font-semibold text-red-600">Sẽ xóa khi lưu</span>`;
      actions = `<button type="button" class="${BTN_NEUTRAL}" data-undo="${i}">Hoàn tác</button>`;
    } else if (it.kind === 'existing' && it.replacement) {
      label = `<span class="line-through text-slate-400">${escapeHtml(it.name)}</span> → 📎 ${escapeHtml(it.replacement.name)}
        <span class="text-[11px] text-slate-500">(${size})</span> <span class="text-[11px] font-semibold text-sky-700">Sẽ thay khi lưu</span>`;
      actions = `<button type="button" class="${BTN_NEUTRAL}" data-replace="${i}">Chọn tệp khác</button>
        <button type="button" class="${BTN_NEUTRAL}" data-undo="${i}">Hoàn tác</button>`;
    } else {
      const badge = it.kind === 'new' ? ' <span class="text-[11px] font-semibold text-emerald-700">Mới</span>' : '';
      const typeText = it.type ? `${escapeHtml(fileTypeLabel(it.type))}, ` : '';
      label = `📎 ${escapeHtml(it.name)} <span class="text-[11px] text-slate-500">(${typeText}${size})</span>${badge}`;
      const canRemove = it.kind === 'new' || canRemoveExisting;
      actions = `<button type="button" class="${BTN_NEUTRAL}" data-replace="${i}">Thay</button>
        ${canRemove ? `<button type="button" class="${BTN_DANGER}" data-remove="${i}">Xóa</button>` : ''}`;
    }
    return `
      <li class="flex flex-wrap items-center gap-2 rounded-lg border ${problem ? 'border-red-300 bg-red-50' : 'border-slate-200 bg-white'} px-2.5 py-2">
        <span class="flex-1 min-w-0 [overflow-wrap:anywhere]">${label}
          ${problem ? `<span class="block text-[11px] font-semibold text-red-600 mt-0.5">⚠ ${escapeHtml(problem)} — bấm "Thay" hoặc "Xóa" tệp này.</span>` : ''}</span>
        ${disabled ? '' : `<span class="flex items-center gap-1.5 shrink-0">${actions}</span>`}
      </li>`;
  }

  function render() {
    const full = total() >= limit;
    container.innerHTML = `
      ${items.length ? `<ul class="flex flex-col gap-1.5">${items.map(rowHtml).join('')}</ul>` : ''}
      ${disabled ? '' : `
        <div class="flex flex-wrap items-center gap-2 ${items.length ? 'mt-2' : ''}">
          <button type="button" data-add-file class="${BTN} border-dashed border-sky-400 bg-sky-50 text-sky-800 hover:bg-sky-100 px-3 py-1.5 text-[13px] disabled:opacity-50 disabled:cursor-not-allowed" ${full ? 'disabled' : ''}>
            <span class="text-[16px] leading-none">＋</span> ${escapeHtml(addLabel)}
          </button>
          <span class="text-[11px] text-slate-500">${total()}/${limit} tệp${full ? ' — đã đủ số tệp tối đa' : ''}</span>
        </div>`}`;
    container.append(addInput, replaceInput);
    container.querySelector('[data-add-file]')?.addEventListener('click', () => {
      addInput.multiple = limit - total() > 1;
      addInput.click();
    });
    container.querySelectorAll('[data-replace]').forEach((b) => b.addEventListener('click', () => {
      replaceIndex = Number(b.dataset.replace);
      replaceInput.click();
    }));
    container.querySelectorAll('[data-remove]').forEach((b) => b.addEventListener('click', () => {
      const i = Number(b.dataset.remove);
      if (items[i].kind === 'new') items.splice(i, 1);
      else items[i].removed = true;
      render();
    }));
    container.querySelectorAll('[data-undo]').forEach((b) => b.addEventListener('click', () => {
      const it = items[Number(b.dataset.undo)];
      it.removed = false;
      it.replacement = null;
      render();
    }));
    onChange?.(api.getChanges());
  }

  const api = {
    /**
     * Đặt lại danh sách.
     * @param {{fileId?:string, fileName:string, fileSize?:number, fileType?:string}[]} existing tệp đã có trên máy chủ
     * @param {{max?: number, canRemoveExisting?: boolean, disabled?: boolean, addLabel?: string}} opts
     */
    reset(existing = [], { max: newMax, canRemoveExisting: removable = true, disabled: off = false, addLabel: label } = {}) {
      items = (existing || []).map((f) => ({
        kind: 'existing', fileId: f.fileId, name: f.fileName, size: f.fileSize || 0, type: f.fileType
      }));
      if (newMax) limit = newMax;
      if (label) addLabel = label;
      canRemoveExisting = removable;
      disabled = off;
      render();
    },
    getChanges() {
      const newFiles = [];
      const removeIds = [];
      let keptCount = 0;
      items.forEach((it) => {
        if (it.kind === 'new') newFiles.push(it.file);
        else if (it.removed) removeIds.push(it.fileId);
        else if (it.replacement) {
          newFiles.push(it.replacement);
          removeIds.push(it.fileId);
        } else keptCount += 1;
      });
      return { newFiles, removeIds, keptCount, total: keptCount + newFiles.length };
    }
  };
  return api;
}
