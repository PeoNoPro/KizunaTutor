import { mountAdminShell } from '../../components/admin-shell/admin-shell.js';
mountAdminShell();
import { fetchAPI } from '../../js/api.js';

const BASE = '/admin/operations';
const byId = id => document.getElementById(id);
const number = value => new Intl.NumberFormat('vi-VN').format(value);
const timestamp = value => value ? new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh'
}).format(new Date(value)) : 'Chưa có lần đo';
const state = { busy: false, health: null, incidentRequest: 0 };

function element(tag, content, className) {
  const node = document.createElement(tag);
  if (content != null) node.textContent = content;
  if (className) node.className = className;
  return node;
}

function empty(container, title, detail) {
  const node = element('div', null, 'empty-state');
  node.append(element('strong', title));
  if (detail) node.append(element('p', detail));
  container.replaceChildren(node);
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
  byId('accountSummaryNotice').textContent = 'Đang tải thống kê tài khoản hiện tại…';
  try {
    const data = await fetchAPI(BASE + '/dashboard/accounts');
    document.querySelectorAll('[data-account-count]').forEach(node => {
      node.textContent = number(data[node.dataset.accountCount]);
    });
    byId('accountSummaryNotice').textContent = 'Tài khoản hiện tại · Cập nhật: ' + timestamp(data.generatedAt) + '. Không gồm tài khoản đã xóa; khoảng ngày phía trên áp dụng cho báo cáo sự cố.';
    const container = byId('accountDistribution');
    container.replaceChildren();
    if (data.totalAccounts === 0) {
      empty(container, 'Chưa có tài khoản', 'Chưa có tài khoản chưa xóa trong hệ thống.');
    } else {
      container.append(element('strong', 'Tổng ' + number(data.totalAccounts) + ' tài khoản'));
      const groups = [['Học viên', data.learnerAccounts], ['Gia sư', data.tutorAccounts], ['Nhân sự nội bộ', data.staffAccounts]];
      if (data.otherAccounts > 0) groups.push(['Nhóm khác', data.otherAccounts]);
      groups.forEach(([label, count]) => {
        const row = element('div', null, 'account-distribution-row');
        const percent = new Intl.NumberFormat('vi-VN', {maximumFractionDigits: 1}).format(count / data.totalAccounts * 100);
        row.append(element('span', label), element('strong', number(count) + ' (' + percent + '%)'));
        const meter = element('meter');
        meter.min = 0; meter.max = data.totalAccounts; meter.value = count;
        meter.setAttribute('aria-label', label + ': ' + number(count) + ' trên ' + number(data.totalAccounts) + ' tài khoản');
        row.append(meter); container.append(row);
      });
    }
  } catch (error) {
    document.querySelectorAll('[data-account-count]').forEach(node => { node.textContent = '—'; });
    byId('accountSummaryNotice').textContent = errorMessage(error);
    empty(byId('accountDistribution'), 'Chưa tải được cơ cấu tài khoản', 'Chọn Làm mới dữ liệu để thử lại.');
    throw error;
  } finally {
    byId('accountSummary').setAttribute('aria-busy', 'false');
  }
}

function isStale(row) {
  return row.stale || (row.observedAt && Date.now() - Date.parse(row.observedAt) > 60000);
}

function renderHealth() {
  const container = byId('healthComponents');
  const rows = state.health.components;
  container.replaceChildren();
  if (!rows.length) {
    empty(container, 'Chưa có kết quả quan sát', 'Kết quả sẽ xuất hiện khi hệ thống ghi nhận lần kiểm tra đầu tiên.');
  }
  let needsAttention = false;
  rows.forEach(row => {
    const stale = isStale(row);
    const status = stale ? 'Dữ liệu quá hạn' : row.statusLabel;
    const tone = stale || status === 'Chưa xác định' || status === 'Suy giảm' ? 'warning' : status === 'Gián đoạn' ? 'error' : '';
    if (tone) needsAttention = true;
    const card = element('article', null, 'health-card');
    card.dataset.tone = tone;
    const top = element('div', null, 'health-card-top');
    const badge = element('span', status, 'status-badge');
    badge.dataset.tone = tone;
    top.append(element('h3', row.label), badge);
    card.append(top, element('p', stale ? 'Kết quả đã quá 60 giây. Cần kiểm tra lại trạng thái thành phần.' : row.reasonLabel));
    if (row.responseTimeMs != null && !stale) card.append(element('p', 'Thời gian phản hồi: ' + number(row.responseTimeMs) + ' mili giây'));
    const time = element('time', timestamp(row.observedAt));
    if (row.observedAt) time.dateTime = row.observedAt;
    card.append(time);
    container.append(card);
  });
  // Chỉ kết luận theo các thành phần đã quan sát, không suy ra toàn bộ hệ thống an toàn.
  byId('healthSummary').textContent = !rows.length ? 'Chưa có kết quả' : needsAttention ? 'Có thành phần cần kiểm tra' : 'Các thành phần đã đo đang hoạt động';
  byId('healthSummary').dataset.tone = needsAttention ? 'warning' : '';
  byId('headerStatus').textContent = !rows.length ? 'Chưa có kết quả giám sát' : needsAttention ? 'Cần kiểm tra vận hành' : 'Đã có kết quả giám sát';
  byId('healthUpdated').textContent = 'Cập nhật: ' + timestamp(state.health.generatedAt) + '. Tự làm mới mỗi 30 giây; dữ liệu quá 60 giây được đánh dấu quá hạn.';
}

