/**
 * FE-11 (2026-11-13): Quản lý người dùng (Học viên, Gia sư) cho Quản lý vận hành.
 * Xem danh sách, lọc, xem chi tiết hoạt động, tạm khóa / mở lại tài khoản kèm lý do (ghi nhật ký).
 */
import {byId, text, date, notice, paging, emptyState, badge, avatar, actionButton, query, createOperationsPage} from '../../js/pages/operations.js';
import {fetchAPI} from '../../js/api.js';

const base = '/admin/operations/users';
const PAGE_SIZE = 20;
const state = {page: 0, filters: new URLSearchParams(), user: null, saving: false};
const detailDialog = byId('detailDialog'), statusDialog = byId('statusDialog');
// CSRF chung chỉ dành cho Admin nên màn Quản lý lấy token ở endpoint riêng.
const send = (path, method, body) => fetchAPI(path, {method, body, csrfPath: base + '/csrf'});
const number = value => Number(value || 0).toLocaleString('vi-VN');

function day(value) {
  if (!value) return '—';
  const [y, m, d] = String(value).slice(0, 10).split('-');
  return d && m && y ? `${d}/${m}/${y}` : '—';
}

function renderSummary(summary) {
  byId('sumLearners').textContent = number(summary.learners);
  byId('sumTutors').textContent = number(summary.tutors);
  byId('sumSuspended').textContent = number(summary.suspended);
  byId('sumRecent').textContent = number(summary.recent);
}

async function loadUsers() {
  const params = new URLSearchParams(state.filters); params.set('page', state.page); params.set('size', PAGE_SIZE);
  const data = await fetchAPI(base + '?' + params), rows = byId('userRows');
  renderSummary(data.summary || {});
  rows.replaceChildren();
  if (!data.items.length) {
    const row = text('tr'), cell = text('td', '', 'table-empty'); cell.colSpan = 5;
    cell.append(emptyState('Không có người dùng phù hợp', 'Thử đổi từ khóa, vai trò hoặc trạng thái.')); row.append(cell); rows.append(row);
  }
  data.items.forEach(user => {
    const row = text('tr'), identity = text('td'), role = text('td'), status = text('td'), created = text('td', day(user.createdAt)), actions = text('td');
    const person = text('div', '', 'portal-person'), who = text('div');
    who.append(text('strong', user.fullName, 'staff-identity-name'), text('p', user.email + (user.phone ? ' · ' + user.phone : ''), 'staff-email'));
    person.append(avatar(user.fullName), who); identity.append(person);
    role.append(text('span', user.roleLabel, 'portal-chip'));
    if (user.verificationLabel) role.append(text('span', 'Hồ sơ: ' + user.verificationLabel, 'mu-sub'));
    status.append(badge(user.statusLabel));
    const box = text('div', '', 'mu-actions');
    box.append(actionButton('Xem chi tiết', () => page.run(() => openDetail(user.userId))));
    const toggle = statusAction(user); if (toggle) box.append(toggle);
    actions.append(box); row.append(identity, role, status, created, actions); rows.append(row);
  });
  byId('userTotal').textContent = 'Tìm thấy ' + number(data.totalElements) + ' tài khoản.';
  byId('userPage').textContent = 'Trang ' + (state.page + 1); paging('user', data, state.page);
}

/** Nút khóa/mở theo trạng thái; tài khoản chờ kích hoạt hoặc đã bị khóa vĩnh viễn thì không thao tác ở đây. */
function statusAction(user) {
  if (user.status === 'ACTIVE') return actionButton('Tạm khóa', () => openStatus(user), 'danger');
  if (user.status === 'SUSPENDED') return actionButton('Mở lại', () => openStatus(user), 'primary');
  return null;
}

function fact(list, label, value) {
  const item = text('div'); item.append(text('dt', label), text('dd', value == null || value === '' ? '—' : value)); list.append(item);
}

function stat(box, label, value) {
  const item = text('div', '', 'mu-stat'); item.append(text('span', label), text('strong', number(value))); box.append(item);
}

