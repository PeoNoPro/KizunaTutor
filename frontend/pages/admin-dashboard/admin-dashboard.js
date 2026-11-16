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
// Tổng quan chỉ xem nhanh biểu đồ; bảng số liệu chi tiết nằm ở trang Thống kê.
const NO_TABLE = {table: false};

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
  try {
    const data = await fetchAPI(BASE + '/dashboard/accounts');
    document.querySelectorAll('[data-account-count]').forEach(node => {
      node.textContent = number(data[node.dataset.accountCount]);
    });
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

/** 4 thẻ đầu trang luôn so sánh 30 ngày gần nhất với 30 ngày trước đó (không theo ô chọn kỳ phía dưới). */
const KPI_DAYS = 30;
function kpiQuery() {
  const end = reportQuery().get('endDate'), start = new Date(end + 'T00:00:00Z');
  start.setUTCDate(start.getUTCDate() - KPI_DAYS + 1);
  return new URLSearchParams({startDate: start.toISOString().slice(0, 10), endDate: end});
}

/** Kỳ liền trước, cùng số ngày với kỳ truyền vào. */
function previousQuery(current = reportQuery()) {
  const start = new Date(current.get('startDate') + 'T00:00:00Z'), end = new Date(current.get('endDate') + 'T00:00:00Z');
  const days = Math.round((end - start) / 86400000) + 1;
  const prevEnd = new Date(start); prevEnd.setUTCDate(prevEnd.getUTCDate() - 1);
  const prevStart = new Date(prevEnd); prevStart.setUTCDate(prevStart.getUTCDate() - days + 1);
  return new URLSearchParams({startDate: prevStart.toISOString().slice(0, 10), endDate: prevEnd.toISOString().slice(0, 10)});
}

/** Nhãn tăng/giảm trên thẻ: ↗ +12% (xanh), ↘ −3% (đỏ), "Mới" khi 30 ngày trước bằng 0. Kèm chữ để không chỉ dựa vào màu. */
const TREND_NOUN = {total: 'tài khoản', ROLE_TUTOR: 'gia sư', ROLE_LEARNER: 'học viên'};
function showTrend(key, current, previous) {
  const chip = document.querySelector(`[data-trend="${key}"]`), compare = document.querySelector(`[data-compare="${key}"]`);
  if (!chip) return;
  let text, tone, percent = null;
  if (!previous) { text = current ? 'Mới' : 'Không đổi'; tone = current ? 'up' : 'flat'; }
  else {
    percent = Math.round((current - previous) * 1000 / previous) / 10;
    tone = percent > 0 ? 'up' : percent < 0 ? 'down' : 'flat';
    text = percent === 0 ? 'Không đổi' : (percent > 0 ? '↗ +' : '↘ −') + new Intl.NumberFormat('vi-VN', {maximumFractionDigits: 1}).format(Math.abs(percent)) + '%';
  }
  chip.textContent = text; chip.dataset.tone = tone; chip.hidden = false;
  const what = key === 'revenue' ? 'Doanh thu' : 'Số ' + TREND_NOUN[key] + ' đăng ký mới';
  chip.title = percent === null ? what + ' 30 ngày gần nhất (30 ngày trước đó: không có)'
    : what + (percent > 0 ? ' tăng ' : percent < 0 ? ' giảm ' : ' không đổi ') + (percent ? new Intl.NumberFormat('vi-VN', {maximumFractionDigits: 1}).format(Math.abs(percent)) + '% ' : '') + 'so với 30 ngày trước đó';
  if (compare) compare.textContent = key === 'revenue'
    ? 'Trong 30 ngày · 30 ngày trước: ' + money(previous)
    : number(current) + ' ' + TREND_NOUN[key] + ' đăng ký mới trong 30 ngày · 30 ngày trước: ' + number(previous);
}

/** Tỷ lệ tăng giảm của tài khoản mới (30 ngày) và doanh thu (30 ngày), cố định không theo ô chọn kỳ. */
async function loadOverview() {
  const query = kpiQuery();
  const [data, revenue, previous] = await Promise.all([fetchAPI(BASE + '/dashboard/overview?' + query),
    fetchAPI(BASE + '/reports/business/revenue?' + query), fetchAPI(BASE + '/reports/business/revenue?' + previousQuery(query)).catch(() => null)]);
  Object.entries(data.newAccounts).forEach(([key, value]) => showTrend(key, value.current, value.previous));
  byId('kpiRevenue').textContent = money(revenue.revenue); byId('kpiRevenueCount').textContent = number(revenue.successCount);
  if (previous) showTrend('revenue', Number(revenue.revenue), Number(previous.revenue));
}

/** Ngày đầu tháng của 11 tháng trước → hôm nay (giờ Việt Nam): đủ 12 tháng, không quá giới hạn 366 ngày của báo cáo. */
function monthQuery() {
  const end = reportQuery().get('endDate'), [y, m] = end.split('-').map(Number);
  const start = new Date(Date.UTC(y, m - 1 - 11, 1));
  return new URLSearchParams({startDate: start.toISOString().slice(0, 10), endDate: end});
}

async function loadMonthly() {
  const box = byId('monthlyRevenue');
  box.setAttribute('aria-busy', 'true');
  try {
    const query = monthQuery(), data = await fetchAPI(BASE + '/reports/business/revenue?' + query);
    const months = new Map(), [y, m] = query.get('startDate').split('-').map(Number);
    for (let i = 0; i < 12; i++) { const d = new Date(Date.UTC(y, m - 1 + i, 1)); months.set(d.toISOString().slice(0, 7), 0); }
    data.daily.forEach(day => { const key = day.date.slice(0, 7); if (months.has(key)) months.set(key, months.get(key) + Number(day.count)); });
    const rows = [...months].map(([key, amount]) => ({key, amount, label: 'T' + Number(key.slice(5)), full: 'Tháng ' + Number(key.slice(5)) + '/' + key.slice(0, 4)}));
    const total = rows.reduce((sum, row) => sum + row.amount, 0), max = Math.max(...rows.map(row => row.amount));
    byId('monthTotal').textContent = money(total);
    if (!total) { box.replaceChildren(element('div', 'Chưa có doanh thu trong 12 tháng gần nhất.', 'chart-empty')); return; }
    // Trục y 4 vạch, số làm tròn gọn (triệu đồng) để không chồng chữ.
    const step = niceStep(max / 4), top = step * 4;
    const grid = element('div', null, 'month-grid');
    for (let i = 4; i >= 0; i--) { const line = element('div', null, 'month-gridline'); line.append(element('span', shortMoney(step * i))); grid.append(line); }
    const bars = element('div', null, 'month-bars');
    rows.forEach((row, index) => {
      const column = element('div', null, 'month-column'), fill = element('div', null, 'month-fill');
      fill.style.height = (row.amount / top * 100) + '%';
      if (index === rows.length - 1) column.dataset.current = 'true';
      column.dataset.tip = row.full + ': ' + money(row.amount); column.tabIndex = 0;
      column.setAttribute('aria-label', row.full + ': ' + money(row.amount));
      column.append(fill, element('span', row.label, 'month-label'));
      bars.append(column);
    });
    grid.append(bars);
    // Bảng ẩn cho trình đọc màn hình.
    const table = element('table', null, 'sr-only'); table.append(element('caption', 'Doanh thu theo tháng'));
    rows.forEach(row => { const tr = element('tr'); tr.append(element('th', row.full), element('td', money(row.amount))); table.append(tr); });
    box.replaceChildren(grid, table);
  } catch (error) {
    chartError(box, 'Chưa tải được doanh thu theo tháng.');
    throw error;
  } finally { box.setAttribute('aria-busy', 'false'); }
}
function niceStep(raw) {
  if (raw <= 0) return 1;
  const power = 10 ** Math.floor(Math.log10(raw)), unit = raw / power;
  return (unit <= 1 ? 1 : unit <= 2 ? 2 : unit <= 5 ? 5 : 10) * power;
}
function shortMoney(value) {
  if (value >= 1e9) return new Intl.NumberFormat('vi-VN', {maximumFractionDigits: 1}).format(value / 1e9) + ' tỷ';
  if (value >= 1e6) return new Intl.NumberFormat('vi-VN', {maximumFractionDigits: 1}).format(value / 1e6) + ' tr';
  if (value >= 1e3) return new Intl.NumberFormat('vi-VN', {maximumFractionDigits: 0}).format(value / 1e3) + 'k';
  return number(value);
}

async function loadReport() {
  const metrics = byId('incidentMetrics');
  metrics.setAttribute('aria-busy', 'true');
  try {
    const data = await fetchAPI(BASE + '/reports/incidents?' + reportQuery());
    const values = [data.createdCount, data.resolvedCount, data.openAtCutoff];
    metrics.querySelectorAll('strong').forEach((node, index) => { node.textContent = number(values[index]); });
    const groups = data.breakdowns.open.status;
    const open = groups => Object.entries(groups || {}).map(([label, count]) => ({label, count}));
    barChart(byId('overviewIncidentStatus'), 'Tiến độ xử lý sự cố', open(groups), 'Hiện không có sự cố nào cần xử lý.', NO_TABLE);
    barChart(byId('overviewIncidentSeverity'), 'Mức độ nghiêm trọng của sự cố', open(data.breakdowns.open.severity), 'Hiện không có sự cố nào cần xử lý.', NO_TABLE);
    const container = byId('incidentBreakdowns');
    if (!Object.keys(groups).length) {
      empty(container, 'Hiện không có sự cố nào cần xử lý', 'Không còn sự cố nào đang mở.');
    } else {
      const list = element('div', null, 'breakdown-list');
      list.append(element('h3', 'Tiến độ xử lý sự cố'));
      Object.entries(groups).forEach(([label, count]) => {
        const row = element('div', null, 'breakdown-row');
        row.append(element('span', label), element('strong', number(count)));
        list.append(row);
      });
      container.replaceChildren(list);
    }
  } catch (error) {
    metrics.querySelectorAll('strong').forEach(node => { node.textContent = '—'; });
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
    columnChart(byId('overviewNewAccounts'), data.newAccounts, {headline: 'tài khoản mới ' + periodText(), empty: 'Không có tài khoản mới ' + periodText() + '.', caption: 'Người dùng mới theo ngày', table: false});
    barChart(byId('overviewRoles'), 'Người dùng theo vai trò', data.roles, 'Chưa có tài khoản trong hệ thống.', NO_TABLE);
    barChart(byId('overviewStatuses'), 'Trạng thái tài khoản', data.statuses, 'Chưa có tài khoản trong hệ thống.', NO_TABLE);
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
    columnChart(byId('overviewRevenue'), data.daily, {headline: 'doanh thu ' + periodText(), empty: 'Chưa có doanh thu ' + periodText() + '.', caption: 'Doanh thu theo ngày', format: money, table: false});
    const container = byId('revenuePlans');
    if (!data.byPlan.length) empty(container, data.sourceIntegrated === false ? 'Chưa tích hợp nguồn thanh toán' : 'Chưa có doanh thu ' + periodText(), data.sourceIntegrated === false ? 'Khi luồng thanh toán được nối, doanh thu sẽ hiển thị tại đây.' : 'Chưa có giao dịch gói hội viên nào thanh toán thành công trong khoảng thời gian này.');
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
        else if (['Cao', 'Chưa xử lý', 'Đang xử lý'].includes(label)) badge.dataset.tone = 'warning';
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

const AUDIT_ICONS = {'Nhân sự': 'nav-users.svg', 'Cấu hình': 'nav-settings.svg', 'Sự cố': 'nav-incidents.svg', 'Lịch ca CS': 'calendar.svg', 'Thông báo': 'nav-notifications.svg'};
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

/** "trong 30 ngày gần nhất", "trong 7 ngày gần nhất" hoặc "hôm nay", theo ô chọn thời gian. */
function periodText() {
  const value = byId('reportPeriod').value;
  return value === '1' ? 'hôm nay' : 'trong ' + value + ' ngày gần nhất';
}

async function refreshDashboard() {
  document.querySelectorAll('[data-period]').forEach(node => { node.textContent = periodText(); });
  if (state.busy) return;
  state.busy = true;
  byId('reportPeriod').disabled = true;
  const notice = byId('dashboardNotice');
  notice.hidden = true;
  try {
    // Các nguồn độc lập: một nguồn lỗi không xóa phần khác đã tải được.
    const fixed = state.fixedLoaded ? [] : [loadAccounts(), loadOverview(), loadMonthly(), loadAudit()];
    state.fixedLoaded = true;
    const results = await Promise.allSettled([...fixed, loadCharts(), loadReport(), loadRevenue(), loadIncidents()]);
    const failed = results.filter(result => result.status === 'rejected');
    // Chỉ hiện dòng thông báo khi có lỗi; tải thành công thì không cần chữ nào.
    notice.dataset.error = 'true';
    notice.hidden = failed.length === 0;
    notice.textContent = failed.length === results.length ? errorMessage(failed[0].reason) : failed.length ? 'Một số nội dung chưa tải được. Hãy đổi khoảng thời gian hoặc tải lại trang để thử lại.' : '';
  } finally {
    state.busy = false;
    byId('reportPeriod').disabled = false;
  }
}

byId('reportPeriod').addEventListener('change', refreshDashboard);
byId('incidentStatus').addEventListener('change', () => { loadIncidents().catch(() => { /* Lỗi hiển thị ngay trong bảng. */ }); });
// Quay lại tab thì tải lại toàn bộ, kể cả 4 thẻ đầu trang.
document.addEventListener('visibilitychange', () => { if (!document.hidden) { state.fixedLoaded = false; refreshDashboard(); } });
bindChartTooltip();
greet();
refreshDashboard();
