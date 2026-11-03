/* Node 24, Chrome CDP. Real browser rendering, not HTTP timing as a substitute. */
const {readFile,writeFile}=require('node:fs/promises');
const {resolve}=require('node:path');
const {performance}=require('node:perf_hooks');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
(async()=>{
 const api=process.env.TICKET_LOAD_API,front=process.env.TICKET_LOAD_FRONTEND||'http://localhost:3000';
 if(api!=='http://localhost:8090'||!/^jdbc:postgresql:\/\/127\.0\.0\.1:\d+\/[a-z0-9_]+_ticket_test$/.test(process.env.TICKET_TEST_DB_URL||''))throw new Error('Only the explicitly isolated localhost test service/database is allowed.');
 const users=JSON.parse(await readFile(process.env.TICKET_LOAD_TOKENS_FILE,'utf8'));
 if(users.length!==100||users.some(u=>!u.token||u.role!=='learner'))throw new Error('Provide 100 distinct learner test sessions.');
 if(new Set(users.map(u=>u.token)).size!==100)throw new Error('The sessions must belong to different users.');
 const subjects=users.map(u=>JSON.parse(Buffer.from(u.token.split('.')[1],'base64url')).sub);
 if(new Set(subjects).size!==100||subjects.some(s=>!/^fe10-load-\d+@test\.invalid$/.test(s)))throw new Error('Only 100 distinct load-fixture accounts are allowed.');
 const info=await(await fetch(process.env.TICKET_LOAD_CDP||'http://localhost:9340/json/version')).json();
 const ws=new WebSocket(info.webSocketDebuggerUrl);await new Promise(r=>ws.addEventListener('open',r,{once:true}));
 let seq=0;const pending=new Map(),sessions=[],samples={list:[],detail:[],http:[]},failures=[],httpRequests=new Map();let mutations=0,measuring=false;
 const source=await readFile(resolve(__dirname,'../../../../frontend/js/config/api.config.js'),'utf8');
 function command(method,params={},sessionId){const id=++seq;return new Promise((resolve,reject)=>{const timer=setTimeout(()=>{pending.delete(id);reject(new Error('CDP timeout: '+method));},30000);pending.set(id,{resolve,reject,timer});ws.send(JSON.stringify({id,method,params,...(sessionId?{sessionId}:{})}));});}
 ws.addEventListener('message',async event=>{const m=JSON.parse(event.data);if(m.id){const p=pending.get(m.id);if(!p)return;pending.delete(m.id);clearTimeout(p.timer);m.error?p.reject(new Error(m.error.message)):p.resolve(m.result);}
   if(m.method==='Runtime.exceptionThrown')failures.push('Browser exception');
   if(measuring&&m.method==='Network.requestWillBeSent'&&m.params.request.url.startsWith(api+'/api/v1/tickets'))httpRequests.set(m.sessionId+':'+m.params.requestId,m.params.timestamp);
   if(m.method==='Network.loadingFinished'){const key=m.sessionId+':'+m.params.requestId;if(httpRequests.has(key)){samples.http.push((m.params.timestamp-httpRequests.get(key))*1000);httpRequests.delete(key);}}
   if(m.method==='Fetch.requestPaused')await command('Fetch.fulfillRequest',{requestId:m.params.requestId,responseCode:200,responseHeaders:[{name:'Content-Type',value:'application/javascript'}],body:Buffer.from(source.replace('http://localhost:8085/api',api+'/api')).toString('base64')},m.sessionId);
 });
 async function evaluate(session,expression){const r=await command('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true},session.id);if(r.exceptionDetails)throw new Error(r.exceptionDetails.text);return r.result.value;}
 async function rendered(session){const deadline=performance.now()+15000;while(performance.now()<deadline){try{if(await evaluate(session,`document.getElementById('supportNotice')?.dataset.error === 'true'`))throw new Error('UI reported an API error');if(await evaluate(session,`document.getElementById('supportNotice')?.textContent.startsWith('Đã tải')`)){await evaluate(session,'new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)))');return;}}catch(error){if(error.message.includes('UI reported'))throw error;}await sleep(80);}throw new Error('Page did not finish rendering');}
 async function navigate(session,kind,record){const started=performance.now();await command('Page.navigate',{url:front+'/learner/support/tickets'+(kind==='detail'?'/'+session.ticketId:'')},session.id);await rendered(session);if(record)samples[kind].push(performance.now()-started);}
 try{
   for(const user of users){const context=await command('Target.createBrowserContext');const target=await command('Target.createTarget',{url:'about:blank',browserContextId:context.browserContextId});const attached=await command('Target.attachToTarget',{targetId:target.targetId,flatten:true});const session={id:attached.sessionId,context:context.browserContextId,user};sessions.push(session);
     await command('Page.enable',{},session.id);await command('Runtime.enable',{},session.id);await command('Network.enable',{},session.id);await command('Network.setCookie',{name:'access_token',value:user.token,url:api,httpOnly:true,sameSite:'Lax'},session.id);await command('Fetch.enable',{patterns:[{urlPattern:front+'/js/config/api.config.js',requestStage:'Request'}]},session.id);
     const start=performance.now();const response=await fetch(api+'/api/v1/tickets?size=20',{headers:{Authorization:'Bearer '+user.token}});const result=await response.json();samples.http.push(performance.now()-start);if(!response.ok||result.data.items.length<10)throw new Error('Each test user needs at least ten populated tickets');session.ticketId=result.data.items[0].ticketId;
   }
   // Warm every browser before the measured ten-minute concurrent interval.
   await Promise.all(sessions.map(s=>navigate(s,'list',false)));
   samples.http.length=0;measuring=true;const started=Date.now(),deadline=started+600000;
   await Promise.all(sessions.map(async session=>{let round=0;while(Date.now()<deadline){try{await navigate(session,'list',true);await navigate(session,'detail',true);if(++round%5===0){const exists=await evaluate(session,`!!document.querySelector('#replyArea textarea')`);if(!exists)throw new Error('Missing owner reply form');await evaluate(session,`document.querySelector('#replyArea textarea').value='Trao đổi trong kiểm thử tải';document.querySelector('#replyArea form').requestSubmit()`);const end=Date.now()+15000;let saved=false;while(Date.now()<end){if(await evaluate(session,`document.getElementById('supportNotice')?.textContent==='Đã lưu thay đổi.'`)){saved=true;break;}if(await evaluate(session,`document.getElementById('supportNotice')?.dataset.error==='true'`))break;await sleep(100);}if(!saved)throw new Error('Mutation did not succeed');mutations++;}await sleep(1000+Math.random()*1000);}catch(error){failures.push(error.message);await sleep(1000);}}}));
   const p95=values=>{const sorted=[...values].sort((a,b)=>a-b);return sorted[Math.ceil(sorted.length*.95)-1]??null;};
   const report={users:100,durationMs:Date.now()-started,datasetMinimumTickets:1000,renderP95Ms:{list:p95(samples.list),detail:p95(samples.detail)},httpP95Ms:p95(samples.http),samples:{list:samples.list.length,detail:samples.detail.length},successfulMutations:mutations,errors:failures,passed:failures.length===0&&p95(samples.list)<=2000&&p95(samples.detail)<=2000};
   await writeFile(process.env.TICKET_LOAD_OUTPUT||'target/support-load-results.json',JSON.stringify(report,null,2));console.log(JSON.stringify({...report,errors:failures.slice(0,10)}));if(!report.passed)process.exitCode=1;
 }finally{for(const session of sessions)await command('Target.disposeBrowserContext',{browserContextId:session.context});ws.close();}
})().catch(error=>{console.error(error.message);process.exitCode=1;});
