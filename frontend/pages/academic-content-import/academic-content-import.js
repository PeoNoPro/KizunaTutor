/**
 * KizunaTutor - Cán bộ học vụ: kho học liệu (Kanji, từ vựng, ngữ pháp).
 * Ba tab dùng chung thẻ chọn loại:
 *  - Danh sách: tìm kiếm, lọc JLPT, sắp xếp, xem chi tiết một mục.
 *  - Nhập từ Excel: "Kiểm tra tệp" (dryRun) → "Nhập vào hệ thống". Chỉ thêm mục mới, mục đã có giữ nguyên.
 *  - Lịch sử nhập: các lần nhập thật, xem chi tiết các mục đã thêm.
 * API: /api/academic/content-imports/(summary | {type}/items | {type} | history | history/{id})
 */

import { API, ApiError } from '/js/api.js';
import { debounce, showToast } from '/js/main.js';
import { mountAcademicLayout } from '/components/academic-layout/academic-layout.js';
import { renderPagination, PAGE_SIZE_OPTIONS } from '/components/pagination/pagination.js';
import {
  CONTENT_TYPES, findContentType, validateImportFile, formatCount, reportStats, canConfirmImport,
  issueLevelLabel, reportHint, LIST_COLUMNS, DETAIL_FIELDS, JLPT_FILTERS, formatDateTime, displayValue, historySummary
} from '/js/content-import-helpers.js';

const TONE = {
  success: 'bg-[#e3f4e8] text-success',
  info: 'bg-primary-fixed text-primary',
  muted: 'bg-surface-container-low text-on-surface',
  warning: 'bg-tertiary-fixed text-tertiary',
  error: 'bg-error-container text-error'
};

const SEARCH_HINT = {
  kanji: 'Tìm theo chữ Kanji, Hán Việt, âm đọc hoặc nghĩa...',
  vocabulary: 'Tìm theo từ, cách đọc, romaji, Hán Việt hoặc nghĩa...',
  grammar: 'Tìm theo mẫu ngữ pháp, mã, romaji hoặc ý nghĩa...'
};

const state = {
  type: CONTENT_TYPES[0].type,
  tab: 'list',
  summary: null,
  list: { page: 0, size: PAGE_SIZE_OPTIONS[0], items: [], requestId: 0 },
  history: { page: 0, size: PAGE_SIZE_OPTIONS[0], requestId: 0 },
  file: null,
  report: null,
  busy: false
};

const $ = (id) => document.getElementById(id);

const SKELETON_ROWS = 5;

/** Khung chờ tải cho bảng (constitution II.4): vài dòng xám nhấp nháy thay cho dữ liệu. */
function showSkeleton(rowsId, stateId, columnCount) {
  $(stateId).classList.add('hidden');
  const cell = '<td class="px-4 py-3"><div class="h-4 rounded bg-surface-container animate-pulse"></div></td>';
  $(rowsId).innerHTML = Array.from({ length: SKELETON_ROWS },
    () => `<tr class="border-t border-surface-container" aria-hidden="true">${cell.repeat(columnCount)}</tr>`).join('');
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text == null ? '' : String(text);
  return div.innerHTML;
}

// ===== Thẻ loại học liệu + tab =====

function summaryOf(type) {
  const apiType = findContentType(type).apiType;
  return state.summary?.types?.find((t) => t.type === apiType) || null;
}

function renderTypeCards() {
  $('typeCards').innerHTML = CONTENT_TYPES.map((t) => {
    const active = t.type === state.type;
    const total = summaryOf(t.type);
    let count = '<span class="inline-block h-3 w-20 rounded bg-surface-container animate-pulse align-middle"></span>';
    if (total) count = `Đang có ${formatCount(total.total)} mục`;
    else if (state.summaryFailed) count = 'Chưa tải được số mục';
    return `
      <button type="button" role="radio" aria-checked="${active}" data-type="${t.type}"
              class="flex items-center gap-3 rounded-xl p-4 text-left border-2 transition-colors ${active ? 'border-[#006194] bg-primary-fixed/40' : 'border-transparent bg-surface-container-lowest shadow-sm hover:bg-surface-container-low'}">
        <span class="inline-flex items-center justify-center w-10 h-10 rounded-lg bg-primary-fixed text-primary">
          <span class="material-symbols-outlined">${t.icon}</span>
        </span>
        <span>
          <span class="block text-title-sm font-semibold">${t.label}</span>
          <span class="block text-caption text-on-surface-variant">${count}</span>
        </span>
      </button>`;
  }).join('');
  renderColumnInfo();
}

