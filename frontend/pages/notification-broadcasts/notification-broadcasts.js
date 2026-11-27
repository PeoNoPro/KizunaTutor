import { fetchAPI } from '../../js/api.js';
import { showToast } from '../../js/main.js';
import { mountFrame, element, icon, formatDate, stateBox } from '../../js/pages/notifications.js';

/**
 * FE-12 Story 3–4 (tab "Gửi thông báo" trong trang Thông báo, chỉ Admin và Quản lý):
 * soạn → hộp xác nhận (tóm tắt, số người dự kiến) → gửi ngay, hẹn giờ hoặc khẩn toàn hệ thống; kèm lịch sử gửi.
 * Mỗi lần mở hộp xác nhận tạo một mã thao tác; bấm gửi hai lần hoặc mạng gửi lại vẫn chỉ tạo một đợt.
 */
const PATH = '/admin/operations/notification-broadcasts';
const api = (path, method = 'GET', body) => fetchAPI(path, {method, body, csrfPath: PATH + '/csrf'});
const $ = id => document.getElementById(id);
const state = {page: 0, submissionKey: null, payload: null, sending: false};
const STATUS_TONES = {SCHEDULED: 'info', QUEUED: 'warning', SENDING: 'info', COMPLETED: 'success', PARTIAL: 'warning', FAILED: 'danger', NEEDS_REVIEW: 'warning', NO_RECIPIENTS: 'neutral', CANCELLED: 'neutral'};
const form = $('broadcastForm');

const newKey = () => (crypto.randomUUID ? crypto.randomUUID() : '10000000-1000-4000-8000-100000000000'.replace(/[018]/g, c => (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)));
const checked = name => [...form.querySelectorAll(`input[name=${name}]:checked`)].map(input => input.value);
const statusBadge = (status, label) => { const node = element('span', label, 'nt-status'); node.dataset.tone = STATUS_TONES[status] || 'neutral'; return node; };

function showFormError(message) {
  const box = $('bcFormError');
  box.textContent = message; box.hidden = !message;
  if (message) box.focus();
}

/** Giờ nhập theo giờ Việt Nam (UTC+7), gửi lên máy chủ kèm múi giờ. */
function scheduledAt() {
  if (form.elements.timing.value !== 'later' || form.elements.targetMode.value === 'ALL') return null;
  const value = $('bcScheduledAt').value;
  if (!value) throw new Error('Vui lòng chọn thời điểm hẹn gửi.');
  return value + ':00+07:00';
}

function payload() {
  const urgent = form.elements.targetMode.value === 'ALL';
  return {title: form.elements.title.value, content: form.elements.content.value, type: form.elements.type.value,
    targetMode: urgent ? 'ALL' : 'ROLES', roles: urgent ? [] : checked('roles'), channels: checked('channels'), urgent, scheduledAt: scheduledAt()};
}

/** Khẩn thì không chọn vai trò, không hẹn giờ và luôn có kênh trong ứng dụng. */
function syncForm() {
  const urgent = form.elements.targetMode.value === 'ALL';
  $('bcRoles').setAttribute('aria-disabled', String(urgent));
  $('bcRoles').querySelectorAll('input').forEach(input => { input.disabled = urgent; });
  $('bcTimingChips').setAttribute('aria-disabled', String(urgent));
  $('bcTimingChips').querySelectorAll('input').forEach(input => { input.disabled = urgent; });
  $('bcUrgentNote').hidden = !urgent;
  const inApp = form.querySelector('input[name=channels][value=IN_APP]');
  if (urgent) inApp.checked = true;
  inApp.disabled = urgent;
  $('bcScheduledAt').hidden = urgent || form.elements.timing.value !== 'later';
  const send = $('bcPreview');
  send.classList.toggle('danger', urgent); send.classList.toggle('primary', !urgent);
  form.querySelectorAll('[data-count-for]').forEach(counter => {
    const length = [...$(counter.dataset.countFor).value].length, max = Number(counter.dataset.max);
    counter.textContent = `${length.toLocaleString('vi-VN')}/${max.toLocaleString('vi-VN')}`;
  });
}

function closeButton(dialog) {
  const close = element('button', null, 'nt-icon-button'); close.type = 'button'; close.setAttribute('aria-label', 'Đóng'); close.append(icon('close'));
  close.onclick = () => { if (!state.sending) dialog.close(); };
  return close;
}

