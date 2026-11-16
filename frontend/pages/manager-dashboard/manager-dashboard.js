import { mountAdminShell } from '../../components/admin-shell/admin-shell.js';
import { fetchAPI } from '../../js/api.js';

/**
 * Tổng quan của Quản lý vận hành: việc đang chờ (yêu cầu chuyển lên, ca trực trống, sự cố đã báo, đề xuất đổi vai trò)
 * và số liệu 7 ngày. Chỉ đọc dữ liệu từ các API đã có; mỗi khối tải riêng nên một khối lỗi không làm hỏng cả trang.
 */
mountAdminShell();

const byId = id => document.getElementById(id);
const number = value => new Intl.NumberFormat('vi-VN').format(value ?? 0);
const TZ = 'Asia/Ho_Chi_Minh';
const PRIORITY_TONE = {URGENT: 'danger', HIGH: 'warning', MEDIUM: 'info', LOW: 'neutral'};
const INCIDENT_TONE = {'Chưa xử lý': 'warning', 'Đang xử lý': 'info', 'Đã khắc phục, đang theo dõi': 'success', 'Đã đóng': 'neutral'};

function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text != null) node.textContent = text;
  if (className) node.className = className;
  return node;
}
function chip(label, tone) { const node = element('span', label, 'mh-chip'); node.dataset.tone = tone || 'neutral'; return node; }
function relative(value) {
  if (!value) return '';
  const minutes = Math.round((Date.now() - new Date(value).getTime()) / 60000);
  if (minutes < 1) return 'vừa xong';
  if (minutes < 60) return minutes + ' phút trước';
  if (minutes < 1440) return Math.floor(minutes / 60) + ' giờ trước';
  if (minutes < 10080) return Math.floor(minutes / 1440) + ' ngày trước';
  return new Intl.DateTimeFormat('vi-VN', {day: '2-digit', month: '2-digit', year: 'numeric', timeZone: TZ}).format(new Date(value));
}
function empty(container, text) { container.replaceChildren(element('p', text, 'mh-empty')); container.removeAttribute('aria-busy'); }
function failed(container, text = 'Chưa tải được dữ liệu. Vui lòng thử làm mới trang.') { empty(container, text); container.firstChild.classList.add('is-error'); }

async function greeting() {
  byId('greetingDate').textContent = new Intl.DateTimeFormat('vi-VN', {weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: TZ}).format(new Date());
  try {
    const account = await fetchAPI('/admin/operations/me');
    byId('greetingName').textContent = ', ' + account.displayName;
    if (account.canAdmin) location.replace('/pages/admin-dashboard/');
  } catch { /* Không có tên thì chỉ hiện lời chào; khung quản trị tự chuyển về đăng nhập khi hết phiên. */ }
}

async function escalated() {
  const box = byId('escalatedList');
  try {
    const data = await fetchAPI('/v1/tickets?status=ESCALATED&page=0&size=5');
    const total = data.totalElements ?? data.items.length;
    byId('kpiEscalated').textContent = number(total);
    byId('kpiEscalatedFoot').textContent = total ? 'Mở để ghi quyết định nội bộ' : 'Không có yêu cầu đang chờ';
    if (!data.items.length) return empty(box, 'Không có yêu cầu nào đang chờ bạn quyết định.');
    box.replaceChildren(...data.items.map(ticket => {
      const row = element('a', null, 'mh-row');
      row.href = '/pages/support-ticket/?role=manager&id=' + encodeURIComponent(ticket.ticketId);
      const main = element('div', null, 'mh-row-main');
      main.append(element('strong', ticket.subject, 'mh-row-title'),
        element('span', [ticket.requesterDisplayName, ticket.assigneeDisplayName && 'CS: ' + ticket.assigneeDisplayName, ticket.categoryLabel].filter(Boolean).join(' · '), 'mh-row-meta'));
      const side = element('div', null, 'mh-row-side');
      side.append(chip(ticket.priorityLabel, PRIORITY_TONE[ticket.priority]), element('span', relative(ticket.updatedAt || ticket.createdAt), 'mh-row-time'));
      row.append(main, side);
      return row;
    }));
    box.removeAttribute('aria-busy');
  } catch {
    byId('kpiEscalated').textContent = '—'; byId('kpiEscalatedFoot').textContent = 'Chưa tải được';
    failed(box);
  }
}

