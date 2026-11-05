import {api, byId, text, date, notice, paging, emptyState, createOperationsPage} from '../../js/pages/operations.js';

const base = '/admin/operations';
const roleMode = document.body.dataset.staffMode === 'roles';
const state = {page: 0, historyPage: 0, roles: [], rows: [], action: '', selected: null, saving: false, promotion: null};
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
    card.append(text('p', role.staffAssignable ? 'Vai trò nội bộ · Có thể gán khi tạo tài khoản' : role.internal ? 'Vai trò nội bộ · Không gán khi tạo tài khoản' : 'Vai trò người dùng · Quản lý hồ sơ tại phân hệ tương ứng', 'staff-help'));
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
    if (account.emailStatusLabel) identity.append(text('p', account.emailStatusLabel + (account.emailStatus === 'SENT' && account.emailSentAt ? ' lúc ' + date(account.emailSentAt) : ''), 'staff-help'));
    if (account.self) identity.append(text('small', 'Tài khoản bạn đang sử dụng', 'staff-help'));
    const actions = cell('');
    if (!roleMode) actions.append(button('Xem chi tiết', () => open('edit', account)));
    else if (['Cán bộ học vụ', 'Chăm sóc khách hàng'].includes(account.roleLabel)) actions.append(text('span', 'Đổi vai trò do Quản lý thực hiện', 'staff-help'));
    else actions.append(text('span', 'Không đổi vai trò tại đây', 'staff-help'));
    row.append(identity, cell(account.statusLabel), actions); rows.append(row);
  });
  byId('staffTotal').textContent = data.totalElements + ' nhân viên phù hợp · Không gồm tài khoản học viên, gia sư hoặc đã xóa. Email cấp tài khoản: ' + data.emailCounts.PENDING + ' chờ gửi, ' + data.emailCounts.SENT + ' đã gửi, ' + data.emailCounts.FAILED + ' thất bại.';
  byId('staffPage').textContent = 'Trang ' + (state.page + 1); paging('staff', data, state.page);
  const historyBox = byId('staffHistory'); historyBox.replaceChildren();
  if (!history.items.length) historyBox.append(emptyState('Chưa có thay đổi nhân sự', 'Các thao tác tạo, sửa, khóa/mở hoặc đổi vai trò sẽ được ghi lại tại đây.'));
  history.items.forEach(event => {
    const item = text('article', '', 'staff-history-item');
    item.append(text('strong', event.actionLabel + ' · ' + event.targetName), text('p', event.actorName + ' · ' + date(event.occurredAt)), text('p', event.reason));
    historyBox.append(item);
  });
  byId('staffHistoryPage').textContent = 'Trang ' + (state.historyPage + 1); paging('staffHistory', history, state.historyPage);
  if (roleMode) await loadPromotions();
  notice('Đã tải nhân viên, vai trò và lịch sử thay đổi.');
}

/** Lỗi riêng phần đề xuất (ví dụ DB chưa có bảng) không làm hỏng danh sách nhân viên. */
async function loadPromotions() {
  const box = byId('promotionList');
  try {
    const data = await api(base + '/role-changes/promotions?status=PENDING&size=50');
    box.replaceChildren();
    if (!data.items.length) { box.append(emptyState('Chưa có đề xuất chờ duyệt', 'Đề xuất thăng chức do Quản lý gửi sẽ xuất hiện tại đây.')); return; }
    data.items.forEach(request => {
      const item = text('article', '', 'portal-row'), info = text('div', '', 'portal-row-info'), actions = text('div', '', 'staff-promotion-actions');
      info.append(text('strong', request.targetName, 'staff-identity-name'), text('p', request.targetEmail + ' · ' + request.fromRoleLabel + ' → ' + request.toRoleLabel), text('p', 'Đề xuất bởi ' + request.requesterName + ' · ' + date(request.createdAt)), text('p', 'Lý do: ' + request.reason));
      actions.append(button('Xem và quyết định', () => openPromotion(request)));
      item.append(info, text('span', request.statusLabel, 'portal-row-status'), actions); box.append(item);
    });
  } catch (error) {
    if (error.status === 401) throw error;
    box.replaceChildren(emptyState('Chưa tải được đề xuất thăng chức', error.message));
  }
}

function openPromotion(request) {
  state.promotion = request; byId('promotionForm').reset(); byId('promotionNotice').textContent = '';
  byId('promotionSummary').textContent = request.targetName + ' · ' + request.fromRoleLabel + ' → ' + request.toRoleLabel;
  byId('promotionReason').textContent = 'Lý do của ' + request.requesterName + ': ' + request.reason;
  byId('promotionDialog').showModal();
}

async function decidePromotion(action) {
  if (state.saving) return;
  state.saving = true; byId('promotionNotice').textContent = 'Đang lưu quyết định…';
  try {
    await api(base + '/role-changes/promotions/' + encodeURIComponent(state.promotion.requestId) + '/' + action, 'POST', {note: byId('promotionNote').value.trim()});
    byId('promotionDialog').close(); await page.run(load);
    notice(action === 'approve' ? 'Đã duyệt: tài khoản được chuyển sang vai trò mới và ghi nhật ký.' : 'Đã từ chối đề xuất. Vai trò của nhân viên giữ nguyên.');
  } catch (error) {
    byId('promotionNotice').textContent = error.message || 'Chưa lưu được quyết định. Vui lòng thử lại.';
  } finally { state.saving = false; }
}

