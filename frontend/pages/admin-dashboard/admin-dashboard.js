import { mountAdminShell } from '../../components/admin-shell/admin-shell.js';
mountAdminShell();
import { fetchAPI } from '../../js/api.js';
import { barChart, columnChart, chartError, bindChartTooltip } from '../../js/pages/admin-charts.js';

/** Tổng quan của Quản trị viên: nhân sự, hoạt động quản trị gần đây và lối tắt tới nhân viên, cấu hình. Doanh thu và người dùng thuộc Quản lý. */
const BASE = '/admin/operations';
const byId = id => document.getElementById(id);
const number = value => new Intl.NumberFormat('vi-VN').format(value ?? 0);
const timestamp = value => value ? new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh'
}).format(new Date(value)) : 'Chưa có thời gian';

function element(tag, content, className) {
  const node = document.createElement(tag);
  if (content != null) node.textContent = content;
  if (className) node.className = className;
  return node;
}

function emptyRow(container, columns, message) {
  const row = element('tr');
  const cell = element('td', message, 'table-empty');
  cell.colSpan = columns;
  row.append(cell);
  container.replaceChildren(row);
}

function errorMessage(error) {
  if (error.status === 401) return 'Vui lòng đăng nhập bằng tài khoản quản trị để xem dữ liệu.';
  if (error.status === 403) return 'Tài khoản hiện tại chưa có quyền xem dữ liệu quản trị.';
  return 'Chưa tải được dữ liệu. Vui lòng thử làm mới sau ít phút.';
}

async function loadAccounts() {
  byId('accountSummary').setAttribute('aria-busy', 'true');
  try {
    const [summary, roles] = await Promise.all([fetchAPI(BASE + '/dashboard/accounts'), fetchAPI(BASE + '/roles')]);
    document.querySelectorAll('[data-account-count]').forEach(node => { node.textContent = number(summary[node.dataset.accountCount]); });
    document.querySelectorAll('[data-role-count]').forEach(node => {
      const role = roles.find(item => item.label === node.dataset.roleCount);
      node.textContent = number(role ? role.accountCount : 0);
      const note = node.closest('.kpi-card')?.querySelector('[data-role-note]');
      if (note) note.textContent = role && !role.active ? 'Đã ngừng' : 'Đang dùng';
    });
  } catch (error) {
    document.querySelectorAll('[data-account-count],[data-role-count]').forEach(node => { node.textContent = '—'; });
    throw error;
  } finally {
    byId('accountSummary').setAttribute('aria-busy', 'false');
  }
}

const NO_TABLE = {table: false};
const TZ = 'Asia/Ho_Chi_Minh';

/** Biểu đồ nhân sự: số nhân viên theo vai trò, theo trạng thái tài khoản, và số thao tác quản trị mỗi ngày. */
async function loadCharts() {
  const section = byId('staffChartsSection');
  section.setAttribute('aria-busy', 'true');
  const [roles, staff, audit] = await Promise.allSettled([
    fetchAPI(BASE + '/roles'), fetchAPI(BASE + '/staff?page=0&size=50'), fetchAPI(BASE + '/audit-log?page=0&size=50')]);
  if (roles.status === 'fulfilled') {
    const rows = roles.value.filter(role => role.internal).map(role => ({label: role.label, count: role.accountCount}));
    barChart(byId('chartStaffRoles'), 'Nhân viên theo vai trò', rows, 'Chưa có nhân viên.', NO_TABLE);
  } else chartError(byId('chartStaffRoles'), 'Chưa tải được số liệu vai trò.');
  if (staff.status === 'fulfilled') {
    const groups = new Map();
    staff.value.items.forEach(item => groups.set(item.statusLabel, (groups.get(item.statusLabel) || 0) + 1));
    barChart(byId('chartStaffStatuses'), 'Trạng thái tài khoản nhân viên', [...groups].map(([label, count]) => ({label, count})), 'Chưa có nhân viên.', NO_TABLE);
  } else chartError(byId('chartStaffStatuses'), 'Chưa tải được số liệu trạng thái.');
  if (audit.status === 'fulfilled') {
    const key = date => new Intl.DateTimeFormat('en-CA', {timeZone: TZ}).format(date);
    const counts = new Map();
    audit.value.items.forEach(item => { const day = key(new Date(item.occurredAt)); counts.set(day, (counts.get(day) || 0) + 1); });
    const days = Array.from({length: 14}, (_, index) => { const day = key(new Date(Date.now() - (13 - index) * 86400000)); return {date: day, count: counts.get(day) || 0}; });
    columnChart(byId('chartAudit'), days, {headline: 'thao tác 14 ngày gần nhất', empty: 'Chưa có thao tác quản trị trong 14 ngày gần nhất.', caption: 'Thao tác quản trị theo ngày', table: false});
  } else chartError(byId('chartAudit'), 'Chưa tải được nhật ký thao tác.');
  section.setAttribute('aria-busy', 'false');
  if ([roles, staff, audit].every(result => result.status === 'rejected')) throw roles.reason;
}

