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

import { userMenuHtml, bindUserMenu, setUserMenuName, setUserMenuDetails } from '/components/user-menu/user-menu.js';
import { brandLogoHtml } from '/components/brand-logo/brand-logo.js';

// Đúng thứ tự và biểu tượng của thiết kế Stitch. "Hồ sơ gia sư" mở trang xác minh hồ sơ (FE-03).
const NAV_ITEMS = [
  { key: 'dashboard', label: 'Tổng quan', icon: 'grid_view', href: '/tutor/dashboard' },
  { key: 'connections', label: 'Yêu cầu kết nối', icon: 'group_add', href: '/tutor/connections', member: true },
  { key: 'schedule', label: 'Lịch giảng dạy', icon: 'calendar_month', href: '/tutor/schedule', member: true },
  // Ba mục cùng mở trang lớp học; ?view= quyết định mục được tô sáng và khối được cuộn tới
  { key: 'students', label: 'Học viên của tôi', icon: 'school', href: '/tutor/classrooms?view=students', member: true },
  { key: 'materials', label: 'Tài liệu học tập', icon: 'menu_book', href: '/tutor/classrooms?view=materials', member: true },
  { key: 'assignments', label: 'Quản lý bài tập', icon: 'assignment', href: '/tutor/classrooms?view=assignments', member: true },
  { key: 'progress', label: 'Báo cáo tiến độ', icon: 'insights', member: true },
  { key: 'verification', label: 'Hồ sơ gia sư', icon: 'badge', href: '/tutor/verification' },
  { key: 'membership', label: 'Gói hội viên', icon: 'workspace_premium', href: '/tutor/membership' },
  { key: 'post', label: 'Bài đăng của tôi', icon: 'campaign', href: '/tutor/post', member: true },
  // FE-10: gia sư gửi và theo dõi yêu cầu hỗ trợ.
  { key: 'support', label: 'Yêu cầu hỗ trợ', icon: 'support_agent', href: '/tutor/support/tickets' },
  { key: 'settings', label: 'Cài đặt', icon: 'settings' }
];
// "Thông báo" nằm cuối sidebar, ngay trên thẻ tài khoản (giống khung CS / Quản lý); số chưa đọc do notification-badge.js gắn vào.
const NOTIFICATION_ITEM = { key: 'notifications', label: 'Thông báo', icon: 'notifications', href: '/pages/notifications/' };

function navItemHtml(item, activeKey) {
  const active = item.key === activeKey;
  const cls = active
    ? 'bg-primary-container text-on-primary-container font-semibold rounded-lg shadow-sm'
    : 'rounded-lg text-on-surface-variant text-label-md hover:bg-surface-container-high hover:text-on-surface';
  // Mục chưa có trang: giữ nguyên giao diện như thiết kế, bấm vào chỉ báo chức năng đang phát triển
  const attrs = item.href
    ? `href="${item.href}"`
    : `href="#" data-coming-soon="${item.label}" title="Chức năng đang được phát triển"`;
  // Mục cần gói hội viên: biểu tượng khóa được bật sau khi biết gia sư chưa có gói còn hạn
  const lock = item.member
    ? '<span data-lock-icon class="hidden ml-auto material-symbols-outlined text-[18px] text-outline" title="Cần gói hội viên">lock</span>'
    : '';
  return `
    <a ${attrs} ${item.member ? `data-member-only="${item.label}"` : ''} class="flex items-center gap-3 px-3 py-2.5 transition-colors ${cls}" ${active ? 'aria-current="page"' : ''}>
      <span class="material-symbols-outlined text-[20px]">${item.icon}</span>
      <span>${item.label}</span>${lock}
    </a>`;
}

const MEMBERSHIP_MESSAGE = 'Bạn cần đăng ký gói hội viên còn hiệu lực để sử dụng chức năng này.';
let membershipLocked = false;

async function showToastMessage(message, type) {
  try {
    const { showToast } = await import('/js/main.js');
    showToast(message, type);
  } catch (e) {
    window.alert(message);
  }
}

