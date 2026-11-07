/**
 * KizunaTutor - Khung giao diện dùng chung cho khu vực Gia sư (sidebar + thanh trên), theo thiết kế Stitch.
 *
 * Trang dùng:
 *   <div id="tutorSidebar"></div>
 *   <div class="lg:pl-72 ..."><div id="tutorTopbar"></div> <main>...</main></div>
 *   import { mountTutorLayout, setTutorIdentity } from '/components/tutor-layout/tutor-layout.js';
 *   mountTutorLayout('dashboard');            // khóa của mục đang mở
 *   setTutorIdentity({ fullName, jlptLevel, statusLabel });   // sau khi tải dữ liệu thật
 *
 * Mục chưa có chức năng được hiển thị mờ kèm nhãn "Sắp ra mắt", không dẫn tới trang giả.
 */

import { userMenuHtml, bindUserMenu, setUserMenuName } from '/components/user-menu/user-menu.js';

// Đúng thứ tự và biểu tượng của thiết kế Stitch. "Hồ sơ gia sư" mở trang xác minh hồ sơ (FE-03).
const NAV_ITEMS = [
  { key: 'dashboard', label: 'Tổng quan', icon: 'grid_view', href: '/tutor/dashboard' },
  { key: 'connections', label: 'Yêu cầu kết nối', icon: 'group_add', href: '/tutor/connections' },
  { key: 'schedule', label: 'Lịch giảng dạy', icon: 'calendar_month', href: '/tutor/schedule' },
  { key: 'students', label: 'Học viên của tôi', icon: 'school', href: '/tutor/classrooms' },
  { key: 'materials', label: 'Tài liệu học tập', icon: 'menu_book', href: '/tutor/classrooms' },
  { key: 'assignments', label: 'Quản lý bài tập', icon: 'assignment', href: '/tutor/classrooms' },
  { key: 'progress', label: 'Báo cáo tiến độ', icon: 'insights' },
  { key: 'notifications', label: 'Thông báo', icon: 'notifications' },
  { key: 'verification', label: 'Hồ sơ gia sư', icon: 'badge', href: '/tutor/verification' },
  { key: 'membership', label: 'Gói hội viên', icon: 'workspace_premium', href: '/tutor/membership' },
  // FE-10: gia sư gửi và theo dõi yêu cầu hỗ trợ.
  { key: 'support', label: 'Yêu cầu hỗ trợ', icon: 'support_agent', href: '/tutor/support/tickets' },
  { key: 'settings', label: 'Cài đặt', icon: 'settings' }
];