async function loadHealth() {
  byId('healthSection').setAttribute('aria-busy', 'true');
  try {
    state.health = await fetchAPI(BASE + '/health');
    renderHealth();
  } catch (error) {
    state.health = null;
    byId('headerStatus').textContent = 'Chưa có kết quả giám sát';
    byId('healthSummary').textContent = 'Chưa lấy được kết quả';
    byId('healthSummary').dataset.tone = 'warning';
    byId('healthUpdated').textContent = 'Chưa có kết quả cập nhật. Chọn Làm mới dữ liệu để thử lại.';
    empty(byId('healthComponents'), 'Chưa tải được tình trạng hệ thống', errorMessage(error));
    throw error;
  } finally {
    byId('healthSection').setAttribute('aria-busy', 'false');
  }
}

function reportQuery() {
  const parts = new Intl.DateTimeFormat('en-CA', {
    year: 'numeric', month: '2-digit', day: '2-digit', timeZone: 'Asia/Ho_Chi_Minh'
  }).formatToParts(new Date());
  const value = type => parts.find(part => part.type === type).value;
  const endDate = `${value('year')}-${value('month')}-${value('day')}`;
  const start = new Date(endDate + 'T00:00:00Z');
  start.setUTCDate(start.getUTCDate() - Number(byId('reportPeriod').value) + 1);
  return new URLSearchParams({ startDate: start.toISOString().slice(0, 10), endDate });
}

async function loadReport() {
  const metrics = byId('incidentMetrics');
  metrics.setAttribute('aria-busy', 'true');
  try {
    const data = await fetchAPI(BASE + '/reports/incidents?' + reportQuery());
    const values = [data.createdCount, data.resolvedCount, data.openAtCutoff];
    metrics.querySelectorAll('strong').forEach((node, index) => { node.textContent = number(values[index]); });
    byId('reportCutoff').textContent = 'Kỳ ' + data.startDate.split('-').reverse().join('/') + ' – ' + data.endDate.split('-').reverse().join('/') + ' · Chốt: ' + timestamp(data.cutoff) + '. Số còn mở gồm sự cố từ trước kỳ chưa đóng.';
    const groups = data.breakdowns.open.status;
    const container = byId('incidentBreakdowns');
    if (!Object.keys(groups).length) {
      empty(container, 'Không có sự cố còn mở cuối kỳ', 'Thống kê dựa trên lịch sử sự cố trong kỳ đã chọn.');
    } else {
      const list = element('div', null, 'breakdown-list');
      list.append(element('h3', 'Sự cố còn mở theo trạng thái'));
      Object.entries(groups).forEach(([label, count]) => {
        const row = element('div', null, 'breakdown-row');
        row.append(element('span', label), element('strong', number(count)));
        list.append(row);
      });
      container.replaceChildren(list);
    }
  } catch (error) {
    metrics.querySelectorAll('strong').forEach(node => { node.textContent = '—'; });
    byId('reportCutoff').textContent = 'Chưa có thời điểm chốt số liệu.';
    empty(byId('incidentBreakdowns'), 'Chưa tải được thống kê', errorMessage(error));
    throw error;
  } finally {
    metrics.setAttribute('aria-busy', 'false');
  }
}

