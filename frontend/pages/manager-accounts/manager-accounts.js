/**
 * KizunaTutor - Quản lý người dùng của Quản lý vận hành (FE-14: UC-20 danh sách, UC-21 chi tiết, UC-12 đổi trạng thái).
 * Dùng khung quản trị chung (admin-shell qua js/pages/operations.js) như các trang Quản lý khác.
 * Dữ liệu lấy từ /api/manager/users; nhãn trạng thái luôn lấy từ API.
 */

import { byId, text, date, paging, emptyState, badge, avatar, actionButton, option, createOperationsPage } from '../../js/pages/operations.js';
import { API, ApiError } from '../../js/api.js';
import { showToast } from '../../js/main.js';
import {
  CATEGORIES, STATUS_FILTERS, accountStatusTone, emptyMessage, validateStatusForm, statusConsequence, orDash
} from '../../js/manager-users-helpers.js';

const PAGE_SIZE = 20;
const state = { category: 'TUTOR', keyword: '', status: '', page: 0, detail: null, target: null, saving: false };
const STATUS_LABEL = Object.fromEntries(STATUS_FILTERS.filter((s) => s.value).map((s) => [s.value, s.label]));
const detailDialog = byId('detailDialog');
const statusDialog = byId('statusDialog');

const statusBadge = (code, label) => badge(label, accountStatusTone(code));
const dateOnly = (value) => (value ? date(value).split(',')[0] : 'Chưa cập nhật');

// ===== Danh sách (UC-20) =====

function skeletonRows() {
  const rows = byId('userRows');
  rows.replaceChildren();
  for (let i = 0; i < 4; i += 1) {
    const row = text('tr');
    for (let c = 0; c < 4; c += 1) {
      const cell = text('td');
      cell.append(text('span', '', 'mu-skeleton'));
      if (c === 0) cell.append(text('span', '', 'mu-skeleton mu-skeleton-short'));
      row.append(cell);
    }
    rows.append(row);
  }
}

async function loadUsers() {
  skeletonRows();
  byId('userEmpty').hidden = true;
  const data = await API.managerUsers.list({
    category: state.category, keyword: state.keyword, status: state.status, page: state.page, size: PAGE_SIZE
  });
  const rows = byId('userRows');
  rows.replaceChildren();
  data.items.forEach((user) => {
    const row = text('tr');
    const identity = text('td');
    const person = text('div', '', 'portal-person');
    const who = text('div');
    who.append(text('strong', user.fullName, 'staff-identity-name'), text('p', user.email, 'staff-email'));
    person.append(avatar(user.fullName), who);
    identity.append(person);
    const status = text('td');
    status.append(statusBadge(user.accountStatus, user.accountStatusLabel));
    const actions = text('td');
    const box = text('div', '', 'portal-actions');
    box.append(actionButton('Xem chi tiết', () => openDetail(user.userId)));
    if (user.accountStatus !== 'PENDING') {
      box.append(actionButton('Cập nhật trạng thái', () => openStatus({
        userId: user.userId, fullName: user.fullName, currentStatus: user.accountStatus, currentLabel: user.accountStatusLabel,
        allowed: Object.keys(STATUS_LABEL).filter((s) => s !== 'PENDING' && s !== user.accountStatus)
          .map((s) => ({ value: s, label: STATUS_LABEL[s] }))
      }), 'primary'));
    }
    actions.append(box);
    row.append(identity, text('td', dateOnly(user.createdAt)), status, actions);
    rows.append(row);
  });
  const empty = byId('userEmpty');
  empty.replaceChildren();
  empty.hidden = data.items.length > 0;
  if (!data.items.length) {
    const searching = Boolean(state.keyword || state.status);
    empty.append(emptyState(searching ? 'Không có kết quả' : 'Chưa có người dùng',
      emptyMessage(state.category, { keyword: state.keyword, status: state.status })));
  }
  const group = CATEGORIES.find((c) => c.value === state.category).label.toLocaleLowerCase('vi-VN');
  byId('userTotal').textContent = `Tổng cộng ${data.totalElements} ${group}.`;
  byId('userPage').textContent = `Trang ${state.page + 1}${data.totalPages ? ' / ' + data.totalPages : ''}`;
  paging('user', data, state.page);
}

// ===== Chi tiết (UC-21) =====

function fields(entries) {
  const list = text('dl', '', 'mu-fields');
  entries.forEach(([label, value, wide]) => {
    const item = text('div', '', wide ? 'mu-wide' : '');
    item.append(text('dt', label), text('dd', orDash(value)));
    list.append(item);
  });
  return list;
}

