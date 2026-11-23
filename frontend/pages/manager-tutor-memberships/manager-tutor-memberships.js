/**
 * KizunaTutor - Danh sách và chi tiết hội viên gia sư (FE-09: UC-44, UC-45) cho Quản lý. Chỉ đọc.
 * Nằm trong nhóm "Doanh thu" của sidebar Quản lý (khung quản trị dùng chung, nạp qua operations.js).
 * API: GET /api/manager/tutor-memberships, GET /api/manager/tutor-memberships/{tutorId} — chỉ ROLE_MANAGER.
 */
import { byId, text, emptyState, badge, avatar, actionButton, option, createOperationsPage } from '../../js/pages/operations.js';
import { API, ApiError } from '../../js/api.js';
import { formatMoney, formatDate, formatDateTime } from '../../js/membership-format.js';

const PAGE_SIZE = 20;
const state = { keyword: '', planId: '', status: '', page: 0 };
const detailDialog = byId('detailDialog');

/** Màu nhãn theo mã trạng thái (nhãn tiếng Việt lấy từ backend). */
const MEMBERSHIP_TONES = { ACTIVE: 'success', UPCOMING: 'info', EXPIRED: 'neutral' };
const PAYMENT_TONES = { SUCCESS: 'success', PENDING: 'warning', FAILED: 'danger' };
const membershipBadge = (code, label) => badge(label, MEMBERSHIP_TONES[code] || 'neutral');
const paymentBadge = (code, label) => badge(label, PAYMENT_TONES[code] || 'neutral');

// ===== Danh sách (UC-44) =====

function skeletonRows() {
  const rows = byId('memberRows');
  rows.replaceChildren();
  for (let i = 0; i < 4; i += 1) {
    const row = text('tr');
    for (let c = 0; c < 6; c += 1) {
      const cell = text('td');
      cell.append(text('span', '', 'tm-skeleton'));
      if (c === 0) cell.append(text('span', '', 'tm-skeleton tm-skeleton-short'));
      row.append(cell);
    }
    rows.append(row);
  }
}

async function loadPlans() {
  try {
    const plans = await API.managerMembership.getPlans();
    plans.forEach((plan) => option(byId('memberPlan'), plan.planId, plan.planName));
  } catch (error) {
    // Bộ lọc gói không bắt buộc; danh sách vẫn tải được.
  }
}

async function loadMembers() {
  skeletonRows();
  byId('memberEmpty').hidden = true;
  const data = await API.managerMembership.getTutorMemberships({
    keyword: state.keyword, planId: state.planId, status: state.status, page: state.page, size: PAGE_SIZE
  });
  const rows = byId('memberRows');
  rows.replaceChildren();
  data.items.forEach((member) => {
    const identity = text('td');
    const person = text('div', '', 'portal-person');
    const who = text('div');
    who.append(text('strong', member.tutorName, 'staff-identity-name'), text('p', member.email, 'staff-email'));
    person.append(avatar(member.tutorName), who);
    identity.append(person);
    const status = text('td');
    status.append(membershipBadge(member.status, member.statusLabel));
    const actions = text('td');
    const box = text('div', '', 'portal-actions');
    box.append(actionButton('Xem chi tiết', () => openDetail(member.tutorId)));
    actions.append(box);
    const row = text('tr');
    row.append(identity, text('td', member.planName), text('td', formatDate(member.registeredAt)),
      text('td', formatDate(member.expiresOn)), status, actions);
    rows.append(row);
  });
  const empty = byId('memberEmpty');
  empty.replaceChildren();
  empty.hidden = data.items.length > 0;
  if (!data.items.length) {
    const searching = Boolean(state.keyword || state.planId || state.status);
    empty.append(searching
      ? emptyState('Không có kết quả', 'Không tìm thấy kết quả phù hợp. Vui lòng điều chỉnh bộ lọc.')
      : emptyState('Chưa có hội viên', 'Chưa có gia sư nào đăng ký gói hội viên.'));
  }
  byId('memberTotal').textContent = `Tổng cộng ${data.totalItems} gia sư.`;
  byId('memberPage').textContent = `Trang ${state.page + 1}${data.totalPages ? ' / ' + data.totalPages : ''}`;
  byId('memberPrev').disabled = state.page <= 0;
  byId('memberNext').disabled = state.page + 1 >= (data.totalPages || 1);
}

// ===== Chi tiết (UC-45) =====

function fields(entries) {
  const list = text('dl', '', 'tm-fields');
  entries.forEach(([label, value]) => {
    const item = text('div');
    item.append(text('dt', label), text('dd', value));
    list.append(item);
  });
  return list;
}

