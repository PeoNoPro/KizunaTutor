import {api, byId, text, date, notice, paging, emptyState, badge, avatar, actionButton, createOperationsPage} from '../../js/pages/operations.js';
import {showToast} from '../../js/main.js';

const base = '/admin/operations';
const state = {page: 0, historyPage: 0, roles: [], rows: [], action: '', selected: null, saving: false};
const dialog = byId('staffDialog');

function cell(value) { return text('td', value); }
function button(label, action) {
  const node = text('button', label); node.type = 'button'; node.addEventListener('click', action); return node;
}

/** Các nút thao tác nằm ngay trên dòng nhân viên, không phải mở "Xem chi tiết" mới thấy. */
function rowActions(account) {
  const box = text('div', '', 'portal-actions');
  box.append(actionButton('Sửa', () => open('edit', account)));
  if (account.self) return box;
  if (['ACTIVE', 'SUSPENDED'].includes(account.status))
    box.append(actionButton(account.status === 'ACTIVE' ? 'Khóa' : 'Mở khóa', () => open('status', account), account.status === 'ACTIVE' ? 'danger' : 'primary'));
  if (account.mustChangePassword && account.status === 'ACTIVE')
    box.append(actionButton(account.emailStatus === 'FAILED' ? 'Gửi lại email' : 'Cấp lại mật khẩu', () => resendCredentials(account)));
  // Xóa mềm: không có với chính mình và tài khoản Quản trị viên (backend trả deletable).
  if (account.deletable) box.append(actionButton('Xóa', () => open('delete', account), 'danger'));
  return box;
}

/**
 * Email gửi mật khẩu chạy nền sau khi tạo tài khoản hoặc cấp lại mật khẩu.
 * Hệ thống ghi nhớ đúng những người vừa thao tác và hỏi kết quả của từng người cho tới khi có kết quả,
 * nên dù email gửi rất nhanh hay gặp lỗi thì Admin vẫn nhận được thông báo.
 */
const mailTrack = {items: new Map(), timer: null, tries: 0};
function trackEmail(userId, email) {
  if (!userId || !email) return;
  mailTrack.items.set(String(userId), email); mailTrack.tries = 0;
  clearTimeout(mailTrack.timer); mailTrack.timer = setTimeout(checkEmails, 1500);
}
async function checkEmails() {
  const finished = [];
  for (const [userId, email] of mailTrack.items) {
    try {
      const data = await api(base + '/staff?' + new URLSearchParams({q: email, page: 0, size: 5}));
      const account = data.items.find(item => String(item.userId) === userId);
      if (!account || account.emailStatus === 'PENDING') continue;
      finished.push(account);
      mailTrack.items.delete(userId);
      if (account.emailStatus === 'SENT') showToast('Đã gửi email mật khẩu tới ' + email + '.', 'success');
      else showToast('Chưa gửi được email mật khẩu tới ' + email + '. Kiểm tra mục "Tài khoản email gửi thư hệ thống" ở Cấu hình hệ thống, sau đó bấm "Gửi lại email".', 'error');
    } catch { /* Lỗi mạng tạm thời: lần kiểm tra sau thử lại. */ }
  }
  // Có kết quả mới thì vẽ lại danh sách để nhãn email đổi theo.
  if (finished.length) page.run(load);
  if (mailTrack.items.size && ++mailTrack.tries < 30) mailTrack.timer = setTimeout(checkEmails, 3000);
  else if (mailTrack.items.size) {
    mailTrack.items.forEach(email => showToast('Email tới ' + email + ' vẫn đang chờ gửi. Bạn có thể tải lại trang sau ít phút để xem kết quả.', 'info'));
    mailTrack.items.clear();
  }
}

