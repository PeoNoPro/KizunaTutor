/** Khung quản trị dùng chung cho dashboard và các trang vận hành FE-11. */
import { fetchAPI } from '../../js/api.js';
// Sidebar quản trị: mỗi nhóm chức năng là một mục thả xuống; mục con giữ đường dẫn riêng để mở trực tiếp.
// [tên mục, đường dẫn, mô tả ngắn hiện dưới tiêu đề trang, chỉ dành cho Quản lý]
const ADMIN_NAV = [
  {label: 'Tổng quan', href: '/pages/admin-dashboard/'},
  {label: 'Quản lý nhân viên', items: [
    ['Danh sách nhân viên', '/pages/admin-staff/', 'Tạo, sửa, khóa tài khoản nhân viên và duyệt đề xuất thăng chức.'],
    ['Đề xuất thăng chức', '/pages/admin-roles/', 'Duyệt hoặc từ chối đề xuất thăng chức do Quản lý gửi.'],
    ['Đề xuất đổi vai trò', '/pages/manager-staff-roles/', 'Đề xuất thăng chức hoặc chuyển vai trò cho nhân viên do bạn quản lý.', true],
    ['Xếp lịch làm việc CS', '/pages/support-shifts/', 'Xếp nhân viên hỗ trợ vào ca làm; yêu cầu mới tự giao cho người đang trong ca.', true]]},
  {label: 'Vận hành hệ thống', items: [
    ['Sự cố kỹ thuật', '/pages/operations-incidents/', 'Ghi nhận, nhận xử lý và theo dõi các sự cố kỹ thuật.'],
    ['Nhật ký thao tác', '/pages/operations-logs/', 'Ai đã thay đổi gì trong khu quản trị và vào lúc nào.'],
    ['Cấu hình hệ thống', '/pages/operations-config/', 'AI hỗ trợ học tập, gói hội viên, email gửi thư cho nhân viên và giới hạn tệp đính kèm.']]},
  {label: 'Thống kê và báo cáo', items: [
    ['Báo cáo sự cố', '/pages/operations-reports/', 'Danh sách sự cố phát sinh, đã khắc phục hoặc chưa đóng trong kỳ.'],
    ['Báo cáo tổng hợp', '/pages/manager-reports/', 'Tài khoản, kết nối gia sư và học thử trong kỳ.', true],
    ['Doanh thu', '/pages/operations-revenue/', 'Tiền gia sư trả khi mua gói hội viên: tổng doanh thu, từng giao dịch và tải báo cáo.']]}
];
const PAGE_ALIASES = {'/pages/operations-incident/': '/pages/operations-incidents/', '/pages/support-ticket/': '/pages/support-tickets/'};
const escapeHtml = value => value.replace(/[&<>"]/g, c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;'})[c]);
function adminNavigation() {
  return ADMIN_NAV.map(entry => entry.href
    ? `<a href="${entry.href}">${escapeHtml(entry.label)}</a>`
    : `<details class="nav-group"><summary>${escapeHtml(entry.label)}</summary><div class="nav-group-items">${entry.items.map(([label, href, , managerOnly]) => `<a href="${href}"${managerOnly ? ' data-manager-only hidden' : ''}>${escapeHtml(label)}</a>`).join('')}</div></details>`).join('')
    + '<a href="/pages/support-tickets/" data-support-link hidden>Yêu cầu hỗ trợ</a>';
}
/** Tiêu đề và mô tả trang lấy theo tên mục trong sidebar để hai nơi gọi cùng một tên. */
function decoratePortalPage(path) {
  const heading = document.querySelector('.operations-page-heading');
  const item = ADMIN_NAV.flatMap(entry => entry.items || []).find(([, href]) => href === path);
  if (!heading || !item) return;
  heading.querySelector('h1').textContent = item[0];
  const lead = heading.querySelector('p'); if (lead) lead.textContent = item[2];
  document.title = item[0] + ' - KizunaTutor';
}
/** Nhóm yêu cầu hỗ trợ hiện thành mục con dưới "Yêu cầu hỗ trợ" (cùng thứ tự với trang danh sách, tham số ?group=). */
const SUPPORT_GROUPS = {cs: ['Cần xử lý', 'Chờ người dùng xác nhận', 'Đã chuyển Quản lý', 'Đã đóng'], manager: ['Chờ xem xét', 'Đã phản hồi']};
/** Bọc link "Yêu cầu hỗ trợ" cùng danh sách nhóm; đóng sẵn, bấm mới mở, nhớ trạng thái mở khi chuyển trang. */
function attachSupportGroups(link, labels, base) {
  if (!link || link.closest('.support-nav-group')) return;
  const params = new URLSearchParams(location.search);
  const onList = location.pathname === '/pages/support-tickets/', current = Math.max(0, Number(params.get('group')) || 0);
  const wrap = document.createElement('div'), list = document.createElement('div');
  wrap.className = 'support-nav-group is-collapsible'; list.className = 'support-nav-items'; list.id = 'supportNavItems';
  labels.forEach((label, index) => {
    const sub = document.createElement('a'); sub.href = base + '?group=' + index; sub.textContent = label;
    if (onList && index === current) sub.setAttribute('aria-current', 'page');
    list.append(sub);
  });
  link.replaceWith(wrap); wrap.append(link, list);
  const key = 'kizuna-support-nav-open';
  const setOpen = open => { wrap.classList.toggle('is-open', open); link.setAttribute('aria-expanded', String(open)); try { sessionStorage.setItem(key, open ? '1' : '0'); } catch { /* Không lưu được thì chỉ không nhớ trạng thái. */ } };
  let remembered = false; try { remembered = sessionStorage.getItem(key) === '1'; } catch { /* Bỏ qua. */ }
  link.setAttribute('aria-controls', list.id); setOpen(remembered);
  link.addEventListener('click', event => { if (event.ctrlKey || event.metaKey || event.shiftKey || event.button) return; event.preventDefault(); setOpen(!wrap.classList.contains('is-open')); });
}
/** Xóa cookie phiên ở backend và dữ liệu phiên trên trình duyệt rồi về trang đăng nhập. */
export async function logout() {
  try { await fetch('/api/auth/logout', {method: 'POST', credentials: 'include'}); } catch { /* Vẫn xóa phiên phía trình duyệt khi mất kết nối. */ }
  ['kizuna_token', 'kizuna_user'].forEach(key => localStorage.removeItem(key));
  sessionStorage.clear();
  location.assign('/login');
}
export function mountAdminShell() {
  if (!document.body.classList.contains('admin-dashboard') || document.getElementById('adminSidebar')) return;
  const csWorkspace=document.body.dataset.workspace==='cs';
  if(csWorkspace){document.body.classList.add('cs-workspace');if(!document.querySelector('link[data-cs-workspace]')){const css=document.createElement('link');css.rel='stylesheet';css.href='/styles/cs-workspace.css';css.dataset.csWorkspace='';document.head.append(css);}}
  const shell = document.createElement('template');
  shell.innerHTML = `  <aside class="admin-sidebar" id="adminSidebar" aria-label="Điều hướng quản trị">
    <a class="admin-brand" href="/pages/home/">
      <img src="/assets/logos/kizuna-logo.png" alt="KizunaTutor" width="36" height="36">
      <span><strong>KizunaTutor</strong><small>Cổng quản trị</small></span>
    </a>
    <div class="sidebar-menu">
      <p class="nav-caption">Hệ thống quản trị</p>
      <nav>
        <a href="/pages/admin-dashboard/"><img src="/assets/icons/admin-dashboard/nav-overview.svg" alt="">Tổng quan hệ thống</a>
        <a href="/pages/admin-staff/"><img src="/assets/icons/admin-dashboard/nav-users.svg" alt="">Quản lý tài khoản nhân viên</a>
        <a href="/pages/admin-roles/"><img src="/assets/icons/admin-dashboard/nav-roles.svg" alt="">Quản lý vai trò và quyền hạn</a>
        <a href="/pages/operations-logs/"><img src="/assets/icons/admin-dashboard/nav-logs.svg" alt="">Nhật ký hệ thống</a>
        <a href="/pages/operations-config/"><img src="/assets/icons/admin-dashboard/nav-settings.svg" alt="">Cấu hình hệ thống</a>
        <a href="/pages/operations-statistics/"><img src="/assets/icons/admin-dashboard/nav-statistics.svg" alt="">Thống kê hệ thống</a>
        <a href="/pages/operations-reports/"><img src="/assets/icons/admin-dashboard/nav-reports.svg" alt="">Báo cáo và xuất dữ liệu</a>
        <a href="/pages/manager-reports/"><img src="/assets/icons/admin-dashboard/nav-statistics.svg" alt="">Báo cáo tổng hợp</a>
        <a href="/pages/support-tickets/" data-support-link hidden><img src="/assets/icons/admin-dashboard/nav-incidents.svg" alt="">Yêu cầu hỗ trợ</a>
        <button type="button" disabled title="Chức năng chưa được tích hợp"><img src="/assets/icons/admin-dashboard/nav-notifications.svg" alt="">Quản lý thông báo</button>
        <a href="/pages/operations-incidents/"><img src="/assets/icons/admin-dashboard/nav-incidents.svg" alt="">Quản lý sự cố kỹ thuật</a>
        <button type="button" disabled title="Nội dung chính sách dành cho học viên và gia sư"><img src="/assets/icons/admin-dashboard/nav-policies.svg" alt="">Điều khoản và chính sách</button>
      </nav>
    </div>
    <div class="sidebar-bottom">
      <a href="/pages/admin-profile/"><img src="/assets/icons/admin-dashboard/nav-profile.svg" alt="">Hồ sơ cá nhân</a>
      <button type="button" class="logout-button"><img src="/assets/icons/admin-dashboard/nav-logout.svg" alt="">Đăng xuất</button>
    </div>
  </aside>
  <button class="sidebar-backdrop" id="sidebarBackdrop" type="button" aria-label="Đóng điều hướng" hidden></button>
  <header class="admin-topbar">
    <div class="topbar-breadcrumb"><button type="button" id="sidebarToggle" class="menu-toggle" aria-label="Mở điều hướng" aria-controls="adminSidebar" aria-expanded="false">☰</button><span>Quản trị hệ thống</span><span aria-hidden="true">›</span><strong>Cổng quản trị KizunaTutor</strong></div>
    <div class="topbar-actions"><span class="security-status"><span class="status-dot"></span><span id="headerStatus">Không gian quản trị</span></span><button class="notification-button" type="button" disabled aria-label="Thông báo chưa được tích hợp"><img src="/assets/icons/admin-dashboard/bell.svg" alt=""></button><div class="admin-identity"><strong>Không gian quản trị</strong><small>Quản trị viên hệ thống</small></div></div>
  </header>`;
  if(csWorkspace){
    const root=shell.content,brand=root.querySelector('.admin-brand'),logo=brand.querySelector('img');logo.src='/assets/cs-dashboard/logo.png';logo.width=40;logo.height=40;brand.querySelector('small').remove();
    root.querySelector('.nav-caption').textContent='Nhân viên hỗ trợ';
    root.querySelector('.sidebar-menu nav').innerHTML=`
      <a href="/cs/dashboard" data-cs-navigation><img src="/assets/cs-dashboard/grid.svg" alt="" width="20" height="20">Tổng quan</a>
      <a href="/cs/support/tickets" data-cs-navigation data-support-link><img src="/assets/cs-dashboard/chat.svg" alt="" width="20" height="20">Yêu cầu hỗ trợ</a>
      <a href="/pages/support-shifts/?view=cs" data-cs-navigation><img src="/assets/icons/admin-dashboard/calendar.svg" alt="" width="20" height="20">Lịch làm việc</a>`;
    root.querySelector('.sidebar-bottom').innerHTML='<p>Kết nối để cùng tiến bộ</p><small>Không gian hỗ trợ người dùng</small><button type="button" class="logout-button">Đăng xuất</button>';
    root.querySelector('.topbar-breadcrumb>span').textContent='Không gian làm việc';
    const notifications=root.querySelector('.notification-button');notifications.textContent='Thông báo';notifications.title='Thông báo tạm thời chưa khả dụng';notifications.setAttribute('aria-label','Thông báo tạm thời chưa khả dụng');
  }
  if(!csWorkspace){
    document.body.classList.add('admin-portal');
    const root=shell.content,topbar=root.querySelector('.admin-topbar'),brand=root.querySelector('.admin-brand'),notifications=root.querySelector('.notification-button');
    root.querySelector('.nav-caption').textContent='Quản trị viên';
    root.querySelector('.sidebar-menu nav').innerHTML=adminNavigation();
    brand.querySelector('small').textContent='Kết nối song ngữ Việt – Nhật';
    topbar.prepend(brand);
    const bell=document.createElement('span');bell.textContent='Thông báo';notifications.append(bell);notifications.title='Thông báo sẽ khả dụng khi FE-12 được kết nối';
    decoratePortalPage(location.pathname);
  }
  const skipLink = document.querySelector('.skip-link');
  if (skipLink) skipLink.after(shell.content.cloneNode(true));
  else document.body.prepend(shell.content.cloneNode(true));
  const footer = document.createElement('template');
  footer.innerHTML = `    <footer class="admin-footer"><div><img src="/assets/logos/kizuna-logo.png" alt="" width="28" height="28"><strong>Cổng quản trị KizunaTutor</strong><span>•</span><span>Kết nối song ngữ Việt – Nhật</span></div><a href="/pages/home/">Về trang chủ ↗</a></footer>`;
  document.querySelector('main').append(footer.content.cloneNode(true));
  const path = location.pathname;
  const activePath = PAGE_ALIASES[path] || path;
  document.querySelectorAll('.sidebar-menu a,.sidebar-bottom a').forEach(link => {
    const target = new URL(link.href);
    if (target.pathname === activePath && !target.hash) { link.setAttribute('aria-current', 'page'); link.closest('details')?.setAttribute('open', ''); }
  });
  if(csWorkspace){
    const selected=path==='/pages/cs-dashboard/'?'/cs/dashboard':path==='/pages/support-shifts/'?'/pages/support-shifts/?view=cs':'/cs/support/tickets';
    document.querySelectorAll('[data-cs-navigation]').forEach(link=>{const url=new URL(link.href);if(url.pathname+url.search===selected)link.setAttribute('aria-current','page');else link.removeAttribute('aria-current');});
  }
  const title = document.querySelector('h1')?.textContent;
  if (title) document.querySelector('.topbar-breadcrumb strong').textContent = title;
  document.getElementById('headerStatus').textContent = 'Không gian quản trị';
  const identity = document.querySelector('.admin-identity');
  identity.querySelector('strong').textContent = 'Đang xác minh tài khoản…';
  identity.setAttribute('aria-live', 'polite');
  fetchAPI('/admin/operations/me').then(account => {
    identity.querySelector('strong').textContent = account.displayName;
    identity.querySelector('small').textContent = csWorkspace&&account.canSupport&&!account.canReport?'Nhân viên hỗ trợ':account.roleLabel;
    identity.setAttribute('tabindex', '0'); identity.setAttribute('role', 'link'); identity.setAttribute('aria-label', 'Xem hồ sơ cá nhân của ' + account.displayName);
    const openProfile = () => location.assign('/pages/admin-profile/');
    identity.addEventListener('click', openProfile);
    identity.addEventListener('keydown', event => { if (event.key === 'Enter') openProfile(); });
    if (account.canAdmin === false) {
      document.querySelectorAll('.sidebar-menu a,.sidebar-menu button').forEach(link => {
        const target = link.getAttribute('href');
        link.hidden = !((['/pages/manager-reports/', '/pages/manager-staff-roles/', '/pages/operations-revenue/', '/pages/support-shifts/'].includes(target) && account.canReport) || (link.hasAttribute('data-support-link') && account.canSupport) || (csWorkspace&&link.hasAttribute('data-cs-navigation')&&account.canSupport&&!account.canReport));
      });
      document.querySelector('.nav-caption').textContent = csWorkspace?'Nhân viên hỗ trợ':account.canReport?'Quản lý':'Không gian nội bộ';
      document.querySelectorAll('.nav-group').forEach(group => { group.hidden = ![...group.querySelectorAll('a')].some(link => !link.hidden); if (!group.hidden) group.open = true; });
      document.getElementById('headerStatus').textContent = account.roleLabel;
      if (location.pathname === '/pages/admin-dashboard/') location.replace(account.canReport ? '/pages/manager-reports/' : account.canSupport?'/cs/dashboard':'/pages/admin-profile/');
    }
    const supportLink = document.querySelector('[data-support-link]');
    supportLink.hidden = !account.canSupport;
    if (account.canSupport) supportLink.href = account.canReport ? '/manager/support/tickets' : '/cs/support/tickets';
    if (account.canSupport && account.canReport) supportLink.lastChild.textContent = 'Yêu cầu chuyển lên';
    if (account.canSupport) attachSupportGroups(supportLink, account.canReport ? SUPPORT_GROUPS.manager : SUPPORT_GROUPS.cs, account.canReport ? '/manager/support/tickets' : '/cs/support/tickets');
    const avatar = document.createElement('span');
    avatar.className = 'admin-avatar';
    avatar.textContent = account.displayName.trim().split(/\s+/).slice(-2).map(word => word[0]).join('').toLocaleUpperCase('vi-VN');
    avatar.setAttribute('aria-hidden', 'true');
    if (account.avatarUrl) {
      try {
        const url = new URL(account.avatarUrl, location.origin);
        if (url.protocol === 'https:' || (url.origin === location.origin && url.pathname.startsWith('/assets/'))) {
          const image = document.createElement('img'); image.src = url.href; image.alt = ''; image.referrerPolicy = 'no-referrer';
          image.addEventListener('error', () => image.remove()); avatar.append(image);
        }
      } catch { /* Dùng chữ viết tắt nếu địa chỉ ảnh không hợp lệ. */ }
    }
    identity.before(avatar);
  }).catch(error => {
    if (error.status === 401) { location.assign('/login'); return; }
    identity.querySelector('strong').textContent = 'Chưa xác minh tài khoản';
    identity.querySelector('small').textContent = 'Vui lòng kiểm tra kết nối hoặc phiên truy cập';
  });
  document.querySelector('.logout-button')?.addEventListener('click', logout);
  const sidebar = document.getElementById('adminSidebar');
  const toggle = document.getElementById('sidebarToggle');
  const backdrop = document.getElementById('sidebarBackdrop');
  function setOpen(open) {
    sidebar.inert = !open && matchMedia('(max-width: 760px)').matches;
    sidebar.classList.toggle('is-open', open);
    toggle.setAttribute('aria-expanded', String(open));
    toggle.setAttribute('aria-label', open ? 'Đóng điều hướng' : 'Mở điều hướng');
    backdrop.hidden = !open;
    if (open) (sidebar.querySelector('a[aria-current]') || sidebar.querySelector('a')).focus();
  }
  toggle.addEventListener('click', () => setOpen(!sidebar.classList.contains('is-open')));
  backdrop.addEventListener('click', () => { setOpen(false); toggle.focus(); });
  document.addEventListener('keydown', event => {
    if (!sidebar.classList.contains('is-open')) return;
    if (event.key === 'Escape') { setOpen(false); toggle.focus(); }
    if (event.key === 'Tab') {
      const links = [...sidebar.querySelectorAll('a[href]')];
      const first = links[0], last = links[links.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    }
  });
  sidebar.querySelectorAll('a').forEach(link => link.addEventListener('click', () => setOpen(false)));
  matchMedia('(max-width: 760px)').addEventListener('change', () => setOpen(false));
  setOpen(false);
}
