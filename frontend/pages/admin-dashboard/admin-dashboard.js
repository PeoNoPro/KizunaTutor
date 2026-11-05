import { mountAdminShell } from '../../components/admin-shell/admin-shell.js';
mountAdminShell();
import { fetchAPI } from '../../js/api.js';
import { downloadFile, printTable } from '../../js/pages/operations.js';
import { barChart, columnChart, chartError, bindChartTooltip } from '../../js/pages/admin-charts.js';

const BASE = '/admin/operations';
const byId = id => document.getElementById(id);
const number = value => new Intl.NumberFormat('vi-VN').format(value);
const timestamp = value => value ? new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh'
}).format(new Date(value)) : 'Chưa có thời gian';
const state = { busy: false, incidentRequest: 0 };

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
    const open = groups => Object.entries(groups || {}).map(([label, count]) => ({label, count}));
    barChart(byId('overviewIncidentStatus'), 'Sự cố còn mở theo trạng thái', open(groups), 'Không có sự cố còn mở cuối kỳ.');
    barChart(byId('overviewIncidentSeverity'), 'Sự cố còn mở theo mức độ', open(data.breakdowns.open.severity), 'Không có sự cố còn mở cuối kỳ.');
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
    ['overviewIncidentStatus', 'overviewIncidentSeverity'].forEach(id => chartError(byId(id), 'Chưa tải được số liệu sự cố.'));
    throw error;
  } finally {
    metrics.setAttribute('aria-busy', 'false');
  }
}

const money = value => number(value) + ' ₫';

async function loadCharts() {
  try {
    const data = await fetchAPI(BASE + '/dashboard/account-statistics?' + reportQuery());
    columnChart(byId('overviewNewAccounts'), data.newAccounts, {headline: 'tài khoản mới trong kỳ', empty: 'Không có tài khoản mới trong kỳ đã chọn.', caption: 'Người dùng mới theo ngày'});
    barChart(byId('overviewRoles'), 'Người dùng theo vai trò', data.roles, 'Chưa có tài khoản trong hệ thống.');
    barChart(byId('overviewStatuses'), 'Trạng thái tài khoản', data.statuses, 'Chưa có tài khoản trong hệ thống.');
  } catch (error) {
    // Lỗi không vẽ thành số 0 để không bị hiểu nhầm là chưa có người dùng.
    ['overviewNewAccounts', 'overviewRoles', 'overviewStatuses'].forEach(id => chartError(byId(id), 'Chưa tải được số liệu tài khoản.'));
    throw error;
  }
}

async function loadRevenue() {
  const section = byId('revenueSection'), metrics = byId('revenueMetrics');
  section.setAttribute('aria-busy', 'true');
  try {
    const data = await fetchAPI(BASE + '/reports/business/revenue?' + reportQuery());
    const values = [money(data.revenue), number(data.successCount), number(data.pendingCount + data.failedCount)];
    metrics.querySelectorAll('strong').forEach((node, index) => { node.textContent = values[index]; });
    columnChart(byId('overviewRevenue'), data.daily, {headline: 'doanh thu trong kỳ', empty: 'Chưa có doanh thu trong kỳ đã chọn.', caption: 'Doanh thu theo ngày', format: money});
    const container = byId('revenuePlans');
    if (!data.byPlan.length) empty(container, data.sourceIntegrated === false ? 'Chưa tích hợp nguồn thanh toán' : 'Chưa có doanh thu trong kỳ', data.sourceIntegrated === false ? 'Khi luồng thanh toán được nối, doanh thu sẽ hiển thị tại đây.' : 'Chưa có giao dịch gói hội viên nào thanh toán thành công trong khoảng thời gian này.');
    else {
      const list = element('div', null, 'breakdown-list');
      list.append(element('h3', 'Doanh thu theo gói'));
      data.byPlan.forEach(plan => { const row = element('div', null, 'breakdown-row'); row.append(element('span', plan.label + ' (' + number(plan.count) + ')'), element('strong', money(plan.amount))); list.append(row); });
      container.replaceChildren(list);
    }
  } catch (error) {
    metrics.querySelectorAll('strong').forEach(node => { node.textContent = '—'; });
    empty(byId('revenuePlans'), 'Chưa tải được doanh thu', errorMessage(error));
    chartError(byId('overviewRevenue'), 'Chưa tải được doanh thu.');
    throw error;
  } finally {
    section.setAttribute('aria-busy', 'false');
  }
}

function bindExport(id, action) {
  byId(id).addEventListener('click', async () => {
    const notice = byId('exportNotice'), button = byId(id);
    button.disabled = true; notice.textContent = 'Đang chuẩn bị tệp…';
    try { notice.textContent = await action(); }
    catch (error) { notice.textContent = error.message || 'Chưa tạo được tệp. Vui lòng thử lại.'; }
    finally { button.disabled = false; }
  });
}
const REPORTS = BASE + '/reports/business';
const savedMessage = saved => saved ? 'Đã tạo tệp Excel.' : 'Chưa có dữ liệu nên chưa tạo tệp.';
async function printReport(path, title, subtitle) {
  const data = await fetchAPI(path);
  if (!data.rows.length) return 'Chưa có dữ liệu nên chưa tạo tệp.';
  printTable(title, subtitle, data.headers, data.rows);
  return 'Trong hộp thoại in, chọn "Lưu dưới dạng PDF" để lưu tệp.';
}
bindExport('tutorsExcel', async () => savedMessage(await downloadFile(REPORTS + '/tutors/export', 'gia-su-jlpt.xlsx')));
bindExport('tutorsPdf', () => printReport(REPORTS + '/tutors/rows', 'Đăng ký gia sư và JLPT', 'Tình trạng hiện tại'));
bindExport('revenueExcel', async () => savedMessage(await downloadFile(REPORTS + '/revenue/export?' + reportQuery(), 'doanh-thu.xlsx')));
bindExport('revenuePdf', () => { const query = reportQuery(); return printReport(REPORTS + '/revenue/rows?' + query, 'Doanh thu gói hội viên', 'Kỳ ' + query.get('startDate').split('-').reverse().join('/') + ' đến ' + query.get('endDate').split('-').reverse().join('/')); });

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
    const data = await fetchAPI(BASE + '/audit-log?page=0&size=4');
    container.replaceChildren();
    if (!data.items.length) emptyRow(container, 4, 'Chưa có hoạt động quản trị nào được ghi nhận.');
    data.items.forEach(item => {
      const row = element('tr');
      row.append(element('td', item.actorDisplayName), element('td', item.actionLabel), element('td', item.reason || 'Không có'), element('td', timestamp(item.occurredAt)));
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
    const results = await Promise.allSettled([loadAccounts(), loadCharts(), loadReport(), loadRevenue(), loadIncidents(), loadAudit()]);
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
// Nguồn phụ: lỗi hoặc DB chưa có bảng đề xuất thì chỉ không hiện số, không ảnh hưởng dashboard.
fetchAPI(BASE + '/role-changes/promotions?status=PENDING&size=1').then(data => {
  if (data.totalElements > 0) byId('promotionCount').textContent = ' (' + number(data.totalElements) + ')';
}).catch(() => {});
bindChartTooltip();
refreshDashboard();