async function load() {
  const params = new URLSearchParams({q: byId('staffQuery').value.trim(), page: state.page, size: 20});
  const [roles, data, history] = await Promise.all([api(base + '/roles'), api(base + '/staff?' + params), api(base + '/staff/history?page=' + state.historyPage + '&size=10')]);
  state.roles = roles; state.rows = data.items;
  const rows = byId('staffRows'); rows.replaceChildren();
  if (!data.items.length) {
    const row = text('tr'); const message = cell('Chưa có nhân viên phù hợp. Thử đổi từ khóa tìm kiếm.');
    message.colSpan = 4; message.className = 'table-empty'; row.append(message); rows.append(row);
  }
  data.items.forEach(account => {
    const row = text('tr'); const identity = cell(''), person = text('div', '', 'portal-person'), who = text('div');
    who.append(text('strong', account.fullName + (account.self ? ' (bạn)' : ''), 'staff-identity-name'), text('p', account.email, 'staff-email'));
    person.append(avatar(account.fullName), who); identity.append(person);
    const role = cell(''); role.append(text('span', account.roleLabel, 'portal-chip'));
    const status = cell(''); status.append(badge(account.statusLabel));
    // Chỉ báo email khi chưa gửi được, để Admin biết cần gửi lại.
    // Nhãn email: chỉ hiện khi tài khoản chưa đăng nhập lần đầu (còn dùng mật khẩu tạm).
    if (account.emailStatus === 'FAILED') status.append(badge('Gửi email lỗi', 'danger'));
    else if (account.emailStatus === 'PENDING') status.append(badge('Đang gửi email', 'warning'));
    else if (account.emailStatus === 'SENT' && account.mustChangePassword) status.append(badge('Đã gửi email', 'success'));
    const actions = cell(''); actions.append(rowActions(account));
    row.append(identity, role, status, actions); rows.append(row);
  });
  // Dòng nào còn "Đang gửi email" (ví dụ vừa nhập từ Excel) thì cũng theo dõi để báo kết quả.
  data.items.filter(account => account.emailStatus === 'PENDING' && !mailTrack.items.has(String(account.userId))).forEach(account => trackEmail(account.userId, account.email));
  byId('staffTotal').textContent = data.totalElements + ' nhân viên' + (data.emailCounts.FAILED ? ' · ' + data.emailCounts.FAILED + ' email cấp tài khoản chưa gửi được' : '');
  byId('staffPage').textContent = 'Trang ' + (state.page + 1); paging('staff', data, state.page);
  const historyBox = byId('staffHistory'); historyBox.replaceChildren();
  if (!history.items.length) historyBox.append(emptyState('Chưa có thay đổi nhân sự', 'Các thao tác tạo, sửa, khóa/mở hoặc đổi vai trò sẽ được ghi lại tại đây.'));
  history.items.forEach(event => {
    const item = text('article', '', 'staff-history-item');
    item.append(text('strong', event.actionLabel + ' · ' + event.targetName), text('p', event.actorName + ' · ' + date(event.occurredAt)), text('p', event.reason));
    historyBox.append(item);
  });
  byId('staffHistoryPage').textContent = 'Trang ' + (state.historyPage + 1); paging('staffHistory', history, state.historyPage);
}

/** Tạo tài khoản Quản lý phải có căn cứ bổ nhiệm; vai trò khác không cần. */
function syncAppointment() {
  const box = document.querySelector('[data-staff-field=appointment]'); if (!box) return;
  const needed = state.action === 'create' && byId('staffRole').selectedOptions[0]?.textContent === 'Quản lý vận hành';
  box.hidden = !needed; byId('staffAppointment').disabled = !needed; byId('staffAppointment').required = needed;
}

/** Chỉ đổi vai trò cho nhân viên nội bộ khác mình; Quản trị viên, Học viên và Gia sư không nằm trong nhóm này. */
function canChangeRole(account) {
  return Boolean(account && !account.self && state.roles.some(role => role.roleId === account.roleId && role.staffAssignable));
}