function navItemHtml(item, activeKey) {
  const active = item.key === activeKey;
  const cls = active
    ? 'bg-primary-container text-on-primary-container font-semibold rounded-lg shadow-sm'
    : 'rounded-lg text-on-surface-variant text-label-md hover:bg-surface-container-high hover:text-on-surface';
  // Mục chưa có trang: giữ nguyên giao diện như thiết kế, bấm vào chỉ báo chức năng đang phát triển
  const attrs = item.href
    ? `href="${item.href}"`
    : `href="#" data-coming-soon="${item.label}" title="Chức năng đang được phát triển"`;
  return `
    <a ${attrs} class="flex items-center gap-3 px-3 py-2.5 transition-colors ${cls}" ${active ? 'aria-current="page"' : ''}>
      <span class="material-symbols-outlined text-[20px]">${item.icon}</span>
      <span>${item.label}</span>
    </a>`;
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

function storedUserName() {
  try {
    const raw = localStorage.getItem('kizuna_user');
    return raw ? (JSON.parse(raw).fullName || '') : '';
  } catch (e) {
    return '';
  }
}

export function mountTutorLayout(activeKey) {
  const sidebar = document.getElementById('tutorSidebar');
  const topbar = document.getElementById('tutorTopbar');

  if (sidebar) {
    sidebar.innerHTML = `
      <div id="tutorSidebarBackdrop" class="fixed inset-0 bg-black/30 z-40 hidden lg:hidden"></div>
      <aside id="tutorSidebarPanel" class="fixed left-0 top-0 h-full w-72 bg-surface-container-lowest shadow-[0_1px_8px_rgba(0,0,0,0.04)] z-50 flex flex-col justify-between -translate-x-full lg:translate-x-0 transition-transform duration-200 overflow-y-auto">
        <div class="flex flex-col">
          <div class="px-6 pt-6 pb-4">
            <a href="/home" title="Về trang chủ KizunaTutor" class="flex items-center gap-2 mb-1 no-underline">
              <div class="w-8 h-8 rounded-lg bg-primary-container flex items-center justify-center text-on-primary text-headline-sm">絆</div>
              <div><span class="text-headline-sm text-brand-navy">Kizuna</span><span class="text-headline-sm text-primary">Tutor</span></div>
            </a>
            <p class="text-caption text-on-surface-variant tracking-wider">Kết nối song ngữ Việt - Nhật</p>
          </div>
          <nav class="flex flex-col gap-1 px-4 mt-2" aria-label="Điều hướng gia sư">
            ${NAV_ITEMS.map((item) => navItemHtml(item, activeKey)).join('')}
          </nav>
        </div>
        <div class="px-6 py-4 mx-4 my-4 rounded-xl bg-surface-container-low">
          <div class="flex items-center gap-3">
            <span class="material-symbols-outlined text-primary text-[22px]">verified</span>
            <div>
              <p id="tutorSidebarLevel" class="text-label-sm text-on-surface">Gia sư tiếng Nhật</p>
              <p id="tutorSidebarStatus" class="text-caption text-on-surface-variant">Trạng thái: đang tải…</p>
            </div>
          </div>
        </div>
      </aside>`;
  }

  if (topbar) {
    topbar.innerHTML = `
      <header class="fixed top-0 left-0 lg:left-72 right-0 h-16 bg-surface-container-lowest/90 backdrop-blur-md shadow-[0_1px_8px_rgba(0,0,0,0.04)] z-30">
        <div class="w-full h-16 px-4 md:px-8 flex items-center justify-between gap-3">
          <button id="tutorMenuButton" type="button" class="lg:hidden p-2 rounded-lg text-on-surface-variant hover:bg-surface-container" aria-label="Mở menu">
            <span class="material-symbols-outlined">menu</span>
          </button>
          <div class="flex-1"></div>
          ${userMenuHtml({ prefix: 'tutor', name: storedUserName() || 'Gia sư', roleLabel: 'Gia sư tiếng Nhật', avatarClass: 'w-8 h-8' })}
        </div>
      </header>`;
  }

  const panel = document.getElementById('tutorSidebarPanel');
  const backdrop = document.getElementById('tutorSidebarBackdrop');
  const toggle = (open) => {
    if (!panel) return;
    panel.classList.toggle('-translate-x-full', !open);
    backdrop?.classList.toggle('hidden', !open);
  };
  sidebar?.querySelectorAll('[data-coming-soon]').forEach((link) => {
    link.addEventListener('click', (event) => {
      event.preventDefault();
      notifyComingSoon(link.dataset.comingSoon);
    });
  });
  document.getElementById('tutorMenuButton')?.addEventListener('click', () => toggle(true));
  backdrop?.addEventListener('click', () => toggle(false));
  bindUserMenu('tutor');
}

/**
 * Cập nhật tên, trình độ và trạng thái thật của gia sư lên sidebar và thanh trên.
 */
export function setTutorIdentity({ fullName, jlptLevel, statusLabel } = {}) {
  const name = fullName || 'Gia sư';
  setUserMenuName('tutor', name);
  const level = document.getElementById('tutorSidebarLevel');
  if (level) level.textContent = jlptLevel ? `Gia sư JLPT ${jlptLevel}` : 'Gia sư tiếng Nhật';
  const status = document.getElementById('tutorSidebarStatus');
  if (status) status.textContent = statusLabel ? `Xác minh: ${statusLabel}` : '';
}