/** Bấm "Gửi thông báo": kiểm tra trên máy chủ, rồi hiện hộp "Bạn có chắc muốn gửi?" kèm tóm tắt. */
async function confirmSend() {
  showFormError('');
  let body;
  try { body = payload(); } catch (error) { showFormError(error.message); return; }
  let data;
  try { data = await api(PATH + '/preview', 'POST', body); }
  catch (error) {
    if (error.status === 401) { location.assign('/login'); return; }
    showFormError(error.message || 'Chưa kết nối được hệ thống. Vui lòng thử lại sau.'); return;
  }
  state.payload = body; state.submissionKey = newKey();
  const dialog = $('bcConfirm');
  const head = element('div', null, 'nt-dialog-head');
  const symbol = icon(data.requiresConfirmation ? 'warning' : 'send', 'nt-type-icon');
  if (data.requiresConfirmation) symbol.style.cssText = 'background:#fdecea;color:#b42318';
  const titles = element('div');
  const title = element('h2', data.requiresConfirmation ? 'Gửi thông báo khẩn cho toàn hệ thống?' : data.scheduledAt ? 'Xác nhận hẹn giờ gửi thông báo?' : 'Bạn có chắc muốn gửi thông báo này?');
  title.id = 'bcConfirmTitle'; title.tabIndex = -1;
  titles.append(title, element('div', 'Kiểm tra lại trước khi gửi. Thông báo đã gửi không sửa hoặc thu hồi được.', 'nt-dialog-meta'));
  head.append(symbol, titles, closeButton(dialog));
  const facts = element('dl', null, 'nt-summary');
  [['Gửi tới', data.targetLabel], ['Số người nhận dự kiến', `${data.estimatedRecipients} tài khoản đang hoạt động`], ['Kênh', data.channelLabels.join(', ')],
    ['Nhóm thông báo', data.typeLabel], ['Thời điểm', data.scheduledAt ? formatDate(data.scheduledAt) + ' (giờ Việt Nam)' : 'Gửi ngay']]
    .forEach(([label, value]) => facts.append(element('dt', label), element('dd', value)));
  const message = element('div', null, 'nt-message'); message.append(element('strong', data.title), element('p', data.content));
  const parts = [head, element('div', null), facts, message];
  if (data.requiresConfirmation) parts.push(element('p', 'Mọi tài khoản đang hoạt động sẽ thấy thông báo này trong ứng dụng ngay, kể cả người đã tắt nhận.', 'nt-warning'));
  const status = element('p', '', 'nt-dialog-status'); status.setAttribute('role', 'alert');
  const foot = element('div', null, 'nt-dialog-foot');
  const cancel = element('button', 'Hủy', 'nt-button'); cancel.type = 'button'; cancel.onclick = () => { if (!state.sending) dialog.close(); };
  const send = element('button', null, 'nt-button ' + (data.requiresConfirmation ? 'danger' : 'primary')); send.type = 'button';
  send.append(icon('send'), document.createTextNode(data.requiresConfirmation ? 'Gửi khẩn' : data.scheduledAt ? 'Hẹn giờ gửi' : 'Gửi ngay'));
  send.onclick = () => submit(send, cancel, status, dialog);
  foot.append(cancel, send);
  parts.push(status, foot);
  dialog.replaceChildren(...parts);
  dialog.showModal(); title.focus();
}

async function submit(button, cancel, status, dialog) {
  if (!state.payload || state.sending) return;
  state.sending = true;
  const label = [...button.childNodes];
  button.disabled = cancel.disabled = true; button.replaceChildren(document.createTextNode('Đang gửi…')); status.textContent = '';
  try {
    const data = await api(PATH, 'POST', {...state.payload, submissionKey: state.submissionKey});
    state.sending = false;
    dialog.close();
    showToast(data.message, 'success');
    import('/js/pages/notification-badge.js').then(module => module.refreshNotificationBadges()).catch(() => {});
    form.reset(); state.payload = null; state.submissionKey = null; syncForm();
    state.page = 0; await loadHistory();
  } catch (error) {
    if (error.status === 401) { location.assign('/login'); return; }
    // Giữ nguyên mã thao tác: bấm gửi lại sau lỗi mạng không tạo thêm đợt.
    status.textContent = 'Chưa gửi được: ' + (error.message || 'chưa kết nối được hệ thống. Vui lòng thử lại.');
  } finally {
    state.sending = false; button.disabled = cancel.disabled = false;
    if (button.isConnected) button.replaceChildren(...label);
  }
}

