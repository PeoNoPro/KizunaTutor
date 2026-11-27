/* Trình bày chi tiết phiếu CS theo Figma, chỉ dùng dữ liệu và quyền do API hiện có trả về. */
import {API_CONFIG} from '../config/api.config.js';

/** Giới hạn do Admin cấu hình; server vẫn kiểm tra lại nên lỗi tải ở đây chỉ làm giao diện dùng mức mặc định. */
let attachmentMaxBytes=10000000;
export const attachmentBytes=()=>attachmentMaxBytes;
export const attachmentMegabytes=()=>Math.round(attachmentMaxBytes/1000000);
let attachmentLimitRequest;
export function loadAttachmentLimit(){
  if(!attachmentLimitRequest)attachmentLimitRequest=fetch(API_CONFIG.BASE_URL+'/v1/tickets/attachment-limit',{credentials:'include',signal:AbortSignal.timeout(4500)})
    .then(response=>response.ok?response.json():null).then(result=>{if(result?.data?.maxBytes){attachmentMaxBytes=result.data.maxBytes;document.querySelectorAll('[data-attachment-limit]').forEach(item=>{item.textContent=attachmentMegabytes();});}return attachmentMaxBytes;})
    .catch(()=>{attachmentLimitRequest=null;return attachmentMaxBytes;});
  return attachmentLimitRequest;
}

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

/**
 * Bố cục 2 cột (gọn lại 2026-11-13, trước đó 3 cột bị rối):
 *  - Trái (rộng): tiêu đề + thời hạn phản hồi, trao đổi xen ghi chú nội bộ, ô soạn "Trả lời người gửi / Ghi chú nội bộ".
 *  - Phải: thao tác, người gửi, phân công, chuyển cấp, lịch sử xử lý (thu gọn sẵn).
 * Trạng thái, mức ưu tiên, nhóm yêu cầu chỉ hiện một lần ở tiêu đề.
 * `manager` (2026-11-13): Quản lý dùng cùng bố cục; cột phải là thẻ chuyển cấp + các thẻ quyết định
 * (Quyết định nội bộ, Trả kết quả) thay cho hai nút của CS. Quản lý chỉ viết ghi chú nội bộ.
 */
export function arrangeCsDetail(manager=false){
  document.body.classList.add('cs-ticket-detail');
  const css=document.createElement('link');css.rel='stylesheet';css.href='/styles/cs-ticket-detail.css';document.head.append(css);
  const replyArea=$('replyArea'),ticketActions=$('ticketActions'),internalActions=$('internalActions');
  const columns=node('div',undefined,'cs-detail-columns');
  const requester=node('div',undefined,'cs-detail-requester');requester.id='csRequester';
  const center=node('div',undefined,'cs-detail-conversation');
  const right=node('aside',undefined,'cs-detail-sidebar');right.setAttribute('aria-label','Thông tin và xử lý yêu cầu');
  // Giữa: tiêu đề, trao đổi, ô soạn.
  const head=$('ticketDetail');head.classList.add('support-card','cs-ticket-head');
  const composer=node('section',undefined,'support-card cs-composer');composer.id='csComposer';composer.setAttribute('aria-label','Soạn trả lời hoặc ghi chú nội bộ');
  const tabs=node('div',undefined,'cs-composer-tabs');tabs.id='csComposerTabs';
  const noteArea=node('div',undefined,'cs-note-area');noteArea.id='noteArea';noteArea.hidden=true;
  composer.append(tabs,replyArea,noteArea);
  $('publicPanel').querySelector('.support-thread>p')?.remove();
  center.append(head,$('publicPanel'),composer);
  $('internalPanel').hidden=true;
  // Phải: thao tác, thông tin, chuyển cấp, lịch sử.
  const result=node('div',undefined,'cs-request-result');result.append(ticketActions);
  // "Chuyển Quản lý" nằm ngay dưới ô soạn (2026-11-13), chỗ CS đang thao tác nên dễ thấy.
  const buttons=node('div',undefined,'cs-action-buttons cs-escalate-row');buttons.id='csEscalateRow';
  buttons.append(node('span','Vượt quá khả năng hoặc thẩm quyền xử lý của bạn?','cs-escalate-text'),openButton('csEscalateOpen','Chuyển Quản lý','csTransfer'));
  center.append(buttons);
  const info=node('section',undefined,'support-card cs-request-information');info.id='csRequestInformation';info.append(node('h2','Thông tin xử lý'));const fields=node('dl',undefined,'cs-information-fields');fields.id='csInformationFields';info.append(fields);
  const edit=node('details',undefined,'cs-edit-information');edit.id='csEditInformation';edit.hidden=true;edit.append(node('summary','Đổi mức ưu tiên'));const controls=node('div');controls.id='csInformationControls';edit.append(controls);info.append(edit);
  const history=$('historyDetails');history.open=false;history.querySelector('summary').replaceChildren('Lịch sử xử lý');
  // Quản lý: các thẻ quyết định nằm thẳng ở cột phải, ngay dưới thẻ chuyển cấp.
  const decisions=node('div',undefined,'cs-manager-decisions');decisions.id='csManagerDecisions';if(manager)decisions.append(internalActions);
  // Quản lý vẫn giữ (ẩn) khung nút/kết quả của CS vì renderActions luôn dọn #ticketActions.
  right.append(...(manager?[result,$('internalArea'),decisions,requester,info,history]:[result,requester,info,$('internalArea'),history]),
    actionDialog('csTransfer','Chuyển yêu cầu tới Quản lý','Chỉ chuyển khi vượt khả năng hoặc thẩm quyền xử lý của bạn. Lý do và tóm tắt tại đây không gửi cho người yêu cầu.',...(manager?[]:[internalActions])));
  if(manager){buttons.hidden=true;result.hidden=true;composer.setAttribute('aria-label','Ghi chú nội bộ');}
  columns.append(center,right);$('supportContent').append(columns);
}

