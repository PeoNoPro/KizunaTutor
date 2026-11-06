// Kiểm thử trình duyệt qua Chrome headless; fixture chỉ nằm trong thư mục tests.
// Chạy frontend/server.js và Chrome với --remote-debugging-port=9337 trước khi chạy file này.
import assert from 'node:assert/strict';
import {mkdir, writeFile} from 'node:fs/promises';

const output = new URL('../../../.dashboard-qa/', import.meta.url);
await mkdir(output, {recursive: true});
const target = await (await fetch('http://localhost:9337/json/new?about:blank', {method: 'PUT'})).json();
const socket = new WebSocket(target.webSocketDebuggerUrl);
await new Promise(resolve => socket.addEventListener('open', resolve, {once: true}));
let nextId = 0;
const pending = new Map();
const errors = [];
const requests = [];
let scenario = 'success';

function command(method, params = {}) {
  const id = ++nextId;
  return new Promise((resolve, reject) => {
    pending.set(id, {resolve, reject});
    socket.send(JSON.stringify({id, method, params}));
  });
}

function fixture(path) {
  if (path.endsWith('/me')) return {displayName: 'Quản trị kiểm thử', roleLabel: 'Quản trị viên hệ thống'};
  if (path.endsWith('/dashboard/accounts')) return {totalAccounts: 6, activeAccounts: 5, tutorAccounts: 1, activeTutorAccounts: 1, learnerAccounts: 1, activeLearnerAccounts: 0, staffAccounts: 4, activeStaffAccounts: 4, otherAccounts: 0, generatedAt: new Date().toISOString()};
  if (path.endsWith('/reports/incidents')) return {startDate: '2026-10-03', endDate: '2026-11-01', cutoff: '2026-11-01T10:00:00Z', createdCount: 2, resolvedCount: 1, openAtCutoff: 1, breakdowns: {open: {status: {'Đang xử lý': 1}}}};
  if (path.endsWith('/dashboard/account-statistics')) return {newAccounts: [{date: '2026-10-31', count: 2}, {date: '2026-11-01', count: 4}], roles: [{label: 'Học viên', count: 3}, {label: 'Gia sư', count: 1}], statuses: [{label: 'Đang hoạt động', count: 5}, {label: 'Tạm khóa', count: 1}]};
  if (path.endsWith('/reports/business/revenue')) return {daily: [{date: '2026-10-31', count: 200000}, {date: '2026-11-01', count: 300000}], revenue: 500000, successCount: 3, refunded: 0, refundedCount: 0, pendingCount: 0, failedCount: 0, byPlan: [{label: 'Gói kiểm thử', count: 3, amount: 500000}]};
  if (path.endsWith('/audit-log')) return {items: [{actorDisplayName: 'Người kiểm thử', actionLabel: 'Cập nhật cấu hình', reason: '<script>window.fixtureExecuted=true</script>', occurredAt: '2026-11-01T10:00:00Z'}]};
  return {items: [{incidentId: 'test-only', subject: '<img src=x onerror="window.fixtureExecuted=true">', components: ['Thành phần kiểm thử'], createdAt: '2026-11-01T10:00:00Z', severityLabel: 'Cao', statusLabel: 'Đang xử lý'}]};
}

socket.addEventListener('message', async event => {
  const message = JSON.parse(event.data);
  if (message.id) {
    const promise = pending.get(message.id);
    pending.delete(message.id);
    if (message.error) promise?.reject(new Error(message.error.message));
    else promise?.resolve(message.result);
  }
  if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails.text);
  if (message.method !== 'Fetch.requestPaused') return;
  const {requestId, request} = message.params;
  const url = new URL(request.url);
  requests.push(url.pathname + url.search);
  if (scenario === 'timeout') return;
  let status = 200;
  let data = fixture(url.pathname);
  if (scenario === 'empty') {
    if (url.pathname.endsWith('/dashboard/accounts')) Object.keys(data).filter(key => key !== 'generatedAt').forEach(key => { data[key] = 0; });
    else if (url.pathname.endsWith('/reports/business/revenue')) data = {...data, revenue: 0, successCount: 0, byPlan: [], daily: data.daily.map(row => ({...row, count: 0}))};
    else if (url.pathname.endsWith('/reports/incidents')) data = {...data, createdCount: 0, resolvedCount: 0, openAtCutoff: 0, breakdowns: {open: {status: {}}}};
    else data.items = [];
  }
  if (scenario === 'unauthorized') status = 401;
  if (scenario === 'partial' && url.pathname.endsWith('/reports/incidents')) status = 503;
  const response = status === 200 ? {success: true, data} : {success: false, message: 'Thông báo kiểm thử cách ly.'};
  await command('Fetch.fulfillRequest', {requestId, responseCode: status, responseHeaders: [
    {name: 'Content-Type', value: 'application/json'},
    {name: 'Access-Control-Allow-Origin', value: 'http://localhost:3000'},
    {name: 'Access-Control-Allow-Methods', value: 'GET, OPTIONS'},
    {name: 'Access-Control-Allow-Headers', value: 'Content-Type'},
    {name: 'Access-Control-Allow-Credentials', value: 'true'}
  ], body: Buffer.from(JSON.stringify(response)).toString('base64')});
});

