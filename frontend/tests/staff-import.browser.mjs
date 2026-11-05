// Kiểm tra nhập nhân viên qua trình duyệt với backend và DB QA riêng; tệp kiểm thử được tạo ngay trong test.
import assert from 'node:assert/strict';
import {crc32} from 'node:zlib';
import {mkdtemp,mkdir,readFile,writeFile} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
const backend=process.env.ADMIN_DEV_BACKEND||'http://localhost:8086';
const output=new URL(process.env.QA_OUTPUT||'../../../.dashboard-qa/',import.meta.url);await mkdir(output,{recursive:true});
const config=(await readFile(new URL('../js/config/api.config.js',import.meta.url),'utf8')).replace('http://localhost:8085/api',backend+'/api');
const folder=await mkdtemp(join(tmpdir(),'staff-import-'));

// Zip không nén, đủ để tạo .xlsx tối giản mà không thêm thư viện cho test.
function zip(files){
  const parts=[],central=[];let offset=0;
  for(const [name,content] of Object.entries(files)){
    const nameBytes=Buffer.from(name),data=Buffer.from(content),crc=crc32(data);
    const common=Buffer.alloc(26);common.writeUInt16LE(20,0);common.writeUInt16LE(0x0800,2);common.writeUInt32LE(crc,10);common.writeUInt32LE(data.length,14);common.writeUInt32LE(data.length,18);common.writeUInt16LE(nameBytes.length,22);
    const local=Buffer.concat([Buffer.from([0x50,0x4b,3,4]),common,nameBytes,data]);
    const entry=Buffer.alloc(46);entry.writeUInt32LE(0x02014b50,0);entry.writeUInt16LE(20,4);common.copy(entry,6);entry.writeUInt32LE(offset,42);
    central.push(Buffer.concat([entry,nameBytes]));parts.push(local);offset+=local.length;
  }
  const directory=Buffer.concat(central),end=Buffer.alloc(22);end.writeUInt32LE(0x06054b50,0);end.writeUInt16LE(central.length,8);end.writeUInt16LE(central.length,10);end.writeUInt32LE(directory.length,12);end.writeUInt32LE(offset,16);
  return Buffer.concat([...parts,directory,end]);
}
const escapeXml=value=>value.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
function xlsx(rows){
  const cell=(value,c,r)=>`<c r="${'ABCD'[c]}${r}" t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>`;
  const sheet=[['Họ và tên','Email','Vai trò','Số điện thoại'],...rows].map((row,n)=>`<row r="${n+1}">${row.map((value,c)=>cell(value,c,n+1)).join('')}</row>`).join('');
  return zip({'[Content_Types].xml':'<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>',
    '_rels/.rels':'<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>',
    'xl/workbook.xml':'<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Nhân viên" sheetId="1" r:id="rId1"/></sheets></workbook>',
    'xl/_rels/workbook.xml.rels':'<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>',
    'xl/worksheets/sheet1.xml':`<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>${sheet}</sheetData></worksheet>`});
}