function table(caption, headers, rows) {
  const wrap = text('div', '', 'table-scroll');
  const node = text('table', '', 'staff-table');
  const cap = text('caption', caption, 'sr-only');
  const head = text('thead');
  const headRow = text('tr');
  headers.forEach(([label, className]) => {
    const th = text('th', label, className);
    th.scope = 'col';
    headRow.append(th);
  });
  head.append(headRow);
  const body = text('tbody');
  rows.forEach((cells) => {
    const row = text('tr');
    cells.forEach((cell) => row.append(cell));
    body.append(row);
  });
  node.append(cap, head, body);
  wrap.append(node);
  return wrap;
}

function cell(value, className) {
  return text('td', value, className);
}

function badgeCell(node) {
  const td = text('td');
  td.append(node);
  return td;
}

function renderDetail(d) {
  const head = text('div', '', 'portal-person tm-detail-head');
  const who = text('div');
  who.append(text('strong', d.tutor.fullName), text('p', d.tutor.email, 'portal-muted'));
  head.append(avatar(d.tutor.fullName), who);

  const memberships = d.memberships.length
    ? table('Các lần đăng ký', [['Gói'], ['Ngày đăng ký'], ['Thời gian hiệu lực'], ['Số tiền', 'tm-money'], ['Trạng thái']],
      d.memberships.map((m) => [cell(m.planName), cell(formatDate(m.registeredAt)),
        cell(`${formatDate(m.startDate)} – ${formatDate(m.endDate)}`), cell(formatMoney(m.amount), 'tm-money'),
        badgeCell(membershipBadge(m.status, m.statusLabel))]))
    : text('p', 'Chưa có lần đăng ký nào.', 'portal-muted');

  let payments;
  if (d.paymentsUnavailable) {
    payments = text('p', 'Lịch sử thanh toán tạm thời không khả dụng.', 'tm-unavailable');
  } else if (!d.payments || !d.payments.length) {
    payments = text('p', 'Chưa có giao dịch.', 'portal-muted');
  } else {
    payments = table('Lịch sử thanh toán', [['Thời gian'], ['Gói'], ['Số tiền', 'tm-money'], ['Trạng thái'], ['Mã giao dịch VNPay']],
      d.payments.map((p) => [cell(formatDateTime(p.transactionTime)), cell(p.planName), cell(formatMoney(p.amount), 'tm-money'),
        badgeCell(paymentBadge(p.status, p.statusLabel)), cell(p.gatewayTransactionId || '—')]));
  }

  byId('detailBody').replaceChildren(
    head,
    fields([['Số điện thoại', d.tutor.phone || 'Chưa cập nhật'], ['Trạng thái gia sư', d.tutor.tutorStateLabel],
      ['Hiệu lực đến', d.expiresOn ? formatDate(d.expiresOn) : 'Không còn gói hiệu lực']]),
    text('h3', 'Các lần đăng ký', 'tm-section-title'),
    memberships,
    text('h3', 'Lịch sử thanh toán', 'tm-section-title'),
    payments
  );
}

async function openDetail(tutorId) {
  const body = byId('detailBody');
  body.replaceChildren(text('span', '', 'tm-skeleton'), text('span', '', 'tm-skeleton'), text('span', '', 'tm-skeleton tm-skeleton-short'));
  if (!detailDialog.open) detailDialog.showModal();
  try {
    renderDetail(await API.managerMembership.getTutorMembershipDetail(tutorId));
  } catch (error) {
    const retry = text('button', 'Thử lại');
    retry.type = 'button';
    retry.addEventListener('click', () => openDetail(tutorId));
    body.replaceChildren(text('p', error instanceof ApiError ? error.message : 'Chưa tải được thông tin. Vui lòng thử lại.', 'staff-dialog-notice'), retry);
  }
}

// ===== Khởi động =====

const page = createOperationsPage(loadMembers);
page.bind('memberSearch', 'submit', async () => {
  state.keyword = byId('memberQuery').value.trim();
  state.planId = byId('memberPlan').value;
  state.status = byId('memberStatus').value;
  state.page = 0;
  await loadMembers();
});
page.bind('clearMemberSearch', 'click', async () => {
  byId('memberQuery').value = '';
  byId('memberPlan').value = '';
  byId('memberStatus').value = '';
  Object.assign(state, { keyword: '', planId: '', status: '', page: 0 });
  await loadMembers();
});
page.bind('memberPrev', 'click', async () => { state.page = Math.max(0, state.page - 1); await loadMembers(); });
page.bind('memberNext', 'click', async () => { state.page += 1; await loadMembers(); });
byId('closeDetail').addEventListener('click', () => detailDialog.close());
byId('backToList').addEventListener('click', () => detailDialog.close());
loadPlans();
page.start();
