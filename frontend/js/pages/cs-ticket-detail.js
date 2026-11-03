/* Trình bày chi tiết phiếu CS theo Figma, chỉ dùng dữ liệu và quyền do API hiện có trả về. */
import {API_CONFIG} from '../config/api.config.js';

const $=id=>document.getElementById(id);
function node(tag,text,cls){const el=document.createElement(tag);if(text!==undefined)el.textContent=text;if(cls)el.className=cls;return el;}
function entry(list,label,value){const row=node('div'),term=node('dt',label),description=node('dd',value);row.append(term,description);list.append(row);return description;}
export function timestamp(value){if(!value)return 'Chưa có thông tin';const d=new Date(value),pad=number=>String(number).padStart(2,'0');return `${pad(d.getDate())}/${pad(d.getMonth()+1)}/${d.getFullYear()}, ${pad(d.getHours())}:${pad(d.getMinutes())}`;}
function initials(name){return name.replace(/\(.*?\)/g,'').trim().split(/\s+/).slice(-2).map(word=>word[0]||'').join('').toLocaleUpperCase('vi-VN');}
export function fileSize(bytes){if(bytes<1000)return `${bytes.toLocaleString('vi-VN')} byte`;return bytes>=1000000?`${(bytes/1000000).toLocaleString('vi-VN',{maximumFractionDigits:2})} MB`:`${(bytes/1000).toLocaleString('vi-VN',{maximumFractionDigits:1})} KB`;}
const types={'image/jpeg':'Ảnh JPG','image/png':'Ảnh PNG','application/pdf':'Tài liệu PDF'};

function actionDialog(id,title,note,...content){
  const dialog=node('dialog',undefined,'cs-action-dialog');dialog.id=id;dialog.hidden=true;dialog.setAttribute('aria-labelledby',id+'Title');
  const head=node('div',undefined,'support-dialog-head'),heading=node('h2',title),close=node('button','Đóng','support-dialog-close');heading.id=id+'Title';close.type='button';close.onclick=()=>dialog.close();head.append(heading,close);
  const body=node('div',undefined,'support-dialog-body'),error=node('p',undefined,'support-dialog-error');error.setAttribute('role','alert');error.tabIndex=-1;body.append(node('p',note),...content,error);dialog.append(head,body);dialog.addEventListener('close',()=>{error.textContent='';});return dialog;
}
function openButton(id,label,dialogId){const button=node('button',label);button.id=id;button.type='button';button.hidden=true;button.onclick=()=>$(dialogId).showModal();return button;}

export function arrangeCsDetail(){
  document.body.classList.add('cs-ticket-detail');
  const css=document.createElement('link');css.rel='stylesheet';css.href='/styles/cs-ticket-detail.css';document.head.append(css);
  const replyArea=$('replyArea'),ticketActions=$('ticketActions'),internalActions=$('internalActions');
  const columns=node('div',undefined,'cs-detail-columns'),left=node('div',undefined,'cs-detail-conversation'),right=node('aside',undefined,'cs-detail-sidebar');right.setAttribute('aria-label','Thông tin và xử lý yêu cầu');
  left.append($('detailTabs'),$('publicPanel'),$('internalPanel'));
  const reply=node('div',undefined,'cs-request-reply');reply.append(replyArea);
  const result=node('div',undefined,'cs-request-result');result.append(ticketActions);
  const buttons=node('div',undefined,'cs-action-buttons');buttons.append(openButton('csEscalateOpen','Chuyển Quản lý','csTransfer'),openButton('csResolveOpen','Ghi nhận đã giải quyết','csResolve'));
  const info=node('section',undefined,'support-card cs-request-information');info.id='csRequestInformation';info.append(node('h2','Thông tin yêu cầu'));const fields=node('dl',undefined,'cs-information-fields');fields.id='csInformationFields';info.append(fields);
  const edit=node('details',undefined,'cs-edit-information');edit.id='csEditInformation';edit.hidden=true;edit.append(node('summary','Cập nhật phân công và ưu tiên'));const controls=node('div');controls.id='csInformationControls';edit.append(controls);info.append(edit);
  const managerResult=node('div',undefined,'cs-manager-result');managerResult.id='csManagerResult';managerResult.hidden=true;
  const resolveBody=node('div');resolveBody.id='csResolveBody';
  right.append(reply,result,buttons,info,
    actionDialog('csTransfer','Chuyển yêu cầu tới Quản lý','Chỉ chuyển khi vượt khả năng hoặc thẩm quyền xử lý của bạn. Lý do và tóm tắt tại đây không gửi cho người yêu cầu.',internalActions),
    actionDialog('csResolve','Ghi nhận đã giải quyết','Nội dung này được gửi công khai cho người yêu cầu. Người gửi sẽ xác nhận đóng yêu cầu.',managerResult,resolveBody));
  columns.append(left,right);$('supportContent').append(columns);
  document.querySelector('#internalPanel .support-internal-hint').textContent='Chỉ nhân viên có quyền mới xem được. Nội dung nội bộ không gửi cho người yêu cầu.';
}

