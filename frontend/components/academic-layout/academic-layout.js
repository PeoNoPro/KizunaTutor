/**
 * KizunaTutor - Khung giao diện "Cổng học vụ" cho Cán bộ học vụ (sidebar + thanh trên), theo thiết kế Stitch.
 *
 * Trang dùng:
 *   <div id="academicSidebar"></div>
 *   <div class="lg:pl-72 ..."><div id="academicTopbar"></div> <main>...</main></div>
 *   import { mountAcademicLayout } from '/components/academic-layout/academic-layout.js';
 *   mountAcademicLayout('dashboard', 'Tổng quan học vụ');   // khóa mục đang mở + tên trang trên breadcrumb
 *
 * Tên người dùng lấy từ phiên đăng nhập (localStorage 'kizuna_user'). Mục chưa có trang chỉ báo "đang được phát triển".
 */

import { userMenuHtml, bindUserMenu } from '/components/user-menu/user-menu.js';

const NAV_ITEMS = [
  { key: 'dashboard', label: 'Tổng quan', icon: 'space_dashboard', href: '/academic/dashboard' },
  { key: 'verifications', label: 'Hồ sơ xác thực gia sư', icon: 'verified_user', href: '/academic/verifications#queue' },
  { key: 'history', label: 'Lịch sử xét duyệt', icon: 'history_edu' },
  { key: 'notifications', label: 'Thông báo', icon: 'notifications' },
  { key: 'profile', label: 'Hồ sơ cá nhân', icon: 'account_circle' }
];

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text == null ? '' : String(text);
  return div.innerHTML;
}

function storedUserName() {
  try {
    const raw = localStorage.getItem('kizuna_user');
    return raw ? (JSON.parse(raw).fullName || '') : '';
  } catch (e) {
    return '';
  }
}

async function notifyComingSoon(label) {
  const message = `Chức năng "${label}" đang được phát triển. Vui lòng quay lại sau.`;
  try {
    const { showToast } = await import('/js/main.js');
    showToast(message, 'info');
  } catch (e) {
    window.alert(message);
  }
}

function navItemHtml(item, activeKey) {
  const active = item.key === activeKey;
  const cls = active
    ? 'bg-primary-container text-on-primary-container font-semibold rounded-lg shadow-sm'
    : 'rounded-lg text-on-surface-variant hover:bg-surface-container hover:text-on-surface';
  const attrs = item.href
    ? `href="${item.href}"`
    : 'href="#" data-coming-soon="true" title="Chức năng đang được phát triển"';
  return `
    <a ${attrs} class="flex items-center gap-3 px-3 py-2.5 transition-all ${cls}" ${active ? 'aria-current="page"' : ''}>
      <span class="material-symbols-outlined text-[20px]">${item.icon}</span>
      <span class="text-label-md">${item.label}</span>
    </a>`;
}

export function mountAcademicLayout(activeKey, pageTitle) {
  const name = storedUserName() || 'Cán bộ học vụ';
  const sidebar = document.getElementById('academicSidebar');
  const topbar = document.getElementById('academicTopbar');

  if (sidebar) {
    sidebar.innerHTML = `
      <div id="academicSidebarBackdrop" class="fixed inset-0 bg-black/30 z-40 hidden lg:hidden"></div>
      <aside id="academicSidebarPanel" class="fixed left-0 top-0 bottom-0 w-72 bg-surface-container-lowest shadow-[0_1px_8px_rgba(0,0,0,0.04)] z-50 flex flex-col justify-between py-6 -translate-x-full lg:translate-x-0 transition-transform duration-200 overflow-y-auto">
        <div class="flex flex-col">
          <a href="/academic/dashboard" class="px-6 mb-8 flex items-center gap-3 no-underline">
            <img src="/assets/logos/kizuna-logo.png" alt="KizunaTutor" class="h-10 w-10 object-contain rounded-lg">
            <div class="flex flex-col">
              <span class="text-title-md font-bold tracking-tight text-on-surface">KizunaTutor</span>
              <span class="text-caption text-on-surface-variant tracking-wider">Cổng học vụ</span>
            </div>
          </a>
          <div class="px-6 mb-3"><span class="text-caption text-outline tracking-wider font-semibold">Chức năng chính</span></div>
          <nav class="flex flex-col gap-1 px-4" aria-label="Điều hướng học vụ">
            ${NAV_ITEMS.map((item) => navItemHtml(item, activeKey)).join('')}
          </nav>
        </div>
      </aside>`;
  }

  if (topbar) {
    topbar.innerHTML = `
      <header class="fixed top-0 left-0 lg:left-72 right-0 h-16 bg-surface-container-lowest/90 backdrop-blur-md shadow-[0_1px_8px_rgba(0,0,0,0.03)] z-30 flex items-center justify-between px-4 md:px-8 gap-3">
        <div class="flex items-center gap-2 text-body-sm text-on-surface-variant min-w-0">
          <button id="academicMenuButton" type="button" class="lg:hidden p-2 rounded-lg hover:bg-surface-container" aria-label="Mở menu">
            <span class="material-symbols-outlined">menu</span>
          </button>
          <a href="/academic/dashboard" class="text-label-md text-outline hover:text-on-surface transition-colors">Học vụ</a>
          <span class="material-symbols-outlined text-[16px] text-outline-variant">chevron_right</span>
          <span class="text-label-md text-on-surface font-semibold truncate">${escapeHtml(pageTitle || 'Tổng quan học vụ')}</span>
        </div>
        ${userMenuHtml({ prefix: 'academic', name, roleLabel: 'Cán bộ học vụ', avatarClass: 'w-10 h-10', showHome: false })}
      </header>`;
  }

  const panel = document.getElementById('academicSidebarPanel');
  const backdrop = document.getElementById('academicSidebarBackdrop');
  const toggle = (open) => {
    panel?.classList.toggle('-translate-x-full', !open);
    backdrop?.classList.toggle('hidden', !open);
  };
  document.getElementById('academicMenuButton')?.addEventListener('click', () => toggle(true));
  backdrop?.addEventListener('click', () => toggle(false));
  bindUserMenu('academic');
  sidebar?.querySelectorAll('[data-coming-soon]').forEach((link) => {
    link.addEventListener('click', (event) => {
      event.preventDefault();
      notifyComingSoon(link.textContent.trim());
    });
  });
}