/** "25 phút", "3 giờ 10 phút", "2 ngày 4 giờ". */
export function duration(ms){const minutes=Math.max(0,Math.round(ms/60000));if(minutes<60)return minutes+' phút';const hours=Math.floor(minutes/60);if(hours<24)return hours+' giờ'+(minutes%60?' '+minutes%60+' phút':'');const days=Math.floor(hours/24);return days+' ngày'+(hours%24?' '+hours%24+' giờ':'');}
function symbol(name){const el=node('span',name,'material-symbols-outlined');el.setAttribute('aria-hidden','true');return el;}

/** Tiêu đề yêu cầu, mã, trạng thái, mức ưu tiên và đồng hồ thời hạn phản hồi. */
export function renderCsHeader(ticket){
  const top=node('div',undefined,'cs-head-top'),title=node('h2',ticket.subject,'cs-head-title'),status=node('span',ticket.statusLabel,'support-badge');status.dataset.state=ticket.status;top.append(title,status);
  const chips=node('div',undefined,'cs-head-chips');
  const code=node('span','#'+String(ticket.ticketId).slice(0,8),'cs-code');code.title='Mã yêu cầu: '+ticket.ticketId;
  const priority=node('span',ticket.priorityLabel,'cs-priority');priority.dataset.priority=ticket.priority;
  chips.append(code,priority,node('span',ticket.categoryLabel,'cs-head-category'));
  const sla=node('div',undefined,'cs-head-sla');
  if(ticket.waitingSince){
    const now=Date.now(),due=new Date(ticket.responseDueAt).getTime(),state=ticket.overdue?'overdue':due-now<30*60000?'soon':'ok';sla.dataset.state=state;
    const text=node('div');
    text.append(node('strong',ticket.overdue?'Quá hạn phản hồi '+duration(now-due):'Người gửi đang chờ '+duration(now-new Date(ticket.waitingSince).getTime())),
      node('span',`Hạn trả lời ${timestamp(ticket.responseDueAt)} · mức ${ticket.priorityLabel.toLocaleLowerCase('vi-VN')} trả lời trong ${duration((ticket.slaTargetMinutes||0)*60000)}`));
    sla.append(symbol(ticket.overdue?'alarm':'schedule'),text);
  }else{
    sla.dataset.state='none';
    const waitingStates={RESOLVED:'Đã gửi kết quả, đang chờ người gửi xác nhận đóng.',CLOSED:'Yêu cầu đã đóng.'};
    sla.append(symbol('check_circle'),node('span',waitingStates[ticket.status]||'Không có tin nhắn nào của người gửi đang chờ trả lời.'));
  }
  const requesterName=ticket.requester?.displayName||ticket.requesterDisplayName;
  chips.append(node('span',[requesterName?'Từ '+requesterName:'','gửi '+timestamp(ticket.createdAt)].filter(Boolean).join(' · '),'cs-head-meta'));
  $('ticketDetail').replaceChildren(top,chips,sla);
}

