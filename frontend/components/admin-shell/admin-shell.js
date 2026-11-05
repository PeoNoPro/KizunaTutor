/** Khung quản trị dùng chung cho dashboard và các trang vận hành FE-11. */
import { fetchAPI } from '../../js/api.js';
// Theo Figma, các màn cùng nhóm hiển thị như tab của một trang; mỗi tab giữ đường dẫn riêng để mở trực tiếp.
const PORTAL_GROUPS = [
  {title: 'Tài khoản nội bộ và phân quyền', lead: 'Tạo tài khoản nhân viên, quản lý truy cập và duyệt đề xuất thăng chức.', tabs: [['Danh sách nhân viên', '/pages/admin-staff/'], ['Vai trò và quyền', '/pages/admin-roles/']]},
  {title: 'Vận hành hệ thống', lead: 'Theo dõi sự cố, thống kê, báo cáo, nhật ký và cấu hình theo quyền Quản trị viên.', tabs: [['Vận hành', '/pages/operations-incidents/'], ['Thống kê', '/pages/operations-statistics/'], ['Báo cáo', '/pages/operations-reports/'], ['Nhật ký', '/pages/operations-logs/'], ['Cấu hình', '/pages/operations-config/']]}
];
function decoratePortalPage(path) {
  const heading = document.querySelector('.operations-page-heading');
  if (!heading) return;
  const group = PORTAL_GROUPS.find(item => item.tabs.some(([, href]) => href === path));
  if (!group) return;
  heading.querySelector('h1').textContent = group.title; heading.querySelector('p').textContent = group.lead;
  const tabs = document.createElement('nav'); tabs.className = 'portal-tabs'; tabs.setAttribute('aria-label', group.title);
  group.tabs.forEach(([label, href]) => { const tab = document.createElement('a'); tab.href = href; tab.textContent = label; if (href === path) tab.setAttribute('aria-current', 'page'); tabs.append(tab); });
  heading.after(tabs);
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
      <a href="/cs/support/tickets" data-cs-navigation data-support-link><img src="/assets/cs-dashboard/chat.svg" alt="" width="20" height="20">Yêu cầu hỗ trợ</a>`;
    root.querySelector('.sidebar-bottom').innerHTML='<p>Kết nối để cùng tiến bộ</p><small>Không gian hỗ trợ người dùng</small><button type="button" class="logout-button">Đăng xuất</button>';
    root.querySelector('.topbar-breadcrumb>span').textContent='Không gian làm việc';
    const notifications=root.querySelector('.notification-button');notifications.textContent='Thông báo';notifications.title='Thông báo tạm thời chưa khả dụng';notifications.setAttribute('aria-label','Thông báo tạm thời chưa khả dụng');
  }
  if(!csWorkspace){
    document.body.classList.add('admin-portal');
    const root=shell.content,topbar=root.querySelector('.admin-topbar'),brand=root.querySelector('.admin-brand'),notifications=root.querySelector('.notification-button');
    root.querySelector('.nav-caption').textContent='Quản trị viên';
    root.querySelector('.sidebar-menu nav').innerHTML=`
      <a href="/pages/admin-dashboard/">Tổng quan</a>
      <a href="/pages/admin-staff/">Tài khoản nội bộ</a>
      <a href="/pages/operations-incidents/">Vận hành hệ thống</a>
      <a href="/pages/manager-reports/">Báo cáo tổng hợp</a>
      <a href="/pages/operations-revenue/">Doanh thu</a>
      <a href="/pages/manager-staff-roles/" hidden>Điều chuyển nhân sự</a>
      <a href="/pages/support-tickets/" data-support-link hidden><img src="/assets/icons/admin-dashboard/nav-incidents.svg" alt="">Yêu cầu hỗ trợ</a>`;
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
  const activePath = path.includes('/support-ticket/') ? '/pages/support-tickets/' : path.includes('operations-incident/') ? '/pages/operations-incidents/' : !csWorkspace && PORTAL_GROUPS.find(group => group.tabs.some(([, href]) => href === path))?.tabs[0][1] || path;
  document.querySelectorAll('.sidebar-menu a,.sidebar-bottom a').forEach(link => {
    const target = new URL(link.href);
    if (target.pathname === activePath && !target.hash) link.setAttribute('aria-current', 'page');
  });
  if(csWorkspace){
    const selected=path==='/pages/cs-dashboard/'?'/cs/dashboard':'/cs/support/tickets';
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
        link.hidden = !((['/pages/manager-reports/', '/pages/manager-staff-roles/', '/pages/operations-revenue/'].includes(target) && account.canReport) || (link.hasAttribute('data-support-link') && account.canSupport) || (csWorkspace&&link.hasAttribute('data-cs-navigation')&&account.canSupport&&!account.canReport));
      });
      document.querySelector('.nav-caption').textContent = csWorkspace?'Nhân viên hỗ trợ':account.canReport?'Quản lý':'Không gian nội bộ';
      document.getElementById('headerStatus').textContent = account.roleLabel;
      if (location.pathname === '/pages/admin-dashboard/') location.replace(account.canReport ? '/pages/manager-reports/' : account.canSupport?'/cs/dashboard':'/pages/admin-profile/');
    }
    const supportLink = document.querySelector('[data-support-link]');
    supportLink.hidden = !account.canSupport;
    if (account.canSupport) supportLink.href = account.canReport ? '/manager/support/tickets' : '/cs/support/tickets';
    if (account.canSupport && account.canReport) supportLink.lastChild.textContent = 'Yêu cầu chuyển lên';
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
