// Hai backend QA dùng cùng DB riêng: 8086 đăng nhập Admin, 8087 đăng nhập Manager; không giả phản hồi nghiệp vụ.
import assert from 'node:assert/strict';
import {mkdir,readFile,writeFile} from 'node:fs/promises';
const ADMIN='http://localhost:8086',MANAGER='http://localhost:8087';
const output=new URL(process.env.QA_OUTPUT||'../../../.dashboard-qa/',import.meta.url);await mkdir(output,{recursive:true});
const source=await readFile(new URL('../js/config/api.config.js',import.meta.url),'utf8');
let backend=MANAGER;
const target=await(await fetch('http://localhost:9337/json/new?about:blank',{method:'PUT'})).json();
const socket=new WebSocket(target.webSocketDebuggerUrl);await new Promise(resolve=>socket.addEventListener('open',resolve,{once:true}));
let sequence=0;const pending=new Map(),errors=[];
function command(method,params={}){const id=++sequence;return new Promise((resolve,reject)=>{pending.set(id,{resolve,reject});socket.send(JSON.stringify({id,method,params}));});}
socket.addEventListener('message',async event=>{
  const message=JSON.parse(event.data);if(message.id){const entry=pending.get(message.id);pending.delete(message.id);if(message.error)entry?.reject(new Error(message.error.message));else entry?.resolve(message.result);}
  if(message.method==='Runtime.exceptionThrown')errors.push(message.params.exceptionDetails.exception?.description||message.params.exceptionDetails.text);
  if(message.method==='Fetch.requestPaused')await command('Fetch.fulfillRequest',{requestId:message.params.requestId,responseCode:200,responseHeaders:[{name:'Content-Type',value:'application/javascript'}],body:Buffer.from(source.replace('http://localhost:8085/api',backend+'/api')).toString('base64')});
});
async function evaluate(expression){const result=await command('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(result.exceptionDetails)throw new Error(result.exceptionDetails.exception?.description||result.exceptionDetails.text);return result.result.value;}
async function until(expression){const start=Date.now();while(Date.now()-start<10000){if(await evaluate(expression))return;await new Promise(resolve=>setTimeout(resolve,80));}throw new Error('Không đạt điều kiện: '+expression+'\n'+await evaluate('document.body.innerText'));}
const idle=()=>until('document.getElementById("operationsContent").getAttribute("aria-busy")==="false"');
async function visit(page){await command('Page.navigate',{url:'http://localhost:3000/pages/'+page+'/'});await until(`location.pathname==='/pages/${page}/'&&document.body.dataset.ready==='true'`);await idle();assert.equal(await evaluate('document.body.dataset.error'),'false');assert.equal(await evaluate('document.documentElement.scrollWidth<=innerWidth'),true);}
async function screenshot(name){await evaluate('document.fonts.ready');const result=await command('Page.captureScreenshot',{format:'png',captureBeyondViewport:true});await writeFile(new URL(name+'.png',output),Buffer.from(result.data,'base64'));}
const fill=(id,value)=>evaluate(`(()=>{const n=document.getElementById(${JSON.stringify(id)});n.value=${JSON.stringify(value)};n.dispatchEvent(new Event('input'));})()`);
const click=(scope,name,label)=>evaluate(`Array.from(document.querySelectorAll(${JSON.stringify(scope)})).find(row=>row.textContent.includes(${JSON.stringify(name)})).querySelectorAll('button').forEach(button=>{if(button.textContent.startsWith(${JSON.stringify(label)}))button.click()})`);
const rowText=(scope,name)=>evaluate(`Array.from(document.querySelectorAll(${JSON.stringify(scope)})).find(row=>row.textContent.includes(${JSON.stringify(name)}))?.textContent||''`);
async function submitRole(reason){await fill('roleReason',reason);await evaluate('document.getElementById("roleForm").requestSubmit()');await until('!document.getElementById("roleDialog").open');await new Promise(resolve=>setTimeout(resolve,150));await idle();assert.equal(await evaluate('document.body.dataset.error'),'false');}
async function decide(name){await click('#promotionList article',name,'Xem và quyết định');await until('document.getElementById("promotionDialog").open');}
const json=async(base,path)=>(await(await fetch(base+'/api/admin/operations'+path)).json()).data;
try{
  await command('Page.enable');await command('Runtime.enable');await command('Fetch.enable',{patterns:[{urlPattern:'http://localhost:3000/js/config/api.config.js',requestStage:'Request'}]});
  await command('Emulation.setDeviceMetricsOverride',{width:1280,height:900,deviceScaleFactor:1,mobile:false});
  const people=(await json(MANAGER,'/role-changes/staff?size=50')).items.filter(person=>!person.pendingPromotion&&person.email.endsWith('@test.invalid'));
  assert(people.length>=3,'DB QA cần ít nhất ba nhân viên đang hoạt động');
  const [moved,promoted,rejected]=people;

  backend=MANAGER;await visit('manager-staff-roles');
  assert.equal(await evaluate(`document.querySelector('.sidebar-menu a[href="/pages/manager-staff-roles/"]').getAttribute('aria-current')`),'page');
  await until(`!document.querySelector('.sidebar-menu a[href="/pages/manager-staff-roles/"]').hidden`);
  await click('#roleRows tr',moved.email,'Đổi sang');await until('document.getElementById("roleDialog").open');
  await submitRole('Kiểm chứng điều chuyển trên DB QA.');
  assert.equal((await rowText('#roleRows tr',moved.email)).includes(moved.moveToLabel),true);
  for(const person of [promoted,rejected]){
    await click('#roleRows tr',person.email,'Đề xuất thăng chức');await until('document.getElementById("roleDialog").open');
    await submitRole('Kiểm chứng đề xuất thăng chức trên DB QA.');
    assert.equal((await rowText('#roleRows tr',person.email)).includes('Đang chờ duyệt thăng chức'),true);
    assert.equal((await rowText('#myPromotions article',person.fullName)).includes('Chờ duyệt'),true);
  }
  assert.equal(await json(MANAGER,'/staff')===undefined,true,'Manager không đọc được danh sách quản trị');
  await screenshot('manager-staff-roles-1280');

  backend=ADMIN;await visit('admin-roles');
  assert.equal(await evaluate(`Array.from(document.querySelectorAll('#staffRows button')).some(button=>button.textContent==='Đổi vai trò')`),false);
  assert.equal(await evaluate(`document.getElementById('promotionSection').hidden`),false);
  assert.equal((await rowText('#promotionList article',promoted.fullName)).includes('Quản lý vận hành'),true);
  await screenshot('admin-roles-promotions-1280');
  await decide(promoted.fullName);await screenshot('admin-promotion-dialog-1280');
  await fill('promotionNote','Theo quyết định kiểm thử.');await evaluate('document.getElementById("promotionForm").requestSubmit()');
  await until('!document.getElementById("promotionDialog").open');await new Promise(resolve=>setTimeout(resolve,150));await idle();
  assert.equal(await rowText('#promotionList article',promoted.fullName),'');
  const staff=(await json(ADMIN,'/staff?q='+encodeURIComponent(promoted.email))).items[0];assert.equal(staff.roleLabel,'Quản lý vận hành');
  await decide(rejected.fullName);
  await evaluate('document.getElementById("rejectPromotion").click()');
  assert.equal(await evaluate('document.getElementById("promotionNotice").textContent'),'Vui lòng nhập lý do từ chối.');
  await fill('promotionNote','Chưa đủ thời gian công tác.');await evaluate('document.getElementById("rejectPromotion").click()');
  await until('!document.getElementById("promotionDialog").open');await new Promise(resolve=>setTimeout(resolve,150));await idle();
  const log=await json(ADMIN,'/audit-log?source=STAFF&size=5');assert.equal(log.items.some(item=>item.targetLabel===promoted.fullName&&item.reason.includes('Duyệt đề xuất')),true);

  backend=MANAGER;await visit('manager-staff-roles');
  assert.equal((await rowText('#myPromotions article',rejected.fullName)).includes('Đã từ chối'),true);
  assert.equal((await rowText('#myPromotions article',rejected.fullName)).includes('Chưa đủ thời gian công tác.'),true);
  assert.equal((await rowText('#myPromotions article',promoted.fullName)).includes('Đã duyệt'),true);
  for(const width of [390,320]){await command('Emulation.setDeviceMetricsOverride',{width,height:844,deviceScaleFactor:1,mobile:true});await visit('manager-staff-roles');await new Promise(resolve=>setTimeout(resolve,500));assert.equal(await evaluate(`Array.from(document.querySelectorAll('#roleRows button')).every(button=>button.getBoundingClientRect().right<=innerWidth)`),true,'Nút thao tác bị cắt ở chiều rộng '+width);await screenshot('manager-staff-roles-'+width);}
  backend=ADMIN;await visit('admin-roles');await new Promise(resolve=>setTimeout(resolve,500));await screenshot('admin-roles-390');
  assert.deepEqual(errors,[]);
  console.log('Staff role change browser checks passed.');
}finally{await command('Page.close');socket.close();}