/** Thay nội dung trang bằng thông báo cần gói hội viên (giữ nguyên phần tử gốc để script của trang không lỗi). */
function showMembershipGate(main, label) {
  Array.from(main.children).forEach((el) => { if (!el.dataset.membershipGate) el.classList.add('hidden'); });
  main.querySelector('[data-membership-gate]')?.remove();
  const gate = document.createElement('div');
  gate.dataset.membershipGate = 'true';
  gate.className = 'px-4 md:px-8 py-6';
  gate.innerHTML = `
    <section class="tv-card p-8 text-center max-w-xl mx-auto mt-10">
      <span class="material-symbols-outlined text-5xl text-primary">lock</span>
      <h2 class="text-headline-sm mt-2">${label ? `"${label}" dành cho hội viên` : 'Chức năng dành cho hội viên'}</h2>
      <p class="text-on-surface-variant mt-1">${MEMBERSHIP_MESSAGE} Sau khi thanh toán thành công, bạn có thể dùng ngay.</p>
      <a href="/tutor/membership" class="tv-btn tv-btn-primary mt-4 inline-flex items-center gap-2">
        <span class="material-symbols-outlined text-[18px]">shopping_cart</span>Mua gói hội viên
      </a>
    </section>`;
  main.appendChild(gate);
}

/** Tô sáng mục sidebar đang xem (dùng khi mở màn hình khóa của mục chưa có trang riêng). */
function highlightNav(link) {
  const activeCls = ['bg-primary-container', 'text-on-primary-container', 'font-semibold', 'shadow-sm'];
  const idleCls = ['text-on-surface-variant', 'hover:bg-surface-container-high', 'hover:text-on-surface'];
  document.querySelectorAll('#tutorSidebar nav a').forEach((a) => {
    const on = a === link;
    activeCls.forEach((c) => a.classList.toggle(c, on));
    idleCls.forEach((c) => a.classList.toggle(c, !on));
    if (on) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
  });
}

// Kết quả kiểm gói được nhớ tạm trong phiên để chuyển trang không phải chờ API mỗi lần.
const MEMBERSHIP_CACHE_KEY = 'kizuna_tutor_membership_lock';
const MEMBERSHIP_CACHE_MS = 5 * 60 * 1000;
// Chờ API kiểm gói tối đa chừng này rồi hiện trang luôn (backend vẫn chặn API nếu chưa có gói).
const MEMBERSHIP_MAX_HIDE_MS = 1500;

function readMembershipCache() {
  // Trang gói hội viên / kết quả thanh toán luôn hỏi lại server: vừa mua gói xong thì mở khóa ngay
  if (/membership/i.test(window.location.pathname)) return null;
  try {
    const raw = sessionStorage.getItem(MEMBERSHIP_CACHE_KEY);
    if (!raw) return null;
    const { locked, at } = JSON.parse(raw);
    return Date.now() - at < MEMBERSHIP_CACHE_MS ? Boolean(locked) : null;
  } catch (e) {
    return null;
  }
}

function writeMembershipCache(locked) {
  try {
    sessionStorage.setItem(MEMBERSHIP_CACHE_KEY, JSON.stringify({ locked, at: Date.now() }));
  } catch (e) { /* trình duyệt chặn bộ nhớ: bỏ qua */ }
}

function applyLockedUi(main, activeItem, pageLocked) {
  document.querySelectorAll('#tutorSidebar [data-lock-icon]').forEach((el) => el.classList.remove('hidden'));
  if (pageLocked) showMembershipGate(main, activeItem.label);
}

/** Kiểm gói hội viên: khóa các mục cần gói trên sidebar và chặn trang đang mở nếu là trang cần gói. */
async function applyMembershipLock(activeItem) {
  const main = document.querySelector('main');
  const pageLocked = Boolean(activeItem && activeItem.member && main);

  // Đã biết kết quả trong phiên: áp dụng ngay, không ẩn trang
  const cached = readMembershipCache();
  if (cached !== null) {
    membershipLocked = cached;
    if (membershipLocked) applyLockedUi(main, activeItem, pageLocked);
  } else if (pageLocked) {
    // Lần đầu: ẩn nội dung trong lúc chờ, nhưng không quá MEMBERSHIP_MAX_HIDE_MS để tránh trang trắng khi API chậm
    main.classList.add('invisible');
    setTimeout(() => main.classList.remove('invisible'), MEMBERSHIP_MAX_HIDE_MS);
  }

  try {
    const { API } = await import('/js/api.js');
    const data = await API.tutorMembership.getMine();
    const locked = !data || !data.expiresOn;
    writeMembershipCache(locked);
    if (locked && !membershipLocked) applyLockedUi(main, activeItem, pageLocked);
    membershipLocked = locked;
  } catch (e) {
    // Không xác định được (lỗi mạng...): không khóa trên giao diện, backend vẫn chặn API
    if (cached === null) membershipLocked = false;
  }
  if (pageLocked) main.classList.remove('invisible');
}

