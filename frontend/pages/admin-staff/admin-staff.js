import {api, byId, text, date, notice, paging, emptyState, createOperationsPage} from '../../js/pages/operations.js';

const base = '/admin/operations';
const roleMode = document.body.dataset.staffMode === 'roles';
const state = {page: 0, historyPage: 0, roles: [], rows: [], action: '', selected: null, saving: false};
const dialog = byId('staffDialog');
if (roleMode) {
  byId('createStaff').hidden = true;
  byId('roleCards').hidden = false;
}

function cell(value) { return text('td', value); }
function button(label, action) {
  const node = text('button', label); node.type = 'button'; node.addEventListener('click', action); return node;
}

function renderRoles() {
  byId('roleCards').replaceChildren();
  state.roles.forEach(role => {
    const card = text('article', '', 'operations-card');
    card.append(text('h2', role.label), text('p', role.accountCount + ' tài khoản · ' + (role.active ? 'Đang sử dụng' : 'Ngừng sử dụng')));
    card.append(text('p', role.staffAssignable ? 'Vai trò nội bộ · Có thể gán cho nhân viên' : 'Vai trò người dùng · Quản lý hồ sơ tại phân hệ tương ứng', 'staff-help'));
    const list = text('ul'); role.capabilities.forEach(capability => list.append(text('li', capability)));
    card.append(list); byId('roleCards').append(card);
  });
}

async function load() {
  const params = new URLSearchParams({q: byId('staffQuery').value.trim(), page: state.page, size: 20});
  const [roles, data, history] = await Promise.all([api(base + '/roles'), api(base + '/staff?' + params), api(base + '/staff/history?page=' + state.historyPage + '&size=10')]);
  state.roles = roles; state.rows = data.items; renderRoles();
  const rows = byId('staffRows'); rows.replaceChildren();
  if (!data.items.length) {
    const row = text('tr'); const message = cell('Chưa có nhân viên phù hợp. Thử đổi từ khóa tìm kiếm.');
    message.colSpan = 3; message.className = 'table-empty'; row.append(message); rows.append(row);
  }
  data.items.forEach(account => {
    const row = text('tr'); const identity = cell('');
    identity.append(text('strong', account.fullName, 'staff-identity-name'), text('p', account.email + ' · ' + account.roleLabel, 'staff-email'));
    if (account.self) identity.append(text('small', 'Tài khoản bạn đang sử dụng', 'staff-help'));
    const actions = cell('');
    if (!roleMode) actions.append(button('Xem chi tiết', () => open('edit', account)));
    else if (!account.self) actions.append(button('Đổi vai trò', () => open('role', account)));
    else actions.append(text('span', 'Không tự đổi vai trò của mình', 'staff-help'));
    row.append(identity, cell(account.statusLabel), actions); rows.append(row);
  });
  byId('staffTotal').textContent = data.totalElements + ' nhân viên phù hợp · Không gồm tài khoản học viên, gia sư hoặc đã xóa.';
  byId('staffPage').textContent = 'Trang ' + (state.page + 1); paging('staff', data, state.page);
  const historyBox = byId('staffHistory'); historyBox.replaceChildren();
  if (!history.items.length) historyBox.append(emptyState('Chưa có thay đổi nhân sự', 'Các thao tác tạo, sửa, khóa/mở hoặc đổi vai trò sẽ được ghi lại tại đây.'));
  history.items.forEach(event => {
    const item = text('article', '', 'staff-history-item');
    item.append(text('strong', event.actionLabel + ' · ' + event.targetName), text('p', event.actorName + ' · ' + date(event.occurredAt)), text('p', event.reason));
    historyBox.append(item);
  });
  byId('staffHistoryPage').textContent = 'Trang ' + (state.historyPage + 1); paging('staffHistory', history, state.historyPage);
  notice('Đã tải nhân viên, vai trò và lịch sử thay đổi.');
}

