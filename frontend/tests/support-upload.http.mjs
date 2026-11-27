import assert from 'node:assert/strict';
import {createHmac} from 'node:crypto';
import {readFile} from 'node:fs/promises';
const secret=process.env.TICKET_BROWSER_SECRET;if(!secret||secret.length<32)throw new Error('Use the isolated test signing key.');
const base='http://localhost:8090/api/v1/tickets';
const encode=x=>Buffer.from(JSON.stringify(x)).toString('base64url');const value=encode({alg:'HS256',typ:'JWT'})+'.'+encode({sub:'fe10-owner@test.invalid',exp:Math.floor(Date.now()/1000)+600});
const headers={Authorization:'Bearer '+value+'.'+createHmac('sha256',secret).update(value).digest('base64url')};
const csrfResponse=await fetch(base+'/csrf',{headers});const csrf=(await csrfResponse.json()).data;headers[csrf.headerName]=csrf.token;headers.Cookie=csrfResponse.headers.getSetCookie().map(c=>c.split(';')[0]).join('; ');
const count=async()=>{const r=await fetch(base,{headers});assert.equal(r.status,200);return (await r.json()).data.totalElements;};const before=await count();
// Fixture bytes are exported only from the isolated database's test attachment.
const image=Buffer.from((await readFile(new URL('../../../.fe10-qa/test-image.txt',import.meta.url),'utf16le')).trim(),'base64');
const payload={subject:'Kiểm tra tải tệp qua máy chủ thật',category:'TECHNICAL',message:'Tệp kiểm thử API riêng.'};
function form(){const data=new FormData();data.append('payload',new Blob([JSON.stringify(payload)],{type:'application/json'}));return data;}
const data=form();data.append('files',new Blob([image],{type:'image/png'}),'ảnh kiểm thử.png');
const created=await fetch(base,{method:'POST',headers,body:data});const result=await created.json();assert.equal(created.status,201,result.message);assert.ok(created.headers.get('location'));const id=result.data.ticketId;
const thread=await(await fetch(`${base}/${id}/messages`,{headers})).json();const file=thread.data.items[0].attachments[0];assert.equal(file.content,undefined);
const downloaded=await fetch('http://localhost:8090'+file.downloadPath,{headers});assert.equal(downloaded.status,200);assert.equal(downloaded.headers.get('cache-control'),'no-store');assert.equal(downloaded.headers.get('x-content-type-options'),'nosniff');assert.deepEqual(Buffer.from(await downloaded.arrayBuffer()),image);
const oversized=form();for(let i=0;i<6;i++)oversized.append('files',new Blob([new Uint8Array(9000000)],{type:'application/pdf'}),`large-${i}.pdf`);
const rejected=await fetch(base,{method:'POST',headers,body:oversized});assert.equal(rejected.status,413);assert.equal((await rejected.json()).success,false);assert.equal(await count(),before+1);
console.log('Real servlet upload/download, 52 MB request cap and rollback checks passed.');