function historyRow(item) {
  const row = element('div', null, 'nt-history-row'); row.tabIndex = 0; row.setAttribute('role', 'button');
  row.setAttribute('aria-label', 'Xem chi tiết: ' + item.title);
  const info = element('div', null, 'nt-item-body');
  const top = element('span', null, 'nt-item-top');
  if (item.urgent) top.append(element('span', 'Khẩn', 'nt-chip urgent'));
  top.append(element('span', item.typeLabel, 'nt-chip'), element('span', item.targetLabel), element('span', '· ' + item.channelLabels.join(' + ')));
  info.append(element('span', item.title, 'nt-item-title'), top);
  const when = element('div', null, 'nt-item-body');
  when.append(statusBadge(item.status, item.statusLabel), element('span', item.scheduledAt && !item.frozenAt ? 'Hẹn ' + formatDate(item.scheduledAt) : formatDate(item.createdAt), 'nt-muted'));
  const people = element('div', item.recipientCount == null ? `Dự kiến ${item.previewCount ?? 0} người` : `${item.recipientCount} người nhận`, 'nt-muted');
  row.append(info, when, people);
  const open = () => openDetail(item.id);
  row.addEventListener('click', open);
  row.addEventListener('keydown', event => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); open(); } });
  return row;
}

async function loadHistory() {
  const list = $('bcHistory');
  try {
    const params = new URLSearchParams([...new FormData($('bcFilters'))].filter(([, value]) => value !== ''));
    params.set('page', state.page); params.set('size', 20);
    const data = await api(PATH + '?' + params);
    if (!data.items.length) list.replaceChildren(stateBox('campaign', 'Chưa có lần gửi phù hợp', 'Các thông báo bạn gửi sẽ hiện ở đây cùng kết quả từng kênh.'));
    else list.replaceChildren(...data.items.map(historyRow));
    $('bcHistoryPage').textContent = data.totalPages > 1 ? `Trang ${data.page + 1} / ${data.totalPages}` : '';
    $('bcHistoryPrev').hidden = $('bcHistoryNext').hidden = data.totalPages <= 1;
    $('bcHistoryPrev').disabled = data.page === 0;
    $('bcHistoryNext').disabled = data.page + 1 >= data.totalPages;
  } catch (error) {
    if (error.status === 401) { location.assign('/login'); return; }
    list.replaceChildren(stateBox('error', 'Chưa tải được lịch sử gửi', error.message || 'Vui lòng thử lại.', () => loadHistory()));
  }
}

