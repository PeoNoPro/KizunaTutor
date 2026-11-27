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

// Mục có "children" là nhóm thả xuống: bấm tiêu đề để mở / đóng; nhóm chứa trang đang mở tự mở sẵn.
// (2026-11-23) Bỏ "Lịch sử xét duyệt" khỏi sidebar theo yêu cầu người phụ trách; trang /academic/history vẫn còn.
const NAV_ITEMS = [
  { key: 'dashboard', label: 'Tổng quan', icon: 'space_dashboard', href: '/academic/dashboard' },
  {
    key: 'review-group', label: 'Xét duyệt', icon: 'fact_check', children: [
      { key: 'verifications', label: 'Hồ sơ xác thực gia sư', icon: 'verified_user', href: '/academic/verifications', counted: true },
      { key: 'posts', label: 'Duyệt bài đăng', icon: 'campaign', href: '/academic/posts', counted: true }
    ]
  },
  {
    key: 'content-group', label: 'Học liệu & đề thi', icon: 'library_books', children: [
      // FE-13 (thêm 2026-11-17): Ngân hàng đề luyện thi JLPT.
      { key: 'practice', label: 'Ngân hàng đề', icon: 'quiz', href: '/academic/practice' },
      { key: 'content-import', label: 'Kho học liệu', icon: 'menu_book', href: '/academic/content-import' }
    ]
  },
  { key: 'notifications', label: 'Thông báo', icon: 'notifications', href: '/pages/notifications/' }
  // "Hồ sơ cá nhân" mở từ menu tài khoản ở góc dưới sidebar (đồng bộ với học viên, gia sư).
];

