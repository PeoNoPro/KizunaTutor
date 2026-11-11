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
const INCIDENT_TONE = {'Chưa xử lý': 'warning', 'Đang xử lý': 'info', 'Đã khắc phục, chờ đóng': 'success', 'Đã đóng': 'neutral'};

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

/** Ca nào đang diễn ra: so giờ hiện tại (giờ Việt Nam) với giờ bắt đầu/kết thúc; ca qua đêm có giờ kết thúc nhỏ hơn giờ bắt đầu. */
function nowMinutes() {
  const [h, m] = new Intl.DateTimeFormat('en-GB', {hour: '2-digit', minute: '2-digit', hour12: false, timeZone: TZ}).format(new Date()).split(':').map(Number);
  return (h % 24) * 60 + m;
}
const minutesOf = value => { const [h, m] = String(value).split(':').map(Number); return h * 60 + (m || 0); };
function shiftState(shift, now) {
  const start = minutesOf(shift.start), end = minutesOf(shift.end);
  const inside = start < end ? now >= start && now < end : now >= start || now < end;
  if (inside) return 'current';
  return (start < end ? now >= end : false) ? 'past' : 'upcoming';
}

async function shifts() {
  const box = byId('shiftList');
  try {
    const data = await fetchAPI('/v1/tickets/shifts');
    const today = data.days.find(day => day.date === data.today);
    byId('shiftDate').textContent = new Intl.DateTimeFormat('vi-VN', {weekday: 'long', day: '2-digit', month: '2-digit', timeZone: TZ}).format(new Date(data.today + 'T12:00:00+07:00'));
    const now = nowMinutes();
    let currentCount = null, currentLabel = '';
    const emptyUpcoming = [];
    box.replaceChildren(...data.shiftTypes.map(shift => {
      const people = today?.shifts?.[shift.code] || [], state = shiftState(shift, now);
      if (state === 'current') { currentCount = people.length; currentLabel = shift.label; }
      if (state !== 'past' && !people.length) emptyUpcoming.push(shift.label);
      const item = element('div', null, 'mh-shift'); item.dataset.state = state;
      const head = element('div', null, 'mh-shift-head');
      head.append(element('strong', shift.label), element('span', String(shift.start).slice(0, 5) + ' – ' + String(shift.end).slice(0, 5), 'mh-shift-time'));
      if (state === 'current') head.append(chip('Đang diễn ra', 'info'));
      const names = element('div', null, 'mh-shift-people');
      if (people.length) people.forEach(person => names.append(element('span', person.displayName, 'mh-person')));
      else names.append(element('span', 'Chưa xếp nhân viên', 'mh-shift-empty'));
      item.append(head, names);
      return item;
    }));
    box.removeAttribute('aria-busy');
    byId('kpiShift').textContent = currentCount == null ? '—' : number(currentCount);
    byId('kpiShiftFoot').textContent = currentCount == null ? 'Hiện ngoài giờ làm việc' : currentLabel + (currentCount ? '' : ' chưa có ai');
    if (emptyUpcoming.length) {
      const alert = byId('shiftAlert');
      alert.textContent = 'Hôm nay ' + emptyUpcoming.join(', ').toLocaleLowerCase('vi-VN') + ' chưa có nhân viên hỗ trợ. Yêu cầu mới sẽ chờ đến khi có người trong ca.';
      const link = element('a', 'Xếp lịch ngay'); link.href = '/pages/support-shifts/'; alert.append(' ', link);
      alert.hidden = false;
    }
  } catch {
    byId('kpiShift').textContent = '—'; byId('kpiShiftFoot').textContent = 'Chưa tải được';
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
      // Mở yêu cầu hỗ trợ gốc (tab Xử lý nội bộ hiện trạng thái sự cố); mô tả có sẵn "Mã yêu cầu hỗ trợ: <mã>".
      const ticketId = /Mã yêu cầu hỗ trợ:\s*([0-9a-f-]{36})/i.exec(incident.description || '')?.[1];
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

greeting();
Promise.allSettled([escalated(), shifts(), incidents(), proposals(), activity()]);