function profileSection(d) {
  if (d.category === 'TUTOR') {
    const t = d.tutorProfile;
    if (!t) return text('p', 'Gia sư chưa cập nhật hồ sơ giảng dạy.', 'portal-muted');
    const rating = t.totalReviews ? `${Number(t.avgRating).toFixed(1)} / 5 (${t.totalReviews} lượt)` : 'Chưa có đánh giá';
    return fields([
      ['Mã gia sư', t.tutorCode], ['Giai đoạn hồ sơ', t.tutorStateLabel],
      ['Tiêu đề giới thiệu', t.headline, true],
      ['Số năm kinh nghiệm', t.experienceYears], ['Trình độ JLPT cao nhất', t.highestJlptLevel],
      ['Đánh giá', rating], ['Ngày được duyệt', t.verifiedAt ? dateOnly(t.verifiedAt) : null],
      ['Ngày sinh', t.dateOfBirth ? t.dateOfBirth.split('-').reverse().join('/') : null], ['Địa chỉ', t.address]
    ]);
  }
  const l = d.learnerProfile;
  if (!l) return text('p', 'Học viên chưa cập nhật hồ sơ học tập.', 'portal-muted');
  return fields([
    ['Trình độ hiện tại', l.currentLevel], ['Trình độ mục tiêu', l.targetLevel],
    ['Tỉnh / thành', l.province], ['Địa chỉ', l.address],
    ['Mục tiêu học tập', l.learningGoals, true]
  ]);
}

function historySection(items) {
  if (!items.length) return text('p', 'Tài khoản chưa từng được đổi trạng thái.', 'portal-muted');
  const list = text('ol', '', 'mu-history');
  items.forEach((h) => {
    const item = text('li');
    const change = text('p');
    change.append(text('strong', h.fromStatusLabel), document.createTextNode(' → '), text('strong', h.toStatusLabel));
    item.append(change, text('p', `${date(h.changedAt)} · ${h.actorName}`, 'portal-muted'),
      text('p', 'Lý do nội bộ: ' + h.reason));
    if (h.messageToUser) item.append(text('p', 'Lời nhắn đã gửi: ' + h.messageToUser, 'portal-muted'));
    list.append(item);
  });
  return list;
}

function renderDetail(d) {
  state.detail = d;
  const head = text('div', '', 'mu-detail-head');
  if (d.avatarUrl) {
    const img = document.createElement('img');
    img.src = d.avatarUrl; img.alt = 'Ảnh đại diện của ' + d.fullName; img.referrerPolicy = 'no-referrer';
    img.addEventListener('error', () => img.replaceWith(avatar(d.fullName)));
    head.append(img);
  } else {
    head.append(avatar(d.fullName));
  }
  const who = text('div');
  const badges = text('div', '', 'mu-detail-badges');
  badges.append(badge(d.categoryLabel, 'info'), statusBadge(d.accountStatus, d.accountStatusLabel));
  who.append(text('strong', d.fullName), text('p', d.email, 'portal-muted'), badges);
  head.append(who);

  const body = byId('detailBody');
  body.replaceChildren(
    head,
    ...(d.statusChangeBlockedReason ? [text('p', d.statusChangeBlockedReason, 'staff-help')] : []),
    text('h3', 'Thông tin tài khoản', 'mu-section-title'),
    fields([['Số điện thoại', d.phone], ['Ngày đăng ký', dateOnly(d.createdAt)], ['Cập nhật gần nhất', date(d.updatedAt)]]),
    text('h3', d.category === 'TUTOR' ? 'Hồ sơ gia sư' : 'Hồ sơ học viên', 'mu-section-title'),
    profileSection(d),
    text('h3', 'Lịch sử đổi trạng thái', 'mu-section-title'),
    historySection(d.statusHistory || [])
  );
  byId('detailStatus').hidden = !d.allowedStatuses.length;
}

async function openDetail(userId) {
  state.detail = null;
  byId('detailStatus').hidden = true;
  const body = byId('detailBody');
  body.replaceChildren(text('span', '', 'mu-skeleton'), text('span', '', 'mu-skeleton'), text('span', '', 'mu-skeleton mu-skeleton-short'));
  if (!detailDialog.open) detailDialog.showModal();
  try {
    renderDetail(await API.managerUsers.detail(userId));
  } catch (error) {
    const retry = text('button', 'Thử lại');
    retry.type = 'button';
    retry.addEventListener('click', () => openDetail(userId));
    body.replaceChildren(text('p', error instanceof ApiError ? error.message : 'Chưa tải được thông tin. Vui lòng thử lại.', 'staff-dialog-notice'), retry);
  }
}

