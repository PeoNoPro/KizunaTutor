// Real Chromium rendering with isolated, deliberately delayed API fixtures. No application DB or real sessions.
import assert from 'node:assert/strict';
import http from 'node:http';
import {readFile,mkdtemp,writeFile,access} from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawn} from 'node:child_process';
const root=fileURLToPath(new URL('../',import.meta.url));
// Hồ sơ trình duyệt và kết quả ghi vào thư mục tạm của hệ điều hành, không ghi ra ngoài repo.
// Đặt QA_OUTPUT_DIR nếu muốn giữ kết quả ở chỗ khác.
const qa=process.env.QA_OUTPUT_DIR||os.tmpdir();
const profile=await mkdtemp(path.join(qa,'support-navigation-'));
/**
 * Tìm Chrome/Chromium/Edge: ưu tiên biến môi trường CHROME_PATH, sau đó các vị trí cài đặt phổ biến
 * trên Windows, macOS và Linux. Không tìm thấy thì báo rõ cách đặt CHROME_PATH.
 */
async function findBrowser(){
 if(process.env.CHROME_PATH)return process.env.CHROME_PATH;
 const programFiles=[process.env.PROGRAMFILES,process.env['PROGRAMFILES(X86)'],process.env.LOCALAPPDATA].filter(Boolean);
 const candidates={
  win32:programFiles.flatMap(dir=>[path.join(dir,'Google/Chrome/Application/chrome.exe'),path.join(dir,'Microsoft/Edge/Application/msedge.exe')]),
  darwin:['/Applications/Google Chrome.app/Contents/MacOS/Google Chrome','/Applications/Chromium.app/Contents/MacOS/Chromium','/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge'],
  linux:['/usr/bin/google-chrome','/usr/bin/google-chrome-stable','/usr/bin/chromium','/usr/bin/chromium-browser','/usr/bin/microsoft-edge']
 }[process.platform]||[];
 for(const file of candidates){try{await access(file);return file;}catch{}}
 throw new Error('Không tìm thấy Chrome/Chromium/Edge. Đặt biến môi trường CHROME_PATH trỏ tới tệp chạy trình duyệt rồi chạy lại.');
}
const browserPath=await findBrowser();
const delay=ms=>new Promise(resolve=>setTimeout(resolve,ms));
let role='learner',requests=[];
const server=http.createServer(async(req,res)=>{
 try{
  const url=new URL(req.url,'http://localhost'),pathname=url.pathname;
  if(pathname==='/delayed-tailwind.js'){await delay(1600);res.setHeader('Content-Type','application/javascript');res.end('window.tailwind={};');return;}
  if(pathname.startsWith('/api/')){
   requests.push(req.url);let data;
   if(pathname.endsWith('/me')){
    await delay(650);
    data=pathname.includes('/v1/tickets/')?{userId:'perf-'+role,viewRole:role,displayName:'Kiểm tra '+role,canCreate:['learner','tutor'].includes(role),canHandle:role==='cs',canDecide:role==='manager'}:{displayName:'Kiểm tra '+role,roleLabel:role,canAdmin:false,canReport:role==='manager',canSupport:true,avatarUrl:''};
   }else if(pathname.endsWith('/counts')){await delay(120);data={OPEN:2,IN_PROGRESS:3,ESCALATED:1,RESOLVED:2,CLOSED:1};}
   else if(pathname.endsWith('/assignees'))data=[];
   else if(pathname.endsWith('/attachment-limit'))data={maxBytes:10000000};
   else if(pathname==='/api/v1/tickets'){
    await delay(180);data={items:[{ticketId:'00000000-0000-0000-0000-000000000001',subject:'Yêu cầu kiểm tra tốc độ',status:(url.searchParams.get('status')||'OPEN').split(',')[0],statusLabel:'Đang xử lý',category:'OTHER',categoryLabel:'Nội dung khác',priority:'MEDIUM',priorityLabel:'Bình thường',requesterDisplayName:'Người kiểm thử',assigneeDisplayName:'CS kiểm thử',createdAt:'2026-11-07T01:00:00Z',updatedAt:'2026-11-07T01:00:00Z'}],page:0,totalPages:1,totalElements:1};
   }else throw new Error('Unexpected fixture endpoint '+req.url);
   res.setHeader('Content-Type','application/json');res.end(JSON.stringify({data}));return;
  }
  let file=pathname==='/baseline'?'pages/support-tickets/index.html':pathname.replace(/^\//,'');
  if(file.endsWith('/'))file+='index.html';
  const absolute=path.resolve(root,file);if(!absolute.startsWith(root))throw new Error('Invalid path');
  let body=await readFile(absolute);
  const ext=path.extname(file);res.setHeader('Content-Type',({'.html':'text/html; charset=utf-8','.js':'application/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.svg':'image/svg+xml','.png':'image/png'})[ext]||'application/octet-stream');
  if(pathname==='/baseline')body=Buffer.from(body.toString().replace('<link rel="stylesheet" href="/styles/support-utilities.css">','<script src="/delayed-tailwind.js"></script>'));
  if(pathname==='/js/config/api.config.js')body=Buffer.from(body.toString().replace('http://localhost:8085/api',`http://127.0.0.1:${server.address().port}/api`));
  res.end(body);
 }catch(error){res.statusCode=404;res.end(error.message);}
});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
const origin=`http://127.0.0.1:${server.address().port}`;
const browser=spawn(browserPath,['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port=0',`--user-data-dir=${profile}`,'about:blank'],{windowsHide:true,stdio:'ignore'});
browser.on('error',error=>{console.error(error);process.exitCode=1;});
let socket;const results=[];
try{
 let port;
 for(let i=0;i<100;i++){try{port=(await readFile(path.join(profile,'DevToolsActivePort'),'utf8')).split('\n')[0];break;}catch{await delay(100);}}
 assert.ok(port,'Chromium did not start');
 const target=await(await fetch(`http://127.0.0.1:${port}/json/new?about:blank`,{method:'PUT'})).json();
 socket=new WebSocket(target.webSocketDebuggerUrl);await new Promise(resolve=>socket.addEventListener('open',resolve,{once:true}));
 let seq=0;const pending=new Map(),errors=[];
 const command=(method,params={})=>new Promise((resolve,reject)=>{const id=++seq;pending.set(id,{resolve,reject});socket.send(JSON.stringify({id,method,params}));});
 socket.addEventListener('message',event=>{const m=JSON.parse(event.data);if(m.id){const p=pending.get(m.id);pending.delete(m.id);m.error?p?.reject(new Error(m.error.message)):p?.resolve(m.result);}if(m.method==='Runtime.exceptionThrown')errors.push(m.params.exceptionDetails.exception?.description||m.params.exceptionDetails.text);});
 const evaluate=async expression=>{const r=await command('Runtime.evaluate',{expression,returnByValue:true});if(r.exceptionDetails)throw new Error(r.exceptionDetails.text);return r.result.value;};
 const until=async expression=>{const start=Date.now();while(Date.now()-start<8000){if(await evaluate(expression))return;await delay(20);}throw new Error('UI timeout: '+expression+'\n'+await evaluate('document.body?.innerText'));};
 await command('Page.enable');await command('Runtime.enable');await command('Network.enable');
 await command('Network.setBlockedURLs',{urls:['*fonts.googleapis.com*','*fonts.gstatic.com*','*cdn.tailwindcss.com*']});
 await command('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false});
 for(const current of ['learner','tutor','cs','manager']){
  role=current;requests=[];errors.length=0;
  await command('Page.navigate',{url:'about:blank'});await delay(60);
  await command('Page.addScriptToEvaluateOnNewDocument',{source:'try{sessionStorage.clear();localStorage.clear();}catch{}'});
  await command('Page.navigate',{url:origin+`/pages/support-tickets/?role=${role}&group=${role==='cs'?0:1}`});
  await until('document.getElementById("supportLoading")?.getBoundingClientRect().height>0');
  const early=await evaluate('({time:performance.now(),loaded:document.body.dataset.role||null,visible:document.getElementById("supportLoading").getBoundingClientRect().height>0})');
  assert.equal(early.visible,true);assert.equal(early.loaded,null,'Placeholder must paint before identity response');
  await until('document.querySelector(".support-row-card") && document.getElementById("supportNotice").dataset.error!=="true"');
  await delay(200);
  assert.equal(errors.length,0,errors.join('\n'));
  const listCalls=requests.filter(url=>url.startsWith('/api/v1/tickets?'));
  assert.equal(listCalls.length,1,'Only one request may load actual ticket rows');
  assert.equal(requests.filter(url=>url.startsWith('/api/v1/tickets/counts')).length,1,'Cards/chips must use one aggregate');
  assert.equal(requests.some(url=>url.includes('/attachment-limit')),false,'Attachment configuration is lazy on list pages');
  const metrics=await evaluate('({readyMs:performance.now(),paint:performance.getEntriesByType("paint").find(x=>x.name==="first-contentful-paint")?.startTime,cdn:performance.getEntriesByType("resource").filter(x=>x.name.includes("cdn.tailwindcss.com")).length})');
  assert.equal(metrics.cdn,0);assert.ok(metrics.paint<650,'Content should paint before delayed identity');
  results.push({role,...metrics,apiRequests:requests.length,listCalls:listCalls.length});
  if(role==='tutor')assert.equal(await evaluate('getComputedStyle(document.querySelector("#tutorSidebar aside")).position'),'fixed');
  await command('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true});
  assert.equal(await evaluate('document.documentElement.scrollWidth<=innerWidth'),true,role+' mobile overflow');
  await command('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false});
 }
 role='learner';await command('Page.navigate',{url:origin+'/baseline?role=learner'});
 await until('!!document.getElementById("supportLoading")');
 const blocked=await evaluate('performance.now()');assert.ok(blocked>=1500,'Baseline must reproduce parser-blocking compiler delay');
 results.push({scenario:'parser-blocking Tailwind simulation',bodyAvailableMs:blocked});
 const resultFile=path.join(profile,'support-navigation-results.json');
 await writeFile(resultFile,JSON.stringify(results,null,2));
 console.log('Kết quả lưu tại: '+resultFile);
 console.log(JSON.stringify(results,null,2));
 console.log('PASS: four roles, local CSS, early paint, one row request, one count aggregate, lazy attachments and mobile layout.');
}finally{socket?.close();browser.kill();server.closeAllConnections();await new Promise(resolve=>server.close(resolve));}
