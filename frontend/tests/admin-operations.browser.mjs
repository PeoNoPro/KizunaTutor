// Kiểm tra khung quản trị với backend thật trên DB phát triển riêng, không dùng fixture phản hồi.
import assert from 'node:assert/strict';
import {mkdir, readFile, writeFile} from 'node:fs/promises';
const backend = process.env.ADMIN_DEV_BACKEND || 'http://localhost:8085';
const configSource = (await readFile(new URL('../js/config/api.config.js', import.meta.url), 'utf8')).replace('http://localhost:8085/api', backend + '/api');
const output = new URL('../../../.dashboard-qa/', import.meta.url);
await mkdir(output, {recursive: true});
const target = await (await fetch('http://localhost:9337/json/new?about:blank', {method: 'PUT'})).json();
const socket = new WebSocket(target.webSocketDebuggerUrl);
await new Promise(resolve => socket.addEventListener('open', resolve, {once: true}));
let sequence = 0;
const pending = new Map();
const errors = [];
function command(method, params = {}) {
  const id = ++sequence;
  return new Promise((resolve, reject) => { pending.set(id, {resolve, reject}); socket.send(JSON.stringify({id, method, params})); });
}
socket.addEventListener('message', async event => {
  const message = JSON.parse(event.data);
  if (message.id) {
    const request = pending.get(message.id); pending.delete(message.id);
    if (message.error) request?.reject(new Error(message.error.message)); else request?.resolve(message.result);
  }
  if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails.text);
  if (message.method === 'Page.javascriptDialogOpening') await command('Page.handleJavaScriptDialog', {accept: true});
  if (message.method === 'Fetch.requestPaused') {
    // Chỉ đổi cấu hình địa chỉ backend trong tab kiểm thử. API và cookie dùng server thật.
    await command('Fetch.fulfillRequest', {requestId: message.params.requestId, responseCode: 200,
      responseHeaders: [{name: 'Content-Type', value: 'application/javascript'}], body: Buffer.from(configSource).toString('base64')});
  }
});
async function evaluate(expression) {
  const result = await command('Runtime.evaluate', {expression, returnByValue: true, awaitPromise: true});
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.text);
  return result.result.value;
}
async function until(expression) {
  const started = Date.now();
  while (Date.now() - started < 12000) {
    if (await evaluate(expression)) return;
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  throw new Error('Không đạt điều kiện: ' + expression + '\n' + await evaluate('document.body.innerText'));
}
async function visit(page) {
  await command('Page.navigate', {url: 'http://localhost:3000/pages/' + page + '/'});
  await until(`location.pathname === '/pages/${page}/' && document.body.dataset.ready === 'true'`);
  assert.equal(await evaluate('document.body.dataset.error'), 'false');
  assert.equal(await evaluate('document.querySelectorAll("#adminSidebar").length'), 1);
  assert.equal(await evaluate('document.querySelectorAll(".sidebar-menu [aria-current=page]").length'), 1);
  assert.equal(await evaluate('document.querySelectorAll("header").length'), 1);
  assert.equal(await evaluate('document.querySelectorAll("footer").length'), 1);
  assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'), true);
}
async function screenshot(name) {
  const shot = await command('Page.captureScreenshot', {format: 'png', captureBeyondViewport: true});
  await writeFile(new URL(name + '.png', output), Buffer.from(shot.data, 'base64'));
}
async function fill(id, value) { await evaluate(`document.getElementById(${JSON.stringify(id)}).value = ${JSON.stringify(value)}`); }
async function submitAction(action) {
  await fill('commandAction', action);
  await evaluate('document.getElementById("commandAction").dispatchEvent(new Event("change"))');
  await evaluate('document.getElementById("incidentCommandForm").requestSubmit()');
  await until('document.getElementById("operationsContent").getAttribute("aria-busy") === "false"');
  assert.equal(await evaluate('document.body.dataset.error'), 'false');
}
try {
  await command('Page.enable');
  await command('Runtime.enable');
  await command('Fetch.enable', {patterns: [{urlPattern: 'http://localhost:3000/js/config/api.config.js', requestStage: 'Request'}]});
  for (const width of [1280, 390]) {
    await command('Emulation.setDeviceMetricsOverride', {width, height: 900, deviceScaleFactor: 1, mobile: width < 760});
    for (const page of ['operations-config', 'operations-health', 'operations-incidents', 'operations-reports']) {
      await visit(page);
      await evaluate('document.fonts.ready');
      await screenshot(page + '-' + width);
    }
  }
  await visit('operations-incidents');
  await evaluate('document.getElementById("createIncidentPanel").open = true');
  const subject = 'Kiểm tra giao diện quản trị cục bộ ' + Date.now();
  await fill('incidentSubject', subject);
  await fill('incidentDescription', 'Bản ghi phát triển riêng để kiểm chứng luồng tạo và xử lý sự cố qua giao diện.');
  await evaluate('document.querySelector("input[name=component]").checked = true; document.getElementById("incidentCreateForm").requestSubmit()');
  await until('location.pathname === "/pages/operations-incident/" && document.body.dataset.ready === "true"');
  assert.equal(await evaluate('document.body.dataset.error'), 'false');
  assert.equal(await evaluate('document.querySelector("#incidentDetail h2").textContent'), subject);
  await submitAction('claim');
  assert.equal(await evaluate('document.getElementById("incidentDetail").textContent.includes("Đang xử lý")'), true);
  await fill('commandDetail', 'Đã đối chiếu giao diện và dữ liệu từ backend cục bộ.');
  await submitAction('progress');
  await fill('commandResult', 'Đã kiểm tra luồng cập nhật.');
  await fill('commandVerification', 'Nội dung mới và lịch sử xuất hiện đúng sau khi tải lại.');
  await submitAction('resolve');
  assert.equal(await evaluate('document.getElementById("incidentDetail").textContent.includes("Đã giải quyết")'), true);
  await submitAction('close');
  assert.equal(await evaluate('document.getElementById("incidentDetail").textContent.includes("Đã đóng")'), true);
  await screenshot('operations-detail-390');
  await command('Page.navigate', {url: 'http://localhost:3000/pages/admin-dashboard/'});
  await until('document.getElementById("dashboardNotice")?.dataset.loading === "false"');
  assert.equal(await evaluate('document.getElementById("dashboardNotice").dataset.error'), 'false');
  assert.equal(await evaluate('document.querySelectorAll(".health-card").length > 0'), true);
  const accountResponse = await (await fetch(backend + '/api/admin/operations/dashboard/accounts')).json();
  assert.equal(accountResponse.success, true);
  assert.equal(await evaluate('document.querySelector("[data-account-count=totalAccounts]").textContent'), new Intl.NumberFormat('vi-VN').format(accountResponse.data.totalAccounts));
  assert.equal(await evaluate('document.querySelector("[data-account-count=staffAccounts]").textContent'), new Intl.NumberFormat('vi-VN').format(accountResponse.data.staffAccounts));
  await command('Emulation.setDeviceMetricsOverride', {width: 1280, height: 900, deviceScaleFactor: 1, mobile: false});
  await screenshot('dashboard-local-accounts-1280');
  assert.deepEqual(errors, []);
  await writeFile(new URL('operations-live-results.json', output), JSON.stringify({passed: true, backend, checks: ['shared shell desktop/mobile', 'active menu', 'configuration read', 'health read', 'reports read', 'create/claim/progress/resolve/close via UI', 'dashboard real data'], subject}, null, 2));
  console.log('Live admin operations browser checks passed.');
} finally {
  await command('Page.close');
  socket.close();
}