function renderTabs() {
  document.querySelectorAll('.content-tab').forEach((tab) => {
    const active = tab.dataset.tab === state.tab;
    tab.setAttribute('aria-selected', String(active));
    tab.className = `content-tab px-4 py-2.5 text-label-md -mb-px border-b-2 ${active ? 'border-[#006194] text-[#006194] font-semibold' : 'border-transparent text-on-surface-variant hover:text-on-surface'}`;
  });
  $('panelList').classList.toggle('hidden', state.tab !== 'list');
  $('panelImport').classList.toggle('hidden', state.tab !== 'import');
  $('panelHistory').classList.toggle('hidden', state.tab !== 'history');
}

function loadActiveTab() {
  if (state.tab === 'list') loadList();
  if (state.tab === 'history') loadHistory();
}

async function loadSummary() {
  try {
    state.summary = await API.academicContent.getSummary();
    state.summaryFailed = false;
  } catch (err) {
    state.summaryFailed = true;
    if (!(err instanceof ApiError && err.status === 401)) showToast(err.message || 'Không tải được số liệu kho học liệu.', 'error');
  }
  renderTypeCards();
}

// ===== Tab Danh sách =====

function renderListFilters() {
  $('listKeyword').placeholder = SEARCH_HINT[state.type];
  $('listJlpt').innerHTML = JLPT_FILTERS.map((l) => `<option value="${l}">${l || 'Mọi cấp độ'}</option>`).join('');
}

function showListState(icon, title, retry = false) {
  $('listRows').innerHTML = '';
  $('listState').classList.remove('hidden');
  $('listStateIcon').textContent = icon;
  $('listStateTitle').textContent = title;
  $('listRetry').classList.toggle('hidden', !retry);
}

async function loadList() {
  const columns = LIST_COLUMNS[state.type];
  $('listHead').innerHTML = `<tr>${columns.map((c) => `<th class="px-4 py-3 font-medium whitespace-nowrap">${c.label}</th>`).join('')}
    <th class="px-4 py-3 font-medium whitespace-nowrap">Ngày thêm</th></tr>`;
  const requestId = ++state.list.requestId;
  showSkeleton('listRows', 'listState', columns.length + 1);
  try {
    const page = await API.academicContent.getItems(state.type, {
      keyword: $('listKeyword').value.trim(), jlpt: $('listJlpt').value, sort: $('listSort').value,
      page: state.list.page, size: state.list.size
    });
    if (requestId !== state.list.requestId) return;
    state.list.items = page.items || [];
    if (state.list.items.length === 0) {
      showListState('search_off', 'Không có mục nào phù hợp.');
    } else {
      $('listState').classList.add('hidden');
      $('listRows').innerHTML = state.list.items.map((item, index) => `
        <tr data-index="${index}" tabindex="0" class="border-t border-surface-container hover:bg-surface-container-low cursor-pointer align-top">
          ${columns.map((c) => `<td class="px-4 py-3 ${c.className || ''}">${escapeHtml(displayValue(c.key, item[c.key]))}</td>`).join('')}
          <td class="px-4 py-3 whitespace-nowrap text-on-surface-variant">${formatDateTime(item.createdAt)}</td>
        </tr>`).join('');
    }
    renderPagination($('listPager'), page, {
      onPage: (p) => { state.list.page = p; loadList(); },
      onSize: (s) => { state.list.size = s; state.list.page = 0; loadList(); }
    });
  } catch (err) {
    if (requestId !== state.list.requestId || (err instanceof ApiError && err.status === 401)) return;
    $('listPager').innerHTML = '';
    showListState('cloud_off', err.message || 'Không tải được danh sách.', true);
  }
}

function reloadList() {
  state.list.page = 0;
  loadList();
}

function openItemDetail(item) {
  const t = findContentType(state.type);
  const main = item.character || item.word || item.pattern;
  $('detailTitle').textContent = `${t.label}: ${main}`;
  $('detailSubtitle').textContent = item.reading && item.reading !== item.word ? item.reading : '';
  $('detailBody').innerHTML = `<dl class="grid grid-cols-1 sm:grid-cols-[180px_1fr] gap-x-4 gap-y-3">
    ${DETAIL_FIELDS[state.type].map(([key, label]) => `
      <dt class="text-label-md text-on-surface-variant">${label}</dt>
      <dd class="text-body-md whitespace-pre-line break-words">${escapeHtml(displayValue(key, item[key]))}</dd>`).join('')}
  </dl>`;
  openModal();
}

