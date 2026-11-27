import { OPERATIONS_BASE, api, byId, text, notice, query, emptyState, badge, downloadFile, printTable, formatReportCell, createOperationsPage } from '../../js/pages/operations.js';

const base = OPERATIONS_BASE + '/reports/business/revenue';
const money = value => new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
const day = value => value.split('-').reverse().join('/');
const PAGE_SIZE = 10;
const state = {params: '', period: '', integrated: true, rows: null, page: 0};

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

/** Giao dịch nhiều thì lọc theo trạng thái và chia trang 10 dòng, không để bảng dài mãi. */
function renderRows() {
  const data = state.rows, box = byId('revenueRows'); box.replaceChildren();
  // Mã giao dịch dài và không giúp đọc nhanh nên chỉ giữ trong tệp xuất.
  const statusIndex = data.headers.indexOf('Trạng thái'), wanted = byId('revenueStatus').value;
  const rows = data.rows.filter(row => !wanted || String(row[statusIndex]) === wanted);
  const pages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE)); state.page = Math.min(state.page, pages - 1);
  byId('revenueCount').textContent = rows.length + ' giao dịch';
  byId('revenuePageInfo').textContent = 'Trang ' + (state.page + 1) + ' / ' + pages;
  byId('revenuePrev').disabled = state.page === 0; byId('revenueNext').disabled = state.page + 1 >= pages;
  if (!rows.length) { box.append(emptyState('Chưa có giao dịch phù hợp', 'Thử đổi khoảng thời gian hoặc trạng thái.')); return; }
  const table = document.createElement('table'), head = document.createElement('tr'), thead = document.createElement('thead'), body = document.createElement('tbody');
  data.headers.slice(1).forEach(label => { const th = text('th', label); th.scope = 'col'; head.append(th); });
  thead.append(head);
  rows.slice(state.page * PAGE_SIZE, (state.page + 1) * PAGE_SIZE).forEach(row => {
    const line = document.createElement('tr');
    row.slice(1).forEach((value, index) => {
      const cell = text('td', index + 1 === statusIndex ? '' : formatReportCell(value));
      if (index + 1 === statusIndex) cell.append(badge(String(value)));
      line.append(cell);
    });
    body.append(line);
  });
  table.append(text('caption', 'Giao dịch gói hội viên trong kỳ', 'sr-only'), thead, body); box.append(table);
}

async function load() {
  const params = query(byId('revenueFilters')).toString(), suffix = params ? '?' + params : '';
  const [data, rows] = await Promise.all([api(base + suffix), api(base + '/rows' + suffix)]);
  const form = byId('revenueFilters').elements; form.startDate.value = data.startDate; form.endDate.value = data.endDate;
  state.params = 'startDate=' + data.startDate + '&endDate=' + data.endDate;
  state.period = 'Kỳ ' + day(data.startDate) + ' đến ' + day(data.endDate);
  byId('revenueTiles').replaceChildren(tile('Doanh thu', money(data.revenue), data.successCount + ' giao dịch thành công'), tile('Chưa thành công', String(data.pendingCount + data.failedCount), data.pendingCount + ' đang chờ, ' + data.failedCount + ' thất bại'));
  state.integrated = data.sourceIntegrated;
  state.rows = rows; state.page = 0;
  renderPlans(data.byPlan); renderRows();
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
byId('revenueStatus').addEventListener('change', () => { state.page = 0; if (state.rows) renderRows(); });
byId('revenuePrev').addEventListener('click', () => { state.page--; renderRows(); });
byId('revenueNext').addEventListener('click', () => { state.page++; renderRows(); });
page.start();
