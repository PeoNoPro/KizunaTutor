/**
 * KizunaTutor - Xử lý sự cố thanh toán của Quản trị viên (FE-09: UC-98).
 * Kiểm tra với cổng chỉ đổi trạng thái giao dịch đang chờ; giao dịch đã thành công/thất bại chỉ được ghi nhật ký.
 */

import { API, ApiError } from '/js/api.js';
import { mountStaffLayout } from '/components/staff-layout/staff-layout.js';
import { renderPagination } from '/components/pagination/pagination.js';
import { escapeHtml, formatMoney, formatDate, formatDateTime, statusBadge, toast } from '/js/membership-format.js';


const $ = (id) => document.getElementById(id);
const state = { page: 0, size: 20, detailId: null, busy: false };

function show(id) {
  ['loadingState', 'errorState', 'contentState'].forEach((s) => $(s).classList.toggle('hidden', s !== id));
}

function errorText(err) {
  return err instanceof ApiError ? err.message : 'Hệ thống đang bận, bạn vui lòng thử lại sau.';
}

function filters() {
  return {
    keyword: $('keyword').value.trim(),
    status: $('statusFilter').value,
    from: $('fromDate').value,
    to: $('toDate').value
  };
}

async function load(page = 0) {
  show('loadingState');
  try {
    const data = await API.adminPayments.getTransactions({ ...filters(), page, size: state.size });
    state.page = data.page;
    $('rowsBody').innerHTML = data.items.map((tx) => `
      <tr class="border-t border-surface-container">
        <td class="px-4 py-3 whitespace-nowrap">${formatDateTime(tx.transactionTime)}</td>
        <td class="px-4 py-3 font-mono text-[12px]">${escapeHtml(tx.txnRef)}</td>
        <td class="px-4 py-3"><p class="font-semibold">${escapeHtml(tx.tutorName)}</p><p class="text-on-surface-variant">${escapeHtml(tx.email)}</p></td>
        <td class="px-4 py-3">${escapeHtml(tx.planName)}</td>
        <td class="px-4 py-3 text-right">${formatMoney(tx.amount)}</td>
        <td class="px-4 py-3">${statusBadge(tx.status, tx.statusLabel)}</td>
        <td class="px-4 py-3 text-right"><button type="button" class="tv-btn-small" data-detail="${tx.transactionId}">Xem chi tiết</button></td>
      </tr>`).join('');
    const empty = data.items.length === 0;
    const f = filters();
    $('emptyState').classList.toggle('hidden', !empty);
    $('emptyState').textContent = (f.keyword || f.status || f.from || f.to)
      ? 'Không tìm thấy kết quả phù hợp. Vui lòng điều chỉnh bộ lọc.'
      : 'Chưa có giao dịch gói hội viên nào.';
    renderPager(data);
    $('rowsBody').querySelectorAll('[data-detail]').forEach((b) => b.addEventListener('click', () => openDetail(b.dataset.detail)));
    show('contentState');
  } catch (err) {
    $('errorMessage').textContent = errorText(err);
    show('errorState');
  }
}

function renderPager(data) {
  renderPagination($('pager'), { ...data, size: state.size }, {
    onPage: (p) => load(p),
    onSize: (s) => { state.size = s; load(0); }
  });
}

function row(label, value) {
  return `<dt class="text-on-surface-variant">${escapeHtml(label)}</dt><dd class="break-all">${value}</dd>`;
}