// ===== Tab Nhập từ Excel =====

function renderColumnInfo() {
  const t = findContentType(state.type);
  const info = summaryOf(state.type);
  if (!info) {
    $('columnInfo').textContent = '';
    return;
  }
  const chips = (cols, cls) => cols.map((c) => `<span class="inline-block px-2 py-0.5 rounded ${cls} mr-1 mb-1">${escapeHtml(c)}</span>`).join('');
  $('columnInfo').innerHTML = `
    <p class="text-label-md mb-2">Cột bắt buộc của tệp ${t.label}</p>
    <div>${chips(info.requiredColumns, 'bg-primary-fixed text-primary')}</div>
    <p class="text-label-md mt-3 mb-2">Cột không bắt buộc</p>
    <div>${chips(info.optionalColumns, 'bg-surface-container text-on-surface-variant')}</div>
    <p class="text-caption text-on-surface-variant mt-3">Tên cột không phân biệt dấu, chữ hoa và phần trong ngoặc. Tệp mẫu: <span class="font-medium">${escapeHtml(t.sample)}</span>.
    ${state.type === 'kanji' ? ' Chữ Kanji chưa có trong hệ thống cần thêm cột Số nét.' : ''}</p>`;
}

function setFile(file) {
  state.file = file || null;
  const error = file ? validateImportFile(file) : null;
  $('fileName').textContent = file ? file.name : 'Bấm để chọn hoặc kéo tệp .xlsx vào đây';
  $('fileError').textContent = error || '';
  $('fileError').classList.toggle('hidden', !error);
  clearReport();
}

function clearReport() {
  state.report = null;
  $('reportSection').classList.add('hidden');
}

function setBusy(busy, button, label) {
  state.busy = busy;
  $('checkButton').disabled = busy;
  $('confirmButton').disabled = busy;
  if (button) button.querySelector('span:last-child').textContent = label;
}

async function runImport(dryRun) {
  const error = validateImportFile(state.file);
  if (error) {
    $('fileError').textContent = error;
    $('fileError').classList.remove('hidden');
    return;
  }
  const button = dryRun ? $('checkButton') : $('confirmButton');
  const idle = dryRun ? 'Kiểm tra tệp' : 'Nhập vào hệ thống';
  setBusy(true, button, dryRun ? 'Đang kiểm tra...' : 'Đang nhập...');
  try {
    state.report = await API.academicContent.importFile(state.type, state.file, { dryRun });
    renderReport();
    if (!dryRun) {
      showToast(state.report.message, 'success');
      loadSummary();
    }
  } catch (err) {
    if (!(err instanceof ApiError && err.status === 401)) showToast(err.message || 'Không nhập được tệp. Vui lòng thử lại.', 'error');
  } finally {
    setBusy(false, button, idle);
  }
}