const evaluate = async expression => {
  const result = await command('Runtime.evaluate', {expression, returnByValue: true, awaitPromise: true});
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.text);
  return result.result.value;
};
async function until(expression, timeout = 8000) {
  const start = Date.now();
  while (Date.now() - start < timeout) {
    if (await evaluate(expression)) return;
    await new Promise(resolve => setTimeout(resolve, 80));
  }
  throw new Error('Không đạt điều kiện: ' + expression);
}
async function visit() {
  await command('Page.navigate', {url: 'http://localhost:3000/pages/admin-dashboard/'});
  await until('document.querySelector("#dashboardNotice")?.dataset.loading === "false"');
}
async function screenshot(name) {
  const result = await command('Page.captureScreenshot', {format: 'png', captureBeyondViewport: true});
  await writeFile(new URL(name, output), Buffer.from(result.data, 'base64'));
}

try {
  await command('Page.enable');
  await command('Runtime.enable');
  await command('Fetch.enable', {patterns: [{urlPattern: 'http://localhost:8085/api/*', requestStage: 'Request'}]});
  await command('Emulation.setDeviceMetricsOverride', {width: 1280, height: 900, deviceScaleFactor: 1, mobile: false});
  await visit();
  await evaluate('document.fonts.ready');
  assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'), true);
  assert.equal(await evaluate('document.querySelector("[data-account-count=totalAccounts]").textContent'), '6');
  assert.equal(await evaluate('/sức khỏe|giám sát|giờ cao điểm|thiết bị truy cập/i.test(document.body.textContent)'), false);
  assert.equal(requests.some(url => url.includes('/health')), false);
  assert.equal(await evaluate('document.querySelector("#revenueMetrics strong").textContent.includes("500.000")'), true);
  assert.equal(await evaluate('document.querySelectorAll("#overviewNewAccounts .chart-column").length'), 2);
  assert.equal(await evaluate('document.querySelectorAll("#overviewRevenue .chart-column").length'), 2);
  assert.equal(await evaluate('document.querySelectorAll("#overviewRoles .chart-bar-row").length'), 2);
  assert.equal(await evaluate('document.querySelector("#overviewRevenue .chart-headline").textContent.includes("500.000")'), true);
  assert.equal(await evaluate('document.querySelectorAll(".export-actions button:disabled").length'), 0);
  assert.equal(await evaluate('document.querySelector("#auditRows").textContent.includes("Cập nhật cấu hình")'), true);
  assert.equal(await evaluate('window.fixtureExecuted === undefined'), true);
  assert.equal(await evaluate('document.querySelectorAll("#incidentRows img, #auditRows script").length'), 0);
  assert.equal(await evaluate('document.querySelector("#auditRows").textContent.includes("17:00")'), true);
  assert.equal(await evaluate('Array.from(document.images).every(img => img.complete && img.naturalWidth > 0)'), true);
  await screenshot('desktop.png');
  await evaluate('document.querySelector("#incidentStatus").value="IN_PROGRESS"; document.querySelector("#incidentStatus").dispatchEvent(new Event("change"))');
  await until('document.querySelector("#incidentsSection").getAttribute("aria-busy") === "false"');
  assert(requests.some(url => url.includes('status=IN_PROGRESS')));
  await command('Emulation.setDeviceMetricsOverride', {width: 390, height: 844, deviceScaleFactor: 1, mobile: true});
  assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'), true);
  await until('document.querySelector("#adminSidebar").inert');
  await evaluate('document.querySelector("#sidebarToggle").click()');
  assert.equal(await evaluate('document.querySelector("#sidebarToggle").getAttribute("aria-expanded")'), 'true');
  await command('Input.dispatchKeyEvent', {type: 'keyDown', key: 'Escape', code: 'Escape'});
  assert.equal(await evaluate('document.querySelector("#sidebarToggle").getAttribute("aria-expanded")'), 'false');
  await until('document.querySelector("#adminSidebar").getBoundingClientRect().right <= 0');
  await screenshot('mobile.png');
  for (const width of [320, 768, 1440]) {
    await command('Emulation.setDeviceMetricsOverride', {width, height: 900, deviceScaleFactor: 1, mobile: width < 768});
    assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'), true, 'Tràn ngang ở chiều rộng ' + width);
  }
  await command('Emulation.setDeviceMetricsOverride', {width: 390, height: 844, deviceScaleFactor: 1, mobile: true});
  scenario = 'empty';
  await visit();
  assert.equal(await evaluate('document.querySelector("#incidentMetrics strong").textContent'), '0');
  assert.equal(await evaluate('document.querySelector("[data-account-count=totalAccounts]").textContent'), '0');
  assert.equal(await evaluate('document.querySelector("#incidentRows").textContent.includes("Chưa có sự cố")'), true);
  scenario = 'partial';
  await visit();
  assert.equal(await evaluate('document.querySelector("#incidentMetrics strong").textContent'), '—');
  assert.equal(await evaluate('document.querySelector("[data-account-count=totalAccounts]").textContent'), '6');
  scenario = 'unauthorized';
  await command('Page.navigate', {url: 'http://localhost:3000/pages/admin-dashboard/'});
  await until('location.pathname === "/pages/login/"');
  scenario = 'timeout';
  const start = Date.now();
  await visit();
  assert(Date.now() - start < 7000);
  assert.equal(await evaluate('document.querySelector("#dashboardNotice").dataset.error'), 'true');
  assert.equal(await evaluate('document.querySelector("#refreshDashboard").disabled'), false);
  assert.deepEqual(errors, []);
  await writeFile(new URL('results.json', output), JSON.stringify({passed: true, checks: ['desktop/mobile layout', 'local assets', 'empty', 'partial failure', 'unauthorized', 'timeout', 'no health block', 'escaping', 'timezone', 'filter', 'sidebar keyboard'], requests}, null, 2));
  console.log('Dashboard browser checks passed. Screenshots: ' + output.pathname);
} finally {
  await command('Page.close');
  socket.close();
}
