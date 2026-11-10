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
import { brandLogoHtml } from '/components/brand-logo/brand-logo.js';

const NAV_ITEMS = [
  { key: 'dashboard', label: 'Tổng quan', icon: 'space_dashboard', href: '/academic/dashboard' },
  { key: 'verifications', label: 'Hồ sơ xác thực gia sư', icon: 'verified_user', href: '/academic/verifications?view=queue' },
  { key: 'posts', label: 'Duyệt bài đăng', icon: 'fact_check', href: '/academic/posts' },
  { key: 'history', label: 'Lịch sử xét duyệt', icon: 'history_edu', href: '/academic/history' },
  { key: 'notifications', label: 'Thông báo', icon: 'notifications' }
  // "Hồ sơ cá nhân" mở từ menu tài khoản ở góc dưới sidebar (đồng bộ với học viên, gia sư).
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

// Màu theo khung Quản trị viên (styles/admin-portal.css, styles/staff-unified.css): nền kem, mục đang mở tô xanh.
export const STAFF_PAGE_BG = '#fcfbf7';
export const STAFF_NAV_ACTIVE = 'bg-[#006194] text-white font-semibold rounded-lg shadow-sm';
export const STAFF_NAV_IDLE = 'rounded-lg text-[#3f4850] hover:bg-[#f7f4ec] hover:text-[#0f2545]';
export const STAFF_SIDEBAR = 'bg-white border-r border-[#ece8de] shadow-[0_1px_8px_rgba(0,0,0,0.04)]';
export const STAFF_ACCOUNT_CARD = 'bg-[#f7f4ec] hover:bg-[#ece8de]';

function navItemHtml(item, activeKey) {
  const active = item.key === activeKey;
  const cls = active ? STAFF_NAV_ACTIVE : STAFF_NAV_IDLE;
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

  // Nền trang kem giống khung Quản trị viên
  document.body.style.backgroundColor = STAFF_PAGE_BG;

  if (sidebar) {
    sidebar.innerHTML = `
      <div id="academicSidebarBackdrop" class="fixed inset-0 bg-black/30 z-40 hidden lg:hidden"></div>
      <aside id="academicSidebarPanel" class="fixed left-0 top-0 bottom-0 w-72 ${STAFF_SIDEBAR} z-50 flex flex-col justify-between py-6 -translate-x-full lg:translate-x-0 transition-transform duration-200 overflow-y-auto">
        <div class="flex flex-col">
          ${brandLogoHtml({ href: '/academic/dashboard', subtitle: 'Cổng học vụ', className: 'px-6 mb-8' })}
          <div class="px-6 mb-3"><span class="text-caption text-[#707881] tracking-wider font-semibold">Chức năng chính</span></div>
          <nav class="flex flex-col gap-1 px-4" aria-label="Điều hướng học vụ">
            ${NAV_ITEMS.map((item) => navItemHtml(item, activeKey)).join('')}
          </nav>
        </div>
        <!-- Menu tài khoản ở góc dưới bên trái (thay cho thanh trên) -->
        <div class="mx-4 mt-4">
          ${userMenuHtml({
            prefix: 'academic', name, roleLabel: 'Cán bộ học vụ', showHome: false, placement: 'sidebar', cardClass: STAFF_ACCOUNT_CARD,
            menuItems: [{ label: 'Hồ sơ cá nhân', icon: 'account_circle', href: '/academic/profile' }]
          })}
        </div>
      </aside>`;
  }

  // Thanh trên chỉ còn trên màn hình nhỏ để mở sidebar; trên máy tính nội dung chiếm toàn bộ màn hình.
  if (topbar) {
    topbar.innerHTML = `
      <header class="lg:hidden fixed top-0 left-0 right-0 h-16 bg-white/90 backdrop-blur-md border-b border-[#ece8de] z-30 flex items-center px-4 gap-2">
        <button id="academicMenuButton" type="button" class="p-2 rounded-lg hover:bg-surface-container" aria-label="Mở menu">
          <span class="material-symbols-outlined">menu</span>
        </button>
        <span class="text-label-md text-on-surface font-semibold truncate">${escapeHtml(pageTitle || 'Tổng quan học vụ')}</span>
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