function open(action, selected = null) {
  state.action = action; state.selected = selected;
  byId('staffForm').reset(); byId('staffDialogNotice').textContent = ''; byId('reloadStaffItem').hidden = true;
  const locking = selected?.status === 'ACTIVE';
  const titles = {create: 'Tạo tài khoản nhân viên', edit: 'Thông tin nhân viên', role: 'Vai trò và quyền truy cập', status: locking ? 'Khóa tài khoản nhân viên' : 'Mở lại tài khoản nhân viên'};
  byId('staffDialogTitle').textContent = titles[action];
  byId('saveStaff').textContent = {create: 'Tạo tài khoản', edit: 'Lưu thông tin', role: 'Lưu quyền truy cập', status: locking ? 'Xác nhận khóa' : 'Xác nhận mở lại'}[action];
  byId('staffDialogDescription').textContent = action === 'create' ? 'Chỉ dành cho tài khoản nội bộ.' : action === 'status' ? (locking ? 'Tài khoản này sẽ không được truy cập hệ thống khi bị khóa.' : 'Tài khoản này sẽ được truy cập hệ thống trở lại.') : selected.fullName + ' · ' + selected.email;
  byId('staffCurrentState').textContent = selected ? 'Hiện tại: ' + selected.roleLabel + ' · ' + selected.statusLabel : '';
  const canManage = action === 'edit' && !selected.self;
  byId('staffExtraActions').hidden = !canManage;
  byId('staffOpenStatus').hidden = !canManage || !['ACTIVE', 'SUSPENDED'].includes(selected.status);
  byId('staffOpenStatus').textContent = locking ? 'Khóa tài khoản nhân viên' : 'Mở lại tài khoản nhân viên';
  const fields = {create: ['name', 'email', 'phone', 'role', 'password'], edit: ['name', 'email', 'phone', 'reason'], role: ['role', 'reason'], status: ['reason']}[action];
  document.querySelectorAll('[data-staff-field]').forEach(wrapper => {
    wrapper.hidden = !fields.includes(wrapper.dataset.staffField);
    wrapper.querySelectorAll('input,textarea,select').forEach(input => { input.disabled = wrapper.hidden; input.required = !wrapper.hidden && wrapper.dataset.staffField !== 'phone'; });
  });
  byId('staffRole').replaceChildren();
  if (action === 'create') { const placeholder = text('option', 'Chọn vai trò nội bộ'); placeholder.value = ''; byId('staffRole').append(placeholder); }
  state.roles.filter(role => role.active && role.staffAssignable).forEach(role => {
    const option = text('option', role.label); option.value = role.roleId; byId('staffRole').append(option);
  });
  byId('staffFullName').value = selected?.fullName || ''; byId('staffEmail').value = selected?.email || '';
  byId('staffEmail').readOnly = Boolean(selected?.self);
  byId('staffPhone').value = selected?.phone || '';
  if (selected && state.roles.some(role => role.active && role.staffAssignable && role.roleId === selected.roleId)) byId('staffRole').value = selected.roleId;
  if (!dialog.open) dialog.showModal();
}

async function save(event) {
  event.preventDefault(); if (state.saving) return;
  state.saving = true; byId('saveStaff').disabled = true; byId('cancelStaff').disabled = true;
  byId('staffDialogNotice').textContent = 'Đang lưu thay đổi…';
  try {
    const action = state.action; const account = state.selected;
    let payload, path = base + '/staff', method = 'POST';
    if (action === 'create') payload = {fullName: byId('staffFullName').value.trim(), email: byId('staffEmail').value.trim(), phone: byId('staffPhone').value.trim(), roleId: Number(byId('staffRole').value), initialPassword: byId('staffPassword').value};
    else {
      method = 'PUT'; path += '/' + encodeURIComponent(account.userId);
      payload = {expectedUpdatedAt: account.updatedAt, reason: byId('staffReason').value.trim()};
      if (action === 'edit') Object.assign(payload, {fullName: byId('staffFullName').value.trim(), email: byId('staffEmail').value.trim(), phone: byId('staffPhone').value.trim()});
      if (action === 'role') { path += '/role'; payload.roleId = Number(byId('staffRole').value); }
      if (action === 'status') { path += '/status'; payload.status = account.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'; }
    }
    await api(path, method, payload); byId('staffPassword').value = ''; dialog.close();
    await page.run(load); notice('Đã lưu thay đổi và ghi nhận lịch sử.');
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
byId('staffOpenRole').addEventListener('click', () => open('role', state.selected));
byId('staffOpenStatus').addEventListener('click', () => open('status', state.selected));
dialog.addEventListener('cancel', event => { if (state.saving) event.preventDefault(); });
byId('staffForm').addEventListener('submit', save);
byId('reloadStaffItem').addEventListener('click', async () => {
  await page.run(load); const latest = state.rows.find(account => account.userId === state.selected.userId);
  if (latest) {
    state.selected = latest; byId('staffCurrentState').textContent = 'Dữ liệu mới: ' + latest.fullName + ' · ' + latest.email + ' · ' + latest.roleLabel + ' · ' + latest.statusLabel;
    byId('staffDialogNotice').textContent = 'Đã tải dữ liệu mới. Nội dung bạn nhập được giữ lại; hãy đối chiếu trước khi xác nhận.';
    byId('reloadStaffItem').hidden = true;
  } else byId('staffDialogNotice').textContent = 'Chưa tìm thấy tài khoản trong trang hiện tại. Đóng biểu mẫu và tìm lại trước khi sửa.';
});
page.bind('staffSearch', 'submit', async () => { state.page = 0; await load(); });
page.bind('clearStaffSearch', 'click', async () => { byId('staffQuery').value = ''; state.page = 0; await load(); });
page.bind('staffPrev', 'click', async () => { state.page = Math.max(0, state.page - 1); await load(); });
page.bind('staffNext', 'click', async () => { state.page++; await load(); });
page.bind('staffHistoryPrev', 'click', async () => { state.historyPage = Math.max(0, state.historyPage - 1); await load(); });
page.bind('staffHistoryNext', 'click', async () => { state.historyPage++; await load(); });
page.start();
if (!roleMode && new URLSearchParams(location.search).get('create') === 'true') {
  window.addEventListener('operations-rendered', () => open('create'), {once: true});
}