function notifyComingSoon(label) {
  showToastMessage(`Chức năng "${label}" đang được phát triển. Vui lòng quay lại sau.`, 'info');
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
            ${brandLogoHtml({ href: '/home', subtitle: 'Không gian gia sư' })}
          </div>
          <nav class="flex flex-col gap-1 px-4 mt-2" aria-label="Điều hướng gia sư">
            ${NAV_ITEMS.map((item) => navItemHtml(item, activeKey)).join('')}
          </nav>
        </div>
        <!-- Menu tài khoản ở góc dưới bên trái (thay cho thanh trên) -->
        <div class="mx-4 my-4 flex flex-col gap-2">
          <nav class="flex flex-col" aria-label="Thông báo">${navItemHtml(NOTIFICATION_ITEM, activeKey)}</nav>
          ${userMenuHtml({
            prefix: 'tutor', name: storedUserName() || 'Gia sư', roleLabel: 'Gia sư tiếng Nhật', placement: 'sidebar',
            // FE-02: thông tin cá nhân, giảng dạy và đổi mật khẩu của gia sư.
            menuItems: [{ label: 'Hồ sơ cá nhân', icon: 'person', href: '/tutor/profile' }]
          })}
        </div>
      </aside>`;
  }

  // Thanh trên chỉ còn trên màn hình nhỏ để mở sidebar; trên máy tính nội dung chiếm toàn bộ màn hình.
  if (topbar) {
    topbar.innerHTML = `
      <header class="lg:hidden fixed top-0 left-0 right-0 h-16 bg-surface-container-lowest/90 backdrop-blur-md shadow-[0_1px_8px_rgba(0,0,0,0.04)] z-30">
        <div class="w-full h-16 px-4 flex items-center gap-3">
          <button id="tutorMenuButton" type="button" class="p-2 rounded-lg text-on-surface-variant hover:bg-surface-container" aria-label="Mở menu">
            <span class="material-symbols-outlined">menu</span>
          </button>
          <div class="flex-1"><span class="text-headline-sm text-brand-navy">Kizuna</span><span class="text-headline-sm text-primary">Tutor</span></div>
          <button type="button" data-notification-bell class="p-2 rounded-lg text-on-surface-variant hover:bg-surface-container hover:text-on-surface transition-colors" aria-label="Thông báo"><span class="material-symbols-outlined text-[22px]">notifications</span></button>
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
  // Mục bị khóa: mục có trang riêng vẫn mở trang (trang tự hiện màn hình mua gói);
  // mục chưa có trang thì hiện màn hình mua gói ngay tại chỗ thay vì báo "đang phát triển".
  // Đăng ký trước listener "Sắp ra mắt" để chặn được listener đó.
  sidebar?.querySelectorAll('[data-member-only]').forEach((link) => {
    link.addEventListener('click', (event) => {
      if (!membershipLocked || !link.dataset.comingSoon) return;
      const main = document.querySelector('main');
      if (!main) return;
      event.preventDefault();
      event.stopImmediatePropagation();
      showMembershipGate(main, link.dataset.memberOnly);
      highlightNav(link);
      toggle(false);
    });
  });
  sidebar?.querySelectorAll('[data-coming-soon]').forEach((link) => {
    link.addEventListener('click', (event) => {
      event.preventDefault();
      notifyComingSoon(link.dataset.comingSoon);
    });
  });
  applyMembershipLock(NAV_ITEMS.find((item) => item.key === activeKey));
  document.getElementById('tutorMenuButton')?.addEventListener('click', () => toggle(true));
  backdrop?.addEventListener('click', () => toggle(false));
  bindUserMenu('tutor');
  // FE-12: số thông báo chưa đọc cạnh mục Thông báo.
  import('/js/pages/notification-badge.js').then(module => module.attachNotificationBadges()).catch(() => {});
}

/**
 * Cập nhật tên, trình độ và trạng thái thật của gia sư lên sidebar và thanh trên.
 */
export function setTutorIdentity({ fullName, jlptLevel, statusLabel } = {}) {
  const name = fullName || 'Gia sư';
  setUserMenuName('tutor', name);
  setUserMenuDetails('tutor', {
    roleLabel: jlptLevel ? `Gia sư JLPT ${jlptLevel}` : 'Gia sư tiếng Nhật',
    statusLabel: statusLabel ? `Xác minh: ${statusLabel}` : ''
  });
}
