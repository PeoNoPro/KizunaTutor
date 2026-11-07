import {byId, text, date, notice, paging, emptyState, badge, avatar, actionButton, createOperationsPage} from '../../js/pages/operations.js';
import {fetchAPI} from '../../js/api.js';

const base = '/admin/operations/role-changes';
const state = {page: 0, promotionPage: 0, account: null, mode: '', saving: false};
const dialog = byId('roleDialog');
// CSRF chung chỉ dành cho Admin nên màn Manager lấy token ở endpoint riêng.
const send = (path, method, body) => fetchAPI(path, {method, body, csrfPath: base + '/csrf'});

function button(label, action) {
  const node = text('button', label); node.type = 'button'; node.addEventListener('click', action); return node;
}

async function loadStaff() {
  const params = new URLSearchParams({q: byId('roleQuery').value.trim(), page: state.page, size: 20});
  const data = await fetchAPI(base + '/staff?' + params), rows = byId('roleRows');
  rows.replaceChildren();
  if (!data.items.length) {
    const row = text('tr'), message = text('td', 'Chưa có nhân viên phù hợp. Thử đổi từ khóa tìm kiếm.', 'table-empty');
    message.colSpan = 3; row.append(message); rows.append(row);
  }
  data.items.forEach(account => {
    const row = text('tr'), identity = text('td', ''), role = text('td', ''), actions = text('td', ''), person = text('div', '', 'portal-person'), who = text('div'), box = text('div', '', 'portal-actions');
    who.append(text('strong', account.fullName, 'staff-identity-name'), text('p', account.email, 'staff-email'));
    person.append(avatar(account.fullName), who); identity.append(person); role.append(text('span', account.roleLabel, 'portal-chip'));
    if (account.pendingPromotion) box.append(badge('Chờ duyệt'));
    else {
      // Một ô chọn "vai trò mới": chuyển ngang sang vai trò còn lại, hoặc đề xuất lên Quản lý vận hành (cần Admin duyệt).
      const select = text('select', '', 'role-change-select'); select.setAttribute('aria-label', 'Chọn vai trò mới cho ' + account.fullName);
      [['', 'Đổi vai trò…'], ['move', account.moveToLabel], ['propose', 'Quản lý vận hành (đề xuất, chờ Admin duyệt)']].forEach(([value, label]) => { const o = text('option', label); o.value = value; select.append(o); });
      select.addEventListener('change', () => { const mode = select.value; select.value = ''; if (mode) open(mode, account); });
      box.append(select);
    }
    actions.append(box); row.append(identity, role, actions); rows.append(row);
  });
  byId('rolePage').textContent = 'Trang ' + (state.page + 1); paging('role', data, state.page);
}

async function loadPromotions() {
  const data = await fetchAPI(base + '/promotions?page=' + state.promotionPage + '&size=10'), box = byId('myPromotions');
  box.replaceChildren();
  if (!data.items.length) box.append(emptyState('Chưa có đề xuất nào', 'Đề xuất bạn gửi và kết quả duyệt sẽ hiển thị tại đây.'));
  data.items.forEach(request => {
    const card = text('article', '', 'promotion-card'), top = text('div', '', 'promotion-top'), person = text('div', '', 'portal-person'), who = text('div');
    who.append(text('strong', request.targetName, 'staff-identity-name'), text('p', date(request.createdAt), 'portal-muted'));
    person.append(avatar(request.targetName), who); top.append(person, badge(request.statusLabel));
    const move = text('div', '', 'promotion-move');
    move.append(text('span', request.fromRoleLabel, 'portal-chip'), text('span', '→', 'promotion-arrow'), text('span', request.toRoleLabel, 'portal-chip portal-chip-strong'));
    const foot = text('div', '', 'promotion-foot'), actions = text('div', '', 'portal-actions');
    foot.append(text('span', request.decisionNote ? 'Ghi chú của ' + request.decidedByName + ': ' + request.decisionNote : '', 'portal-muted'));
    if (request.status === 'PENDING') actions.append(actionButton('Hủy đề xuất', () => page.run(() => cancel(request)), 'danger'));
    foot.append(actions);
    card.append(top, move, text('p', request.reason, 'promotion-reason'), foot); box.append(card);
  });
  byId('promotionPage').textContent = 'Trang ' + (state.promotionPage + 1); paging('promotion', data, state.promotionPage);
}

async function load() {
  await Promise.all([loadStaff(), loadPromotions()]);
}

async function cancel(request) {
  await send(base + '/promotions/' + encodeURIComponent(request.requestId) + '/cancel', 'POST');
  await load(); notice('Đã hủy đề xuất. Vai trò của nhân viên giữ nguyên.');
}

function open(mode, account) {
  state.mode = mode; state.account = account;
  byId('roleForm').reset(); byId('roleNotice').textContent = '';
  const moving = mode === 'move', target = moving ? account.moveToLabel : 'Quản lý vận hành';
  byId('roleDialogTitle').textContent = moving ? 'Đổi vai trò nhân viên' : 'Đề xuất thăng chức';
  byId('roleDialogSummary').textContent = account.fullName + ' · ' + account.roleLabel + ' → ' + target + (moving ? '. Có hiệu lực ngay.' : '. Chỉ có hiệu lực sau khi Quản trị viên duyệt.');
  byId('saveRole').textContent = moving ? 'Xác nhận đổi vai trò' : 'Gửi đề xuất';
  dialog.showModal();
}

async function save(event) {
  event.preventDefault(); if (state.saving) return;
  state.saving = true; byId('saveRole').disabled = true; byId('roleNotice').textContent = 'Đang lưu…';
  try {
    const {account, mode} = state, reason = byId('roleReason').value.trim();
    if (mode === 'move') await send(base + '/staff/' + encodeURIComponent(account.userId) + '/role', 'PUT', {expectedUpdatedAt: account.updatedAt, reason});
    else await send(base + '/promotions', 'POST', {targetUserId: account.userId, expectedUpdatedAt: account.updatedAt, reason});
    dialog.close(); await page.run(load);
    notice(mode === 'move' ? 'Đã đổi vai trò và ghi nhật ký.' : 'Đã gửi đề xuất. Vai trò chỉ đổi sau khi Quản trị viên duyệt.');
  } catch (error) {
    byId('roleNotice').textContent = error.message || 'Chưa lưu được thay đổi. Vui lòng thử lại.';
    if (error.status === 409) byId('roleNotice').textContent += ' Hãy đóng biểu mẫu và tải lại danh sách.';
  } finally {
    state.saving = false; byId('saveRole').disabled = false;
  }
}

const page = createOperationsPage(load);
byId('roleForm').addEventListener('submit', save);
['closeRole', 'cancelRole'].forEach(id => byId(id).addEventListener('click', () => { if (!state.saving) dialog.close(); }));
dialog.addEventListener('cancel', event => { if (state.saving) event.preventDefault(); });
page.bind('roleSearch', 'submit', async () => { state.page = 0; await load(); });
page.bind('rolePrev', 'click', async () => { state.page = Math.max(0, state.page - 1); await loadStaff(); });
page.bind('roleNext', 'click', async () => { state.page++; await loadStaff(); });
page.bind('promotionPrev', 'click', async () => { state.promotionPage = Math.max(0, state.promotionPage - 1); await loadPromotions(); });
page.bind('promotionNext', 'click', async () => { state.promotionPage++; await loadPromotions(); });
page.start();
