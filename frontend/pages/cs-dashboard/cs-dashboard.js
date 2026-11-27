import {fetchAPI} from '/js/api.js';
import {mountAdminShell} from '/components/admin-shell/admin-shell.js';

// Dựng sidebar ngay, không chờ API: trước đây trang trắng cho tới khi tải xong tài khoản nên mỗi lần chuyển trang bị khựng.
mountAdminShell();
const $=id=>document.getElementById(id);
// Thứ tự nhóm trong ô lọc (nhãn trùng SupportTicketProjection.CATEGORY ở backend).
const labels={ACCOUNT:'Tài khoản',BILLING:'Thanh toán',TUTOR:'Kết nối gia sư',BOOKING:'Đặt học thử',MEMBERSHIP:'Hội viên',TECHNICAL:'Sự cố kỹ thuật',OTHER:'Nội dung khác'};
let category='',view='',priority='',requestId=0,authenticated=false;
const PRIORITY_LABELS={LOW:'Thấp',MEDIUM:'Bình thường',HIGH:'Cao',URGENT:'Khẩn cấp'};
function node(tag,text,cls){const el=document.createElement(tag);if(text!==undefined)el.textContent=text;if(cls)el.className=cls;return el;}
function date(value){return new Date(value).toLocaleString('vi-VN',{day:'2-digit',month:'2-digit',year:'numeric',hour:'2-digit',minute:'2-digit'});}
/** "25 phút", "3 giờ 10 phút", "2 ngày 4 giờ". */
function duration(ms){const minutes=Math.max(0,Math.round(ms/60000));if(minutes<60)return minutes+' phút';const hours=Math.floor(minutes/60);if(hours<24)return hours+' giờ'+(minutes%60?' '+minutes%60+' phút':'');const days=Math.floor(hours/24);return days+' ngày'+(hours%24?' '+hours%24+' giờ':'');}
function icon(name){const el=node('span',name,'material-symbols-outlined');el.setAttribute('aria-hidden','true');return el;}
/** Ba chỉ số thời hạn phản hồi (SupportTicketSla ở backend). */
function renderSla(sla){
  const box=$('csSla');if(!sla){box.hidden=true;return;}
  box.querySelectorAll('[data-sla]').forEach(el=>{const key=el.dataset.sla,value=sla[key];
    el.textContent=Number(value||0).toLocaleString('vi-VN');});
  box.querySelector('[data-tone=danger]').dataset.alert=String(Number(sla.myOverdue)>0);
  box.hidden=false;
}
/** Mã, mức ưu tiên, thời gian chờ của một yêu cầu trong hàng đợi. */
function chips(ticket){
  const row=node('div',undefined,'cs-ticket-chips');
  row.append(node('span','#'+String(ticket.ticketId).slice(0,8),'cs-code'));
  const priority=node('span',ticket.priorityLabel||PRIORITY_LABELS[ticket.priority],'cs-priority');priority.dataset.priority=ticket.priority;row.append(priority);
  if(ticket.waitingSince){const now=Date.now(),due=new Date(ticket.responseDueAt).getTime(),wait=node('span',undefined,'cs-wait');
    wait.dataset.state=ticket.overdue?'overdue':due-now<30*60000?'soon':'ok';
    wait.append(icon(ticket.overdue?'alarm':'schedule'),document.createTextNode(ticket.overdue?'Quá hạn '+duration(now-due):'Chờ '+duration(now-new Date(ticket.waitingSince).getTime())));row.append(wait);}
  return row;
}
function renderQueue(data){
  $('csQueueCount').textContent=`${data.totalActive.toLocaleString('vi-VN')} yêu cầu đang chờ tiếp nhận, đang xử lý hoặc chờ quản lý.`;
  const link=new URLSearchParams();if(category)link.set('category',category);if(priority)link.set('priority',priority);if(view==='OVERDUE')link.set('overdue','true');
  $('allTicketsLink').href='/cs/support/tickets'+(link.size?'?'+link:'');
  // Ô "Nhóm yêu cầu" kèm số yêu cầu đang mở của từng nhóm.
  const select=$('csQueueFilters').elements.category;select.replaceChildren(new Option(`Tất cả nhóm (${data.totalActive.toLocaleString('vi-VN')})`,''));
  Object.entries(labels).forEach(([key,label])=>select.append(new Option(`${label} (${Number(data.countsByCategory[key]||0).toLocaleString('vi-VN')})`,key)));
  select.value=category;$('csQueueFilters').elements.view.value=view;$('csQueueFilters').elements.priority.value=priority;
  $('csClearFilters').hidden=!(category||view||priority);
  const target=$('csTicketQueue');target.replaceChildren();
  if(!data.queue.length){target.append(node('p',view==='OVERDUE'?'Không có yêu cầu nào quá hạn phản hồi.':view==='MINE'?'Bạn chưa phụ trách yêu cầu nào đang mở.':category||priority?'Chưa có yêu cầu thuộc nhóm này đang cần theo dõi.':'Chưa có yêu cầu hỗ trợ đang cần theo dõi.','cs-dashboard-empty'));return;}
  for(const ticket of data.queue){
    const row=node('article',undefined,'cs-ticket'),copy=node('div',undefined,'cs-ticket-copy'),heading=node('h3'),link=node('a',ticket.subject);link.href=`/cs/support/tickets/${ticket.ticketId}`;heading.append(link);
    const meta=node('p',`${ticket.requesterDisplayName} · ${ticket.categoryLabel} · ${date(ticket.updatedAt)}`,'cs-ticket-meta');
    const open=node('a','Xem yêu cầu →','cs-ticket-link');open.href=link.href;open.setAttribute('aria-label','Xem yêu cầu: '+ticket.subject);copy.append(heading,meta,chips(ticket),open);if(ticket.overdue)row.dataset.overdue='true';
    const status=node('span',ticket.statusLabel,'cs-ticket-status');status.dataset.state=ticket.status;row.append(copy,status);target.append(row);
  }
}
function renderFeedback(feedback){
  const target=$('managerFeedbackContent');target.replaceChildren();
  if(!feedback){target.append(node('p','Chưa có kết quả từ quản lý cần bạn tiếp tục phản hồi.'));return;}
  const title=node('h3',feedback.subject),time=node('p',date(feedback.returnedAt),'cs-feedback-time'),link=node('a','Xem kết quả','cs-result-link');link.href=`/cs/support/tickets/${feedback.ticketId}?tab=internal`;
  target.append(node('p','Kết quả mới nhất','cs-feedback-caption'),title,node('p','Đã có kết quả xử lý. Tiếp tục phản hồi và hướng dẫn người yêu cầu.'),time,link);
}
async function loadDashboard(){
  const current=++requestId;$('csDashboardContent').setAttribute('aria-busy','true');$('csDashboardNotice').textContent='Đang tải hàng đợi hỗ trợ…';$('csDashboardNotice').dataset.error='false';$('retryDashboard').hidden=true;
  try{
    if(!authenticated){const account=await fetchAPI('/v1/tickets/me');if(!account.canHandle)throw new Error('Trang tổng quan này dành cho nhân viên hỗ trợ. Vui lòng mở trang phù hợp với tài khoản của bạn.');authenticated=true;$('csGreeting').textContent='Xin chào, '+account.displayName;}
    const query=new URLSearchParams();if(category)query.set('category',category);if(view)query.set('view',view);if(priority)query.set('priority',priority);
    const data=await fetchAPI('/v1/tickets/dashboard'+(query.size?'?'+query:''));if(current!==requestId)return;
    renderQueue(data);renderSla(data.sla);renderFeedback(data.managerFeedback);$('csDashboardContent').hidden=false;$('csDashboardNotice').textContent='';document.body.dataset.ready='true';document.body.dataset.error='false';
  }catch(error){if(current!==requestId)return;$('csDashboardNotice').textContent=error.message;$('csDashboardNotice').dataset.error='true';$('csDashboardNotice').focus();$('retryDashboard').hidden=false;document.body.dataset.error='true';}
  finally{if(current===requestId)$('csDashboardContent').setAttribute('aria-busy','false');}
}
// Đổi ô lọc nào là tải lại hàng đợi ngay, không cần bấm nút.
$('csQueueFilters').addEventListener('change',()=>{const form=$('csQueueFilters').elements;category=form.category.value;view=form.view.value;priority=form.priority.value;loadDashboard();});
$('csQueueFilters').addEventListener('submit',event=>event.preventDefault());
$('csClearFilters').addEventListener('click',()=>{category='';view='';priority='';loadDashboard();});
$('retryDashboard').addEventListener('click',loadDashboard);
loadDashboard();
