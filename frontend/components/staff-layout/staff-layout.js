/**
 * KizunaTutor - Khung giao diện cho Quản lý vận hành và Quản trị viên (FE-09), cùng phong cách Stitch của cổng học vụ.
 *
 * Trang dùng:
 *   <div id="staffSidebar"></div>
 *   <div class="lg:pl-72 ..."><div id="staffTopbar"></div> <main>...</main></div>
 *   import { mountStaffLayout } from '/components/staff-layout/staff-layout.js';
 *   mountStaffLayout('manager', 'membershipPlans', 'Quản lý gói hội viên');
 *
 * Chỉ chứa mục đã có trang thật của FE-09; không dẫn tới trang chưa xây.
 */

import { userMenuHtml, bindUserMenu } from '/components/user-menu/user-menu.js';

const ROLE_LABELS = {
  ROLE_ADMIN: 'Quản trị viên',
  ROLE_MANAGER: 'Quản lý vận hành',
  ROLE_CS_STAFF: 'Chăm sóc khách hàng',
  ROLE_ACADEMIC_OFFICER: 'Cán bộ học vụ',
  ROLE_TUTOR: 'Gia sư',
  ROLE_LEARNER: 'Học viên'
};

const ROLES = {
  manager: {
    requiredRole: 'ROLE_MANAGER',
    portal: 'Quản lý vận hành',
    roleLabel: 'Quản lý vận hành',
    home: '/manager/membership-plans',
    items: [
      { key: 'membershipPlans', label: 'Gói hội viên', icon: 'workspace_premium', href: '/manager/membership-plans' },
      { key: 'tutorMemberships', label: 'Hội viên gia sư', icon: 'card_membership', href: '/manager/tutor-memberships' }
    ]
  },
  admin: {
    requiredRole: 'ROLE_ADMIN',
    portal: 'Quản trị hệ thống',
    roleLabel: 'Quản trị viên',
    home: '/admin/payments',
    items: [
      { key: 'payments', label: 'Quản lý thanh toán', icon: 'receipt_long', href: '/admin/payments' }
    ]
  }
};

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text == null ? '' : String(text);
  return div.innerHTML;
}

function storedUser() {
  try {
    const raw = localStorage.getItem('kizuna_user');
    return raw ? (JSON.parse(raw) || {}) : {};
  } catch (e) {
    return {};
  }
}

/** Thông báo khi tài khoản đang đăng nhập không đúng vai trò của trang (API vẫn tự chặn bằng 403). */
function renderWrongRole(config, currentRole) {
  const main = document.querySelector('main');
  if (!main) return;
  const current = ROLE_LABELS[currentRole] || 'vai trò khác';
  main.innerHTML = `
    <div class="px-4 md:px-8 py-10 flex justify-center">
      <section class="tv-card w-full max-w-lg p-8 text-center">
        <span class="material-symbols-outlined text-5xl text-outline">lock</span>
        <h1 class="text-headline-sm mt-2">Trang dành cho ${escapeHtml(config.roleLabel)}</h1>
        <p class="text-body-md text-on-surface-variant mt-2">Bạn đang đăng nhập với vai trò ${escapeHtml(current)}. Vui lòng đăng nhập bằng tài khoản ${escapeHtml(config.roleLabel)} để sử dụng chức năng này.</p>
        <div class="mt-6 flex justify-center gap-2">
          <button id="staffSwitchAccount" type="button" class="tv-btn tv-btn-primary">Đăng nhập tài khoản khác</button>
        </div>
      </section>
    </div>`;
  document.getElementById('staffSwitchAccount')?.addEventListener('click', async () => {
    const { logout } = await import('/components/user-menu/user-menu.js');
    logout();
  });
}