/** Thẻ người gửi ở cột phải: tên, vai trò, số yêu cầu đã gửi; các yêu cầu khác thu gọn trong một mục mở ra được. */
export function renderCsRequester(ticket,link){
  const box=$('csRequester');box.replaceChildren();
  const person=ticket.requester||{displayName:ticket.requesterDisplayName,roleLabel:ticket.requesterRoleLabel};
  const card=node('section',undefined,'support-card cs-requester-card');
  const who=node('div',undefined,'cs-requester-who'),avatar=node('span',initials(person.displayName||'?'),'cs-requester-avatar');avatar.setAttribute('aria-hidden','true');
  const name=node('div');name.append(node('strong',person.displayName||'Người gửi'),node('span',[person.roleLabel,typeof person.ticketCount==='number'?'đã gửi '+person.ticketCount.toLocaleString('vi-VN')+' yêu cầu':''].filter(Boolean).join(' · '),'cs-requester-role'));who.append(avatar,name);card.append(who);
  card.prepend(node('h3','Người gửi'));
  if(person.otherTickets?.length){
    // Mặc định thu gọn: chỉ mở khi cần đối chiếu yêu cầu cũ.
    const others=node('details',undefined,'cs-other-details');others.append(node('summary',`Yêu cầu khác của người này (${person.otherTickets.length})`));
    const list=node('ul',undefined,'cs-other-tickets');person.otherTickets.forEach(item=>{const row=node('li'),anchor=node('a',undefined);anchor.href=link(item.ticketId);
      const badge=node('span',item.statusLabel,'support-badge');badge.dataset.state=item.status;anchor.append(node('strong',item.subject),node('span',timestamp(item.createdAt),'cs-other-date'),badge);row.append(anchor);list.append(row);});
    others.append(list);card.append(others);
  }else if(person.otherTickets)card.append(node('p','Đây là yêu cầu đầu tiên của người này.','cs-requester-empty'));
  box.append(card);
}

export function renderCsInformation(ticket){
  // Hệ thống chỉ có một nhân viên CS nên không hiện người phụ trách; trạng thái, ưu tiên, nhóm đã ở tiêu đề.
  const fields=$('csInformationFields');fields.replaceChildren();
  if(ticket.recipientDisplayName)entry(fields,'Quản lý tiếp nhận',ticket.recipientDisplayName);
  if(ticket.relatedContext?.length)entry(fields,'Thông tin liên quan',ticket.relatedContext.map(item=>item.label).join('; '));
}

/** Một ghi chú nội bộ trong dòng trao đổi: nền vàng, biểu tượng khóa, ghi rõ người gửi không thấy. */
function noteArticle(note){
  const name=note.authorDisplayName||'Nhân viên',article=node('article',undefined,'cs-thread-message cs-thread-note'),avatar=node('span',initials(name),'cs-message-avatar');avatar.setAttribute('aria-hidden','true');
  const body=node('div',undefined,'cs-message-body'),header=node('div',undefined,'cs-message-header'),author=node('div');
  const tag=node('span',undefined,'cs-note-tag');tag.append(symbol('lock'),document.createTextNode('Ghi chú nội bộ · người gửi không thấy'));
  author.append(node('strong',name),node('span',note.authorRoleLabel,'cs-message-role'));
  const time=node('time',timestamp(note.createdAt));if(note.createdAt)time.dateTime=new Date(note.createdAt).toISOString();header.append(author,time);
  body.append(tag,header,node('p',note.note,'support-message-text'));article.append(avatar,body);return article;
}
/** `notes`: ghi chú nội bộ thuộc khoảng thời gian của trang tin nhắn này, được xếp xen theo thời gian. */
export function renderCsThread(target,messages,notes=[]){
  target.replaceChildren();
  if(!messages.length&&!notes.length){target.append(node('p','Chưa có trao đổi về yêu cầu này.','support-empty'));return;}
  const items=[...messages.map(item=>({time:new Date(item.sentAt).getTime(),message:item})),...notes.map(item=>({time:new Date(item.createdAt).getTime(),note:item}))].sort((a,b)=>a.time-b.time);
  for(const item of items){
    if(item.note){target.append(noteArticle(item.note));continue;}
    const message=item.message;
    const requester=message.fromRequester,name=message.senderDisplayName||'Người tham gia';
    const article=node('article',undefined,`cs-thread-message ${requester?'cs-thread-requester':'cs-thread-participant'}`),avatar=node('span',initials(name),'cs-message-avatar');avatar.setAttribute('aria-hidden','true');
    const body=node('div',undefined,'cs-message-body'),header=node('div',undefined,'cs-message-header'),author=node('div');author.append(node('strong',name),node('span',requester?`Người gửi yêu cầu · ${message.senderRoleLabel}`:message.senderRoleLabel,'cs-message-role'));
    const time=node('time',timestamp(message.sentAt));if(message.sentAt)time.dateTime=new Date(message.sentAt).toISOString();header.append(author,time);body.append(header,node('p',message.message,'support-message-text'));
    if(message.attachments?.length){const files=node('div',undefined,'cs-message-attachments');for(const file of message.attachments){const link=node('a',undefined,'cs-attachment-card');link.href=API_CONFIG.BASE_URL.replace(/\/api\/?$/,'')+file.downloadPath;link.target='_blank';link.rel='noopener';const title=node('strong',file.originalName),meta=node('span',[types[file.contentType]||'Tệp đính kèm',typeof file.sizeBytes==='number'?fileSize(file.sizeBytes):''].filter(Boolean).join(' · '));const symbol=node('span','download','material-symbols-outlined cs-attachment-symbol');symbol.setAttribute('aria-hidden','true');link.append(symbol,title,meta);files.append(link);}body.append(files);}
    article.append(avatar,body);target.append(article);
  }
}

