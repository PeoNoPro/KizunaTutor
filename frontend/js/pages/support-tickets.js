import {fetchAPI, fetchMultipartAPI} from '../api.js';
import {API_CONFIG} from '../config/api.config.js';
import {mountAdminShell,logout} from '../../components/admin-shell/admin-shell.js';
import {mountLearnerFrame} from './learner-frame.js';
import {mountTutorFrame} from './tutor-frame.js';
import {showToast} from '../main.js';
import {createTicketCountsLoader,countTicketStatuses} from './support-counts.js';
import {loadAttachmentLimit,attachmentBytes,attachmentMegabytes,arrangeCsDetail,renderCsHeader,renderCsInformation,renderCsThread,createCsReplyForm,finishCsActions,timestamp} from './cs-ticket-detail.js';

const base = '/v1/tickets';
const statuses = {OPEN:'Chưa nhận xử lý', IN_PROGRESS:'Đang xử lý', ESCALATED:'Chờ quản lý xem xét', RESOLVED:'Đã giải quyết', CLOSED:'Đã đóng'};
/**
 * Học viên, gia sư không cần biết yêu cầu đã có nhân viên nhận hay đang chờ quản lý: với họ đều là "Đang xử lý".
 * Đã giải quyết nghĩa là đang chờ chính họ xác nhận đóng.
 */
const requesterStatuses = {OPEN:'Đang xử lý', IN_PROGRESS:'Đang xử lý', ESCALATED:'Đang xử lý', RESOLVED:'Chờ bạn xác nhận', CLOSED:'Đã đóng'};
const categories = {ACCOUNT:'Tài khoản', BILLING:'Thanh toán', TUTOR:'Kết nối gia sư', BOOKING:'Đặt học thử', MEMBERSHIP:'Hội viên', TECHNICAL:'Sự cố kỹ thuật', OTHER:'Nội dung khác'};
const priorities = {LOW:'Thấp', MEDIUM:'Bình thường', HIGH:'Cao', URGENT:'Khẩn cấp'};
/** Biểu tượng (Material Symbols) cho từng nhóm yêu cầu, dùng ở thẻ yêu cầu của học viên/gia sư. */
const categoryIcons = {ACCOUNT:'person', BILLING:'payments', TUTOR:'school', BOOKING:'event', MEMBERSHIP:'workspace_premium', TECHNICAL:'build', OTHER:'help'};
function icon(name, cls='') {const node=element('span',name,'material-symbols-outlined '+cls);node.setAttribute('aria-hidden','true');return node;}
const events = {CREATE:'Tạo yêu cầu', CLAIM:'Tiếp nhận yêu cầu', AUTO_ASSIGN:'Hệ thống giao theo ca', REASSIGN_CS:'Đổi nhân viên phụ trách', PRIORITY_CHANGE:'Đổi mức ưu tiên', MESSAGE_SENT:'Gửi trao đổi', STATUS_CHANGE:'Gửi kết quả giải quyết', ESCALATE:'Chuyển quản lý', REASSIGN_MANAGER:'Đổi quản lý tiếp nhận', SAVE_DECISION:'Lưu quyết định nội bộ', RETURN_RESOLUTION:'Trả kết quả cho nhân viên', CLOSE:'Người gửi xác nhận đóng'};
// Tab theo Figma chỉ gom các trạng thái FE-10 có sẵn, không thêm trạng thái nghiệp vụ mới.
const tabGroups = {
  requester:[{label:'Đang theo dõi',statuses:['OPEN','IN_PROGRESS','ESCALATED']},{label:'Đã giải quyết',statuses:['RESOLVED','CLOSED']}],
  cs:[{label:'Cần xử lý',statuses:['OPEN','IN_PROGRESS']},{label:'Chờ người dùng xác nhận',statuses:['RESOLVED']},{label:'Đã chuyển Quản lý',statuses:['ESCALATED']},{label:'Đã đóng',statuses:['CLOSED']}],
  manager:[{label:'Chờ xem xét',statuses:['ESCALATED']},{label:'Đã phản hồi',statuses:['IN_PROGRESS','RESOLVED','CLOSED']}]
};
const pageCopy = {
  requester:{title:'Hỗ trợ của tôi',lead:'Theo dõi trao đổi và kết quả hỗ trợ về việc sử dụng nền tảng.',list:'Các yêu cầu liên quan',empty:'Chưa có yêu cầu hỗ trợ phù hợp',back:['/pages/home/','← Về trang chủ'],hint:'Chọn “Gửi yêu cầu hỗ trợ” khi bạn cần được giúp đỡ.'},
  cs:{title:'Yêu cầu hỗ trợ',lead:'Tiếp nhận, trao đổi với người dùng và chuyển Quản lý khi cần.',list:'Các yêu cầu liên quan',empty:'Chưa có yêu cầu phù hợp',hint:'Hãy thử chọn nhóm khác hoặc điều chỉnh bộ lọc.'},
  manager:{title:'Yêu cầu hỗ trợ chuyển lên',lead:'Xem lịch sử xử lý và gửi phương án trở lại Nhân viên hỗ trợ.',list:'Yêu cầu từ Nhân viên hỗ trợ',empty:'Chưa có yêu cầu phù hợp',hint:'Yêu cầu được chuyển cho bạn sẽ hiển thị tại đây.'}
};
const $ = id => document.getElementById(id);
function element(tag, text, cls) {const el=document.createElement(tag); if(text!==undefined)el.textContent=text; if(cls)el.className=cls; return el;}
function option(select,value,label) {const el=element('option',label);el.value=value;select.append(el);}
function options(select, choices) {Object.entries(choices).forEach(([value,label])=>option(select,value,label));}
function date(value) {return value ? timestamp(value) : 'Chưa có thời gian';}
function notice(message,error=false,target='supportNotice') {const el=$(target);el.textContent=message;el.dataset.error=String(error);el.dataset.loading=String(!error&&message.endsWith('…'));if(error)el.focus();}
function empty(target,message,title) {const box=element('div',undefined,'support-empty');if(title){const icon=element('span','inbox','material-symbols-outlined');icon.setAttribute('aria-hidden','true');box.append(icon,element('h3',title));}box.append(element('p',message));target.replaceChildren(box);}
function badge(text,state) {const el=element('span',text,'support-badge');if(state)el.dataset.state=state;return el;}
function paging(target,data,load) {target.replaceChildren();if(data.totalPages<=1)return;const prev=element('button','Trang trước'),next=element('button','Trang sau');prev.type=next.type='button';prev.disabled=data.page===0;next.disabled=data.page+1>=data.totalPages;prev.onclick=()=>load(data.page-1);next.onclick=()=>load(data.page+1);target.append(prev,element('span',`Trang ${data.page+1} / ${data.totalPages}`),next);}
function validText(value,max) {if(!value?.replace(/[\s\p{Z}\p{Cf}]/gu,'') || [...value].length>max)throw new Error(`Vui lòng nhập nội dung không bỏ trống, tối đa ${max.toLocaleString('vi-VN')} ký tự.`);return value.trim();}
function validFiles(input) {const files=[...input.files];if(files.length>5 || files.some(f=>f.size<1||f.size>attachmentBytes()))throw new Error(`Chọn tối đa 5 tệp, mỗi tệp từ 1 byte đến ${attachmentMegabytes()} MB.`);return files;}
function field(form,labelText,name,tag='input',choices) {const label=element('label',labelText),input=element(tag);input.name=name;input.required=true;if(tag==='textarea')input.rows=4;if(choices)options(input,choices);label.append(input);form.append(label);return input;}