function renderReport() {
  const r = state.report;
  if (!r) return;
  $('reportSection').classList.remove('hidden');
  $('reportTitle').textContent = r.dryRun ? `Kết quả kiểm tra tệp ${r.typeLabel}` : `Đã nhập ${r.typeLabel}`;
  $('reportMeta').textContent = `${r.fileName} · Sheet đã đọc: ${r.sheets.join(', ')}`;
  $('reportBadge').textContent = r.dryRun ? 'Chưa ghi vào hệ thống' : 'Đã ghi vào hệ thống';
  $('reportBadge').className = `px-2.5 py-1 rounded-full text-label-sm ${r.dryRun ? TONE.warning : TONE.success}`;

  $('reportStats').innerHTML = reportStats(r).map((s) => `
    <div class="rounded-lg p-3 ${TONE[s.tone]}">
      <p class="text-headline-sm font-bold">${formatCount(s.value)}</p>
      <p class="text-caption">${s.label}</p>
    </div>`).join('');

  $('reportHint').innerHTML = r.dryRun
    ? escapeHtml(reportHint(r))
    : `${escapeHtml(r.message)} <button type="button" data-goto="history" class="text-[#006194] underline">Xem trong lịch sử nhập</button>`;

  const issues = r.issues || [];
  $('issueBlock').classList.toggle('hidden', issues.length === 0);
  $('issueRows').innerHTML = issues.map((i) => `
    <tr class="border-t border-surface-container align-top">
      <td class="px-3 py-2"><span class="px-2 py-0.5 rounded-full text-caption ${i.level === 'ERROR' ? TONE.error : TONE.warning}">${issueLevelLabel(i.level)}</span></td>
      <td class="px-3 py-2 whitespace-nowrap">${escapeHtml(i.sheet)}</td>
      <td class="px-3 py-2">${i.row || ''}</td>
      <td class="px-3 py-2">${escapeHtml(i.message)}</td>
    </tr>`).join('');
  $('issueTruncated').classList.toggle('hidden', !r.issuesTruncated);
  $('confirmButton').classList.toggle('hidden', !canConfirmImport(r));
  $('reportSection').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// ===== Tab Lịch sử nhập =====

function showHistoryState(title, retry = false) {
  $('historyRows').innerHTML = '';
  $('historyState').classList.remove('hidden');
  $('historyStateTitle').textContent = title;
  $('historyRetry').classList.toggle('hidden', !retry);
}

async function loadHistory() {
  const requestId = ++state.history.requestId;
  showSkeleton('historyRows', 'historyState', 5);
  try {
    const page = await API.academicContent.getHistory({ type: state.type, page: state.history.page, size: state.history.size });
    if (requestId !== state.history.requestId) return;
    const items = page.items || [];
    if (items.length === 0) {
      showHistoryState(`Chưa có lần nhập ${findContentType(state.type).label} nào.`);
    } else {
      $('historyState').classList.add('hidden');
      $('historyRows').innerHTML = items.map((h) => `
        <tr class="border-t border-surface-container align-top">
          <td class="px-4 py-3 whitespace-nowrap">${formatDateTime(h.createdAt)}</td>
          <td class="px-4 py-3">
            <p class="text-label-md">${escapeHtml(h.actorName || 'Không rõ')}</p>
            <p class="text-caption text-on-surface-variant">${escapeHtml(h.actorEmail || '')}</p>
          </td>
          <td class="px-4 py-3 break-all">${escapeHtml(h.fileName || '—')}</td>
          <td class="px-4 py-3">${escapeHtml(historySummary(h))}</td>
          <td class="px-4 py-3 text-right">
            <button type="button" data-history="${h.id}" class="px-3 py-1.5 rounded-lg bg-surface-container text-label-sm hover:bg-surface-container-high whitespace-nowrap">Xem chi tiết</button>
          </td>
        </tr>`).join('');
    }
    renderPagination($('historyPager'), page, {
      onPage: (p) => { state.history.page = p; loadHistory(); },
      onSize: (s) => { state.history.size = s; state.history.page = 0; loadHistory(); }
    });
  } catch (err) {
    if (requestId !== state.history.requestId || (err instanceof ApiError && err.status === 401)) return;
    $('historyPager').innerHTML = '';
    showHistoryState(err.message || 'Không tải được lịch sử nhập.', true);
  }
}

async function openHistoryDetail(id) {
  try {
    const detail = await API.academicContent.getHistoryDetail(id);
    const h = detail.item;
    $('detailTitle').textContent = `Lần nhập ${h.typeLabel} lúc ${formatDateTime(h.createdAt)}`;
    $('detailSubtitle').textContent = `${h.fileName || ''} · ${h.actorName || 'Không rõ'}`;
    const stats = [
      ['Dòng dữ liệu đọc được', h.totalRows], ['Đã thêm mới', h.created], ['Đã có, giữ nguyên', h.skippedExisting],
      ['Trùng lặp trong tệp', h.duplicatesInFile], ['Dòng lỗi bị bỏ qua', h.invalidRows]
    ];
    const added = detail.addedItems || [];
    $('detailBody').innerHTML = `
      <div class="grid grid-cols-2 sm:grid-cols-5 gap-2">
        ${stats.map(([label, value]) => `<div class="rounded-lg p-3 bg-surface-container-low"><p class="text-title-md font-bold">${formatCount(value)}</p><p class="text-caption">${label}</p></div>`).join('')}
      </div>
      <h3 class="text-title-sm font-semibold mt-2">Các mục đã thêm</h3>
      ${added.length === 0
        ? '<p class="text-body-sm text-on-surface-variant">Lần nhập này không thêm mục nào.</p>'
        : `<div class="flex flex-wrap gap-1.5">${added.map((a) => `<span class="px-2 py-1 rounded bg-[#e3f4e8] text-body-sm">${escapeHtml(a)}</span>`).join('')}</div>`}
      ${detail.addedTruncated ? '<p class="text-caption text-on-surface-variant">Chỉ lưu 2.000 mục đầu tiên của lần nhập này.</p>' : ''}`;
    openModal();
  } catch (err) {
    if (!(err instanceof ApiError && err.status === 401)) showToast(err.message || 'Không tải được chi tiết lần nhập.', 'error');
  }
}

// ===== Hộp thoại =====

function openModal() {
  $('detailModal').classList.remove('hidden');
  $('detailClose').focus();
}

function closeModal() {
  $('detailModal').classList.add('hidden');
}

// ===== Sự kiện =====

function bindEvents() {
  $('typeCards').addEventListener('click', (e) => {
    const card = e.target.closest('[data-type]');
    if (!card || state.busy || card.dataset.type === state.type) return;
    state.type = card.dataset.type;
    state.list.page = 0;
    state.history.page = 0;
    renderTypeCards();
    renderListFilters();
    $('listKeyword').value = '';
    // Tệp đã chọn thuộc loại cũ: bỏ chọn để không kiểm tra nhầm loại
    $('fileInput').value = '';
    setFile(null);
    loadActiveTab();
  });

  document.querySelectorAll('.content-tab').forEach((tab) => tab.addEventListener('click', () => switchTab(tab.dataset.tab)));
  $('reportHint').addEventListener('click', (e) => {
    if (e.target.closest('[data-goto="history"]')) switchTab('history');
  });

  // Danh sách
  $('listFilter').addEventListener('submit', (e) => { e.preventDefault(); reloadList(); });
  $('listKeyword').addEventListener('input', debounce(reloadList, 400));
  $('listJlpt').addEventListener('change', reloadList);
  $('listSort').addEventListener('change', reloadList);
  $('listRetry').addEventListener('click', loadList);
  $('listRows').addEventListener('click', (e) => {
    const row = e.target.closest('tr[data-index]');
    if (row) openItemDetail(state.list.items[Number(row.dataset.index)]);
  });
  $('listRows').addEventListener('keydown', (e) => {
    const row = e.target.closest('tr[data-index]');
    if (row && (e.key === 'Enter' || e.key === ' ')) {
      e.preventDefault();
      openItemDetail(state.list.items[Number(row.dataset.index)]);
    }
  });

  // Nhập tệp
  $('fileInput').addEventListener('change', (e) => setFile(e.target.files[0]));
  const zone = $('dropZone');
  ['dragenter', 'dragover'].forEach((name) => zone.addEventListener(name, (e) => {
    e.preventDefault();
    zone.classList.add('bg-surface-container-low');
  }));
  ['dragleave', 'drop'].forEach((name) => zone.addEventListener(name, (e) => {
    e.preventDefault();
    zone.classList.remove('bg-surface-container-low');
  }));
  zone.addEventListener('drop', (e) => setFile(e.dataTransfer.files[0]));
  $('checkButton').addEventListener('click', () => runImport(true));
  $('confirmButton').addEventListener('click', () => runImport(false));
  $('resetButton').addEventListener('click', () => {
    $('fileInput').value = '';
    setFile(null);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });

  // Lịch sử
  $('historyRetry').addEventListener('click', loadHistory);
  $('historyRows').addEventListener('click', (e) => {
    const btn = e.target.closest('[data-history]');
    if (btn) openHistoryDetail(btn.dataset.history);
  });

  // Hộp thoại
  $('detailClose').addEventListener('click', closeModal);
  $('detailModal').addEventListener('click', (e) => { if (e.target === $('detailModal')) closeModal(); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeModal(); });
}

function switchTab(tab) {
  if (!['list', 'import', 'history'].includes(tab)) return;
  state.tab = tab;
  renderTabs();
  loadActiveTab();
  try {
    const url = new URL(window.location.href);
    url.searchParams.set('tab', tab);
    window.history.replaceState(null, '', url);
  } catch (e) { }
}

mountAcademicLayout('content-import', 'Kho học liệu');
bindEvents();
const initialTab = new URLSearchParams(window.location.search).get('tab');
state.tab = ['list', 'import', 'history'].includes(initialTab) ? initialTab : 'list';
renderTypeCards();
renderListFilters();
renderTabs();
loadActiveTab();
loadSummary();