function open(action, selected = null) {
  state.action = action; state.selected = selected;
  byId('staffForm').reset(); byId('staffDialogNotice').textContent = ''; byId('reloadStaffItem').hidden = true;
  const locking = selected?.status === 'ACTIVE';
  const titles = {create: 'Tạo tài khoản nhân viên', edit: 'Thông tin nhân viên', status: locking ? 'Khóa tài khoản nhân viên' : 'Mở lại tài khoản nhân viên', delete: 'Xóa tài khoản nhân viên'};
  byId('staffDialogTitle').textContent = titles[action];
  byId('saveStaff').textContent = {create: 'Tạo tài khoản', edit: 'Lưu thông tin', status: locking ? 'Xác nhận khóa' : 'Xác nhận mở lại', delete: 'Xác nhận xóa'}[action];
  byId('saveStaff').classList.toggle('danger', action === 'delete');
  byId('staffDialogDescription').textContent = action === 'create' ? 'Hệ thống tự tạo mật khẩu tạm và gửi vào email của nhân viên; họ phải đổi mật khẩu khi đăng nhập lần đầu.' : action === 'status' ? (locking ? 'Tài khoản này sẽ không được truy cập hệ thống khi bị khóa.' : 'Tài khoản này sẽ được truy cập hệ thống trở lại.') : selected.fullName + ' · ' + selected.email;
  byId('staffCurrentState').textContent = selected ? 'Hiện tại: ' + selected.roleLabel + ' · ' + selected.statusLabel : '';
  // Khóa/mở và cấp lại mật khẩu đã có nút riêng trên từng dòng.
  byId('staffExtraActions').hidden = true;
  if (action === 'delete') byId('staffDialogDescription').textContent = 'Xóa ' + selected.fullName + ' (' + selected.email + '). Tài khoản sẽ biến mất khỏi danh sách và không đăng nhập được nữa; lịch sử xử lý, nhật ký và dữ liệu liên quan vẫn được giữ. Ca làm từ hôm nay trở đi sẽ bị gỡ; yêu cầu hỗ trợ đang dở chuyển sang nhóm "Cần bàn giao" của Quản lý. Không hoàn tác được trên giao diện, email này cũng không dùng lại để tạo tài khoản mới.';
  const fields = {create: ['name', 'email', 'phone', 'role', 'appointment'], edit: ['name', 'email', 'phone', ...(canChangeRole(selected) ? ['role'] : []), 'reason'], status: ['reason'], delete: ['reason']}[action];
  document.querySelectorAll('[data-staff-field]').forEach(wrapper => {
    wrapper.hidden = !fields.includes(wrapper.dataset.staffField);
    wrapper.querySelectorAll('input,textarea,select').forEach(input => { input.disabled = wrapper.hidden; input.required = !wrapper.hidden && wrapper.dataset.staffField !== 'phone'; });
  });
  byId('staffRole').replaceChildren();
  if (action === 'create') { const placeholder = text('option', 'Chọn vai trò nội bộ'); placeholder.value = ''; byId('staffRole').append(placeholder); }
  state.roles.filter(role => role.active && role.staffAssignable).forEach(role => {
    const option = text('option', role.label); option.value = role.roleId; byId('staffRole').append(option);
  });
  if (action === 'edit' && canChangeRole(selected)) byId('staffRole').value = String(selected.roleId);
  byId('staffFullName').value = selected?.fullName || ''; byId('staffEmail').value = selected?.email || '';
  byId('staffEmail').readOnly = Boolean(selected?.self);
  byId('staffPhone').value = selected?.phone || '';
  syncAppointment();
  if (!dialog.open) dialog.showModal();
}

