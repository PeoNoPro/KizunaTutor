/** Biểu đồ dùng chung cho Tổng quan và Thống kê: một đại lượng một màu, số liệu giữ màu chữ, luôn có dạng bảng. */
const number = value => new Intl.NumberFormat('vi-VN').format(value);
const day = value => value.split('-').reverse().join('/');

function node(tag, value, className) {
  const item = document.createElement(tag);
  if (value != null) item.textContent = String(value);
  if (className) item.className = className;
  return item;
}

function dataTable(caption, heading, rows, format) {
  const box = node('details', null, 'chart-table'), scroll = node('div', null, 'chart-table-scroll'), table = document.createElement('table');
  box.append(node('summary', 'Xem dạng bảng'));
  const head = document.createElement('tr'); [heading, 'Số lượng'].forEach(label => { const th = node('th', label); th.scope = 'col'; head.append(th); });
  const thead = document.createElement('thead'); thead.append(head);
  const body = document.createElement('tbody');
  rows.forEach(row => { const line = document.createElement('tr'); line.append(node('td', row.label), node('td', format(row.count))); body.append(line); });
  table.append(node('caption', caption, 'sr-only'), thead, body); scroll.append(table); box.append(scroll);
  return box;
}

function markTip(item, tip) {
  item.tabIndex = 0; item.setAttribute('aria-label', tip); item.dataset.tip = tip;
}

export function barChart(container, caption, rows, emptyMessage) {
  container.replaceChildren();
  const total = rows.reduce((sum, row) => sum + row.count, 0);
  if (!total) { container.append(node('div', emptyMessage, 'chart-empty')); return; }
  const max = Math.max(...rows.map(row => row.count)), list = node('div', null, 'chart-bars');
  rows.filter(row => row.count > 0).forEach(row => {
    const line = node('div', null, 'chart-bar-row'), track = node('div', null, 'chart-bar-track'), fill = node('div', null, 'chart-bar-fill');
    const percent = new Intl.NumberFormat('vi-VN', {maximumFractionDigits: 1}).format(row.count / total * 100);
    fill.style.width = (row.count / max * 100) + '%'; track.append(fill);
    line.append(node('span', row.label), track, node('strong', number(row.count)));
    markTip(line, row.label + ': ' + number(row.count) + ' (' + percent + '%)');
    list.append(line);
  });
  container.append(list, dataTable(caption, 'Nhóm', rows, number));
}

/** Kỳ dài gộp theo tuần/tháng để cột vẫn đủ rộng để đọc và rê chuột. */
function buckets(days) {
  const size = days.length <= 31 ? 1 : days.length <= 120 ? 7 : 30, rows = [];
  for (let i = 0; i < days.length; i += size) {
    const part = days.slice(i, i + size), first = day(part[0].date), last = day(part.at(-1).date);
    rows.push({label: size === 1 ? first : first + ' – ' + last, count: part.reduce((sum, row) => sum + Number(row.count), 0)});
  }
  return rows;
}

/** `days` gồm {date, count}; `format` đổi số sang chữ, ví dụ thêm đơn vị tiền. */
export function columnChart(container, days, {headline, empty, caption, format = number}) {
  container.replaceChildren();
  const total = days.reduce((sum, row) => sum + Number(row.count), 0), head = node('p', null, 'chart-headline');
  head.append(node('strong', format(total)), node('span', headline));
  container.append(head);
  if (!total) { container.append(node('div', empty, 'chart-empty')); return; }
  const rows = buckets(days), max = Math.max(...rows.map(row => row.count)), chart = node('div', null, 'chart-columns');
  rows.forEach(row => {
    const column = node('div', null, 'chart-column'), fill = node('div', null, 'chart-column-fill');
    fill.style.height = (row.count / max * 100) + '%'; column.append(fill);
    markTip(column, row.label + ': ' + format(row.count));
    chart.append(column);
  });
  const axis = node('div', null, 'chart-axis');
  axis.append(node('span', day(days[0].date)), node('span', 'Cao nhất: ' + format(max)), node('span', day(days.at(-1).date)));
  container.append(chart, axis, dataTable(caption, 'Thời gian', rows, format));
}

export function chartError(container, message) {
  container.replaceChildren(node('div', message, 'chart-empty chart-error'));
}

export function bindChartTooltip() {
  let tip = document.getElementById('chartTip');
  if (!tip) { tip = node('div', null, 'chart-tip'); tip.id = 'chartTip'; tip.setAttribute('role', 'presentation'); tip.hidden = true; document.body.append(tip); }
  const show = (target, x, y) => {
    tip.textContent = target.dataset.tip; tip.hidden = false;
    const box = tip.getBoundingClientRect();
    tip.style.left = Math.max(8, Math.min(x - box.width / 2, innerWidth - box.width - 8)) + 'px';
    tip.style.top = Math.max(8, y - box.height - 10) + 'px';
  };
  document.addEventListener('pointermove', event => {
    const target = event.target.closest?.('[data-tip]');
    if (target) show(target, event.clientX, event.clientY); else tip.hidden = true;
  });
  document.addEventListener('focusin', event => {
    const target = event.target.closest?.('[data-tip]');
    if (!target) { tip.hidden = true; return; }
    const box = target.getBoundingClientRect(); show(target, box.left + box.width / 2, box.top);
  });
  document.addEventListener('focusout', () => { tip.hidden = true; });
}