async function openDetail(id) {
  const dialog = $('bcDetail');
  dialog.replaceChildren(stateBox('hourglass_top', 'Đang tải…'));
  if (!dialog.open) dialog.showModal();
  try {
    const d = await api(PATH + '/' + encodeURIComponent(id));
    const head = element('div', null, 'nt-dialog-head');
    const titles = element('div');
    const title = element('h2', d.title); title.id = 'bcDetailTitle'; title.tabIndex = -1;
    const meta = element('div', null, 'nt-dialog-meta');
    if (d.urgent) meta.append(element('span', 'Khẩn', 'nt-chip urgent'));
    meta.append(statusBadge(d.status, d.statusLabel), element('span', d.typeLabel, 'nt-chip'));
    titles.append(title, meta);
    head.append(icon('campaign', 'nt-type-icon'), titles, closeButton(dialog));
    const facts = element('dl', null, 'nt-summary');
    [['Gửi tới', d.targetLabel], ['Người gửi', d.creatorName || ''], ['Tạo lúc', formatDate(d.createdAt)], ['Hẹn gửi', d.scheduledAt ? formatDate(d.scheduledAt) : 'Gửi ngay'],
      ['Chốt người nhận', d.frozenAt ? formatDate(d.frozenAt) : 'Chưa chốt'],
      ['Người nhận', d.recipientCount == null ? `Chưa chốt (dự kiến ${d.previewCount ?? 0})` : `${d.recipientCount} (dự kiến lúc gửi: ${d.previewCount ?? 0})`]]
      .forEach(([label, value]) => facts.append(element('dt', label), element('dd', value)));
    const table = element('table', null, 'nt-counts');
    const headRow = element('tr'); ['Kênh', 'Thành công', 'Đang chờ', 'Thất bại', 'Bỏ qua', 'Chưa rõ'].forEach(label => headRow.append(element('th', label)));
    const thead = element('thead'); thead.append(headRow);
    const tbody = element('tbody');
    d.channels.forEach(c => { const tr = element('tr'); [c.channelLabel, c.success, c.pending, c.failed, c.skipped, c.unknown].forEach(value => tr.append(element('td', value))); tbody.append(tr); });
    table.append(thead, tbody);
    const message = element('div', null, 'nt-message'); message.append(element('p', d.content));
    const note = element('p', '“Thành công” với email là máy chủ thư đã nhận gửi, không có nghĩa người nhận đã đọc. “Bỏ qua” là người nhận đã tắt kênh này hoặc tài khoản không còn hoạt động. “Chưa rõ” là gửi bị gián đoạn; hệ thống không tự gửi lại để tránh trùng.', 'nt-help');
    note.style.margin = '12px 22px 0';
    const foot = element('div', null, 'nt-dialog-foot');
    const done = element('button', 'Đóng', 'nt-button primary'); done.type = 'button'; done.onclick = () => dialog.close();
    // Hủy được khi còn hẹn giờ, chưa đến giờ gửi (người tạo hoặc Quản trị viên).
    if (d.canCancel) {
      const cancel = element('button', null, 'nt-button ghost-danger'); cancel.type = 'button';
      cancel.append(icon('event_busy'), document.createTextNode('Hủy hẹn giờ'));
      cancel.onclick = async () => {
        if (!confirm('Hủy thông báo hẹn giờ "' + d.title + '"? Người nhận sẽ không nhận được thông báo này.')) return;
        cancel.disabled = true;
        try {
          const result = await api(PATH + '/' + encodeURIComponent(d.id) + '/cancel', 'POST');
          showToast(result.message || 'Đã hủy thông báo hẹn giờ.', 'success');
          dialog.close(); loadHistory();
        } catch (error) {
          cancel.disabled = false;
          if (error.status === 401) { location.assign('/login'); return; }
          showToast(error.message || 'Chưa hủy được. Vui lòng thử lại.', 'error');
          if (error.status === 409) { dialog.close(); loadHistory(); }
        }
      };
      foot.append(cancel);
    }
    foot.append(done);
    dialog.replaceChildren(head, element('div', null), facts, table, note, message, foot);
    title.focus();
  } catch (error) {
    if (error.status === 401) { location.assign('/login'); return; }
    dialog.replaceChildren(stateBox('error', 'Chưa tải được chi tiết', error.message || 'Vui lòng thử lại.'));
  }
}

async function start() {
  const firstHistory = loadHistory;
  try {
    const account = await mountFrame();
    if (!['admin', 'manager'].includes(account.viewRole)) { location.replace('/pages/notifications/'); return; }
  } catch (error) {
    window.kizunaFrameReady?.(); document.body.dataset.notificationRole = 'error';
    if (error.status === 401) { location.assign('/login'); return; }
    form.replaceWith(stateBox('error', 'Chưa tải được trang', error.message || 'Vui lòng thử lại.', () => location.reload()));
    return;
  }
  // Số chưa đọc ở dòng dưới tiêu đề lấy cùng nguồn với chuông.
  fetchAPI('/v1/notifications/unread-count').then(data => {
    const line = $('notificationUnread'); line.replaceChildren();
    if (data.unreadCount) line.append(element('strong', `${data.unreadCount} thông báo`), document.createTextNode(' chưa đọc'));
    else line.textContent = 'Soạn thông báo gửi cho người dùng trong hệ thống.';
  }).catch(() => {});
  form.addEventListener('input', syncForm);
  form.addEventListener('change', syncForm);
  form.addEventListener('submit', event => { event.preventDefault(); confirmSend(); });
  $('bcFilters').addEventListener('submit', event => { event.preventDefault(); state.page = 0; loadHistory(); });
  $('bcHistoryPrev').onclick = () => { state.page = Math.max(0, state.page - 1); loadHistory(); };
  $('bcHistoryNext').onclick = () => { state.page++; loadHistory(); };
  ['bcConfirm', 'bcDetail'].forEach(id => $(id).addEventListener('click', event => { if (event.target === $(id) && !state.sending) $(id).close(); }));
  $('bcConfirm').addEventListener('cancel', event => { if (state.sending) event.preventDefault(); });
  syncForm();
  await firstHistory();
}

start();