async function openDetail(transactionId) {
  state.detailId = transactionId;
  $('detailDrawer').classList.remove('hidden');
  const body = $('detailBody');
  body.innerHTML = '<div class="tv-skeleton h-48 rounded-xl"></div>';
  try {
    const d = await API.adminPayments.getDetail(transactionId);
    const tx = d.transaction;
    const logs = d.logs.length
      ? `<ol class="flex flex-col gap-3">${d.logs.map((l) => `
          <li class="rounded-lg bg-surface-container-low p-3">
            <p class="text-label-md">${escapeHtml(l.typeLabel)} <span class="text-on-surface-variant font-normal">· ${formatDateTime(l.loggedAt)}</span></p>
            ${l.summary ? `<p class="text-body-sm text-on-surface-variant mt-1">${escapeHtml(l.summary)}</p>` : ''}
          </li>`).join('')}</ol>`
      : '<p class="text-body-sm text-on-surface-variant">Chưa có nhật ký.</p>';
    const notes = d.notes.length
      ? `<ul class="flex flex-col gap-2">${d.notes.map((n) => `
          <li class="rounded-lg bg-surface-container-low p-3">
            <p class="text-body-sm whitespace-pre-line">${escapeHtml(n.note)}</p>
            <p class="text-caption text-on-surface-variant mt-1">${escapeHtml(n.adminName)} · ${formatDateTime(n.createdAt)}</p>
          </li>`).join('')}</ul>`
      : '<p class="text-body-sm text-on-surface-variant">Chưa có ghi chú.</p>';

    body.innerHTML = `
      <dl class="grid grid-cols-[auto,1fr] gap-x-4 gap-y-2 text-body-sm rounded-xl bg-surface-container-low p-4">
        ${row('Mã giao dịch', `<span class="font-mono">${escapeHtml(tx.txnRef)}</span>`)}
        ${row('Mã phía cổng', escapeHtml(tx.gatewayTransactionId || '—'))}
        ${row('Gia sư', `${escapeHtml(tx.tutorName)} · ${escapeHtml(tx.email)}`)}
        ${row('Gói', escapeHtml(tx.planName))}
        ${row('Số tiền', formatMoney(tx.amount))}
        ${row('Thời gian tạo', formatDateTime(tx.transactionTime))}
        ${row('Trạng thái', statusBadge(tx.status, tx.statusLabel))}
        ${row('Gói đã kích hoạt', d.membership ? `${formatDate(d.membership.startDate)} – ${formatDate(d.membership.endDate)}` : 'Chưa có')}
      </dl>
      <button id="gatewayCheckButton" type="button" class="tv-btn tv-btn-primary mt-4 inline-flex items-center gap-2">
        <span class="material-symbols-outlined text-[18px]">sync</span>Kiểm tra với cổng thanh toán
      </button>
      <h3 class="text-title-md mt-6 mb-2">Nhật ký xử lý</h3>
      ${logs}
      <h3 class="text-title-md mt-6 mb-2">Ghi chú điều tra</h3>
      ${notes}
      <form id="noteForm" class="mt-3 flex flex-col gap-2" novalidate>
        <label for="noteInput" class="text-label-md">Thêm ghi chú (gia sư không thấy nội dung này)</label>
        <textarea id="noteInput" rows="3" maxlength="2000" class="rounded-lg border border-outline-variant px-3 py-2 bg-surface-container-lowest"></textarea>
        <p id="noteError" class="hidden text-body-sm text-error"></p>
        <button type="submit" class="tv-btn self-end">Lưu ghi chú</button>
      </form>`;
    $('gatewayCheckButton').addEventListener('click', () => gatewayCheck(transactionId));
    $('noteForm').addEventListener('submit', (event) => saveNote(event, transactionId));
  } catch (err) {
    body.innerHTML = `<p class="text-body-md text-error">${escapeHtml(errorText(err))}</p>
      <button type="button" class="tv-btn mt-3" id="detailRetry">Thử lại</button>`;
    $('detailRetry').addEventListener('click', () => openDetail(transactionId));
  }
}

async function gatewayCheck(transactionId) {
  if (state.busy) return;
  state.busy = true;
  const button = $('gatewayCheckButton');
  button.disabled = true;
  try {
    const res = await API.adminPayments.checkWithGateway(transactionId);
    toast(res.message, 'success');
    await openDetail(transactionId);
    load(state.page);
  } catch (err) {
    toast(errorText(err), 'error');
    button.disabled = false;
  } finally {
    state.busy = false;
  }
}

async function saveNote(event, transactionId) {
  event.preventDefault();
  const note = $('noteInput').value.trim();
  if (!note) {
    $('noteError').textContent = 'Vui lòng nhập nội dung ghi chú.';
    $('noteError').classList.remove('hidden');
    return;
  }
  try {
    const res = await API.adminPayments.addNote(transactionId, note);
    toast(res.message, 'success');
    await openDetail(transactionId);
  } catch (err) {
    $('noteError').textContent = errorText(err);
    $('noteError').classList.remove('hidden');
  }
}

function closeDetail() {
  $('detailDrawer').classList.add('hidden');
  state.detailId = null;
}

if (mountStaffLayout('admin', 'payments', 'Quản lý thanh toán')) {
$('filterForm').addEventListener('submit', (event) => {
  event.preventDefault();
  const { from, to } = filters();
  if (from && to && to < from) {
    toast('Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.', 'error');
    return;
  }
  load(0);
});
$('resetButton').addEventListener('click', () => {
  ['keyword', 'statusFilter', 'fromDate', 'toDate'].forEach((id) => { $(id).value = ''; });
  load(0);
});
$('retryButton').addEventListener('click', () => load(state.page));
$('closeDetail').addEventListener('click', closeDetail);
$('detailDrawer').addEventListener('click', (event) => { if (event.target.id === 'detailDrawer') closeDetail(); });
document.addEventListener('keydown', (event) => { if (event.key === 'Escape') closeDetail(); });
load(0);
}