/** `copy` (tùy chọn) đổi chữ cho học viên/gia sư: {label, help, button, placeholder}. */
export function createCsReplyForm(submit,copy={}){
  const form=node('form',undefined,'support-form cs-reply-form');form.dataset.action='message';
  const label=node('label',copy.label||'Phản hồi cho người dùng'),text=node('textarea');text.name='message';text.rows=3;text.required=true;text.id='csReplyText';if(copy.placeholder)text.placeholder=copy.placeholder;label.htmlFor=text.id;label.append(text);text.setAttribute('aria-describedby','csReplyHelp');
  const help=node('p',copy.help??'Nội dung này sẽ được gửi cho người yêu cầu. Tối đa 10.000 ký tự.','cs-reply-help');help.id='csReplyHelp';if(copy.help==='')help.hidden=true;
  const input=node('input');input.type='file';input.name='files';input.multiple=true;input.accept='.jpg,.jpeg,.png,.pdf';input.hidden=true;input.tabIndex=-1;
  const files=node('ul',undefined,'cs-selected-files');files.setAttribute('aria-label','Tệp đã chọn');
  const notice=node('p',undefined,'cs-reply-notice');notice.id='csReplyNotice';notice.setAttribute('role','alert');notice.tabIndex=-1;
  const footer=node('div',undefined,'cs-reply-footer'),attach=node('button','Đính kèm','cs-attach-button'),send=node('button',copy.button||'Gửi hướng dẫn',copy.resolve?undefined:'primary');attach.type='button';send.type='submit';attach.onclick=()=>input.click();footer.append(attach,node('span',`Tối đa 5 tệp · ${attachmentMegabytes()} MB mỗi tệp`,'cs-file-limit'),send);
  // Gộp "Ghi nhận đã giải quyết" vào ô soạn (2026-11-13): gửi nội dung cuối và chờ người gửi xác nhận đóng.
  let resolveButton=null;
  if(copy.resolve){resolveButton=node('button','Đánh dấu đã giải quyết','primary cs-resolve-submit');resolveButton.type='submit';resolveButton.dataset.resolve='true';footer.append(resolveButton);
    help.textContent='“Gửi”: trao đổi tiếp, yêu cầu vẫn đang xử lý. “Đánh dấu đã giải quyết”: báo người gửi là đã xong (kèm nội dung đang viết nếu có), người gửi xác nhận thì yêu cầu được đóng.';
    // Ô soạn để trống: không bắt buộc nhập khi đánh dấu giải quyết, chỉ bắt buộc khi bấm "Gửi".
    text.required=false;}
  let selected=[];
  function showFiles(){files.replaceChildren();selected.forEach((file,index)=>{const row=node('li'),details=node('span');details.append(node('strong',file.name),node('small',fileSize(file.size)));const remove=node('button','Bỏ');remove.type='button';remove.setAttribute('aria-label','Bỏ tệp '+file.name);remove.onclick=()=>{selected.splice(index,1);showFiles();attach.focus();};row.append(details,remove);files.append(row);});}
  input.onchange=()=>{const incoming=[...input.files],combined=[...selected,...incoming];input.value='';let error='';if(combined.length>5)error='Mỗi lần gửi chỉ được đính kèm tối đa 5 tệp.';else if(incoming.some(file=>file.size<1||file.size>attachmentBytes()))error=`Mỗi tệp phải có dung lượng từ 1 byte đến ${attachmentMegabytes()} MB.`;else if(incoming.some(file=>!(/\.(jpe?g|png|pdf)$/i.test(file.name))))error='Chỉ hỗ trợ tệp JPG, PNG và PDF.';
    notice.textContent=error;if(error){notice.focus();return;}selected=combined;showFiles();};
  form.onsubmit=event=>{event.preventDefault();
    if(resolveButton&&event.submitter===resolveButton){
      if(selected.length){notice.textContent='Đánh dấu đã giải quyết không gửi kèm tệp được. Hãy bấm “Gửi” cho tệp trước, rồi gửi kết quả cuối.';notice.focus();return;}
      copy.resolve(form);return;}
    if(!text.value.trim()){notice.textContent='Vui lòng nhập nội dung trước khi gửi.';text.focus();return;}
    submit(form,[...selected]);};
  form.append(label,help,input,files,notice,footer);return form;
}