async function loadIncidents() {
  const request = ++state.incidentRequest;
  const container = byId('incidentRows');
  byId('incidentsSection').setAttribute('aria-busy', 'true');
  emptyRow(container, 6, 'Đang tải danh sách sự cố…');
  try {
    const params = new URLSearchParams({ page: '0', size: '4' });
    if (byId('incidentStatus').value) params.set('status', byId('incidentStatus').value);
    const data = await fetchAPI(BASE + '/incidents?' + params);
    if (request !== state.incidentRequest) return;
    container.replaceChildren();
    if (!data.items.length) emptyRow(container, 6, 'Chưa có sự cố phù hợp. Bạn có thể đổi bộ lọc hoặc xem toàn bộ sự cố.');
    data.items.forEach(item => {
      const row = element('tr');
      row.append(element('td', item.subject), element('td', item.components.join(', ')), element('td', timestamp(item.createdAt)));
      [item.severityLabel, item.statusLabel].forEach(label => {
        const cell = element('td');
        const badge = element('span', label, 'status-badge');
        if (label === 'Nghiêm trọng') badge.dataset.tone = 'error';
        else if (['Cao', 'Mới ghi nhận', 'Đang xử lý'].includes(label)) badge.dataset.tone = 'warning';
        cell.append(badge);
        row.append(cell);
      });
      const action = element('td');
      const link = element('a', 'Xem sự cố ›');
      link.href = '/pages/operations-incident/?id=' + encodeURIComponent(item.incidentId);
      action.append(link);
      row.append(action);
      container.append(row);
    });
  } catch (error) {
    if (request !== state.incidentRequest) return;
    emptyRow(container, 6, errorMessage(error));
    throw error;
  } finally {
    if (request === state.incidentRequest) byId('incidentsSection').setAttribute('aria-busy', 'false');
  }
}

async function loadAudit() {
  const container = byId('auditRows');
  byId('activitySection').setAttribute('aria-busy', 'true');
  try {
    const data = await fetchAPI(BASE + '/booking-policy/history?page=0&size=4');
    container.replaceChildren();
    if (!data.items.length) emptyRow(container, 4, 'Chưa có thay đổi cấu hình. Lịch sử sẽ xuất hiện sau lần cập nhật đầu tiên.');
    data.items.forEach(item => {
      const row = element('tr');
      row.append(element('td', item.actorDisplayName), element('td', 'Cập nhật cấu hình học thử'), element('td', item.reason), element('td', timestamp(item.occurredAt)));
      container.append(row);
    });
  } catch (error) {
    emptyRow(container, 4, errorMessage(error));
    throw error;
  } finally {
    byId('activitySection').setAttribute('aria-busy', 'false');
  }
}

async function refreshDashboard() {
  if (state.busy) return;
  state.busy = true;
  byId('refreshDashboard').disabled = true;
  byId('reportPeriod').disabled = true;
  const notice = byId('dashboardNotice');
  notice.dataset.loading = 'true';
  notice.dataset.error = 'false';
  notice.textContent = 'Đang tải dữ liệu vận hành…';
  try {
    // Các nguồn độc lập: một nguồn lỗi không xóa phần khác đã tải được.
    const results = await Promise.allSettled([loadAccounts(), loadHealth(), loadReport(), loadIncidents(), loadAudit()]);
    const failed = results.filter(result => result.status === 'rejected');
    notice.dataset.error = String(failed.length > 0);
    notice.textContent = failed.length === results.length ? errorMessage(failed[0].reason) : failed.length ? 'Một số nội dung chưa tải được. Bạn có thể làm mới để thử lại.' : 'Đã cập nhật thống kê tài khoản và dữ liệu vận hành.';
  } finally {
    notice.dataset.loading = 'false';
    state.busy = false;
    byId('refreshDashboard').disabled = false;
    byId('reportPeriod').disabled = false;
  }
}

byId('refreshDashboard').addEventListener('click', refreshDashboard);
byId('reportPeriod').addEventListener('change', refreshDashboard);
byId('incidentStatus').addEventListener('change', () => { loadIncidents().catch(() => { /* Lỗi hiển thị ngay trong bảng. */ }); });
document.addEventListener('visibilitychange', () => { if (!document.hidden) refreshDashboard(); });
setInterval(() => {
  if (!document.hidden && !state.busy) loadHealth().catch(() => { /* Giữ lỗi tại khu vực sức khỏe. */ });
}, 30000);
setInterval(() => { if (state.health) renderHealth(); }, 5000);
refreshDashboard();