function renderDetail(user) {
  state.user = user;
  byId('detailTitle').textContent = user.fullName;
  const body = byId('detailBody'); body.replaceChildren();
  const head = text('div', '', 'portal-person'), who = text('div');
  who.append(text('strong', user.roleLabel, 'staff-identity-name')); who.append(badge(user.statusLabel)); head.append(avatar(user.fullName), who);
  const facts = text('dl', '', 'mu-facts');
  fact(facts, 'Email', user.email); fact(facts, 'Số điện thoại', user.phone); fact(facts, 'Ngày tạo tài khoản', day(user.createdAt));
  fact(facts, 'Yêu cầu hỗ trợ đã gửi', number(user.ticketCount));
  if (user.role === 'TUTOR') {
    fact(facts, 'Hồ sơ gia sư', user.verificationLabel); fact(facts, 'Gói hội viên', user.membershipLabel);
    fact(facts, 'Hội viên còn hạn đến', user.activity?.membershipEndDate ? day(user.activity.membershipEndDate) : 'Không có gói còn hạn');
    fact(facts, 'Đánh giá', user.totalReviews ? Number(user.avgRating).toFixed(1) + ' / 5 (' + number(user.totalReviews) + ' lượt)' : 'Chưa có đánh giá');
  } else {
    fact(facts, 'Trình độ hiện tại', user.currentLevel); fact(facts, 'Mục tiêu', user.targetLevel);
  }
  body.append(head, facts, text('h3', 'Hoạt động', 'mu-section-title'));
  if (user.activity) {
    const stats = text('div', '', 'mu-stats'), tutor = user.role === 'TUTOR';
    stat(stats, tutor ? 'Yêu cầu kết nối nhận' : 'Yêu cầu kết nối gửi', user.activity.connections);
    stat(stats, 'Kết nối đã chấp nhận', user.activity.acceptedConnections);
    stat(stats, 'Buổi học thử', user.activity.trials);
    stat(stats, 'Lớp đang học', user.activity.activeClasses);
    body.append(stats);
  } else body.append(text('p', 'Người dùng chưa tạo hồ sơ ' + (user.role === 'TUTOR' ? 'gia sư' : 'học viên') + ' nên chưa có hoạt động.', 'mu-muted'));
  body.append(text('h3', 'Lịch sử khóa / mở tài khoản', 'mu-section-title'));
  if (!user.historyReady) body.append(text('p', 'Chưa bật được lịch sử khóa tài khoản: cơ sở dữ liệu chưa được cập nhật.', 'mu-warning'));
  else if (!user.history.length) body.append(text('p', 'Chưa có lần khóa hoặc mở lại nào.', 'mu-muted'));
  else {
    const list = text('ul', '', 'mu-history');
    user.history.forEach(event => {
      const item = text('li'); item.dataset.action = event.action;
      item.append(text('strong', event.actionLabel + ' · ' + date(event.occurredAt)), text('p', 'Người thực hiện: ' + event.actorName), text('p', 'Lý do: ' + event.reason));
      list.append(item);
    });
    body.append(list);
  }
  const button = byId('detailStatus');
  button.hidden = !['ACTIVE', 'SUSPENDED'].includes(user.status);
  button.textContent = user.status === 'ACTIVE' ? 'Tạm khóa tài khoản' : 'Mở lại tài khoản';
  button.className = user.status === 'ACTIVE' ? 'danger' : 'primary';
}

async function openDetail(userId) {
  renderDetail(await fetchAPI(base + '/' + encodeURIComponent(userId)));
  if (!detailDialog.open) detailDialog.showModal();
}

function openStatus(user) {
  state.user = user;
  const suspend = user.status === 'ACTIVE';
  byId('statusForm').reset(); byId('statusNotice').textContent = '';
  byId('statusTitle').textContent = suspend ? 'Tạm khóa tài khoản' : 'Mở lại tài khoản';
  byId('statusSummary').textContent = user.fullName + ' · ' + user.roleLabel + '. ' + (suspend
    ? 'Người dùng bị đăng xuất ngay và không đăng nhập được cho đến khi được mở lại. Lý do được ghi vào nhật ký hệ thống.'
    : 'Người dùng đăng nhập và sử dụng lại bình thường, đồng thời nhận thông báo tài khoản đã được mở lại.');
  byId('saveStatus').textContent = suspend ? 'Xác nhận tạm khóa' : 'Xác nhận mở lại';
  byId('saveStatus').className = suspend ? 'danger' : 'primary';
  statusDialog.showModal(); byId('statusReason').focus();
}

async function saveStatus(event) {
  event.preventDefault(); if (state.saving) return;
  const user = state.user, reason = byId('statusReason').value.trim(), suspend = user.status === 'ACTIVE';
  if (!reason) { byId('statusNotice').textContent = 'Vui lòng nhập lý do.'; byId('statusReason').focus(); return; }
  state.saving = true; byId('saveStatus').disabled = true; byId('statusNotice').textContent = 'Đang lưu…';
  try {
    const updated = await send(base + '/' + encodeURIComponent(user.userId) + '/status', 'PUT', {status: suspend ? 'SUSPENDED' : 'ACTIVE', reason, expectedVersion: user.version});
    statusDialog.close();
    if (detailDialog.open) renderDetail(updated);
    await page.run(loadUsers);
    notice(suspend ? 'Đã khóa tạm thời tài khoản ' + user.fullName + '.' : 'Đã mở lại tài khoản ' + user.fullName + '.');
  } catch (error) {
    byId('statusNotice').textContent = error.message || 'Chưa lưu được thay đổi. Vui lòng thử lại.';
    if (error.status === 409) byId('statusNotice').textContent += ' Hãy đóng biểu mẫu và tải lại danh sách.';
  } finally {
    state.saving = false; byId('saveStatus').disabled = false;
  }
}

const page = createOperationsPage(loadUsers);
byId('statusForm').addEventListener('submit', saveStatus);
['closeStatus', 'cancelStatus'].forEach(id => byId(id).addEventListener('click', () => { if (!state.saving) statusDialog.close(); }));
statusDialog.addEventListener('cancel', event => { if (state.saving) event.preventDefault(); });
['closeDetail', 'detailClose'].forEach(id => byId(id).addEventListener('click', () => detailDialog.close()));
byId('detailStatus').addEventListener('click', () => { if (state.user) openStatus(state.user); });
page.bind('userFilters', 'submit', async () => { state.filters = query(byId('userFilters')); state.page = 0; await loadUsers(); });
page.bind('clearFilters', 'click', async () => { byId('userFilters').reset(); state.filters = new URLSearchParams(); state.page = 0; await loadUsers(); });
page.bind('userPrev', 'click', async () => { state.page = Math.max(0, state.page - 1); await loadUsers(); });
page.bind('userNext', 'click', async () => { state.page++; await loadUsers(); });
page.start();