export function renderCsHeader(ticket){
  const meta=node('p',undefined,'support-ticket-meta'),status=node('span',ticket.statusLabel,'support-badge');status.dataset.state=ticket.status;
  meta.append(status,` · ${ticket.requesterDisplayName} · ${ticket.requesterRoleLabel} · ${ticket.categoryLabel} · Gửi ${timestamp(ticket.createdAt)}`);$('ticketDetail').replaceChildren(meta);
}

export function renderCsInformation(ticket){
  const fields=$('csInformationFields');fields.replaceChildren();entry(fields,'Loại yêu cầu',ticket.categoryLabel);const status=entry(fields,'Trạng thái',''),badge=node('span',ticket.statusLabel,'support-badge');badge.dataset.state=ticket.status;status.append(badge);entry(fields,'Mức ưu tiên',ticket.priorityLabel);entry(fields,'Người phụ trách',ticket.assigneeDisplayName||'Chưa phân công');
  if(ticket.recipientDisplayName)entry(fields,'Quản lý tiếp nhận',ticket.recipientDisplayName);
  if(ticket.relatedContext?.length)entry(fields,'Thông tin liên quan',ticket.relatedContext.map(item=>item.label).join('; '));
}

export function renderCsThread(target,messages){
  target.replaceChildren();
  if(!messages.length){target.append(node('p','Chưa có trao đổi về yêu cầu này.','support-empty'));return;}
  for(const message of messages){
    const requester=message.fromRequester,name=message.senderDisplayName||'Người tham gia';
    const article=node('article',undefined,`cs-thread-message ${requester?'cs-thread-requester':'cs-thread-participant'}`),avatar=node('span',initials(name),'cs-message-avatar');avatar.setAttribute('aria-hidden','true');
    const body=node('div',undefined,'cs-message-body'),header=node('div',undefined,'cs-message-header'),author=node('div');author.append(node('strong',name),node('span',requester?`Người gửi yêu cầu · ${message.senderRoleLabel}`:message.senderRoleLabel,'cs-message-role'));
    const time=node('time',timestamp(message.sentAt));if(message.sentAt)time.dateTime=new Date(message.sentAt).toISOString();header.append(author,time);body.append(header,node('p',message.message,'support-message-text'));
    if(message.attachments?.length){const files=node('div',undefined,'cs-message-attachments');for(const file of message.attachments){const link=node('a',undefined,'cs-attachment-card');link.href=API_CONFIG.BASE_URL.replace(/\/api\/?$/,'')+file.downloadPath;link.target='_blank';link.rel='noopener';const title=node('strong',file.originalName),meta=node('span',[types[file.contentType]||'Tệp đính kèm',typeof file.sizeBytes==='number'?fileSize(file.sizeBytes):''].filter(Boolean).join(' · '));const symbol=node('span','download','material-symbols-outlined cs-attachment-symbol');symbol.setAttribute('aria-hidden','true');link.append(symbol,title,meta);files.append(link);}body.append(files);}
    article.append(avatar,body);target.append(article);
  }
}