const AUDIT_ICONS = {'Nhân sự': 'nav-users.svg', 'Cấu hình': 'nav-settings.svg', 'Thông báo': 'nav-notifications.svg'};
function ago(value) {
  const minutes = Math.round((Date.now() - new Date(value).getTime()) / 60000);
  if (minutes < 1) return 'Vừa xong';
  if (minutes < 60) return minutes + ' phút trước';
  if (minutes < 1440) return Math.floor(minutes / 60) + ' giờ trước';
  if (minutes < 10080) return Math.floor(minutes / 1440) + ' ngày trước';
  return timestamp(value);
}

async function loadAudit() {
  const container = byId('auditRows');
  byId('activitySection').setAttribute('aria-busy', 'true');
  try {
    const data = await fetchAPI(BASE + '/audit-log?page=0&size=6');
    container.replaceChildren();
    if (!data.items.length) emptyRow(container, 4, 'Chưa có hoạt động quản trị nào được ghi nhận.');
    data.items.forEach(item => {
      const row = element('tr');
      const what = element('td'), box = element('div', null, 'activity-cell'), tile = element('span', null, 'activity-icon');
      const img = element('img'); img.src = '/assets/icons/admin-dashboard/' + (AUDIT_ICONS[item.sourceLabel] || 'nav-logs.svg'); img.alt = ''; tile.append(img);
      const text = element('div'); text.append(element('strong', item.actionLabel || 'Thao tác'), element('span', [item.sourceLabel, item.targetLabel].filter(Boolean).join(' · ')));
      box.append(tile, text); what.append(box);
      const when = element('td', ago(item.occurredAt)); when.title = timestamp(item.occurredAt);
      const result = element('td'); result.append(element('span', item.resultLabel || 'Đã lưu', 'status-badge'));
      row.append(what, element('td', item.actorDisplayName), result, when);
      container.append(row);
    });
  } catch (error) {
    emptyRow(container, 4, errorMessage(error));
    throw error;
  } finally {
    byId('activitySection').setAttribute('aria-busy', 'false');
  }
}

/** Lời chào kèm tên tài khoản đang đăng nhập. */
async function greet() {
  byId('greeting').textContent = 'Xin chào';
  byId('greetingDate').textContent = new Intl.DateTimeFormat('vi-VN', {weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'Asia/Ho_Chi_Minh'}).format(new Date());
  try { const account = await fetchAPI(BASE + '/me'); byId('greetingName').textContent = ', ' + account.displayName; } catch { /* Không có tên thì chỉ hiện lời chào. */ }
}

async function refreshDashboard() {
  const notice = byId('dashboardNotice');
  notice.hidden = true;
  const results = await Promise.allSettled([loadAccounts(), loadAudit(), loadCharts()]);
  const failed = results.filter(result => result.status === 'rejected');
  notice.dataset.error = 'true';
  notice.hidden = failed.length === 0;
  notice.textContent = failed.length === results.length ? errorMessage(failed[0].reason) : failed.length ? 'Một số nội dung chưa tải được. Hãy tải lại trang để thử lại.' : '';
}

// Quay lại tab thì tải lại số liệu.
document.addEventListener('visibilitychange', () => { if (!document.hidden) refreshDashboard(); });
bindChartTooltip();
greet();
refreshDashboard();