// admin.css/operations.css có rule toàn cục (như "td a") nên chỉ nạp cho giao diện nội bộ, không áp vào trang học viên/gia sư.
function loadStaffStyles() {
  const anchor=document.querySelector('link[href="/styles/support-tickets.css"]');
  return Promise.all(['/styles/admin.css','/styles/operations.css','/styles/admin-portal.css'].map(href=>new Promise((resolve,reject)=>{const link=document.createElement('link');link.rel='stylesheet';link.href=href;link.onload=resolve;link.onerror=()=>reject(new Error('Chưa tải được giao diện. Vui lòng thử lại.'));anchor.before(link);})));
}

export async function startSupportPage(isDetail) {
  let account,role,ticket,page=0,threadPage=0,historyPage=0,busy=false,initialized=false,listGeneration=0,countGeneration=0;
  let activeDetailTab='public',historyLoaded=false,historyLoading=false,historyGeneration=0,activeListTab=0;
  // Nhóm yêu cầu đang xem (Đang theo dõi, Đã giải quyết...) chọn từ mục con ở sidebar, giữ trên đường dẫn (?group=).
  const audience=()=>account.canCreate?'requester':account.canHandle?'cs':'manager';
  const csDetail=()=>isDetail&&account?.canHandle;
  const query=new URLSearchParams(location.search),id=query.get('id');
  const content=$('supportContent');
  const route=(ticketId='')=>`/${role}/support/tickets${ticketId?'/'+ticketId:''}`;
  const api=(path='',opts={})=>fetchAPI(base+path,{csrfPath:base+'/csrf',...opts});
  const counts=createTicketCountsLoader(api);
  /**
   * Trình duyệt dùng chung một phiên đăng nhập cho mọi tab. Nếu ở tab khác vừa đăng nhập tài khoản khác,
   * server sẽ từ chối (403); báo rõ và tải lại trang theo tài khoản hiện tại thay vì báo lỗi khó hiểu.
   */
  async function accountChanged(error) {
    if(error?.status!==403)return false;
    try{const now=await api('/me');if(now.userId===account.userId&&now.viewRole===account.viewRole)return false;
      try{sessionStorage.setItem('kizuna-support-account',JSON.stringify(now));}catch{}
      alert('Trình duyệt này vừa đăng nhập tài khoản khác ('+now.displayName+') ở tab khác. Trang sẽ tải lại theo tài khoản đó. Muốn dùng nhiều tài khoản cùng lúc, hãy mở cửa sổ ẩn danh hoặc trình duyệt khác.');
      location.reload();return true;}catch{return false;}
  }
  function fail(error) {$('supportLoading')?.remove();if(error.status===401){location.assign('/login');return;}const message=error.message||'Chưa tải được yêu cầu hỗ trợ. Vui lòng thử lại.';notice(message,true);showToast(message,'error');$('reloadSupport').hidden=false;}
  async function init() {
    try {
      // Dùng lại tài khoản đã xác minh trong phiên để trang hiện ngay; vẫn hỏi lại server ở nền,
      // nếu tài khoản/vai trò đã đổi (đăng nhập người khác) thì tải lại trang.
      const cacheKey='kizuna-support-account',same=(x,y)=>['userId','viewRole','canCreate','canHandle','canDecide','displayName'].every(k=>x?.[k]===y?.[k]);
      let cached=null;try{cached=JSON.parse(sessionStorage.getItem(cacheKey)||'null');}catch{}
      const fresh=api('/me').then(value=>{try{sessionStorage.setItem(cacheKey,JSON.stringify(value));}catch{}return value;});
      if(cached){fresh.then(value=>{if(!same(value,cached))location.reload();}).catch(error=>{try{sessionStorage.removeItem(cacheKey);}catch{}if(error.status===401)location.assign('/login');});}
      account=cached||await fresh;role=account.viewRole;
      if(query.has('role')&&query.get('role')!==role)throw new Error('Trang này dành cho vai trò khác. Hãy mở mục Yêu cầu hỗ trợ từ menu tài khoản của bạn.');
      if(isDetail&&!/^[0-9a-f-]{36}$/i.test(id||''))throw new Error('Đường dẫn yêu cầu chưa hợp lệ. Vui lòng trở lại danh sách.');
      document.body.dataset.role=role;
      if(!isDetail){const copy=pageCopy[audience()];$('supportTitle').textContent=copy.title;$('supportLead').textContent=copy.lead;if(copy.back&&false){$('supportBack').href=copy.back[0];$('supportBack').textContent=copy.back[1];$('supportBack').hidden=false;}document.title=`${copy.title} - KizunaTutor`;}
      if(role==='learner')mountLearnerFrame('support',account.displayName);else if(account.canCreate)await mountTutorFrame(account.displayName);else {await loadStaffStyles();document.body.classList.add('admin-dashboard','operations-page');if(account.canHandle)document.body.dataset.workspace='cs';$('supportMain').classList.add('dashboard-main');mountAdminShell();}
      initialized=true;
      mountSupportGroups();
      content.hidden=false;
      $('supportLoading')?.remove();
      if(isDetail){$('backToTickets').href=route();if(csDetail())arrangeCsDetail();initDetailTabs();if(!account.canCreate&&query.get('tab')==='internal')selectDetailTab('internal');const mainDetail=loadDetail();loadAttachmentLimit();await mainDetail;}else await initList();
    }catch(error){fail(error);}
    finally {content.setAttribute('aria-busy','false');}
  }
  async function publicShell() {
    document.body.classList.add('support-public-header');
    const [header,footer]=await Promise.all([fetch('/components/header/header.html'),fetch('/components/footer/footer.html')]);
    if(!header.ok||!footer.ok)throw new Error('Chưa tải được điều hướng. Vui lòng thử lại.');
    const fragment=document.createElement('template');fragment.innerHTML=await header.text();document.body.prepend(fragment.content);
    document.querySelectorAll('header [data-path="dang-ky"],header [data-path="dashboard"]').forEach(link=>link.remove());
    const foot=document.createElement('template');foot.innerHTML=await footer.text();document.body.append(foot.content);
    // Chỉ liệt kê mục đã có trang thật; các mục khác của Figma sẽ thêm khi trang tương ứng hoàn thành.
    const bar=element('div',undefined,'support-role-bar'),nav=element('nav',undefined,'support-role-nav');nav.setAttribute('aria-label',role==='learner'?'Điều hướng học viên':'Điều hướng gia sư');
    [...(role==='learner'?[['Tổng quan','/learner/dashboard']]:[]),['Khám phá gia sư','/pages/tutor-discovery/'],...(role==='learner'?[['Kết nối','/pages/connection-requests/']]:[]),['Hỗ trợ',route()],['Chính sách',`/${role}/policies`]].forEach(([label,href])=>{const link=element('a',label);link.href=href;if(href===route())link.setAttribute('aria-current','page');nav.append(link);});
    const who=element('div',undefined,'support-account'),avatar=element('span',account.displayName.replace(/\(.*?\)/g,'').trim().split(/\s+/).slice(-2).map(word=>word[0]).join('').toLocaleUpperCase('vi-VN'),'support-account-avatar'),identity=element('div');avatar.setAttribute('aria-hidden','true');identity.append(element('strong',account.displayName),element('small',role==='learner'?'Học viên':'Gia sư tiếng Nhật'));const signOut=element('button','Đăng xuất');signOut.type='button';signOut.onclick=logout;who.append(avatar,identity,signOut);bar.append(nav,who);$('supportMain').before(bar);
    const mobile=$('mobileMenuBtn');if(mobile){mobile.setAttribute('aria-controls','mobileMenu');mobile.setAttribute('aria-expanded','false');mobile.onclick=()=>{const menu=$('mobileMenu');menu?.classList.toggle('hidden');mobile.setAttribute('aria-expanded',String(!menu?.classList.contains('hidden')));};document.addEventListener('keydown',event=>{if(event.key==='Escape'&&!$('mobileMenu')?.classList.contains('hidden')){$('mobileMenu')?.classList.add('hidden');mobile.setAttribute('aria-expanded','false');mobile.focus();}});}
  }
  async function initList() {
    options($('ticketFilters').elements.category,categories);options($('ticketFilters').elements.priority,priorities);
    for(const [key,choices] of [['category',categories],['priority',priorities]]){const value=query.get(key);if(value&&Object.hasOwn(choices,value))$('ticketFilters').elements[key].value=value;}
    const groups=tabGroups[audience()],requested=query.get('status');
    activeListTab=Math.min(groups.length-1,Math.max(0,Number(query.get('group'))||0));
    if(!query.has('group')&&requested){const found=groups.findIndex(group=>group.statuses.includes(requested));if(found>=0)activeListTab=found;}
    $('ticketListTitle').textContent=groups[activeListTab].label;
    renderStatusFilter(requested);
    // Học viên, gia sư không thấy mức ưu tiên (thông tin nội bộ của nhân viên hỗ trợ).
    $('priorityFilter').hidden=Boolean(account.canCreate);
    $('ticketFilters').onsubmit=event=>{event.preventDefault();loadList(0);};
    // Chọn xong là lọc ngay, không cần bấm nút.
    $('ticketFilters').onchange=()=>{syncClearFilter();loadList(0);loadStatusCounts();};
    $('clearFilters').onclick=()=>{$('ticketFilters').reset();syncClearFilter();loadList(0);loadStatusCounts();};
    if(account.canCreate){$('createTicket').hidden=false;initCreate();buildRequesterHeader();}
    // Gọi song song: danh sách, số lượng từng trạng thái và danh sách nhân viên (cho bộ lọc).
    // Give the primary list the first connection. Counters and secondary controls must not delay its rendering.
    const mainList=loadList(0);
    const secondary=()=>{loadStatusCounts();if(account.canCreate)loadRequesterStats();if(account.canHandle)api('/assignees').then(people=>{$('assigneeFilter').hidden=false;people.forEach(p=>option($('ticketFilters').elements.assignedTo,p.userId,p.displayName));}).catch(()=>{});};
    setTimeout(secondary,0);
    await mainList;
  }
  /**
   * Thêm mục con dưới "Yêu cầu hỗ trợ" ở sidebar (hoặc thanh điều hướng của gia sư): mỗi nhóm một mục,
   * bấm để xem nhóm đó. Dùng chung cho học viên, gia sư, nhân viên hỗ trợ và quản lý.
   */
  function mountSupportGroups() {
    // Nhân viên hỗ trợ và quản lý: khung quản trị (admin-shell) tự dựng menu này.
    if(!account.canCreate)return;
    // Giữ nguyên link gốc (khung quản trị còn đổi tên/đường dẫn của nó sau khi tải), chỉ bọc thêm danh sách mục con.
    const link=document.querySelector('#learnerSidebar a[href="/learner/support/tickets"]')||document.querySelector('#tutorSidebar a[href="/tutor/support/tickets"]')||document.querySelector('.support-role-nav a[href="'+route()+'"]')||document.querySelector('[data-support-link]');
    if(!link||link.closest('.support-nav-group'))return;
    const current=Math.max(0,Number(query.get('group'))||0);
    const wrap=element('div',undefined,'support-nav-group'),list=element('div',undefined,'support-nav-items');
    list.setAttribute('aria-label','Nhóm yêu cầu hỗ trợ');
    tabGroups[audience()].forEach((item,index)=>{
      const sub=element('a',item.label);sub.href=route()+'?group='+index;
      if(!isDetail&&index===current)sub.setAttribute('aria-current','page');
      list.append(sub);
    });
    link.replaceWith(wrap);wrap.append(link,list);
    // Mục con đóng sẵn; bấm "Yêu cầu hỗ trợ" mới mở ra. Trạng thái mở được nhớ khi chuyển giữa các nhóm.
    if(wrap.closest('.support-role-nav'))return;
    const key='kizuna-support-nav-open',setOpen=open=>{wrap.classList.toggle('is-open',open);link.setAttribute('aria-expanded',String(open));try{sessionStorage.setItem(key,open?'1':'0');}catch{}};
    let remembered=false;try{remembered=sessionStorage.getItem(key)==='1';}catch{}
    wrap.classList.add('is-collapsible');link.setAttribute('aria-controls',list.id='supportNavItems');setOpen(remembered);
    link.addEventListener('click',event=>{if(event.ctrlKey||event.metaKey||event.shiftKey||event.button)return;event.preventDefault();setOpen(!wrap.classList.contains('is-open'));});
  }
  /**
   * Trạng thái là hàng nút bấm ngay trên danh sách: "Tất cả" và từng trạng thái của nhóm đang xem,
   * mỗi nút kèm số yêu cầu để biết bấm vào sẽ thấy bao nhiêu.
   */
  let statusChoice='';
  const statusLabel=(status,fallback)=>(account?.canCreate?requesterStatuses[status]:statuses[status])||fallback;
  /** Nút lọc của nhóm: trạng thái nào hiển thị cùng một chữ thì gộp lại (học viên: 3 trạng thái đang xử lý thành 1). */
  function statusChoices(group) {
    const byLabel=new Map();
    group.statuses.forEach(status=>{const label=statusLabel(status,status);byLabel.set(label,[...(byLabel.get(label)||[]),status]);});
    return [...byLabel].map(([label,list])=>[list.join(','),label]);
  }
  function renderStatusFilter(selected) {
    const group=tabGroups[audience()][activeListTab];
    const choices=statusChoices(group);
    statusChoice=choices.find(([value])=>value.split(',').includes(selected))?.[0]||'';
    const bar=$('statusChips');bar.replaceChildren();bar.hidden=choices.length<2;
    [['',  'Tất cả'],...choices].forEach(([value,label])=>{
      const chip=element('button',undefined,'support-chip');chip.type='button';chip.dataset.status=value;
      chip.append(element('span',label),element('span','','support-chip-count'));
      chip.setAttribute('aria-pressed',String(value===statusChoice));
      chip.onclick=()=>{statusChoice=value;bar.querySelectorAll('.support-chip').forEach(item=>item.setAttribute('aria-pressed',String(item===chip)));syncClearFilter();loadList(0);};
      bar.append(chip);
    });
    syncClearFilter();
  }
  /** Đếm số yêu cầu của từng nút trạng thái (theo bộ lọc nhóm yêu cầu đang chọn). */
  async function loadStatusCounts() {
    const generation=++countGeneration;
    const group=tabGroups[audience()][activeListTab],extra=new URLSearchParams();
    for(const [key,value] of new FormData($('ticketFilters')))if(value)extra.set(key,value);
    if($('statusChips').hidden)return;
    const chips=[...$('statusChips').querySelectorAll('.support-chip')];
    try{const data=await counts(extra);if(generation!==countGeneration)return;chips.forEach(chip=>{chip.querySelector('.support-chip-count').textContent=countTicketStatuses(data,(chip.dataset.status||group.statuses.join(',')).split(','));});}
    catch{if(generation===countGeneration)chips.forEach(chip=>{chip.querySelector('.support-chip-count').textContent='';});}
  }
  function statusParam() {
    return statusChoice||tabGroups[audience()][activeListTab].statuses.join(',');
  }
  function syncClearFilter() {
    const form=$('ticketFilters').elements;
    $('clearFilters').hidden=!form.category.value&&!form.priority.value&&!form.assignedTo?.value;
  }
  /** Đầu trang học viên/gia sư: tiêu đề bên trái, nút "Gửi yêu cầu hỗ trợ" bên phải. */
  function buildRequesterHeader() {
    const intro=document.querySelector('.support-intro');if(!intro||intro.parentElement.classList.contains('support-hero'))return;
    const hero=element('section',undefined,'support-hero'),button=$('createTicket');
    intro.before(hero);hero.append(intro,button);button.prepend(icon('add'));
    const stats=element('div',undefined,'support-stats');stats.id='requesterStats';$('supportContent').prepend(stats);
  }
  /** Ba thẻ số liệu: đang xử lý, chờ bạn xác nhận, đã đóng; bấm để mở đúng nhóm. */
  async function loadRequesterStats() {
    const box=$('requesterStats');if(!box)return;
    const cards=[['Đang xử lý','OPEN,IN_PROGRESS,ESCALATED','support_agent','?group=0','progress'],['Chờ bạn xác nhận','RESOLVED','task_alt','?group=1&status=RESOLVED','confirm'],['Đã đóng','CLOSED','inventory_2','?group=1&status=CLOSED','closed']];
    box.replaceChildren(...cards.map(([label,,symbol,link,tone])=>{const card=element('a',undefined,'support-stat');card.href=route()+link;card.dataset.tone=tone;const text=element('div');text.append(element('span',label),element('strong','—'));card.append(text,icon(symbol,'support-stat-icon'));return card;}));
    try{const data=await counts();cards.forEach(([,status],index)=>{box.children[index].querySelector('strong').textContent=countTicketStatuses(data,status.split(','));});}
    catch{[...box.children].forEach(card=>{card.querySelector('strong').textContent='—';});}
  }
  function ticketRow(t) {
    const row=element('article',undefined,'support-row support-row-card'),info=element('div',undefined,'support-row-info'),link=element('a',t.subject,'support-row-title'),status=element('div'),open=element('span','›','support-row-arrow');
    link.href=route(t.ticketId);open.setAttribute('aria-hidden','true');status.append(badge(statusLabel(t.status,t.statusLabel),t.status));
    // Bấm vào bất kỳ đâu trên thẻ cũng mở yêu cầu (trừ khi đang bôi đen chữ).
    row.onclick=event=>{if(event.target.closest('a')||String(getSelection()))return;location.assign(link.href);};
    const meta=account.canCreate?`${t.categoryLabel} · Gửi ${date(t.createdAt)} · Cập nhật ${date(t.updatedAt)}`:account.canHandle?`${t.requesterDisplayName} · ${t.categoryLabel} · Ưu tiên ${t.priorityLabel.toLocaleLowerCase('vi-VN')} · ${t.assigneeDisplayName||'Chưa phân công'}`:`${t.requesterDisplayName} · Nhân viên hỗ trợ ${t.assigneeDisplayName||'chưa phân công'} · Cập nhật ${date(t.updatedAt)}`;
    info.append(link,element('p',meta));if(t.descriptionPreview)info.append(element('p',t.descriptionPreview,'support-preview'));
    if(account.canCreate){row.classList.add('support-row-icon');row.append(icon(categoryIcons[t.category]||'help','support-row-symbol'));}
    row.append(info,status,open);return row;
  }
  async function loadList(next=page) {
    const generation=++listGeneration;
    page=next;content.setAttribute('aria-busy','true');notice('Đang tải yêu cầu…');
    try {const params=new URLSearchParams({page:String(page),size:'20'});for(const [key,value] of new FormData($('ticketFilters')))if(value&&key!=='status')params.set(key,value);
      params.set('status',statusParam());
      const data=await api('?'+params);if(generation!==listGeneration)return;const target=$('ticketList');
      if(!data.items.length){empty(target,pageCopy[audience()].hint,pageCopy[audience()].empty);if(account.canCreate){const cta=element('button',undefined,'primary');cta.type='button';cta.append(icon('add'),document.createTextNode('Gửi yêu cầu hỗ trợ'));cta.onclick=()=>$('createTicket').click();target.querySelector('.support-empty').append(cta);}}
      else {const rows=[];if(account.canDecide){const head=element('div',undefined,'support-row-head');['Thông tin','Trạng thái','Thao tác'].forEach(label=>head.append(element('span',label)));rows.push(head);}rows.push(...data.items.map(ticketRow));target.replaceChildren(...rows);}
      paging($('ticketPaging'),data,loadList);$('ticketCount').textContent=data.totalElements+' yêu cầu';notice('');$('reloadSupport').hidden=true;
    }catch(error){if(generation===listGeneration)fail(error);}finally{if(generation===listGeneration)content.setAttribute('aria-busy','false');}
  }
  function initCreate() {
    const dialog=$('createDialog'),form=$('createForm');options(form.elements.category,categories);
    $('createTicket').onclick=()=>{loadAttachmentLimit();notice('',false,'createNotice');dialog.showModal();form.elements.subject.focus({preventScroll:true});dialog.scrollTop=0;};$('cancelCreate').onclick=$('cancelCreateFooter').onclick=()=>dialog.close();
    dialog.addEventListener('close',()=>$('createTicket').focus());
    let contextPage=0,contextItems=[];
    async function loadContexts(append=false){const type=$('contextType').value;const select=$('contextChoice');$('contextChoiceLabel').hidden=!type;$('moreContexts').hidden=true;
      const names={booking:'buổi học thử',membership:'gói hội viên',payment:'lần thanh toán'};
      if(names[type])$('contextChoiceText').textContent='Chọn '+names[type];
      if(!append){contextPage=0;contextItems=[];select.replaceChildren();option(select,'',names[type]?'Chọn '+names[type]:'Chọn mục');}
      if(!type){$('contextNotice').textContent='';return;}select.disabled=true;$('contextNotice').textContent='Đang tải thông tin của bạn…';
      try {const data=await api(`/related-contexts?type=${type}&page=${contextPage}`);if($('contextType').value!==type)return;contextItems.push(...data.items);data.items.forEach((item,index)=>option(select,item.contextId,`${item.label} · Lựa chọn ${contextPage*20+index+1}`));$('contextNotice').textContent=data.totalElements?'':'Bạn chưa có mục nào thuộc loại này. Bạn vẫn gửi yêu cầu bình thường được.';$('moreContexts').hidden=contextPage+1>=data.totalPages;}catch(e){if(await accountChanged(e))return;$('contextNotice').textContent=e.message;}finally{select.disabled=false;}}
    $('contextType').onchange=()=>loadContexts();$('moreContexts').onclick=()=>{contextPage++;loadContexts(true);};
    form.onsubmit=async event=>{event.preventDefault();const submit=form.querySelector('[type=submit]');submit.disabled=true;try{const payload={subject:validText(form.elements.subject.value,200),category:form.elements.category.value,message:validText(form.elements.message.value,10000)};
        const type=$('contextType').value,contextId=$('contextChoice').value;if(type&&contextId){const keys={booking:'relatedBookingId',membership:'relatedMembershipId',payment:'relatedTransactionId'};payload[keys[type]]=contextId;}
        await loadAttachmentLimit();const created=await fetchMultipartAPI(base,payload,validFiles(form.elements.files));location.assign(route(created.ticketId));
      }catch(error){if(await accountChanged(error))return;notice(error.message,true,'createNotice');}finally{submit.disabled=false;}};
  }
  function initDetailTabs() {
    if(account.canCreate){$('publicPanel').removeAttribute('role');$('publicPanel').removeAttribute('tabindex');return;}
    $('detailTabs').hidden=false;
    for(const name of ['public','internal']){
      const tab=$(name+'Tab');tab.onclick=()=>selectDetailTab(name);
      tab.onkeydown=event=>{if(!['ArrowLeft','ArrowRight','Home','End'].includes(event.key))return;event.preventDefault();const next=event.key==='Home'?'public':event.key==='End'?'internal':name==='public'?'internal':'public';selectDetailTab(next);$(next+'Tab').focus();};
    }
    $('historyDetails').ontoggle=()=>{if($('historyDetails').open&&activeDetailTab==='internal')ensureHistory();};
  }
  function selectDetailTab(name) {
    activeDetailTab=name;
    for(const tabName of ['public','internal']){const selected=tabName===name;$(tabName+'Tab').setAttribute('aria-selected',String(selected));$(tabName+'Tab').tabIndex=selected?0:-1;$(tabName+'Panel').hidden=!selected;}
    if(name==='internal'&&$('historyDetails').open)ensureHistory();
  }
  function ensureHistory(){if(!historyLoaded&&!historyLoading)loadHistory(historyPage);}
  function drafts() {return [...document.querySelectorAll('#replyArea form,#ticketActions form,#internalActions form,#csInformationControls form')].map(form=>({key:form.dataset.action,values:[...form.elements].filter(e=>e.name&&e.type!=='file').map(e=>[e.name,e.value])}));}
  async function loadDetail(preserve=false) {
    const saved=preserve?drafts():[];content.setAttribute('aria-busy','true');notice('Đang tải nội dung yêu cầu…');
    try {ticket=await api('/'+id);renderDetail();renderInternalCurrent();await Promise.all([loadThread(threadPage),renderActions()]);
      if(!account.canCreate&&activeDetailTab==='internal'&&$('historyDetails').open)ensureHistory();
      saved.forEach(d=>{const form=[...document.querySelectorAll('form[data-action]')].find(f=>f.dataset.action===d.key);d.values.forEach(([key,value])=>{if(form?.elements[key])form.elements[key].value=value;});});
      notice(preserve?'Đã tải bản mới nhất và giữ nội dung đang soạn. Vui lòng chọn lại tệp nếu cần.':'Đã tải nội dung yêu cầu.');$('reloadSupport').hidden=true;return true;
    }catch(error){fail(error);return false;}finally{content.setAttribute('aria-busy','false');}
  }
  function renderDetail() {
    document.querySelector('#supportMain h1').textContent=ticket.subject;
    if(csDetail()){renderCsHeader(ticket);renderCsInformation(ticket);return;}
    const meta=element('p',undefined,'support-ticket-meta');
    if(account.canCreate){
      meta.append(badge(statusLabel(ticket.status,ticket.statusLabel),ticket.status));
      const category=element('span',undefined,'support-meta-chip');category.append(icon(categoryIcons[ticket.category]||'help'),document.createTextNode(ticket.categoryLabel));
      const sent=element('span',undefined,'support-meta-chip');sent.append(icon('schedule'),document.createTextNode('Gửi '+date(ticket.createdAt)));
      meta.append(category,sent);
      const head=document.querySelector('.support-sheet-head');if(head&&!head.querySelector('.support-head-symbol'))head.querySelector('h1').before(icon(categoryIcons[ticket.category]||'help','support-head-symbol'));
    } else meta.append(badge(statusLabel(ticket.status,ticket.statusLabel),ticket.status),` · ${ticket.requesterDisplayName} · Nhân viên hỗ trợ ${ticket.assigneeDisplayName||'chưa phân công'} · ${ticket.categoryLabel}`);
    const parts=[meta];
    if(ticket.relatedContext?.length){const contexts=element('div',undefined,'support-context');ticket.relatedContext.forEach(c=>contexts.append(element('p',(c.available?'Liên quan: ':'')+c.label)));parts.push(contexts);}
    $('ticketDetail').replaceChildren(...parts);
  }
  async function loadThread(next=0) {
    threadPage=next;const data=await api(`/${id}/messages?page=${next}`);const target=$('ticketThread');target.replaceChildren();if(!data.items.length)empty(target,'Chưa có trao đổi.');
    if(csDetail()){renderCsThread(target,data.items);paging($('threadPaging'),data,page=>loadThread(page).catch(fail));return;}
    data.items.forEach(m=>{const article=element('article',undefined,'support-message'+(account.canCreate&&m.fromRequester?' support-message-mine':' support-message-staff')),heading=element('div',undefined,'support-message-heading');const time=element('time',date(m.sentAt));if(m.sentAt)time.dateTime=new Date(m.sentAt).toISOString();const author=element('div');author.append(element('h3',m.fromRequester?(account.canCreate?'Bạn':m.senderDisplayName):(account.canCreate?'Nhân viên hỗ trợ':m.senderRoleLabel)));if(!m.fromRequester)author.append(element('small',m.senderDisplayName));heading.append(author,time);article.append(heading,element('p',m.message,'support-message-text'));m.attachments.forEach(f=>{const link=element('a',`${f.originalName} · ${{'image/jpeg':'Ảnh JPG','image/png':'Ảnh PNG','application/pdf':'Tài liệu PDF'}[f.contentType]||'Tệp đính kèm'} · ${(f.sizeBytes/1000000).toLocaleString('vi-VN',{maximumFractionDigits:2})} MB`);link.href=API_CONFIG.BASE_URL.replace(/\/api\/?$/,'')+f.downloadPath;link.target='_blank';link.rel='noopener';article.append(link);});target.append(article);});paging($('threadPaging'),data,next=>loadThread(next).catch(fail));
  }
  async function perform(action,payload,form,multipart=false,selectedFiles) {
    if(busy)return;if(action==='close'&&!confirm('Bạn xác nhận vấn đề đã được giải quyết và muốn đóng yêu cầu?'))return;
    busy=true;const controls=[...document.querySelectorAll(csDetail()?'form button,form input,form textarea,form select':'form button')].map(control=>[control,control.disabled]);controls.forEach(([control])=>control.disabled=true);
    const submit=form.querySelector('[type=submit]'),submitText=submit?.textContent;if(csDetail()){form.setAttribute('aria-busy','true');if(submit)submit.textContent=action==='messages'||action==='resolve'?'Đang gửi…':'Đang xử lý…';if(action==='messages')$('csReplyNotice').textContent='';}
    try {payload.expectedVersion=ticket.version;
      const endpoint={escalate:'escalations',recipient:'escalations/current/recipient',decision:'escalations/current/decision',return:'escalations/current/return'}[action]||action;
      if(multipart)await fetchMultipartAPI(`${base}/${id}/messages`,payload,selectedFiles??validFiles(form.elements.files));else await api(`/${id}/${endpoint}`,{method:'POST',body:payload});
      document.querySelectorAll('dialog[open]').forEach(dialog=>dialog.close());
      if(await loadDetail()){const done=csDetail()&&action==='messages'?'Đã gửi phản hồi.':csDetail()&&action==='resolve'?'Đã gửi kết quả và đánh dấu đã giải quyết.':'Đã lưu thay đổi.';notice(done);showToast(done,'success');}else notice('Đã lưu thay đổi, nhưng chưa tải được bản mới. Vui lòng tải lại dữ liệu trước khi tiếp tục.',true);
    }catch(error){fail(error);const dialogError=form.closest('dialog')?.querySelector('.support-dialog-error');if(dialogError){dialogError.textContent=error.message;dialogError.focus();}if(csDetail()&&action==='messages'){$('csReplyNotice').textContent=error.message;$('csReplyNotice').focus();}}
    finally{busy=false;controls.forEach(([control,disabled])=>{if(control.isConnected)control.disabled=disabled;});if(submit?.isConnected)submit.textContent=submitText;form.setAttribute('aria-busy','false');}
  }
  function commandForm(action,title,buttonLabel,fields=[],choices={}) {
    const card=element('section',undefined,'support-card support-action-card'),form=element('form',undefined,'support-form');form.dataset.action=action;card.append(element('h3',title));
    fields.forEach(([name,label,kind])=>field(form,label,name,kind,choices[name]));const submit=element('button',buttonLabel,['claim','return'].includes(action)?'primary':undefined);submit.type='submit';form.append(submit);card.append(form);
    form.onsubmit=event=>{event.preventDefault();try{const payload={};fields.forEach(([name,,kind])=>payload[name]=kind==='textarea'?validText(form.elements[name].value,10000):form.elements[name].value);perform(action,payload,form);}catch(error){fail(error);}};
    return card;
  }
  async function renderActions() {
    $('replyArea').replaceChildren();$('ticketActions').replaceChildren();$('internalActions').replaceChildren();const actions=ticket.allowedActions;
    if(csDetail()){$('csInformationControls').replaceChildren();$('csResolveBody').replaceChildren();}
    if(actions.includes('message')&&csDetail()){$('replyArea').append(createCsReplyForm((form,files)=>{try{perform('messages',{message:validText(form.elements.message.value,10000)},form,true,files);}catch(error){$('csReplyNotice').textContent=error.message;$('csReplyNotice').focus();}}));}
    else if(actions.includes('message')){
      // Ô trả lời giống khung chat: gõ nội dung, bấm "Đính kèm" để chọn ảnh/PDF, bấm "Gửi".
      const form=createCsReplyForm((replyForm,files)=>{try{perform('messages',{message:validText(replyForm.elements.message.value,10000)},replyForm,true,files);}catch(error){$('csReplyNotice').textContent=error.message;$('csReplyNotice').focus();}},
        {label:account.canCreate?'Trả lời nhân viên hỗ trợ':'Phản hồi',help:'',button:'Gửi',placeholder:account.canCreate?'Nhập câu trả lời hoặc thông tin bổ sung…':'Nhập phản hồi…'});
      form.classList.add('support-reply');$('replyArea').append(form);}
    else $('replyArea').append(element('p',ticket.status==='CLOSED'?(account.canCreate?'Yêu cầu đã đóng, không thể thêm trao đổi. Nếu vẫn còn vấn đề, vui lòng tạo yêu cầu mới.':'Yêu cầu đã đóng, không thể thêm trao đổi.'):account.canDecide?'Mở tab “Xử lý nội bộ” để ghi quyết định và trả kết quả cho nhân viên.':actions.includes('claim')?'Tiếp nhận yêu cầu để bắt đầu phản hồi người gửi.':'Chỉ người phụ trách hiện tại được gửi phản hồi.'));
    const target=$('internalActions'),publicActions=$('ticketActions');
    const informationActions=csDetail()?$('csInformationControls'):target;
    if(actions.includes('claim'))publicActions.append(commandForm('claim','Tiếp nhận yêu cầu','Xác nhận tiếp nhận'));
    if(actions.includes('assignment')){const people=await api('/assignees');informationActions.append(commandForm('assignment','Đổi người phụ trách','Chuyển nhân viên',[['assigneeId','Nhân viên tiếp nhận','select']],{assigneeId:Object.fromEntries(people.filter(p=>p.userId!==ticket.assigneeId).map(p=>[p.userId,p.displayName]))}));}
    if(actions.includes('priority')){const card=commandForm('priority','Mức ưu tiên','Cập nhật ưu tiên',[['priority','Ưu tiên','select']],{priority:priorities});card.querySelector('select').value=ticket.priority;informationActions.append(card);}
    if(actions.includes('resolve'))publicActions.append(commandForm('resolve','Kết quả xử lý','Gửi kết quả hỗ trợ',[['resolutionText','Phản hồi tới người dùng','textarea']]));
    if(actions.includes('close')){const card=commandForm('close','Xác nhận hoàn tất','Xác nhận đóng yêu cầu');card.prepend(element('p','Nếu cần hỗ trợ thêm, bạn có thể gửi bổ sung để mở lại yêu cầu.'));publicActions.append(card);}
    if(actions.includes('escalate')||actions.includes('recipient')){const people=await api('/managers'),choices={managerId:Object.fromEntries(people.filter(p=>p.userId!==ticket.recipientId||actions.includes('escalate')).map(p=>[p.userId,p.displayName]))};
      if(actions.includes('escalate'))target.append(commandForm('escalate','Chuyển yêu cầu tới Quản lý','Chuyển yêu cầu',[['managerId','Quản lý tiếp nhận','select'],['reason','Lý do chuyển cấp (nội bộ)','textarea'],['handlingSummary','Tóm tắt đã xử lý (nội bộ)','textarea']],choices));
      if(actions.includes('recipient'))target.append(commandForm('recipient','Đổi quản lý tiếp nhận','Đổi quản lý',[['managerId','Quản lý mới','select'],['reason','Lý do đổi người nhận (nội bộ)','textarea']],choices));}
    if(actions.includes('decision')){const card=commandForm('decision','Quyết định nội bộ','Lưu quyết định',[['decisionText','Kết quả gửi cho Nhân viên hỗ trợ','textarea']]);card.querySelector('textarea').value=ticket.escalation?.decisionText||'';target.append(card);}
    if(actions.includes('return')&&ticket.escalation?.decisionText)target.append(commandForm('return','Trả kết quả cho nhân viên','Gửi kết quả cho Nhân viên hỗ trợ'));
    $('publicLayout').classList.toggle('support-detail-single',!publicActions.childElementCount);
    $('internalLayout').classList.toggle('support-detail-single',!target.childElementCount);
    if(csDetail())finishCsActions(ticket);
  }
  function renderInternalCurrent() {
    if(account.canCreate)return;
    historyGeneration++;historyLoaded=false;historyLoading=false;$('historyList').replaceChildren();$('historyPaging').replaceChildren();$('historyNotice').textContent='Mở lịch sử để xem các bước trước.';
    const section=element('section',undefined,'support-card support-internal support-current');section.append(element('h2','Tình trạng xử lý hiện tại'),badge(ticket.statusLabel,ticket.status));
    section.append(element('p',`Người phụ trách: ${ticket.assigneeDisplayName||'Chưa phân công'}`));
    if(ticket.recipientDisplayName)section.append(element('p',`Quản lý tiếp nhận: ${ticket.recipientDisplayName}`));
    if(ticket.escalation){const e=ticket.escalation;section.append(element('h3',e.pending?'Lần chuyển cấp hiện tại':'Kết quả chuyển cấp gần nhất'));[['Lý do chuyển cấp',e.reason],['Tóm tắt đã xử lý',e.handlingSummary],['Quyết định của quản lý',e.decisionText||'Chưa có quyết định cho người nhận hiện tại']].forEach(([label,value])=>section.append(element('h3',label),element('p',value,'support-preserve')));}
    else section.append(element('p','Yêu cầu chưa được chuyển cấp. Các bước đã thực hiện nằm trong lịch sử xử lý.'));
    $('internalArea').replaceChildren(section);
  }
  async function loadHistory(next=0) {
    if(account.canCreate||historyLoading)return;
    const generation=historyGeneration;historyLoading=true;$('historyNotice').textContent='Đang tải lịch sử…';$('historyNotice').dataset.error='false';$('historyList').setAttribute('aria-busy','true');$('historyPaging').querySelectorAll('button').forEach(button=>button.disabled=true);
    try{const data=await api(`/${id}/history?page=${next}`);if(generation!==historyGeneration)return;historyPage=next;const target=$('historyList');target.replaceChildren();
      if(!data.items.length)empty(target,'Chưa có lịch sử xử lý.');
      data.items.forEach(item=>{const row=element('article',undefined,'support-message');row.append(element('strong',events[item.eventType]||'Cập nhật yêu cầu'),element('p',`${item.actorDisplayName} · ${date(item.occurredAt)}`));for(const change of item.changes||[])row.append(element('p',`${change.label}: ${change.before} → ${change.after}`));if(item.note)row.append(element('p',item.note,'support-preserve'));target.append(row);});
      paging($('historyPaging'),data,loadHistory);historyLoaded=true;$('historyNotice').textContent=`Đã tải ${data.items.length} trong ${data.totalElements} bước xử lý.`;
    }catch(error){if(generation!==historyGeneration)return;$('historyNotice').textContent=error.message;$('historyNotice').dataset.error='true';const retry=element('button','Thử tải lại lịch sử');retry.type='button';retry.onclick=()=>loadHistory(next);$('historyList').replaceChildren(retry);$('historyPaging').replaceChildren();}
    finally{if(generation===historyGeneration){historyLoading=false;$('historyList').setAttribute('aria-busy','false');}}
  }
  $('reloadSupport').onclick=()=>{if(!initialized)init();else if(isDetail)loadDetail(true);else loadList(page);};
  await init();
}