const target=await(await fetch('http://localhost:9337/json/new?about:blank',{method:'PUT'})).json();
const socket=new WebSocket(target.webSocketDebuggerUrl);await new Promise(resolve=>socket.addEventListener('open',resolve,{once:true}));
let sequence=0;const pending=new Map(),errors=[];
function command(method,params={}){const id=++sequence;return new Promise((resolve,reject)=>{pending.set(id,{resolve,reject});socket.send(JSON.stringify({id,method,params}));});}
socket.addEventListener('message',async event=>{
  const message=JSON.parse(event.data);if(message.id){const entry=pending.get(message.id);pending.delete(message.id);if(message.error)entry?.reject(new Error(message.error.message));else entry?.resolve(message.result);}
  if(message.method==='Runtime.exceptionThrown')errors.push(message.params.exceptionDetails.exception?.description||message.params.exceptionDetails.text);
  if(message.method==='Fetch.requestPaused')await command('Fetch.fulfillRequest',{requestId:message.params.requestId,responseCode:200,responseHeaders:[{name:'Content-Type',value:'application/javascript'}],body:Buffer.from(config).toString('base64')});
});
async function evaluate(expression){const result=await command('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(result.exceptionDetails)throw new Error(result.exceptionDetails.exception?.description||result.exceptionDetails.text);return result.result.value;}
async function until(expression){const start=Date.now();while(Date.now()-start<15000){if(await evaluate(expression))return;await new Promise(resolve=>setTimeout(resolve,80));}throw new Error('Không đạt điều kiện: '+expression+'\n'+await evaluate('document.getElementById("importDialog").innerText+document.getElementById("operationsNotice").textContent'));}
async function screenshot(name){await evaluate('document.fonts.ready');const result=await command('Page.captureScreenshot',{format:'png'});await writeFile(new URL(name+'.png',output),Buffer.from(result.data,'base64'));}
async function choose(name,rows){
  const path=join(folder,name);await writeFile(path,xlsx(rows));
  const {root}=await command('DOM.getDocument');const {nodeId}=await command('DOM.querySelector',{nodeId:root.nodeId,selector:'#importFile'});
  await command('DOM.setFileInputFiles',{nodeId,files:[path]});
  await evaluate('document.getElementById("checkImport").click()');
  await until('!document.getElementById("checkImport").disabled&&document.getElementById("importNotice").textContent!=="Đang kiểm tra tệp…"&&document.getElementById("importNotice").textContent!==""');
}
try{
  await command('Page.enable');await command('Runtime.enable');await command('DOM.enable');await command('Fetch.enable',{patterns:[{urlPattern:'http://localhost:3000/js/config/api.config.js',requestStage:'Request'}]});
  await command('Emulation.setDeviceMetricsOverride',{width:1280,height:900,deviceScaleFactor:1,mobile:false});
  await command('Page.navigate',{url:'http://localhost:3000/pages/admin-staff/'});
  await until(`document.body.dataset.ready==='true'&&document.getElementById('operationsContent').getAttribute('aria-busy')==='false'`);
  await evaluate('document.getElementById("importStaff").click()');await until('document.getElementById("importDialog").open');
  await evaluate(`window.__blob=null;const o=URL.createObjectURL;URL.createObjectURL=b=>{window.__blob=b;return o.call(URL,b)};HTMLAnchorElement.prototype.click=function(){window.__name=this.download};document.getElementById('downloadTemplate').click()`);
  await until('document.getElementById("importNotice").textContent==="Đã tải tệp mẫu."');
  assert.equal(await evaluate('window.__name'),'mau-nhap-nhan-vien.xlsx');assert.equal(await evaluate('window.__blob.size>1000'),true);

  const stamp=Date.now(),first='import-ui-'+stamp+'-a@test.invalid',second='import-ui-'+stamp+'-b@test.invalid';
  await choose('co-loi.xlsx',[['Nhân viên lỗi','sai-email','Cán bộ học vụ','0901234567'],['Vai trò cao',first,'Quản lý vận hành',''],['Trùng tệp',first,'Cán bộ học vụ','']]);
  assert.equal(await evaluate('document.getElementById("importNotice").textContent.startsWith("Có 3 trong 3 dòng")'),true);
  assert.equal(await evaluate('document.querySelectorAll("#importPreview .staff-import-error").length'),3);
  assert.equal(await evaluate('document.getElementById("confirmImport").hidden'),true);
  await screenshot('import-errors-1280');

  await choose('hop-le.xlsx',[['Nhập Excel A '+stamp,first,'Cán bộ học vụ','0901234567'],['Nhập Excel B '+stamp,second,'Chăm sóc khách hàng','']]);
  assert.equal(await evaluate('document.getElementById("confirmImport").hidden'),false);
  assert.equal(await evaluate('document.getElementById("confirmImport").textContent'),'Xác nhận tạo 2 tài khoản');
  await screenshot('import-ready-1280');
  assert.equal(await evaluate('document.getElementById("importDialog").querySelectorAll("input[type=password]").length'),0);
  await evaluate('document.getElementById("importForm").requestSubmit()');
  await until('document.getElementById("importNotice").textContent.startsWith("Đã tạo 2 tài khoản")');
  await screenshot('import-done-1280');

  // Tệp cũ tải lại sau khi đã tạo phải báo email đã có, không tạo trùng.
  await choose('tai-lai.xlsx',[['Nhập lại',first,'Cán bộ học vụ','']]);
  assert.equal(await evaluate('document.querySelector("#importPreview .staff-import-error").textContent'),'Email này đã được sử dụng.');
  await evaluate('document.getElementById("closeImport").click()');
  await until(`document.getElementById('operationsNotice').textContent==='Đã nhập 2 tài khoản và ghi nhận lịch sử.'`);
  const found=(await(await fetch(backend+'/api/admin/operations/staff?q=import-ui-'+stamp)).json()).data;
  assert.equal(found.totalElements,2);
  assert.deepEqual(found.items.map(item=>item.roleLabel).sort(),['Chăm sóc khách hàng','Cán bộ học vụ'].sort());
  assert.equal(await evaluate(`document.getElementById('staffRows').textContent.includes(${JSON.stringify('Nhập Excel A '+stamp)})`),true);

  for(const width of [390,320]){
    await command('Emulation.setDeviceMetricsOverride',{width,height:844,deviceScaleFactor:1,mobile:true});
    await evaluate('document.getElementById("importStaff").click()');await until('document.getElementById("importDialog").open');
    await choose('co-loi-'+width+'.xlsx',[['Dài '+stamp,'sai-email','Cán bộ học vụ','']]);
    assert.equal(await evaluate('document.documentElement.scrollWidth<=innerWidth'),true,'Tràn ngang ở chiều rộng '+width);
    assert.equal(await evaluate(`(()=>{const box=document.getElementById('importDialog').getBoundingClientRect();return box.left>=0&&box.right<=innerWidth})()`),true,'Hộp thoại vượt màn hình ở chiều rộng '+width);
    assert.equal(await evaluate(`document.getElementById('checkImport').getBoundingClientRect().right<=innerWidth`),true);
    assert.equal(await evaluate(`document.querySelector('#importPreview .staff-import-error').getBoundingClientRect().left<document.getElementById('importDialog').getBoundingClientRect().right-40`),true,'Cột lỗi bị khuất ở chiều rộng '+width);
    assert.equal(await evaluate(`getComputedStyle(document.querySelector('#importDialog .portal-dialog-actions')).display`),'none');
    await screenshot('import-'+width);
    await evaluate('document.getElementById("closeImport").click()');
  }
  assert.deepEqual(errors,[]);
  console.log('Staff import browser checks passed.');
}finally{await command('Page.close');socket.close();}