const GROUP_STATE_KEY = 'kizuna_academic_nav_group_';

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
      ${item.counted ? `<span data-nav-count="${item.key}" hidden class="ml-auto min-w-[20px] h-5 px-1.5 rounded-full bg-[#b42318] text-white text-[11px] font-bold leading-5 text-center shrink-0"></span>` : ''}
    </a>`;
}

/** Nhóm thả xuống: tiêu đề là nút bấm; danh sách mục con thụt vào. Tổng số đơn chờ của nhóm hiện trên tiêu đề. */
function navGroupHtml(group, activeKey) {
  const hasActive = group.children.some((child) => child.key === activeKey);
  let open = hasActive;
  if (!hasActive) {
    try {
      open = sessionStorage.getItem(GROUP_STATE_KEY + group.key) === '1';
    } catch (e) {
      open = false;
    }
  }
  const counted = group.children.filter((child) => child.counted).map((child) => child.key);
  return `
    <div data-nav-group="${group.key}">
      <button type="button" data-nav-group-toggle="${group.key}" aria-expanded="${open}" aria-controls="academicNavGroup-${group.key}"
        class="w-full flex items-center gap-3 px-3 py-2.5 transition-all ${STAFF_NAV_IDLE} ${hasActive ? 'text-[#0f2545] font-semibold' : ''}">
        <span class="material-symbols-outlined text-[20px]">${group.icon}</span>
        <span class="text-label-md">${group.label}</span>
        <!-- Số đếm + mũi tên luôn ở mép phải (số đếm ẩn khi bằng 0 thì mũi tên vẫn thẳng hàng với nhóm không có số đếm) -->
        <span class="ml-auto flex items-center gap-2 shrink-0">
          ${counted.length ? `<span data-nav-group-count="${counted.join(',')}" hidden class="min-w-[20px] h-5 px-1.5 rounded-full bg-[#b42318] text-white text-[11px] font-bold leading-5 text-center shrink-0"></span>` : ''}
          <span data-nav-group-chevron class="material-symbols-outlined text-[20px] transition-transform ${open ? 'rotate-180' : ''}">expand_more</span>
        </span>
      </button>
      <div id="academicNavGroup-${group.key}" class="flex flex-col gap-1 pl-4 mt-1 ${open ? '' : 'hidden'}">
        ${group.children.map((child) => navItemHtml(child, activeKey)).join('')}
      </div>
    </div>`;
}

function navEntryHtml(entry, activeKey) {
  return entry.children ? navGroupHtml(entry, activeKey) : navItemHtml(entry, activeKey);
}

/** Bấm tiêu đề nhóm để mở / đóng; nhớ trạng thái trong phiên (lỗi lưu trữ thì chỉ không nhớ). */
function bindNavGroups(root) {
  root?.querySelectorAll('[data-nav-group-toggle]').forEach((button) => {
    button.addEventListener('click', () => {
      const key = button.dataset.navGroupToggle;
      const list = document.getElementById('academicNavGroup-' + key);
      const open = list.classList.contains('hidden');
      list.classList.toggle('hidden', !open);
      button.setAttribute('aria-expanded', String(open));
      button.querySelector('[data-nav-group-chevron]')?.classList.toggle('rotate-180', open);
      try {
        sessionStorage.setItem(GROUP_STATE_KEY + key, open ? '1' : '0');
      } catch (e) {
        /* không nhớ được trạng thái */
      }
    });
  });
}

/**
 * Số đơn đang chờ cán bộ học vụ xử lý, hiện cạnh mục tương ứng trên sidebar (ẩn khi bằng 0):
 * - Hồ sơ xác thực gia sư: hồ sơ chờ thẩm định + yêu cầu cập nhật hồ sơ chờ duyệt.
 * - Duyệt bài đăng: bài đăng chờ duyệt.
 * Lỗi tải số thì bỏ qua (số chỉ để tham khảo). Trang gọi lại sau khi duyệt / từ chối để số khớp ngay.
 */
export async function refreshAcademicNavCounts() {
  if (!document.querySelector('[data-nav-count]')) return;
  let API;
  try {
    ({ API } = await import('/js/api.js'));
  } catch (e) {
    return;
  }
  const totals = {};
  const show = (key, value) => {
    const n = Number(value) || 0;
    totals[key] = n;
    document.querySelectorAll(`[data-nav-count="${key}"]`).forEach((el) => {
      el.hidden = n === 0;
      el.textContent = n > 99 ? '99+' : String(n);
      el.closest('a')?.setAttribute('title', n ? `${n} đơn đang chờ xử lý` : '');
    });
  };
  const [verifications, posts] = await Promise.allSettled([
    API.academicVerification.getSummary(),
    API.academicPosts.getSummary()
  ]);
  if (verifications.status === 'fulfilled' && verifications.value) {
    const s = verifications.value;
    show('verifications', (s.pendingCount || 0) + (s.profileUpdatePendingCount || 0));
  }
  if (posts.status === 'fulfilled' && posts.value) {
    show('posts', posts.value.pendingCount);
  }
  // Tổng của nhóm trên tiêu đề nhóm (thấy được cả khi nhóm đang đóng)
  document.querySelectorAll('[data-nav-group-count]').forEach((el) => {
    const n = el.dataset.navGroupCount.split(',').reduce((sum, key) => sum + (totals[key] || 0), 0);
    el.hidden = n === 0;
    el.textContent = n > 99 ? '99+' : String(n);
  });
}

const NAV_COUNT_REFRESH_MS = 60000;

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
          <div class="px-6 mb-8 flex items-center justify-between gap-2">
          ${brandLogoHtml({ href: '/academic/dashboard', subtitle: 'Cổng học vụ', className: 'min-w-0' })}
          <button type="button" data-notification-bell class="p-2 rounded-lg text-on-surface-variant hover:bg-surface-container hover:text-on-surface transition-colors shrink-0" aria-label="Thông báo"><span class="material-symbols-outlined text-[22px]">notifications</span></button>
          </div>
          <div class="px-6 mb-3"><span class="text-caption text-[#707881] tracking-wider font-semibold">Chức năng chính</span></div>
          <nav class="flex flex-col gap-1 px-4" aria-label="Điều hướng học vụ">
            ${NAV_ITEMS.map((item) => navEntryHtml(item, activeKey)).join('')}
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
        <span class="text-label-md text-on-surface font-semibold truncate flex-1">${escapeHtml(pageTitle || 'Tổng quan học vụ')}</span>
        <button type="button" data-notification-bell class="p-2 rounded-lg text-on-surface-variant hover:bg-surface-container hover:text-on-surface transition-colors" aria-label="Thông báo"><span class="material-symbols-outlined text-[22px]">notifications</span></button>
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
  bindNavGroups(sidebar);
  sidebar?.querySelectorAll('[data-coming-soon]').forEach((link) => {
    link.addEventListener('click', (event) => {
      event.preventDefault();
      notifyComingSoon(link.textContent.trim());
    });
  });
  // Số đơn chờ xử lý cạnh mục Hồ sơ xác thực gia sư / Duyệt bài đăng; tự cập nhật mỗi phút và khi quay lại tab.
  refreshAcademicNavCounts();
  setInterval(() => { if (!document.hidden) refreshAcademicNavCounts(); }, NAV_COUNT_REFRESH_MS);
  document.addEventListener('visibilitychange', () => { if (!document.hidden) refreshAcademicNavCounts(); });
  // FE-12: số thông báo chưa đọc cạnh mục Thông báo.
  import('/js/pages/notification-badge.js').then(module => module.attachNotificationBadges()).catch(() => {});
}