export function createCsReplyForm(submit){
  const form=node('form',undefined,'support-form cs-reply-form');form.dataset.action='message';
  const label=node('label','Phản hồi cho người dùng'),text=node('textarea');text.name='message';text.rows=3;text.required=true;text.id='csReplyText';label.htmlFor=text.id;label.append(text);text.setAttribute('aria-describedby','csReplyHelp');
  const help=node('p','Nội dung này sẽ được gửi cho người yêu cầu. Tối đa 10.000 ký tự.','cs-reply-help');help.id='csReplyHelp';
  const input=node('input');input.type='file';input.name='files';input.multiple=true;input.accept='.jpg,.jpeg,.png,.pdf';input.hidden=true;input.tabIndex=-1;
  const files=node('ul',undefined,'cs-selected-files');files.setAttribute('aria-label','Tệp đã chọn');
  const notice=node('p',undefined,'cs-reply-notice');notice.id='csReplyNotice';notice.setAttribute('role','alert');notice.tabIndex=-1;
  const footer=node('div',undefined,'cs-reply-footer'),attach=node('button','Đính kèm','cs-attach-button'),send=node('button','Gửi hướng dẫn','primary');attach.type='button';send.type='submit';attach.onclick=()=>input.click();footer.append(attach,node('span','Tối đa 5 tệp · 10 MB mỗi tệp','cs-file-limit'),send);
  let selected=[];
  function showFiles(){files.replaceChildren();selected.forEach((file,index)=>{const row=node('li'),details=node('span');details.append(node('strong',file.name),node('small',fileSize(file.size)));const remove=node('button','Bỏ');remove.type='button';remove.setAttribute('aria-label','Bỏ tệp '+file.name);remove.onclick=()=>{selected.splice(index,1);showFiles();attach.focus();};row.append(details,remove);files.append(row);});}
  input.onchange=()=>{const incoming=[...input.files],combined=[...selected,...incoming];input.value='';let error='';if(combined.length>5)error='Mỗi lần gửi chỉ được đính kèm tối đa 5 tệp.';else if(incoming.some(file=>file.size<1||file.size>10000000))error='Mỗi tệp phải có dung lượng từ 1 byte đến 10 MB.';else if(incoming.some(file=>!(/\.(jpe?g|png|pdf)$/i.test(file.name))))error='Chỉ hỗ trợ tệp JPG, PNG và PDF.';
    notice.textContent=error;if(error){notice.focus();return;}selected=combined;showFiles();};
  form.onsubmit=event=>{event.preventDefault();submit(form,[...selected]);};form.append(label,help,input,files,notice,footer);return form;
}

function dialogFooter(dialog){
  dialog.querySelectorAll('form').forEach(form=>{const submit=form.querySelector('[type=submit]'),actions=node('div',undefined,'support-dialog-actions'),cancel=node('button','Hủy');cancel.type='button';cancel.onclick=()=>dialog.close();submit.classList.add('primary');actions.append(submit,cancel);form.append(actions);});
}

export function finishCsActions(ticket){
  $('csEditInformation').hidden=!$('csInformationControls').childElementCount;
  const canEscalate=$('internalActions').childElementCount>0,recipient=ticket.allowedActions.includes('recipient');
  $('csTransfer').hidden=!canEscalate;$('csEscalateOpen').hidden=!canEscalate;$('csEscalateOpen').textContent=recipient?'Đổi quản lý tiếp nhận':'Chuyển Quản lý';$('csTransferTitle').textContent=recipient?'Đổi quản lý tiếp nhận':'Chuyển yêu cầu tới Quản lý';
  const resolveCard=$('ticketActions').querySelector('[data-action=resolve]')?.closest('.support-card');if(resolveCard)$('csResolveBody').append(resolveCard);
  $('csResolve').hidden=!resolveCard;$('csResolveOpen').hidden=!resolveCard;
  const decision=!ticket.escalation?.pending&&ticket.escalation?.decisionText,result=$('csManagerResult');result.hidden=!decision;result.replaceChildren();
  if(decision)result.append(node('h3','Quản lý đã phản hồi'),node('p',decision,'support-preserve'));
  $('csResolveTitle').textContent=decision?'Kết quả từ Quản lý':'Ghi nhận đã giải quyết';
  dialogFooter($('csTransfer'));dialogFooter($('csResolve'));
  if(!$('ticketActions').childElementCount&&!resolveCard){const explanations={ESCALATED:'Yêu cầu đang chờ quản lý. Chỉ có thể gửi kết quả sau khi được trả lại xử lý.',RESOLVED:'Đã gửi kết quả giải quyết, đang chờ người gửi xác nhận đóng.',CLOSED:'Yêu cầu đã đóng. Bạn chỉ có thể xem lại nội dung và lịch sử.'};$('ticketActions').append(node('p',explanations[ticket.status]||'Chỉ nhân viên đang phụ trách được gửi kết quả giải quyết.','cs-action-explanation'));}
}
