// Hai backend QA dùng cùng DB riêng, một tài khoản Admin và một Manager; không giả dữ liệu báo cáo.
import assert from 'node:assert/strict';
import {mkdir,readFile,writeFile} from 'node:fs/promises';
const output=new URL('../../../.dashboard-qa/',import.meta.url);await mkdir(output,{recursive:true});
const source=await readFile(new URL('../js/config/api.config.js',import.meta.url),'utf8');
let backend='http://localhost:8086';
const target=await(await fetch('http://localhost:9337/json/new?about:blank',{method:'PUT'})).json();
const socket=new WebSocket(target.webSocketDebuggerUrl);await new Promise(resolve=>socket.addEventListener('open',resolve,{once:true}));
let sequence=0;const pending=new Map(),errors=[];
function command(method,params={}){const id=++sequence;return new Promise((resolve,reject)=>{pending.set(id,{resolve,reject});socket.send(JSON.stringify({id,method,params}));});}
socket.addEventListener('message',async event=>{
  const message=JSON.parse(event.data);if(message.id){const entry=pending.get(message.id);pending.delete(message.id);if(message.error)entry?.reject(new Error(message.error.message));else entry?.resolve(message.result);}
  if(message.method==='Runtime.exceptionThrown')errors.push(message.params.exceptionDetails.text);
  if(message.method==='Fetch.requestPaused')await command('Fetch.fulfillRequest',{requestId:message.params.requestId,responseCode:200,responseHeaders:[{name:'Content-Type',value:'application/javascript'}],body:Buffer.from(source.replace('http://localhost:8085/api',backend+'/api')).toString('base64')});
});
async function evaluate(expression){const result=await command('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(result.exceptionDetails)throw new Error(result.exceptionDetails.exception?.description||result.exceptionDetails.text);return result.result.value;}
async function until(expression){const start=Date.now();while(Date.now()-start<10000){if(await evaluate(expression))return;await new Promise(resolve=>setTimeout(resolve,80));}throw new Error('Không đạt điều kiện: '+expression+'\n'+await evaluate('document.body.innerText'));}
async function visit(page){await command('Page.navigate',{url:'http://localhost:3000/pages/'+page+'/'});await until(`location.pathname === '/pages/${page}/' && document.body.dataset.ready === 'true'`);assert.equal(await evaluate('document.body.dataset.error'),'false');assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'),true);}
async function screenshot(name){await evaluate('document.fonts.ready');const result=await command('Page.captureScreenshot',{format:'png',captureBeyondViewport:true});await writeFile(new URL(name+'.png',output),Buffer.from(result.data,'base64'));}
try{
  await command('Page.enable');await command('Runtime.enable');await command('Fetch.enable',{patterns:[{urlPattern:'http://localhost:3000/js/config/api.config.js',requestStage:'Request'}]});
  for(const [role,port] of [['admin',8086],['manager',8087]]){
    backend='http://localhost:'+port;
    const identity=await(await fetch(backend+'/api/admin/operations/profile')).json();assert.equal(identity.success,true);
    const report=await(await fetch(backend+'/api/admin/operations/management-report')).json();assert.equal(report.success,true);
    for(const width of [1280,390]){
      await command('Emulation.setDeviceMetricsOverride',{width,height:900,deviceScaleFactor:1,mobile:width<760});
      await visit('manager-reports');
      assert.equal(await evaluate('document.getElementById("totalAccounts").textContent'),new Intl.NumberFormat('vi-VN').format(report.data.totalAccounts));
      assert.equal(await evaluate('document.getElementById("connectionCount").textContent'),new Intl.NumberFormat('vi-VN').format(report.data.connectionCount));
      assert.equal(await evaluate('document.getElementById("bookingCount").textContent'),new Intl.NumberFormat('vi-VN').format(report.data.bookingCount));
      if(role==='manager'){
        await until('document.querySelector(".admin-identity small").textContent === "Quản lý vận hành"');
        assert.equal(await evaluate("Array.from(document.querySelectorAll('.sidebar-menu a')).filter(link=>!link.hidden).every(link=>['/pages/manager-reports/','/manager/support/tickets'].includes(new URL(link.href).pathname))"),true);
      }
      await screenshot(role+'-management-report-'+width);
      await visit('admin-profile');
      assert.equal(await evaluate('document.getElementById("profileName").textContent'),identity.data.fullName);
      assert.equal(await evaluate('document.getElementById("profileDetails").textContent.includes('+JSON.stringify(identity.data.email)+')'),true);
      assert.equal(await evaluate("document.querySelector('.sidebar-bottom a').getAttribute('aria-current')"),'page');
      await screenshot(role+'-profile-'+width);
    }
    if(role==='manager'){
      assert.equal((await fetch(backend+'/api/admin/operations/staff')).status,403);
      assert.equal((await fetch(backend+'/api/admin/operations/booking-policy')).status,403);
      await command('Page.navigate',{url:'http://localhost:3000/admin/dashboard'});
      await until('location.pathname === "/pages/manager-reports/" && document.body.dataset.ready === "true"');
    }
  }
  assert.deepEqual(errors,[]);await writeFile(new URL('management-profile-results.json',output),JSON.stringify({passed:true,checks:['admin/manager real reports','profile belongs to current account','desktop/mobile','manager menu restricted','manager cannot access staff/settings','manager root redirect'],aiDeferred:true},null,2));
  console.log('Manager reports and personal profile browser checks passed.');
}finally{await command('Page.close');socket.close();}