function navItemHtml(item, activeKey) {
  const active = item.key === activeKey;
  const cls = active
    ? 'bg-primary-container text-on-primary-container font-semibold rounded-lg shadow-sm'
    : 'rounded-lg text-on-surface-variant hover:bg-surface-container hover:text-on-surface';
  return `
    <a href="${item.href}" class="flex items-center gap-3 px-3 py-2.5 transition-all ${cls}" ${active ? 'aria-current="page"' : ''}>
      <span class="material-symbols-outlined text-[20px]">${item.icon}</span>
      <span class="text-label-md">${item.label}</span>
    </a>`;
}

/**
 * Dựng khung trang. Trả về false nếu tài khoản không đúng vai trò của trang — khi đó trang không gọi API.
 */
export function mountStaffLayout(role, activeKey, pageTitle) {
  const config = ROLES[role] || ROLES.manager;
  const user = storedUser();
  const allowed = !user.role || user.role === config.requiredRole;
  const name = user.fullName || config.roleLabel;
  const roleLabel = ROLE_LABELS[user.role] || config.roleLabel;
  const sidebar = document.getElementById('staffSidebar');
  const topbar = document.getElementById('staffTopbar');

  if (sidebar) {
    sidebar.innerHTML = `
      <div id="staffSidebarBackdrop" class="fixed inset-0 bg-black/30 z-40 hidden lg:hidden"></div>
      <aside id="staffSidebarPanel" class="fixed left-0 top-0 bottom-0 w-72 bg-surface-container-lowest shadow-[0_1px_8px_rgba(0,0,0,0.04)] z-50 flex flex-col py-6 -translate-x-full lg:translate-x-0 transition-transform duration-200 overflow-y-auto">
        <div class="px-6 mb-8 flex items-center gap-3">
          <img src="/assets/logos/kizuna-logo.png" alt="KizunaTutor" class="h-10 w-10 object-contain rounded-lg">
          <div class="flex flex-col">
            <span class="text-title-md font-bold tracking-tight text-on-surface">KizunaTutor</span>
            <span class="text-caption text-on-surface-variant tracking-wider">${escapeHtml(config.portal)}</span>
          </div>
        </div>
        <div class="px-6 mb-3"><span class="text-caption text-outline tracking-wider font-semibold">Gói hội viên và thanh toán</span></div>
        <nav class="flex flex-col gap-1 px-4" aria-label="Điều hướng ${escapeHtml(config.portal.toLowerCase())}">
          ${config.items.map((item) => navItemHtml(item, activeKey)).join('')}
        </nav>
      </aside>`;
  }

  if (topbar) {
    topbar.innerHTML = `
      <header class="fixed top-0 left-0 lg:left-72 right-0 h-16 bg-surface-container-lowest/90 backdrop-blur-md shadow-[0_1px_8px_rgba(0,0,0,0.03)] z-30 flex items-center justify-between px-4 md:px-8 gap-3">
        <div class="flex items-center gap-2 text-body-sm text-on-surface-variant min-w-0">
          <button id="staffMenuButton" type="button" class="lg:hidden p-2 rounded-lg hover:bg-surface-container" aria-label="Mở menu">
            <span class="material-symbols-outlined">menu</span>
          </button>
          <a href="${config.home}" class="text-label-md text-outline hover:text-on-surface transition-colors">${escapeHtml(config.portal)}</a>
          <span class="material-symbols-outlined text-[16px] text-outline-variant">chevron_right</span>
          <span class="text-label-md text-on-surface font-semibold truncate">${escapeHtml(pageTitle || '')}</span>
        </div>
        ${userMenuHtml({ prefix: 'staff', name, roleLabel, avatarClass: 'w-10 h-10', showHome: false })}
      </header>`;
  }

  const panel = document.getElementById('staffSidebarPanel');
  const backdrop = document.getElementById('staffSidebarBackdrop');
  const toggle = (open) => {
    panel?.classList.toggle('-translate-x-full', !open);
    backdrop?.classList.toggle('hidden', !open);
  };
  document.getElementById('staffMenuButton')?.addEventListener('click', () => toggle(true));
  backdrop?.addEventListener('click', () => toggle(false));
  bindUserMenu('staff');

  if (!allowed) {
    renderWrongRole(config, user.role);
  }
  return allowed;
}