// ===== Đổi trạng thái (UC-12) =====

function setFieldError(name, message) {
  const node = statusDialog.querySelector(`[data-error-for="${name}"]`);
  node.textContent = message || '';
  node.hidden = !message;
}

function showStatusDetails(visible) {
  statusDialog.querySelectorAll('[data-status-detail]').forEach((node) => { node.hidden = !visible; });
}

function openStatus(target) {
  state.target = target;
  byId('statusForm').reset();
  ['status', 'reason', 'message'].forEach((n) => setFieldError(n, ''));
  byId('statusNotice').textContent = '';
  byId('statusSummary').textContent = `${target.fullName} · Trạng thái hiện tại: ${target.currentLabel}`;
  const select = byId('newStatus');
  select.replaceChildren();
  option(select, '', 'Chọn trạng thái mới');
  target.allowed.forEach((s) => option(select, s.value, s.label));
  byId('statusConsequence').textContent = '';
  showStatusDetails(false);
  statusDialog.showModal();
}

function closeStatus() {
  if (state.saving) return;
  state.target = null;
  statusDialog.close();
}

async function submitStatus(event) {
  event.preventDefault();
  if (state.saving || !state.target) return;
  ['status', 'reason', 'message'].forEach((n) => setFieldError(n, ''));
  byId('statusNotice').textContent = '';
  const check = validateStatusForm({
    status: byId('newStatus').value, currentStatus: state.target.currentStatus,
    reason: byId('statusReason').value, message: byId('statusMessage').value
  });
  if (!check.valid) {
    Object.entries(check.errors).forEach(([name, message]) => setFieldError(name, message));
    return;
  }
  const userId = state.target.userId;
  state.saving = true;
  byId('saveStatus').disabled = true;
  byId('statusNotice').textContent = 'Đang lưu…';
  try {
    const res = await API.managerUsers.updateStatus(userId, check.payload);
    state.saving = false;
    closeStatus();
    showToast(res.message, 'success');
    if (detailDialog.open && state.detail?.userId === userId) renderDetail(res.data);
    await page.run(loadUsers);
  } catch (error) {
    byId('statusNotice').textContent = error instanceof ApiError ? error.message : 'Chưa lưu được thay đổi. Vui lòng thử lại.';
    if (error instanceof ApiError && (error.status === 409 || error.status === 404)) {
      page.run(loadUsers);
      if (detailDialog.open && state.detail?.userId === userId) openDetail(userId);
    }
  } finally {
    state.saving = false;
    byId('saveStatus').disabled = false;
  }
}

// ===== Khởi động =====

CATEGORIES.forEach((c) => option(byId('userCategory'), c.value, c.label));
STATUS_FILTERS.forEach((s) => option(byId('userStatus'), s.value, s.label));

const page = createOperationsPage(loadUsers);
page.bind('userSearch', 'submit', async () => {
  state.category = byId('userCategory').value;
  state.keyword = byId('userQuery').value.trim();
  state.status = byId('userStatus').value;
  state.page = 0;
  await loadUsers();
});
page.bind('clearUserSearch', 'click', async () => {
  byId('userQuery').value = '';
  byId('userStatus').value = '';
  state.keyword = '';
  state.status = '';
  state.page = 0;
  await loadUsers();
});
// Đổi nhóm thì tải ngay, không cần bấm Tìm kiếm.
page.bind('userCategory', 'change', async () => {
  state.category = byId('userCategory').value;
  state.page = 0;
  await loadUsers();
});
page.bind('userPrev', 'click', async () => { state.page = Math.max(0, state.page - 1); await loadUsers(); });
page.bind('userNext', 'click', async () => { state.page += 1; await loadUsers(); });

byId('closeDetail').addEventListener('click', () => detailDialog.close());
byId('backToList').addEventListener('click', () => detailDialog.close());
byId('detailStatus').addEventListener('click', () => {
  const d = state.detail;
  if (!d) return;
  openStatus({ userId: d.userId, fullName: d.fullName, currentStatus: d.accountStatus, currentLabel: d.accountStatusLabel, allowed: d.allowedStatuses });
});
byId('newStatus').addEventListener('change', () => {
  const value = byId('newStatus').value;
  byId('statusConsequence').textContent = statusConsequence(value);
  showStatusDetails(Boolean(value));
  setFieldError('status', '');
});
byId('statusForm').addEventListener('submit', submitStatus);
['closeStatus', 'cancelStatus'].forEach((id) => byId(id).addEventListener('click', closeStatus));
statusDialog.addEventListener('cancel', (event) => { if (state.saving) event.preventDefault(); });
page.start();