async function incidents() {
  const box = byId('incidentList');
  try {
    const data = await fetchAPI('/admin/operations/reported-incidents?page=0&size=50');
    const more = data.totalElements > data.items.length ? '+' : '';
    const open = data.items.filter(item => item.statusLabel !== 'Đã đóng').length;
    byId('kpiIncidents').textContent = number(open) + more;
    byId('kpiIncidentsFoot').textContent = data.totalElements ? 'Tổng đã báo: ' + number(data.totalElements) : 'Chưa báo sự cố nào';
    if (!data.items.length) return empty(box, 'Bạn chưa báo sự cố nào cho Admin.');
    box.replaceChildren(...data.items.slice(0, 4).map(incident => {
      // Mở yêu cầu hỗ trợ gốc (tab Xử lý nội bộ hiện trạng thái sự cố).
      const ticketId = incident.supportTicketId;
      const row = element(ticketId ? 'a' : 'div', null, 'mh-row');
      if (ticketId) row.href = '/pages/support-ticket/?role=manager&tab=internal&id=' + encodeURIComponent(ticketId);
      const main = element('div', null, 'mh-row-main');
      main.append(element('strong', incident.subject, 'mh-row-title'), element('span', 'Báo ' + relative(incident.createdAt) + ' · ' + incident.assigneeLabel, 'mh-row-meta'));
      const side = element('div', null, 'mh-row-side'); side.append(chip(incident.statusLabel, INCIDENT_TONE[incident.statusLabel]));
      row.append(main, side);
      return row;
    }));
    box.removeAttribute('aria-busy');
  } catch {
    byId('kpiIncidents').textContent = '—'; byId('kpiIncidentsFoot').textContent = 'Chưa tải được';
    failed(box);
  }
}

async function proposals() {
  try {
    const data = await fetchAPI('/admin/operations/role-changes/promotions?status=PENDING&page=0&size=1');
    const pending = data.totalElements ?? data.items.length;
    byId('kpiProposals').textContent = number(pending);
    byId('kpiProposalsFoot').textContent = pending ? 'Đang chờ Admin duyệt' : 'Không có đề xuất đang chờ';
  } catch {
    byId('kpiProposals').textContent = '—'; byId('kpiProposalsFoot').textContent = 'Chưa tải được';
  }
}

async function activity() {
  const box = byId('activityStats');
  try {
    const data = await fetchAPI('/admin/operations/management-report');
    box.querySelectorAll('[data-stat]').forEach(node => {
      const value = data[node.dataset.stat];
      node.textContent = node.dataset.stat === 'acceptanceRatePercent' ? (value == null ? 'Chưa có' : number(value) + '%') : number(value);
    });
  } catch {
    box.querySelectorAll('[data-stat]').forEach(node => { node.textContent = '—'; });
  }
  box.removeAttribute('aria-busy');
}

/**
 * Tình hình toàn bộ yêu cầu còn mở (chỉ số lượng). Dòng có trang xử lý thì bấm được:
 * chuyển Quản lý → danh sách chờ bạn xem xét. (Bỏ phân ca 2026-11-16: không còn trang xếp ca.)
 */
const SUPPORT_ROWS = [
  ['unassigned', 'Chưa có người nhận', 'neutral'],
  ['inProgress', 'Đang xử lý', 'info'],
  ['escalated', 'Chuyển Quản lý', 'warning', '/manager/support/tickets?group=0', 'Mở danh sách chờ xem xét'],
  ['overdue', 'Quá hạn phản hồi', 'danger']];
/** "25 phút", "9 giờ 33 phút", "2 ngày 4 giờ". */
function minutes(total) {
  const m = Math.max(0, Math.round(total)); if (m < 60) return m + ' phút';
  const h = Math.floor(m / 60); if (h < 24) return h + ' giờ' + (m % 60 ? ' ' + m % 60 + ' phút' : '');
  return Math.floor(h / 24) + ' ngày' + (h % 24 ? ' ' + h % 24 + ' giờ' : '');
}
async function support() {
  const box = byId('supportPanel');
  try {
    const data = await fetchAPI('/v1/tickets/overview'), total = Math.max(1, data.waiting);
    byId('supportWaiting').textContent = number(data.waiting);
    byId('kpiUnassigned').textContent = number(data.unassigned || 0);
    byId('kpiUnassignedFoot').textContent = data.unassigned ? 'Đang chờ nhân viên hỗ trợ nhận xử lý' : 'Không có yêu cầu nào đang chờ';
    byId('supportAvg').textContent = data.avgFirstResponseMinutes == null ? 'Chưa có' : minutes(data.avgFirstResponseMinutes);
    byId('supportRows').replaceChildren(...SUPPORT_ROWS.map(([key, label, tone, href, hint]) => {
      const row = element(href ? 'a' : 'div', null, 'mh-support-row'); row.dataset.tone = tone;
      if (href) { row.href = href; row.title = hint; }
      const top = element('div', null, 'mh-support-top');
      top.append(element('span', label), element('strong', number(data[key] || 0)));
      const bar = element('div', null, 'mh-support-bar'), fill = element('span');
      fill.style.width = Math.round((data[key] || 0) / total * 100) + '%'; bar.append(fill);
      row.append(top, bar); return row;
    }));
  } catch {
    byId('supportWaiting').textContent = '—';byId('kpiUnassigned').textContent = '—';byId('kpiUnassignedFoot').textContent = 'Chưa tải được';
    failed(byId('supportRows'));
  } finally { box.removeAttribute('aria-busy'); }
}

greeting();
Promise.allSettled([escalated(), support(), incidents(), proposals(), activity()]);
