import {fetchAPI} from '/js/api.js';
import {mountAdminShell} from '/components/admin-shell/admin-shell.js';

const $=id=>document.getElementById(id);
const labels={'':'Tất cả',BOOKING:'Học thử',TUTOR:'Kết nối',MEMBERSHIP:'Gói hội viên'};
let category='',requestId=0,authenticated=false;
function node(tag,text,cls){const el=document.createElement(tag);if(text!==undefined)el.textContent=text;if(cls)el.className=cls;return el;}
function date(value){return new Date(value).toLocaleString('vi-VN',{day:'2-digit',month:'2-digit',year:'numeric',hour:'2-digit',minute:'2-digit'});}
function renderQueue(data){
  $('csQueueCount').textContent=`${data.totalActive.toLocaleString('vi-VN')} yêu cầu đang chờ tiếp nhận, đang xử lý hoặc chờ quản lý.`;
  $('allTicketsLink').href='/cs/support/tickets'+(category?'?category='+category:'');
  document.querySelectorAll('[data-category]').forEach(button=>{const key=button.dataset.category,count=key?data.countsByCategory[key]:data.totalActive;button.textContent=`${labels[key]} · ${count.toLocaleString('vi-VN')}`;button.setAttribute('aria-pressed',String(key===category));});
  const target=$('csTicketQueue');target.replaceChildren();
  if(!data.queue.length){target.append(node('p',category?'Chưa có yêu cầu thuộc nhóm này đang cần theo dõi.':'Chưa có yêu cầu hỗ trợ đang cần theo dõi.','cs-dashboard-empty'));return;}
  for(const ticket of data.queue){
    const row=node('article',undefined,'cs-ticket'),copy=node('div',undefined,'cs-ticket-copy'),heading=node('h3'),link=node('a',ticket.subject);link.href=`/cs/support/tickets/${ticket.ticketId}`;heading.append(link);
    const meta=node('p',`${ticket.requesterDisplayName} · ${ticket.categoryLabel} · ${date(ticket.updatedAt)}`,'cs-ticket-meta');
    const open=node('a','Xem yêu cầu →','cs-ticket-link');open.href=link.href;open.setAttribute('aria-label','Xem yêu cầu: '+ticket.subject);copy.append(heading,meta,open);
    const status=node('span',ticket.statusLabel,'cs-ticket-status');status.dataset.state=ticket.status;row.append(copy,status);target.append(row);
  }
}
/** Ca của nhân viên: đang trong ca hay chưa, ca kế tiếp, đang giữ bao nhiêu yêu cầu. */
function renderShift(shift){
  const box=$('csShift');box.replaceChildren();if(!shift){box.hidden=true;return;}
  const dayLabel=value=>{const [y,m,d]=value.split('-');return `${d}/${m}`;};
  const icon=node('span',shift.current?'●':'○','cs-shift-dot'),copy=node('div',undefined,'cs-shift-copy');
  if(shift.current){box.dataset.state='on';copy.append(node('strong',`Bạn đang trong ${shift.current.label.toLowerCase()} (${shift.current.start} – ${shift.current.end})`),node('p',shift.activeTickets>=shift.maxActive?`Bạn đang xử lý ${shift.activeTickets} yêu cầu. Chưa nhận thêm việc mới; kết quả quản lý trả về vẫn tiếp tục do bạn phụ trách.`:'Bạn đang rảnh. Yêu cầu mới sẽ được tự giao cho bạn.'));}
  else{box.dataset.state='off';copy.append(node('strong','Bạn không trong ca làm lúc này'),node('p',(shift.next?`Ca tiếp theo: ${shift.next.label.toLowerCase()} ${dayLabel(shift.next.date)}, ${shift.next.start} – ${shift.next.end}.`:'Bạn chưa được xếp ca nào sắp tới.')+' Yêu cầu đang phụ trách vẫn được giữ để tiếp tục xử lý.'));}
  const link=node('a','Xem lịch làm việc','cs-shift-link');link.href='/pages/support-shifts/?view=cs';
  box.append(icon,copy,link);box.hidden=false;
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
    if(!authenticated){const account=await fetchAPI('/v1/tickets/me');if(!account.canHandle)throw new Error('Trang tổng quan này dành cho nhân viên hỗ trợ. Vui lòng mở trang phù hợp với tài khoản của bạn.');authenticated=true;mountAdminShell();$('csGreeting').textContent='Xin chào, '+account.displayName;}
    const data=await fetchAPI('/v1/tickets/dashboard'+(category?'?category='+category:''));if(current!==requestId)return;
    renderQueue(data);renderFeedback(data.managerFeedback);renderShift(data.shift);$('csDashboardContent').hidden=false;$('csDashboardNotice').textContent='';document.body.dataset.ready='true';document.body.dataset.error='false';
  }catch(error){if(current!==requestId)return;$('csDashboardNotice').textContent=error.message;$('csDashboardNotice').dataset.error='true';$('csDashboardNotice').focus();$('retryDashboard').hidden=false;document.body.dataset.error='true';}
  finally{if(current===requestId)$('csDashboardContent').setAttribute('aria-busy','false');}
}
document.querySelectorAll('[data-category]').forEach(button=>button.addEventListener('click',()=>{category=button.dataset.category;loadDashboard();}));
$('retryDashboard').addEventListener('click',loadDashboard);
loadDashboard();
