import { OPERATIONS_BASE, api, byId, text, date, notice, query, emptyState, downloadFile, printTable, formatReportCell, createOperationsPage } from '../../js/pages/operations.js';

const base = OPERATIONS_BASE + '/reports/business/revenue';
const money = value => new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
const day = value => value.split('-').reverse().join('/');
const state = {params: '', period: '', integrated: true};

function tile(label, value, detail) {
  const node = text('div', '', 'statistics-tile');
  node.append(text('span', label), text('strong', value), text('span', detail));
  return node;
}

function renderPlans(rows) {
  const box = byId('revenuePlans'); box.replaceChildren();
  if (!rows.length) { box.append(emptyState(state.integrated ? 'Chưa có doanh thu trong kỳ' : 'Chưa tích hợp nguồn thanh toán', state.integrated ? 'Chưa có giao dịch gói hội viên nào thanh toán thành công trong khoảng thời gian này.' : 'Khi luồng thanh toán được nối, giao dịch sẽ hiển thị tại đây.')); return; }
  const max = Math.max(...rows.map(row => Number(row.amount))), list = text('div', '', 'chart-bars');
  rows.forEach(row => {
    const line = text('div', '', 'chart-bar-row'), track = text('div', '', 'chart-bar-track'), fill = text('div', '', 'chart-bar-fill');
    fill.style.width = (Number(row.amount) / max * 100) + '%'; track.append(fill);
    line.append(text('span', row.label + ' (' + row.count + ')'), track, text('strong', money(row.amount)));
    list.append(line);
  });
  box.append(list);
}

function renderRows(data) {
  const box = byId('revenueRows'); box.replaceChildren();
  if (!data.rows.length) { box.append(emptyState('Chưa có giao dịch trong kỳ', 'Giao dịch gói hội viên sẽ xuất hiện tại đây khi có thanh toán.')); return; }
  const table = document.createElement('table'), head = document.createElement('tr'), thead = document.createElement('thead'), body = document.createElement('tbody');
  // Mã giao dịch dài và không giúp đọc nhanh nên chỉ giữ trong tệp xuất.
  data.headers.slice(1).forEach(label => { const th = text('th', label); th.scope = 'col'; head.append(th); });
  thead.append(head);
  data.rows.forEach(row => { const line = document.createElement('tr'); row.slice(1).forEach(value => line.append(text('td', formatReportCell(value)))); body.append(line); });
  table.append(text('caption', 'Giao dịch gói hội viên trong kỳ', 'sr-only'), thead, body); box.append(table);
}

async function load() {
  const params = query(byId('revenueFilters')).toString(), suffix = params ? '?' + params : '';
  const [data, rows] = await Promise.all([api(base + suffix), api(base + '/rows' + suffix)]);
  const form = byId('revenueFilters').elements; form.startDate.value = data.startDate; form.endDate.value = data.endDate;
  state.params = 'startDate=' + data.startDate + '&endDate=' + data.endDate;
  state.period = 'Kỳ ' + day(data.startDate) + ' đến ' + day(data.endDate);
  byId('revenuePeriod').textContent = state.period + ' · Cập nhật: ' + date(data.generatedAt) + '. Tối đa 366 ngày theo giờ Việt Nam.';
  byId('revenueTiles').replaceChildren(tile('Doanh thu', money(data.revenue), data.successCount + ' giao dịch thành công'), tile('Chưa thành công', String(data.pendingCount + data.failedCount), data.pendingCount + ' đang chờ, ' + data.failedCount + ' thất bại'));
  state.integrated = data.sourceIntegrated;
  renderPlans(data.byPlan); renderRows(rows);
  notice(rows.rows.length ? 'Đã tổng hợp từ giao dịch thật trong kỳ.' : 'Chưa có giao dịch gói hội viên trong kỳ này.');
}

async function excel() {
  const saved = await downloadFile(base + '/export?' + state.params, 'doanh-thu.xlsx');
  notice(saved ? 'Đã tạo tệp Excel doanh thu.' : 'Chưa có giao dịch trong kỳ nên chưa tạo tệp.');
}

async function pdf() {
  const data = await api(base + '/rows?' + state.params);
  if (!data.rows.length) { notice('Chưa có giao dịch trong kỳ nên chưa tạo tệp.'); return; }
  printTable('Báo cáo doanh thu gói hội viên', state.period, data.headers, data.rows);
  notice('Trong hộp thoại in, chọn "Lưu dưới dạng PDF" để lưu tệp.');
}

const page = createOperationsPage(load);
page.bind('revenueFilters', 'submit', load);
page.bind('revenueExcel', 'click', excel);
page.bind('revenuePdf', 'click', pdf);
page.start();
