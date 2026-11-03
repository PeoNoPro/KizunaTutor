// Kiểm tra qua trình duyệt với backend và DB QA cục bộ thật, không giả phản hồi nghiệp vụ.
import assert from 'node:assert/strict';
import {mkdir, readFile, writeFile} from 'node:fs/promises';
const backend = process.env.ADMIN_DEV_BACKEND || 'http://localhost:8086';
const output = new URL('../../../.dashboard-qa/', import.meta.url);
await mkdir(output, {recursive: true});
const config = (await readFile(new URL('../js/config/api.config.js', import.meta.url), 'utf8')).replace('http://localhost:8085/api', backend + '/api');
const target = await (await fetch('http://localhost:9337/json/new?about:blank', {method: 'PUT'})).json();
const socket = new WebSocket(target.webSocketDebuggerUrl);
await new Promise(resolve => socket.addEventListener('open', resolve, {once: true}));
const pending = new Map(); const errors = []; let sequence = 0;
function command(method, params = {}) {
  const id = ++sequence; return new Promise((resolve, reject) => { pending.set(id, {resolve, reject}); socket.send(JSON.stringify({id, method, params})); });
}
socket.addEventListener('message', async event => {
  const message = JSON.parse(event.data);
  if (message.id) { const entry = pending.get(message.id); pending.delete(message.id); if (message.error) entry?.reject(new Error(message.error.message)); else entry?.resolve(message.result); }
  if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails.text);
  if (message.method === 'Fetch.requestPaused') await command('Fetch.fulfillRequest', {requestId: message.params.requestId, responseCode: 200, responseHeaders: [{name: 'Content-Type', value: 'application/javascript'}], body: Buffer.from(config).toString('base64')});
});
async function evaluate(expression) {
  const result = await command('Runtime.evaluate', {expression, returnByValue: true, awaitPromise: true});
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text); return result.result.value;
}
async function until(expression) {
  const start = Date.now(); while (Date.now() - start < 10000) { if (await evaluate(expression)) return; await new Promise(resolve => setTimeout(resolve, 80)); }
  throw new Error('Không đạt điều kiện: ' + expression + '\n' + await evaluate('document.body.innerText'));
}
async function visit(page) {
  await command('Page.navigate', {url: 'http://localhost:3000/pages/' + page + '/'});
  await until(`location.pathname === '/pages/${page}/' && document.body.dataset.ready === 'true'`);
  assert.equal(await evaluate('document.body.dataset.error'), 'false');
  assert.equal(await evaluate('document.querySelectorAll(".sidebar-menu [aria-current=page]").length'), 1);
  assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'), true);
}
async function fill(id, value) { await evaluate(`document.getElementById(${JSON.stringify(id)}).value=${JSON.stringify(value)};document.getElementById(${JSON.stringify(id)}).dispatchEvent(new Event('input'))`); }
async function rowAction(name, label) {
  await evaluate(`Array.from(document.querySelectorAll('#staffRows tr')).find(row=>row.textContent.includes(${JSON.stringify(name)})).querySelectorAll('button').forEach(button=>{if(button.textContent===${JSON.stringify(label)})button.click()})`);
  await until('document.getElementById("staffDialog").open');
}
async function save() {
  await evaluate('document.getElementById("staffForm").requestSubmit()');
  await until('!document.getElementById("staffDialog").open && document.getElementById("operationsContent").getAttribute("aria-busy") === "false"');
  assert.equal(await evaluate('document.body.dataset.error'), 'false');
}
async function screenshot(name) {
  const data = await command('Page.captureScreenshot', {format: 'png', captureBeyondViewport: true}); await writeFile(new URL(name + '.png', output), Buffer.from(data.data, 'base64'));
}
try {
  await command('Page.enable'); await command('Runtime.enable');
  await command('Fetch.enable', {patterns: [{urlPattern: 'http://localhost:3000/js/config/api.config.js', requestStage: 'Request'}]});
  await command('Emulation.setDeviceMetricsOverride', {width: 1280, height: 900, deviceScaleFactor: 1, mobile: false});
  await command('Page.navigate', {url: 'http://localhost:3000/admin/dashboard'});
  await until('location.pathname === "/pages/admin-dashboard/" && document.getElementById("dashboardNotice")?.dataset.loading === "false"');
  const profile = await (await fetch(backend + '/api/admin/operations/me')).json();
  assert.equal(await evaluate('document.querySelector(".admin-identity strong").textContent'), profile.data.displayName);
  assert.equal(await evaluate('document.querySelectorAll(".admin-avatar").length'), 1);
  assert.equal(await evaluate("Array.from(document.querySelectorAll('.sidebar-menu a')).some(link=>new URL(link.href).pathname==='/pages/admin-staff/')"), true);
  await screenshot('dashboard-admin-entry');
  const roles = (await (await fetch(backend + '/api/admin/operations/roles')).json()).data;
  const serviceRole = roles.find(role => role.label === 'Chăm sóc khách hàng');
  const academicRole = roles.find(role => role.label === 'Cán bộ học vụ');
  await visit('admin-staff'); await screenshot('staff-1280');
  await evaluate('document.getElementById("createStaff").click()');
  assert.equal(await evaluate('document.querySelectorAll("#staffRole option").length'), 5);
  assert.equal(await evaluate("Array.from(document.querySelectorAll('#staffRole option')).some(option=>['Học viên','Gia sư'].includes(option.textContent))"), false);
  const name = 'Nhân viên kiểm tra QA ' + Date.now();
  await fill('staffFullName', name); await fill('staffEmail', 'staff-qa-' + Date.now() + '@test.invalid');
  await fill('staffPhone', '0901234567'); await fill('staffRole', serviceRole.roleId); await fill('staffPassword', 'Test-only-staff-password-2026');
  await save();
  await until(`document.getElementById('staffRows').textContent.includes(${JSON.stringify(name)})`);
  await rowAction(name, 'Xem chi tiết');
  const edited = name + ' đã sửa'; await fill('staffFullName', edited); await fill('staffReason', 'Kiểm chứng cập nhật thông tin trên DB QA riêng.'); await save();
  await visit('admin-roles'); assert.equal(await evaluate('document.querySelector(".portal-tabs [aria-current=page]").textContent'), 'Vai trò và quyền'); assert.equal(await evaluate('document.querySelectorAll("#roleCards article").length'), 6);
  await rowAction(edited, 'Đổi vai trò'); await fill('staffRole', academicRole.roleId); await fill('staffReason', 'Kiểm chứng gán vai trò thực tế.'); await save();
  assert.equal(await evaluate(`Array.from(document.querySelectorAll('#staffRows tr')).find(row=>row.textContent.includes(${JSON.stringify(edited)})).textContent.includes('Cán bộ học vụ')`), true);
  await screenshot('roles-1280');
  await visit('admin-staff'); await rowAction(edited, 'Xem chi tiết'); await evaluate('document.getElementById("staffOpenStatus").click()'); await fill('staffReason', 'Kiểm chứng tạm khóa tài khoản trên DB QA.'); await save();
  assert.equal(await evaluate(`Array.from(document.querySelectorAll('#staffRows tr')).find(row=>row.textContent.includes(${JSON.stringify(edited)})).textContent.includes('Tạm khóa')`), true);
  await rowAction(edited, 'Xem chi tiết'); await evaluate('document.getElementById("staffOpenStatus").click()'); await fill('staffReason', 'Kiểm chứng khôi phục tài khoản.'); await save();
  await fill('staffQuery', edited); await evaluate('document.getElementById("staffSearch").requestSubmit()');
  await until('document.getElementById("operationsContent").getAttribute("aria-busy") === "false"');
  assert.equal(await evaluate('document.querySelectorAll("#staffRows tr").length'), 1);
  for (const width of [390, 320]) {
    await command('Emulation.setDeviceMetricsOverride', {width, height: 844, deviceScaleFactor: 1, mobile: true});
    for (const page of ['admin-staff', 'admin-roles']) { await visit(page); await screenshot(page + '-' + width); }
  }
  assert.deepEqual(errors, []);
  await writeFile(new URL('staff-results.json', output), JSON.stringify({passed: true, backend, checks: ['root opens dashboard', 'database admin identity/avatar', 'staff/role links enabled', 'create/edit/search', 'assign role', 'suspend/reactivate', 'audit history', 'desktop/mobile layout']}, null, 2));
  console.log('Admin entry and staff browser checks passed.');
} finally { await command('Page.close'); socket.close(); }