async function save(event) {
  event.preventDefault(); if (state.saving) return;
  state.saving = true; byId('saveStaff').disabled = true; byId('cancelStaff').disabled = true;
  byId('staffDialogNotice').textContent = 'Đang lưu thay đổi…';
  try {
    const action = state.action; const account = state.selected;
    let payload, path = base + '/staff', method = 'POST';
    if (action === 'create') payload = {fullName: byId('staffFullName').value.trim(), email: byId('staffEmail').value.trim(), phone: byId('staffPhone').value.trim(), roleId: Number(byId('staffRole').value), appointmentReason: byId('staffAppointment')?.value.trim() || null};
    else {
      method = 'PUT'; path += '/' + encodeURIComponent(account.userId);
      payload = {expectedUpdatedAt: account.updatedAt, reason: byId('staffReason').value.trim()};
      if (action === 'edit') Object.assign(payload, {fullName: byId('staffFullName').value.trim(), email: byId('staffEmail').value.trim(), phone: byId('staffPhone').value.trim()});
      if (action === 'edit' && canChangeRole(account)) payload.roleId = Number(byId('staffRole').value);
      if (action === 'status') { path += '/status'; payload.status = account.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'; }
      if (action === 'delete') { path += '/delete'; method = 'POST'; }
    }
    const result = await api(path, method, payload); dialog.close();
    await page.run(load); notice(action === 'create' ? 'Đã tạo tài khoản. Đang gửi email mật khẩu tạm tới ' + payload.email + '…' : action === 'delete' ? 'Đã xóa tài khoản ' + account.fullName + '.' : 'Đã lưu thay đổi và ghi nhận lịch sử.');
    if (action === 'create') trackEmail(result?.userId, payload.email);
    // Khi sửa tên của chính mình, header cũng lấy lại tên từ DB.
    if (account?.self) {
      const profile = await api(base + '/me'); document.querySelector('.admin-identity strong').textContent = profile.displayName;
    }
  } catch (error) {
    byId('staffDialogNotice').textContent = error.message || 'Chưa lưu được thay đổi. Vui lòng thử lại.';
    byId('reloadStaffItem').hidden = error.status !== 409;
  } finally {
    state.saving = false; byId('saveStaff').disabled = false; byId('cancelStaff').disabled = false;
  }
}

const page = createOperationsPage(load);
byId('createStaff').addEventListener('click', () => open('create'));
byId('cancelStaff').addEventListener('click', () => dialog.close());
byId('closeStaff').addEventListener('click', () => { if (!state.saving) dialog.close(); });
async function resendCredentials(account) {
  if (state.saving) return;
  if (!confirm('Cấp lại mật khẩu tạm cho ' + account.fullName + '? Mật khẩu cũ sẽ không dùng được nữa, mật khẩu mới được gửi qua email.')) return;
  state.saving = true;
  try {
    await page.run(async () => {
      await api(base + '/staff/' + encodeURIComponent(account.userId) + '/resend-credentials', 'POST', {expectedUpdatedAt: account.updatedAt});
      await load(); notice('Đã cấp lại mật khẩu tạm. Đang gửi email tới ' + account.email + '…');
      trackEmail(account.userId, account.email);
    });
  } finally { state.saving = false; }
}
dialog.addEventListener('cancel', event => { if (state.saving) event.preventDefault(); });
byId('staffForm').addEventListener('submit', save);
byId('staffRole').addEventListener('change', syncAppointment);
byId('reloadStaffItem').addEventListener('click', async () => {
  await page.run(load); const latest = state.rows.find(account => account.userId === state.selected.userId);
  if (latest) {
    state.selected = latest; byId('staffCurrentState').textContent = 'Dữ liệu mới: ' + latest.fullName + ' · ' + latest.email + ' · ' + latest.roleLabel + ' · ' + latest.statusLabel;
    byId('staffDialogNotice').textContent = 'Đã tải dữ liệu mới. Nội dung bạn nhập được giữ lại; hãy đối chiếu trước khi xác nhận.';
    byId('reloadStaffItem').hidden = true;
  } else byId('staffDialogNotice').textContent = 'Chưa tìm thấy tài khoản trong trang hiện tại. Đóng biểu mẫu và tìm lại trước khi sửa.';
});
window.addEventListener('staff-imported', event => { page.run(async () => { await load(); notice('Đã nhập ' + event.detail + ' tài khoản và ghi nhận lịch sử.'); }); });
page.bind('staffSearch', 'submit', async () => { state.page = 0; await load(); });
page.bind('clearStaffSearch', 'click', async () => { byId('staffQuery').value = ''; state.page = 0; await load(); });
page.bind('staffPrev', 'click', async () => { state.page = Math.max(0, state.page - 1); await load(); });
page.bind('staffNext', 'click', async () => { state.page++; await load(); });
page.bind('staffHistoryPrev', 'click', async () => { state.historyPage = Math.max(0, state.historyPage - 1); await load(); });
page.bind('staffHistoryNext', 'click', async () => { state.historyPage++; await load(); });
page.start();
if (new URLSearchParams(location.search).get('create') === 'true') {
  window.addEventListener('operations-rendered', () => open('create'), {once: true});
}