/** Tạo tài khoản Quản lý phải có căn cứ bổ nhiệm; vai trò khác không cần. */
function syncAppointment() {
  const box = document.querySelector('[data-staff-field=appointment]'); if (!box) return;
  const needed = state.action === 'create' && byId('staffRole').selectedOptions[0]?.textContent === 'Quản lý vận hành';
  box.hidden = !needed; byId('staffAppointment').disabled = !needed; byId('staffAppointment').required = needed;
}

function open(action, selected = null) {
  state.action = action; state.selected = selected;
  byId('staffForm').reset(); byId('staffDialogNotice').textContent = ''; byId('reloadStaffItem').hidden = true;
  const locking = selected?.status === 'ACTIVE';
  const titles = {create: 'Tạo tài khoản nhân viên', edit: 'Thông tin nhân viên', status: locking ? 'Khóa tài khoản nhân viên' : 'Mở lại tài khoản nhân viên'};
  byId('staffDialogTitle').textContent = titles[action];
  byId('saveStaff').textContent = {create: 'Tạo tài khoản', edit: 'Lưu thông tin', status: locking ? 'Xác nhận khóa' : 'Xác nhận mở lại'}[action];
  byId('staffDialogDescription').textContent = action === 'create' ? 'Hệ thống tự tạo mật khẩu tạm và gửi vào email của nhân viên; họ phải đổi mật khẩu khi đăng nhập lần đầu.' : action === 'status' ? (locking ? 'Tài khoản này sẽ không được truy cập hệ thống khi bị khóa.' : 'Tài khoản này sẽ được truy cập hệ thống trở lại.') : selected.fullName + ' · ' + selected.email;
  byId('staffCurrentState').textContent = selected ? 'Hiện tại: ' + selected.roleLabel + ' · ' + selected.statusLabel : '';
  const canManage = action === 'edit' && !selected.self;
  byId('staffExtraActions').hidden = !canManage;
  byId('staffOpenStatus').hidden = !canManage || !['ACTIVE', 'SUSPENDED'].includes(selected.status);
  const resend = byId('staffResend'); if (resend) resend.hidden = !canManage || !selected.mustChangePassword || selected.status !== 'ACTIVE';
  if (resend) resend.textContent = selected?.emailStatus === 'FAILED' ? 'Gửi lại email (email trước chưa gửi được)' : 'Cấp lại mật khẩu tạm qua email';
  byId('staffOpenStatus').textContent = locking ? 'Khóa tài khoản nhân viên' : 'Mở lại tài khoản nhân viên';
  const fields = {create: ['name', 'email', 'phone', 'role', 'appointment'], edit: ['name', 'email', 'phone', 'reason'], status: ['reason']}[action];
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
      if (action === 'status') { path += '/status'; payload.status = account.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'; }
    }
    await api(path, method, payload); dialog.close();
    await page.run(load); notice(action === 'create' ? 'Đã tạo tài khoản. Email đang chờ gửi; xem trạng thái trong danh sách, bấm Tìm kiếm để cập nhật.' : 'Đã lưu thay đổi và ghi nhận lịch sử.');
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
byId('staffOpenStatus').addEventListener('click', () => open('status', state.selected));
byId('staffResend')?.addEventListener('click', async () => {
  if (state.saving) return;
  state.saving = true; byId('staffDialogNotice').textContent = 'Đang cấp lại mật khẩu tạm…';
  try {
    await api(base + '/staff/' + encodeURIComponent(state.selected.userId) + '/resend-credentials', 'POST', {expectedUpdatedAt: state.selected.updatedAt});
    dialog.close(); await page.run(load); notice('Đã cấp lại; mật khẩu cũ không còn dùng được. Email đang chờ gửi, xem trạng thái trong danh sách.');
  } catch (error) { byId('staffDialogNotice').textContent = error.message || 'Chưa cấp lại được. Vui lòng thử lại.'; byId('reloadStaffItem').hidden = error.status !== 409; }
  finally { state.saving = false; }
});
dialog.addEventListener('cancel', event => { if (state.saving) event.preventDefault(); });
byId('staffForm').addEventListener('submit', save);
byId('staffRole').addEventListener('change', syncAppointment);
if (roleMode) {
  byId('promotionSection').hidden = false;
  byId('promotionForm').addEventListener('submit', event => { event.preventDefault(); decidePromotion('approve'); });
  byId('rejectPromotion').addEventListener('click', () => {
    if (!byId('promotionNote').value.trim()) { byId('promotionNotice').textContent = 'Vui lòng nhập lý do từ chối.'; byId('promotionNote').focus(); return; }
    decidePromotion('reject');
  });
  byId('closePromotion').addEventListener('click', () => { if (!state.saving) byId('promotionDialog').close(); });
}
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
if (!roleMode && new URLSearchParams(location.search).get('create') === 'true') {
  window.addEventListener('operations-rendered', () => open('create'), {once: true});
}