/** Ô ghi chú nội bộ: tối đa 5.000 ký tự, chỉ nhân viên hỗ trợ và Quản lý thấy. */
export function createNoteForm(submit){
  const form=node('form',undefined,'support-form cs-note-form');form.dataset.action='note';
  const label=node('label','Ghi chú nội bộ'),text=node('textarea');text.name='note';text.rows=3;text.required=true;text.maxLength=5000;text.id='csNoteText';
  text.placeholder='Ví dụ: Đã kiểm tra giao dịch, chờ bên thanh toán xác nhận trước 17:00.';label.htmlFor=text.id;label.append(text);
  const help=node('p',undefined,'cs-note-help');help.append(symbol('lock'),document.createTextNode('Chỉ nhân viên hỗ trợ và Quản lý thấy. Không gửi cho người yêu cầu, không tính là đã trả lời.'));
  const notice=node('p',undefined,'cs-reply-notice');notice.setAttribute('role','alert');notice.tabIndex=-1;
  const footer=node('div',undefined,'cs-reply-footer'),save=node('button','Lưu ghi chú','cs-note-submit');save.type='submit';footer.append(save);
  form.onsubmit=event=>{event.preventDefault();submit(form,notice);};form.append(label,help,notice,footer);return form;
}

function dialogFooter(dialog){
  dialog.querySelectorAll('form').forEach(form=>{const submit=form.querySelector('[type=submit]'),actions=node('div',undefined,'support-dialog-actions'),cancel=node('button','Hủy');cancel.type='button';cancel.onclick=()=>dialog.close();submit.classList.add('primary');actions.append(submit,cancel);form.append(actions);});
}

export function finishCsActions(ticket,manager=false){
  $('csEditInformation').hidden=!$('csInformationControls').childElementCount;
  if(manager){
    ['csTransfer','csEscalateOpen','csEscalateRow'].forEach(id=>{$(id).hidden=true;});
    $('csManagerDecisions').hidden=!$('internalActions').childElementCount;return;
  }
  const canEscalate=$('internalActions').childElementCount>0,recipient=ticket.allowedActions.includes('recipient');
  $('csTransfer').hidden=!canEscalate;$('csEscalateOpen').hidden=!canEscalate;$('csEscalateRow').hidden=!canEscalate;
  $('csEscalateRow').querySelector('.cs-escalate-text').textContent=recipient?'Cần đổi Quản lý tiếp nhận yêu cầu này?':'Vượt quá khả năng hoặc thẩm quyền xử lý của bạn?';$('csEscalateOpen').textContent=recipient?'Đổi quản lý tiếp nhận':'Chuyển Quản lý';$('csTransferTitle').textContent=recipient?'Đổi quản lý tiếp nhận':'Chuyển yêu cầu tới Quản lý';
  // Ghi nhận đã giải quyết nằm trong ô soạn (nút "Đánh dấu đã giải quyết"); kết quả của Quản lý hiện ở thẻ chuyển cấp.
  dialogFooter($('csTransfer'));
  if(!$('ticketActions').childElementCount&&!ticket.allowedActions.includes('message')&&!['IN_PROGRESS','OPEN'].includes(ticket.status)){const explanations={ESCALATED:'Yêu cầu đang chờ quản lý. Chỉ có thể gửi kết quả sau khi được trả lại xử lý.',RESOLVED:'Đã gửi kết quả giải quyết, đang chờ người gửi xác nhận đóng.',CLOSED:'Yêu cầu đã đóng. Bạn chỉ có thể xem lại nội dung và lịch sử.'};$('ticketActions').append(node('p',explanations[ticket.status]||'Chỉ nhân viên đang phụ trách được gửi kết quả giải quyết.','cs-action-explanation'));}
}
